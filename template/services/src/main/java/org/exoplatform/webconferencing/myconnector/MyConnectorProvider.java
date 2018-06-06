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

import java.util.Locale;

import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.profile.settings.IMType;
import org.exoplatform.social.core.profile.settings.UserProfileSettingsService;
import org.exoplatform.webconferencing.CallProvider;
import org.exoplatform.webconferencing.UserInfo.IMInfo;

/**
 * My Connector provider implementation.
 * 
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: MyConnectorProvider.java 00000 Mar 30, 2017 pnedonosko $
 */
public class MyConnectorProvider extends CallProvider {

  public static final String TYPE            = "myconnector";

  /** The Constant CONFIG_APIKEY. */
  public static final String CONFIG_APIKEY   = "my-apiKey";

  /** The Constant CONFIG_CLIENTID. */
  public static final String CONFIG_CLIENTID = "my-clientId";

  /** The Constant CONFIG_URL. */
  public static final String CONFIG_URL      = "my-serviceUrl";

  /** The Constant TITLE. */
  public static final String TITLE           = "My Connector";

  /** The Constant VERSION. */
  public static final String VERSION         = "1.0.0";

  /**
   * Settings for My Call provider.
   */
  public class MySettings extends Settings {

    /**
     * Gets the API client id.
     *
     * @return the API client id
     */
    public String getApiClientId() {
      return MyConnectorProvider.this.getApiClientId();
    }

    /**
     * Gets the url.
     *
     * @return the url
     */
    public String getUrl() {
      return MyConnectorProvider.this.getUrl();
    }

    /**
     * Gets the api key.
     *
     * @return the api key
     */
    public String getApiKey() {
      return MyConnectorProvider.this.getApiKey();
    }
  }

  /**
   * IM info for user profile.
   */
  public class MyConnectorIMInfo extends IMInfo {

    /**
     * Instantiates a new MyConnector IM info.
     *
     * @param id the id
     */
    protected MyConnectorIMInfo(String id) {
      super(TYPE, id);
    }

    // You may add other specific methods here. Getters will be serialized to JSON and available on client
    // side (in Javascript provider module).
  }

  /** The client id (it's a sample, can be used for OAUth2 authentication). */
  protected final String apiClientId;

  /** The api key. (it's a sample) */
  protected final String apiKey;

  /** The connector web-services URL (will be used to generate Call page URLs). */
  protected final String url;

  /**
   * Instantiates a new My Call provider.
   *
   * @param profileSettings the profile settings
   * @param params the params (from configuration.xml)
   * @throws ConfigurationException the configuration exception
   */
  public MyConnectorProvider(UserProfileSettingsService profileSettings, InitParams params) throws ConfigurationException {
    super(params);

    // it's WAR name here,
    this.url = "/myconnector";
    // if need set this from external you can introduce a configuration parameter
    // String url = this.config.get(CONFIG_URL);
    // if (url == null || (url = url.trim()).length() == 0) {
    // throw new ConfigurationException(CONFIG_URL + " required and should be non empty.");
    // }
    // this.url = url;

    String apiClientId = this.config.get(CONFIG_CLIENTID);
    if (apiClientId == null || (apiClientId = apiClientId.trim()).length() == 0) {
      throw new ConfigurationException(CONFIG_CLIENTID + " required and should be non empty.");
    }
    this.apiClientId = apiClientId;

    String apiKey = this.config.get(CONFIG_APIKEY);
    if (apiKey == null || (apiKey = apiKey.trim()).length() == 0) {
      throw new ConfigurationException(CONFIG_APIKEY + " required and should be non empty.");
    }
    this.apiKey = apiKey;

    if (profileSettings != null) {
      // add plugin programmatically as it's an integral part of the provider
      profileSettings.addIMType(new IMType(TYPE, TITLE));
    }
  }

  /**
   * Instantiates a new My Connector provider. This constructor can be used in environments when no
   * {@link UserProfileSettingsService} found (e.g. in test environments).
   *
   * @param params the params (from configuration.xml)
   * @throws ConfigurationException the configuration exception
   */
  public MyConnectorProvider(InitParams params) throws ConfigurationException {
    this(null, params);
  }

  /**
   * Gets the settings.
   *
   * @return the settings
   */
  public MySettings getSettings() {
    return new MySettings();
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
   * Gets the API client id.
   *
   * @return the API client id
   */
  public String getApiClientId() {
    return apiClientId;
  }

  /**
   * Gets the api key.
   *
   * @return the api key
   */
  public String getApiKey() {
    return apiKey;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IMInfo getIMInfo(String imId) {
    // TODO here you can validate, extend or do any other IM id preparations
    return new MyConnectorIMInfo(imId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getType() {
    return TYPE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String[] getSupportedTypes() {
    return new String[] { getType() };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTitle() {
    return TITLE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription(Locale locale) {
    // TODO Implement i18n description of the provider
    return super.getDescription(locale);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getVersion() {
    return VERSION;
  }

}
