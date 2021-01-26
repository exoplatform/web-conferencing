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

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.application.RequestNavigationData;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.social.common.router.ExoRouter;
import org.exoplatform.social.common.router.ExoRouter.Route;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webconferencing.cometd.CometdWebConferencingService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: Utils.java 00000 Mar 30, 2017 pnedonosko $
 */
public class Utils {

    /**
   * Generate a space room name.
   *
   * @param spacePrettyName the space pretty name
   * @return the string
   */
  public static String spaceRoomName(String spacePrettyName) {
    StringBuilder sname = new StringBuilder();
    sname.append("eXoWebConferencing");
    for (String s : spacePrettyName.split("_")) {
      if (s.length() > 0) {
        sname.append(Character.toUpperCase(s.charAt(0)));
        if (s.length() > 1) {
          sname.append(s.substring(1));
        }
      }
    }
    sname.append("Space");
    return sname.toString();
  }

  /**
   * Gets the space by context.
   *
   * @return the space by context
   */
  public static Space getSpaceByContext() {
    WebuiRequestContext webuiContext = WebuiRequestContext.getCurrentInstance();
    if (webuiContext != null) {
      SpaceService spaceService = webuiContext.getUIApplication().getApplicationComponent(SpaceService.class);
      if (spaceService != null) {
        String spacePrettyName = getSpaceNameByContext();
        if (spacePrettyName != null) {
          return spaceService.getSpaceByPrettyName(spacePrettyName);
        }
      }
    }
    return null;
  }

  /**
   * Gets the space name by context.
   *
   * @return the space name in portal context
   */
  public static String getSpaceNameByContext() {
    // Idea of this method build on SpaceUtils.getSpaceByContext()
    PortalRequestContext portlalContext;
    WebuiRequestContext webuiContext = WebuiRequestContext.getCurrentInstance();
    if (webuiContext != null) {
      if (webuiContext instanceof PortalRequestContext) {
        portlalContext = (PortalRequestContext) webuiContext;
      } else {
        portlalContext = (PortalRequestContext) webuiContext.getParentAppRequestContext();
      }

      String requestPath = portlalContext.getControllerContext().getParameter(RequestNavigationData.REQUEST_PATH);
      Route route = ExoRouter.route(requestPath);
      if (route != null) {
        if (portlalContext.getSiteType().equals(SiteType.GROUP)
            && portlalContext.getSiteName().startsWith(SpaceUtils.SPACE_GROUP)) {
          return route.localArgs.get("spacePrettyName");
        }
      }
    }
    return null;
  }

  /**
   * Gets the current context.
   *
   * @param userId the user id
   * @param locale the locale
   * @return the current context
   */
  public static ContextInfo getCurrentContext(String userId, Locale locale) {
    String spaceRoomName;
    String spacePrettyName = Utils.getSpaceNameByContext();
    if (spacePrettyName != null) {
      // TODO do we need a room name? what if chat room?
      spaceRoomName = Utils.spaceRoomName(spacePrettyName);
    } else {
      spacePrettyName = spaceRoomName = IdentityInfo.EMPTY;
    }
    ExoContainer exo = ExoContainerContext.getCurrentContainer();
    WebConferencingService webConferencing = exo.getComponentInstanceOfType(WebConferencingService.class);
    CometdWebConferencingService cometdService = exo.getComponentInstanceOfType(CometdWebConferencingService.class);
    ContextInfo context;
    if (cometdService != null) {
      context = new ContextInfo(exo.getContext().getName(),
                                spacePrettyName,
                                spaceRoomName,
                                cometdService.getCometdServerPath(),
                                cometdService.getUserToken(userId),
                                webConferencing.getProviderConfigurations());
    } else {
      context = new ContextInfo(exo.getContext().getName(),
                                spacePrettyName,
                                spaceRoomName,
                                webConferencing.getProviderConfigurations());
    }
    if (locale != null) {
      context.addMessages(getResourceMessages("locale.webconferencing.WebConferencingClient", locale));
    }
    return context;
  }

  /**
   * Gets the resource messages.
   *
   * @param bundleName the bundle name
   * @param locale the locale
   * @return the resource messages
   */
  public static Map<String, String> getResourceMessages(String bundleName, Locale locale) {
    ExoContainer exo = ExoContainerContext.getCurrentContainer();
    ResourceBundleService service = exo.getComponentInstanceOfType(ResourceBundleService.class);
    ResourceBundle res = service.getResourceBundle(bundleName, locale);
    Map<String, String> resMap = new HashMap<String, String>();
    for (Enumeration<String> keys = res.getKeys(); keys.hasMoreElements();) {
      String key = keys.nextElement();
      resMap.put(key, res.getString(key));
    }
    return resMap;
  }

  /**
   * Gets the resource message.
   *
   * @param bundleName the bundle name
   * @param locale the locale
   * @param messageKey the message key
   * @return the resource message
   */
  public static String getResourceMessage(String bundleName, Locale locale, String messageKey) {
    ExoContainer exo = ExoContainerContext.getCurrentContainer();
    ResourceBundleService service = exo.getComponentInstanceOfType(ResourceBundleService.class);
    ResourceBundle res = service.getResourceBundle(bundleName, locale);
    return res.getString(messageKey);
  }

  /**
   * Format specified object to JSON string.
   *
   * @param obj the obj
   * @return the string
   * @throws JsonException the json exception
   */
  public static String asJSON(Object obj) throws JsonException {
    if (obj != null) {
      JsonGeneratorImpl gen = new JsonGeneratorImpl();
      if (obj.getClass().isArray()) {
        return gen.createJsonArray(obj).toString();
      } else if (CallInfo.class.isAssignableFrom(obj.getClass())) {
        // Special handling for CallInfo to return its dates in standard format.
        return CallInfo.class.cast(obj).toJSON();
      } else {
        return gen.createJsonObject(obj).toString();
      }
    } else {
      return "null".intern();
    }
  }
  
  /**
   * Parses the date from a ISO-8601 formatted string.
   *
   * @param date the date
   * @return the date
   * @throws ParseException the parse exception
   */
  public static Date parseISODate(String date) throws ParseException {
    if (date != null) {
      return ISO8601.parse(date).getTime();
    } else {
      return null;
    }
  }
  
  /**
   * Format date as ISO-8601 string with UTC time zone.
   *
   * @param date the date
   * @return the string
   */
  public static String formatISODate(Date date) {
    if (date != null) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(date);
      calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
      return ISO8601.format(calendar);
    } else {
      return null;
    }
  }

  /**
   * Build client URL using given parameters.
   *
   * @param protocol the scheme
   * @param hostname the host name
   * @param port the port
   * @param path the path
   * @return the string
   * @throws MalformedURLException if the protocol hasn't a handled
   */
  public static String buildUrl(String protocol, String hostname, int port, String path) throws MalformedURLException {
    return new URL(protocol, hostname, port, path).toString();
  }

}
