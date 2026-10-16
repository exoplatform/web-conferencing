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
package org.exoplatform.webconferencing.notification.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * The missed call notification goes to the user who missed the call, from the
 * user who started it, and carries what the drawer, the email and the push
 * need to render it.
 */
class MissedCallPluginTest {

  private MissedCallPlugin    plugin;

  private NotificationContext ctx;

  private Map<String, Object> values;

  @BeforeEach
  void setUp() {
    plugin = new MissedCallPlugin(mock(InitParams.class));
    ctx = mock(NotificationContext.class);
    values = new HashMap<>();
    // ArgumentLiteral equality only compares the value type: answer the values by key
    when(ctx.value(any())).thenAnswer(invocation -> values.get(invocation.<ArgumentLiteral<?>> getArgument(0).getKey()));
  }

  @Test
  void theNotificationGoesToTheUserWhoMissedTheCallFromTheCaller() {
    stubGroupCall();

    NotificationInfo notification = plugin.makeNotification(ctx);

    assertEquals(List.of("john"), notification.getSendToUserIds());
    assertEquals("root", notification.getFrom());
    assertEquals(MissedCallPlugin.ID, notification.getKey().getId());
    assertEquals("call-1", notification.getValueOwnerParameter(NotificationConstants.CALL_ID.getKey()));
    assertEquals("pexip", notification.getValueOwnerParameter(NotificationConstants.PROVIDER_TYPE.getKey()));
    assertEquals("root", notification.getValueOwnerParameter(NotificationConstants.CALLER_ID.getKey()));
    assertEquals(WebConferencingService.OWNER_TYPE_SPACE,
                 notification.getValueOwnerParameter(NotificationConstants.CALL_TYPE.getKey()));
    assertEquals("marketing", notification.getValueOwnerParameter(NotificationConstants.OWNER_ID.getKey()));
    assertEquals("Marketing", notification.getValueOwnerParameter(NotificationConstants.CALL_OWNER.getKey()));
    assertEquals("/avatar", notification.getValueOwnerParameter(NotificationConstants.AVATAR_URL.getKey()));
  }

  @Test
  void aCompleteMissedCallIsValid() {
    stubGroupCall();

    assertTrue(plugin.isValid(ctx));
  }

  @Test
  void aMissedCallWithoutRecipientCallerOrCallIsNotValid() {
    stubGroupCall();
    values.remove(NotificationConstants.CALL_PARTICIPANTS.getKey());
    assertFalse(plugin.isValid(ctx));

    stubGroupCall();
    values.put(NotificationConstants.CALLER_ID.getKey(), " ");
    assertFalse(plugin.isValid(ctx));

    stubGroupCall();
    values.remove(NotificationConstants.CALL_ID.getKey());
    assertFalse(plugin.isValid(ctx));
  }

  private void stubGroupCall() {
    values.put(NotificationConstants.CALL_PARTICIPANTS.getKey(), List.of("john"));
    values.put(NotificationConstants.CALLER_ID.getKey(), "root");
    values.put(NotificationConstants.CALL_ID.getKey(), "call-1");
    values.put(NotificationConstants.PROVIDER_TYPE.getKey(), "pexip");
    values.put(NotificationConstants.CALL_TYPE.getKey(), WebConferencingService.OWNER_TYPE_SPACE);
    values.put(NotificationConstants.OWNER_ID.getKey(), "marketing");
    values.put(NotificationConstants.CALL_OWNER.getKey(), "Marketing");
    values.put(NotificationConstants.AVATAR_URL.getKey(), "/avatar");
  }
}
