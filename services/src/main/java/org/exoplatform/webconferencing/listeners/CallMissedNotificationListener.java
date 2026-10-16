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
package org.exoplatform.webconferencing.listeners;

import java.util.List;
import java.util.Map;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.IdentityInfo;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.notification.plugin.MissedCallPlugin;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * Sends the missed call notification of each participant for whom
 * {@link WebConferencingService#EVENT_CALL_MISSED} is broadcast.
 */
public class CallMissedNotificationListener extends Listener<CallInfo, Map<? extends String, ? extends String>> {

  @Override
  public void onEvent(Event<CallInfo, Map<? extends String, ? extends String>> event) throws Exception {
    CallInfo call = event.getSource();
    String userId = event.getData().get(WebConferencingService.EVENT_DATA_USER_ID);
    String callerId = event.getData().get(WebConferencingService.EVENT_DATA_CALLER_ID);
    IdentityInfo owner = call.getOwner();

    NotificationContext ctx = newNotificationContext();
    ctx.append(NotificationConstants.CALL_PARTICIPANTS, List.of(userId));
    ctx.append(NotificationConstants.CALLER_ID, callerId);
    ctx.append(NotificationConstants.CALL_ID, call.getId());
    ctx.append(NotificationConstants.PROVIDER_TYPE, call.getProviderType());
    ctx.append(NotificationConstants.CALL_TYPE, owner.getType());
    if (owner.isGroup()) {
      ctx.append(NotificationConstants.OWNER_ID, owner.getId());
      ctx.append(NotificationConstants.CALL_OWNER, owner.getTitle());
      ctx.append(NotificationConstants.AVATAR_URL, owner.getAvatarLink());
    }
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(MissedCallPlugin.ID))).execute(ctx);
  }

  protected NotificationContext newNotificationContext() {
    return NotificationContextImpl.cloneInstance();
  }
}
