package org.exoplatform.webconferencing.server.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.web.filter.Filter;
import org.exoplatform.webconferencing.WebConferencingService;

/**
 * The Class LogoutFilter removes the webconf session cookie when user logouts. 
 */
public class LogoutFilter implements Filter {

  /**
   * Do filter.
   *
   * @param request the request
   * @param response the response
   * @param chain the chain
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws ServletException the servlet exception
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    if (httpRequest.getAuthType() == null) {
      Cookie cookie = new Cookie(WebConferencingService.SESSION_TOKEN_COOKIE, "");
      cookie.setPath("/");
      cookie.setMaxAge(0);
      httpResponse.addCookie(cookie);
    }
    chain.doFilter(request, response);
  }

}
