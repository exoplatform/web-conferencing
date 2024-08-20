/*
 * Copyright (C) 2024 eXo Platform SAS.
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

import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.pwa.model.PwaNotificationMessage;
import io.meeds.pwa.plugin.PwaNotificationPlugin;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

public class CallRecordingPwaPlugin implements PwaNotificationPlugin {

    public static final  String ID  = "CallRecordingPlugin";

    private static final String TITLE_LABEL_KEY_OK = "pwa.notification.webconferencing.callrecording.success";
    private static final String TITLE_LABEL_KEY_KO = "pwa.notification.webconferencing.callrecording.failed";

    private static final Log LOG = ExoLogger.getLogger(PwaNotificationPlugin.class);

    private ResourceBundleService resourceBundleService;

    public CallRecordingPwaPlugin(ResourceBundleService resourceBundleService) {
        this.resourceBundleService = resourceBundleService;
    }

    @Override
    public String getId() {
        return ID ;
    }

    @Override
    public PwaNotificationMessage process(NotificationInfo notification, LocaleConfig localeConfig) {
        PwaNotificationMessage notificationMessage = new PwaNotificationMessage();

        String status = notification.getValueOwnerParameter(NotificationConstants.RECORDING_STATUS.getKey());
        String title;
        if (status.equals("ok")) {
            title = resourceBundleService.getSharedString(TITLE_LABEL_KEY_OK, localeConfig.getLocale());
        } else {
            title = resourceBundleService.getSharedString(TITLE_LABEL_KEY_KO, localeConfig.getLocale());

        }

        notificationMessage.setTitle(title);
        notificationMessage.setUrl(notification.getValueOwnerParameter(NotificationConstants.RECORDED_FILE_URL.getKey()));
        return notificationMessage;
    }

}
