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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.model.UserSetting;
import org.exoplatform.commons.api.notification.model.WebNotificationFilter;
import org.exoplatform.commons.api.notification.service.WebNotificationService;
import org.exoplatform.commons.api.notification.service.setting.PluginSettingService;
import org.exoplatform.commons.api.notification.service.setting.UserSettingService;
import org.exoplatform.commons.notification.channel.WebChannel;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

import io.meeds.pwa.service.PwaNotificationService;

/**
 * The caller gets one web notification per call start, created at the first
 * decline and updated at each next one, read or not, and pushed again; the
 * email waits for the declines to be closed.
 */
class DeclinedCallPluginTest {

  private static final String    DECLINE_KEY = "g_marketing@1700000000000";

  private DeclinedCallPlugin     plugin;

  private NotificationContext    ctx;

  private Map<String, Object>    values;

  private WebNotificationService webNotificationService;

  private PwaNotificationService pwaNotificationService;

  private UserSetting            userSetting;

  private PluginSettingService   pluginSettingService;

  @BeforeEach
  void setUp() {
    plugin = spy(new DeclinedCallPlugin(mock(InitParams.class)));
    ctx = mock(NotificationContext.class);
    values = new HashMap<>();
    // ArgumentLiteral equality only compares the value type: answer the values by key
    when(ctx.value(any())).thenAnswer(invocation -> values.get(invocation.<ArgumentLiteral<?>> getArgument(0).getKey()));
    webNotificationService = mock(WebNotificationService.class);
    pwaNotificationService = mock(PwaNotificationService.class);
    UserSettingService userSettingService = mock(UserSettingService.class);
    userSetting = mock(UserSetting.class);
    when(userSettingService.get("root")).thenReturn(userSetting);
    when(userSetting.isEnabled()).thenReturn(true);
    when(userSetting.isChannelGloballyActive(WebChannel.ID)).thenReturn(true);
    when(userSetting.isActive(WebChannel.ID, DeclinedCallPlugin.ID)).thenReturn(true);
    doReturn(webNotificationService).when(plugin).getWebNotificationService();
    doReturn(pwaNotificationService).when(plugin).getPwaNotificationService();
    doReturn(userSettingService).when(plugin).getUserSettingService();
    pluginSettingService = mock(PluginSettingService.class);
    when(pluginSettingService.isActive(WebChannel.ID, DeclinedCallPlugin.ID)).thenReturn(true);
    doReturn(pluginSettingService).when(plugin).getPluginSettingService();
  }

  @Test
  void theFirstDeclineCreatesTheWebNotificationOfTheCaller() {
    stubDecline("john");

    NotificationInfo notification = plugin.makeNotification(ctx);

    ArgumentCaptor<NotificationInfo> saved = ArgumentCaptor.forClass(NotificationInfo.class);
    verify(webNotificationService).save(saved.capture());
    assertEquals("root", saved.getValue().getTo());
    assertEquals("john", saved.getValue().getFrom());
    assertTrue(saved.getValue().isOnPopOver());
    assertFalse(saved.getValue().isRead());
    assertEquals("john", saved.getValue().getValueOwnerParameter(NotificationConstants.DECLINER_IDS.getKey()));
    assertEquals(DECLINE_KEY, saved.getValue().getValueOwnerParameter(NotificationConstants.DECLINE_KEY.getKey()));
    assertEquals("g_marketing", saved.getValue().getValueOwnerParameter(NotificationConstants.CALL_ID.getKey()));
    assertEquals(WebConferencingService.OWNER_TYPE_SPACE,
                 saved.getValue().getValueOwnerParameter(NotificationConstants.CALL_TYPE.getKey()));
    verify(webNotificationService, never()).update(any(), anyBoolean());
    verify(pwaNotificationService, never()).create(anyLong());
    assertWebChannelSkipped(notification);
  }

  @Test
  void aNextDeclineUpdatesTheWebNotificationAndPushesItAgain() {
    NotificationInfo previous = previous("john");
    stubDecline("mary");

    NotificationInfo notification = plugin.makeNotification(ctx);

    verify(webNotificationService).update(previous, true);
    assertEquals("mary,john", previous.getValueOwnerParameter(NotificationConstants.DECLINER_IDS.getKey()));
    assertEquals("mary", previous.getFrom());
    assertTrue(previous.isUpdate());
    assertFalse(previous.isRead());
    assertTrue(previous.isOnPopOver());
    assertEquals("false", previous.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey()));
    assertEquals("true", previous.getValueOwnerParameter(NotificationMessageUtils.SHOW_POPOVER_PROPERTY.getKey()));
    verify(pwaNotificationService).create(42L);
    verify(webNotificationService, never()).save(any());
    assertWebChannelSkipped(notification);
  }

  @Test
  void aParticipantCountsOnceWhateverItsDeclines() {
    NotificationInfo previous = previous("mary,john");
    stubDecline("john");

    plugin.makeNotification(ctx);

    assertEquals("john,mary", previous.getValueOwnerParameter(NotificationConstants.DECLINER_IDS.getKey()));
  }

  @Test
  void theWebNotificationOfTheCallStartIsLookedForReadOrNot() {
    previous("john");
    stubDecline("mary");

    plugin.makeNotification(ctx);

    ArgumentCaptor<WebNotificationFilter> filter = ArgumentCaptor.forClass(WebNotificationFilter.class);
    verify(webNotificationService).getNotificationInfos(filter.capture(), any(Integer.class), anyInt());
    assertEquals("root", filter.getValue().getUserId());
    assertEquals(List.of(PluginKey.key(DeclinedCallPlugin.ID)), filter.getValue().getPluginKeys());
    assertEquals(Pair.of(NotificationConstants.DECLINE_KEY.getKey(), DECLINE_KEY), filter.getValue().getParameter());
    assertNull(filter.getValue().getIsRead());
    assertFalse(filter.getValue().isOnPopover());
  }

  @Test
  void aCallerWhoDoesNotGetItOnTheWebGetsNoWebNotification() {
    when(userSetting.isActive(WebChannel.ID, DeclinedCallPlugin.ID)).thenReturn(false);
    previous("john");
    stubDecline("mary");

    NotificationInfo notification = plugin.makeNotification(ctx);

    verify(webNotificationService, never()).save(any());
    verify(webNotificationService, never()).update(any(), anyBoolean());
    assertWebChannelSkipped(notification);
  }

  @Test
  void noWebNotificationIsStoredWhenThePlatformDisablesItOnTheWeb() {
    when(pluginSettingService.isActive(WebChannel.ID, DeclinedCallPlugin.ID)).thenReturn(false);
    stubDecline("john");

    NotificationInfo notification = plugin.makeNotification(ctx);

    verify(webNotificationService, never()).save(any());
    assertWebChannelSkipped(notification);
  }

  @Test
  void theClosedDeclinesOnlyGoOnByEmail() {
    stubDecline("mary,john");
    values.put(NotificationConstants.DECLINES_CLOSED.getKey(), "true");

    NotificationInfo notification = plugin.makeNotification(ctx);

    verify(webNotificationService, never()).getNotificationInfos(any(), anyInt(), anyInt());
    verify(webNotificationService, never()).save(any());
    assertEquals(List.of("root"), notification.getSendToUserIds());
    assertEquals("mary,john", notification.getValueOwnerParameter(NotificationConstants.DECLINER_IDS.getKey()));
    assertWebChannelSkipped(notification);
  }

  @Test
  void aPushThatCannotBeSentAgainKeepsTheUpdate() {
    NotificationInfo previous = previous("john");
    when(pwaNotificationService.create(anyLong())).thenThrow(new IllegalStateException("down"));
    stubDecline("mary");

    plugin.makeNotification(ctx);

    verify(webNotificationService).update(previous, true);
  }

  @Test
  void aDeclineWithItsCallerItsCallItsStartAndItsDeclinerIsValid() {
    stubDecline("john");
    assertTrue(plugin.isValid(ctx));

    for (ArgumentLiteral<String> required : List.of(NotificationConstants.CALLER_ID,
                                                    NotificationConstants.CALL_ID,
                                                    NotificationConstants.DECLINE_KEY,
                                                    NotificationConstants.DECLINER_IDS)) {
      Object value = values.remove(required.getKey());
      assertFalse(plugin.isValid(ctx), required.getKey());
      values.put(required.getKey(), value);
    }
  }

  private void assertWebChannelSkipped(NotificationInfo notification) {
    assertTrue(notification.isRead());
    assertFalse(notification.isOnPopOver());
  }

  private NotificationInfo previous(String declinerIds) {
    NotificationInfo previous = NotificationInfo.instance()
                                                .setTo("root")
                                                .setFrom("john")
                                                .with(NotificationConstants.DECLINER_IDS.getKey(), declinerIds)
                                                .with(NotificationConstants.DECLINE_KEY.getKey(), DECLINE_KEY)
                                                .key(PluginKey.key(DeclinedCallPlugin.ID));
    previous.setId("42");
    // As the storage reads a notification the caller already read and closed: its states are parameters too
    previous.setRead(true);
    previous.setOnPopOver(false);
    previous.with(NotificationMessageUtils.READ_PORPERTY.getKey(), "true");
    previous.with(NotificationMessageUtils.SHOW_POPOVER_PROPERTY.getKey(), "false");
    when(webNotificationService.getNotificationInfos(any(WebNotificationFilter.class), anyInt(), anyInt())).thenReturn(List.of(previous));
    return previous;
  }

  private void stubDecline(String declinerIds) {
    values.put(NotificationConstants.CALLER_ID.getKey(), "root");
    values.put(NotificationConstants.DECLINER_IDS.getKey(), declinerIds);
    values.put(NotificationConstants.DECLINE_KEY.getKey(), DECLINE_KEY);
    values.put(NotificationConstants.CALL_ID.getKey(), "g_marketing");
    values.put(NotificationConstants.PROVIDER_TYPE.getKey(), "pexip");
    values.put(NotificationConstants.CALL_TYPE.getKey(), WebConferencingService.OWNER_TYPE_SPACE);
    values.put(NotificationConstants.OWNER_ID.getKey(), "marketing");
    values.put(NotificationConstants.CALL_OWNER.getKey(), "Marketing");
  }
}
