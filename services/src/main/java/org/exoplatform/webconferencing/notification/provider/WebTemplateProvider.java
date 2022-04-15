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
package org.exoplatform.webconferencing.notification.provider;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.webconferencing.notification.plugin.CallRecordingPlugin;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;
import org.exoplatform.webui.utils.TimeConvertUtils;

import java.io.Writer;
import java.util.Calendar;
import java.util.Locale;

@TemplateConfigs(templates = {
        @TemplateConfig(pluginId = CallRecordingPlugin.ID, template = "war:/notification/templates/web/CallRecordingPlugin.gtmpl") })
public class WebTemplateProvider extends TemplateProvider {
    public WebTemplateProvider(InitParams initParams) {
        super(initParams);
        this.templateBuilders.put(PluginKey.key(CallRecordingPlugin.ID), new TemplateBuilder());
    }
    private class TemplateBuilder extends AbstractTemplateBuilder {

        @Override
        protected MessageInfo makeMessage(NotificationContext notificationContext) {
            NotificationInfo notificationInfo = notificationContext.getNotificationInfo();
            String pluginId = notificationInfo.getKey().getId();
            String recordingStatus = notificationInfo.getValueOwnerParameter(NotificationConstants.RECORDING_STATUS.getKey());
            String fileName = notificationInfo.getValueOwnerParameter(NotificationConstants.FILE_NAME.getKey());
            String fileUrl = notificationInfo.getValueOwnerParameter(NotificationConstants.RECORDED_FILE_URL.getKey());
            String avatarUrl = notificationInfo.getValueOwnerParameter(NotificationConstants.AVATAR_URL.getKey());
            String callOwner = notificationInfo.getValueOwnerParameter(NotificationConstants.CALL_OWNER.getKey());
            String language = getLanguage(notificationInfo);
            TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), pluginId, language);
            templateContext.put("RECORDING_STATUS", recordingStatus);
            templateContext.put("FILE_NAME", fileName);
            templateContext.put("AVATAR", avatarUrl);
            templateContext.put("FILE_URL", fileUrl);
            templateContext.put("CALL_OWNER", callOwner);
            templateContext.put("USER", notificationInfo.getTo());
            Calendar lastModified = Calendar.getInstance();
            lastModified.setTimeInMillis(notificationInfo.getLastModifiedDate());
            templateContext.put("LAST_UPDATED_TIME",
                    TimeConvertUtils.convertXTimeAgoByTimeServer(lastModified.getTime(),
                            "EE, dd yyyy",
                            new Locale(language),
                            TimeConvertUtils.YEAR));
            boolean isRead =
                    Boolean.parseBoolean(notificationInfo.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey()));
            templateContext.put("READ", isRead ? "read" : "unread");
            templateContext.put("NOTIFICATION_ID", notificationInfo.getId());

            String body = TemplateUtils.processGroovy(templateContext);
            notificationContext.setException(templateContext.getException());
            MessageInfo messageInfo = new MessageInfo();
            return messageInfo.body(body).end();
        }

        @Override
        protected boolean makeDigest(NotificationContext notificationContext, Writer writer) {
            return false;
        }
    }
}
