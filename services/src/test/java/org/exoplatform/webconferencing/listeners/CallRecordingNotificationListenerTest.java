/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.webconferencing.listeners;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * The recording notification of a {@code space_event} call names the host of
 * the event: a space for a space event, a user for a personal event
 * (EXO-92565).
 */
class CallRecordingNotificationListenerTest {

  private static final String               HOST_ID = "5";

  private IdentityManager                   identityManager;

  private SpaceService                      spaceService;

  private CallRecordingNotificationListener listener;

  private NotificationContext               ctx;

  @BeforeEach
  void setUp() throws Exception {
    identityManager = mock(IdentityManager.class);
    spaceService = mock(SpaceService.class);
    WebConferencingService webConferencingService = mock(WebConferencingService.class);
    when(webConferencingService.getRecordingUrl(anyString(), anyString(), anyString())).thenReturn("http://host/recording");
    listener = new CallRecordingNotificationListener(identityManager, webConferencingService, spaceService);
    ctx = mock(NotificationContext.class, RETURNS_DEEP_STUBS);
  }

  @Test
  void testPersonalEventRecordingNamesTheHostUser() throws Exception {
    Identity userIdentity = new Identity("organization", "john");
    userIdentity.setId(HOST_ID);
    Profile profile = new Profile(userIdentity);
    profile.setProperty(Profile.FULL_NAME, "John Smith");
    profile.setAvatarUrl("/avatar/john");
    userIdentity.setProfile(profile);
    when(identityManager.getIdentity(HOST_ID)).thenReturn(userIdentity);
    when(spaceService.getSpaceByPrettyName("john")).thenReturn(null);

    fireSpaceEventRecording();

    verify(ctx).append(NotificationConstants.CALL_OWNER, "John Smith");
    verify(ctx).append(NotificationConstants.AVATAR_URL, "/avatar/john");
  }

  @Test
  void testSpaceEventRecordingNamesTheHostSpace() throws Exception {
    Identity spaceIdentity = new Identity("space", "sales_team");
    spaceIdentity.setId(HOST_ID);
    Space space = new Space();
    space.setPrettyName("sales_team");
    space.setDisplayName("Sales Team");
    space.setAvatarUrl("/avatar/sales_team");
    when(identityManager.getIdentity(HOST_ID)).thenReturn(spaceIdentity);
    when(spaceService.getSpaceByPrettyName("sales_team")).thenReturn(space);

    fireSpaceEventRecording();

    verify(ctx).append(NotificationConstants.CALL_OWNER, "Sales Team");
    verify(ctx).append(NotificationConstants.AVATAR_URL, "/avatar/sales_team");
  }

  private void fireSpaceEventRecording() throws Exception {
    CallInfo callInfo = mock(CallInfo.class);
    when(callInfo.getParticipants()).thenReturn(Collections.emptySet());
    Map<String, String> data = new HashMap<>();
    data.put("status", "success");
    data.put("file_name", "recording.mp4");
    data.put("identity", HOST_ID);
    data.put("type", WebConferencingService.OWNER_TYPE_SPACEEVENT);
    try (MockedStatic<NotificationContextImpl> contexts = mockStatic(NotificationContextImpl.class);
        MockedStatic<CommonsUtils> commonsUtils = mockStatic(CommonsUtils.class)) {
      contexts.when(NotificationContextImpl::cloneInstance).thenReturn(ctx);
      commonsUtils.when(CommonsUtils::getCurrentDomain).thenReturn("http://host");
      listener.onEvent(new Event<>("recording", callInfo, data));
    }
  }
}
