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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.upload.UploadResource;

/**
 * A recording is saved with a session opened for the user it names, never
 * with the session provider left in the thread by an earlier caller.
 */
class WebConferencingServiceSaveRecordingTest {

  private static final String    USER_FOLDER  = "/Users/j___/jo/john/Private";

  private static final String    SPACE_FOLDER = "/Groups/spaces/sales_team/Documents";

  private WebConferencingService webConferencing;

  private SessionProviderService sessionProviders;

  private SpaceService           spaceService;

  private ManageableRepository   repository;

  private SessionProvider        callerProvider;

  private SessionProvider        systemProvider;

  private ExtendedSession        userSession;

  private ExtendedSession        systemSession;

  private Node                   userFolder;

  private UploadResource         resource;

  private ConversationState      callerState;

  private File                   recording;

  private List<String>           loginIdentities = new ArrayList<>();

  @BeforeEach
  void setUp() throws Exception {
    webConferencing = mock(WebConferencingService.class, CALLS_REAL_METHODS);
    spaceService = mock(SpaceService.class);
    sessionProviders = mock(SessionProviderService.class);
    RepositoryService repositoryService = mock(RepositoryService.class);
    IdentityRegistry identityRegistry = mock(IdentityRegistry.class);
    repository = mock(ManageableRepository.class);
    RepositoryEntry configuration = mock(RepositoryEntry.class);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.getDefaultWorkspaceName()).thenReturn("collaboration");

    // What an earlier caller left in the thread: neither of them is john's
    callerProvider = mock(SessionProvider.class);
    when(sessionProviders.getSessionProvider(any())).thenReturn(callerProvider);
    callerState = new ConversationState(new Identity("caller"));
    ConversationState.setCurrent(callerState);

    when(identityRegistry.getIdentity("john")).thenReturn(new Identity("john"));

    userSession = mock(ExtendedSession.class);
    // A provider built on a ConversationState logs in as the state current when it opens the session
    when(repository.login("collaboration")).thenAnswer(invocation -> {
      loginIdentities.add(ConversationState.getCurrent().getIdentity().getUserId());
      return userSession;
    });
    systemProvider = mock(SessionProvider.class);
    systemSession = mock(ExtendedSession.class);
    when(sessionProviders.getSystemSessionProvider(any())).thenReturn(systemProvider);
    when(systemProvider.getSession(anyString(), any())).thenReturn(systemSession);

    userFolder = mockFolder(USER_FOLDER);
    when(userSession.getItem(USER_FOLDER)).thenReturn(userFolder);
    Node spaceFolder = mockFolder(SPACE_FOLDER);
    when(systemSession.getItem(SPACE_FOLDER)).thenReturn(spaceFolder);

    recording = File.createTempFile("recording", ".webm");
    resource = mock(UploadResource.class);
    when(resource.getFileName()).thenReturn("call.webm");
    when(resource.getMimeType()).thenReturn("video/webm");
    when(resource.getStoreLocation()).thenReturn(recording.getAbsolutePath());

    ReflectionTestUtils.setField(webConferencing, "sessionProviders", sessionProviders);
    ReflectionTestUtils.setField(webConferencing, "repositoryService", repositoryService);
    ReflectionTestUtils.setField(webConferencing, "identityRegistry", identityRegistry);
    ReflectionTestUtils.setField(webConferencing, "spaceService", spaceService);
    ReflectionTestUtils.setField(webConferencing, "nodeCreator", mock(NodeHierarchyCreator.class));
  }

  @AfterEach
  void tearDown() {
    ConversationState.setCurrent(null);
    recording.delete();
  }

  @Test
  void testRecordingIsSavedWithASessionOpenedForTheNamedUser() throws Exception {
    webConferencing.saveFile(mockFolder(USER_FOLDER), resource, "john", null);

    verify(repository).login("collaboration");
    assertEquals(List.of("john"), loginIdentities);
    verify(userFolder).save();
    // The provider left in the thread by an earlier caller is not used
    verifyNoInteractions(callerProvider);
    verify(sessionProviders, never()).setSessionProvider(any(), any());
    verifyNoInteractions(systemProvider);
    // The session opened for the user is closed afterwards
    verify(userSession).logout();
  }

  @Test
  void testCallerStateAndSessionProviderSlotAreRestored() throws Exception {
    webConferencing.saveFile(mockFolder(USER_FOLDER), resource, "john", null);

    assertSame(callerState, ConversationState.getCurrent());
    verify(sessionProviders, never()).setSessionProvider(any(), any());
    verify(sessionProviders, never()).removeSessionProvider(any());
  }

  @Test
  void testCallerStateIsRestoredAndSessionClosedWhenTheSaveFails() throws Exception {
    when(userSession.getItem(USER_FOLDER)).thenThrow(new RepositoryException("save failed"));

    assertThrows(RepositoryException.class, () -> webConferencing.saveFile(mockFolder(USER_FOLDER), resource, "john", null));

    assertSame(callerState, ConversationState.getCurrent());
    verify(sessionProviders, never()).setSessionProvider(any(), any());
    verify(userSession).logout();
  }

  @Test
  void testUserOfAnUnknownIdentityDoesNotFallBackOnTheThreadProvider() throws Exception {
    assertThrows(UploadFileException.class, () -> webConferencing.saveFile(mockFolder(USER_FOLDER), resource, "ghost", null));

    assertSame(callerState, ConversationState.getCurrent());
    verifyNoInteractions(callerProvider);
  }

  @Test
  void testNonMemberOfTheSpaceStillSavesInTheSpaceDocuments() throws Exception {
    Space space = new Space();
    when(spaceService.getSpaceByGroupId("/spaces/sales_team")).thenReturn(space);
    when(spaceService.isMember(space, "john")).thenReturn(false);

    webConferencing.saveFile(mockFolder(SPACE_FOLDER), resource, "john", null);

    // Unchanged: a non-member cannot write in the space folder with their own rights
    verify(systemSession).getItem(SPACE_FOLDER);
    verify(repository, never()).login(anyString());
    verifyNoInteractions(callerProvider);
    assertSame(callerState, ConversationState.getCurrent());
  }

  @Test
  void testMemberOfTheSpaceSavesWithTheirOwnSession() throws Exception {
    Space space = new Space();
    when(spaceService.getSpaceByGroupId("/spaces/sales_team")).thenReturn(space);
    when(spaceService.isMember(space, "john")).thenReturn(true);
    Node spaceFolder = mockFolder(SPACE_FOLDER);
    when(userSession.getItem(SPACE_FOLDER)).thenReturn(spaceFolder);

    webConferencing.saveFile(mockFolder(SPACE_FOLDER), resource, "john", null);

    verify(repository).login("collaboration");
    assertEquals(List.of("john"), loginIdentities);
    verify(spaceFolder).save();
    verifyNoInteractions(systemProvider);
    verify(userSession).logout();
  }

  private Node mockFolder(String path) throws RepositoryException {
    Node folder = mock(Node.class, RETURNS_DEEP_STUBS);
    when(folder.getPath()).thenReturn(path);
    when(folder.hasNode("recordings")).thenReturn(false);
    return folder;
  }
}
