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
package org.exoplatform.webconferencing.notification.pwa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.notification.plugin.MissedCallPlugin;
import org.exoplatform.webconferencing.notification.utils.MissedCallNotificationUtils;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * The push of a missed call names the caller, and the group for a group call,
 * and leads to the page of the call.
 */
class MissedCallPwaPluginTest {

  private MissedCallPwaPlugin plugin;

  private LocaleConfig        localeConfig;

  @BeforeEach
  void setUp() {
    ResourceBundleService resourceBundleService = mock(ResourceBundleService.class);
    when(resourceBundleService.getSharedString("pwa.notification.webconferencing.missedcall.user",
                                               Locale.ENGLISH)).thenReturn("{0} has tried to call you");
    when(resourceBundleService.getSharedString("pwa.notification.webconferencing.missedcall.group",
                                               Locale.ENGLISH)).thenReturn("{0} has started a group call in {1}");
    when(resourceBundleService.getSharedString("pwa.notification.webconferencing.missedcall.ended",
                                               Locale.ENGLISH)).thenReturn("The call has ended");
    IdentityManager identityManager = mock(IdentityManager.class);
    Identity caller = new Identity("1");
    Profile profile = new Profile(caller);
    profile.setProperty(Profile.FULL_NAME, "Root Root");
    caller.setProfile(profile);
    when(identityManager.getOrCreateUserIdentity("root")).thenReturn(caller);
    plugin = new MissedCallPwaPlugin(resourceBundleService, identityManager);
    localeConfig = mock(LocaleConfig.class);
    when(localeConfig.getLocale()).thenReturn(Locale.ENGLISH);
  }

  @Test
  void aMissedUserCallNamesTheCaller() {
    NotificationInfo notification = notification(WebConferencingService.OWNER_TYPE_USER, null);

    try (MockedStatic<MissedCallNotificationUtils> utils = mockLink(notification, "/portal/dw/profile/root")) {
      var message = plugin.process(notification, localeConfig);

      assertEquals("Root Root has tried to call you", message.getTitle());
      assertEquals("/portal/dw/profile/root", message.getUrl());
    }
  }

  @Test
  void aMissedGroupCallNamesTheCallerAndTheGroup() {
    NotificationInfo notification = notification(WebConferencingService.OWNER_TYPE_SPACE, "Marketing");

    try (MockedStatic<MissedCallNotificationUtils> utils = mockLink(notification, "/portal/s/1")) {
      var message = plugin.process(notification, localeConfig);

      assertEquals("Root Root has started a group call in Marketing", message.getTitle());
      assertEquals("/portal/s/1", message.getUrl());
      assertNull(message.getBody());
    }
  }

  @Test
  void aMissedGroupCallThatEndedSaysSo() {
    NotificationInfo notification = notification(WebConferencingService.OWNER_TYPE_SPACE, "Marketing");
    notification.with(NotificationConstants.CALL_ENDED.getKey(), "true");

    try (MockedStatic<MissedCallNotificationUtils> utils = mockLink(notification, "/portal/s/1")) {
      var message = plugin.process(notification, localeConfig);

      assertEquals("Root Root has started a group call in Marketing", message.getTitle());
      assertEquals("The call has ended", message.getBody());
    }
  }

  @Test
  void thePushHandlesTheMissedCallNotification() {
    assertEquals(MissedCallPlugin.ID, plugin.getId());
  }

  private MockedStatic<MissedCallNotificationUtils> mockLink(NotificationInfo notification, String link) {
    MockedStatic<MissedCallNotificationUtils> utils = mockStatic(MissedCallNotificationUtils.class);
    utils.when(() -> MissedCallNotificationUtils.isGroupCall(notification))
         .thenReturn(!WebConferencingService.OWNER_TYPE_USER.equals(notification.getValueOwnerParameter(NotificationConstants.CALL_TYPE.getKey())));
    utils.when(() -> MissedCallNotificationUtils.getCallLink(notification)).thenReturn(link);
    utils.when(() -> MissedCallNotificationUtils.isEndedCall(notification)).thenCallRealMethod();
    return utils;
  }

  private NotificationInfo notification(String callType, String callOwner) {
    NotificationInfo notification = NotificationInfo.instance()
                                                    .key(MissedCallPlugin.ID)
                                                    .with(NotificationConstants.CALLER_ID.getKey(), "root")
                                                    .with(NotificationConstants.CALL_TYPE.getKey(), callType);
    if (callOwner != null) {
      notification.with(NotificationConstants.CALL_OWNER.getKey(), callOwner);
    }
    return notification;
  }
}
