
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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.container.web.AbstractFilter;
import org.exoplatform.web.filter.Filter;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * Filter forwards requests from My Connector call URLs (/myconnector/*) to related servlet. See also
 * configuration.xml for URL mappings.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: MyCallFilter.java 00000 Mar 14, 2017 pnedonosko $
 * 
 */
public class MyCallFilter extends AbstractFilter implements Filter {

  /** The Constant LOG. */
  protected static final Logger LOG          = LoggerFactory.getLogger(MyCallFilter.class);

  /** The Constant SERVLET_CTX. */
  public static final String    SERVLET_CTX  = "/myconnector";

  /** The Constant CALL_SERVLET. */
  public static final String    CALL_SERVLET = "/mycallservlet".intern();

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpReq = (HttpServletRequest) request;
    HttpServletResponse httpRes = (HttpServletResponse) response;

    if (httpReq.getRemoteUser() != null) {
      ServletContext callContext = httpReq.getSession().getServletContext().getContext(SERVLET_CTX);
      callContext.getRequestDispatcher(CALL_SERVLET).forward(httpReq, httpRes);
    } else {
      // User not authenticated into eXo Platform: follow default logic
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
