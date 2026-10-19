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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.notification.LinkProviderUtils;

/**
 * What the web notification, the email and the push of a declined call need from its notification.
 */
public final class DeclinedCallNotificationUtils {

  private DeclinedCallNotificationUtils() {
  }

  /**
   * Gets the key grouping the declines of a call start: the call id, whose calls are started again under the
   * same id, and the start date.
   *
   * @param callId the call id
   * @param startedAt the start date of the call
   * @return the key
   */
  public static String getDeclineKey(String callId, Date startedAt) {
    return callId + "@" + (startedAt == null ? 0 : startedAt.getTime());
  }

  /**
   * Splits a comma separated list of user ids.
   *
   * @param userIds the user ids, comma separated
   * @return the user ids, empty for a blank value
   */
  public static List<String> split(String userIds) {
    if (StringUtils.isBlank(userIds)) {
      return List.of();
    }
    return Arrays.stream(userIds.split(",")).map(String::trim).filter(StringUtils::isNotBlank).distinct().toList();
  }

  /**
   * Gets the participants who declined the call, latest first.
   *
   * @param notification the declined call notification
   * @return the user ids
   */
  public static List<String> getDeclinerIds(NotificationInfo notification) {
    return split(notification.getValueOwnerParameter(NotificationConstants.DECLINER_IDS.getKey()));
  }

  /**
   * Tells if the notification reports the declines of a call start once its incoming call alert closed.
   *
   * @param notification the declined call notification
   * @return <code>true</code> once the alert closed
   */
  public static boolean isClosed(NotificationInfo notification) {
    return Boolean.parseBoolean(notification.getValueOwnerParameter(NotificationConstants.DECLINES_CLOSED.getKey()));
  }

  /**
   * Gets the page a declined call leads to, relative to the server: the profile of the participant who declined
   * it, when only one did.
   *
   * @param notification the declined call notification
   * @return the path of the page, or <code>null</code> for the home page
   */
  public static String getDeclineLink(NotificationInfo notification) {
    List<String> declinerIds = getDeclinerIds(notification);
    return declinerIds.size() == 1 ? LinkProvider.getUserProfileUri(declinerIds.get(0)) : null;
  }

  /**
   * Gets the absolute URL of the page a declined call leads to, for an email.
   *
   * @param notification the declined call notification
   * @return the URL of the page, the portal home page by default
   */
  public static String getAbsoluteDeclineLink(NotificationInfo notification) {
    String link = getDeclineLink(notification);
    return link == null ? LinkProviderUtils.getBaseUrl() : CommonsUtils.getCurrentDomain() + link;
  }
}
