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
import static org.mockito.Mockito.CALLS_REAL_METHODS;
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
import org.exoplatform.webconferencing.notification.plugin.DeclinedCallPlugin;
import org.exoplatform.webconferencing.notification.utils.DeclinedCallNotificationUtils;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * The push of a declined call reads as its web notification when sent: the
 * participant who declined when only one did, their number otherwise.
 */
class DeclinedCallPwaPluginTest {

  private DeclinedCallPwaPlugin plugin;

  private LocaleConfig          localeConfig;

  @BeforeEach
  void setUp() {
    ResourceBundleService resourceBundleService = mock(ResourceBundleService.class);
    when(resourceBundleService.getSharedString("pwa.notification.webconferencing.declinedcall.one",
                                               Locale.ENGLISH)).thenReturn("{0} has declined your call");
    when(resourceBundleService.getSharedString("pwa.notification.webconferencing.declinedcall.several",
                                               Locale.ENGLISH)).thenReturn("{0} participants have declined your call");
    IdentityManager identityManager = mock(IdentityManager.class);
    Identity john = new Identity("1");
    Profile profile = new Profile(john);
    profile.setProperty(Profile.FULL_NAME, "John Smith");
    john.setProfile(profile);
    when(identityManager.getOrCreateUserIdentity("john")).thenReturn(john);
    plugin = new DeclinedCallPwaPlugin(resourceBundleService, identityManager);
    localeConfig = mock(LocaleConfig.class);
    when(localeConfig.getLocale()).thenReturn(Locale.ENGLISH);
  }

  @Test
  void itIsThePushOfTheDeclinedCallNotification() {
    assertEquals(DeclinedCallPlugin.ID, plugin.getId());
  }

  @Test
  void aSingleDeclineNamesTheParticipantAndLeadsToTheirProfile() {
    NotificationInfo notification = notification("john");

    try (MockedStatic<DeclinedCallNotificationUtils> utils = mockLink(notification, "/portal/dw/profile/john")) {
      var message = plugin.process(notification, localeConfig);

      assertEquals("John Smith has declined your call", message.getTitle());
      assertEquals("/portal/dw/profile/john", message.getUrl());
    }
  }

  @Test
  void severalDeclinesAreCountedWithoutNamingTheParticipants() {
    NotificationInfo notification = notification("mary,john,paul");

    try (MockedStatic<DeclinedCallNotificationUtils> utils = mockLink(notification, "/portal/s/1")) {
      var message = plugin.process(notification, localeConfig);

      assertEquals("3 participants have declined your call", message.getTitle());
      assertEquals("/portal/s/1", message.getUrl());
    }
  }

  private MockedStatic<DeclinedCallNotificationUtils> mockLink(NotificationInfo notification, String link) {
    MockedStatic<DeclinedCallNotificationUtils> utils = mockStatic(DeclinedCallNotificationUtils.class, CALLS_REAL_METHODS);
    utils.when(() -> DeclinedCallNotificationUtils.getDeclineLink(notification)).thenReturn(link);
    return utils;
  }

  private NotificationInfo notification(String declinerIds) {
    return NotificationInfo.instance().setTo("root").with(NotificationConstants.DECLINER_IDS.getKey(), declinerIds);
  }
}
