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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.web.security.codec.CodecInitializer;

/**
 * The leave grace time, how long the leave of a call window whose connection
 * ended waits, is read from the service's init params: 15 seconds when unset
 * or wrong, 0 applying the leave at once.
 */
class WebConferencingServiceLeaveGraceTest {

  @Test
  void theLeaveGraceTimeIs15SecondsWhenUnset() {
    assertEquals(15, service(null).getLeaveGraceTime());
  }

  @Test
  void theLeaveGraceTimeIsReadFromItsInitParam() {
    assertEquals(30, service("30").getLeaveGraceTime());
    assertEquals(0, service(" 0 ").getLeaveGraceTime());
  }

  @Test
  void aWrongLeaveGraceTimeFallsBackTo15Seconds() {
    assertEquals(15, service("-1").getLeaveGraceTime());
    assertEquals(15, service("soon").getLeaveGraceTime());
    assertEquals(15, service(String.valueOf(WebConferencingService.MAX_INCOMING_CALL_TIMEOUT + 1)).getLeaveGraceTime());
  }

  private WebConferencingService service(String leaveGraceTime) {
    InitParams initParams = mock(InitParams.class);
    PropertiesParam jwtParams = new PropertiesParam();
    jwtParams.setProperty(WebConferencingService.SECRET_KEY, "secret");
    when(initParams.getPropertiesParam(WebConferencingService.JWT_CONFIGURATION_PROPERTIES)).thenReturn(jwtParams);
    if (leaveGraceTime != null) {
      ValueParam param = new ValueParam();
      param.setName(WebConferencingService.LEAVE_GRACE_TIME);
      param.setValue(leaveGraceTime);
      when(initParams.getValueParam(WebConferencingService.LEAVE_GRACE_TIME)).thenReturn(param);
    }
    return new WebConferencingService(null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null,
                                      initParams,
                                      null,
                                      mock(CodecInitializer.class));
  }
}
