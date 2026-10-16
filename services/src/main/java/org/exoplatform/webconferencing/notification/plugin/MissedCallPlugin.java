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
package org.exoplatform.webconferencing.notification.plugin;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * Notifies a user who did not answer a call before its incoming call alert
 * timed out. The notification is sent from the user who started the call, and
 * carries the call, its provider and its owner: the owner type tells a 1:1 call
 * from a group one, whose title and avatar are kept as they were when the call
 * was missed.
 */
public class MissedCallPlugin extends BaseNotificationPlugin {

  public static final String ID = "MissedCallPlugin";

  public MissedCallPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    List<String> participants = ctx.value(NotificationConstants.CALL_PARTICIPANTS);
    return participants != null && !participants.isEmpty()
        && StringUtils.isNotBlank(ctx.value(NotificationConstants.CALLER_ID))
        && StringUtils.isNotBlank(ctx.value(NotificationConstants.CALL_ID));
  }

  @Override
  protected NotificationInfo makeNotification(NotificationContext ctx) {
    List<String> participants = ctx.value(NotificationConstants.CALL_PARTICIPANTS);
    String callerId = ctx.value(NotificationConstants.CALLER_ID);
    return NotificationInfo.instance()
                           .setFrom(callerId)
                           .to(participants)
                           .with(NotificationConstants.CALL_ID.getKey(), ctx.value(NotificationConstants.CALL_ID))
                           .with(NotificationConstants.PROVIDER_TYPE.getKey(), ctx.value(NotificationConstants.PROVIDER_TYPE))
                           .with(NotificationConstants.CALLER_ID.getKey(), callerId)
                           .with(NotificationConstants.CALL_TYPE.getKey(), ctx.value(NotificationConstants.CALL_TYPE))
                           .with(NotificationConstants.OWNER_ID.getKey(), ctx.value(NotificationConstants.OWNER_ID))
                           .with(NotificationConstants.CALL_OWNER.getKey(), ctx.value(NotificationConstants.CALL_OWNER))
                           .with(NotificationConstants.AVATAR_URL.getKey(), ctx.value(NotificationConstants.AVATAR_URL))
                           .key(getKey())
                           .end();
  }
}
