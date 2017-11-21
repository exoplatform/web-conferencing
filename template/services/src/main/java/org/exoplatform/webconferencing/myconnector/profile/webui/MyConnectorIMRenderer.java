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
package org.exoplatform.webconferencing.myconnector.profile.webui;

import java.io.IOException;

import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.social.core.profile.settings.IMType;
import org.exoplatform.social.core.profile.settings.UserProfileSettingsService;
import org.exoplatform.social.webui.profile.settings.UIIMControlRenderer;
import org.exoplatform.webconferencing.myconnector.MyConnectorProvider;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * An IM control renderer for user profile to setup your call connector with user settings (e.g. account name
 * to sign-in into the call). If IM semantic not actual for your connector - remove this class and its record
 * in configuration.xml.
 * 
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: MyConnectorIMRenderer.java 00000 May 4, 2017 pnedonosko $
 */
public class MyConnectorIMRenderer extends UIIMControlRenderer {

  /**
   * Instantiates a new my connector IM renderer.
   *
   * @param imtype the imtype
   */
  MyConnectorIMRenderer(IMType imtype) {
    super(imtype);
  }

  /**
   * Instantiates a new my connector IM renderer.
   *
   * @param settingsService the settings service
   * @throws ConfigurationException the configuration exception
   */
  public MyConnectorIMRenderer(UserProfileSettingsService settingsService) throws ConfigurationException {
    super(findIMType(MyConnectorProvider.TYPE, settingsService));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void render(String imValue, WebuiRequestContext context) throws IOException, Exception {
    // it's a button with icon that user will click to setup the connector settings
    StringBuffer elem = new StringBuffer();
    // add markup here
    elem.append("<a class='actionIcon myControl' data-placement='bottom' rel='tooltip' title='' data-original-title='Settings' href='javascript:void(0)'>")
        .append("<i class='uiIconSettings uiIconLightGray'></i></a>");
    context.getWriter().append(elem.toString());
    // add Javascript that will load the connector module (in myProvider variable)
    context.getJavascriptManager()
           .require("SHARED/webConferencing_myconnector", "myProvider")
           // ...and call a custom initialization on it (it's optional but often required)
           .addScripts("if (myProvider) { myProvider.initSettings(); }");
  }

}
