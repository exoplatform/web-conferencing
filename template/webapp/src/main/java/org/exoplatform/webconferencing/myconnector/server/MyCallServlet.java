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
package org.exoplatform.webconferencing.myconnector.server;

import static org.exoplatform.webconferencing.Utils.asJSON;
import static org.exoplatform.webconferencing.Utils.getCurrentContext;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.web.AbstractHttpServlet;
import org.exoplatform.webconferencing.ContextInfo;
import org.exoplatform.webconferencing.UserInfo;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.myconnector.MyConnectorProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The Class MyCallServlet.
 */
public class MyCallServlet extends AbstractHttpServlet {

  /** The Constant serialVersionUID. */
  private static final long     serialVersionUID  = -6075521943684342671L;

  /** The Constant LOG. */
  protected static final Log  LOG               = ExoLogger.getLogger(MyCallServlet.class);

  /** The Constant UNAUTHORIZED_PAGE. */
  private final static String   UNAUTHORIZED_PAGE = "/WEB-INF/pages/unauthorized.html";

  /** The Constant SERVER_ERROR_PAGE. */
  private final static String   SERVER_ERROR_PAGE = "/WEB-INF/pages/servererror.html";

  /** The Constant CALL_PAGE. */
  private final static String   CALL_PAGE         = "/WEB-INF/pages/call.jsp";

  /**
   * Instantiates a new my call servlet.
   */
  public MyCallServlet() {
    //
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    HttpServletRequest httpReq = (HttpServletRequest) req;
    HttpServletResponse httpRes = (HttpServletResponse) resp;

    httpRes.setContentType("text/html; charset=UTF-8");

    String remoteUser = httpReq.getRemoteUser();

    ExoContainer container = getContainer();
    WebConferencingService webConferencing =
                                           (WebConferencingService) container.getComponentInstanceOfType(WebConferencingService.class);
    if (webConferencing != null) {
      MyConnectorProvider provider;
      try {
        provider = (MyConnectorProvider) webConferencing.getProvider(MyConnectorProvider.TYPE);
      } catch (ClassCastException e) {
        LOG.error("Provider " + MyConnectorProvider.TYPE + " isn't an instance of " + MyConnectorProvider.class.getName(), e);
        provider = null;
      }

      if (provider != null) {
        try {
          // We set the character encoding now to UTF-8 before obtaining parameters
          req.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
          LOG.error("Encoding not supported", e);
        }

        if (remoteUser != null) {
          try {
            // init page scope with settings for webConferencing and My Connector provider

            ContextInfo context = getCurrentContext(remoteUser, req.getLocale());
            httpReq.setAttribute("contextInfo", asJSON(context));

            UserInfo exoUser = webConferencing.getUserInfo(remoteUser);
            if (exoUser != null) {
              httpReq.setAttribute("userInfo", asJSON(exoUser));
              httpReq.setAttribute("settings", asJSON(provider.getSettings()));
              httpReq.getRequestDispatcher(CALL_PAGE).include(httpReq, httpRes);
            } else {
              LOG.warn("My Call servlet cannot be initialized: user info cannot be obtained for " + remoteUser);
            }
          } catch (Exception e) {
            LOG.error("Error processing My Call page", e);
            httpRes.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            httpReq.getRequestDispatcher(SERVER_ERROR_PAGE).include(httpReq, httpRes);
          }
        } else {
          httpRes.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          httpReq.getRequestDispatcher(UNAUTHORIZED_PAGE).include(httpReq, httpRes);
        }
      } else {
        LOG.error("My Connector provider not found for call page and user " + remoteUser);
        httpRes.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        httpReq.getRequestDispatcher(SERVER_ERROR_PAGE).include(httpReq, httpRes);
      }
    } else {
      LOG.error("Web Conferencing service not found for call page and user " + remoteUser);
      httpRes.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      httpReq.getRequestDispatcher(SERVER_ERROR_PAGE).include(httpReq, httpRes);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doGet(req, resp);
  }

}
