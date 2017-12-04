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
package org.exoplatform.webconferencing.webrtc.rest;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.client.ErrorInfo;
import org.exoplatform.webconferencing.webrtc.WebrtcProvider;
import org.exoplatform.webconferencing.webrtc.WebrtcProvider.RTCConfiguration;
import org.json.JSONObject;

/**
 * REST service for WebRTC provider in Web Conferencing.
 * 
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: RESTWebRTCService.java 00000 Feb 22, 2017 pnedonosko $
 */
@Path("/webrtc/webconferencing")
@Produces(MediaType.APPLICATION_JSON)
public class RESTWebRTCService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log             LOG = ExoLogger.getLogger(RESTWebRTCService.class);

  /** The video calls. */
  protected final WebConferencingService webConferencing;

  /** The cache control. */
  private final CacheControl             cacheControl;

  /**
   * Instantiates a new REST service for WebRTC provider in Web Conferencing.
   *
   * @param webConferencing
   *          the skype
   */
  public RESTWebRTCService(WebConferencingService webConferencing) {
    this.webConferencing = webConferencing;
    this.cacheControl = new CacheControl();
    this.cacheControl.setNoCache(true);
    this.cacheControl.setNoStore(true);
  }

  /**
   * Post RTC settings.
   *
   * @param uriInfo the uri info
   * @param rtcConfig the rtc config
   * @return the response
   */
  @POST
  @RolesAllowed("administrators")
  @Path("/settings")
  public Response postSettings(@Context UriInfo uriInfo, @FormParam("rtcConfiguration") String rtcConfig) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      try {
        WebrtcProvider webrtc = (WebrtcProvider) webConferencing.getProvider(WebrtcProvider.WEBRTC_TYPE);
        if (webrtc != null) {
          RTCConfiguration conf = webrtc.jsonToRtcConfig(new JSONObject(rtcConfig));
          webrtc.saveRtcConfiguration(conf);
          return Response.ok().cacheControl(cacheControl).entity(conf).build();
        } else {
          return Response.status(Status.NOT_FOUND)
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.notFoundError("WebRTC provider not found"))
                         .build();
        }
      } catch (Throwable e) {
        LOG.error("Error saving WebRTC settings by '" + currentUserName + "'", e);
        return Response.serverError()
                       .cacheControl(cacheControl)
                       .entity(ErrorInfo.serverError("Error saving WebRTC settings"))
                       .build();
      }
    } else {
      return Response.status(Status.UNAUTHORIZED)
                     .cacheControl(cacheControl)
                     .entity(ErrorInfo.accessError("Unauthorized user"))
                     .build();
    }
  }

  /**
   * Gets the settings.
   *
   * @param uriInfo the uri info
   * @return the settings
   */
  @GET
  @RolesAllowed("administrators")
  @Path("/settings")
  @Deprecated // TODO not used
  public Response getSettings(@Context UriInfo uriInfo) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      try {
        WebrtcProvider webrtc = (WebrtcProvider) webConferencing.getProvider(WebrtcProvider.WEBRTC_TYPE);
        if (webrtc != null) {
          return Response.ok().cacheControl(cacheControl).entity(webrtc.getRtcConfiguration()).build();
        } else {
          return Response.status(Status.NOT_FOUND)
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.notFoundError("WebRTC provider not found"))
                         .build();
        }
      } catch (Throwable e) {
        LOG.error("Error getting WebRTC settings by '" + currentUserName + "'", e);
        return Response.serverError()
                       .cacheControl(cacheControl)
                       .entity(ErrorInfo.serverError("Error getting WebRTC settings"))
                       .build();
      }
    } else {
      return Response.status(Status.UNAUTHORIZED)
                     .cacheControl(cacheControl)
                     .entity(ErrorInfo.accessError("Unauthorized user"))
                     .build();
    }
  }
}
