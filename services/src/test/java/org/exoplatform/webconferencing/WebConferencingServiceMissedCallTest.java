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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.webconferencing.dao.ParticipantDAO;
import org.exoplatform.webconferencing.dao.StorageException;

/**
 * A participant alerted of a started call who neither joins nor declines it
 * before the incoming call alert times out missed the call: the call start
 * marks the alerted participants invited and schedules the check, and the check
 * marks each still invited participant missed, broadcasting the missed call
 * once per participant.
 */
class WebConferencingServiceMissedCallTest {

  private static final String    CALL_ID   = "pexip-call-1";

  private static final String    CALLER    = "root";

  private static final String    CALLEE    = "john";

  private static final String    OTHER     = "mary";

  private static final String    CLIENT_ID = "client-1";

  private WebConferencingService service;

  private ListenerService        listenerService;

  private ParticipantDAO         participantsStorage;

  @BeforeEach
  void setUp() {
    service = newService(null);
  }

  @AfterEach
  void tearDown() {
    service.stop();
  }

  @Test
  void anAlertedCallStartInvitesTheOtherParticipantsAndSchedulesTheCheck() throws Exception {
    CallInfo call = call(space(),
                         participant(CALLER, UserState.LEAVED),
                         participant(CALLEE, UserState.LEAVED),
                         participant(OTHER, UserState.DECLINED));
    stubStorage();

    service.startCall(call, CALLER, CLIENT_ID, true);

    assertEquals(UserState.JOINED, stateOf(call, CALLER));
    assertEquals(UserState.INVITED, stateOf(call, CALLEE));
    assertEquals(UserState.INVITED, stateOf(call, OTHER));
    verify(service).scheduleMissedCall(CALL_ID, CALLER, call.getLastDate());
  }

  @Test
  void aCreatedAndStartedUserCallInvitesThePeerAndSchedulesTheCheck() throws Exception {
    UserInfo caller = participant(CALLER, UserState.LEAVED);
    UserInfo callee = participant(CALLEE, UserState.LEAVED);
    stubCallCreation();
    doReturn(new UserInfo(CALLER, "Root", "Root")).when(service)
                                                  .createOwner(CALLER, WebConferencingService.OWNER_TYPE_USER, "Call", true, false, false, false);
    doReturn(new LinkedHashSet<>(List.of(caller, callee))).when(service).createParticipants("pexip", List.of(CALLER, CALLEE));
    doNothing().when(service).notifyUserCallStateChanged(any(CallInfo.class), anyString(), anyString());

    CallInfo call = createStartedCall(WebConferencingService.OWNER_TYPE_USER, CALLER);

    assertEquals(UserState.INVITED, callee.getState());
    assertEquals(UserState.LEAVED, caller.getState());
    verify(service).scheduleMissedCall(CALL_ID, CALLER, call.getLastDate());
  }

  @Test
  void aCreatedAndStartedSpaceCallInvitesTheMembersButTheCaller() throws Exception {
    UserInfo caller = participant(CALLER, null);
    UserInfo callee = participant(CALLEE, null);
    WebConferencingService.SpaceInfo space = groupOwner(WebConferencingService.SpaceInfo.class,
                                                        WebConferencingService.OWNER_TYPE_SPACE,
                                                        caller,
                                                        callee);
    stubCallCreation();
    doReturn(space).when(service).spaceInfo("space1", CALL_ID);

    CallInfo call = createStartedCall(WebConferencingService.OWNER_TYPE_SPACE, "space1");

    assertEquals(UserState.INVITED, callee.getState());
    assertEquals(UserState.LEAVED, caller.getState());
    verify(service).scheduleMissedCall(CALL_ID, CALLER, call.getLastDate());
  }

  @Test
  void aCreatedAndStartedPlannedEventCallInvitesNobody() throws Exception {
    UserInfo callee = participant(CALLEE, null);
    WebConferencingService.SpaceEventInfo event = groupOwner(WebConferencingService.SpaceEventInfo.class,
                                                             WebConferencingService.OWNER_TYPE_SPACEEVENT,
                                                             participant(CALLER, null),
                                                             callee);
    stubCallCreation();
    doReturn(event).when(service).spaceEventInfo(eq("space1"), eq(CALL_ID), any(String[].class), any(String[].class));

    createStartedCall(WebConferencingService.OWNER_TYPE_SPACEEVENT, "space1");

    assertEquals(UserState.LEAVED, callee.getState());
    verify(service, never()).scheduleMissedCall(anyString(), anyString(), any(Date.class));
  }

  @Test
  void aCallStartedWithoutAlertingInvitesNobody() throws Exception {
    CallInfo call = call(space(), participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.LEAVED));
    stubStorage();

    service.startCall(call, CALLER, CLIENT_ID, false);

    assertEquals(UserState.LEAVED, stateOf(call, CALLEE));
    verify(service, never()).scheduleMissedCall(anyString(), anyString(), any(Date.class));
  }

  @Test
  void aPlannedEventCallStartInvitesNobody() throws Exception {
    CallInfo call = call(group(WebConferencingService.OWNER_TYPE_SPACEEVENT),
                         participant(CALLER, UserState.LEAVED),
                         participant(CALLEE, UserState.LEAVED));
    stubStorage();

    service.startCall(call, CALLER, CLIENT_ID, true);

    assertEquals(UserState.LEAVED, stateOf(call, CALLEE));
    verify(service, never()).scheduleMissedCall(anyString(), anyString(), any(Date.class));
  }

  @Test
  void aGuestIsNeverInvited() throws Exception {
    UserInfo guest = new GuestInfo("guest-1", "Guest", "One");
    guest.setState(UserState.LEAVED);
    CallInfo call = call(space(), participant(CALLER, UserState.LEAVED), guest);
    stubStorage();

    service.startCall(call, CALLER, CLIENT_ID, true);

    assertEquals(UserState.LEAVED, guest.getState());
  }

  @Test
  void theCheckRunsOnceTheIncomingCallAlertTimedOut() {
    Date startedAt = new Date();
    doReturn(1000L).when(service).getMissedCallCheckDelay();
    doNothing().when(service).checkMissedCall(anyString(), anyString(), any(Date.class));

    service.scheduleMissedCall(CALL_ID, CALLER, startedAt);

    verify(service, never()).checkMissedCall(anyString(), anyString(), any(Date.class));
    verify(service, timeout(3000)).checkMissedCall(CALL_ID, CALLER, startedAt);
  }

  @Test
  void aStillInvitedParticipantMissedTheCall() throws Exception {
    Date startedAt = new Date();
    CallInfo call = startedCall(startedAt,
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.INVITED),
                                participant(OTHER, UserState.JOINED));
    when(participantsStorage.markParticipantMissed(CALL_ID, CALLEE, latestStart(startedAt))).thenReturn(1);

    service.notifyMissedCall(CALL_ID, CALLER, startedAt);

    assertEquals(UserState.MISSED, stateOf(call, CALLEE));
    verify(listenerService).broadcast(WebConferencingService.EVENT_CALL_MISSED,
                                      call,
                                      Map.of(WebConferencingService.EVENT_DATA_USER_ID,
                                             CALLEE,
                                             WebConferencingService.EVENT_DATA_CALLER_ID,
                                             CALLER,
                                             "status",
                                             WebConferencingService.STATUS_OK));
    verify(participantsStorage, never()).markParticipantMissed(eq(CALL_ID), eq(OTHER), any(Date.class));
  }

  @Test
  void aParticipantUpdatedMeanwhileIsNotNotified() throws Exception {
    Date startedAt = new Date();
    CallInfo call = startedCall(startedAt, participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.INVITED));
    when(participantsStorage.markParticipantMissed(CALL_ID, CALLEE, latestStart(startedAt))).thenReturn(0);

    service.notifyMissedCall(CALL_ID, CALLER, startedAt);

    assertEquals(UserState.INVITED, stateOf(call, CALLEE));
    verifyNoMissedCallBroadcast();
  }

  @Test
  void aParticipantWhoAnsweredOrDeclinedIsNotNotified() throws Exception {
    Date startedAt = new Date();
    startedCall(startedAt,
                participant(CALLER, UserState.LEAVED),
                participant(CALLEE, UserState.DECLINED),
                participant(OTHER, UserState.LEAVED));

    service.notifyMissedCall(CALL_ID, CALLER, startedAt);

    verify(participantsStorage, never()).markParticipantMissed(anyString(), anyString(), any(Date.class));
    verifyNoMissedCallBroadcast();
  }

  @Test
  void nobodyIsNotifiedOfACallStoppedMeanwhile() throws Exception {
    Date startedAt = new Date();
    CallInfo call = startedCall(startedAt, participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.INVITED));
    call.setState(CallState.STOPPED);

    service.notifyMissedCall(CALL_ID, CALLER, startedAt);

    verify(participantsStorage, never()).markParticipantMissed(anyString(), anyString(), any(Date.class));
    verifyNoMissedCallBroadcast();
  }

  @Test
  void aStartDateReadRoundedUpFromTheStorageIsTheSameStart() throws Exception {
    Date startedAt = new Date(1_700_000_000_600L);
    CallInfo call = startedCall(new Date(1_700_000_001_000L),
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.INVITED));
    when(participantsStorage.markParticipantMissed(CALL_ID, CALLEE, latestStart(startedAt))).thenReturn(1);

    service.notifyMissedCall(CALL_ID, CALLER, startedAt);

    assertEquals(UserState.MISSED, stateOf(call, CALLEE));
  }

  @Test
  void aParticipantWhoseMarkFailsIsNotNotified() throws Exception {
    Date startedAt = new Date();
    CallInfo call = startedCall(startedAt, participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.INVITED));
    when(participantsStorage.markParticipantMissed(CALL_ID, CALLEE, latestStart(startedAt))).thenThrow(new PersistenceException("down"));

    assertThrows(StorageException.class, () -> service.notifyMissedCall(CALL_ID, CALLER, startedAt));

    assertEquals(UserState.INVITED, stateOf(call, CALLEE));
    verifyNoMissedCallBroadcast();
  }

  @Test
  void nobodyIsNotifiedOfACallRemovedMeanwhile() throws Exception {
    doReturn(null).when(service).getCall(CALL_ID);

    service.notifyMissedCall(CALL_ID, CALLER, new Date());

    verifyNoMissedCallBroadcast();
  }

  @Test
  void theCheckOfAnEarlierStartOfACallStartedAgainDoesNothing() throws Exception {
    Date startedAt = new Date(System.currentTimeMillis() - 30000);
    startedCall(new Date(), participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.INVITED));

    service.notifyMissedCall(CALL_ID, CALLER, startedAt);

    verify(participantsStorage, never()).markParticipantMissed(anyString(), anyString(), any(Date.class));
    verifyNoMissedCallBroadcast();
  }

  @Test
  void aGroupCallStopsWhenItsLastJoinedParticipantLeavesAndTheOthersWereInvitedOrMissedIt() throws Exception {
    CallInfo call = startedCall(new Date(),
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.INVITED),
                                participant(OTHER, UserState.MISSED));
    doNothing().when(service).updateParticipant(anyString(), any(UserInfo.class));
    doNothing().when(service).stopCall(any(CallInfo.class), anyString(), anyBoolean());

    service.leaveCall(CALL_ID, CALLER, CLIENT_ID);

    verify(service).stopCall(call, CALLER, false);
  }

  @Test
  void theIncomingCallTimeoutIsConfigurable() {
    assertEquals(WebConferencingService.DEFAULT_INCOMING_CALL_TIMEOUT, service.getIncomingCallTimeout());
    assertEquals(120, timeoutOf("120"));
    assertEquals(WebConferencingService.DEFAULT_INCOMING_CALL_TIMEOUT, timeoutOf("not-a-number"));
    assertEquals(WebConferencingService.DEFAULT_INCOMING_CALL_TIMEOUT, timeoutOf("0"));
    assertEquals(WebConferencingService.MAX_INCOMING_CALL_TIMEOUT,
                 timeoutOf(String.valueOf(WebConferencingService.MAX_INCOMING_CALL_TIMEOUT)));
    assertEquals(WebConferencingService.DEFAULT_INCOMING_CALL_TIMEOUT,
                 timeoutOf(String.valueOf(WebConferencingService.MAX_INCOMING_CALL_TIMEOUT + 1)));
  }

  @Test
  void theCheckRunsAMarginAfterTheIncomingCallTimeout() {
    assertEquals((WebConferencingService.DEFAULT_INCOMING_CALL_TIMEOUT + WebConferencingService.MISSED_CALL_CHECK_MARGIN) * 1000,
                 service.getMissedCallCheckDelay());
  }

  @Test
  void stoppingTheServiceStopsTheMissedCallTimer() {
    service.stop();

    assertTrue(service.missedCallTimer.isShutdown());
  }

  private Date latestStart(Date startedAt) {
    return new Date(startedAt.getTime() + WebConferencingService.LAST_DATE_PRECISION);
  }

  private long timeoutOf(String value) {
    WebConferencingService other = newService(value);
    try {
      return other.getIncomingCallTimeout();
    } finally {
      other.stop();
    }
  }

  private WebConferencingService newService(String timeout) {
    InitParams initParams = mock(InitParams.class);
    PropertiesParam jwtParams = new PropertiesParam();
    jwtParams.setProperty(WebConferencingService.SECRET_KEY, "secret");
    when(initParams.getPropertiesParam(WebConferencingService.JWT_CONFIGURATION_PROPERTIES)).thenReturn(jwtParams);
    if (timeout != null) {
      ValueParam timeoutParam = new ValueParam();
      timeoutParam.setValue(timeout);
      when(initParams.getValueParam(WebConferencingService.INCOMING_CALL_TIMEOUT)).thenReturn(timeoutParam);
    }
    listenerService = mock(ListenerService.class);
    participantsStorage = mock(ParticipantDAO.class);
    return spy(new WebConferencingService(null,
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
  }

  private void stubCallCreation() throws Exception {
    doReturn(CALLER).when(service).currentUserId();
    doReturn(null).when(service).findGroupCallId(anyString());
    doNothing().when(service).invalidateCall(anyString(), anyBoolean());
    doReturn(new LinkedHashSet<>()).when(service).createOrigins(anyString(), any(), any());
    doNothing().when(service).createCall(any(CallInfo.class));
    doNothing().when(service).scheduleMissedCall(anyString(), anyString(), any(Date.class));
  }

  private CallInfo createStartedCall(String ownerType, String ownerId) throws Exception {
    return service.createCall(CALL_ID, ownerId, ownerType, "Call", "pexip", List.of(CALLER, CALLEE), List.of(), true, null, null);
  }

  private <T extends GroupInfo> T groupOwner(Class<T> type, String ownerType, UserInfo... members) {
    T group = mock(type);
    Map<String, UserInfo> byId = new LinkedHashMap<>();
    for (UserInfo member : members) {
      byId.put(member.getId(), member);
    }
    when(group.getMembers()).thenReturn(byId);
    when(group.getType()).thenReturn(ownerType);
    when(group.isGroup()).thenReturn(true);
    when(group.getId()).thenReturn("space1");
    when(group.getTitle()).thenReturn("Space 1");
    return group;
  }

  private void stubStorage() throws Exception {
    doNothing().when(service).syncMembersAndParticipants(any(CallInfo.class));
    doNothing().when(service).updateCallAndParticipants(any(CallInfo.class));
    doNothing().when(service).scheduleMissedCall(anyString(), anyString(), any(Date.class));
  }

  @SuppressWarnings("unchecked")
  private void verifyNoMissedCallBroadcast() throws Exception {
    verify(listenerService, never()).broadcast(eq(WebConferencingService.EVENT_CALL_MISSED), any(CallInfo.class), anyMap());
  }

  private CallInfo startedCall(Date lastDate, UserInfo... participants) throws Exception {
    CallInfo call = call(space(), participants);
    call.setState(CallState.STARTED);
    call.setLastDate(lastDate);
    doReturn(call).when(service).getCall(CALL_ID);
    return call;
  }

  private CallInfo call(IdentityInfo owner, UserInfo... participants) {
    CallInfo call = new CallInfo(CALL_ID, "Call", owner, "pexip");
    for (UserInfo participant : participants) {
      call.addParticipant(participant);
    }
    return call;
  }

  private UserInfo participant(String id, String state) {
    UserInfo participant = new UserInfo(id, id, id);
    participant.setState(state);
    return participant;
  }

  private GroupInfo space() {
    return group(WebConferencingService.OWNER_TYPE_SPACE);
  }

  private GroupInfo group(String type) {
    return new GroupInfo("space1", "Space 1") {
      @Override
      public String getType() {
        return type;
      }
    };
  }

  private String stateOf(CallInfo call, String id) {
    return call.getParticipants().stream().filter(part -> part.getId().equals(id)).findFirst().orElseThrow().getState();
  }
}
