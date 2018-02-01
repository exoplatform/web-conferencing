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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.CallInfoException;
import org.exoplatform.webconferencing.CallProviderConfiguration;
import org.exoplatform.webconferencing.CallState;
import org.exoplatform.webconferencing.GroupInfo;
import org.exoplatform.webconferencing.IdentityNotFound;
import org.exoplatform.webconferencing.InvalidCallStateException;
import org.exoplatform.webconferencing.UserInfo;
import org.exoplatform.webconferencing.UserState;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.client.ErrorInfo;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: RESTWebConferencingService.java 00000 Feb 22, 2017 pnedonosko $
 */
@Path("/webconferencing")
@Produces(MediaType.APPLICATION_JSON)
public class RESTWebConferencingService implements ResourceContainer {

  /** The Constant ME. */
  public static final String             ME    = "me";

  /** The Constant EMPTY. */
  public static final String             EMPTY = "".intern();

  /** The Constant LOG. */
  protected static final Log             LOG   = ExoLogger.getLogger(RESTWebConferencingService.class);

  /** The web conferencing. */
  protected final WebConferencingService webConferencing;

  /** The cache control. */
  private final CacheControl             cacheControl;

  /**
   * Instantiates a new REST service for web conferencing.
   *
   * @param webConferencing the web conferencing
   */
  public RESTWebConferencingService(WebConferencingService webConferencing) {
    this.webConferencing = webConferencing;
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
  public Response getProviderConfig(@Context UriInfo uriInfo,
                                    @Context HttpServletRequest request,
                                    @PathParam("type") String type) {
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
  public Response postProviderConfig(@Context UriInfo uriInfo,
                                     @Context HttpServletRequest request,
                                     @PathParam("type") String type,
                                     @FormParam("active") String active) {
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
  public Response getUserInfo(@Context UriInfo uriInfo, @PathParam("name") String userName) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      if (userName != null) {
        if (ME.equals(userName)) {
          userName = currentUserName;
        }
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
   * Gets the user calls with possibility to filter by call state.
   *
   * @param uriInfo the uri info
   * @param userName the id
   * @param callState the call state
   * @return the user calls response
   */
  @GET
  @RolesAllowed("users")
  @Path("/user_/{name}/calls")
  @Deprecated // CometD used instead
  public Response getUserCalls(@Context UriInfo uriInfo,
                               @PathParam("name") String userName,
                               @QueryParam("state") String callState /* TODO not used */) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      if (userName != null) {
        if (ME.equals(userName)) {
          userName = currentUserName;
        }
        if (userName.equals(currentUserName)) {
          if (callState != null && callState.length() == 0) {
            callState = null;
          }
          try {
            CallState[] calls = webConferencing.getUserCalls(userName);
            if (calls != null) {
              return Response.ok().cacheControl(cacheControl).entity(calls).build();
            } else {
              return Response.serverError()
                             .cacheControl(cacheControl)
                             .entity(ErrorInfo.serverError("Error reading calls storage (null)"))
                             .build();
            }
          } catch (Throwable e) {
            LOG.error("Error reading user calls of '" + userName + "' by '" + currentUserName + "'", e);
            return Response.serverError()
                           .cacheControl(cacheControl)
                           .entity(ErrorInfo.serverError("Error reading user '" + userName + "' calls"))
                           .build();
          }
        } else {
          return Response.status(Status.BAD_REQUEST)
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.clientError("Wrong request parameters: name (does not match)"))
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
   * Update the user call with given state.
   *
   * @param uriInfo the uri info
   * @param userName the id
   * @param type the type
   * @param id the id
   * @param state the state
   * @param clientId the client id
   * @return the user calls response
   */
  @PUT
  @RolesAllowed("users")
  @Path("/user_/{name}/call/{type}/{id}")
  @Deprecated // CometD used instead
  public Response putUserCall(@Context UriInfo uriInfo,
                              @PathParam("name") String userName,
                              @PathParam("type") String type,
                              @PathParam("id") String id,
                              @FormParam("state") String state,
                              @FormParam("clientId") String clientId) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      if (userName != null) {
        if (ME.equals(userName)) {
          userName = currentUserName;
        }
        if (userName.equals(currentUserName)) {
          String callId = callId(type, id);
          try {
            if (UserState.JOINED.equals(state)) {
              CallInfo call = webConferencing.joinCall(callId, userName, clientId);
              if (call != null) {
                return Response.ok().cacheControl(cacheControl).entity(call).build();
              } else {
                return Response.status(Status.NOT_FOUND)
                               .cacheControl(cacheControl)
                               .entity(ErrorInfo.notFoundError("Call not found"))
                               .build();
              }
            } else if (UserState.LEAVED.equals(state)) {
              CallInfo call = webConferencing.leaveCall(callId, userName, clientId);
              if (call != null) {
                return Response.ok().cacheControl(cacheControl).entity(call).build();
              } else {
                return Response.status(Status.NOT_FOUND)
                               .cacheControl(cacheControl)
                               .entity(ErrorInfo.notFoundError("Call not found"))
                               .build();
              }
            }
            return Response.ok().build();
          } catch (InvalidCallStateException e) {
            return Response.status(Status.BAD_REQUEST)
                           .cacheControl(cacheControl)
                           .entity(ErrorInfo.clientError(e.getMessage()))
                           .build();
          } catch (Throwable e) {
            LOG.error("Error adding user call by '" + currentUserName + "'", e);
            return Response.serverError().entity(ErrorInfo.serverError("Error adding user call for " + userName)).build();
          }
        } else {
          return Response.status(Status.BAD_REQUEST)
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.clientError("Wrong request parameters: name (does not match)"))
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
   * Gets the users info.
   *
   * @param uriInfo
   *          the uri info
   * @param names
   *          user names
   * @return the users info response
   */
  @GET
  @RolesAllowed("users")
  @Path("/users")
  @Deprecated // not used
  public Response getUsersInfo(@Context UriInfo uriInfo, @QueryParam("names") String names) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      if (names != null) {
        try {
          Set<UserInfo> users = new LinkedHashSet<>();
          for (String userName : names.trim().split(";")) {
            if (ME.equals(userName)) {
              userName = currentUserName;
            }
            UserInfo user = webConferencing.getUserInfo(userName);
            if (user != null) {
              users.add(user);
            } else {
              if (LOG.isDebugEnabled()) {
                LOG.debug("Skipped not found user: " + userName);
              }
              return Response.status(Status.NOT_FOUND)
                             .cacheControl(cacheControl)
                             .entity(ErrorInfo.notFoundError("User " + userName + " not found or not accessible"))
                             .build();
            }
          }
          return Response.ok().cacheControl(cacheControl).entity(users.toArray()).build();
        } catch (Throwable e) {
          LOG.error("Error reading users info of '" + names + "' by '" + currentUserName + "'", e);
          return Response.serverError()
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.serverError("Error reading users " + names))
                         .build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST)
                       .cacheControl(cacheControl)
                       .entity(ErrorInfo.clientError("Wrong request parameters: names"))
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
  public Response getSpaceInfo(@Context UriInfo uriInfo, @PathParam("spaceName") String spaceName) {
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
   * Gets the chat room info.
   *
   * @param uriInfo the uri info
   * @param roomName the room name
   * @param roomId the room id
   * @param roomTitle the room title
   * @param roomMembers the room members
   * @return the room info response
   */
  @GET
  @RolesAllowed("users")
  @Path("/room/{name}/{id}")
  public Response getRoomInfo(@Context UriInfo uriInfo,
                              @PathParam("name") String roomName,
                              @PathParam("id") String roomId,
                              @QueryParam("title") String roomTitle,
                              @QueryParam("members") String roomMembers) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String currentUserName = convo.getIdentity().getUserId();
      if (roomId != null && roomId.length() > 0) {
        if (roomTitle != null && roomTitle.length() > 0) {
          if (roomName == null || roomName.length() == 0) {
            roomName = roomTitle.toUpperCase().toLowerCase().replace(' ', '_');
          }
          if (roomMembers != null && roomMembers.length() > 0) {
            try {
              GroupInfo room = webConferencing.getRoomInfo(roomId, roomName, roomTitle, roomMembers.trim().split(";"));
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
            } catch (IdentityNotFound e) {
              if (LOG.isDebugEnabled()) {
                LOG.debug("Room member not found", e);
              }
              return Response.status(Status.NOT_FOUND)
                             .cacheControl(cacheControl)
                             .entity(ErrorInfo.notFoundError(e.getMessage()))
                             .build();
            } catch (Throwable e) {
              LOG.error("Error reading room info of '" + roomName + "' by '" + currentUserName + "'", e);
              return Response.serverError()
                             .cacheControl(cacheControl)
                             .entity(ErrorInfo.serverError("Error reading room " + roomName))
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
                         .entity(ErrorInfo.clientError("Wrong request parameters: name"))
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

  /**
   * Gets the call info.
   *
   * @param uriInfo the uri info
   * @param type the type
   * @param id the id
   * @return the call info
   */
  @GET
  @RolesAllowed("users")
  @Path("/call_/{type}/{id}")
  @Deprecated // CometD used instead
  public Response getCallInfo(@Context UriInfo uriInfo, @PathParam("type") String type, @PathParam("id") String id) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String callId = callId(type, id);
      String currentUserName = convo.getIdentity().getUserId();
      try {
        CallInfo call = webConferencing.getCall(callId);
        if (call != null) {
          return Response.ok().cacheControl(cacheControl).entity(call).build();
        } else {
          return Response.status(Status.NOT_FOUND)
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.notFoundError("Call not found"))
                         .build();
        }
      } catch (Throwable e) {
        LOG.error("Error reading call info for '" + callId + "' by '" + currentUserName + "'", e);
        return Response.serverError()
                       .cacheControl(cacheControl)
                       .entity(ErrorInfo.serverError("Error reading call information"))
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
   * Delete call.
   *
   * @param uriInfo the uri info
   * @param type the type
   * @param id the id
   * @return the response
   */
  @DELETE
  @RolesAllowed("users")
  @Path("/call_/{type}/{id}")
  @Deprecated // CometD used instead
  public Response deleteCall(@Context UriInfo uriInfo, @PathParam("type") String type, @PathParam("id") String id) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String callId = callId(type, id);
      String currentUserName = convo.getIdentity().getUserId();
      try {
        CallInfo call = webConferencing.stopCall(callId, true);
        if (call != null) {
          return Response.ok().cacheControl(cacheControl).entity(call).build();
        } else {
          return Response.status(Status.NOT_FOUND)
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.notFoundError("Call not found"))
                         .build();
        }
      } catch (Throwable e) {
        LOG.error("Error removing call info for '" + callId + "' by '" + currentUserName + "'", e);
        return Response.serverError()
                       .cacheControl(cacheControl)
                       .entity(ErrorInfo.serverError("Error removing call information"))
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
   * Put call.
   *
   * @param uriInfo the uri info
   * @param type the type
   * @param id the id
   * @param state the state
   * @param clientId the client id
   * @return the response
   */
  @PUT
  @RolesAllowed("users")
  @Path("/call_/{type}/{id}")
  @Deprecated // CometD used instead
  public Response putCall(@Context UriInfo uriInfo,
                          @PathParam("type") String type,
                          @PathParam("id") String id,
                          @FormParam("state") String state,
                          @FormParam("clientId") String clientId) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String callId = callId(type, id);
      String currentUserName = convo.getIdentity().getUserId();
      try {
        if (CallState.STOPPED.equals(state)) {
          CallInfo call = webConferencing.stopCall(callId, false);
          if (call != null) {
            return Response.ok().cacheControl(cacheControl).entity(call).build();
          } else {
            return Response.status(Status.NOT_FOUND)
                           .cacheControl(cacheControl)
                           .entity(ErrorInfo.notFoundError("Call not found"))
                           .build();
          }
        } else if (CallState.STARTED.equals(state)) {
          CallInfo call = webConferencing.startCall(callId, clientId);
          if (call != null) {
            return Response.ok().cacheControl(cacheControl).entity(call).build();
          } else {
            return Response.status(Status.NOT_FOUND)
                           .cacheControl(cacheControl)
                           .entity(ErrorInfo.notFoundError("Call not found"))
                           .build();
          }
        } else {
          return Response.status(Status.BAD_REQUEST)
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.clientError("Wrong request parameters: state"))
                         .build();
        }
      } catch (Throwable e) {
        LOG.error("Error updating call info for '" + callId + "' by '" + currentUserName + "'", e);
        return Response.serverError().cacheControl(cacheControl).entity(ErrorInfo.serverError("Error updating the call")).build();
      }
    } else {
      return Response.status(Status.UNAUTHORIZED)
                     .cacheControl(cacheControl)
                     .entity(ErrorInfo.accessError("Unauthorized user"))
                     .build();
    }
  }

  /**
   * Post call.
   *
   * @param uriInfo the uri info
   * @param type the provider type
   * @param id the id
   * @param title the title
   * @param providerType the provider type
   * @param ownerId the owner id
   * @param ownerType the owner type
   * @param participants the participants
   * @return the response
   */
  @POST
  @RolesAllowed("users")
  @Path("/call_/{type}/{id}")
  @Deprecated // CometD used instead
  public Response postCall(@Context UriInfo uriInfo,
                           @PathParam("type") String type,
                           @PathParam("id") String id,
                           @FormParam("title") String title,
                           @FormParam("provider") String providerType,
                           @FormParam("owner") String ownerId,
                           @FormParam("ownerType") String ownerType,
                           @FormParam("participants") String participants) {
    ConversationState convo = ConversationState.getCurrent();
    if (convo != null) {
      String callId = callId(type, id);
      if (title != null) {
        if (providerType != null) {
          if (ownerId != null) {
            if (ownerType != null) {
              if (participants != null) {
                String currentUserName = convo.getIdentity().getUserId();
                try {
                  CallInfo call = webConferencing.addCall(callId,
                                                          ownerId,
                                                          ownerType,
                                                          title,
                                                          providerType,
                                                          Arrays.asList(participants.split(";")));
                  return Response.ok().cacheControl(cacheControl).entity(call).build();
                } catch (CallInfoException e) {
                  return Response.status(Status.BAD_REQUEST)
                                 .cacheControl(cacheControl)
                                 .entity(ErrorInfo.clientError(e.getMessage()))
                                 .build();
                } catch (Throwable e) {
                  LOG.error("Error creating call info for '" + callId + "' by '" + currentUserName + "'", e);
                  return Response.serverError()
                                 .cacheControl(cacheControl)
                                 .entity(ErrorInfo.serverError("Error creating call record"))
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
                             .entity(ErrorInfo.clientError("Wrong request parameters: ownerType"))
                             .build();
            }
          } else {
            return Response.status(Status.BAD_REQUEST)
                           .cacheControl(cacheControl)
                           .entity(ErrorInfo.clientError("Wrong request parameters: owner"))
                           .build();
          }
        } else {
          return Response.status(Status.BAD_REQUEST)
                         .cacheControl(cacheControl)
                         .entity(ErrorInfo.clientError("Wrong request parameters: provider"))
                         .build();
        }
      } else {
        return Response.status(Status.BAD_REQUEST)
                       .cacheControl(cacheControl)
                       .entity(ErrorInfo.clientError("Wrong request parameters: title"))
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
   * Call id.
   *
   * @param type the type
   * @param id the id
   * @return the string
   */
  private String callId(String type, String id) {
    return new StringBuffer(type).append('/').append(id).toString();
  }
}
