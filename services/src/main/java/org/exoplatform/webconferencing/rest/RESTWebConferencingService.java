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
package org.exoplatform.webconferencing.rest;

import java.util.Locale;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.webconferencing.*;
import org.exoplatform.webconferencing.client.ErrorInfo;
import org.exoplatform.webconferencing.dao.StorageException;
import org.exoplatform.ws.frameworks.json.impl.JsonException;


/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: RESTWebConferencingService.java 00000 Feb 22, 2017 pnedonosko $
 */
@Path("/webconferencing")
@Tag(name = "/webconferencing", description = "Operations on call providers and participant information")
@Produces(MediaType.APPLICATION_JSON)
public class RESTWebConferencingService implements ResourceContainer {

  /** The Constant LOG. */
  protected static final Log             LOG   = ExoLogger.getLogger(RESTWebConferencingService.class);

  /** The web conferencing. */
  protected final WebConferencingService webConferencing;

  protected final LocaleConfigService localeConfigService;

  /** The cache control. */
  private final CacheControl             cacheControl;

  /**
   * Instantiates a new REST service for web conferencing.
   *
   * @param webConferencing the web conferencing
   */
  public RESTWebConferencingService(WebConferencingService webConferencing, LocaleConfigService localeConfigService) {
    this.webConferencing = webConferencing;
    this.localeConfigService = localeConfigService;
    this.cacheControl = new CacheControl();
    cacheControl.setNoCache(true);
    cacheControl.setNoStore(true);
  }

  /**
   * Gets the provider config.
   *
   * @param uriInfo the uri info
   * @param request the request
   * @param type the type
   * @return the provider config
   */
  @GET
  @RolesAllowed("administrators")
  @Path("/provider/{type}/configuration") // TODO not used
  @Operation(
          summary = "Read a call provider configuration",
          method = "GET",
          description = "Use this method to read a call provider configuration. This operation only available to Administrator user.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled. Provider configuration object returned."),
    @ApiResponse(responseCode = "401", description = "Unauthorized user (conversation state not present). Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
    @ApiResponse(responseCode = "404", description = "Provider not found. Error code: " + ErrorInfo.CODE_NOT_FOUND_ERROR),
    @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding or formatting result to JSON. Error code: " + ErrorInfo.CODE_SERVER_ERROR)})
  public Response getProviderConfig(@Context UriInfo uriInfo,
                                    @Context HttpServletRequest request,
                                    @Parameter(description = "Call provider type, ex: 'webrtc'", required = true) @PathParam("type") String type) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      try {
        CallProviderConfiguration conf = webConferencing.getProviderConfiguration(type, request.getLocale());
        if (conf != null) {
          return Response.ok().cacheControl(cacheControl).entity(conf).build();
        } else {
          return Response.status(Status.NOT_FOUND)
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.notFoundError("Provider or configuration not found"))
                         .build();
        }
      } catch (Throwable e) {
        LOG.error("Error reading provider configuration for '" + type + "' by '" + currentUserName + "'", e);
        return Response.serverError()
                       .cacheControl(cacheControl)
                       .entity(ErrorInfo.serverError("Error reading provider configuration"))
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
   * Post provider config.
   *
   * @param uriInfo the uri info
   * @param request the request
   * @param type the type
   * @param active the active
   * @return the response
   */
  @POST
  @RolesAllowed("administrators")
  @Path("/provider/{type}/configuration")
  @Operation(
          summary = "Updates a call provider activation status",
          method = "POST",
          description = "Use this method to enable or disable a call provider. This operation only available to Administrator user.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled. Updated provider config returned."),
      @ApiResponse(responseCode = "401", description = "Unauthorized user (conversation state not present). Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
      @ApiResponse(responseCode = "404", description = "Provider not found. Error code: " + ErrorInfo.CODE_NOT_FOUND_ERROR),
      @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding or formatting result to JSON. Error code: " + ErrorInfo.CODE_SERVER_ERROR)})
  public Response postProviderConfig(@Context UriInfo uriInfo,
                                     @Context HttpServletRequest request,
                                     @Parameter(description = "Call provider type, ex: 'webrtc'", required = true) @PathParam("type") String type,
                                     @Parameter(description = "Activation switch in form of boolean value (case insensitive), 'true' to enable, disable by any other value", required = true) @FormParam("active") String active) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      try {
        CallProviderConfiguration conf = webConferencing.getProviderConfiguration(type, request.getLocale());
        if (conf != null) {
          boolean activeVal = Boolean.valueOf(active);
          if (activeVal != conf.isActive()) {
            conf.setActive(activeVal);
            webConferencing.saveProviderConfiguration(conf);
          }
          return Response.ok().cacheControl(cacheControl).entity(conf).build();
        } else {
          return Response.status(Status.NOT_FOUND)
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.notFoundError("Provider or configuration not found"))
                         .build();
        }
      } catch (Throwable e) {
        LOG.error("Error saving provider configuration for '" + type + "' by '" + currentUserName + "'", e);
        return Response.serverError()
                       .cacheControl(cacheControl)
                       .entity(ErrorInfo.serverError("Error saving provider configuration"))
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
   * Gets the provider configs.
   *
   * @param uriInfo the uri info
   * @param request the request
   * @return the provider configs
   */
  @GET
  @RolesAllowed("administrators")
  @Path("/providers/configuration")
  @Operation(
          summary = "Read call providers configurations",
          method = "GET",
          description = "Use this method to read all providers configuration. This operation only available to Administrator user.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled. Providers configurations returned."),
    @ApiResponse(responseCode = "401", description = "Unauthorized user (conversation state not present). Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
    @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding or formatting result to JSON. Error code: " + ErrorInfo.CODE_SERVER_ERROR)})
  public Response getProviderConfigs(@Context UriInfo uriInfo, @Context HttpServletRequest request) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      try {
        Set<CallProviderConfiguration> confs = webConferencing.getProviderConfigurations(request.getLocale());
        return Response.ok().cacheControl(cacheControl).entity(confs).build();
      } catch (Throwable e) {
        LOG.error("Error reading providers configuration by '" + currentUserName + "'", e);
        return Response.serverError()
                       .cacheControl(cacheControl)
                       .entity(ErrorInfo.serverError("Error reading providers configuration"))
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
   * Gets the user info.
   *
   * @param uriInfo
   *          the uri info
   * @param userName
   *          the id
   * @return the user info response
   */
  @GET
  @RolesAllowed("users")
  @Path("/user/{name}")
  @Operation(
          summary = "Return user information",
          method = "GET",
          description = "Use this method to read an user info used as call owner or participants. This operation is available to all Platform users.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled. User info object returned."),
    @ApiResponse(responseCode = "400", description = "Wrong request parameters: name. Error code: " + ErrorInfo.CODE_CLIENT_ERROR),
    @ApiResponse(responseCode = "401", description = "Unauthorized user (conversation state not present). Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
    @ApiResponse(responseCode = "404", description = "User not found. Error code: " + ErrorInfo.CODE_NOT_FOUND_ERROR),
    @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding or formatting result to JSON. Error code: " + ErrorInfo.CODE_SERVER_ERROR)})
  public Response getUserInfo(@Context UriInfo uriInfo, 
                              @Parameter(description = "Call provider name, ex: 'webrtc'", required = true) @PathParam("name") String userName) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      if (userName != null) {
        try {
          UserInfo user = webConferencing.getUserInfo(userName);
          if (user != null) {
            return Response.ok().cacheControl(cacheControl).entity(user).build();
          } else {
            return Response.status(Status.NOT_FOUND)
                           .cacheControl(cacheControl)
                           .entity(ErrorInfo.notFoundError("User not found or not accessible"))
                           .build();
          }
        } catch (Throwable e) {
          LOG.error("Error reading user info of '" + userName + "' by '" + currentUserName + "'", e);
          return Response.serverError()
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.serverError("Error reading user " + userName))
                         .build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST)
                       .cacheControl(cacheControl)
                       .entity(ErrorInfo.clientError("Wrong request parameters: name"))
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
   * Gets the Call context info.
   *
   * @param uriInfo
   *          the uri info
   * @param userName
   *          the id
   * @param language
   *          the current language of the user
   * @return the user info response
   */
  @GET
  @RolesAllowed("users")
  @Path("/context")
  @Operation(
          summary = "Return the current context of the call",
          method = "GET",
          description = "Use this method to read the current context of the call. This operation is available to all Platform users.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled. Call context object returned."),
    @ApiResponse(responseCode = "400", description = "Wrong request parameters: name or language. Error code: " + ErrorInfo.CODE_CLIENT_ERROR),
    @ApiResponse(responseCode = "401", description = "Unauthorized user (conversation state not present). Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
     @ApiResponse(responseCode = "500", description = "Internal server error due to data encoding or formatting result to JSON. Error code: " + ErrorInfo.CODE_SERVER_ERROR)})
  public Response getContext(@Context UriInfo uriInfo,
                              @Parameter(description = "User name", required = true) @QueryParam("name") String userName,
                              @Parameter(description = "Language", required = true) @QueryParam("lang") String language) {
    Locale currentLocale = localeConfigService.getDefaultLocaleConfig().getLocale();
    if(StringUtils.isBlank(userName)) {
      return Response.status(Status.BAD_REQUEST)
              .cacheControl(cacheControl)
              .entity(ErrorInfo.clientError("Wrong request parameters: name"))
              .build();
    }
    if(StringUtils.isNotBlank(language)) {
      currentLocale = Locale.forLanguageTag(language);
    }
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      if (StringUtils.isNotBlank(userName) && userName.equals(currentUserName)) {
        ContextInfo context = Utils.getCurrentContext(userName, currentLocale);
        try {
          return Response.ok().cacheControl(cacheControl).entity(Utils.asJSON(context)).build();
        } catch (JsonException jsonException) {
          return Response.serverError()                         .cacheControl(cacheControl)
                  .entity(ErrorInfo.serverError("Error creating Json for context "))
                  .build();
        }
      } else {
        return Response.status(Status.UNAUTHORIZED)
                .cacheControl(cacheControl)
                .entity(ErrorInfo.accessError("Unauthorized user"))
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
   * Gets the space info.
   *
   * @param uriInfo
   *          the uri info
   * @param spaceName
   *          the space name
   * @return the space info response
   */
  @GET
  @RolesAllowed("users")
  @Path("/space/{spaceName}")
  @Operation(
          summary = "Return social space information",
          method = "GET",
          description = "Use this method to read a Social space info used as call owner and origins. This operation is available to all Platform users.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled. Space info object returned."),
    @ApiResponse(responseCode = "400", description = "Wrong request parameters: spaceName. Error code: " + ErrorInfo.CODE_CLIENT_ERROR),
    @ApiResponse(responseCode = "401", description = "Unauthorized user (conversation state not present). Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
    @ApiResponse(responseCode = "403", description = "Not space member. Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
    @ApiResponse(responseCode = "404", description = "Space not found or not accessible. Error code: " + ErrorInfo.CODE_NOT_FOUND_ERROR),
    @ApiResponse(responseCode = "500", description = "Internal server error due to data reading from DB, its encoding or formatting result to JSON. Error code: " + ErrorInfo.CODE_SERVER_ERROR)})
  public Response getSpaceInfo(@Context UriInfo uriInfo, 
                               @Parameter(description = "Space pretty name, ex: 'sales_team'", required = true) @PathParam("spaceName") String spaceName) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      if (spaceName != null && spaceName.length() > 0) {
        try {
          GroupInfo space = webConferencing.getSpaceInfo(spaceName);
          if (space != null) {
            if (space.getMembers().containsKey(currentUserName)) {
              return Response.ok().cacheControl(cacheControl).entity(space).build();
            } else {
              return Response.status(Status.FORBIDDEN)
                             .cacheControl(cacheControl)
                             .entity(ErrorInfo.accessError("Not space member"))
                             .build();
            }
          } else {
            return Response.status(Status.NOT_FOUND)
                           .cacheControl(cacheControl)
                           .entity(ErrorInfo.notFoundError("Space not found or not accessible"))
                           .build();
          }
        } catch (IdentityStateException e) {
          LOG.error("Error reading member of space '" + spaceName + "' by '" + currentUserName + "'", e);
          return Response.serverError()
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.serverError("Error reading member of space '" + spaceName + "'"))
                         .build();
        } catch (StorageException e) {
          LOG.error("Storage error for space info of '" + spaceName + "' by '" + currentUserName + "'", e);
          return Response.serverError()
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.serverError("Storage error for space '" + spaceName + "'"))
                         .build();
        } catch (Throwable e) {
          LOG.error("Error reading space info of '" + spaceName + "' by '" + currentUserName + "'", e);
          return Response.serverError()
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.serverError("Error reading space " + spaceName))
                         .build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST)
                       .cacheControl(cacheControl)
                       .entity(ErrorInfo.clientError("Wrong request parameters: spaceName"))
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
   * Gets the space event info.
   *
   * @param uriInfo the uri info
   * @param spaceIdentityId the space identity id
   * @return the space event info response
   */
    @GET
    @RolesAllowed("users")
    @Path("/space-event/{spaceIdentityId}")
    @Operation(
            summary = "Return a Social space event information",
            method = "GET",
            description = "Use this method to read a Social space event used as call origin. This operation is available to all Platform users.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled. Space event info object returned."),
      @ApiResponse(responseCode = "400", description = "Wrong request parameters: spaceName. Error code: " + ErrorInfo.CODE_CLIENT_ERROR),
      @ApiResponse(responseCode = "400", description = "Wrong request parameters: participants. Error code: " + ErrorInfo.CODE_CLIENT_ERROR),
      @ApiResponse(responseCode = "400", description = "Wrong request parameters: spaces. Error code: " + ErrorInfo.CODE_CLIENT_ERROR),
      @ApiResponse(responseCode = "401", description = "Unauthorized user (conversation state not present). Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
      @ApiResponse(responseCode = "403", description = "Not space member. Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
      @ApiResponse(responseCode = "404", description = "Space not found or not accessible. Error code: " + ErrorInfo.CODE_NOT_FOUND_ERROR),
      @ApiResponse(responseCode = "500", description = "Internal server error due to data reading from DB, its encoding or formatting result to JSON. Error code: " + ErrorInfo.CODE_SERVER_ERROR)})
    public Response getSpaceEventInfo(@Context UriInfo uriInfo,
                                      @Parameter(description = "Space pretty name used as the event host, ex: 'sales_team'", required = true) @PathParam("spaceIdentityId") String spaceIdentityId,
                                      @Parameter(description = "Participants directly invited to the event, a string of comma-separated names, ex: 'john,mary,james'", required = true) @QueryParam("participants") String participants,
                                      @Parameter(description = "Space pretty names for inviting its participants to the event, a string of comma-separated names, ex: 'sales_team,acme_project,ux_pride'", required = true) @QueryParam("spaces") String spaces) {
      ConversationState convo = ConversationState.getCurrent();
      if (convo != null) {
        String currentUserName = convo.getIdentity().getUserId();
        if (spaceIdentityId != null && spaceIdentityId.length() > 0) {
          if (participants != null && participants.length() > 0) {
            if (spaces != null && spaces.length() > 0) {
              try {
                GroupInfo space = webConferencing.getSpaceEventInfo(spaceIdentityId,
                                                                    participants.trim().split(";"),
                                                                    spaces.trim().split(";"));
                if (space != null) {
                  if (space.getMembers().containsKey(currentUserName)) {
                    return Response.ok().cacheControl(cacheControl).entity(space).build();
                  } else {
                    return Response.status(Status.FORBIDDEN)
                                   .cacheControl(cacheControl)
                                   .entity(ErrorInfo.accessError("Not space member"))
                                   .build();
                  }
                } else {
                  return Response.status(Status.NOT_FOUND)
                                 .cacheControl(cacheControl)
                                 .entity(ErrorInfo.notFoundError("Space not found or not accessible"))
                                 .build();
                }
              } catch (IdentityStateException e) {
                LOG.error("Error reading member of space with id'" + spaceIdentityId + "' by '" + currentUserName + "'", e);
                return Response.serverError()
                               .cacheControl(cacheControl)
                               .entity(ErrorInfo.serverError("Error reading member of space with id'" + spaceIdentityId + "'"))
                               .build();
              } catch (StorageException e) {
                LOG.error("Storage error for space event info of space with id'" + spaceIdentityId + "' by '" + currentUserName + "'", e);
                return Response.serverError()
                               .cacheControl(cacheControl)
                               .entity(ErrorInfo.serverError("Storage error for space with id'" + spaceIdentityId + "'"))
                               .build();
              } catch (Throwable e) {
                LOG.error("Error reading space event info of space with id'" + spaceIdentityId + "' by '" + currentUserName + "'", e);
                return Response.serverError()
                               .cacheControl(cacheControl)
                               .entity(ErrorInfo.serverError("Error reading space with id" + spaceIdentityId))
                               .build();
              }
            } else {
              return Response.status(Status.BAD_REQUEST)
                             .cacheControl(cacheControl)
                             .entity(ErrorInfo.clientError("Wrong request parameters: spaces"))
                             .build();
            }
          } else {
            return Response.status(Status.BAD_REQUEST)
                           .cacheControl(cacheControl)
                           .entity(ErrorInfo.clientError("Wrong request parameters: participants"))
                           .build();
          }
        } else {
          return Response.status(Status.BAD_REQUEST)
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.clientError("Wrong request parameters: spaceName"))
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
   * Gets the chat room info.
   *
   * @param uriInfo the uri info
   * @param roomId the room id
   * @param roomTitle the room title
   * @param roomMembers the room members
   * @return the room info response
   */
  @GET
  @RolesAllowed("users")
  @Path("/room/{id}")
  @Operation(
          summary = "Return chat room information",
          method = "GET",
          description = "Use this method to chat room info used as call owner and origins. This operation is available to all Platform users.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled. Chat room info object returned."),
    @ApiResponse(responseCode = "400", description = "Wrong request parameters: id. Error code: " + ErrorInfo.CODE_CLIENT_ERROR),
    @ApiResponse(responseCode = "400", description = "Wrong request parameters: title. Error code: " + ErrorInfo.CODE_CLIENT_ERROR),
    @ApiResponse(responseCode = "400", description = "Wrong request parameters: members. Error code: " + ErrorInfo.CODE_CLIENT_ERROR),
    @ApiResponse(responseCode = "401", description = "Unauthorized user (conversation state not present). Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
    @ApiResponse(responseCode = "403", description = "Not room member. Error code: " + ErrorInfo.CODE_ACCESS_ERROR),
    @ApiResponse(responseCode = "404", description = "Room not found or not accessible. Error code: " + ErrorInfo.CODE_NOT_FOUND_ERROR),
    @ApiResponse(responseCode = "500", description = "Internal server error due to data reading from DB, its encoding or formatting result to JSON. Error code: " + ErrorInfo.CODE_SERVER_ERROR)})
  public Response getRoomInfo(@Context UriInfo uriInfo,
                              @Parameter(description = "Room ID, ex: 'team-ec5e257858734e40a98505475d8eedc4'", required = true) @PathParam("id") String roomId,
                              @Parameter(description = "Room title, ex: 'ACME meetings'", required = true) @QueryParam("title") String roomTitle,
                              @Parameter(description = "Room members (platform users or external Chat users), a string of comma-separated names, ex: 'john,james,julia'", required = true) @QueryParam("members") String roomMembers) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      if (roomId != null && roomId.length() > 0) {
        if (roomTitle != null && roomTitle.length() > 0) {
          if (roomMembers != null && roomMembers.length() > 0) {
            try {
              GroupInfo room = webConferencing.getRoomInfo(roomId, roomTitle, roomMembers.trim().split(";"));
              if (room != null) {
                if (room.getMembers().containsKey(currentUserName)) {
                  return Response.ok().cacheControl(cacheControl).entity(room).build();
                } else {
                  return Response.status(Status.FORBIDDEN)
                                 .cacheControl(cacheControl)
                                 .entity(ErrorInfo.accessError("Not room member"))
                                 .build();
                }
              } else {
                // FYI this will not happen until we don't request chat server database
                return Response.status(Status.NOT_FOUND)
                               .cacheControl(cacheControl)
                               .entity(ErrorInfo.notFoundError("Room not found or not accessible"))
                               .build();
              }
            } catch (IdentityStateException e) {
              LOG.error("Error reading member of room '" + roomTitle + "' by '" + currentUserName + "'", e);
              return Response.serverError()
                             .cacheControl(cacheControl)
                             .entity(ErrorInfo.serverError("Error reading member of room '" + roomTitle + "'"))
                             .build();
            } catch (StorageException e) {
              LOG.error("Storage error for room info of '" + roomTitle + "' by '" + currentUserName + "'", e);
              return Response.serverError()
                             .cacheControl(cacheControl)
                             .entity(ErrorInfo.serverError("Storage error for room '" + roomTitle + "'"))
                             .build();
            } catch (Throwable e) {
              LOG.error("Error reading room info of '" + roomTitle + "' by '" + currentUserName + "'", e);
              return Response.serverError()
                             .cacheControl(cacheControl)
                             .entity(ErrorInfo.serverError("Error reading room '" + roomTitle + "'"))
                             .build();
            }
          } else {
            return Response.status(Status.BAD_REQUEST)
                           .cacheControl(cacheControl)
                           .entity(ErrorInfo.clientError("Wrong request parameters: members"))
                           .build();
          }
        } else {
          return Response.status(Status.BAD_REQUEST)
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.clientError("Wrong request parameters: title"))
                         .build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST)
                       .cacheControl(cacheControl)
                       .entity(ErrorInfo.clientError("Wrong request parameters: id"))
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
