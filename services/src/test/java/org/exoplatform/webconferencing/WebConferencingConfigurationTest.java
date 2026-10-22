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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.exoplatform.container.configuration.ConfigurationUnmarshaller;
import org.exoplatform.container.xml.Component;
import org.exoplatform.container.xml.Configuration;
import org.exoplatform.webconferencing.listeners.CallMissedNotificationListener;
import org.exoplatform.webconferencing.notification.pwa.MissedCallPwaPlugin;

/**
 * The Kernel configuration of the webapp, which declares the service, is read
 * by the Kernel's own parser: a file it refuses is not loaded at startup, and
 * every component it declares, the service first, is then missing. The
 * incoming call timeout it gives the service defaults to 60 seconds, the leave
 * grace time to 0 seconds. The notification files declaring the missed call notification are read the same.
 */
class WebConferencingConfigurationTest {

  private static final Path CONFIGURATION              =
                                                       Path.of("..", "webapp", "src", "main", "webapp", "WEB-INF", "conf", "webconferencing", "configuration.xml");

  private static final Path NOTIFICATION_CONFIGURATION =
                                                       Path.of("..", "webapp", "src", "main", "webapp", "WEB-INF", "conf", "webconferencing", "notification-configuration.xml");

  private static final Path PORTAL_CONFIGURATION       = Path.of("src", "main", "resources", "conf", "portal", "configuration.xml");

  @Test
  void theServiceConfigurationIsReadByTheKernelWithItsIncomingCallTimeout() throws Exception {
    Configuration configuration = read(CONFIGURATION);

    Component service = configuration.getComponent(WebConferencingService.class.getName());
    assertNotNull(service);
    assertEquals("60",
                 service.getInitParams().getValueParam(WebConferencingService.INCOMING_CALL_TIMEOUT).getValue());
  }

  @Test
  void theServiceConfigurationGivesALeaveGraceTimeOf0Seconds() throws Exception {
    Component service = read(CONFIGURATION).getComponent(WebConferencingService.class.getName());

    assertEquals("0", service.getInitParams().getValueParam(WebConferencingService.LEAVE_GRACE_TIME).getValue());
  }

  @Test
  void theMissedCallNotificationConfigurationIsReadByTheKernel() throws Exception {
    assertTrue(read(NOTIFICATION_CONFIGURATION).toXML().contains("MissedCallPlugin"));
    Configuration portal = read(PORTAL_CONFIGURATION);
    assertNotNull(portal.getComponent(MissedCallPwaPlugin.class.getName()));
    assertTrue(portal.toXML().contains(CallMissedNotificationListener.class.getName()));
  }

  private Configuration read(Path path) throws Exception {
    return new ConfigurationUnmarshaller().unmarshall(path.toUri().toURL());
  }
}
