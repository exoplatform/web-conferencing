/**
 * 
 */
package org.exoplatform.webconferencing.webrtc.server;

import static org.exoplatform.webconferencing.Utils.getResourceMessages;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.web.AbstractHttpServlet;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The Class WebrtcSettingsServlet.
 */
public class WebrtcSettingsServlet extends AbstractHttpServlet {

  /** The Constant serialVersionUID. */
  private static final long     serialVersionUID  = -6075521943684442791L;

  /** The Constant LOG. */
  protected static final Log  LOG               = ExoLogger.getLogger(WebrtcSettingsServlet.class);

  /** The Constant UNAUTHORIZED_PAGE. */
  private final static String   UNAUTHORIZED_PAGE = "/WEB-INF/pages/unauthorized.html";

  /** The Constant SERVER_ERROR_PAGE. */
  private final static String   SERVER_ERROR_PAGE = "/WEB-INF/pages/servererror.html";

  /**
   * Instantiates a new WebRTC settings servlet.
   */
  public WebrtcSettingsServlet() {
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

    try {
      // We set the character encoding now to UTF-8 before obtaining parameters
      req.setCharacterEncoding("UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOG.error("Encoding not supported", e);
    }

    if (remoteUser != null) {
      try {
        Map<String, String> messages = getResourceMessages("locale.webrtc.WebRTCAdmin", httpReq.getLocale());
        httpReq.setAttribute("messages", messages);

        httpReq.getRequestDispatcher("/WEB-INF/pages/settings.jsp").include(httpReq, httpRes);
      } catch (Exception e) {
        LOG.error("Error processing WebRTC settings page", e);
        httpRes.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        httpReq.getRequestDispatcher(SERVER_ERROR_PAGE).include(httpReq, httpRes);
      }
    } else {
      httpRes.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      httpReq.getRequestDispatcher(UNAUTHORIZED_PAGE).include(httpReq, httpRes);
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
