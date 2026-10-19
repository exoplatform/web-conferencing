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

import java.util.List;

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
import org.exoplatform.webconferencing.notification.utils.DeclinedCallNotificationUtils;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * Builds the email of a declined call, sent once its incoming call alert closed:
 * who declined it when only one participant did, with their avatar; how many
 * did otherwise, naming nobody, with the avatar of the call's group. A
 * notification of a single decline builds no email.
 */
public class DeclinedCallTemplateBuilder extends AbstractTemplateBuilder {

  private final ChannelKey channelKey;

  public DeclinedCallTemplateBuilder(ChannelKey channelKey) {
    this.channelKey = channelKey;
  }

  @Override
  protected MessageInfo makeMessage(NotificationContext ctx) {
    NotificationInfo notification = ctx.getNotificationInfo();
    List<String> declinerIds = DeclinedCallNotificationUtils.getDeclinerIds(notification);
    if (!DeclinedCallNotificationUtils.isClosed(notification) || declinerIds.isEmpty()) {
      return null;
    }
    HTMLEntityEncoder encoder = HTMLEntityEncoder.getInstance();
    TemplateContext templateContext = TemplateContext.newChannelInstance(channelKey,
                                                                         notification.getKey().getId(),
                                                                         getLanguage(notification));
    String declinerName = "";
    String avatarUrl;
    if (declinerIds.size() == 1) {
      Identity decliner = CommonsUtils.getService(IdentityManager.class).getOrCreateUserIdentity(declinerIds.get(0));
      declinerName = decliner == null ? declinerIds.get(0) : decliner.getProfile().getFullName();
      avatarUrl = LinkProviderUtils.getUserAvatarUrl(decliner == null ? null : decliner.getProfile());
    } else {
      // Several declines name nobody: the email shows the call's group, as the web notification does
      avatarUrl = getGroupAvatarUrl(notification);
    }
    templateContext.put("USER", encoder.encode(notification.getTo()));
    templateContext.put("DECLINER_NAME", encoder.encode(declinerName));
    templateContext.put("DECLINER_AVATAR", encoder.encode(avatarUrl));
    templateContext.put("DECLINERS_COUNT", String.valueOf(declinerIds.size()));
    templateContext.put("CALL_LINK", encoder.encode(DeclinedCallNotificationUtils.getAbsoluteDeclineLink(notification)));
    templateContext.put("NOTIFICATION_ID", notification.getId());
    templateContext.put("COMPANY_LINK", LinkProviderUtils.getBaseUrl());
    String subject = TemplateUtils.processSubject(templateContext);
    String body = TemplateUtils.processGroovy(templateContext);
    ctx.setException(templateContext.getException());
    return new MessageInfo().subject(subject).body(body).end();
  }

  private String getGroupAvatarUrl(NotificationInfo notification) {
    String avatarUrl = notification.getValueOwnerParameter(NotificationConstants.AVATAR_URL.getKey());
    if (avatarUrl == null || avatarUrl.isBlank()) {
      return LinkProviderUtils.getUserAvatarUrl(null);
    }
    return avatarUrl.startsWith("/") ? CommonsUtils.getCurrentDomain() + avatarUrl : avatarUrl;
  }
}
