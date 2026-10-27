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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * The plugin carries the meeting's title and start date from the listener's
 * context into the stored notification, which the web and mail templates read.
 */
class CallRecordingPluginTest {

  /**
   * The title and the start date are stored with the other parameters, for the
   * recipients the listener chose.
   */
  @Test
  void makeNotificationCarriesTitleAndStartDate() {
    NotificationContext ctx = context("Demo ACME", "1792764000000");

    NotificationInfo notification = new CallRecordingPlugin(new InitParams()).makeNotification(ctx);

    assertEquals("Demo ACME", notification.getValueOwnerParameter(NotificationConstants.CALL_TITLE.getKey()));
    assertEquals("1792764000000", notification.getValueOwnerParameter(NotificationConstants.CALL_START_DATE.getKey()));
    assertEquals("Sales team", notification.getValueOwnerParameter(NotificationConstants.CALL_OWNER.getKey()));
    assertEquals(List.of("alice"), notification.getSendToUserIds());
  }

  /**
   * A call without a title or a date stores neither parameter, rather than a
   * null value.
   */
  @Test
  void makeNotificationOmitsMissingTitleAndStartDate() {
    NotificationContext ctx = context(null, " ");

    NotificationInfo notification = new CallRecordingPlugin(new InitParams()).makeNotification(ctx);

    assertFalse(notification.getOwnerParameter().containsKey(NotificationConstants.CALL_TITLE.getKey()));
    assertFalse(notification.getOwnerParameter().containsKey(NotificationConstants.CALL_START_DATE.getKey()));
  }

  /**
   * Builds the context the listener fills.
   *
   * @param title the meeting's title
   * @param startDate the meeting's start date
   * @return the context
   */
  private NotificationContext context(String title, String startDate) {
    Map<String, Object> values = new HashMap<>();
    values.put(NotificationConstants.RECORDING_STATUS.getKey(), "ok");
    values.put(NotificationConstants.RECORDED_FILE_URL.getKey(), "/doc");
    values.put(NotificationConstants.FILE_NAME.getKey(), "g_1234_20261022.mp4");
    values.put(NotificationConstants.AVATAR_URL.getKey(), "/avatar");
    values.put(NotificationConstants.CALL_OWNER.getKey(), "Sales team");
    values.put(NotificationConstants.CALL_TITLE.getKey(), title);
    values.put(NotificationConstants.CALL_START_DATE.getKey(), startDate);
    values.put(NotificationConstants.CALL_PARTICIPANTS.getKey(), List.of("alice"));
    NotificationContext ctx = mock(NotificationContext.class);
    // ArgumentLiteral.equals compares the value type only, so values are looked up by key
    when(ctx.value(any())).thenAnswer(invocation -> values.get(((ArgumentLiteral<?>) invocation.getArgument(0)).getKey()));
    return ctx;
  }
}
