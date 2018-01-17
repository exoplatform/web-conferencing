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
package org.exoplatform.webconferencing.webrtc.portlet;

import static org.exoplatform.webconferencing.Utils.asJSON;
import static org.exoplatform.webconferencing.Utils.buildUrl;

import java.io.IOException;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webconferencing.CallProvider.Settings;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.webrtc.WebrtcProvider;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: SkypeProviderPortlet.java 00000 Mar 29, 2017 pnedonosko $
 */
public class WebrtcProviderPortlet extends GenericPortlet {

  /** The Constant LOG. */
  private static final Log       LOG = ExoLogger.getLogger(WebrtcProviderPortlet.class);

  /** The video calls. */
  private WebConferencingService webConferencing;

  /** The provider. */
  private WebrtcProvider         provider;

  /**
   * {@inheritDoc}
   */
  @Override
  public void init() throws PortletException {
    super.init();

    ExoContainer container = ExoContainerContext.getCurrentContainer();
    this.webConferencing = container.getComponentInstanceOfType(WebConferencingService.class);
    try {
      this.provider = (WebrtcProvider) webConferencing.getProvider(WebrtcProvider.WEBRTC_TYPE);
    } catch (ClassCastException e) {
      LOG.error("Provider " + WebrtcProvider.WEBRTC_TYPE + " isn't an instance of " + WebrtcProvider.class.getName(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doView(final RenderRequest request, final RenderResponse response) throws PortletException, IOException {
    if (this.provider != null) {
      try {
        Settings settings = provider.settings()
                                    .callUri(buildUrl(request.getScheme(),
                                                      request.getServerName(),
                                                      request.getServerPort(),
                                                      "/webrtc/call"))
                                    .locale(request.getLocale())
                                    .build();

        // Markup (incoming call popup etc)
        request.setAttribute("messages", settings.getMessages());
        PortletRequestDispatcher prDispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/pages/incoming-call.jsp");
        prDispatcher.include(request, response);

        String settingsJson = asJSON(settings);

        JavascriptManager js = ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).getJavascriptManager();
        js.require("SHARED/webConferencing", "webConferencing")
          .require("SHARED/webConferencing_webrtc", "webrtcProvider")
          .addScripts("if (webrtcProvider) { webrtcProvider.configure(" + settingsJson
              + "); webConferencing.addProvider(webrtcProvider); webConferencing.update(); }");
      } catch (Exception e) {
        LOG.error("Error processing WebRTC call portlet for user " + request.getRemoteUser(), e);
      }
    }
  }
}
