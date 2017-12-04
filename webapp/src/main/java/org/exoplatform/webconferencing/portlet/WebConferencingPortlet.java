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
package org.exoplatform.webconferencing.portlet;

import static org.exoplatform.webconferencing.Utils.asJSON;
import static org.exoplatform.webconferencing.Utils.getCurrentContext;
import static org.exoplatform.webconferencing.Utils.getResourceMessages;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webconferencing.ContextInfo;
import org.exoplatform.webconferencing.UserInfo;
import org.exoplatform.webconferencing.Utils;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: WebConferencingPortlet.java 00000 Mar 29, 2017 pnedonosko $
 */
public class WebConferencingPortlet extends GenericPortlet {

  /** The Constant LOG. */
  private static final Log       LOG = ExoLogger.getLogger(WebConferencingPortlet.class);

  /** The Web Conferencing service. */
  private WebConferencingService webConferencing;

  /**
   * {@inheritDoc}
   */
  @Override
  public void init() throws PortletException {
    super.init();

    ExoContainer container = ExoContainerContext.getCurrentContainer();
    this.webConferencing = container.getComponentInstanceOfType(WebConferencingService.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doView(final RenderRequest request, final RenderResponse response) throws PortletException, IOException {
    final String remoteUser = request.getRemoteUser();
    try {
      ContextInfo context = getCurrentContext(remoteUser, request.getLocale());
      String contextJson = asJSON(context);

      UserInfo exoUser = webConferencing.getUserInfo(remoteUser);
      if (exoUser != null) {
        String exoUserJson = asJSON(exoUser);
        JavascriptManager js = ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).getJavascriptManager();
        js.require("SHARED/webConferencingPortlet", "webConferencingPortlet")
          .addScripts("webConferencingPortlet.start(" + exoUserJson + "," + contextJson + ");");
      } else {
        LOG.warn("Web Conferencing portlet cannot be initialized: user info cannot be obtained for " + remoteUser);
      }
    } catch (Exception e) {
      LOG.error("Error processing Web Conferencing portlet for user " + remoteUser, e);
    }
  }

}
