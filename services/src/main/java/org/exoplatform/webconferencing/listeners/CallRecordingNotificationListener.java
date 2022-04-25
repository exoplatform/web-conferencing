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
package org.exoplatform.webconferencing.listeners;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.services.listener.*;
import org.exoplatform.webconferencing.UserInfo;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.notification.plugin.CallRecordingPlugin;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CallRecordingNotificationListener extends Listener<CallInfo, Map<? extends String, ? extends String>> {

  private final IdentityManager        identityManager;

  private final WebConferencingService webConferencingService;

  private final SpaceService           spaceService;

  public CallRecordingNotificationListener(IdentityManager identityManager,
                                           WebConferencingService webConferencingService,
                                           SpaceService spaceService) {
    this.identityManager = identityManager;
    this.webConferencingService = webConferencingService;
    this.spaceService = spaceService;
  }
  
  @Override
  public void onEvent(Event<CallInfo, Map<? extends String, ? extends String>> event) throws Exception {
    CallInfo callInfo = event.getSource();
    Map<? extends String, ? extends String> info = event.getData();
    List<String> participants = callInfo.getParticipants().stream().map(UserInfo::getId).collect(Collectors.toList());
    String status = info.get("status");
    String fileName = info.get("file_name") == null ? "" : info.get("file_name");
    String identity = info.get("identity") == null ? null : info.get("identity");
    String callType = info.get("type") == null ? "" : info.get("type");
    String name = "";
    String avatarUrl = CommonsUtils.getCurrentDomain();
    if (callType.equals(WebConferencingService.OWNER_TYPE_SPACE)
        || callType.equals(WebConferencingService.OWNER_TYPE_SPACEEVENT)) {
      Space space;
      if (callType.equals(WebConferencingService.OWNER_TYPE_SPACEEVENT)) {
        Identity spaceIdentity = identityManager.getIdentity(identity);
        space = spaceService.getSpaceByPrettyName(spaceIdentity.getRemoteId());
      } else {
        space = spaceService.getSpaceByPrettyName(identity);
      }
      NotificationContext ctx = NotificationContextImpl.cloneInstance();
      String fileUrl = webConferencingService.getRecordingUrl(identity, fileName, callType);
      ctx.append(NotificationConstants.RECORDED_FILE_URL, fileUrl);
      ctx.append(NotificationConstants.CALL_PARTICIPANTS, participants);
      ctx.append(NotificationConstants.FILE_NAME, fileName);
      ctx.append(NotificationConstants.AVATAR_URL, space.getAvatarUrl());
      ctx.append(NotificationConstants.RECORDING_STATUS, status);
      ctx.append(NotificationConstants.CALL_TYPE, callType);
      ctx.append(NotificationConstants.CALL_OWNER, space.getDisplayName());
      ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(CallRecordingPlugin.ID))).execute(ctx);
    } else {
      if (callType.equals(WebConferencingService.OWNER_TYPE_USER)) {
        Identity ident = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, identity);
        if (ident != null) {
          name = ident.getProfile().getFullName();
          avatarUrl += ident.getProfile().getAvatarUrl();
        }
      } else {
        name = callInfo.getTitle();
        avatarUrl += "/chat/img/room-default.jpg";
      }
      for (String participant : participants) {
        NotificationContext ctx = NotificationContextImpl.cloneInstance();
        List<String> part = new ArrayList<>();
        part.add(participant);
        String fileUrl = webConferencingService.getRecordingUrl(participant, fileName, callType);
        ctx.append(NotificationConstants.FILE_NAME, fileName);
        ctx.append(NotificationConstants.RECORDING_STATUS, status);
        ctx.append(NotificationConstants.RECORDED_FILE_URL, fileUrl);
        ctx.append(NotificationConstants.CALL_PARTICIPANTS, part);
        ctx.append(NotificationConstants.AVATAR_URL, avatarUrl);
        ctx.append(NotificationConstants.CALL_TYPE, callType);
        ctx.append(NotificationConstants.CALL_OWNER, name);
        ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(CallRecordingPlugin.ID))).execute(ctx);
      }
    }

  }
}
