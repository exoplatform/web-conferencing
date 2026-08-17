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

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.CallState;
import org.exoplatform.webconferencing.UserInfo;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.plugin.VisioApplicationBadgePlugin;

import io.meeds.appcenter.plugin.ApplicationBadgePlugin;
import io.meeds.appcenter.service.ApplicationBadgeService;

import jakarta.annotation.PostConstruct;

/**
 * Refreshes the Visio badge of every participant of a call that just started or
 * just stopped.
 * <p>
 * Pure glue: it holds no counting logic, it only tells the Application Center
 * that those users' counts went stale. This is what makes the badge real-time —
 * a colleague starting the space visio makes the icon light up without anyone
 * reloading a page.
 * <p>
 * The three event names below are the complete set of broadcasts that change
 * whether a call is live:
 * <ul>
 * <li>{@code callStarted} and {@code callStopped} are the obvious ones;</li>
 * <li>{@code callCreated} is the one that is easy to miss — a call created
 * already running (the space call button, and any client calling {@code addCall}
 * with {@code start=true}) is persisted in state {@code started} and broadcasts
 * <strong>only</strong> {@code callCreated}, never {@code callStarted}. Without
 * it the most common way a visio begins would raise no badge at all.</li>
 * </ul>
 * {@code callJoined} and {@code callLeft} are deliberately <em>not</em>
 * listened to: with the plugin's "every started call" semantics the count does
 * not move when somebody joins or leaves, so subscribing would evict and push
 * to every participant of every call on every join, for no change.
 * <p>
 * Two paths change a call's liveness without broadcasting anything, and both
 * are left to the badge cache's own expiry rather than papered over here:
 * an outdated {@code started} call silently deleted while a replacement is
 * created, and the reset of lingering {@code started} calls at server start —
 * where the cache is empty anyway.
 */
@Component
@Asynchronous
@ConditionalOnClass(ApplicationBadgePlugin.class)
public class VisioApplicationBadgeListener extends Listener<CallInfo, Map<String, String>> {

  /** Logger of this listener. */
  private static final Log          LOG         = ExoLogger.getLogger(VisioApplicationBadgeListener.class);

  /** The complete set of broadcasts that move a call in or out of the live set. */
  private static final List<String> EVENT_NAMES = List.of(WebConferencingService.EVENT_CALL_CREATED,
                                                          WebConferencingService.EVENT_CALL_STARTED,
                                                          WebConferencingService.EVENT_CALL_STOPPED);

  /**
   * Optional for the same reason as the plugin it feeds: without the
   * Application Center there is no badge to refresh, and Web Conferencing must
   * still start.
   */
  @Autowired(required = false)
  private ApplicationBadgeService   applicationBadgeService;

  /** The Kernel event bus this listener subscribes to. */
  @Autowired
  private ListenerService           listenerService;

  /**
   * Subscribes to the call lifecycle broadcasts, unless the Application Center
   * is not deployed.
   */
  @PostConstruct
  public void init() {
    if (applicationBadgeService == null) {
      LOG.debug("Application Center badge service not available, Visio badge listener not registered");
      return;
    }
    EVENT_NAMES.forEach(eventName -> listenerService.addListener(eventName, this));
  }

  /**
   * Marks the badge of every eXo user taking part in the call as stale.
   *
   * @param event the call lifecycle event, carrying the call as its source
   */
  @Override
  public void onEvent(Event<CallInfo, Map<String, String>> event) throws Exception {
    CallInfo call = event.getSource();
    if (call == null || !changesLiveness(event.getEventName(), call)) {
      return;
    }
    call.getParticipants()
        .stream()
        .filter(participant -> UserInfo.TYPE_NAME.equals(participant.getType()))
        .map(UserInfo::getId)
        .distinct()
        .forEach(username -> applicationBadgeService.updateBadge(VisioApplicationBadgePlugin.BADGE_NAME, username));
  }

  /**
   * Tells whether an event actually moved a call in or out of the live set.
   * <p>
   * {@code callCreated} is raised for every call, including the scheduled ones
   * an agenda event creates hours in advance; only those created already
   * running change anyone's count. Filtering here is what keeps a calendar full
   * of planned visios from evicting and pushing to all their attendees.
   *
   * @param  eventName the broadcast name
   * @param  call      the call carried by the event
   * @return           {@code true} when participants' badges have to be
   *                     refreshed
   */
  private boolean changesLiveness(String eventName, CallInfo call) {
    if (WebConferencingService.EVENT_CALL_CREATED.equals(eventName)) {
      return CallState.STARTED.equals(call.getState());
    }
    return true;
  }

}
