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
package org.exoplatform.webconferencing.myconnector.portlet;

import static org.exoplatform.webconferencing.Utils.asJSON;

import java.io.IOException;

import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.JavascriptManager;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.myconnector.MyConnectorProvider;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * My Connector provider portlet should be added to the portal pages (configuration.xml) where we need add the
 * provider to Web Conferencing call buttons.
 * This portlet loads Javascript module of this connector and register its provider(s) in the Web
 * Conferencing core. By doing this we add the connector to call buttons on the page. And this connector
 * script should implement call button element and logic on clicking it.
 * 
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: MyConnectorPortlet.java 00000 Mar 29, 2017 pnedonosko $
 */
public class MyConnectorPortlet extends GenericPortlet {

  /** The Constant LOG. */
  private static final Log       LOG = ExoLogger.getLogger(MyConnectorPortlet.class);

  /** The web conferencing. */
  private WebConferencingService webConferencing;

  /** The provider. */
  private MyConnectorProvider    provider;

  /**
   * {@inheritDoc}
   */
  @Override
  public void init() throws PortletException {
    super.init();
    // Get eXo container and Web Conferencing service once per portlet initialization
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    this.webConferencing = container.getComponentInstanceOfType(WebConferencingService.class);
    try {
      this.provider = (MyConnectorProvider) webConferencing.getProvider(MyConnectorProvider.TYPE);
    } catch (ClassCastException e) {
      LOG.error("Provider " + MyConnectorProvider.TYPE + " isn't an instance of " + MyConnectorProvider.class.getName(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void doView(final RenderRequest request, final RenderResponse response) throws PortletException, IOException {
    if (this.provider != null) {
      try {
        // If we have settings to send to a client side
        String settingsJson = asJSON(provider.getSettings());
        JavascriptManager js = ((WebuiRequestContext) WebuiRequestContext.getCurrentInstance()).getJavascriptManager();
        // first load Web Conferencing itself,
        js.require("SHARED/webConferencing", "webConferencing")
          // load our connector module to myProvider variable
          .require("SHARED/webConferencing_myconnector", "myProvider")
          // check if the variable contains an object to ensure the provider was loaded successfully
          .addScripts("if (myProvider) { "
              // optionally configure the provider with settings (from the server-side)
              + "myProvider.configure(" + settingsJson + "); "
              // then add an instance of the provider to the Web Conferencing client
              + "webConferencing.addProvider(myProvider); "
              // and force Web Conferencing client update (to update call buttons and related stuff)
              + "webConferencing.update(); " + "}");
      } catch (Exception e) {
        LOG.error("Error processing My Connector calls portlet for user " + request.getRemoteUser(), e);
      }
    }
  }
}
