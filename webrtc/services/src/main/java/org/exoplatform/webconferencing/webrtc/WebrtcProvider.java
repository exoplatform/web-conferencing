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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webconferencing.CallProvider;
import org.exoplatform.webconferencing.CallProviderException;
import org.exoplatform.webconferencing.UserInfo.IMInfo;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: WebrtcProvider.java 00000 Aug 15, 2017 pnedonosko $
 */
public class WebrtcProvider extends CallProvider {

  /** The Constant WEBRTC_TYPE. */
  public static final String WEBRTC_TYPE              = "webrtc";

  /** The Constant WEBRTC_TITLE. */
  public static final String WEBRTC_TITLE             = "WebRTC";

  /** The Constant CONFIG_RTC_CONFIGURATION. */
  public static final String CONFIG_RTC_CONFIGURATION = "rtc-configuration";

  /** The Constant VERSION. */
  public static final String VERSION                  = "1.0.0";

  /** The Constant LOG. */
  protected static final Log LOG                      = ExoLogger.getLogger(WebrtcProvider.class);

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
      return new WebrtcSettings(getType(),
                                getSupportedTypes(),
                                getTitle(),
                                "Call", // TODO in18n
                                "Join", // TODO in18n
                                getVersion(),
                                callUri,
                                rtcConfiguration.cloneEnabled());
    }
  }

  public static class RTCConfiguration {

    protected String bundlePolicy;

    protected int    iceCandidatePoolSize;

    protected String iceTransportPolicy;

    protected Set<?> iceServers = new LinkedHashSet<>();

    /**
     * @return the bundlePolicy
     */
    public String getBundlePolicy() {
      return bundlePolicy;
    }

    /**
     * @param bundlePolicy the bundlePolicy to set
     */
    public void setBundlePolicy(String bundlePolicy) {
      this.bundlePolicy = bundlePolicy;
    }

    /**
     * @return the iceCandidatePoolSize
     */
    public int getIceCandidatePoolSize() {
      return iceCandidatePoolSize;
    }

    /**
     * @param iceCandidatePoolSize the iceCandidatePoolSize to set
     */
    public void setIceCandidatePoolSize(int iceCandidatePoolSize) {
      this.iceCandidatePoolSize = iceCandidatePoolSize;
    }

    /**
     * @return the iceTransportPolicy
     */
    public String getIceTransportPolicy() {
      return iceTransportPolicy;
    }

    /**
     * @param iceTransportPolicy the iceTransportPolicy to set
     */
    public void setIceTransportPolicy(String iceTransportPolicy) {
      this.iceTransportPolicy = iceTransportPolicy;
    }

    /**
     * @return the iceServers
     */
    public Set<?> getIceServers() {
      return iceServers;
    }

    /**
     * @param iceServers the iceServers to set
     */
    public void setIceServers(Set<?> iceServers) {
      this.iceServers = iceServers;
    }

    RTCConfiguration cloneEnabled() {
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
          if (ices.isEnabled()) {
            iceServers.add(ices);
          }
        }
      }
      enabled.setIceServers(iceServers);
      return enabled;
    }
  }

  public static class ICEServer {
    protected boolean enabled;

    protected String  username;

    protected String  credential;

    protected List<?> urls = new ArrayList<>();

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * @return the username
     */
    public String getUsername() {
      return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
      this.username = username;
    }

    /**
     * @return the credential
     */
    public String getCredential() {
      return credential;
    }

    /**
     * @param credential the credential to set
     */
    public void setCredential(String credential) {
      this.credential = credential;
    }

    /**
     * @return the urls
     */
    public List<?> getUrls() {
      return urls;
    }

    /**
     * @param urls the urls to set
     */
    public void setUrls(List<?> urls) {
      this.urls = urls;
    }
  }

  protected final RTCConfiguration rtcConfiguration;

  /**
   * Instantiates a new WebRTC provider.
   *
   * @param params the params
   * @throws ConfigurationException the configuration exception
   */
  public WebrtcProvider(InitParams params) throws ConfigurationException {
    super(params);

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

}
