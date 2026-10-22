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
package org.exoplatform.webconferencing.cometd;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.CallState;
import org.exoplatform.webconferencing.GroupInfo;
import org.exoplatform.webconferencing.IdentityInfo;
import org.exoplatform.webconferencing.UserInfo;
import org.exoplatform.webconferencing.UserState;
import org.exoplatform.webconferencing.WebConferencingService;

/**
 * The update command applies a participant state sent by a client to the user
 * of the Cometd session, the declined state included.
 */
class CometdWebConferencingServiceStateTest {

  private static final String         CALL_ID   = "pexip-call-1";

  private static final String         USER_ID   = "john";

  private static final String         CLIENT_ID = "client-1";

  private static final long           REMOVED_AT = 1_800_000_000_000L;

  private static final String         CHANNEL_ID = "/eXo/Application/WebConferencing/call/" + CALL_ID;

  private WebConferencingService      webConferencing;

  private CometdWebConferencingService cometdService;

  @BeforeEach
  void setUp() {
    webConferencing = mock(WebConferencingService.class);
    cometdService = new CometdWebConferencingService(null, null, webConferencing, null, mock(CacheService.class), null);
  }

  @AfterEach
  void tearDown() {
    cometdService.stop();
  }

  /** What follows the end of a call window's connection runs once the leave grace time passed. */
  @Test
  void aTaskAfterTheLeaveGraceRunsOnceTheGraceTimePassed() throws Exception {
    when(webConferencing.getLeaveGraceTime()).thenReturn(0L);
    CountDownLatch ran = new CountDownLatch(1);

    cometdService.afterLeaveGrace(ran::countDown);

    assertTrue(ran.await(5, TimeUnit.SECONDS));
  }

  /** A call window reloaded joins its call again before the leave of its former connection applies. */
  @Test
  void aTaskAfterTheLeaveGraceDoesNotRunBeforeTheGraceTimePassed() throws Exception {
    when(webConferencing.getLeaveGraceTime()).thenReturn(60L);
    CountDownLatch ran = new CountDownLatch(1);

    cometdService.afterLeaveGrace(ran::countDown);

    assertFalse(ran.await(300, TimeUnit.MILLISECONDS));
  }

  @Test
  void declinedIsAnUpdatableState() {
    assertTrue(CometdWebConferencingService.isUpdatableState(UserState.DECLINED));
    assertFalse(CometdWebConferencingService.isUpdatableState("rejected"));
  }

  @Test
  void declinedStateDeclinesTheCallForTheSessionUser() throws Exception {
    CallInfo call = mock(CallInfo.class);
    when(webConferencing.declineCall(CALL_ID, USER_ID, CLIENT_ID)).thenReturn(call);

    assertSame(call, cometdService.updateCallState(CALL_ID, UserState.DECLINED, USER_ID, CLIENT_ID));

    verify(webConferencing).declineCall(CALL_ID, USER_ID, CLIENT_ID);
    verifyNoMoreInteractions(webConferencing);
  }

  @Test
  void anUnknownStateIsRefused() {
    assertThrows(IllegalArgumentException.class,
                 () -> cometdService.updateCallState(CALL_ID, "rejected", USER_ID, CLIENT_ID));
  }

  /** The call channel goes when its last subscriber, a call window, goes: the call still running is stopped. */
  @Test
  void aRunningGroupCallIsStoppedWhenItsChannelIsRemoved() throws Exception {
    CallInfo call = callOwnedBy(space(), CallState.STARTED);
    when(webConferencing.getCall(CALL_ID)).thenReturn(call);

    cometdService.stopCallOfRemovedChannel(CALL_ID, REMOVED_AT);

    verify(webConferencing).stopCall(CALL_ID, false);
  }

  @Test
  void aRunningP2PCallIsRemovedWhenItsChannelIsRemoved() throws Exception {
    CallInfo call = callOwnedBy(new UserInfo(USER_ID, "John", "Smith"), CallState.STARTED);
    when(webConferencing.getCall(CALL_ID)).thenReturn(call);

    cometdService.stopCallOfRemovedChannel(CALL_ID, REMOVED_AT);

    verify(webConferencing).stopCall(CALL_ID, true);
  }

  /** The leave of its last party stopped the call before its channel went: it is not stopped twice. */
  @Test
  void aStoppedCallIsNotStoppedAgainWhenItsChannelIsRemoved() throws Exception {
    CallInfo call = callOwnedBy(space(), CallState.STOPPED);
    when(webConferencing.getCall(CALL_ID)).thenReturn(call);

    cometdService.stopCallOfRemovedChannel(CALL_ID, REMOVED_AT);

    verify(webConferencing, never()).stopCall(anyString(), anyBoolean());
  }

  /** A call started again since its channel went, within the leave grace time, is not stopped by that removal. */
  @Test
  void aCallStartedAgainSinceItsChannelWasRemovedIsNotStopped() throws Exception {
    CallInfo call = callOwnedBy(new UserInfo(USER_ID, "John", "Smith"), CallState.STARTED, REMOVED_AT + 5000);
    when(webConferencing.getCall(CALL_ID)).thenReturn(call);

    cometdService.stopCallOfRemovedChannel(CALL_ID, REMOVED_AT);

    verify(webConferencing, never()).stopCall(anyString(), anyBoolean());
  }

  /** A start read from the storage a second off is not told from the removal: the call is not stopped. */
  @Test
  void aCallStartedWithinTheDatePrecisionOfItsChannelRemovalIsNotStopped() throws Exception {
    CallInfo call = callOwnedBy(space(), CallState.STARTED, REMOVED_AT - WebConferencingService.LAST_DATE_PRECISION + 1);
    when(webConferencing.getCall(CALL_ID)).thenReturn(call);

    cometdService.stopCallOfRemovedChannel(CALL_ID, REMOVED_AT);

    verify(webConferencing, never()).stopCall(anyString(), anyBoolean());
  }

  @Test
  void aCallGoneIsNotStoppedWhenItsChannelIsRemoved() throws Exception {
    cometdService.stopCallOfRemovedChannel(CALL_ID, REMOVED_AT);

    verify(webConferencing, never()).stopCall(anyString(), anyBoolean());
  }

  /** The latest removal of a call channel no client subscribed to again stops its call. */
  @Test
  void theCallOfAChannelRemovedAndNotSubscribedAgainIsStopped() throws Exception {
    when(webConferencing.getCall(CALL_ID)).thenReturn(callOwnedBy(space(), CallState.STARTED));
    Object removal = cometdService.service.recordChannelRemoval(CHANNEL_ID);

    cometdService.service.stopCallAfterChannelRemoval(CHANNEL_ID, CALL_ID, removal, REMOVED_AT);

    verify(webConferencing).stopCall(CALL_ID, false);
  }

  /** A call window reloaded subscribed to its call again within the leave grace time: the call goes on. */
  @Test
  void theCallOfAChannelSubscribedAgainIsNotStopped() throws Exception {
    when(webConferencing.getCall(CALL_ID)).thenReturn(callOwnedBy(space(), CallState.STARTED));
    Object removal = cometdService.service.recordChannelRemoval(CHANNEL_ID);
    cometdService.service.addCallClient(CHANNEL_ID, "portal", "session-2", USER_ID, "client-2");

    cometdService.service.stopCallAfterChannelRemoval(CHANNEL_ID, CALL_ID, removal, REMOVED_AT);

    verify(webConferencing, never()).stopCall(anyString(), anyBoolean());
  }

  /** A channel removed twice: only the latest removal, once its own grace time passed, stops the call. */
  @Test
  void onlyTheLatestRemovalOfAChannelStopsItsCall() throws Exception {
    when(webConferencing.getCall(CALL_ID)).thenReturn(callOwnedBy(space(), CallState.STARTED));
    Object first = cometdService.service.recordChannelRemoval(CHANNEL_ID);
    Object latest = cometdService.service.recordChannelRemoval(CHANNEL_ID);

    cometdService.service.stopCallAfterChannelRemoval(CHANNEL_ID, CALL_ID, first, REMOVED_AT);
    verify(webConferencing, never()).stopCall(anyString(), anyBoolean());

    cometdService.service.stopCallAfterChannelRemoval(CHANNEL_ID, CALL_ID, latest, REMOVED_AT);
    verify(webConferencing).stopCall(CALL_ID, false);
  }

  /** A call window whose subscription ended leaves the call from its client. */
  @Test
  void aClientWhoseSubscriptionEndedLeavesTheCall() throws Exception {
    when(webConferencing.getCall(CALL_ID)).thenReturn(callOwnedBy(space(), CallState.STARTED));
    cometdService.service.addCallClient(CHANNEL_ID, "portal", "session-1", USER_ID, CLIENT_ID);
    var context = cometdService.service.getCallChannelContext(CHANNEL_ID);

    cometdService.service.leaveCallOfEndedSubscription(CHANNEL_ID, CALL_ID, context, "session-1");

    verify(webConferencing).leaveCallFromClient(CALL_ID, USER_ID, CLIENT_ID);
  }

  /** A call window whose session expired reconnected with the same client and subscribed again: it stays. */
  @Test
  void aClientReconnectedToItsCallDoesNotLeaveIt() throws Exception {
    when(webConferencing.getCall(CALL_ID)).thenReturn(callOwnedBy(space(), CallState.STARTED));
    cometdService.service.addCallClient(CHANNEL_ID, "portal", "session-1", USER_ID, CLIENT_ID);
    var context = cometdService.service.getCallChannelContext(CHANNEL_ID);
    cometdService.service.addCallClient(CHANNEL_ID, "portal", "session-2", USER_ID, CLIENT_ID);

    cometdService.service.leaveCallOfEndedSubscription(CHANNEL_ID, CALL_ID, context, "session-1");

    verify(webConferencing, never()).leaveCallFromClient(anyString(), anyString(), anyString());
  }

  /** A call window reloaded subscribed again from a new client: its former client leaves the call. */
  @Test
  void theFormerClientOfAReloadedWindowLeavesTheCall() throws Exception {
    when(webConferencing.getCall(CALL_ID)).thenReturn(callOwnedBy(space(), CallState.STARTED));
    cometdService.service.addCallClient(CHANNEL_ID, "portal", "session-1", USER_ID, CLIENT_ID);
    var context = cometdService.service.getCallChannelContext(CHANNEL_ID);
    cometdService.service.addCallClient(CHANNEL_ID, "portal", "session-2", USER_ID, "client-2");

    cometdService.service.leaveCallOfEndedSubscription(CHANNEL_ID, CALL_ID, context, "session-1");

    verify(webConferencing).leaveCallFromClient(CALL_ID, USER_ID, CLIENT_ID);
  }

  /** Another user's window on the same client id does not keep a user in the call. */
  @Test
  void anotherUsersClientDoesNotKeepAUserInTheCall() throws Exception {
    when(webConferencing.getCall(CALL_ID)).thenReturn(callOwnedBy(space(), CallState.STARTED));
    cometdService.service.addCallClient(CHANNEL_ID, "portal", "session-1", USER_ID, CLIENT_ID);
    var context = cometdService.service.getCallChannelContext(CHANNEL_ID);
    cometdService.service.addCallClient(CHANNEL_ID, "portal", "session-2", "mary", CLIENT_ID);

    cometdService.service.leaveCallOfEndedSubscription(CHANNEL_ID, CALL_ID, context, "session-1");

    verify(webConferencing).leaveCallFromClient(CALL_ID, USER_ID, CLIENT_ID);
  }

  private CallInfo callOwnedBy(IdentityInfo owner, String state) {
    return callOwnedBy(owner, state, REMOVED_AT - 60000);
  }

  private CallInfo callOwnedBy(IdentityInfo owner, String state, long startedAt) {
    CallInfo call = new CallInfo(CALL_ID, "Call", owner, "jitsi");
    call.setState(state);
    call.setLastDate(new Date(startedAt));
    return call;
  }

  private GroupInfo space() {
    return new GroupInfo("space1", "Space 1") {
      @Override
      public String getType() {
        return WebConferencingService.OWNER_TYPE_SPACE;
      }
    };
  }
}
