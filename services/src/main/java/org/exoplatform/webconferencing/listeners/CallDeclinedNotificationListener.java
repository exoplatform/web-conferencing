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

import java.util.Map;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.IdentityInfo;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.notification.plugin.DeclinedCallPlugin;
import org.exoplatform.webconferencing.notification.utils.DeclinedCallNotificationUtils;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * Sends the caller the declined call notification: on each decline of a call
 * start ({@link WebConferencingService#EVENT_CALL_DECLINED}), and once its
 * incoming call alert closed with the declines it got
 * ({@link WebConferencingService#EVENT_CALL_DECLINES_CLOSED}). The declines of
 * a call start are grouped by its start date, as stored with the call.
 */
public class CallDeclinedNotificationListener extends Listener<CallInfo, Map<? extends String, ? extends String>> {

  @Override
  public void onEvent(Event<CallInfo, Map<? extends String, ? extends String>> event) throws Exception {
    CallInfo call = event.getSource();
    Map<? extends String, ? extends String> data = event.getData();
    boolean closed = WebConferencingService.EVENT_CALL_DECLINES_CLOSED.equals(event.getEventName());
    IdentityInfo owner = call.getOwner();
    NotificationContext ctx = newNotificationContext();
    ctx.append(NotificationConstants.CALLER_ID, data.get(WebConferencingService.EVENT_DATA_USER_ID));
    ctx.append(NotificationConstants.DECLINER_IDS,
               closed ? data.get(WebConferencingService.EVENT_DATA_DECLINER_IDS)
                      : data.get(WebConferencingService.EVENT_DATA_DECLINER_ID));
    ctx.append(NotificationConstants.DECLINE_KEY, DeclinedCallNotificationUtils.getDeclineKey(call.getId(), call.getLastDate()));
    ctx.append(NotificationConstants.CALL_ID, call.getId());
    ctx.append(NotificationConstants.PROVIDER_TYPE, call.getProviderType());
    ctx.append(NotificationConstants.CALL_TYPE, owner.getType());
    if (closed) {
      ctx.append(NotificationConstants.DECLINES_CLOSED, Boolean.TRUE.toString());
    }
    if (owner.isGroup()) {
      ctx.append(NotificationConstants.OWNER_ID, owner.getId());
      ctx.append(NotificationConstants.CALL_OWNER, owner.getTitle());
      ctx.append(NotificationConstants.AVATAR_URL, owner.getAvatarLink());
    }
    ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(DeclinedCallPlugin.ID))).execute(ctx);
  }

  protected NotificationContext newNotificationContext() {
    return NotificationContextImpl.cloneInstance();
  }
}
