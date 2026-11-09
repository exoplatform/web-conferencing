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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.cometd.annotation.server.RemoteCall;
import org.cometd.bayeux.server.ServerSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;

import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.client.ErrorInfo;
import org.exoplatform.webconferencing.support.CallLog;
import org.exoplatform.webconferencing.support.CallLogService;

/**
 * The call and log remote calls act only for the user their CometD session was opened for.
 */
public class CometdWebConferencingServiceTest {

  private static final String          SESSION_USER = "john";

  private static final String          OTHER_USER   = "mary";

  private static final String          SESSION_ID   = "session-john";

  private static final String          UNAUTHORIZED = ErrorInfo.clientError("Unauthorized user").asJSON();

  private EXoContinuationBayeux        exoBayeux;

  private CallLogService               callLogs;

  private CallLog                      callLog;

  private RemoteCall.Caller            caller;

  private CometdWebConferencingService cometdService;

  @BeforeEach
  public void setUp() {
    exoBayeux = mock(EXoContinuationBayeux.class);
    when(exoBayeux.isSubscribed(SESSION_USER, SESSION_ID)).thenReturn(true);
    callLogs = mock(CallLogService.class);
    callLog = mock(CallLog.class);
    when(callLogs.getLog()).thenReturn(callLog);
    ServerSession session = mock(ServerSession.class);
    when(session.getId()).thenReturn(SESSION_ID);
    caller = mock(RemoteCall.Caller.class);
    when(caller.getServerSession()).thenReturn(session);
    cometdService = new CometdWebConferencingService(mock(IdentityRegistry.class),
                                                     mock(OrganizationService.class),
                                                     mock(WebConferencingService.class),
                                                     exoBayeux,
                                                     mock(CacheService.class),
                                                     callLogs) {
      @Override
      protected ExecutorService createThreadExecutor(String threadNamePrefix, int maxFactor, int queueFactor) {
        return mock(ExecutorService.class);
      }
    };
  }

  @Test
  public void testCallCommandOfTheSessionUserIsProcessed() {
    cometdService.service.rcCalls(caller, callArguments(SESSION_USER));

    verify(cometdService.callHandlers).submit(any(Runnable.class));
    verify(caller, never()).failure(nullable(Object.class));
  }

  @Test
  public void testCallCommandForAnotherUserIsRefused() {
    cometdService.service.rcCalls(caller, callArguments(OTHER_USER));

    verify(caller).failure(UNAUTHORIZED);
    verify(cometdService.callHandlers, never()).submit(any(Runnable.class));
  }

  @Test
  public void testLogOfTheSessionUserIsWritten() {
    cometdService.service.rcLogs(caller, logArguments(SESSION_USER));

    verify(callLog).info(contains(SESSION_USER + "-client-1 call started"), nullable(LocalDateTime.class));
    verify(caller).result(CometdWebConferencingService.LOG_OK);
    verify(caller, never()).failure(nullable(Object.class));
  }

  @Test
  public void testLogForAnotherUserIsRefused() {
    cometdService.service.rcLogs(caller, logArguments(OTHER_USER));

    verify(caller).failure(UNAUTHORIZED);
    verify(callLog, never()).info(anyString(), nullable(LocalDateTime.class));
    verify(callLogs, never()).getLog();
  }

  private Map<String, Object> callArguments(String userId) {
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("exoId", userId);
    arguments.put("exoClientId", "client-1");
    arguments.put("exoContainerName", "portal");
    arguments.put("id", "call-1");
    arguments.put("command", "update");
    arguments.put("state", "joined");
    return arguments;
  }

  private Map<String, Object> logArguments(String userId) {
    Map<String, Object> arguments = new HashMap<>();
    arguments.put("exoId", userId);
    arguments.put("exoClientId", "client-1");
    arguments.put("level", CallLog.INFO_LEVEL);
    arguments.put("timestamp", "2026-11-09T20:00:00.000Z");
    arguments.put("data", "call started");
    return arguments;
  }
}
