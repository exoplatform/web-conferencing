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
package org.exoplatform.webconferencing;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.webconferencing.UserInfo.IMInfo;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CallProvider.java 00000 Mar 30, 2017 pnedonosko $
 */
public abstract class CallProvider extends BaseComponentPlugin {

  /** The Constant CONFIG_PROVIDER_ACTIVE. */
  public static final String          CONFIG_PROVIDER_ACTIVE        = "active";

  /** The Constant CONFIG_PROVIDER_DESCRIPTION. */
  public static final String          CONFIG_PROVIDER_DESCRIPTION   = "description";

  /** The Constant CONFIG_PROVIDER_CONFIGURATION. */
  public static final String          CONFIG_PROVIDER_CONFIGURATION = "provider-configuration";

  /** The Constant EMAIL_REGEX. */
  protected static final String       EMAIL_REGEX                   =
                                                  "^(?=[A-Z0-9][A-Z0-9@._%+-]{5,253}+$)[A-Z0-9._%+-]{1,64}+@(?:(?=[A-Z0-9-]{1,63}+\\.)[A-Z0-9]++(?:-[A-Z0-9]++)*+\\.){1,8}+[A-Z]{2,63}+$";

  /**
   * Call Provider runtime Settings (for serialization in JSON to remote clients).
   */
  public abstract class Settings {
    /**
     * Checks if is active.
     *
     * @return true, if is active
     */
    public final boolean isActive() {
      return CallProvider.this.isActive();
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getType() {
      return CallProvider.this.getType();
    }

    /**
     * Gets the supported types.
     *
     * @return the supported types
     */
    public String[] getSupportedTypes() {
      return CallProvider.this.getSupportedTypes();
    }

    /**
     * Gets the title.
     *
     * @return the title
     */
    public String getTitle() {
      return CallProvider.this.getTitle();
    }
    
    /**
     * Gets the version.
     *
     * @return the version
     */
    public String getVersion() {
      return CallProvider.this.getVersion();
    }
  }
  
  /** The email test. */
  protected final Pattern             emailTest                     =
                                                Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

  /** The config. */
  protected final Map<String, String> config;

  /** The active flag. */
  protected boolean                   active;

  /**
   * Instantiates a new video calls provider.
   *
   * @param params the params
   * @throws ConfigurationException the configuration exception
   */
  public CallProvider(InitParams params) throws ConfigurationException {
    // Configuration
    PropertiesParam param = params.getPropertiesParam(CONFIG_PROVIDER_CONFIGURATION);
    if (param != null) {
      this.config = Collections.unmodifiableMap(param.getProperties());
      this.active = Boolean.valueOf(this.config.getOrDefault(CONFIG_PROVIDER_ACTIVE, Boolean.TRUE.toString()));
    } else {
      throw new ConfigurationException("Property parameters provider-configuration required.");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    // TODO consider calc the hash once and save in instance variable
    final String type = getType();
    if (type != null && type.length() > 0) {
      int hc = 7 + type.hashCode();
      final String version = getVersion();
      if (version != null) {
        hc = hc * 31 + version.hashCode();
      }
      // TODO not sure it should depend on supported types, they are optional and may be dynamic in runtime
      /*
       * final String[] stypes = getSupportedTypes();
       * if (stypes != null) {
       * for (String stype : stypes) {
       * hc = hc * 31 + stype.hashCode();
       * }
       * }
       */
      return hc;
    } else {
      return super.hashCode();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj != null && CallProvider.class.isAssignableFrom(obj.getClass())) {
      return CallProvider.class.cast(obj).getType().equals(this.getType());
    }
    return false;
  }

  /**
   * Checks if is active.
   *
   * @return true, if is active
   */
  public final boolean isActive() {
    return active;
  }

  /**
   * Sets the active.
   *
   * @param active the new active
   */
  final void setActive(boolean active) {
    this.active = active;
  }
  
  /**
   * Checks if it is a supported type by this provider.
   *
   * @param type the type
   * @return <code>true</code>, if is supported type, <code>false</code> otherwise
   */
  public boolean isSupportedType(String type) {
    return getType().equals(type);
  }

  /**
   * Gets human-readable name of this provider (e.g. 'Skype'). Provider name can be used in building UI
   * labels and messages.
   *
   * @return the name
   */
  public abstract String getTitle();

  /**
   * Technical details description for this provider. Will be used in administrative settings.
   *
   * @return the details
   */
  public String getDetails() {
    return this.getDescription();
  }

  /**
   * Gets the version.
   *
   * @return the version
   */
  public abstract String getVersion();

  /**
   * Gets the main type name of this provider (e.g. 'skype'). Provider type should be in lower case and
   * without
   * white spaces. A provider may support several types as well, to observe all supported types use
   * {@link #getSupportedTypes()}.
   *
   * @return the type
   */
  public abstract String getType();

  /**
   * Gets all types supported by this provider. Provider type should be in lower case and without
   * white spaces.
   *
   * @return the types array
   */
  public abstract String[] getSupportedTypes();

  /**
   * Gets the {@link IMInfo} instance for given IM identifier.
   *
   * @param imId the IM identifier
   * @return an instance of {@link IMInfo} or <code>null</code> if IM not supported by the provider
   * @throws CallProviderException if the provider cannot recognize given IM id or failed to instantiate
   *           an {@link IMInfo} object
   */
  public abstract IMInfo getIMInfo(String imId) throws CallProviderException;

}
