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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webconferencing.WebConferencingService.SpaceEventInfo;

/**
 * The details of an event's call are given only to the members of its host
 * space, or to the host user of a personal event.
 */
class WebConferencingServiceSpaceEventAccessTest {

  private static final String    HOST_ID = "5";

  private WebConferencingService webConferencing;

  private IdentityManager        identityManager;

  private SpaceService           spaceService;

  @BeforeEach
  void setUp() throws Exception {
    webConferencing = mock(WebConferencingService.class, CALLS_REAL_METHODS);
    identityManager = mock(IdentityManager.class);
    spaceService = mock(SpaceService.class);
    ReflectionTestUtils.setField(webConferencing, "socialIdentityManager", identityManager);
    ReflectionTestUtils.setField(webConferencing, "spaceService", spaceService);
    doReturn("call1").when(webConferencing).findLastSpaceEventCallId(HOST_ID);
    doReturn(new UserInfo("mary", "Mary", "Smith")).when(webConferencing).userInfo("mary");
    doReturn(new UserInfo("paul", "Paul", "Jones")).when(webConferencing).userInfo("paul");
    doReturn(new UserInfo("eve", "Eve", "Hidden")).when(webConferencing).userInfo("eve");
    doReturn(new UserInfo("secret", "Secret", "Member")).when(webConferencing).userInfo("secret");
  }

  @Test
  void testRequesterOutsideTheHostSpaceGetsNothing() throws Exception {
    hostSpace("mary");

    SpaceEventInfo info = webConferencing.getSpaceEventInfo("eve", HOST_ID, new String[] { "paul" }, new String[0]);

    assertNull(info);
  }

  @Test
  void testSpaceTheRequesterCannotViewAddsNoMember() throws Exception {
    hostSpace("mary");
    Space hidden = new Space();
    hidden.setPrettyName("hidden_team");
    hidden.setMembers(new String[] { "secret" });
    when(spaceService.getSpaceByPrettyName("hidden_team")).thenReturn(hidden);
    when(spaceService.canViewSpace(hidden, "mary")).thenReturn(false);

    SpaceEventInfo info = webConferencing.getSpaceEventInfo("mary", HOST_ID, new String[0], new String[] { "hidden_team" });

    assertNotNull(info);
    assertFalse(info.getMembers().containsKey("secret"));
    assertTrue(info.getMembers().containsKey("mary"));
  }

  @Test
  void testSpaceTheRequesterCanViewAddsItsMembers() throws Exception {
    hostSpace("mary");
    Space open = new Space();
    open.setPrettyName("open_team");
    open.setMembers(new String[] { "paul" });
    when(spaceService.getSpaceByPrettyName("open_team")).thenReturn(open);
    when(spaceService.canViewSpace(open, "mary")).thenReturn(true);

    SpaceEventInfo info = webConferencing.getSpaceEventInfo("mary", HOST_ID, new String[0], new String[] { "open_team" });

    assertTrue(info.getMembers().containsKey("paul"));
  }

  @Test
  void testHostSpaceMemberGetsTheEventWithItsCallId() throws Exception {
    hostSpace("mary");

    SpaceEventInfo info = webConferencing.getSpaceEventInfo("mary", HOST_ID, new String[] { "paul" }, new String[0]);

    assertEquals("call1", info.getCallId());
    assertTrue(info.getMembers().containsKey("mary"));
    assertTrue(info.getMembers().containsKey("paul"));
  }

  @Test
  void testPersonalEventIsGivenToItsHostUserOnly() throws Exception {
    Identity user = new Identity("organization", "mary");
    user.setId(HOST_ID);
    user.setProfile(new Profile(user));
    when(identityManager.getIdentity(HOST_ID)).thenReturn(user);

    assertNull(webConferencing.getSpaceEventInfo("eve", HOST_ID, new String[] { "paul" }, new String[0]));
    SpaceEventInfo info = webConferencing.getSpaceEventInfo("mary", HOST_ID, new String[] { "paul" }, new String[0]);

    assertEquals("call1", info.getCallId());
    assertTrue(info.getMembers().containsKey("paul"));
  }

  private void hostSpace(String... members) {
    Identity spaceIdentity = new Identity("space", "sales_team");
    spaceIdentity.setId(HOST_ID);
    Space space = new Space();
    space.setPrettyName("sales_team");
    space.setUrl("sales_team");
    space.setGroupId("/spaces/sales_team");
    space.setMembers(members);
    when(identityManager.getIdentity(HOST_ID)).thenReturn(spaceIdentity);
    when(spaceService.getSpaceByPrettyName("sales_team")).thenReturn(space);
    for (String member : members) {
      when(spaceService.isMember(space, member)).thenReturn(true);
    }
  }
}
