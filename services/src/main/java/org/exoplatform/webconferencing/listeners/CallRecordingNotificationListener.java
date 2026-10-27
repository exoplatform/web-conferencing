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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

/**
 * Sends the « recording available » (or « recording failed ») notification when
 * a call recording has been uploaded. A space or space-event recording is saved
 * in the space's Documents, so it is announced only to the participants who are
 * members of that space; a chat-room or one-to-one recording is shared with each
 * participant, so each of them is notified.
 */
public class CallRecordingNotificationListener extends Listener<CallInfo, Map<? extends String, ? extends String>> {

  private static final Log             LOG = ExoLogger.getLogger(CallRecordingNotificationListener.class);

  private final IdentityManager        identityManager;

  private final WebConferencingService webConferencingService;

  private final SpaceService           spaceService;

  /**
   * Creates the listener.
   *
   * @param identityManager resolves the space-event and user identities
   * @param webConferencingService builds the recording's URL
   * @param spaceService resolves the call's space and its membership
   */
  public CallRecordingNotificationListener(IdentityManager identityManager,
                                           WebConferencingService webConferencingService,
                                           SpaceService spaceService) {
    this.identityManager = identityManager;
    this.webConferencingService = webConferencingService;
    this.spaceService = spaceService;
  }

  /**
   * Builds and sends the recording notification of the recorded call, with the
   * meeting's title and start date.
   *
   * @param event the recorded event: the call as source, the upload information
   *          (status, file name, owner identity and type) as data
   * @throws Exception when the recording URL cannot be built
   */
  @Override
  public void onEvent(Event<CallInfo, Map<? extends String, ? extends String>> event) throws Exception {

    CallInfo callInfo = event.getSource();
    if (callInfo == null) {
      LOG.warn("No call found for a recording event, no recording notification sent");
      return;
    }
    Map<? extends String, ? extends String> info = event.getData();
    List<String> participants = callInfo.getParticipants().stream().map(UserInfo::getId).collect(Collectors.toList());
    String status = info.get("status");
    String fileName = info.get("file_name") == null ? "" : info.get("file_name");
    String identity = info.get("identity") == null ? null : info.get("identity");
    String callType = info.get("type") == null ? "" : info.get("type");
    String callStartDate = getCallStartDate(callInfo);
    String name = "";
    String avatarUrl = CommonsUtils.getCurrentDomain();
    if (callType.equals(WebConferencingService.OWNER_TYPE_SPACE) || callType.equals(WebConferencingService.OWNER_TYPE_SPACEEVENT)) {
      Space space;
      if (callType.equals(WebConferencingService.OWNER_TYPE_SPACEEVENT)) {
        Identity spaceIdentity = identityManager.getIdentity(identity);
        space = spaceIdentity == null ? null : spaceService.getSpaceByPrettyName(spaceIdentity.getRemoteId());
      } else {
        space = spaceService.getSpaceByPrettyName(identity);
      }
      if (space == null) {
        LOG.warn("Space of call {} not found, no recording notification sent", callInfo.getId());
        return;
      }
      List<String> recipients = getSpaceMembers(space, participants);
      if (recipients.isEmpty()) {
        LOG.debug("No participant of call {} is a member of space {}, no recording notification sent",
                  callInfo.getId(),
                  space.getPrettyName());
        return;
      }
      String callTitle = StringUtils.isBlank(callInfo.getTitle()) ? space.getDisplayName() : callInfo.getTitle();
      NotificationContext ctx = newNotificationContext();
      String fileUrl = webConferencingService.getRecordingUrl(identity, fileName, callType);
      fileUrl=fileUrl.replace(CommonsUtils.getCurrentDomain(),"");
      ctx.append(NotificationConstants.RECORDED_FILE_URL, fileUrl);
      ctx.append(NotificationConstants.CALL_PARTICIPANTS, recipients);
      ctx.append(NotificationConstants.FILE_NAME, fileName);
      ctx.append(NotificationConstants.AVATAR_URL, space.getAvatarUrl());
      ctx.append(NotificationConstants.RECORDING_STATUS, status);
      ctx.append(NotificationConstants.CALL_TYPE, callType);
      ctx.append(NotificationConstants.CALL_OWNER, space.getDisplayName());
      ctx.append(NotificationConstants.CALL_TITLE, callTitle);
      ctx.append(NotificationConstants.CALL_START_DATE, callStartDate);
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
        NotificationContext ctx = newNotificationContext();
        List<String> part = new ArrayList<>();
        part.add(participant);
        String fileUrl = webConferencingService.getRecordingUrl(participant, fileName, callType);
        fileUrl=fileUrl.replace(CommonsUtils.getCurrentDomain(),"");
        ctx.append(NotificationConstants.FILE_NAME, fileName);
        ctx.append(NotificationConstants.RECORDING_STATUS, status);
        ctx.append(NotificationConstants.RECORDED_FILE_URL, fileUrl);
        ctx.append(NotificationConstants.CALL_PARTICIPANTS, part);
        ctx.append(NotificationConstants.AVATAR_URL, avatarUrl);
        ctx.append(NotificationConstants.CALL_TYPE, callType);
        ctx.append(NotificationConstants.CALL_OWNER, name);
        ctx.append(NotificationConstants.CALL_TITLE, name);
        ctx.append(NotificationConstants.CALL_START_DATE, callStartDate);
        ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(CallRecordingPlugin.ID))).execute(ctx);
      }
    }

  }

  /**
   * Creates the context of one notification.
   *
   * @return a new notification context
   */
  protected NotificationContext newNotificationContext() {
    return NotificationContextImpl.cloneInstance();
  }

  /**
   * Keeps, among the call's participants, only the members of the space: a
   * space or space-event recording is saved in the space's Documents, which a
   * non-member (an attendee invited directly, a guest) cannot open.
   *
   * @param space the space the recording is saved in
   * @param participants the ids of the call's participants
   * @return the participants who are members of the space, in the same order
   */
  private List<String> getSpaceMembers(Space space, List<String> participants) {
    if (participants == null || participants.isEmpty()) {
      return Collections.emptyList();
    }
    return participants.stream()
                       .filter(participant -> StringUtils.isNotBlank(participant) && spaceService.isMember(space, participant))
                       .collect(Collectors.toList());
  }

  /**
   * Gives the moment the recorded meeting started: the last time the call was
   * started, which is the session the recording belongs to, else its planned
   * start date (set once, from the planned event, when the call is created).
   *
   * @param callInfo the recorded call
   * @return the start date in milliseconds since the epoch, as a string, or
   *         {@code null} when the call carries no date
   */
  private String getCallStartDate(CallInfo callInfo) {
    Date startDate = callInfo.getLastDate() != null ? callInfo.getLastDate() : callInfo.getStartDate();
    return startDate == null ? null : String.valueOf(startDate.getTime());
  }
}
