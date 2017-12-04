/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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
package org.exoplatform.webconferencing.webrtc.server;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: WebrtcContext.java 00000 Dec 4, 2017 pnedonosko $
 */
public class WebrtcContext {

  /** The Constant WEBRTC_SERVLET_CTX. */
  public static final String WEBRTC_SERVLET_CTX   = "/webrtc".intern();

  /** The Constant SETTINGS_SERVLET. */
  public static final String CALL_SERVLET         = "/webrtccallservlet".intern();

  /** The Constant SETTINGS_SERVLET. */
  public static final String SETTINGS_SERVLET     = "/webrtcsettingsservlet".intern();

  /** The Constant WEBRTC_CALL_REDIRECT. */
  public static final String WEBRTC_CALL_REDIRECT = "webrtccall_redirect";

}
