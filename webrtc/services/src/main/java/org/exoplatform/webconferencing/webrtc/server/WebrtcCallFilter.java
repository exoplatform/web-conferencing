
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

import static org.exoplatform.webconferencing.webrtc.server.WebrtcContext.CALL_SERVLET;
import static org.exoplatform.webconferencing.webrtc.server.WebrtcContext.CALL_REDIRECT;
import static org.exoplatform.webconferencing.webrtc.server.WebrtcContext.WEBRTC_SERVLET_CTX;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.web.filter.Filter;
import org.exoplatform.webconferencing.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Filter forwards requests to WebRTC call URLs to related servlets.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: WebrtcCallFilter.java 00000 Aug 14, 2017 pnedonosko $
 * 
 */
public class WebrtcCallFilter extends AbstractFilter implements Filter {

  /** The Constant LOG. */
  protected static final Log LOG          = ExoLogger.getLogger(WebrtcCallFilter.class);

  /** The Constant SCHEME_HTTP. */
  public static final String    SCHEME_HTTP  = "http";

  /** The Constant SCHEME_HTTPS. */
  public static final String    SCHEME_HTTPS = "https";

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpReq = (HttpServletRequest) request;
    HttpServletResponse httpRes = (HttpServletResponse) response;

    String scheme = httpReq.getScheme();
    if (scheme != null) {
      scheme = scheme.toUpperCase().toLowerCase();
    } else {
      scheme = SCHEME_HTTP;
    }

    if (httpReq.getRemoteUser() != null) {
      if (scheme.equals("http")) {
        // Redirect to HTTPS: it seems doesn't work?
        LOG.warn(new StringBuilder("WebRTC call page request with not secure shceme ").append(httpReq.getRequestURL())
                                                                                      .append(". Redirecting to secure page."));
        String secure;
        try {
          secure = Utils.buildUrl(SCHEME_HTTPS,
                                  httpReq.getServerName(),
                                  httpReq.getServerPort(),
                                  httpReq.getServletPath() + "?" + httpReq.getQueryString());
        } catch (MalformedURLException e) {
          LOG.warn("Error creating secure URL for " + httpReq.getRequestURL().toString(), e);
          // Replace http to https manually
          secure = new StringBuilder(SCHEME_HTTPS).append(httpReq.getRequestURL().substring(4))
                                                  .append('/')
                                                  .append(httpReq.getQueryString())
                                                  .toString();
        }
        httpReq.setAttribute(CALL_REDIRECT, secure);
      } else {
        ServletContext context = httpReq.getSession().getServletContext().getContext(WEBRTC_SERVLET_CTX);
        context.getRequestDispatcher(CALL_SERVLET).forward(httpReq, httpRes);
      }
    } else {
      // TODO user not authenticated into eXo Platform
      // Redirect to login page?
      // Return 401 Unauthorized?
      chain.doFilter(request, response);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
    // nothing
  }
}
