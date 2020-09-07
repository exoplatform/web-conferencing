package org.exoplatform.webconferencing.server.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.web.filter.Filter;
import org.exoplatform.webconferencing.WebConferencingService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * The Class SessionFilter.
 */
public class SessionFilter implements Filter {

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

    HttpServletRequest httpReq = (HttpServletRequest) request;
    HttpServletResponse httpRes = (HttpServletResponse) response;
    if (httpReq.getRemoteUser() != null) {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      WebConferencingService webconfService =
                                            (WebConferencingService) container.getComponentInstanceOfType(WebConferencingService.class);

      String sessionToken = Jwts.builder()
                                .setSubject("exo-webconf")
                                .claim("username", httpReq.getRemoteUser())
                                .signWith(Keys.hmacShaKeyFor(webconfService.getSecretKey().getBytes()))
                                .compact();
      Cookie cookie = new Cookie(WebConferencingService.SESSION_TOKEN_COOKIE, sessionToken);
      cookie.setPath("/");
      cookie.setMaxAge(1200); // 20 mins
      httpRes.addCookie(cookie);
    }

    chain.doFilter(request, response);
  }

}
