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
package org.exoplatform.webconferencing.listener;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.CallState;
import org.exoplatform.webconferencing.GuestInfo;
import org.exoplatform.webconferencing.UserInfo;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.plugin.VisioApplicationBadgePlugin;

import io.meeds.appcenter.service.ApplicationBadgeService;

/**
 * The listener is glue: it must refresh every eXo participant of a call whose
 * liveness changed, and nobody else — including on the one path that starts a
 * call without ever broadcasting {@code callStarted}.
 */
@ExtendWith(MockitoExtension.class)
class VisioApplicationBadgeListenerTest {

  @Mock
  private ApplicationBadgeService        applicationBadgeService;

  @Mock
  private ListenerService                listenerService;

  @Mock
  private CallInfo                       call;

  @InjectMocks
  private VisioApplicationBadgeListener  listener;

  /**
   * A call that starts changes the count of everyone it was synced to, not just
   * of whoever pressed the button.
   */
  @Test
  void refreshesEveryParticipantWhenACallStarts() throws Exception {
    when(call.getParticipants()).thenReturn(participants());

    listener.onEvent(event(WebConferencingService.EVENT_CALL_STARTED));

    verify(applicationBadgeService).updateBadge(VisioApplicationBadgePlugin.BADGE_NAME, "mary");
    verify(applicationBadgeService).updateBadge(VisioApplicationBadgePlugin.BADGE_NAME, "john");
  }

  /**
   * Guests are external invitees with no Application Center to badge; pushing
   * to them is a wasted eviction at best.
   */
  @Test
  void skipsGuestParticipants() throws Exception {
    when(call.getParticipants()).thenReturn(participants());

    listener.onEvent(event(WebConferencingService.EVENT_CALL_STARTED));

    verify(applicationBadgeService, never()).updateBadge(any(), eq("guest-1"));
  }

  /**
   * The count has to drop when the last participant leaves, too.
   */
  @Test
  void refreshesEveryParticipantWhenACallStops() throws Exception {
    when(call.getParticipants()).thenReturn(participants());

    listener.onEvent(event(WebConferencingService.EVENT_CALL_STOPPED));

    verify(applicationBadgeService).updateBadge(VisioApplicationBadgePlugin.BADGE_NAME, "mary");
  }

  /**
   * The bug this prevents: {@code addCall(..., start = true)} — the space call
   * button, and every client starting a visio outright — persists the call in
   * state {@code started} and broadcasts <strong>only</strong>
   * {@code callCreated}. Listening to {@code callStarted} alone would leave the
   * most common way a visio begins with no badge.
   */
  @Test
  void refreshesOnACallCreatedAlreadyStarted() throws Exception {
    when(call.getState()).thenReturn(CallState.STARTED);
    when(call.getParticipants()).thenReturn(participants());

    listener.onEvent(event(WebConferencingService.EVENT_CALL_CREATED));

    verify(applicationBadgeService).updateBadge(VisioApplicationBadgePlugin.BADGE_NAME, "mary");
  }

  /**
   * An agenda event books its visio hours in advance. Refreshing then would
   * evict and push to every attendee of every planned meeting, for a count that
   * did not move.
   */
  @Test
  void ignoresACallCreatedButNotStarted() throws Exception {
    when(call.getState()).thenReturn(CallState.STOPPED);

    listener.onEvent(event(WebConferencingService.EVENT_CALL_CREATED));

    verifyNoInteractions(applicationBadgeService);
  }

  /**
   * The listener is asynchronous and the payload comes from another module; a
   * missing call must not throw inside the event bus.
   */
  @Test
  void toleratesAnEventWithoutACall() {
    Event<CallInfo, java.util.Map<String, String>> event =
                                                         new Event<>(WebConferencingService.EVENT_CALL_STARTED, null, null);

    assertDoesNotThrow(() -> listener.onEvent(event));
    verifyNoInteractions(applicationBadgeService);
  }

  /**
   * Subscribing to the three liveness broadcasts is what makes the badge
   * real-time; a missing one is silent staleness.
   */
  @Test
  void subscribesToTheThreeLivenessEvents() {
    listener.init();

    verify(listenerService).addListener(WebConferencingService.EVENT_CALL_CREATED, listener);
    verify(listenerService).addListener(WebConferencingService.EVENT_CALL_STARTED, listener);
    verify(listenerService).addListener(WebConferencingService.EVENT_CALL_STOPPED, listener);
  }

  /**
   * Without the Application Center there is no badge to refresh, and Web
   * Conferencing must still start.
   */
  @Test
  void standsDownWithoutTheApplicationCenter() {
    ReflectionTestUtils.setField(listener, "applicationBadgeService", null);

    assertDoesNotThrow(() -> listener.init());
    verifyNoInteractions(listenerService);
  }

  /**
   * @param  eventName the broadcast name to simulate
   * @return           an event shaped like {@code broacastCallEvent} builds
   *                     them: the call as source, the metric map as data
   */
  private Event<CallInfo, java.util.Map<String, String>> event(String eventName) {
    return new Event<>(eventName, call, java.util.Map.of("user_id", "mary"));
  }

  /**
   * @return a participant set mixing eXo users and an external guest, as a
   *           started call with an invited external has
   */
  private Set<UserInfo> participants() {
    Set<UserInfo> participants = new LinkedHashSet<>();
    participants.add(new UserInfo("mary", "Mary", "Kelly"));
    participants.add(new UserInfo("john", "John", "Smith"));
    participants.add(new GuestInfo("guest-1"));
    return participants;
  }

}
