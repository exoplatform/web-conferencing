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

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.webconferencing.notification.plugin.MissedCallPlugin;
import org.exoplatform.webconferencing.notification.utils.MissedCallNotificationUtils;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

import io.meeds.pwa.model.PwaNotificationMessage;
import io.meeds.pwa.plugin.PwaNotificationPlugin;

/**
 * Builds the push of a missed call: who called, and for a group call where. A
 * push cannot tell whether a group call is still running when it is read, so it
 * only states that the call was started.
 */
public class MissedCallPwaPlugin implements PwaNotificationPlugin {

  private static final String         USER_CALL_LABEL_KEY  = "pwa.notification.webconferencing.missedcall.user";

  private static final String         GROUP_CALL_LABEL_KEY = "pwa.notification.webconferencing.missedcall.group";

  private final ResourceBundleService resourceBundleService;

  private final IdentityManager       identityManager;

  public MissedCallPwaPlugin(ResourceBundleService resourceBundleService, IdentityManager identityManager) {
    this.resourceBundleService = resourceBundleService;
    this.identityManager = identityManager;
  }

  @Override
  public String getId() {
    return MissedCallPlugin.ID;
  }

  @Override
  public IdentityManager getIdentityManager() {
    return identityManager;
  }

  @Override
  public PwaNotificationMessage process(NotificationInfo notification, LocaleConfig localeConfig) {
    String callerName = getFullName(notification.getValueOwnerParameter(NotificationConstants.CALLER_ID.getKey()));
    boolean groupCall = MissedCallNotificationUtils.isGroupCall(notification);
    String title = resourceBundleService.getSharedString(groupCall ? GROUP_CALL_LABEL_KEY : USER_CALL_LABEL_KEY,
                                                         localeConfig.getLocale());
    title = title.replace("{0}", callerName);
    if (groupCall) {
      String callOwner = notification.getValueOwnerParameter(NotificationConstants.CALL_OWNER.getKey());
      title = title.replace("{1}", callOwner == null ? "" : callOwner);
    }
    PwaNotificationMessage message = new PwaNotificationMessage();
    message.setTitle(title);
    message.setUrl(MissedCallNotificationUtils.getCallLink(notification));
    return message;
  }
}
