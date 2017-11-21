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
package org.exoplatform.webconferencing.myconnector;

import org.exoplatform.webconferencing.CallProviderSettings;

/**
 * Java object (POJO) defining settings required by a call connector. Required settings given in
 * {@link CallProviderSettings}. Others are up to the implementation - this class instance will be serialized
 * to JSON and provided to Javascript module of the connector via its configure() method (see in
 * {@link MyConnectorPortlet}).<br>
 * 
 * Setting implemented in this class are for example only - put yours instead.<br>
 * 
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: MyConnectorSettings.java 00000 Mar 30, 2017 pnedonosko $
 */
public class MyConnectorSettings extends CallProviderSettings {

  /** The client id. */
  protected final String clientId;

  /** The url. */
  protected final String url;

  /** The api key. */
  protected final String apiKey;

  /**
   * Instantiates a new settings.
   *
   * @param type the name
   * @param supportedTypes the supported types
   * @param title the title
   * @param callTitle the call title
   * @param joinTitle the join title
   * @param version the version
   * @param clientId the client id
   * @param url the url
   * @param apiKey the api key
   */
  public MyConnectorSettings(String type,
                             String[] supportedTypes,
                             String title,
                             String callTitle,
                             String joinTitle,
                             String version,
                             String clientId,
                             String url,
                             String apiKey) {
    super(type, supportedTypes, title, callTitle, joinTitle, version);
    this.clientId = clientId;
    this.url = url;
    this.apiKey = apiKey;
  }

  /**
   * Gets the client id.
   *
   * @return the client id
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * Gets the url.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Gets the api key.
   *
   * @return the api key
   */
  public String getApiKey() {
    return apiKey;
  }
}
