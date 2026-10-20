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

import java.util.List;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.webconferencing.notification.plugin.DeclinedCallPlugin;
import org.exoplatform.webconferencing.notification.utils.DeclinedCallNotificationUtils;

import io.meeds.pwa.model.PwaNotificationMessage;
import io.meeds.pwa.plugin.PwaNotificationPlugin;

/**
 * Builds the push of a declined call, as its web notification reads when the
 * push is sent: who declined it when only one participant did, how many did
 * otherwise.
 */
public class DeclinedCallPwaPlugin implements PwaNotificationPlugin {

  private static final String         ONE_DECLINER_LABEL_KEY      = "pwa.notification.webconferencing.declinedcall.one";

  private static final String         SEVERAL_DECLINERS_LABEL_KEY = "pwa.notification.webconferencing.declinedcall.several";

  private final ResourceBundleService resourceBundleService;

  private final IdentityManager       identityManager;

  public DeclinedCallPwaPlugin(ResourceBundleService resourceBundleService, IdentityManager identityManager) {
    this.resourceBundleService = resourceBundleService;
    this.identityManager = identityManager;
  }

  @Override
  public String getId() {
    return DeclinedCallPlugin.ID;
  }

  @Override
  public IdentityManager getIdentityManager() {
    return identityManager;
  }

  @Override
  public PwaNotificationMessage process(NotificationInfo notification, LocaleConfig localeConfig) {
    List<String> declinerIds = DeclinedCallNotificationUtils.getDeclinerIds(notification);
    String title;
    if (declinerIds.size() > 1) {
      title = resourceBundleService.getSharedString(SEVERAL_DECLINERS_LABEL_KEY, localeConfig.getLocale())
                                   .replace("{0}", String.valueOf(declinerIds.size()));
    } else {
      title = resourceBundleService.getSharedString(ONE_DECLINER_LABEL_KEY, localeConfig.getLocale())
                                   .replace("{0}", declinerIds.isEmpty() ? "" : getFullName(declinerIds.get(0)));
    }
    PwaNotificationMessage message = new PwaNotificationMessage();
    message.setTitle(title);
    message.setUrl(DeclinedCallNotificationUtils.getDeclineLink(notification));
    return message;
  }
}
