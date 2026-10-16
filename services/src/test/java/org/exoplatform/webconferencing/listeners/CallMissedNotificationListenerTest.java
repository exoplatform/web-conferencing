/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.webconferencing.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.services.listener.Event;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.GroupInfo;
import org.exoplatform.webconferencing.UserInfo;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.notification.plugin.MissedCallPlugin;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * The listener sends one missed call notification to the user the event names,
 * from the caller the event names, with the group of a group call.
 */
class CallMissedNotificationListenerTest {

  private NotificationContext            ctx;

  private Map<String, Object>            appended;

  private CallMissedNotificationListener listener;

  @BeforeEach
  void setUp() {
    ctx = mock(NotificationContext.class, RETURNS_DEEP_STUBS);
    appended = new HashMap<>();
    // ArgumentLiteral equality only compares the value type: record the appended values by key
    when(ctx.append(any(), any())).thenAnswer(invocation -> {
      appended.put(invocation.<ArgumentLiteral<?>> getArgument(0).getKey(), invocation.getArgument(1));
      return ctx;
    });
    listener = spy(new CallMissedNotificationListener());
    doReturn(ctx).when(listener).newNotificationContext();
  }

  @Test
  void aMissedGroupCallIsNotifiedWithItsGroup() throws Exception {
    GroupInfo space = group();
    space.setAvatarLink("/space-avatar");

    listener.onEvent(event(new CallInfo("g_marketing", "Marketing", space, "pexip")));

    assertEquals(Map.of(NotificationConstants.CALL_PARTICIPANTS.getKey(),
                        List.of("john"),
                        NotificationConstants.CALLER_ID.getKey(),
                        "root",
                        NotificationConstants.CALL_ID.getKey(),
                        "g_marketing",
                        NotificationConstants.PROVIDER_TYPE.getKey(),
                        "pexip",
                        NotificationConstants.CALL_TYPE.getKey(),
                        WebConferencingService.OWNER_TYPE_SPACE,
                        NotificationConstants.OWNER_ID.getKey(),
                        "marketing",
                        NotificationConstants.CALL_OWNER.getKey(),
                        "Marketing",
                        NotificationConstants.AVATAR_URL.getKey(),
                        "/space-avatar"),
                 appended);
    verify(ctx).makeCommand(PluginKey.key(MissedCallPlugin.ID));
  }

  @Test
  void aMissedUserCallIsNotifiedWithoutGroup() throws Exception {
    listener.onEvent(event(new CallInfo("p_call", "Root", new UserInfo("root", "Root", "Root"), "jitsi")));

    assertEquals(Map.of(NotificationConstants.CALL_PARTICIPANTS.getKey(),
                        List.of("john"),
                        NotificationConstants.CALLER_ID.getKey(),
                        "root",
                        NotificationConstants.CALL_ID.getKey(),
                        "p_call",
                        NotificationConstants.PROVIDER_TYPE.getKey(),
                        "jitsi",
                        NotificationConstants.CALL_TYPE.getKey(),
                        WebConferencingService.OWNER_TYPE_USER),
                 appended);
    verify(ctx).makeCommand(PluginKey.key(MissedCallPlugin.ID));
  }

  private Event<CallInfo, Map<? extends String, ? extends String>> event(CallInfo call) {
    return new Event<>(WebConferencingService.EVENT_CALL_MISSED,
                       call,
                       Map.of(WebConferencingService.EVENT_DATA_USER_ID,
                              "john",
                              WebConferencingService.EVENT_DATA_CALLER_ID,
                              "root"));
  }

  private GroupInfo group() {
    return new GroupInfo("marketing", "Marketing") {
      @Override
      public String getType() {
        return WebConferencingService.OWNER_TYPE_SPACE;
      }
    };
  }
}
