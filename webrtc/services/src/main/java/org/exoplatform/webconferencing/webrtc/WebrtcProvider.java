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
package org.exoplatform.webconferencing.webrtc;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webconferencing.CallProvider;
import org.exoplatform.webconferencing.CallProviderConfiguration;
import org.exoplatform.webconferencing.CallProviderException;
import org.exoplatform.webconferencing.UserInfo.IMInfo;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: WebrtcProvider.java 00000 Aug 15, 2017 pnedonosko $
 */
public class WebrtcProvider extends CallProvider {

  /** The Constant WEBRTC_TYPE. */
  public static final String    WEBRTC_TYPE              = "webrtc";

  /** The Constant WEBRTC_TITLE. */
  public static final String    WEBRTC_TITLE             = "WebRTC";

  /** The Constant CONFIG_RTC_CONFIGURATION. */
  public static final String    CONFIG_RTC_CONFIGURATION = "rtc-configuration";

  /** The Constant WEBRTC_SCOPE_NAME. */
  protected static final String WEBRTC_SCOPE_NAME        = "webconferencing.webrtc".intern();

  /** The Constant KEY_RTC_SETTINGS. */
  protected static final String KEY_RTC_SETTINGS         = "rtc-settings".intern();

  /** The Constant VERSION. */
  public static final String    VERSION                  = "1.0.0";

  /** The Constant LOG. */
  protected static final Log    LOG                      = ExoLogger.getLogger(WebrtcProvider.class);

  /**
   * The Class WebrtcSettings.
   */
  public class WebrtcSettings extends Settings {

    /** The call URI. */
    protected final String           callUri;

    /** The rtc configuration. */
    protected final RTCConfiguration rtcConfiguration;

    /**
     * Instantiates a new webrtc settings.
     *
     * @param callUri the call URI
     * @param rtcConfiguration the rtc configuration
     */
    public WebrtcSettings(String callUri, RTCConfiguration rtcConfiguration) {
      this.callUri = callUri;
      this.rtcConfiguration = rtcConfiguration;
    }

    /**
     * Gets the call URI.
     *
     * @return the call URI
     */
    public String getCallUri() {
      return callUri;
    }

    /**
     * Gets the rtc configuration.
     *
     * @return the rtcConfiguration
     */
    public RTCConfiguration getRtcConfiguration() {
      return rtcConfiguration;
    }

  }

  /**
   * The Class SettingsBuilder.
   */
  public class SettingsBuilder {

    /** The call URI. */
    protected String callUri;

    /**
     * Call URI.
     *
     * @param callUri the call URI
     * @return the settings builder
     */
    public SettingsBuilder callUri(String callUri) {
      this.callUri = callUri;
      return this;
    }

    /**
     * Builds the WebRTC settings.
     *
     * @return the WebRTC settings
     */
    public WebrtcSettings build() {
      return new WebrtcSettings(callUri, rtcConfiguration.clone(true));
    }
  }

  /**
   * The Class RTCConfiguration.
   */
  public static class RTCConfiguration {

    /** The bundle policy. */
    protected String         bundlePolicy;

    /** The ice candidate pool size. */
    protected int            iceCandidatePoolSize;

    /** The ice transport policy. */
    protected String         iceTransportPolicy;

    /** The ice servers. */
    protected Set<ICEServer> iceServers = new LinkedHashSet<>();

    /**
     * Gets the bundle policy.
     *
     * @return the bundlePolicy
     */
    public String getBundlePolicy() {
      return bundlePolicy;
    }

    /**
     * Sets the bundle policy.
     *
     * @param bundlePolicy the bundlePolicy to set
     */
    public void setBundlePolicy(String bundlePolicy) {
      this.bundlePolicy = bundlePolicy;
    }

    /**
     * Gets the ice candidate pool size.
     *
     * @return the iceCandidatePoolSize
     */
    public int getIceCandidatePoolSize() {
      return iceCandidatePoolSize;
    }

    /**
     * Sets the ice candidate pool size.
     *
     * @param iceCandidatePoolSize the iceCandidatePoolSize to set
     */
    public void setIceCandidatePoolSize(int iceCandidatePoolSize) {
      this.iceCandidatePoolSize = iceCandidatePoolSize;
    }

    /**
     * Gets the ice transport policy.
     *
     * @return the iceTransportPolicy
     */
    public String getIceTransportPolicy() {
      return iceTransportPolicy;
    }

    /**
     * Sets the ice transport policy.
     *
     * @param iceTransportPolicy the iceTransportPolicy to set
     */
    public void setIceTransportPolicy(String iceTransportPolicy) {
      this.iceTransportPolicy = iceTransportPolicy;
    }

    /**
     * Gets the ice servers.
     *
     * @return the iceServers
     */
    public Set<ICEServer> getIceServers() {
      return iceServers;
    }

    /**
     * Sets the ice servers.
     *
     * @param iceServers the iceServers to set
     */
    public void setIceServers(Set<ICEServer> iceServers) {
      this.iceServers = iceServers;
    }

    /**
     * Clone enabled.
     *
     * @return the RTC configuration
     */
    RTCConfiguration clone(boolean onlyEnabled) {
      RTCConfiguration enabled = new RTCConfiguration();
      if (this.bundlePolicy != null && this.bundlePolicy.length() > 0) {
        enabled.setBundlePolicy(this.bundlePolicy);
      }
      if (this.iceTransportPolicy != null && this.iceTransportPolicy.length() > 0) {
        enabled.setIceTransportPolicy(this.iceTransportPolicy);
      }
      if (this.iceCandidatePoolSize > 0) {
        enabled.setIceCandidatePoolSize(this.iceCandidatePoolSize);
      }
      Set<ICEServer> iceServers = new LinkedHashSet<>();
      if (this.iceServers.size() > 0) {
        for (Object o : this.iceServers) {
          ICEServer ices = (ICEServer) o;
          if (!onlyEnabled || ices.isEnabled()) {
            iceServers.add(ices);
          }
        }
      }
      enabled.setIceServers(iceServers);
      return enabled;
    }
  }

  /**
   * The Class ICEServer.
   */
  public static class ICEServer {

    /** The enabled. */
    protected boolean      enabled;

    /** The username. */
    protected String       username;

    /** The credential. */
    protected String       credential;

    /** The urls. */
    protected List<String> urls = new ArrayList<>();

    /**
     * Checks if is enabled.
     *
     * @return the enabled
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Sets the enabled.
     *
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * Gets the username.
     *
     * @return the username
     */
    public String getUsername() {
      return username;
    }

    /**
     * Sets the username.
     *
     * @param username the username to set
     */
    public void setUsername(String username) {
      this.username = username;
    }

    /**
     * Gets the credential.
     *
     * @return the credential
     */
    public String getCredential() {
      return credential;
    }

    /**
     * Sets the credential.
     *
     * @param credential the credential to set
     */
    public void setCredential(String credential) {
      this.credential = credential;
    }

    /**
     * Gets the urls.
     *
     * @return the urls
     */
    public List<String> getUrls() {
      return urls;
    }

    /**
     * Sets the urls.
     *
     * @param urls the urls to set
     */
    public void setUrls(List<String> urls) {
      this.urls = urls;
    }
  }

  /** The settings service. */
  protected final SettingService settingService;

  /** The rtc configuration. */
  protected RTCConfiguration     rtcConfiguration;

  /**
   * Instantiates a new WebRTC provider.
   *
   * @param params the params
   * @throws ConfigurationException the configuration exception
   */
  public WebrtcProvider(InitParams params, SettingService settingService) throws ConfigurationException {
    super(params);

    this.settingService = settingService;

    // try read RTC config from storage first
    RTCConfiguration rtcConfiguration;
    try {
      rtcConfiguration = readRtcConfig();
    } catch (Exception e) {
      LOG.error("Error reading RTC configuration", e);
      rtcConfiguration = null;
    }

    if (rtcConfiguration == null) {
      ObjectParameter objParam = params.getObjectParam(CONFIG_RTC_CONFIGURATION);
      if (objParam != null) {
        Object obj = objParam.getObject();
        if (obj != null) {
          this.rtcConfiguration = (RTCConfiguration) obj;
        } else {
          LOG.warn("Predefined services configuration found but null object returned.");
          this.rtcConfiguration = new RTCConfiguration();
        }
      } else {
        this.rtcConfiguration = new RTCConfiguration();
      }
    } else {
      this.rtcConfiguration = rtcConfiguration;
    }
  }

  /**
   * Gets the rtc configuration.
   *
   * @return the rtc configuration
   */
  public RTCConfiguration getRtcConfiguration() {
    return this.rtcConfiguration.clone(false); // all ICE servers will be here
  }

  /**
   * Save rtc configuration.
   *
   * @param conf the conf
   * @throws Exception the exception
   */
  public void saveRtcConfiguration(RTCConfiguration conf) throws Exception {
    saveRtcConfig(conf);
    this.rtcConfiguration = conf;
  }

  /**
   * Gets the settings.
   *
   * @return the settings
   */
  public SettingsBuilder settings() {
    return new SettingsBuilder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public IMInfo getIMInfo(String imId) throws CallProviderException {
    return null; // WebRTC has not (and don't need) an IM account // new WebrtcIMInfo(imId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getVersion() {
    return VERSION;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getType() {
    return WEBRTC_TYPE;
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
    return WEBRTC_TITLE;
  }

  /**
   * Json to RTC config.
   *
   * @param json the json
   * @return the RTC configuration
   * @throws Exception the exception
   */
  public RTCConfiguration jsonToRtcConfig(JSONObject json) throws Exception {
    JSONArray jsonIceServers = json.optJSONArray("iceServers");
    if (jsonIceServers == null) {
      throw new WebrtcProviderException("ICE Servers not found");
    }

    Set<ICEServer> iceServers = new LinkedHashSet<>();
    for (int si = 0; si < jsonIceServers.length(); si++) {
      JSONObject jsonIs = jsonIceServers.getJSONObject(si);
      ICEServer ices = new ICEServer();
      boolean enabled = jsonIs.optBoolean("enabled", true);
      ices.setEnabled(enabled);
      JSONArray jsonUrls = json.optJSONArray("urls");
      if (jsonUrls == null) {
        throw new WebrtcProviderException("ICE Server has no URLs");
      }
      List<String> urls = new ArrayList<>();
      for (int ui = 0; ui < jsonUrls.length(); ui++) {
        String url = jsonUrls.optString(ui, null);
        if (url != null) {
          urls.add(url);
        }
      }
      if (urls.size() > 0) {
        ices.setUrls(urls);
      } else {
        throw new WebrtcProviderException("ICE Server has empty URLs");
      }

      String username = jsonIs.optString("username", null);
      if (username != null) {
        ices.setUsername(username);
      }
      String credential = jsonIs.optString("credential", null);
      if (credential != null) {
        ices.setCredential(credential);
      }
      iceServers.add(ices);
    }

    RTCConfiguration rtcConf = new RTCConfiguration();
    rtcConf.setIceServers(iceServers);

    String bundlePolicy = json.optString("bundlePolicy", null);
    if (bundlePolicy != null) {
      rtcConf.setBundlePolicy(bundlePolicy);
    }
    String iceTransportPolicy = json.optString("iceTransportPolicy", null);
    if (iceTransportPolicy != null) {
      rtcConf.setIceTransportPolicy(iceTransportPolicy);
    }
    int iceCandidatePoolSize = json.optInt("iceCandidatePoolSize", -1);
    if (iceCandidatePoolSize >= 0) {
      rtcConf.setIceCandidatePoolSize(iceCandidatePoolSize);
    }

    return rtcConf;
  }

  /**
   * RTC config to json.
   *
   * @param rtcConf the rtc conf
   * @return the JSON object
   * @throws Exception the exception
   */
  public JSONObject rtcConfigToJson(RTCConfiguration rtcConf) throws Exception {
    JSONObject json = new JSONObject();

    JSONArray jsonIces = new JSONArray();
    for (ICEServer is : rtcConf.getIceServers()) {
      JSONObject jsonIs = new JSONObject();
      jsonIs.put("enabled", is.isEnabled());
      JSONArray jsonUrls = new JSONArray();
      for (String url : is.getUrls()) {
        jsonUrls.put(url);
      }
      jsonIs.put("urls", jsonUrls);
      if (is.getUsername() != null) {
        jsonIs.put("username", is.getUsername());
      }
      if (is.getCredential() != null) {
        jsonIs.put("credential", is.getCredential());
      }
      jsonIces.put(jsonIs);
    }
    json.put("iceServers", jsonIces);

    if (rtcConf.getBundlePolicy() != null) {
      json.put("bundlePolicy", rtcConf.getBundlePolicy());
    }
    if (rtcConf.getIceTransportPolicy() != null) {
      json.put("iceTransportPolicy", rtcConf.getIceTransportPolicy());
    }
    if (rtcConf.getIceCandidatePoolSize() >= 0) {
      json.put("iceCandidatePoolSize", rtcConf.getIceCandidatePoolSize());
    }

    return json;
  }

  /**
   * Save rtc config.
   *
   * @param conf the conf
   * @throws Exception the exception
   */
  protected void saveRtcConfig(RTCConfiguration conf) throws Exception {
    final String initialGlobalId = Scope.GLOBAL.getId();
    try {
      JSONObject json = rtcConfigToJson(conf);
      settingService.set(Context.GLOBAL,
                         Scope.GLOBAL.id(WEBRTC_SCOPE_NAME),
                         KEY_RTC_SETTINGS,
                         SettingValue.create(json.toString()));
    } finally {
      Scope.GLOBAL.id(initialGlobalId);
    }
  }

  /**
   * Read rtc config.
   *
   * @return the RTC configuration
   * @throws Exception the exception
   */
  protected RTCConfiguration readRtcConfig() throws Exception {
    final String initialGlobalId = Scope.GLOBAL.getId();
    try {
      SettingValue<?> val = settingService.get(Context.GLOBAL, Scope.GLOBAL.id(WEBRTC_SCOPE_NAME), KEY_RTC_SETTINGS);
      if (val != null) {
        String str = String.valueOf(val.getValue());
        if (str.startsWith("{")) {
          // Assuming it's JSON
          RTCConfiguration conf = jsonToRtcConfig(new JSONObject(str));
          return conf;
        } else {
          LOG.warn("Cannot parse saved RTCConfiguration: " + str);
        }
      }
      return null;
    } finally {
      Scope.GLOBAL.id(initialGlobalId);
    }
  }

}
