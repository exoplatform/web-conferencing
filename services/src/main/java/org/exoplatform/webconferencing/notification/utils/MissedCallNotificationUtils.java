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

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.webconferencing.WebConferencingService;

/**
 * What the email and the push of a missed call need from its notification.
 */
public final class MissedCallNotificationUtils {

  private MissedCallNotificationUtils() {
  }

  /**
   * Tells if the missed call was a group call: any call but a 1:1 one.
   *
   * @param notification the missed call notification
   * @return <code>true</code> for a group call
   */
  public static boolean isGroupCall(NotificationInfo notification) {
    return !WebConferencingService.OWNER_TYPE_USER.equals(notification.getValueOwnerParameter(NotificationConstants.CALL_TYPE.getKey()));
  }

  /**
   * Tells if a missed group call had already ended when it was notified: it stopped before the participant
   * answered its incoming call alert.
   *
   * @param notification the missed call notification
   * @return <code>true</code> for a group call that had ended
   */
  public static boolean isEndedCall(NotificationInfo notification) {
    return Boolean.parseBoolean(notification.getValueOwnerParameter(NotificationConstants.CALL_ENDED.getKey()));
  }

  /**
   * Gets the page a missed call leads to, relative to the server: the caller's
   * profile for a 1:1 call, the space of a space call.
   *
   * @param notification the missed call notification
   * @return the path of the page, or <code>null</code> for the home page
   */
  public static String getCallLink(NotificationInfo notification) {
    String callType = notification.getValueOwnerParameter(NotificationConstants.CALL_TYPE.getKey());
    if (WebConferencingService.OWNER_TYPE_USER.equals(callType)) {
      return LinkProvider.getUserProfileUri(notification.getValueOwnerParameter(NotificationConstants.CALLER_ID.getKey()));
    } else if (WebConferencingService.OWNER_TYPE_SPACE.equals(callType)) {
      Space space = CommonsUtils.getService(SpaceService.class)
                                .getSpaceByPrettyName(notification.getValueOwnerParameter(NotificationConstants.OWNER_ID.getKey()));
      if (space != null) {
        try {
          return LinkProvider.getSpaceLink(space.getId());
        } catch (ObjectNotFoundException e) {
          // the space was removed meanwhile, the home page is used instead
        }
      }
    }
    return null;
  }

  /**
   * Gets the absolute URL of the page a missed call leads to, for an email.
   *
   * @param notification the missed call notification
   * @return the URL of the page, the portal home page by default
   */
  public static String getAbsoluteCallLink(NotificationInfo notification) {
    String link = getCallLink(notification);
    return link == null ? LinkProviderUtils.getBaseUrl() : CommonsUtils.getCurrentDomain() + link;
  }
}
