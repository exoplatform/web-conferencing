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
package org.exoplatform.webconferencing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webconferencing.dao.CallDAO;
import org.exoplatform.webconferencing.dao.OriginDAO;
import org.exoplatform.webconferencing.dao.StorageException;
import org.exoplatform.webconferencing.domain.CallEntity;

/**
 * A user's calls list skips the call of a space event whose hosting space is
 * gone, and keeps the user's other calls, those of a personal event included.
 */
class WebConferencingServiceUserCallsTest {

  private static final String    USER = "mary";

  private WebConferencingService webConferencing;

  private CallDAO                callStorage;

  private OriginDAO              originsStorage;

  private IdentityManager        identityManager;

  private SpaceService           spaceService;

  @BeforeEach
  void setUp() throws Exception {
    webConferencing = mock(WebConferencingService.class, CALLS_REAL_METHODS);
    callStorage = mock(CallDAO.class);
    originsStorage = mock(OriginDAO.class);
    identityManager = mock(IdentityManager.class);
    spaceService = mock(SpaceService.class);
    ReflectionTestUtils.setField(webConferencing, "callStorage", callStorage);
    ReflectionTestUtils.setField(webConferencing, "originsStorage", originsStorage);
    ReflectionTestUtils.setField(webConferencing, "socialIdentityManager", identityManager);
    ReflectionTestUtils.setField(webConferencing, "spaceService", spaceService);
    doReturn(null).when(webConferencing).getInviteId(anyString());
  }

  @Test
  void testCallOfAGoneSpaceIsSkippedAndTheOthersAreKept() throws Exception {
    spaceIdentity("1", "sales_team", true);
    spaceIdentity("2", "gone_team", false);
    userIdentity("3", "john");
    when(callStorage.findUserGroupCalls(USER)).thenReturn(List.of(spaceEventCall("call-space", "1"),
                                                                  spaceEventCall("call-gone", "2"),
                                                                  spaceEventCall("call-personal", "3")));

    try (MockedStatic<LinkProvider> linkProvider = mockStatic(LinkProvider.class)) {
      linkProvider.when(() -> LinkProvider.getUserProfileUri("john")).thenReturn("/portal/dw/profile/john");

      CallState[] states = webConferencing.getUserCalls(USER);

      assertEquals(List.of("call-space", "call-personal"), List.of(states[0].getId(), states[1].getId()));
      assertEquals(2, states.length);
    }
  }

  @Test
  void testIllegalStateOfTheStorageStillFailsTheRead() {
    spaceIdentity("1", "sales_team", true);
    when(callStorage.findUserGroupCalls(USER)).thenReturn(List.of(spaceEventCall("call-space", "1")));
    when(originsStorage.findCallOrigins("call-space", WebConferencingService.OWNER_TYPE_USER))
                                                                                              .thenThrow(new IllegalStateException("closed"));

    StorageException e = assertThrows(StorageException.class, () -> webConferencing.getUserCalls(USER));

    IllegalStateException cause = assertInstanceOf(IllegalStateException.class, e.getCause());
    assertEquals("closed", cause.getMessage());
  }

  @Test
  void testAFailedReadOfAUserHostedCallStillFailsTheRead() {
    // a personal event's host is a user: no space of that name exists, and the call is not skipped
    userIdentity("3", "john");
    when(callStorage.findUserGroupCalls(USER)).thenReturn(List.of(spaceEventCall("call-personal", "3")));
    when(originsStorage.findCallOrigins("call-personal", WebConferencingService.OWNER_TYPE_USER))
                                                                                                 .thenThrow(new IllegalStateException("closed"));

    StorageException e = assertThrows(StorageException.class, () -> webConferencing.getUserCalls(USER));

    IllegalStateException cause = assertInstanceOf(IllegalStateException.class, e.getCause());
    assertEquals("closed", cause.getMessage());
  }

  @Test
  void testAFailedHostCheckRethrowsTheOriginalFailureWithTheCheckFailureSuppressed() {
    Identity identity = new Identity("space", "gone_team");
    identity.setId("2");
    IllegalStateException checkFailure = new IllegalStateException("down");
    when(identityManager.getIdentity("2")).thenReturn(identity).thenThrow(checkFailure);
    when(callStorage.findUserGroupCalls(USER)).thenReturn(List.of(spaceEventCall("call-gone", "2")));

    StorageException e = assertThrows(StorageException.class, () -> webConferencing.getUserCalls(USER));

    IllegalStateException cause = assertInstanceOf(IllegalStateException.class, e.getCause());
    assertEquals("Cannot find the space hosting the event: gone_team", cause.getMessage());
    assertArrayEquals(new Throwable[] { checkFailure }, cause.getSuppressed());
  }

  private void spaceIdentity(String id, String prettyName, boolean exists) {
    Identity identity = new Identity("space", prettyName);
    identity.setId(id);
    when(identityManager.getIdentity(id)).thenReturn(identity);
    if (exists) {
      Space space = new Space();
      space.setPrettyName(prettyName);
      space.setUrl(prettyName);
      space.setGroupId("/spaces/" + prettyName);
      space.setMembers(new String[0]);
      when(spaceService.getSpaceByPrettyName(prettyName)).thenReturn(space);
    }
  }

  private void userIdentity(String id, String name) {
    Identity identity = new Identity("organization", name);
    identity.setId(id);
    identity.setProfile(new Profile(identity));
    when(identityManager.getIdentity(id)).thenReturn(identity);
  }

  private CallEntity spaceEventCall(String callId, String ownerId) {
    CallEntity call = new CallEntity();
    call.setId(callId);
    call.setOwnerType(WebConferencingService.OWNER_TYPE_SPACEEVENT);
    call.setOwnerId(ownerId);
    call.setIsGroup(true);
    call.setState(CallState.STARTED);
    return call;
  }
}
