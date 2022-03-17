/***************************************************************************
 * Copyright (C) 2022 eXo Platform SAS.
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
 ***************************************************************************/
package org.exoplatform.webconferencing.notification.plugin;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

import java.util.List;

public class CallRecordingPlugin extends BaseNotificationPlugin {

    public static final  String ID  = "CallRecordingPlugin";

    public static final String RECEIVER = null;

    public CallRecordingPlugin(InitParams initParams) {
        super(initParams);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isValid(NotificationContext notificationContext) {
        return true;
    }

    @Override
    protected NotificationInfo makeNotification(NotificationContext notificationContext) {
        String recordingStatus= notificationContext.value(NotificationConstants.RECORDING_STATUS);
        String fileUrl = notificationContext.value(NotificationConstants.RECORDED_FILE_URL);
        String fileName = notificationContext.value((NotificationConstants.FILE_NAME));
        List<String> participants = notificationContext.value(NotificationConstants.CALL_PARTICIPANTS);
        return NotificationInfo.instance()
                    .to(participants)
                    .with(NotificationConstants.RECORDING_STATUS.getKey(), recordingStatus)
                    .with(NotificationConstants.FILE_NAME.getKey(), fileName)
                    .with(NotificationConstants.RECORDED_FILE_URL.getKey(), fileUrl)
                    .key(getKey())
                    .end();
    }
}
