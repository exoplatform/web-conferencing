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

import static org.exoplatform.webconferencing.Utils.getResourceMessages;
import static org.json.JSONObject.NULL;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
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
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.webconferencing.CallProvider;
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

    /** The locale. */
    protected Locale locale;

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
     * Locale for internationalized messages to load in the settings.
     *
     * @param locale the locale
     * @return the settings builder
     */
    public SettingsBuilder locale(Locale locale) {
      this.locale = locale;
      return this;
    }

    /**
     * Builds the WebRTC settings.
     *
     * @return the WebRTC settings
     */
    public WebrtcSettings build() {
      WebrtcSettings settings = new WebrtcSettings(callUri, rtcConfiguration.clone(true));
      if (locale != null) {
        settings.addMessages(getResourceMessages("locale.webrtc.WebRTCClient", locale));
      }
      return settings;
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

    /** The enable log. */
    protected boolean        logEnabled;

    /**
     * Gets the bundle policy.
     *
     * @return the bundlePolicy
     */
    public String getBundlePolicy() {
      return bundlePolicy;
    }

    /**
     * Checks if is log enabled.
     *
     * @return true, if is log enabled
     */
    public boolean isLogEnabled() {
      return logEnabled;
    }

    /**
     * Sets enabled log.
     *
     * @param enableLog the new log enabled
     */
    public void setLogEnabled(boolean enableLog) {
      this.logEnabled = enableLog;
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
     * @param onlyEnabled the only enabled
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
      enabled.setLogEnabled(isLogEnabled());
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
     * Checks if is default.
     *
     * @return true, if is default
     */
    public boolean isDefault() {
      return false;
    }

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

  /**
   * The Class DefaultICEServer.
   */
  public static final class DefaultICEServer extends ICEServer {
  
    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
      return true;
    }
  }

  /** The settings service. */
  protected final SettingService        settingService;

  /** The resource bundle service. */
  protected final ResourceBundleService resourceBundleService;

  /** The rtc configuration. */
  protected RTCConfiguration            rtcConfiguration;

  /**
   * Instantiates a new WebRTC provider.
   *
   * @param params the params
   * @param settingService the setting service
   * @param resourceBundleService the resource bundle service
   * @throws ConfigurationException the configuration exception
   */
  public WebrtcProvider(InitParams params, SettingService settingService, ResourceBundleService resourceBundleService)
      throws ConfigurationException {
    super(params);

    this.settingService = settingService;
    this.resourceBundleService = resourceBundleService;

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
        if (obj != null && RTCConfiguration.class.isAssignableFrom(obj.getClass())) {
          this.rtcConfiguration = RTCConfiguration.class.cast(obj);
        } else {
          LOG.warn("Predefined services configuration exists but RTCConfiguration object not found.");
          this.rtcConfiguration = new RTCConfiguration();
        }
      } else {
        this.rtcConfiguration = new RTCConfiguration();
      }
    } else {
      this.rtcConfiguration = rtcConfiguration;
    }

    // Log warning if default (aka public) ICE servers in use
    for (ICEServer ices : this.rtcConfiguration.getIceServers()) {
      if (ices.isEnabled() && ices.isDefault()) {
        LOG.warn("Default ICE servers will be used for WebRTC calls: " + ices.getUrls().toString()
            + ". Configure own set of servers and disable the default one.");
      }
    }

    logRemoteLogEnabled();
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
    logRemoteLogEnabled();
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
    return null; // WebRTC has not (and don't need) an IM account
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
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return this.getDescription(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription(Locale locale) {
    if (locale == null) {
      locale = Locale.getDefault();
    }
    ResourceBundle res = resourceBundleService.getResourceBundle("locale.webrtc.WebRTCAdmin", locale);
    return res.getString("webrtc.admin.description");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLogEnabled() {
    return rtcConfiguration.isLogEnabled();
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
      JSONArray jsonUrls = jsonIs.optJSONArray("urls");
      if (jsonUrls == null) {
        throw new WebrtcProviderException("ICE Server has no URLs");
      }
      List<String> urls = new ArrayList<>();
      for (int ui = 0; ui < jsonUrls.length(); ui++) {
        Object url = jsonUrls.opt(ui);
        if (isNotNull(url)) {
          urls.add((String) url);
        }
      }
      if (urls.size() > 0) {
        ices.setUrls(urls);
      } else {
        throw new WebrtcProviderException("ICE Server has empty URLs");
      }
      Object username = jsonIs.opt("username");
      if (isNotNull(username)) {
        ices.setUsername((String) username);
      }
      Object credential = jsonIs.opt("credential");
      if (isNotNull(credential)) {
        ices.setCredential((String) credential);
      }
      iceServers.add(ices);
    }

    RTCConfiguration rtcConf = new RTCConfiguration();
    rtcConf.setIceServers(iceServers);

    Object bundlePolicy = json.opt("bundlePolicy");
    if (isNotNull(bundlePolicy)) {
      rtcConf.setBundlePolicy((String) bundlePolicy);
    }
    Object iceTransportPolicy = json.opt("iceTransportPolicy");
    if (isNotNull(iceTransportPolicy)) {
      rtcConf.setIceTransportPolicy((String) iceTransportPolicy);
    }
    int iceCandidatePoolSize = json.optInt("iceCandidatePoolSize", -1);
    if (iceCandidatePoolSize >= 0) {
      rtcConf.setIceCandidatePoolSize(iceCandidatePoolSize);
    }

    boolean logEnabled = json.optBoolean("logEnabled", false);
    rtcConf.setLogEnabled(logEnabled);

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
    if (rtcConf.getIceCandidatePoolSize() > 0) {
      json.put("iceCandidatePoolSize", rtcConf.getIceCandidatePoolSize());
    }

    json.put("logEnabled", rtcConf.isLogEnabled());

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

  /**
   * Checks if is not <code>null</code> or JSON NULL.
   *
   * @param obj the obj
   * @return true, if is null
   */
  protected boolean isNotNull(Object obj) {
    return obj != null && obj != NULL;
  }

  /**
   * Log remote log enabled.
   */
  protected void logRemoteLogEnabled() {
    if (rtcConfiguration != null && rtcConfiguration.isLogEnabled()) {
      LOG.info("Remote diagnostic log enabled for WebRTC connector");
    } else {
      LOG.info("Remote diagnostic log disabled for WebRTC connector");
    }
  }

}
