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
package org.exoplatform.webconferencing.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.WebConferencingService.SpaceEventInfo;

/**
 * The space event route answers what the Service decides: the details to the
 * requester it lets through, whether or not that requester is listed among
 * the event's members, and a 404 to anyone else.
 */
class RESTWebConferencingServiceSpaceEventTest {

  private WebConferencingService        webConferencing;

  private RESTWebConferencingService    rest;

  @BeforeEach
  void setUp() {
    webConferencing = mock(WebConferencingService.class);
    rest = new RESTWebConferencingService(webConferencing, null, mock(SpaceService.class));
    ConversationState.setCurrent(new ConversationState(new Identity("mary")));
  }

  @AfterEach
  void tearDown() {
    ConversationState.setCurrent(null);
  }

  @Test
  void testHostUserNotAmongTheMembersGetsTheDetails() throws Exception {
    SpaceEventInfo info = mock(SpaceEventInfo.class);
    when(webConferencing.getSpaceEventInfo(eq("mary"), eq("5"), any(), any())).thenReturn(info);

    Response response = rest.getSpaceEventInfo(null, "5", "paul", "team");

    assertEquals(200, response.getStatus());
    assertSame(info, response.getEntity());
    String cacheControl = String.valueOf(response.getMetadata().getFirst("Cache-Control"));
    assertTrue(cacheControl.contains("no-cache"));
    assertTrue(cacheControl.contains("no-store"));
  }

  @Test
  void testRequesterTheServiceRefusesGetsNotFoundNotCached() throws Exception {
    when(webConferencing.getSpaceEventInfo(eq("mary"), eq("5"), any(), any())).thenReturn(null);

    Response response = rest.getSpaceEventInfo(null, "5", "paul", "team");

    assertEquals(404, response.getStatus());
    String cacheControl = String.valueOf(response.getMetadata().getFirst("Cache-Control"));
    assertTrue(cacheControl.contains("no-cache"));
    assertTrue(cacheControl.contains("no-store"));
  }
}
