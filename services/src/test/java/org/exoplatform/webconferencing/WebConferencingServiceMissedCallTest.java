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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
import org.mockito.InOrder;
import org.mockito.MockedStatic;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.webconferencing.dao.ParticipantDAO;
import org.exoplatform.webconferencing.dao.StorageException;
import org.exoplatform.webconferencing.domain.CallEntity;

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
    assertEquals(CALLER, call.getCallerId());
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
    assertEquals(CALLER, call.getCallerId());
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
    assertEquals(CALLER, call.getCallerId());
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

    CallInfo call = createStartedCall(WebConferencingService.OWNER_TYPE_SPACEEVENT, "space1");

    assertEquals(UserState.LEAVED, callee.getState());
    assertNull(call.getCallerId());
    verify(service, never()).scheduleMissedCall(anyString(), anyString(), any(Date.class));
  }

  @Test
  void aCallStartedWithoutAlertingInvitesNobody() throws Exception {
    CallInfo call = call(space(), participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.LEAVED));
    call.setCallerId(OTHER);
    stubStorage();

    service.startCall(call, CALLER, CLIENT_ID, false);

    assertEquals(UserState.LEAVED, stateOf(call, CALLEE));
    assertNull(call.getCallerId());
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
  void aParticipantWhoseMarkFailsIsNotNotifiedAndTheOthersAre() throws Exception {
    Date startedAt = new Date();
    CallInfo call = startedCall(startedAt,
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.INVITED),
                                participant(OTHER, UserState.INVITED));
    when(participantsStorage.markParticipantMissed(CALL_ID, CALLEE, latestStart(startedAt))).thenThrow(new PersistenceException("down"));
    when(participantsStorage.markParticipantMissed(CALL_ID, OTHER, latestStart(startedAt))).thenReturn(1);

    service.notifyMissedCall(CALL_ID, CALLER, startedAt);

    assertEquals(UserState.INVITED, stateOf(call, CALLEE));
    assertEquals(UserState.MISSED, stateOf(call, OTHER));
    verify(listenerService, never()).broadcast(eq(WebConferencingService.EVENT_CALL_MISSED),
                                               eq(call),
                                               argThat((Map<String, String> info) -> CALLEE.equals(info.get(WebConferencingService.EVENT_DATA_USER_ID))));
    verify(listenerService).broadcast(eq(WebConferencingService.EVENT_CALL_MISSED),
                                      eq(call),
                                      argThat((Map<String, String> info) -> OTHER.equals(info.get(WebConferencingService.EVENT_DATA_USER_ID))));
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
  void aStoppedCallNotifiesItsStillInvitedParticipantsOutOfTheRequest() throws Exception {
    CallInfo call = startedCall(new Date(),
                                participant(CALLER, UserState.LEAVED),
                                participant(CALLEE, UserState.INVITED),
                                participant(OTHER, UserState.JOINED));
    call.setCallerId(CALLER);
    stubStop();

    service.stopCall(call, CALLER, false);

    verify(service).scheduleStoppedMissedCall(call, List.of(CALLEE), false);
  }

  @Test
  void aRemovedUserCallMarksItsStillInvitedPeerBeforeTheRemovalAndNotifiesIt() throws Exception {
    CallInfo call = removedUserCall();
    when(participantsStorage.markParticipantMissed(CALL_ID, CALLEE, latestStart(call.getLastDate()))).thenReturn(1);
    stubStop();

    service.stopCall(call, CALLER, true);

    InOrder order = inOrder(participantsStorage, service);
    order.verify(participantsStorage).markParticipantMissed(CALL_ID, CALLEE, latestStart(call.getLastDate()));
    order.verify(service).txStopCall(call, true);
    verify(service).scheduleStoppedMissedCall(call, List.of(CALLEE), true);
  }

  @Test
  void theMarkedPeerOfAUserCallIsNotifiedEvenWhenItsRemovalFails() throws Exception {
    CallInfo call = removedUserCall();
    when(participantsStorage.markParticipantMissed(CALL_ID, CALLEE, latestStart(call.getLastDate()))).thenReturn(1);
    stubStop();
    doThrow(new PersistenceException("down")).when(service).txStopCall(call, true);

    assertThrows(StorageException.class, () -> service.stopCall(call, CALLER, true));

    verify(service).scheduleStoppedMissedCall(call, List.of(CALLEE), true);
  }

  @Test
  void aGroupCallWhoseStopFailsNotifiesNobody() throws Exception {
    CallInfo call = startedCall(new Date(), participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.INVITED));
    call.setCallerId(CALLER);
    stubStop();
    doThrow(new PersistenceException("down")).when(service).txStopCall(call, false);

    assertThrows(StorageException.class, () -> service.stopCall(call, CALLER, false));

    verify(service, never()).scheduleStoppedMissedCall(any(CallInfo.class), any(), anyBoolean());
  }

  @Test
  void updatingACallKeepsItsCaller() throws Exception {
    CallInfo current = startedCall(new Date(), participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.INVITED));
    current.setCallerId(CALLER);
    doReturn(new LinkedHashSet<>()).when(service).createOrigins(anyString(), any(), any());
    doNothing().when(service).updateCallAndOrigins(any(CallInfo.class));

    CallInfo updated = service.updateCall(CALL_ID, null, null, "Renamed", null, List.of(), List.of(), null, null);

    assertEquals(CALLER, updated.getCallerId());
  }

  @Test
  void aRemovedUserCallWhosePeerTheCheckAlreadyMarkedNotifiesNobody() throws Exception {
    CallInfo call = removedUserCall();
    when(participantsStorage.markParticipantMissed(CALL_ID, CALLEE, latestStart(call.getLastDate()))).thenReturn(0);
    stubStop();

    service.stopCall(call, CALLER, true);

    verify(service, never()).scheduleStoppedMissedCall(any(CallInfo.class), any(), anyBoolean());
  }

  @Test
  void aCallStoppedWithNobodyInvitedNotifiesNobody() throws Exception {
    CallInfo call = startedCall(new Date(),
                                participant(CALLER, UserState.LEAVED),
                                participant(CALLEE, UserState.MISSED),
                                participant(OTHER, UserState.LEAVED));
    call.setCallerId(CALLER);
    stubStop();

    service.stopCall(call, CALLER, false);

    verify(service, never()).scheduleStoppedMissedCall(any(CallInfo.class), any(), anyBoolean());
  }

  @Test
  void aCallStartedBeforeTheCallerWasStoredNotifiesNobodyWhenStopped() throws Exception {
    CallInfo call = startedCall(new Date(), participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.INVITED));
    stubStop();

    service.stopCall(call, CALLER, false);

    verify(service, never()).scheduleStoppedMissedCall(any(CallInfo.class), any(), anyBoolean());
  }

  @Test
  void theParticipantsOfAStoppedCallAreNotifiedOnTheMissedCallTimer() throws Exception {
    CallInfo call = startedCall(new Date(), participant(CALLEE, UserState.INVITED));
    doNothing().when(service).checkStoppedMissedCall(any(CallInfo.class), any(), anyBoolean());

    service.scheduleStoppedMissedCall(call, List.of(CALLEE), false);

    verify(service, timeout(3000)).checkStoppedMissedCall(call, List.of(CALLEE), false);
  }

  @Test
  void aStillInvitedParticipantOfAStoppedGroupCallMissedACallThatEnded() throws Exception {
    Date startedAt = new Date();
    CallInfo call = startedCall(startedAt, participant(CALLEE, UserState.INVITED), participant(OTHER, UserState.INVITED));
    call.setCallerId(CALLER);
    when(participantsStorage.markParticipantMissedAtStop(CALL_ID, CALLEE, latestStart(startedAt))).thenReturn(1);
    when(participantsStorage.markParticipantMissedAtStop(CALL_ID, OTHER, latestStart(startedAt))).thenReturn(0);

    service.notifyStoppedMissedCall(call, List.of(CALLEE, OTHER), false);

    verify(listenerService).broadcast(WebConferencingService.EVENT_CALL_MISSED,
                                      call,
                                      Map.of(WebConferencingService.EVENT_DATA_USER_ID,
                                             CALLEE,
                                             WebConferencingService.EVENT_DATA_CALLER_ID,
                                             CALLER,
                                             WebConferencingService.EVENT_DATA_CALL_ENDED,
                                             "true",
                                             "status",
                                             WebConferencingService.STATUS_OK));
    verify(listenerService, never()).broadcast(eq(WebConferencingService.EVENT_CALL_MISSED),
                                               eq(call),
                                               argThat((Map<String, String> info) -> OTHER.equals(info.get(WebConferencingService.EVENT_DATA_USER_ID))));
  }

  @Test
  void theStillInvitedPeerOfARemovedUserCallIsNotifiedWithoutAMark() throws Exception {
    CallInfo call = call(new UserInfo(CALLER, CALLER, CALLER), participant(CALLEE, UserState.INVITED));
    call.setLastDate(new Date());
    call.setCallerId(CALLER);

    service.notifyStoppedMissedCall(call, List.of(CALLEE), true);

    verify(participantsStorage, never()).markParticipantMissedAtStop(anyString(), anyString(), any(Date.class));
    verify(listenerService).broadcast(WebConferencingService.EVENT_CALL_MISSED,
                                      call,
                                      Map.of(WebConferencingService.EVENT_DATA_USER_ID,
                                             CALLEE,
                                             WebConferencingService.EVENT_DATA_CALLER_ID,
                                             CALLER,
                                             "status",
                                             WebConferencingService.STATUS_OK));
  }

  @Test
  void aParticipantOfAStoppedCallWhoseMarkFailsIsNotNotifiedAndTheOthersAre() throws Exception {
    Date startedAt = new Date();
    CallInfo call = startedCall(startedAt, participant(CALLEE, UserState.INVITED), participant(OTHER, UserState.INVITED));
    call.setCallerId(CALLER);
    when(participantsStorage.markParticipantMissedAtStop(CALL_ID, CALLEE, latestStart(startedAt))).thenThrow(new PersistenceException("down"));
    when(participantsStorage.markParticipantMissedAtStop(CALL_ID, OTHER, latestStart(startedAt))).thenReturn(1);

    service.notifyStoppedMissedCall(call, List.of(CALLEE, OTHER), false);

    verify(listenerService, never()).broadcast(eq(WebConferencingService.EVENT_CALL_MISSED),
                                               eq(call),
                                               argThat((Map<String, String> info) -> CALLEE.equals(info.get(WebConferencingService.EVENT_DATA_USER_ID))));
    verify(listenerService).broadcast(eq(WebConferencingService.EVENT_CALL_MISSED),
                                      eq(call),
                                      argThat((Map<String, String> info) -> OTHER.equals(info.get(WebConferencingService.EVENT_DATA_USER_ID))));
  }

  @Test
  void theCallerIsStoredWithTheCallAndReadBack() throws Exception {
    CallInfo call = call(new UserInfo(CALLER, CALLER, CALLER), participant(CALLER, UserState.JOINED));
    call.setState(CallState.STARTED);
    call.setLastDate(new Date());
    call.setCallerId(CALLER);
    CallEntity entity = new CallEntity();
    entity.setId(CALL_ID);

    service.syncCallEntity(call, entity);

    assertEquals(CALLER, entity.getCallerId());
    doReturn(new UserInfo(CALLER, CALLER, CALLER)).when(service).userInfo(CALLER);
    doReturn(null).when(service).getInviteId(CALL_ID);
    assertEquals(CALLER, service.readCallEntity(entity, false).getCallerId());
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

  @Test
  void theTimerChecksTheMissedCallsThenReportsTheDeclines() throws Exception {
    Date startedAt = new Date();
    doNothing().when(service).notifyMissedCall(CALL_ID, CALLER, startedAt);
    doNothing().when(service).notifyDeclinesClosed(CALL_ID, CALLER, startedAt);

    try (MockedStatic<PortalContainer> containers = mockStatic(PortalContainer.class);
         MockedStatic<RequestLifeCycle> requests = mockStatic(RequestLifeCycle.class)) {
      service.checkMissedCall(CALL_ID, CALLER, startedAt);

      InOrder order = inOrder(service);
      order.verify(service).notifyMissedCall(CALL_ID, CALLER, startedAt);
      order.verify(service).notifyDeclinesClosed(CALL_ID, CALLER, startedAt);
      requests.verify(RequestLifeCycle::end);
    }
  }

  @Test
  void theTimerReportsTheDeclinesEvenWhenTheMissedCallCheckFails() throws Exception {
    Date startedAt = new Date();
    doThrow(new InvalidCallException("down")).when(service).notifyMissedCall(CALL_ID, CALLER, startedAt);
    doNothing().when(service).notifyDeclinesClosed(CALL_ID, CALLER, startedAt);

    try (MockedStatic<PortalContainer> containers = mockStatic(PortalContainer.class);
         MockedStatic<RequestLifeCycle> requests = mockStatic(RequestLifeCycle.class)) {
      service.checkMissedCall(CALL_ID, CALLER, startedAt);

      verify(service).notifyDeclinesClosed(CALL_ID, CALLER, startedAt);
    }
  }

  @Test
  void theTimerReportsTheDeclinesOfItsStartToTheCaller() throws Exception {
    Date startedAt = new Date();
    CallInfo call = startedCall(startedAt,
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.DECLINED),
                                participant(OTHER, UserState.DECLINED),
                                participant("paul", UserState.INVITED));
    when(participantsStorage.findDeclinedParticipantIds(CALL_ID)).thenReturn(List.of(CALLEE, OTHER));
    when(participantsStorage.markParticipantDeclinedNotified(eq(CALL_ID), anyString(), eq(latestStart(startedAt)))).thenReturn(1);

    service.notifyDeclinesClosed(CALL_ID, CALLER, startedAt);

    verify(listenerService).broadcast(WebConferencingService.EVENT_CALL_DECLINES_CLOSED,
                                      call,
                                      Map.of(WebConferencingService.EVENT_DATA_USER_ID,
                                             CALLER,
                                             WebConferencingService.EVENT_DATA_DECLINER_IDS,
                                             CALLEE + "," + OTHER,
                                             "status",
                                             WebConferencingService.STATUS_OK));
    verify(participantsStorage, never()).markParticipantDeclinedNotified(eq(CALL_ID), eq("paul"), any(Date.class));
  }

  @Test
  void theTimerReportsADeclineSavedAfterThisRequestLoadedTheCall() throws Exception {
    Date startedAt = new Date();
    // The missed call check of the same request loaded the participant still invited
    CallInfo call = startedCall(startedAt, participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.INVITED));
    when(participantsStorage.findDeclinedParticipantIds(CALL_ID)).thenReturn(List.of(CALLEE));
    when(participantsStorage.markParticipantDeclinedNotified(CALL_ID, CALLEE, latestStart(startedAt))).thenReturn(1);

    service.notifyDeclinesClosed(CALL_ID, CALLER, startedAt);

    verify(listenerService).broadcast(eq(WebConferencingService.EVENT_CALL_DECLINES_CLOSED),
                                      eq(call),
                                      argThat((Map<String, String> data) -> CALLEE.equals(data.get(WebConferencingService.EVENT_DATA_DECLINER_IDS))));
  }

  @Test
  void theTimerReportsOnlyTheDeclinesItMarked() throws Exception {
    Date startedAt = new Date();
    CallInfo call = startedCall(startedAt,
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.DECLINED),
                                participant(OTHER, UserState.DECLINED));
    when(participantsStorage.findDeclinedParticipantIds(CALL_ID)).thenReturn(List.of(CALLEE, OTHER));
    when(participantsStorage.markParticipantDeclinedNotified(CALL_ID, CALLEE, latestStart(startedAt))).thenReturn(0);
    when(participantsStorage.markParticipantDeclinedNotified(CALL_ID, OTHER, latestStart(startedAt))).thenReturn(1);

    service.notifyDeclinesClosed(CALL_ID, CALLER, startedAt);

    verify(listenerService).broadcast(eq(WebConferencingService.EVENT_CALL_DECLINES_CLOSED),
                                      eq(call),
                                      argThat((Map<String, String> data) -> OTHER.equals(data.get(WebConferencingService.EVENT_DATA_DECLINER_IDS))));
  }

  @Test
  void theTimerReportsNothingWhenNobodyDeclined() throws Exception {
    Date startedAt = new Date();
    startedCall(startedAt, participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.MISSED));

    service.notifyDeclinesClosed(CALL_ID, CALLER, startedAt);

    verify(participantsStorage, never()).markParticipantDeclinedNotified(anyString(), anyString(), any(Date.class));
    verifyNoDeclinesClosedBroadcast();
  }

  @Test
  void theTimerReportsNothingWhenEveryDeclineWasMarkedMeanwhile() throws Exception {
    Date startedAt = new Date();
    startedCall(startedAt, participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.DECLINED));
    when(participantsStorage.findDeclinedParticipantIds(CALL_ID)).thenReturn(List.of(CALLEE));
    when(participantsStorage.markParticipantDeclinedNotified(CALL_ID, CALLEE, latestStart(startedAt))).thenReturn(0);

    service.notifyDeclinesClosed(CALL_ID, CALLER, startedAt);

    verifyNoDeclinesClosedBroadcast();
  }

  @Test
  void theTimerOfAnEarlierStartOfACallStartedAgainReportsNothing() throws Exception {
    Date startedAt = new Date();
    startedCall(new Date(startedAt.getTime() + 5000), participant(CALLER, UserState.JOINED), participant(CALLEE, UserState.DECLINED));

    service.notifyDeclinesClosed(CALL_ID, CALLER, startedAt);

    verify(participantsStorage, never()).markParticipantDeclinedNotified(anyString(), anyString(), any(Date.class));
    verifyNoDeclinesClosedBroadcast();
  }

  @Test
  void theTimerReportsNothingForACallRemovedMeanwhile() throws Exception {
    doReturn(null).when(service).getCall(CALL_ID);

    service.notifyDeclinesClosed(CALL_ID, CALLER, new Date());

    verifyNoDeclinesClosedBroadcast();
  }

  @Test
  void theTimerLeavesTheDeclinesOfACallStoppedMeanwhileToItsStop() throws Exception {
    Date startedAt = new Date();
    CallInfo call = startedCall(startedAt, participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.DECLINED));
    call.setState(CallState.STOPPED);

    service.notifyDeclinesClosed(CALL_ID, CALLER, startedAt);

    verify(participantsStorage, never()).markParticipantDeclinedNotified(anyString(), anyString(), any(Date.class));
    verifyNoDeclinesClosedBroadcast();
  }

  @Test
  void aStoppedCallMarksItsDeclinesBeforeTheStopAndReportsThem() throws Exception {
    CallInfo call = removedUserCall();
    when(participantsStorage.findDeclinedParticipantIds(CALL_ID)).thenReturn(List.of(CALLEE));
    when(participantsStorage.markParticipantDeclinedNotified(CALL_ID, CALLEE, latestStart(call.getLastDate()))).thenReturn(1);
    stubStop();

    service.stopCall(call, CALLER, true);

    InOrder order = inOrder(participantsStorage, service);
    order.verify(participantsStorage).markParticipantDeclinedNotified(CALL_ID, CALLEE, latestStart(call.getLastDate()));
    order.verify(service).txStopCall(call, true);
    verify(service).scheduleStoppedDeclinesClosed(call, List.of(CALLEE));
  }

  @Test
  void theMarkedDeclinesOfARemovedCallAreReportedEvenWhenItsRemovalFails() throws Exception {
    CallInfo call = removedUserCall();
    when(participantsStorage.findDeclinedParticipantIds(CALL_ID)).thenReturn(List.of(CALLEE));
    when(participantsStorage.markParticipantDeclinedNotified(CALL_ID, CALLEE, latestStart(call.getLastDate()))).thenReturn(1);
    stubStop();
    doThrow(new PersistenceException("down")).when(service).txStopCall(call, true);

    assertThrows(StorageException.class, () -> service.stopCall(call, CALLER, true));

    verify(service).scheduleStoppedDeclinesClosed(call, List.of(CALLEE));
  }

  @Test
  void aStoppedCallMarksItsDeclinesOnceStoppedAndReportsThem() throws Exception {
    CallInfo call = startedCall(new Date(), participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.INVITED));
    call.setCallerId(CALLER);
    // The decline was saved after the stop read the call
    when(participantsStorage.findDeclinedParticipantIds(CALL_ID)).thenReturn(List.of(CALLEE));
    when(participantsStorage.markParticipantDeclinedNotified(CALL_ID, CALLEE, latestStart(call.getLastDate()))).thenReturn(1);
    stubStop();

    service.stopCall(call, CALLER, false);

    InOrder order = inOrder(service, participantsStorage);
    order.verify(service).txStopCall(call, false);
    order.verify(participantsStorage).findDeclinedParticipantIds(CALL_ID);
    order.verify(participantsStorage).markParticipantDeclinedNotified(CALL_ID, CALLEE, latestStart(call.getLastDate()));
    order.verify(service).scheduleStoppedDeclinesClosed(call, List.of(CALLEE));
  }

  @Test
  void aCallWhoseStopFailsLeavesItsDeclinesToTheTimer() throws Exception {
    CallInfo call = startedCall(new Date(), participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.DECLINED));
    call.setCallerId(CALLER);
    stubStop();
    doThrow(new PersistenceException("down")).when(service).txStopCall(call, false);

    assertThrows(StorageException.class, () -> service.stopCall(call, CALLER, false));

    verify(participantsStorage, never()).findDeclinedParticipantIds(anyString());
    verify(service, never()).scheduleStoppedDeclinesClosed(any(CallInfo.class), any());
  }

  @Test
  void aStoppedCallThatAlertedNobodyReportsNoDecline() throws Exception {
    CallInfo call = startedCall(new Date(), participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.DECLINED));
    stubStop();

    service.stopCall(call, CALLER, false);

    verify(participantsStorage, never()).findDeclinedParticipantIds(anyString());
    verify(service, never()).scheduleStoppedDeclinesClosed(any(CallInfo.class), any());
  }

  @Test
  void theDeclinesOfAStoppedCallAreReadFromTheStorageAndMarked() throws Exception {
    CallInfo call = startedCall(new Date(), participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.INVITED));
    call.setCallerId(CALLER);
    // The decline was saved after the stop read the call
    when(participantsStorage.findDeclinedParticipantIds(CALL_ID)).thenReturn(List.of(CALLEE));
    when(participantsStorage.markParticipantDeclinedNotified(CALL_ID, CALLEE, latestStart(call.getLastDate()))).thenReturn(1);

    assertEquals(List.of(CALLEE), service.markStopCallDeclinesClosed(call));

    verify(participantsStorage, never()).markParticipantDeclinedNotified(eq(CALL_ID), eq(OTHER), any(Date.class));
  }

  @Test
  void aStoppedCallWhoseDeclinesCannotBeReadStaysStopped() throws Exception {
    CallInfo call = startedCall(new Date(), participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.DECLINED));
    call.setCallerId(CALLER);
    when(participantsStorage.findDeclinedParticipantIds(CALL_ID)).thenThrow(new PersistenceException("down"));
    stubStop();

    service.stopCall(call, CALLER, false);

    verify(service).txStopCall(call, false);
    verify(service, never()).scheduleStoppedDeclinesClosed(any(CallInfo.class), any());
  }

  @Test
  void aRemovedCallWhoseDeclinesCannotBeReadIsRemovedAndNotifiesWhoMissedIt() throws Exception {
    CallInfo call = removedUserCall();
    when(participantsStorage.markParticipantMissed(CALL_ID, CALLEE, latestStart(call.getLastDate()))).thenReturn(1);
    when(participantsStorage.findDeclinedParticipantIds(CALL_ID)).thenThrow(new PersistenceException("down"));
    stubStop();

    service.stopCall(call, CALLER, true);

    verify(service).txStopCall(call, true);
    verify(service).scheduleStoppedMissedCall(call, List.of(CALLEE), true);
    verify(service, never()).scheduleStoppedDeclinesClosed(any(CallInfo.class), any());
  }

  @Test
  void aStoppedCallWithoutDeclineReportsNothing() throws Exception {
    CallInfo call = startedCall(new Date(), participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.MISSED));
    call.setCallerId(CALLER);
    when(participantsStorage.findDeclinedParticipantIds(CALL_ID)).thenReturn(List.of());

    assertEquals(List.of(), service.markStopCallDeclinesClosed(call));

    verify(participantsStorage, never()).markParticipantDeclinedNotified(anyString(), anyString(), any(Date.class));
  }

  @Test
  void theDeclinesOfAStoppedCallAreReportedOnTheMissedCallTimer() throws Exception {
    CallInfo call = removedUserCall();

    service.scheduleStoppedDeclinesClosed(call, List.of(CALLEE));

    verify(listenerService, timeout(3000)).broadcast(WebConferencingService.EVENT_CALL_DECLINES_CLOSED,
                                                     call,
                                                     Map.of(WebConferencingService.EVENT_DATA_USER_ID,
                                                            CALLER,
                                                            WebConferencingService.EVENT_DATA_DECLINER_IDS,
                                                            CALLEE,
                                                            "status",
                                                            WebConferencingService.STATUS_OK));
  }

  @Test
  void aMarkThatFailsLeavesTheOtherDeclinesReported() throws Exception {
    Date startedAt = new Date();
    CallInfo call = startedCall(startedAt,
                                participant(CALLER, UserState.JOINED),
                                participant(CALLEE, UserState.DECLINED),
                                participant(OTHER, UserState.DECLINED));
    when(participantsStorage.findDeclinedParticipantIds(CALL_ID)).thenReturn(List.of(CALLEE, OTHER));
    when(participantsStorage.markParticipantDeclinedNotified(CALL_ID, CALLEE, latestStart(startedAt))).thenThrow(new PersistenceException("down"));
    when(participantsStorage.markParticipantDeclinedNotified(CALL_ID, OTHER, latestStart(startedAt))).thenReturn(1);

    service.notifyDeclinesClosed(CALL_ID, CALLER, startedAt);

    verify(listenerService).broadcast(eq(WebConferencingService.EVENT_CALL_DECLINES_CLOSED),
                                      eq(call),
                                      argThat((Map<String, String> data) -> OTHER.equals(data.get(WebConferencingService.EVENT_DATA_DECLINER_IDS))));
  }

  @SuppressWarnings("unchecked")
  private void verifyNoDeclinesClosedBroadcast() throws Exception {
    verify(listenerService, never()).broadcast(eq(WebConferencingService.EVENT_CALL_DECLINES_CLOSED), any(CallInfo.class), anyMap());
  }

  private UserInfo participant(CallInfo call, String id) {
    return call.getParticipants().stream().filter(part -> part.getId().equals(id)).findFirst().orElseThrow();
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

  private CallInfo removedUserCall() {
    CallInfo call = call(new UserInfo(CALLER, CALLER, CALLER), participant(CALLER, UserState.LEAVED), participant(CALLEE, UserState.INVITED));
    call.setState(CallState.STARTED);
    call.setLastDate(new Date());
    call.setCallerId(CALLER);
    return call;
  }

  private void stubStop() throws Exception {
    doNothing().when(service).txStopCall(any(CallInfo.class), anyBoolean());
    doNothing().when(service).fireUserCallStateChanged(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    doNothing().when(service).notifyUserCallStateChanged(any(CallInfo.class), anyString(), anyString());
    doNothing().when(service).scheduleStoppedMissedCall(any(CallInfo.class), any(), anyBoolean());
    doNothing().when(service).scheduleStoppedDeclinesClosed(any(CallInfo.class), any());
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
