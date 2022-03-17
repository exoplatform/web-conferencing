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
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.webconferencing.notification.plugin.CallRecordingPlugin;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

import java.io.Writer;

@TemplateConfigs(templates = {
        @TemplateConfig(pluginId = CallRecordingPlugin.ID, template = "war:/notification/templates/mail/CallRecordingPlugin.gtmpl") })
public class MailTemplateProvider extends TemplateProvider {

    public MailTemplateProvider(InitParams initParams) {
        super(initParams);
        this.templateBuilders.put(PluginKey.key(CallRecordingPlugin.ID), new TemplateBuilder());}
    private class TemplateBuilder extends AbstractTemplateBuilder {

        @Override
        protected MessageInfo makeMessage(NotificationContext notificationContext) {
            NotificationInfo notificationInfo = notificationContext.getNotificationInfo();
            String pluginId = notificationInfo.getKey().getId();

            String language = getLanguage(notificationInfo);
            HTMLEntityEncoder encoder = HTMLEntityEncoder.getInstance();
            TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), pluginId, language);
            String recordingStatus = notificationInfo.getValueOwnerParameter(NotificationConstants.RECORDING_STATUS.getKey());
            String fileName = notificationInfo.getValueOwnerParameter(NotificationConstants.FILE_NAME.getKey());
            String fileUrl = notificationInfo.getValueOwnerParameter(NotificationConstants.RECORDED_FILE_URL.getKey());
            templateContext.put("RECORDING_STATUS", encoder.encode(recordingStatus));
            templateContext.put("FILE_URL", encoder.encode(fileUrl));
            templateContext.put("FILE_NAME", encoder.encode(fileName));
            String user = notificationInfo.getTo();
            templateContext.put("USER", encoder.encode(user));
            templateContext.put("NOTIFICATION_ID", notificationInfo.getId());
            String subject = TemplateUtils.processSubject(templateContext);
            String body = TemplateUtils.processGroovy(templateContext);

            notificationContext.setException(templateContext.getException());
            MessageInfo messageInfo = new MessageInfo();
                return messageInfo.subject(subject).body(body).end();
        }

        @Override
        protected boolean makeDigest(NotificationContext notificationContext, Writer writer) {
            return false;
        }
    }

}
