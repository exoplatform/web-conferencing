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
package org.exoplatform.webconferencing.listener.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import org.exoplatform.services.listener.Event;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.UserInfo;
import org.exoplatform.webconferencing.UserState;
import org.exoplatform.webconferencing.WebConferencingService;

import io.meeds.analytics.model.StatisticData;
import io.meeds.analytics.utils.AnalyticsUtils;

/**
 * A decline is tracked as the "callDeclined" operation of the caller, with the
 * type of the call.
 */
class WebConferencingListenerTest {

  @Test
  void aDeclineIsTrackedForTheCallerWithTheCallType() throws Exception {
    CallInfo call = new CallInfo("p_call", "Call", new UserInfo("root", "Root", "Root"), "pexip");
    UserInfo callee = new UserInfo("john", "John", "Smith");
    callee.setState(UserState.DECLINED);
    call.addParticipant(callee);

    try (MockedStatic<AnalyticsUtils> analytics = mockStatic(AnalyticsUtils.class)) {
      analytics.when(() -> AnalyticsUtils.getUserIdentityId("root")).thenReturn(7L);

      new WebConferencingListener(null).onEvent(new Event<>(WebConferencingService.EVENT_CALL_DECLINED,
                                                            call,
                                                            Map.of(WebConferencingService.EVENT_DATA_USER_ID,
                                                                   "root",
                                                                   WebConferencingService.EVENT_DATA_DECLINER_ID,
                                                                   "john",
                                                                   "status",
                                                                   WebConferencingService.STATUS_OK)));

      ArgumentCaptor<StatisticData> data = ArgumentCaptor.forClass(StatisticData.class);
      analytics.verify(() -> AnalyticsUtils.addStatisticData(data.capture()));
      assertEquals("callDeclined", data.getValue().getOperation());
      assertEquals(7L, data.getValue().getUserId());
      assertEquals("user", data.getValue().getParameters().get("callType"));
      assertEquals("pexip", data.getValue().getModule());
      assertEquals(StatisticData.StatisticStatus.OK, data.getValue().getStatus());
    }
  }
}
