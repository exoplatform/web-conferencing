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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webconferencing.WebConferencingService;

/**
 * A missed 1:1 call leads to the caller's profile, a missed space call to the
 * space, any other call to the home page.
 */
class MissedCallNotificationUtilsTest {

  private MockedStatic<LinkProvider> links;

  private MockedStatic<CommonsUtils> services;

  private SpaceService               spaceService;

  @BeforeEach
  void setUp() {
    links = mockStatic(LinkProvider.class);
    services = mockStatic(CommonsUtils.class);
    spaceService = mock(SpaceService.class);
    services.when(() -> CommonsUtils.getService(SpaceService.class)).thenReturn(spaceService);
  }

  @AfterEach
  void tearDown() {
    links.close();
    services.close();
  }

  @Test
  void aMissedUserCallLeadsToTheCallerProfile() {
    links.when(() -> LinkProvider.getUserProfileUri("root")).thenReturn("/portal/dw/profile/root");

    assertEquals("/portal/dw/profile/root", MissedCallNotificationUtils.getCallLink(notification(WebConferencingService.OWNER_TYPE_USER)));
  }

  @Test
  void aMissedSpaceCallLeadsToTheSpace() {
    Space space = new Space();
    space.setId("12");
    when(spaceService.getSpaceByPrettyName("marketing")).thenReturn(space);
    links.when(() -> LinkProvider.getSpaceLink("12")).thenReturn("/portal/s/12");

    assertEquals("/portal/s/12", MissedCallNotificationUtils.getCallLink(notification(WebConferencingService.OWNER_TYPE_SPACE)));
  }

  @Test
  void aMissedCallOfARemovedSpaceLeadsToTheHomePage() {
    Space space = new Space();
    space.setId("12");
    when(spaceService.getSpaceByPrettyName("marketing")).thenReturn(space);
    links.when(() -> LinkProvider.getSpaceLink("12")).thenThrow(new ObjectNotFoundException("removed"));

    assertEquals("/", MissedCallNotificationUtils.getCallLink(notification(WebConferencingService.OWNER_TYPE_SPACE)));
    assertEquals("/", MissedCallNotificationUtils.getCallLink(notification(WebConferencingService.OWNER_TYPE_CHATROOM)));
  }

  @Test
  void onlyAUserCallIsNotAGroupCall() {
    assertFalse(MissedCallNotificationUtils.isGroupCall(notification(WebConferencingService.OWNER_TYPE_USER)));
    assertTrue(MissedCallNotificationUtils.isGroupCall(notification(WebConferencingService.OWNER_TYPE_SPACE)));
    assertTrue(MissedCallNotificationUtils.isGroupCall(notification(WebConferencingService.OWNER_TYPE_CHATROOM)));
  }

  private NotificationInfo notification(String callType) {
    return NotificationInfo.instance()
                           .with(NotificationConstants.CALLER_ID.getKey(), "root")
                           .with(NotificationConstants.CALL_TYPE.getKey(), callType)
                           .with(NotificationConstants.OWNER_ID.getKey(), "marketing");
  }
}
