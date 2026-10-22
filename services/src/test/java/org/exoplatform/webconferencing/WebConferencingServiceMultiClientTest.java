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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.webconferencing.dao.ParticipantDAO;

/**
 * A user in a call from several devices or browser tabs leaves it only from the
 * last of them: leaving from one of them neither shows the user as gone nor
 * stops the call for the others.
 */
class WebConferencingServiceMultiClientTest {

  private static final String    CALL_ID = "pexip-call-1";

  private static final String    CALLER  = "root";

  private static final String    CALLEE  = "john";

  private static final String    OTHER   = "mary";

  private static final String    PHONE   = "phone-uuid";

  private static final String    LAPTOP  = "laptop-uuid";

  private WebConferencingService service;

  private List<String>           leavedEvents;

  @BeforeEach
  void setUp() throws Exception {
    InitParams initParams = mock(InitParams.class);
    PropertiesParam jwtParams = new PropertiesParam();
    jwtParams.setProperty(WebConferencingService.SECRET_KEY, "secret");
    when(initParams.getPropertiesParam(WebConferencingService.JWT_CONFIGURATION_PROPERTIES)).thenReturn(jwtParams);
    service = spy(new WebConferencingService(null,
                                             null,
                                             mock(ListenerService.class),
                                             null,
                                             null,
                                             mock(ParticipantDAO.class),
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
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));
    doNothing().when(service).stopCall(any(CallInfo.class), anyString(), anyBoolean());
  }

  @Test
  void leavingAP2PCallFromOneDeviceKeepsTheUserInItFromTheOther() throws Exception {
    CallInfo call = startedCall(new UserInfo(CALLER, "Root", "Root"),
                                participant(CALLER, UserState.JOINED, "caller-client"),
                                participant(CALLEE, UserState.JOINED, PHONE + "," + LAPTOP));

    service.leaveCall(CALL_ID, CALLEE, PHONE);

    UserInfo callee = participantOf(call, CALLEE);
    assertEquals(UserState.JOINED, callee.getState());
    assertEquals(LAPTOP, callee.getClientId());
    verify(service).updateParticipant(CALL_ID, callee);
    verify(service, never()).stopCall(any(CallInfo.class), anyString(), anyBoolean());
    assertTrue(leavedEvents.isEmpty());
  }

  @Test
  void leavingAP2PCallFromTheLastDeviceEndsItAsBefore() throws Exception {
    CallInfo call = startedCall(new UserInfo(CALLER, "Root", "Root"),
                                participant(CALLER, UserState.JOINED, "caller-client"),
                                participant(CALLEE, UserState.JOINED, LAPTOP));

    service.leaveCall(CALL_ID, CALLEE, LAPTOP);

    UserInfo callee = participantOf(call, CALLEE);
    assertEquals(UserState.LEAVED, callee.getState());
    assertNull(callee.getClientId());
    verify(service).stopCall(call, CALLEE, true);
    assertEquals(List.of(CALLEE + " <- " + CALLER, CALLEE + " <- " + CALLEE), leavedEvents);
  }

  @Test
  void leavingAGroupCallFromOneDeviceKeepsItGoingForTheLastJoinedUser() throws Exception {
    CallInfo call = startedCall(space(),
                                participant(CALLER, UserState.LEAVED, null),
                                participant(CALLEE, UserState.JOINED, PHONE + "," + LAPTOP),
                                participant(OTHER, UserState.LEAVED, null));

    service.leaveCall(CALL_ID, CALLEE, LAPTOP);

    assertEquals(UserState.JOINED, participantOf(call, CALLEE).getState());
    assertEquals(PHONE, participantOf(call, CALLEE).getClientId());
    verify(service, never()).stopCall(any(CallInfo.class), anyString(), anyBoolean());
    assertTrue(leavedEvents.isEmpty());
  }

  /**
   * A window closed before entering the meeting, or a page that never joined, leaves from a client the user is
   * not in the call from.
   */
  @Test
  void aLeaveFromAClientNotInTheCallChangesNothingWhileTheUserIsInItFromAnother() throws Exception {
    CallInfo call = startedCall(new UserInfo(CALLER, "Root", "Root"),
                                participant(CALLER, UserState.JOINED, "caller-client"),
                                participant(CALLEE, UserState.JOINED, LAPTOP));

    service.leaveCall(CALL_ID, CALLEE, "unjoined-tab");

    assertEquals(UserState.JOINED, participantOf(call, CALLEE).getState());
    assertEquals(LAPTOP, participantOf(call, CALLEE).getClientId());
    verify(service, never()).updateParticipant(anyString(), any(UserInfo.class));
    verify(service, never()).stopCall(any(CallInfo.class), anyString(), anyBoolean());
    assertTrue(leavedEvents.isEmpty());
  }

  /** A provider's server ending its meeting leaves each user from all their clients. */
  @Test
  void aLeaveWithNoClientLeavesFromAllTheClients() throws Exception {
    CallInfo call = startedCall(new UserInfo(CALLER, "Root", "Root"),
                                participant(CALLER, UserState.JOINED, "caller-client"),
                                participant(CALLEE, UserState.JOINED, PHONE + "," + LAPTOP));

    service.leaveCall(CALL_ID, CALLEE, null);

    UserInfo callee = participantOf(call, CALLEE);
    assertEquals(UserState.LEAVED, callee.getState());
    assertNull(callee.getClientId());
    verify(service).stopCall(call, CALLEE, true);
  }

  @Test
  void aLeaveOfAUserInTheCallFromNoClientLeavesAsBefore() throws Exception {
    CallInfo call = startedCall(space(),
                                participant(CALLER, UserState.JOINED, null),
                                participant(CALLEE, UserState.INVITED, null),
                                participant(OTHER, UserState.JOINED, null));

    service.leaveCall(CALL_ID, CALLEE, "unjoined-tab");

    assertEquals(UserState.LEAVED, participantOf(call, CALLEE).getState());
    verify(service).updateParticipant(CALL_ID, participantOf(call, CALLEE));
  }

  @Test
  void joiningFromAnotherDeviceAddsItsClient() throws Exception {
    CallInfo call = startedCall(space(),
                                participant(CALLER, UserState.JOINED, "caller-client"),
                                participant(CALLEE, UserState.JOINED, PHONE));

    service.joinCall(CALL_ID, CALLEE, LAPTOP);

    assertEquals(UserState.JOINED, participantOf(call, CALLEE).getState());
    assertEquals(PHONE + "," + LAPTOP, participantOf(call, CALLEE).getClientId());
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

  private UserInfo participant(String id, String state, String clientIds) {
    UserInfo participant = new UserInfo(id, id, id);
    participant.setState(state);
    participant.setClientId(clientIds);
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

  private UserCallListener recordingListener(String userId) {
    return new UserCallListener(userId, "listener-client") {
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
