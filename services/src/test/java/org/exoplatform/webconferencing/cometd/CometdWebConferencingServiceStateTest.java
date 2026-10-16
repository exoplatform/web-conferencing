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
package org.exoplatform.webconferencing.cometd;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.UserState;
import org.exoplatform.webconferencing.WebConferencingService;

/**
 * The update command applies a participant state sent by a client to the user
 * of the Cometd session, the declined state included.
 */
class CometdWebConferencingServiceStateTest {

  private static final String         CALL_ID   = "pexip-call-1";

  private static final String         USER_ID   = "john";

  private static final String         CLIENT_ID = "client-1";

  private WebConferencingService      webConferencing;

  private CometdWebConferencingService cometdService;

  @BeforeEach
  void setUp() {
    webConferencing = mock(WebConferencingService.class);
    cometdService = new CometdWebConferencingService(null, null, webConferencing, null, mock(CacheService.class), null);
  }

  @Test
  void declinedIsAnUpdatableState() {
    assertTrue(CometdWebConferencingService.isUpdatableState(UserState.DECLINED));
    assertFalse(CometdWebConferencingService.isUpdatableState("rejected"));
  }

  @Test
  void declinedStateDeclinesTheCallForTheSessionUser() throws Exception {
    CallInfo call = mock(CallInfo.class);
    when(webConferencing.declineCall(CALL_ID, USER_ID, CLIENT_ID)).thenReturn(call);

    assertSame(call, cometdService.updateCallState(CALL_ID, UserState.DECLINED, USER_ID, CLIENT_ID));

    verify(webConferencing).declineCall(CALL_ID, USER_ID, CLIENT_ID);
    verifyNoMoreInteractions(webConferencing);
  }

  @Test
  void anUnknownStateIsRefused() {
    assertThrows(IllegalArgumentException.class,
                 () -> cometdService.updateCallState(CALL_ID, "rejected", USER_ID, CLIENT_ID));
  }
}
