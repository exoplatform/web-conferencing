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
package org.exoplatform.webconferencing.admin.portlet;

import static org.exoplatform.webconferencing.Utils.asJSON;
import static org.exoplatform.webconferencing.Utils.getCurrentContext;
import static org.exoplatform.webconferencing.Utils.getResourceMessages;

import java.io.IOException;
import java.util.Map;

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
import org.exoplatform.webconferencing.ContextInfo;
import org.exoplatform.webconferencing.UserInfo;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: WebConferencingAdminPortlet.java 00000 Nov 23, 2017 pnedonosko $
 * 
 */
public class WebConferencingAdminPortlet extends GenericPortlet {

  /** The Constant LOG. */
  private static final Log       LOG                   = ExoLogger.getLogger(WebConferencingAdminPortlet.class);

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
   * Admin view.
   *
   * @param request the request
   * @param response the response
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws PortletException the portlet exception
   */
  @Override
  public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
    final String remoteUser = request.getRemoteUser();
    try {
      UserInfo exoUser = webConferencing.getUserInfo(remoteUser);
      if (exoUser != null) {
        // i18n messages
        //Map<String, String> messages = getResourceMessages("locale.webconferencing.WebConferencingAdmin", request.getLocale());
        // Reduce and simplify the map for use in JSP: no real sense with small amount of messages
        //        messages = messages.entrySet().stream()
        //                   .filter(e -> e.getKey().startsWith(ADMIN_RESOURCE_PREFIX))
        //                   .collect(Collectors.toMap(e -> e.getKey()
        //                                                   .substring(ADMIN_RESOURCE_PREFIX.length()),
        //                                             a -> a.getValue()));
        
        // Markup
        //request.setAttribute("messages", messages);
        PortletRequestDispatcher prDispatcher = getPortletContext().getRequestDispatcher("/WEB-INF/pages/admin.jsp");
        prDispatcher.include(request, response);
        String exoUserJson = asJSON(exoUser);
        ContextInfo context = getCurrentContext(remoteUser, request.getLocale());
        String contextJson = asJSON(context);

        // Javascript
        JavascriptManager js = ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).getJavascriptManager();
        js.require("SHARED/webConferencingPortlet", "webConferencingPortlet")
          .require("PORTLET/webconferencing/webConferencingAdminPortlet", "webConferencingAdminPortlet")
          .addScripts("webConferencingPortlet.start(" + exoUserJson + "," + contextJson + ");")
          .addScripts("webConferencingAdminPortlet.init();"); // messages: " + asJSON(messages) + " - i18n not yet required by the script

      } else {
        LOG.warn("Web Conferencing Admin portlet cannot be initialized: user info cannot be obtained for " + remoteUser);
      }
    } catch (Exception e) {
      LOG.error("Error processing Web Conferencing Admin portlet for user " + remoteUser, e);
    }
  }

}
