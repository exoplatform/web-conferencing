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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.support.CallLogService;

/**
 * The threads of the call commands do not keep a stopped server running.
 */
public class CometdWebConferencingServiceStopTest {

  private CometdWebConferencingService cometdService;

  @BeforeEach
  public void setUp() {
    // The constructor reads the cluster profile: answered here, so that no Kernel container is started
    try (MockedStatic<ExoContainer> exoContainer = mockStatic(ExoContainer.class)) {
      exoContainer.when(() -> ExoContainer.hasProfile("cluster")).thenReturn(false);
      cometdService = new CometdWebConferencingService(mock(IdentityRegistry.class),
                                                       mock(OrganizationService.class),
                                                       mock(WebConferencingService.class),
                                                       mock(EXoContinuationBayeux.class),
                                                       mock(CacheService.class),
                                                       mock(CallLogService.class));
    }
  }

  @Test
  public void testTheCallCommandsRunOnDaemonThreadsThatEndWithTheService() throws Exception {
    Thread commandThread = cometdService.callHandlers.submit(Thread::currentThread).get(5, TimeUnit.SECONDS);
    assertTrue(commandThread.getName().startsWith(CometdWebConferencingService.CALL_THREAD_PREFIX));
    assertTrue(commandThread.isDaemon(), "A call command thread must not keep the JVM alive");

    cometdService.stop();

    assertTrue(cometdService.callHandlers.isShutdown());
    assertTrue(cometdService.callHandlers.awaitTermination(5, TimeUnit.SECONDS),
               "The call command threads must end once the service stopped");
  }

  @Test
  public void testTheStopWaitsForACallCommandAlreadySubmitted() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    AtomicBoolean completed = new AtomicBoolean();
    cometdService.callHandlers.submit(() -> {
      started.countDown();
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      completed.set(true);
    });
    assertTrue(started.await(5, TimeUnit.SECONDS));

    cometdService.stop();

    assertTrue(completed.get(), "A call command submitted before the stop must reach its end before the stop returns");
    assertTrue(cometdService.callHandlers.isTerminated());
  }

}
