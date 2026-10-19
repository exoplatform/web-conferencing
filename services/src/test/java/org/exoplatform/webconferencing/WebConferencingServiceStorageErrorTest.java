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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.persistence.impl.EntityManagerService;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wcm.ext.component.document.service.ShareDocumentService;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.webconferencing.dao.CallDAO;
import org.exoplatform.webconferencing.dao.InviteDAO;
import org.exoplatform.webconferencing.dao.OriginDAO;
import org.exoplatform.webconferencing.dao.ParticipantDAO;
import org.exoplatform.webconferencing.dao.StorageException;
import org.exoplatform.webconferencing.domain.CallEntity;
import org.exoplatform.webconferencing.domain.InviteEntity;
import org.exoplatform.webconferencing.domain.ParticipantEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.RollbackException;
import jakarta.persistence.TypedQuery;

/**
 * A database failure while saving a call is reported as that call operation's
 * error. The calls run through the real DAOs, the kernel's EntityManager proxy
 * they get from GenericDAOJPAImpl and the woven ExoTransactional aspect: only
 * the persistence engine behind them is mocked (the EntityManager, its
 * transaction and the container that provides them), so a failure reaches the
 * service in the form the platform delivers it. Each failure is raised where a
 * database failure leaves an EntityManager call: the entities have assigned
 * ids, so their writes run at commit, and the call that reaches the database
 * through the proxy is the read that precedes the write, or a persist that
 * flushes.
 */
@ExtendWith(MockitoExtension.class)
class WebConferencingServiceStorageErrorTest {

  private static final String    CALL_ID  = "p2p_john_mary";

  private static final String    PROVIDER = "jitsi";

  private static final UserInfo  JOHN     = new UserInfo("john", "John", "Smith");

  private static final UserInfo  MARY     = new UserInfo("mary", "Mary", "Williams");

  @Mock
  private ExoContainer           container;

  @Mock
  private EntityManagerService   entityManagerService;

  @Mock
  private EntityManager          entityManager;

  @Mock
  private EntityTransaction      transaction;

  private WebConferencingService service;

  @BeforeEach
  void setUp() {
    lenient().when(container.getComponentInstanceOfType(EntityManagerService.class)).thenReturn(entityManagerService);
    lenient().when(entityManagerService.getEntityManager()).thenReturn(entityManager);
    lenient().when(entityManager.getTransaction()).thenReturn(transaction);
    AtomicBoolean active = new AtomicBoolean();
    lenient().when(transaction.isActive()).thenAnswer(invocation -> active.get());
    lenient().doAnswer(invocation -> {
      active.set(true);
      return null;
    }).when(transaction).begin();
    lenient().doAnswer(invocation -> {
      active.set(false);
      return null;
    }).when(transaction).commit();
    lenient().doAnswer(invocation -> {
      active.set(false);
      return null;
    }).when(transaction).rollback();
    ExoContainerContext.setCurrentContainer(container);

    InitParams initParams = mock(InitParams.class);
    when(initParams.getPropertiesParam(anyString())).thenReturn(new PropertiesParam());
    service = spy(new WebConferencingService(mock(OrganizationService.class),
                                             mock(IdentityManager.class),
                                             mock(ListenerService.class),
                                             mock(SettingService.class),
                                             new CallDAO(),
                                             new ParticipantDAO(),
                                             new OriginDAO(),
                                             new InviteDAO(),
                                             mock(UploadService.class),
                                             mock(RepositoryService.class),
                                             mock(SessionProviderService.class),
                                             mock(NodeHierarchyCreator.class),
                                             mock(IdentityRegistry.class),
                                             mock(Authenticator.class),
                                             mock(ShareDocumentService.class),
                                             initParams,
                                             mock(LinkManager.class),
                                             mock(CodecInitializer.class)));
  }

  @AfterEach
  void tearDown() {
    ExoContainerContext.setCurrentContainer(null);
  }

  @Test
  void addParticipantReportsAFailedReadAsAStorageError() throws Exception {
    doReturn(MARY).when(service).userInfo(MARY.getId());
    PersistenceException failure = failOnFind(ParticipantEntity.class);

    StorageException error = assertThrows(StorageException.class, () -> service.addParticipant(CALL_ID, MARY.getId()));
    assertSame(failure, error.getCause());
  }

  @Test
  void addGuestReportsAFailedReadAsAStorageError() throws Exception {
    doReturn(null).when(service).userInfo("guest@acme.com");
    doReturn(newCall()).when(service).getCall(CALL_ID);
    PersistenceException failure = failOnFind(ParticipantEntity.class);

    StorageException error = assertThrows(StorageException.class, () -> service.addGuest(CALL_ID, "guest@acme.com"));
    assertSame(failure, error.getCause());
  }

  /**
   * Inviting again an identity whose invitation the update has just deleted
   * persists over a deleted key, which makes Hibernate flush: a database failure
   * then leaves the persist.
   */
  @Test
  void updateInvitesReportsAFailedSaveAsAStorageError() throws Exception {
    doReturn(newCall()).when(service).getCall(CALL_ID);
    returnOnQuery("WebConfInvite.findCallInvites",
                  InviteEntity.class,
                  List.of(new InviteEntity(CALL_ID, MARY.getId(), UserInfo.TYPE_NAME, "invitation")));
    PersistenceException failure = new PersistenceException("Database unavailable");
    doThrow(failure).when(entityManager).persist(any(InviteEntity.class));

    StorageException error = assertThrows(StorageException.class,
                                          () -> service.updateInvites(CALL_ID,
                                                                      List.of(new InvitedIdentity(MARY.getId(),
                                                                                                  UserInfo.TYPE_NAME))));
    assertSame(failure, error.getCause());
  }

  @Test
  void updateParticipantsReportsAFailedReadAsAStorageError() throws Exception {
    doReturn(MARY).when(service).userInfo(MARY.getId());
    PersistenceException failure = failOnFind(CallEntity.class);

    StorageException error = assertThrows(StorageException.class,
                                          () -> service.updateParticipants(CALL_ID, List.of(MARY.getId())));
    assertSame(failure, error.getCause());
  }

  @Test
  void startCallReportsAFailedReadAsAnInvalidCall() throws Exception {
    doReturn(newCall()).when(service).getCall(CALL_ID);
    doReturn(JOHN.getId()).when(service).currentUserId();
    PersistenceException failure = failOnFind(CallEntity.class);

    InvalidCallException error = assertThrows(InvalidCallException.class, () -> service.startCall(CALL_ID, "client"));
    assertSame(failure, error.getCause().getCause());
  }

  @Test
  void stopCallReportsAFailedReadAsAnInvalidCall() throws Exception {
    doReturn(newCall()).when(service).getCall(CALL_ID);
    doReturn(JOHN.getId()).when(service).currentUserId();
    PersistenceException failure = failOnFind(CallEntity.class);

    InvalidCallException error = assertThrows(InvalidCallException.class, () -> service.stopCall(CALL_ID, false));
    assertSame(failure, error.getCause().getCause());
  }

  @Test
  void removeCallReportsAFailedReadAsAnInvalidCall() throws Exception {
    doReturn(newCall()).when(service).getCall(CALL_ID);
    doReturn(JOHN.getId()).when(service).currentUserId();
    PersistenceException failure = failOnFind(CallEntity.class);

    InvalidCallException error = assertThrows(InvalidCallException.class, () -> service.stopCall(CALL_ID, true));
    assertSame(failure, error.getCause().getCause());
  }

  @Test
  void joinCallReportsAFailedReadAsAnInvalidCall() throws Exception {
    CallInfo call = newCall();
    call.setState(CallState.STARTED);
    doReturn(call).when(service).getCall(CALL_ID);
    PersistenceException failure = failOnFind(ParticipantEntity.class);

    InvalidCallException error = assertThrows(InvalidCallException.class,
                                              () -> service.joinCall(CALL_ID, MARY.getId(), "client"));
    assertSame(failure, error.getCause().getCause());
  }

  @Test
  void leaveCallReportsAFailedReadAsAnInvalidCall() throws Exception {
    CallInfo call = newCall();
    call.setState(CallState.STARTED);
    call.addParticipant(new GuestInfo("guest@acme.com"));
    doReturn(call).when(service).getCall(CALL_ID);
    PersistenceException failure = failOnFind(ParticipantEntity.class);

    InvalidCallException error = assertThrows(InvalidCallException.class,
                                              () -> service.leaveCall(CALL_ID, "guest@acme.com", null));
    assertSame(failure, error.getCause().getCause());
  }

  @Test
  void updateCallReportsAFailedReadAsAStorageError() throws Exception {
    doReturn(newCall()).when(service).getCall(CALL_ID);
    PersistenceException failure = failOnFind(CallEntity.class);

    StorageException error = assertThrows(StorageException.class,
                                          () -> service.updateCall(CALL_ID,
                                                                   null,
                                                                   null,
                                                                   "John and Mary weekly",
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null,
                                                                   null));
    assertSame(failure, error.getCause());
  }

  /**
   * A group call created started saves its members as participants, reading
   * each one first.
   */
  @Test
  void createCallReportsAFailedReadAsAStorageError() throws Exception {
    WebConferencingService.RoomInfo room = service.new RoomInfo("room", "Weekly sync");
    room.addMember(new UserInfo(MARY.getId(), MARY.getFirstName(), MARY.getLastName()));
    CallInfo call = new CallInfo("g_room", "Weekly sync", room, PROVIDER);
    call.setState(CallState.STARTED);
    PersistenceException failure = failOnFind(ParticipantEntity.class);

    StorageException error = assertThrows(StorageException.class, () -> service.createCall(call));
    assertSame(failure, error.getCause());
  }

  /**
   * Reading a call whose owner no longer exists removes the call, and a failed
   * removal leaves the call unread instead of failing the read.
   */
  @Test
  void getCallOfAMissingOwnerToleratesAFailedRemoval() throws Exception {
    CallEntity entity = new CallEntity();
    entity.setId(CALL_ID);
    entity.setOwnerId(JOHN.getId());
    entity.setOwnerType(WebConferencingService.OWNER_TYPE_USER);
    entity.setProviderType(PROVIDER);
    when(entityManager.find(CallEntity.class, CALL_ID)).thenReturn(entity)
                                                       .thenThrow(new PersistenceException("Database unavailable"));
    doReturn(null).when(service).userInfo(JOHN.getId());

    assertNull(service.getCall(CALL_ID));
  }

  /**
   * A failure at commit is raised by the transaction the aspect ends, outside
   * the EntityManager proxy.
   */
  @Test
  void addParticipantReportsAFailedCommitAsAStorageError() throws Exception {
    doReturn(MARY).when(service).userInfo(MARY.getId());
    RollbackException failure = new RollbackException("Commit failed");
    doThrow(failure).when(transaction).commit();

    StorageException error = assertThrows(StorageException.class, () -> service.addParticipant(CALL_ID, MARY.getId()));
    assertSame(failure, error.getCause());
  }

  private CallInfo newCall() {
    CallInfo call = new CallInfo(CALL_ID, "John and Mary", JOHN, PROVIDER);
    call.addParticipant(new UserInfo(JOHN.getId(), JOHN.getFirstName(), JOHN.getLastName()));
    call.addParticipant(new UserInfo(MARY.getId(), MARY.getFirstName(), MARY.getLastName()));
    return call;
  }

  private <T> PersistenceException failOnFind(Class<T> entityType) {
    PersistenceException failure = new PersistenceException("Database unavailable");
    when(entityManager.find(eq(entityType), any())).thenThrow(failure);
    return failure;
  }

  @SuppressWarnings("unchecked")
  private <T> void returnOnQuery(String name, Class<T> type, List<T> result) {
    TypedQuery<T> query = mock(TypedQuery.class);
    when(entityManager.createNamedQuery(name, type)).thenReturn(query);
    when(query.setParameter("callId", CALL_ID)).thenReturn(query);
    when(query.getResultList()).thenReturn(result);
  }

}
