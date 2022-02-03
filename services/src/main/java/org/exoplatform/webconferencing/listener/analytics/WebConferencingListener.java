package org.exoplatform.webconferencing.listener.analytics;

import static org.exoplatform.analytics.utils.AnalyticsUtils.*;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.codec.binary.StringUtils;

import org.exoplatform.analytics.model.StatisticData;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.listener.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webconferencing.*;
import org.exoplatform.webconferencing.WebConferencingService.SpaceEventInfo;
import org.exoplatform.webconferencing.WebConferencingService.SpaceInfo;

@Asynchronous
public class WebConferencingListener extends Listener<CallInfo, String> {

  private static final Log LOG = ExoLogger.getLogger(WebConferencingListener.class);

  private ExoContainer     container;

  private SpaceService     spaceService;

  public WebConferencingListener(PortalContainer container) {
    this.container = container;
  }

  @Override
  public void onEvent(Event<CallInfo, String> event) throws Exception {
    CallInfo callInfo = event.getSource();
    String username = event.getData();

    if (callInfo == null) {
      LOG.warn("Call information is null.");
      return;
    }

    long userIdentityId = getUserIdentityId(username);
    if (userIdentityId <= 0) {
      LOG.warn("User '{}' identity identifier can't be found.", username);
      return;
    }

    StatisticData statisticData = new StatisticData();
    statisticData.setModule(callInfo.getProviderType());
    statisticData.setSubModule("web-conferencing");
    statisticData.setUserId(userIdentityId);

    long callDuration = 0;

    String operation = null;
    switch (event.getEventName()) {
      case WebConferencingService.EVENT_CALL_CREATED:
        operation = "callCreated";
        break;
      case WebConferencingService.EVENT_CALL_JOINDED:
        operation = "callJoined";
        if (callInfo.getLastDate() != null) {
          callDuration = (System.currentTimeMillis() - callInfo.getLastDate().getTime()) / 1000;
        }
        break;
      case WebConferencingService.EVENT_CALL_LEFT:
        operation = "callLeft";
        if (callInfo.getLastDate() != null) {
          callDuration = (System.currentTimeMillis() - callInfo.getLastDate().getTime()) / 1000;
        }
        break;
      case WebConferencingService.EVENT_CALL_RECORDED:
        operation = "callRecorded";
        if (callInfo.getLastDate() != null) {
          callDuration = (System.currentTimeMillis() - callInfo.getLastDate().getTime()) / 1000;
        }
        break;
      case WebConferencingService.EVENT_CALL_STARTED:
        operation = "callStarted";
        break;
      case WebConferencingService.EVENT_CALL_STOPPED:
        operation = "callStopped";
        if (callInfo.getLastDate() != null) {
          callDuration = (System.currentTimeMillis() - callInfo.getLastDate().getTime()) / 1000;
        }
        break;
      default:
        break;
    }
    statisticData.setOperation(operation);
    IdentityInfo callOwner = callInfo.getOwner();
    String callType = null;
    if (callOwner instanceof SpaceInfo) {
      Space space = getSpaceService().getSpaceByGroupId(((SpaceInfo) callOwner).getGroupId());
      addSpaceStatistics(statisticData, space);
      callType = "space";
    } else if (callOwner instanceof SpaceEventInfo) {
      Space space = getSpaceService().getSpaceByGroupId(((SpaceEventInfo) callOwner).getGroupId());
      addSpaceStatistics(statisticData, space);
      callType = "space";
    } else if (callOwner instanceof GroupInfo) {
      callType = "group";
    } else {
      callType = "user";
    }

    Set<UserInfo> participants = callInfo.getParticipants() == null ? Collections.emptySet() : callInfo.getParticipants();
    long participantsCount = participants.size();
    long activeParticipantsCount =
                                 participants.stream()
                                             .filter(participant -> StringUtils.equals(UserState.JOINED, participant.getState()))
                                             .count();

    statisticData.addParameter("participantsCount", participantsCount);
    statisticData.addParameter("activeParticipantsCount", activeParticipantsCount);
    statisticData.addParameter("callId", callInfo.getId());
    statisticData.addParameter("callType", callType);
    statisticData.addParameter("callOwnerType", callInfo.getOwner().getType());
    statisticData.addParameter("callStartDate", callInfo.getStartDate());
    statisticData.addParameter("callEndDate", callInfo.getEndDate());
    statisticData.addParameter("callState", callInfo.getState());
    if (callDuration > 0) {
      statisticData.addParameter("callDuration", callDuration);
    }
    addStatisticData(statisticData);
  }

  private SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = this.container.getComponentInstanceOfType(SpaceService.class);
    }
    return spaceService;
  }

}
