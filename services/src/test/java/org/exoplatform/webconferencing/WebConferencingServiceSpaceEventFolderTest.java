/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.webconferencing;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * The recording folder of a {@code space_event} call is the Documents folder
 * of the hosting space, or the private folder of the hosting user for a
 * personal event (EXO-92565).
 */
class WebConferencingServiceSpaceEventFolderTest {

  private static final String    HOST_ID = "5";

  private WebConferencingService webConferencing;

  private IdentityManager        identityManager;

  private SpaceService           spaceService;

  private NodeHierarchyCreator   nodeCreator;

  private SessionProvider        sessionProvider;

  private Session                session;

  @BeforeEach
  void setUp() throws Exception {
    webConferencing = mock(WebConferencingService.class, CALLS_REAL_METHODS);
    identityManager = mock(IdentityManager.class);
    spaceService = mock(SpaceService.class);
    nodeCreator = mock(NodeHierarchyCreator.class);
    RepositoryService repositoryService = mock(RepositoryService.class);
    SessionProviderService sessionProviders = mock(SessionProviderService.class);
    sessionProvider = mock(SessionProvider.class);
    session = mock(Session.class);
    ManageableRepository repository = mock(ManageableRepository.class, RETURNS_DEEP_STUBS);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(repository.getConfiguration().getDefaultWorkspaceName()).thenReturn("collaboration");
    when(sessionProviders.getSystemSessionProvider(any())).thenReturn(sessionProvider);
    when(sessionProvider.getSession(anyString(), any())).thenReturn(session);
    when(nodeCreator.getJcrPath(WebConferencingService.CMS_GROUPS_PATH)).thenReturn("/Groups");
    when(nodeCreator.getJcrPath("userPrivate")).thenReturn("Private");
    ReflectionTestUtils.setField(webConferencing, "socialIdentityManager", identityManager);
    ReflectionTestUtils.setField(webConferencing, "spaceService", spaceService);
    ReflectionTestUtils.setField(webConferencing, "nodeCreator", nodeCreator);
    ReflectionTestUtils.setField(webConferencing, "repositoryService", repositoryService);
    ReflectionTestUtils.setField(webConferencing, "sessionProviders", sessionProviders);
  }

  @Test
  void testPersonalEventRecordingFolderIsTheHostUserPrivateFolder() throws Exception {
    Identity userIdentity = new Identity("organization", "john");
    userIdentity.setId(HOST_ID);
    when(identityManager.getIdentity(HOST_ID)).thenReturn(userIdentity);
    // A space whose pretty name equals the user name must not be taken for the host
    Space homonymSpace = new Space();
    homonymSpace.setPrettyName("john");
    homonymSpace.setGroupId("/spaces/john");
    when(spaceService.getSpaceByPrettyName("john")).thenReturn(homonymSpace);
    Node homonymSpaceRoot = mock(Node.class);
    when(session.getItem("/Groups/spaces/john")).thenReturn(homonymSpaceRoot);
    when(homonymSpaceRoot.getNode("Documents")).thenReturn(mock(Node.class));
    Node userNode = mock(Node.class);
    Node privateFolder = mock(Node.class);
    when(nodeCreator.getUserNode(sessionProvider, "john")).thenReturn(userNode);
    when(userNode.getNode("Private")).thenReturn(privateFolder);

    Node folder = webConferencing.getRootFolderNode(HOST_ID, WebConferencingService.OWNER_TYPE_SPACEEVENT);

    assertSame(privateFolder, folder);
  }

  @Test
  void testSpaceEventRecordingFolderIsTheHostSpaceDocuments() throws Exception {
    Identity spaceIdentity = new Identity("space", "sales_team");
    spaceIdentity.setId(HOST_ID);
    when(identityManager.getIdentity(HOST_ID)).thenReturn(spaceIdentity);
    Space space = new Space();
    space.setPrettyName("sales_team");
    space.setGroupId("/spaces/sales_team");
    when(spaceService.getSpaceByPrettyName("sales_team")).thenReturn(space);
    Node spaceRoot = mock(Node.class);
    Node documents = mock(Node.class);
    when(session.getItem("/Groups/spaces/sales_team")).thenReturn(spaceRoot);
    when(spaceRoot.getNode("Documents")).thenReturn(documents);

    Node folder = webConferencing.getRootFolderNode(HOST_ID, WebConferencingService.OWNER_TYPE_SPACEEVENT);

    assertSame(documents, folder);
  }

  @Test
  void testSpaceEventOfAGoneSpaceHasNoRecordingFolder() throws Exception {
    // A space identity whose space is not found gets no user folder in its name
    Identity spaceIdentity = new Identity("space", "sales_team");
    spaceIdentity.setId(HOST_ID);
    when(identityManager.getIdentity(HOST_ID)).thenReturn(spaceIdentity);
    Node userNode = mock(Node.class);
    when(nodeCreator.getUserNode(sessionProvider, "sales_team")).thenReturn(userNode);
    when(userNode.getNode("Private")).thenReturn(mock(Node.class));

    assertThrows(RepositoryException.class,
                 () -> webConferencing.getRootFolderNode(HOST_ID, WebConferencingService.OWNER_TYPE_SPACEEVENT));
  }
}
