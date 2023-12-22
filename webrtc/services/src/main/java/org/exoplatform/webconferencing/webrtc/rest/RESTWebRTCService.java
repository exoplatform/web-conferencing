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
import javax.print.attribute.standard.Media;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.webconferencing.CallProvider;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.client.ErrorInfo;
import org.exoplatform.webconferencing.webrtc.WebrtcProvider;
import org.exoplatform.webconferencing.webrtc.WebrtcProvider.RTCConfiguration;


import org.json.JSONObject;

import static org.exoplatform.webconferencing.Utils.buildUrl;

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
@Tag(name = "/webrtc/webconferencing", description = "Operations on WebRTC provider settings")
public class RESTWebRTCService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log             LOG = ExoLogger.getLogger(RESTWebRTCService.class);

  /** The web conferencing. */
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
  @Operation(
          summary = "Updates RTC configuration in WebRTC settings",
          method = "POST",
          description = "Use this method to update RTC configuration of WebRTC provider. This operation only available to Administrator user.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled. Updated provider config returned."),
    @ApiResponse(responseCode = "401", description = "Unauthorized user (conversation state not present). Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
    @ApiResponse(responseCode = "404", description = "Provider (WebRTC) not found. Error code: " + ErrorInfo.CODE_NOT_FOUND_ERROR),
    @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding or formatting result to JSON. Error code: " + ErrorInfo.CODE_SERVER_ERROR)})
  public Response postSettings(@Context UriInfo uriInfo, 
                               @Parameter(description = "RTC configuration in JSON format. See WebrtcProvider.jsonToRtcConfig() for details", required = true) @FormParam("rtcConfiguration") String rtcConfig) {
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
  @Path("/settings") // TODO not used
  @Operation(
          summary = "Read WebRTC providers settings",
          method = "GET",
          description = "Use this method to read WebRTC providers settings. This operation only available to Administrator user.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled. Settings object returned."),
    @ApiResponse(responseCode = "401", description = "Unauthorized user (conversation state not present). Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
    @ApiResponse(responseCode = "404", description = "Provider (WebRTC) not found. Error code: " + ErrorInfo.CODE_NOT_FOUND_ERROR),
    @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding or formatting result to JSON. Error code: " + ErrorInfo.CODE_SERVER_ERROR)})
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

  @GET
  @RolesAllowed("administrators")
  @Path("/connectorsettings")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Read WebRTC connector settings",
      method = "GET",
      description = "Use this method to read WebRTC connecotr settings. This operation only available to Administrator user.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled. Settings object returned."),
      @ApiResponse(responseCode = "401", description = "Unauthorized user (conversation state not present). Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
      @ApiResponse(responseCode = "404", description = "Provider (WebRTC) not found. Error code: " + ErrorInfo.CODE_NOT_FOUND_ERROR),
      @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding or formatting result to JSON. Error code: " + ErrorInfo.CODE_SERVER_ERROR)})
  public Response getConnectorSettings(@Context UriInfo uriInfo, @Context HttpServletRequest request) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      try {
        WebrtcProvider webrtc = (WebrtcProvider) webConferencing.getProvider(WebrtcProvider.WEBRTC_TYPE);
        if (webrtc != null) {
          CallProvider.Settings settings = webrtc.settings()
                                                 .callUri(buildUrl(request.getScheme(),
                                request.getServerName(),
                                request.getServerPort(),
                                "/webrtc/call"))
                                                 .locale(request.getLocale())
                                                 .build();


          return Response.ok().cacheControl(cacheControl).entity(settings).build();
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
