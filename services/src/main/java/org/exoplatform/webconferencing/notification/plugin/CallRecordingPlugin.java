/*
 * Copyright (C) 2022 eXo Platform SAS.
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

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class CallRecordingPlugin extends BaseNotificationPlugin {

    public static final  String ID  = "CallRecordingPlugin";

    public static final String RECEIVER = null;

    /**
     * Creates the plugin.
     *
     * @param initParams the plugin's parameters
     */
    public CallRecordingPlugin(InitParams initParams) {
        super(initParams);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(NotificationContext notificationContext) {
        return true;
    }

    /**
     * Builds the notification sent to the recipients chosen by the listener,
     * carrying the recording's status, file, link, the meeting's owner, title
     * and start date.
     *
     * @param notificationContext the context the listener filled
     * @return the notification
     */
    @Override
    protected NotificationInfo makeNotification(NotificationContext notificationContext) {
        String recordingStatus= notificationContext.value(NotificationConstants.RECORDING_STATUS);
        String fileUrl = notificationContext.value(NotificationConstants.RECORDED_FILE_URL);
        String fileName = notificationContext.value((NotificationConstants.FILE_NAME));
        String avatarUrl = notificationContext.value((NotificationConstants.AVATAR_URL));
        String callOwner = notificationContext.value(NotificationConstants.CALL_OWNER);
        String callTitle = notificationContext.value(NotificationConstants.CALL_TITLE);
        String callStartDate = notificationContext.value(NotificationConstants.CALL_START_DATE);
        List<String> participants = notificationContext.value(NotificationConstants.CALL_PARTICIPANTS);
        NotificationInfo notification = NotificationInfo.instance()
                    .to(participants)
                    .with(NotificationConstants.RECORDING_STATUS.getKey(), recordingStatus)
                    .with(NotificationConstants.FILE_NAME.getKey(), fileName)
                    .with(NotificationConstants.RECORDED_FILE_URL.getKey(), fileUrl)
                    .with(NotificationConstants.AVATAR_URL.getKey(), avatarUrl)
                    .with(NotificationConstants.CALL_OWNER.getKey(), callOwner);
        if (StringUtils.isNotBlank(callTitle)) {
          notification.with(NotificationConstants.CALL_TITLE.getKey(), callTitle);
        }
        if (StringUtils.isNotBlank(callStartDate)) {
          notification.with(NotificationConstants.CALL_START_DATE.getKey(), callStartDate);
        }
        return notification.key(getKey()).end();
    }
}
