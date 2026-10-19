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
package org.exoplatform.webconferencing.notification.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.webconferencing.WebConferencingService;

/**
 * A declined call leads to the profile of the participant who declined it when
 * only one did, to the home page otherwise; its declines are grouped per call start.
 */
class DeclinedCallNotificationUtilsTest {

  private MockedStatic<LinkProvider> links;

  private MockedStatic<CommonsUtils> services;

  @BeforeEach
  void setUp() {
    links = mockStatic(LinkProvider.class);
    services = mockStatic(CommonsUtils.class);
  }

  @AfterEach
  void tearDown() {
    links.close();
    services.close();
  }

  @Test
  void theDeclinesAreGroupedPerCallStart() {
    assertEquals("g_marketing@1700000000000", DeclinedCallNotificationUtils.getDeclineKey("g_marketing", new Date(1_700_000_000_000L)));
    assertEquals("g_marketing@0", DeclinedCallNotificationUtils.getDeclineKey("g_marketing", null));
  }

  @Test
  void theDeclinersAreReadInOrderOnceEach() {
    assertEquals(List.of("mary", "john"), DeclinedCallNotificationUtils.split("mary, john,,mary"));
    assertEquals(List.of(), DeclinedCallNotificationUtils.split(" "));
    assertEquals(List.of(), DeclinedCallNotificationUtils.split(null));
  }

  @Test
  void onlyTheClosedDeclinesAreClosed() {
    assertTrue(DeclinedCallNotificationUtils.isClosed(notification(WebConferencingService.OWNER_TYPE_USER, "john").with("DECLINES_CLOSED", "true")));
    assertFalse(DeclinedCallNotificationUtils.isClosed(notification(WebConferencingService.OWNER_TYPE_USER, "john")));
  }

  @Test
  void aSingleDeclineLeadsToTheProfileOfTheParticipant() {
    links.when(() -> LinkProvider.getUserProfileUri("john")).thenReturn("/portal/dw/profile/john");

    assertEquals("/portal/dw/profile/john",
                 DeclinedCallNotificationUtils.getDeclineLink(notification(WebConferencingService.OWNER_TYPE_SPACE, "john")));
  }

  @Test
  void severalDeclinesLeadToTheHomePage() {
    assertNull(DeclinedCallNotificationUtils.getDeclineLink(notification(WebConferencingService.OWNER_TYPE_SPACE, "mary,john")));
    assertNull(DeclinedCallNotificationUtils.getDeclineLink(notification(WebConferencingService.OWNER_TYPE_USER, "")));
  }

  @Test
  void anEmailOfSeveralDeclinesLinksTheHomePage() {
    try (MockedStatic<LinkProviderUtils> baseUrls = mockStatic(LinkProviderUtils.class)) {
      baseUrls.when(LinkProviderUtils::getBaseUrl).thenReturn("https://host/portal");

      assertEquals("https://host/portal",
                   DeclinedCallNotificationUtils.getAbsoluteDeclineLink(notification(WebConferencingService.OWNER_TYPE_SPACE, "mary,john")));
    }
  }

  @Test
  void anEmailLinksThePageOnTheServerDomain() {
    links.when(() -> LinkProvider.getUserProfileUri("john")).thenReturn("/portal/dw/profile/john");
    services.when(CommonsUtils::getCurrentDomain).thenReturn("https://host");

    assertEquals("https://host/portal/dw/profile/john",
                 DeclinedCallNotificationUtils.getAbsoluteDeclineLink(notification(WebConferencingService.OWNER_TYPE_USER, "john")));
  }

  private NotificationInfo notification(String callType, String declinerIds) {
    return NotificationInfo.instance()
                           .with(NotificationConstants.CALL_TYPE.getKey(), callType)
                           .with(NotificationConstants.OWNER_ID.getKey(), "marketing")
                           .with(NotificationConstants.DECLINER_IDS.getKey(), declinerIds);
  }
}
