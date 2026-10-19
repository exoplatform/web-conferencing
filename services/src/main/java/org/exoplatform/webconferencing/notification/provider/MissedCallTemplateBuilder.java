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
package org.exoplatform.webconferencing.notification.provider;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.model.ChannelKey;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.webconferencing.notification.utils.MissedCallNotificationUtils;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * Builds the email of a missed call: who called, and for a group call where.
 * The email cannot tell whether a group call is still running when it is read,
 * so it only states that the call was started.
 */
public class MissedCallTemplateBuilder extends AbstractTemplateBuilder {

  private final ChannelKey channelKey;

  public MissedCallTemplateBuilder(ChannelKey channelKey) {
    this.channelKey = channelKey;
  }

  @Override
  protected MessageInfo makeMessage(NotificationContext ctx) {
    NotificationInfo notification = ctx.getNotificationInfo();
    HTMLEntityEncoder encoder = HTMLEntityEncoder.getInstance();
    TemplateContext templateContext = TemplateContext.newChannelInstance(channelKey,
                                                                         notification.getKey().getId(),
                                                                         getLanguage(notification));
    String callerId = notification.getValueOwnerParameter(NotificationConstants.CALLER_ID.getKey());
    Identity caller = CommonsUtils.getService(IdentityManager.class).getOrCreateUserIdentity(callerId);
    String callerName = caller == null ? callerId : caller.getProfile().getFullName();
    String callOwner = notification.getValueOwnerParameter(NotificationConstants.CALL_OWNER.getKey());

    templateContext.put("USER", encoder.encode(notification.getTo()));
    templateContext.put("CALLER_NAME", encoder.encode(callerName));
    templateContext.put("CALLER_AVATAR", encoder.encode(LinkProviderUtils.getUserAvatarUrl(caller == null ? null : caller.getProfile())));
    templateContext.put("IS_GROUP", String.valueOf(MissedCallNotificationUtils.isGroupCall(notification)));
    templateContext.put("IS_ENDED", String.valueOf(MissedCallNotificationUtils.isEndedCall(notification)));
    templateContext.put("CALL_OWNER", encoder.encode(callOwner == null ? "" : callOwner));
    templateContext.put("CALL_LINK", encoder.encode(MissedCallNotificationUtils.getAbsoluteCallLink(notification)));
    templateContext.put("HAS_CALL_PAGE", String.valueOf(MissedCallNotificationUtils.getCallLink(notification) != null));
    templateContext.put("NOTIFICATION_ID", notification.getId());
    templateContext.put("COMPANY_LINK", LinkProviderUtils.getBaseUrl());

    String subject = TemplateUtils.processSubject(templateContext);
    String body = TemplateUtils.processGroovy(templateContext);
    ctx.setException(templateContext.getException());
    return new MessageInfo().subject(subject).body(body).end();
  }
}
