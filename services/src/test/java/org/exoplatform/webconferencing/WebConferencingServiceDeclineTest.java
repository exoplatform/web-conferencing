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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.webconferencing.dao.ParticipantDAO;

/**
 * A declined call goes on for its other participants, a P2P one included, and
 * the decline reaches only the decliner's own clients, to close the incoming
 * call alert on each of them.
 */
class WebConferencingServiceDeclineTest {

  private static final String    CALL_ID   = "pexip-call-1";

  private static final String    CALLER    = "root";

  private static final String    CALLEE    = "john";

  private static final String    OTHER     = "mary";

  private static final String    CLIENT_ID = "client-1";

  private WebConferencingService service;

  private List<String>           leavedEvents;

  private ListenerService        listenerService;

  private ParticipantDAO         participantsStorage;

  @BeforeEach
  void setUp() throws Exception {
    InitParams initParams = mock(InitParams.class);
    PropertiesParam jwtParams = new PropertiesParam();
    jwtParams.setProperty(WebConferencingService.SECRET_KEY, "secret");
    when(initParams.getPropertiesParam(WebConferencingService.JWT_CONFIGURATION_PROPERTIES)).thenReturn(jwtParams);
    listenerService = mock(ListenerService.class);
    participantsStorage = mock(ParticipantDAO.class);
    service = spy(new WebConferencingService(null,
                                             null,
                                             listenerService,
                                             null,
                                             null,
                                             participantsStorage,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             initParams,
                                             null,
                                             mock(CodecInitializer.class)));
    leavedEvents = new ArrayList<>();
    service.addUserCallListener(recordingListener(CALLER));
    service.addUserCallListener(recordingListener(CALLEE));
    service.addUserCallListener(recordingListener(OTHER));
  }

  @Test
  void declineKeepsAP2PCallStartedForTheCaller() throws Exception {
    CallInfo call = startedCall(new UserInfo(CALLER, "Root", "Root"),
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.LEAVED));
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(UserState.DECLINED, stateOf(call, CALLEE));
    assertEquals(UserState.JOINED, stateOf(call, CALLER));
    assertEquals(CallState.STARTED, call.getState());
    verify(service).updateParticipant(CALL_ID, participantOf(call, CALLEE));
    verify(service, never()).stopCall(any(CallInfo.class), anyString(), anyBoolean());
  }

  @Test
  void declineKeepsAGroupCallStarted() throws Exception {
    CallInfo call = startedCall(space(),
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.LEAVED),
                                participant(OTHER, UserState.LEAVED));
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(UserState.DECLINED, stateOf(call, CALLEE));
    assertEquals(UserState.LEAVED, stateOf(call, OTHER));
    assertEquals(CallState.STARTED, call.getState());
    verify(service, never()).stopCall(any(CallInfo.class), anyString(), anyBoolean());
  }

  @Test
  void declineIsSentToTheDeclinerClientsOnly() throws Exception {
    startedCall(space(),
                participant(CALLER, UserState.JOINED),
                participant(CALLEE, UserState.LEAVED),
                participant(OTHER, UserState.LEAVED));
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(List.of(CALLEE + " <- " + CALLEE), leavedEvents);
  }

  @Test
  void declineByANonParticipantIsRefused() throws Exception {
    CallInfo call = startedCall(new UserInfo(CALLER, "Root", "Root"),
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.LEAVED));

    assertThrows(ParticipantNotFoundException.class, () -> service.declineCall(CALL_ID, OTHER, CLIENT_ID));

    assertEquals(UserState.LEAVED, stateOf(call, CALLEE));
    verify(service, never()).updateParticipant(anyString(), any(UserInfo.class));
    assertTrue(leavedEvents.isEmpty());
  }

  @Test
  void declineOfAnUnknownCallIsRefused() throws Exception {
    doReturn(null).when(service).getCall(CALL_ID);

    assertThrows(CallNotFoundException.class, () -> service.declineCall(CALL_ID, CALLEE, CLIENT_ID));
  }

  @Test
  void declineAfterAJoinFromAnotherClientKeepsTheUserJoined() throws Exception {
    CallInfo call = startedCall(new UserInfo(CALLER, "Root", "Root"),
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.JOINED));
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(UserState.JOINED, stateOf(call, CALLEE));
    verify(service, never()).updateParticipant(anyString(), any(UserInfo.class));
    assertTrue(leavedEvents.isEmpty());
  }

  @Test
  void declineOfACallNoLongerStartedChangesNothing() throws Exception {
    CallInfo call = startedCall(new UserInfo(CALLER, "Root", "Root"),
                                participant(CALLER, UserState.LEAVED),
                                participant(CALLEE, UserState.LEAVED));
    call.setState(CallState.STOPPED);
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(UserState.LEAVED, stateOf(call, CALLEE));
    verify(service, never()).updateParticipant(anyString(), any(UserInfo.class));
    assertTrue(leavedEvents.isEmpty());
  }

  @Test
  void aUserWhoDeclinedCanStillJoinTheCall() throws Exception {
    CallInfo call = startedCall(space(),
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.DECLINED));
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.joinCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(UserState.JOINED, stateOf(call, CALLEE));
  }

  @Test
  void aGroupCallStopsWhenItsLastJoinedParticipantLeavesAndTheOthersDeclined() throws Exception {
    CallInfo call = startedCall(space(),
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.DECLINED),
                                participant(OTHER, UserState.DECLINED));
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));
    doNothing().when(service).stopCall(any(CallInfo.class), anyString(), anyBoolean());

    service.leaveCall(CALL_ID, CALLER, CLIENT_ID);

    verify(service).stopCall(call, CALLER, false);
  }

  @Test
  void aGroupCallGoesOnWhileAParticipantIsStillInIt() throws Exception {
    startedCall(space(),
                participant(CALLER, UserState.JOINED),
                participant(CALLEE, UserState.DECLINED),
                participant(OTHER, UserState.JOINED));
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.leaveCall(CALL_ID, CALLER, CLIENT_ID);

    verify(service, never()).stopCall(any(CallInfo.class), anyString(), anyBoolean());
  }

  @Test
  void aDeclinedP2PCallEndsWhenTheCallerLeavesIt() throws Exception {
    CallInfo call = startedCall(new UserInfo(CALLER, "Root", "Root"),
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.DECLINED));
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));
    doNothing().when(service).stopCall(any(CallInfo.class), anyString(), anyBoolean());

    service.leaveCall(CALL_ID, CALLER, CLIENT_ID);

    verify(service).stopCall(call, CALLER, true);
  }

  @Test
  void theFirstDeclineOfAnAlertedCallIsReportedToItsCaller() throws Exception {
    CallInfo call = startedCall(space(),
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.INVITED),
                                participant(OTHER, UserState.INVITED));
    call.setCallerId(CALLER);
    when(participantsStorage.markParticipantDeclined(CALL_ID, CALLEE)).thenReturn(1);

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(UserState.DECLINED, stateOf(call, CALLEE));
    verify(service, never()).updateParticipant(anyString(), any(UserInfo.class));
    InOrder order = inOrder(participantsStorage, listenerService);
    order.verify(participantsStorage).markParticipantDeclined(CALL_ID, CALLEE);
    order.verify(listenerService)
         .broadcast(WebConferencingService.EVENT_CALL_DECLINED,
                    call,
                    Map.of(WebConferencingService.EVENT_DATA_USER_ID,
                           CALLER,
                           WebConferencingService.EVENT_DATA_DECLINER_ID,
                           CALLEE,
                           "status",
                           WebConferencingService.STATUS_OK));
  }

  @Test
  void aDeclineFromAnotherClientOfAParticipantWhoDeclinedIsNotReportedAgain() throws Exception {
    CallInfo call = startedCall(space(), participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.DECLINED));
    call.setCallerId(CALLER);
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(UserState.DECLINED, stateOf(call, CALLEE));
    verify(service, never()).updateParticipant(anyString(), any(UserInfo.class));
    verifyNoDeclineBroadcast();
    assertEquals(List.of(CALLEE + " <- " + CALLEE), leavedEvents);
  }

  @Test
  void aDeclineFromAnotherClientKeepsTheDeclineTheCallerWasNotifiedOf() throws Exception {
    CallInfo call = startedCall(space(), participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.DECLINED_NOTIFIED));
    call.setCallerId(CALLER);
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(UserState.DECLINED_NOTIFIED, stateOf(call, CALLEE));
    verify(service, never()).updateParticipant(anyString(), any(UserInfo.class));
    verifyNoDeclineBroadcast();
  }

  @Test
  void aDeclineOfACallThatAlertedNobodyIsNotReported() throws Exception {
    CallInfo call = startedCall(space(), participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.LEAVED));
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(UserState.DECLINED, stateOf(call, CALLEE));
    verifyNoDeclineBroadcast();
  }

  @Test
  void aDeclineOfAJoinedParticipantIsNotReported() throws Exception {
    CallInfo call = startedCall(space(), participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.JOINED));
    call.setCallerId(CALLER);

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    verifyNoDeclineBroadcast();
  }

  @Test
  void aDeclineWhoseWriteFailsIsNotReported() throws Exception {
    CallInfo call = startedCall(space(), participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.INVITED));
    call.setCallerId(CALLER);
    when(participantsStorage.markParticipantDeclined(CALL_ID, CALLEE)).thenThrow(new PersistenceException("down"));

    assertThrows(InvalidCallException.class, () -> service.declineCall(CALL_ID, CALLEE, CLIENT_ID));

    verifyNoDeclineBroadcast();
  }

  @Test
  void aDeclineMarkedFirstByAnotherWriterIsNotReported() throws Exception {
    CallInfo call = startedCall(space(), participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.INVITED));
    call.setCallerId(CALLER);
    when(participantsStorage.markParticipantDeclined(CALL_ID, CALLEE)).thenReturn(0);

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(UserState.INVITED, stateOf(call, CALLEE));
    verifyNoDeclineBroadcast();
    assertEquals(List.of(CALLEE + " <- " + CALLEE), leavedEvents);
  }

  @Test
  void aDeclineOfACallThatAlertedNobodyIsWrittenAsBefore() throws Exception {
    CallInfo call = startedCall(space(), participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.LEAVED));
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    verify(service).updateParticipant(CALL_ID, participantOf(call, CALLEE));
    verify(participantsStorage, never()).markParticipantDeclined(anyString(), anyString());
  }

  @Test
  void aDeclineOfAParticipantWhoMissedTheCallChangesNothingButTheAlert() throws Exception {
    CallInfo call = startedCall(space(), participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.MISSED));
    call.setCallerId(CALLER);
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(UserState.MISSED, stateOf(call, CALLEE));
    verify(service, never()).updateParticipant(anyString(), any(UserInfo.class));
    verifyNoDeclineBroadcast();
    assertEquals(List.of(CALLEE + " <- " + CALLEE), leavedEvents);
  }

  @Test
  void aDeclineOfAParticipantWhoLeftTheCallChangesNothingButTheAlert() throws Exception {
    CallInfo call = startedCall(space(), participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.LEAVED));
    call.setCallerId(CALLER);
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));

    service.declineCall(CALL_ID, CALLEE, CLIENT_ID);

    assertEquals(UserState.LEAVED, stateOf(call, CALLEE));
    verify(service, never()).updateParticipant(anyString(), any(UserInfo.class));
    verifyNoDeclineBroadcast();
  }

  @Test
  void aParticipantWhoDeclinedIsNotInTheCall() {
    assertTrue(service.isGone(participant(CALLEE, UserState.DECLINED)));
    assertTrue(service.isGone(participant(CALLEE, UserState.DECLINED_NOTIFIED)));
    assertFalse(service.isGone(participant(CALLEE, UserState.JOINED)));
  }

  @SuppressWarnings("unchecked")
  private void verifyNoDeclineBroadcast() throws Exception {
    verify(listenerService, never()).broadcast(eq(WebConferencingService.EVENT_CALL_DECLINED), any(CallInfo.class), anyMap());
  }

  private CallInfo startedCall(IdentityInfo owner, UserInfo... participants) throws Exception {
    CallInfo call = new CallInfo(CALL_ID, "Call", owner, "pexip");
    call.setState(CallState.STARTED);
    for (UserInfo participant : participants) {
      call.addParticipant(participant);
    }
    doReturn(call).when(service).getCall(CALL_ID);
    return call;
  }

  private UserInfo participant(String id, String state) {
    UserInfo participant = new UserInfo(id, id, id);
    participant.setState(state);
    return participant;
  }

  private GroupInfo space() {
    return new GroupInfo("space1", "Space 1") {
      @Override
      public String getType() {
        return WebConferencingService.OWNER_TYPE_SPACE;
      }
    };
  }

  private UserInfo participantOf(CallInfo call, String id) {
    return call.getParticipants().stream().filter(part -> part.getId().equals(id)).findFirst().orElseThrow();
  }

  private String stateOf(CallInfo call, String id) {
    return participantOf(call, id).getState();
  }

  private UserCallListener recordingListener(String userId) {
    return new UserCallListener(userId, CLIENT_ID) {
      @Override
      public void onCallStateChanged(String callId, String providerType, String callState, String ownerId, String ownerType) {
        // not observed here
      }

      @Override
      public void onPartJoined(String callId, String providerType, String ownerId, String ownerType, String partId) {
        // not observed here
      }

      @Override
      public void onPartLeaved(String callId, String providerType, String ownerId, String ownerType, String partId) {
        leavedEvents.add(partId + " <- " + getUserId());
      }
    };
  }
}
