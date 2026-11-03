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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
import org.exoplatform.webconferencing.WebConferencingService.SpaceEventInfo;

/**
 * The owner of a {@code space_event} call is the identity hosting the event:
 * a space for a space event, a user for a personal event (EXO-92565).
 */
class WebConferencingServiceSpaceEventOwnerTest {

  private static final String   OWNER_ID = "5";

  private WebConferencingService webConferencing;

  private IdentityManager        identityManager;

  private SpaceService           spaceService;

  @BeforeEach
  void setUp() {
    webConferencing = mock(WebConferencingService.class, CALLS_REAL_METHODS);
    identityManager = mock(IdentityManager.class);
    spaceService = mock(SpaceService.class);
    ReflectionTestUtils.setField(webConferencing, "socialIdentityManager", identityManager);
    ReflectionTestUtils.setField(webConferencing, "spaceService", spaceService);
  }

  @Test
  void testCreateOwnerOfPersonalEventTakesTheUserProfile() throws Exception {
    Identity userIdentity = new Identity("organization", "john");
    userIdentity.setId(OWNER_ID);
    Profile profile = new Profile(userIdentity);
    profile.setAvatarUrl("/avatar/john");
    userIdentity.setProfile(profile);
    when(identityManager.getIdentity(OWNER_ID)).thenReturn(userIdentity);
    when(spaceService.getSpaceByPrettyName("john")).thenReturn(null);

    try (MockedStatic<LinkProvider> linkProvider = mockStatic(LinkProvider.class)) {
      linkProvider.when(() -> LinkProvider.getUserProfileUri("john")).thenReturn("/portal/dw/profile/john");

      IdentityInfo owner = createSpaceEventOwner();

      SpaceEventInfo spaceEvent = assertInstanceOf(SpaceEventInfo.class, owner);
      assertEquals(OWNER_ID, spaceEvent.getId());
      assertEquals(WebConferencingService.OWNER_TYPE_SPACEEVENT, spaceEvent.getType());
      assertEquals("/portal/dw/profile/john", spaceEvent.getProfileLink());
      assertEquals("/avatar/john", spaceEvent.getAvatarLink());
      assertNull(spaceEvent.getGroupId());
    }
  }

  @Test
  void testCreateOwnerOfSpaceEventTakesTheSpace() throws Exception {
    Identity spaceIdentity = new Identity("space", "sales_team");
    spaceIdentity.setId(OWNER_ID);
    Space space = new Space();
    space.setPrettyName("sales_team");
    space.setUrl("sales_team");
    space.setGroupId("/spaces/sales_team");
    when(identityManager.getIdentity(OWNER_ID)).thenReturn(spaceIdentity);
    when(spaceService.getSpaceByPrettyName("sales_team")).thenReturn(space);

    IdentityInfo owner = createSpaceEventOwner();

    SpaceEventInfo spaceEvent = assertInstanceOf(SpaceEventInfo.class, owner);
    assertEquals("/spaces/sales_team", spaceEvent.getGroupId());
    assertEquals(space.getUrl(), spaceEvent.getProfileLink());
  }

  private IdentityInfo createSpaceEventOwner() throws Exception {
    return webConferencing.createOwner(OWNER_ID,
                                       WebConferencingService.OWNER_TYPE_SPACEEVENT,
                                       "event1",
                                       false,
                                       false,
                                       true,
                                       false);
  }
}
