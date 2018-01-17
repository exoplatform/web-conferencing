/**
 * 
 */
package org.exoplatform.webconferencing.webrtc.server;

import static org.exoplatform.webconferencing.Utils.asJSON;
import static org.exoplatform.webconferencing.Utils.buildUrl;
import static org.exoplatform.webconferencing.Utils.getCurrentContext;
import static org.exoplatform.webconferencing.webrtc.server.WebrtcContext.CALL_REDIRECT;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.web.AbstractHttpServlet;
import org.exoplatform.webconferencing.ContextInfo;
import org.exoplatform.webconferencing.UserInfo;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.webrtc.WebrtcProvider;
import org.exoplatform.webconferencing.webrtc.WebrtcProvider.WebrtcSettings;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * The Class WebrtcCallServlet.
 */
public class WebrtcCallServlet extends AbstractHttpServlet {

  /** The Constant serialVersionUID. */
  private static final long     serialVersionUID  = -6075521943684342591L;

  /** The Constant LOG. */
  protected static final Logger LOG               = LoggerFactory.getLogger(WebrtcCallServlet.class);

  /** The Constant UNAUTHORIZED_PAGE. */
  private final static String   UNAUTHORIZED_PAGE = "/WEB-INF/pages/unauthorized.html";

  /** The Constant SERVER_ERROR_PAGE. */
  private final static String   SERVER_ERROR_PAGE = "/WEB-INF/pages/servererror.html";

  /**
   * Instantiates a new WebRTC call servlet.
   */
  public WebrtcCallServlet() {
    //
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    Object redirectUri = req.getAttribute(CALL_REDIRECT);
    if (redirectUri != null) {
      String ruri = (String) redirectUri;
      if (ruri.length() > 0) {
        resp.sendRedirect(ruri);
      }
    } else {
      resp.setContentType("text/html; charset=UTF-8");

      String remoteUser = req.getRemoteUser();

      ExoContainer container = getContainer();
      WebConferencingService webConferencing =
                                             (WebConferencingService) container.getComponentInstanceOfType(WebConferencingService.class);
      if (webConferencing != null) {
        WebrtcProvider provider;
        try {
          provider = (WebrtcProvider) webConferencing.getProvider(WebrtcProvider.WEBRTC_TYPE);
        } catch (ClassCastException e) {
          LOG.error("Provider " + WebrtcProvider.WEBRTC_TYPE + " isn't an instance of " + WebrtcProvider.class.getName(), e);
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
              // init page scope with settings for webConferencing and WebRTC provider
              ContextInfo context = getCurrentContext(remoteUser, req.getLocale());
              req.setAttribute("contextInfo", asJSON(context));

              UserInfo exoUser = webConferencing.getUserInfo(remoteUser);
              if (exoUser != null) {
                req.setAttribute("userInfo", asJSON(exoUser));

                // URI callURI = new URI(httpReq.getScheme(),
                // null,
                // httpReq.getServerName(),
                // httpReq.getServerPort(),
                // "/webrtc/call",
                // null,
                // null);
                WebrtcSettings settings = provider.settings()
                                                  .callUri(buildUrl(req.getScheme(),
                                                                    req.getServerName(),
                                                                    req.getServerPort(),
                                                                    "/webrtc/call"))
                                                  .locale(req.getLocale())
                                                  .build();
                req.setAttribute("settings", asJSON(settings));

                // XXX nasty-nasty-nasty include of CometD script
                req.getRequestDispatcher("/WEB-INF/pages/call_part1.jsp").include(req, resp);
                ServletContext cometdContext = req.getSession().getServletContext().getContext("/cometd");
                cometdContext.getRequestDispatcher("/javascript/eXo/commons/commons-cometd3.js").include(req, resp);
                req.getRequestDispatcher("/WEB-INF/pages/call_part2.jsp").include(req, resp);

                // to JSP page (in a right way)
                // httpReq.getRequestDispatcher(CALL_PAGE).include(httpReq, httpRes);
              } else {
                LOG.warn("WebRTC servlet cannot be initialized: user info cannot be obtained for " + remoteUser);
              }
            } catch (Exception e) {
              LOG.error("Error processing WebRTC call page", e);
              resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
              req.getRequestDispatcher(SERVER_ERROR_PAGE).include(req, resp);
            }
          } else {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            req.getRequestDispatcher(UNAUTHORIZED_PAGE).include(req, resp);
          }
        } else {
          LOG.error("WebRTC provider not found for call page and user " + remoteUser);
          resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          req.getRequestDispatcher(SERVER_ERROR_PAGE).include(req, resp);
        }
      } else {
        LOG.error("Web Conferencing service not found for call page and user " + remoteUser);
        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        req.getRequestDispatcher(SERVER_ERROR_PAGE).include(req, resp);
      }
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
