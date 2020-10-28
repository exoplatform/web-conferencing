
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileUploadException;
import org.json.JSONException;
import org.json.JSONObject;
import org.picocontainer.Startable;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.webconferencing.UserInfo.IMInfo;
import org.exoplatform.webconferencing.dao.CallDAO;
import org.exoplatform.webconferencing.dao.ParticipantDAO;
import org.exoplatform.webconferencing.dao.StorageException;
import org.exoplatform.webconferencing.domain.CallEntity;
import org.exoplatform.webconferencing.domain.ParticipantEntity;
import org.exoplatform.webconferencing.domain.ParticipantId;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: SkypeService.java 00000 Feb 22, 2017 pnedonosko $
 */
public class WebConferencingService implements Startable {

  /** The Constant ID_MAX_LENGTH. */
  public static final int       ID_MAX_LENGTH                = 255;

  /** The Constant TEXT_MAX_LENGTH. */
  public static final int       TEXT_MAX_LENGTH              = 255;

  /** The Constant ARG_MAX_LENGTH. */
  public static final int       ARG_MAX_LENGTH               = 32;

  /** The Constant DATA_MAX_LENGTH. */
  public static final int       DATA_MAX_LENGTH              = 2000;

  /** The Constant SPACE_TYPE_NAME. */
  public static final String    SPACE_TYPE_NAME              = "space".intern();

  /** The Constant CHAT_ROOM_TYPE_NAME. */
  public static final String    CHAT_ROOM_TYPE_NAME          = "chat_room".intern();

  /** The Constant SESSION_TOKEN_COOKIE. */
  public static final String    SESSION_TOKEN_COOKIE         = "webconf_session_token".intern();

  /** The operation call added. */
  public static String          OPERATION_CALL_ADDED         = "call-added";

  /** The operation call started. */
  public static String          OPERATION_CALL_STARTED       = "call-started";

  /** The operation call joined. */
  public static String          OPERATION_CALL_JOINED        = "call-joined";

  /** The operation call leaved. */
  public static String          OPERATION_CALL_LEAVED        = "call-leaved";

  /** The operation call stopped. */
  public static String          OPERATION_CALL_STOPPED       = "call-stopped";

  /** The operation call deleted. */
  public static String          OPERATION_CALL_DELETED       = "call-deleted";

  /** The operation call recorded. */
  public static String          OPERATION_CALL_RECORDED      = "call-recorded";

  public static String          STATUS_OK                    = "ok";

  public static String          STATUS_KO                    = "ko";

  /** The Constant GROUP_CALL_TYPE. */
  protected static final String GROUP_CALL_TYPE              = "group".intern();

  /** The Constant CALL_OWNER_SCOPE_NAME. */
  protected static final String CALL_OWNER_SCOPE_NAME        = "webconferencing.callOwner".intern();

  /** The Constant CALL_ID_SCOPE_NAME. */
  protected static final String CALL_ID_SCOPE_NAME           = "webconferencing.callId".intern();

  /** The Constant USER_CALLS_SCOPE_NAME. */
  protected static final String USER_CALLS_SCOPE_NAME        = "webconferencing.user.calls".intern();

  /** The Constant PROVIDER_SCOPE_NAME. */
  protected static final String PROVIDER_SCOPE_NAME          = "webconferencing.provider".intern();

  /** The Constant JWT_CONFIGURATION_PROPERTIES. */
  protected static final String JWT_CONFIGURATION_PROPERTIES = "jwt-configuration";

  /** The Constant SECRET_KEY. */
  protected static final String SECRET_KEY                   = "secret-key";

  /**
   * Represent Space in calls.
   */
  public class SpaceInfo extends GroupInfo {

    /** The space group id. */
    protected final String groupId;

    /**
     * Instantiates a new space info.
     *
     * @param socialSpace the social space
     */
    public SpaceInfo(Space socialSpace) {
      super(socialSpace.getPrettyName(), socialSpace.getDisplayName());
      this.groupId = socialSpace.getGroupId();
    }

    /**
     * Gets the group id of the space.
     *
     * @return the groupId
     */
    public String getGroupId() {
      return groupId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
      return SPACE_TYPE_NAME;
    }
  }

  /**
   * Represent Chat room in calls.
   */
  public class RoomInfo extends GroupInfo {

    /**
     * Instantiates a new room info.
     *
     * @param id the id
     * @param title the title
     */
    public RoomInfo(String id, String title) {
      super(id, title);
      this.profileLink = IdentityInfo.EMPTY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
      return CHAT_ROOM_TYPE_NAME;
    }
  }

  /** The Constant OWNER_TYPE_SPACE. */
  public static final String                         OWNER_TYPE_SPACE       = "space";

  /** The Constant OWNER_TYPE_CHATROOM. */
  public static final String                         OWNER_TYPE_CHATROOM    = "chat_room";

  /** The Constant CMS_GROUPS_PATH. */
  public static final String                         CMS_GROUPS_PATH        = "groupsPath";

  /** The Constant EXO_TITLE_PROP. */
  public static final String                         EXO_TITLE_PROP         = "exo:title";

  /** The Constant NT_FILE. */
  public static final String                         NT_FILE                = "nt:file";

  /** The Constant JCR_CONTENT. */
  public static final String                         JCR_CONTENT            = "jcr:content";

  /** The Constant JCR_DATA. */
  public static final String                         JCR_DATA               = "jcr:data";

  /** The Constant JCR_MIME_TYPE. */
  public static final String                         JCR_MIME_TYPE          = "jcr:mimeType";

  /** The Constant JCR_LAST_MODIFIED_PROP. */
  public static final String                         JCR_LAST_MODIFIED_PROP = "jcr:lastModified";

  /** The Constant EXO_RSS_ENABLE_PROP. */
  public static final String                         EXO_RSS_ENABLE_PROP    = "exo:rss-enable";

  /** The Constant NT_RESOURCE. */
  public static final String                         NT_RESOURCE            = "nt:resource";

  /** The Constant LOG. */
  protected static final Log                         LOG                    = ExoLogger.getLogger(WebConferencingService.class);

  /** The secret key. */
  protected final String                             secretKey;

  /** The organization. */
  protected final OrganizationService                organization;

  /** The social identity manager. */
  protected final IdentityManager                    socialIdentityManager;

  /** The listener service. */
  protected final ListenerService                    listenerService;

  /** The settings service. */
  protected final SettingService                     settingService;

  /** The calls storage. */
  protected final CallDAO                            callStorage;

  /** The participants storage. */
  protected final ParticipantDAO                     participantsStorage;

  /** The providers. */
  protected final Map<String, CallProvider>          providers              = new ConcurrentHashMap<>();

  /** The space service. */
  protected SpaceService                             spaceService;

  /** The user listeners. */
  protected final Map<String, Set<UserCallListener>> userListeners          = new ConcurrentHashMap<>();

  /** The upload service. */
  protected final UploadService                      uploadService;

  /** The repository service. */
  protected final RepositoryService                  repositoryService;

  /** The session providers. */
  protected final SessionProviderService             sessionProviders;

  /** The node creator. */
  protected final NodeHierarchyCreator               nodeCreator;

  /** The identity registry. */
  protected final IdentityRegistry                   identityRegistry;

  /** The authenticator. */
  protected final Authenticator                      authenticator;

  /**
   * Checks is ID valid (not null, not empty and not longer of {@value #ID_MAX_LENGTH} chars).
   *
   * @param id the id
   * @return true, if is valid id
   */
  public static boolean isValidId(String id) {
    return id != null && id.length() > 0 && id.length() <= ID_MAX_LENGTH;
  }

  /**
   * Checks if is valid text (null, not empty and not longer of {@value #TEXT_MAX_LENGTH} chars).
   *
   * @param text the text
   * @return true, if is valid text
   */
  public static boolean isValidText(String text) {
    return text == null || (text.length() > 0 && text.length() <= TEXT_MAX_LENGTH);
  }

  /**
   * Checks if argument valid (null or not empty and not longer of {@value #ARG_MAX_LENGTH} chars).
   *
   * @param arg the arg
   * @return true, if is valid arg
   */
  public static boolean isValidArg(String arg) {
    return arg == null || (arg.length() > 0 && arg.length() <= ARG_MAX_LENGTH);
  }

  /**
   * Checks if argument not null (also not empty and not longer of {@value #ARG_MAX_LENGTH} chars).
   *
   * @param arg the arg
   * @return true, if is not null arg
   */
  public static boolean isNotNullArg(String arg) {
    return arg != null && arg.length() > 0 && arg.length() <= ARG_MAX_LENGTH;
  }

  /**
   * Checks if data valid (null or not empty and not longer of {@value #DATA_MAX_LENGTH} bytes in UTF8
   * encoding).
   *
   * @param data the data
   * @return true, if is valid data
   * @throws UnsupportedEncodingException if UTF8 encoding not found in runtime
   */
  public static boolean isValidData(String data) throws UnsupportedEncodingException {
    return data == null || (data.length() > 0 && data.getBytes("UTF8").length <= DATA_MAX_LENGTH);
  }

  /**
   * Instantiates a new web conferencing service.
   *
   * @param organization the organization
   * @param socialIdentityManager the social identity manager
   * @param listenerService the listener service
   * @param settingService the setting service
   * @param callStorage the call storage
   * @param participantsStorage the participants storage
   * @param uploadService the upload service
   * @param repositoryService the repository service
   * @param sessionProviders the session providers
   * @param nodeCreator the node creator
   * @param identityRegistry the identity registry
   * @param authenticator the authenticator
   * @param initParams the initParams
   */
  public WebConferencingService(OrganizationService organization,
                                IdentityManager socialIdentityManager,
                                ListenerService listenerService,
                                SettingService settingService,
                                CallDAO callStorage,
                                ParticipantDAO participantsStorage,
                                UploadService uploadService,
                                RepositoryService repositoryService,
                                SessionProviderService sessionProviders,
                                NodeHierarchyCreator nodeCreator,
                                IdentityRegistry identityRegistry,
                                Authenticator authenticator,
                                InitParams initParams) {
    this.organization = organization;
    this.socialIdentityManager = socialIdentityManager;
    this.listenerService = listenerService;
    this.settingService = settingService;
    this.callStorage = callStorage;
    this.participantsStorage = participantsStorage;
    this.uploadService = uploadService;
    this.repositoryService = repositoryService;
    this.sessionProviders = sessionProviders;
    this.nodeCreator = nodeCreator;
    this.identityRegistry = identityRegistry;
    this.authenticator = authenticator;
    PropertiesParam jwtSecretParam = initParams.getPropertiesParam(JWT_CONFIGURATION_PROPERTIES);
    this.secretKey = jwtSecretParam.getProperty(SECRET_KEY);
  }

  /**
   * Gets the user info.
   *
   * @param id the id
   * @return the user info
   * @throws IdentityStateException if error happened during searching the user in Organization Service
   */
  public UserInfo getUserInfo(String id) throws IdentityStateException {
    User user;
    try {
      user = organization.getUserHandler().findUserByName(id, UserStatus.ANY);
    } catch (Exception e) {
      throw new IdentityStateException("Error finding user in organization service", e);
    }
    if (user != null) {
      // Check if user not disabled
      if (user.isEnabled()) {
        org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                         socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                                                                   id,
                                                                                                                   true);
        if (userIdentity != null) {
          Profile socialProfile = userIdentity.getProfile();
          @SuppressWarnings("unchecked")
          List<Map<String, String>> ims = (List<Map<String, String>>) socialProfile.getProperty(Profile.CONTACT_IMS);
          UserInfo info = new UserInfo(user.getUserName(), user.getFirstName(), user.getLastName());
          if (ims != null) {
            for (Map<String, String> m : ims) {
              String imType = m.get("key");
              String imId = m.get("value");
              if (imId != null && imId.length() > 0) {
                CallProvider provider = getProvider(imType);
                // Here we take in account that provider may change its supported types in runtime
                if (provider != null && provider.isActive() && provider.isSupportedType(imType)) {
                  try {
                    IMInfo im = provider.getIMInfo(imId);
                    if (im != null) {
                      info.addImAccount(im);
                    } // otherwise provider doesn't have an IM type at all
                  } catch (CallProviderException e) {
                    LOG.warn(e.getMessage());
                  }
                }
              }
            }
          }
          info.setAvatarLink(socialProfile.getAvatarUrl());
          info.setProfileLink(LinkProvider.getUserProfileUri(id));
          return info;
        } else {
          LOG.warn("Social identity not found for " + user.getUserName() + " (" + user.getFirstName() + " " + user.getLastName()
              + ")");
        }
      } else if (LOG.isDebugEnabled()) {
        LOG.debug("Ignore disabled user (treat as not found): " + id);
      }
    } else {
      LOG.warn("User not found: " + id);
    }
    return null;
  }

  /**
   * Gets the space info.
   *
   * @param spacePrettyName the space pretty name
   * @return the space info
   * @throws IdentityStateException if error reading space member in Organization Service
   * @throws StorageException if error reading saved group call ID associated with a space
   */
  public SpaceInfo getSpaceInfo(String spacePrettyName) throws IdentityStateException, StorageException {
    return spaceInfo(spacePrettyName, findGroupCallId(spacePrettyName));
  }

  /**
   * Gets the JWT secret key.
   *
   * @return the secret key
   */
  public String getSecretKey() {
    return this.secretKey;
  }

  /**
   * Space info.
   *
   * @param spacePrettyName the space pretty name
   * @param callId the call id
   * @return the space info
   * @throws IdentityStateException if error reading space member in Organization Service
   */
  protected SpaceInfo spaceInfo(String spacePrettyName, String callId) throws IdentityStateException {
    Space socialSpace = spaceService.getSpaceByPrettyName(spacePrettyName);
    SpaceInfo space = new SpaceInfo(socialSpace);
    for (String sm : socialSpace.getMembers()) {
      UserInfo user = getUserInfo(sm);
      if (user != null) {
        space.addMember(user);
      } else {
        LOG.warn("Skipped not found space member " + sm + " of " + spacePrettyName);
        // for space we have members from inside, thus if it is not found, we ignore him assuming space
        // should be consistent
      }
    }
    space.setProfileLink(socialSpace.getUrl());
    space.setAvatarLink(socialSpace.getAvatarUrl());
    space.setCallId(callId);
    return space;
  }

  /**
   * Gets the room info.
   *
   * @param id the id
   * @param title the title
   * @param members the room members
   * @return the room info
   * @throws IdentityStateException if error reading room member in Organization Service
   * @throws StorageException if error reading saved group call ID associated with a room
   */
  public RoomInfo getRoomInfo(String id, String title, String[] members) throws IdentityStateException, StorageException {
    return roomInfo(id, title, members, findGroupCallId(id));
  };

  /**
   * Room info.
   *
   * @param id the id
   * @param title the title
   * @param members the members
   * @param callId the call id
   * @return the room info
   * @throws IdentityStateException if user cannot be read
   */
  protected RoomInfo roomInfo(String id, String title, String[] members, String callId) throws IdentityStateException {
    RoomInfo room = new RoomInfo(id, title);
    for (String userName : members) {
      UserInfo user = getUserInfo(userName);
      if (user != null) {
        room.addMember(user);
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("External room member " + userName + " for '" + title + "'");
        }
        // for chat room we have members from outside, if not eXo user - add it as external participant
        room.addMember(new ParticipantInfo(userName));
      }
    }
    room.setProfileLink(IdentityInfo.EMPTY);
    room.setAvatarLink(LinkProvider.SPACE_DEFAULT_AVATAR_URL);
    room.setCallId(callId);
    return room;
  };

  /**
   * Adds the call to list of active and fires STARTED event.
   *
   * @param id the id of the call
   * @param ownerId the owner id of the call
   * @param ownerType the owner type
   * @param title the title of the call
   * @param providerType the provider type
   * @param parts the participants
   * @return the call info object of type {@link CallInfo}
   * @throws CallArgumentException if call argument has wrong value (failed validation)
   * @throws StorageException if error reading, adding or updating call information in persistent storage
   * @throws IdentityStateException if error reading room member in Organization Service
   * @throws CallConflictException if such call already exists and has active state (started and/or running)
   * @throws CallSettingsException if call entry has wrong settings (it's for Chat room call, the title -
   *           too long or has bad value)
   */
  public CallInfo addCall(String id,
                          String ownerId,
                          String ownerType,
                          String title,
                          String providerType,
                          Collection<String> parts) throws CallArgumentException,
                                                    StorageException,
                                                    IdentityStateException,
                                                    CallConflictException,
                                                    CallSettingsException {
    final long opStart = System.currentTimeMillis();
    final String currentUserId = currentUserId();

    if (isValidId(id)) {
      if (isValidId(ownerId)) {
        if (isNotNullArg(ownerType)) {
          if (isNotNullArg(providerType)) {
            if (isValidText(title)) {

              final boolean isUser = UserInfo.TYPE_NAME.equals(ownerType);
              final boolean isSpace = OWNER_TYPE_SPACE.equals(ownerType);
              final boolean isRoom = OWNER_TYPE_CHATROOM.equals(ownerType);
              final boolean isGroup = isSpace || isRoom;

              // TODO find a way to create a call in a single transaction (everything including
              // cleanup/deletion of an outdated). As for this moment, it fails to create a new call if some
              // call ID was deleted prior. See comment for clearing storage session below in this method.

              // Check if group doesn't have a call with another ID assigned
              if (isGroup) {
                // it's group call
                String prevId = findGroupCallId(ownerId);
                if (prevId != null && !prevId.equals(id)) {
                  // XXX For a case when some client failed to delete an existing (but outdated etc.) call but
                  // already starting a new one.
                  // It's SfB usecase when browser client failed to delete outdated call (browser/plugin
                  // crashed in IE11) and then starts a new one.
                  deleteCall(prevId);
                  LOG.warn("Deleted outdated group call: " + prevId);
                }
              }

              // Ensure we can create this call
              invalidateCall(id, isGroup);

              // Collecting the call data
              final IdentityInfo owner;
              if (isUser) {
                UserInfo userInfo = getUserInfo(ownerId);
                if (userInfo == null) {
                  // if owner user not found, it's possibly an external user, thus treat it as a chat room
                  owner = new RoomInfo(ownerId, title);
                  owner.setAvatarLink(LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
                } else {
                  owner = userInfo;
                  owner.setProfileLink(userInfo.getProfileLink());
                  String avatar = userInfo.getAvatarLink();
                  avatar = avatar != null ? avatar : LinkProvider.PROFILE_DEFAULT_AVATAR_URL;
                  owner.setAvatarLink(avatar);
                }
              } else if (isSpace) {
                Space space = spaceService.getSpaceByPrettyName(ownerId);
                if (space != null) {
                  owner = new SpaceInfo(space);
                  owner.setProfileLink(space.getUrl());
                  String avatar = space.getAvatarUrl();
                  avatar = avatar != null ? avatar : LinkProvider.SPACE_DEFAULT_AVATAR_URL;
                  owner.setAvatarLink(avatar);
                } else {
                  LOG.warn("Cannot find call's owner space: " + ownerId);
                  owner = new RoomInfo(ownerId, title);
                  owner.setAvatarLink(LinkProvider.SPACE_DEFAULT_AVATAR_URL);
                }
              } else if (isRoom) {
                owner = new RoomInfo(ownerId, title);
                owner.setAvatarLink(LinkProvider.SPACE_DEFAULT_AVATAR_URL);
              } else {
                throw new CallArgumentException("Wrong call owner type: " + ownerType);
              }

              // Call participants
              Set<UserInfo> participants = new LinkedHashSet<>();
              for (String pid : parts) {
                if (isValidId(pid)) {
                  UserInfo part = getUserInfo(pid);
                  if (part != null) {
                    // it's eXo user
                    participants.add(part);
                  } else {
                    // external participant
                    participants.add(part = new ParticipantInfo(providerType, pid));
                  }
                  // We start the call with all parts leaved, later call pages will update it to JOINED
                  part.setState(UserState.LEAVED);
                } else {
                  LOG.error("Cannot add call participant with too long ID: " + pid);
                  throw new CallArgumentException("Wrong participant ID (" + pid + ")");
                }
              }

              // Save the call
              CallInfo call = new CallInfo(id, title, owner, providerType);
              call.addParticipants(participants);
              call.setState(CallState.STARTED);
              call.setLastDate(Calendar.getInstance().getTime());

              // Clear storage session:
              // XXX It is REQUIRED stuff (actual when something were deleted above), otherwise
              // ExoTx/Hibernate will fail to enter into the transaction in createCall() with an exception:
              // javax.persistence.EntityExistsException: a different object with the same identifier value
              // was already associated with the session, e.g.:
              // [org.exoplatform.webconferencing.domain.ParticipantEntity#org.exoplatform.webconferencing.domain.ParticipantId@4aa63de3]
              try {
                participantsStorage.clear();
                callStorage.clear();
              } catch (IllegalArgumentException | IllegalStateException | PersistenceException e) {
                LOG.warn("Call storage cleanup failed before creating call: " + call.getId(), e);
              }

              // Create the call in storage, handle conflicts if required
              createCall(call);

              // Notify participants (about started call)
              if (isGroup) {
                // it's group call: fire group's user listener for incoming, except of the caller
                for (UserInfo part : call.getParticipants()) {
                  if (UserInfo.TYPE_NAME.equals(part.getType())) {
                    if (!currentUserId.equals(part.getId())) {
                      fireUserCallStateChanged(part.getId(), id, providerType, CallState.STARTED, ownerId, ownerType);
                    }
                  }
                }
              } else if (isUser) {
                // It's P2P call
                notifyUserCallStateChanged(call, currentUserId, CallState.STARTED);
              }

              // Log metrics - call created
              // service=notifications operation=send-push-notification
              // parameters="user:thomas,token:xxxxxxxxxxxDLu-,type:android,pluginId:RelationshipReceivedRequestPlugin" status=ok
              // duration_ms=298
              LOG.info(metricMessage(currentUserId,
                                     call,
                                     OPERATION_CALL_ADDED,
                                     STATUS_OK,
                                     System.currentTimeMillis() - opStart,
                                     null));

              return call;
            } else {
              throw new CallArgumentException("Wrong call title");
            }
          } else {
            throw new CallArgumentException("Wrong provider");
          }
        } else {
          throw new CallArgumentException("Wrong owner type");
        }
      } else {
        throw new CallArgumentException("Wrong owner ID value");
      }
    } else {
      throw new CallArgumentException("Wrong call ID value");
    }
  }

  /**
   * Gets an active call info.
   *
   * @param id the id
   * @return the call info or <code>null</code> if call not found
   * @throws InvalidCallException if call in erroneous state, see cause exception for details
   */
  public CallInfo getCall(String id) throws InvalidCallException {
    try {
      return findCallById(id);
    } catch (CallSettingsException | CallOwnerException | StorageException | IdentityStateException e) {
      throw new InvalidCallException("Error getting call: " + id, e);
    }
  }

  /**
   * Removes the call info from active and fires STOPPED event.
   *
   * @param id the id
   * @param remove the remove
   * @return the call info object of type {@link CallInfo}
   * @throws CallNotFoundException if call not found
   * @throws InvalidCallException if call in erroneous state and cannot be used, details are in caused
   *           exception
   */
  public CallInfo stopCall(String id, boolean remove) throws CallNotFoundException, InvalidCallException {
    final long opStart = System.currentTimeMillis();
    CallInfo call = getCall(id);
    if (call != null) {
      String userId = currentUserId();
      try {
        stopCall(call, userId, remove);
        if (remove) {
          // Log metrics - call deleted
          LOG.info(metricMessage(userId, call, OPERATION_CALL_DELETED, STATUS_OK, System.currentTimeMillis() - opStart, null));
        } else {
          // Log metrics - call stopped
          LOG.info(metricMessage(userId, call, OPERATION_CALL_JOINED, STATUS_OK, System.currentTimeMillis() - opStart, null));
        }
        return call;
      } catch (StorageException e) {
        throw new InvalidCallException("Error stopping call: " + id, e);
      }
    } else {
      throw new CallNotFoundException("Call not found: " + id);
    }
  }

  /**
   * Stop call.
   *
   * @param call the call
   * @param userId the user id
   * @param remove the remove
   * @return the call info
   * @throws StorageException if persistence exception happen
   */
  protected CallInfo stopCall(CallInfo call, String userId, boolean remove) throws StorageException {
    // Delete or update in single tx
    if (remove) {
      deleteCall(call.getId());
    } else {
      call.setState(CallState.STOPPED);
      // we don't update each participant with LEAVED state here as the call state already shows this,
      // see also startCall()
      try {
        updateCall(call);
      } catch (CallNotFoundException | CallSettingsException e) {
        LOG.warn("Failed to save stopped call: " + call.getId(), e);
      }
    }
    // Then notify users
    if (call.getOwner().isGroup()) {
      String callId = call.getId();
      for (UserInfo part : call.getParticipants()) {
        if (UserInfo.TYPE_NAME.equals(part.getType())) {
          // It's eXo user: fire user listener for stopped call,
          // but, in case if stopped with removal (deleted call), don't fire to the initiator (of deletion) -
          // a given user.
          // A given user also can be null when not possible to define it (e.g. on CometD channel removal, or
          // other server side action) - then we notify to all participants.
          if (userId == null || !(remove && userId.equals(part.getId()))) {
            fireUserCallStateChanged(part.getId(),
                                     callId,
                                     call.getProviderType(),
                                     CallState.STOPPED,
                                     call.getOwner().getId(),
                                     call.getOwner().getType());
          }
        }
      }
    } else {
      notifyUserCallStateChanged(call, userId, CallState.STOPPED);
    }
    return call;
  }

  /**
   * Starts existing call and fires STARTED event. It's actual for group calls.
   *
   * @param id the id
   * @param clientId the client id
   * @return the call info object of type {@link CallInfo}
   * @throws CallNotFoundException if call not found
   * @throws InvalidCallException if call in erroneous state and cannot be used, details are in caused
   *           exception
   */
  public CallInfo startCall(String id, String clientId) throws CallNotFoundException, InvalidCallException {
    final long opStart = System.currentTimeMillis();
    CallInfo call = getCall(id);
    if (call != null) {
      try {
        // TODO use current user from the request (Comet) not an one system
        String userId = currentUserId();
        startCall(call, userId, clientId);

        // Log metrics - call started
        LOG.info(metricMessage(userId, call, OPERATION_CALL_STARTED, STATUS_OK, System.currentTimeMillis() - opStart, null));

        return call;
      } catch (StorageException | ParticipantNotFoundException | CallSettingsException e) {
        throw new InvalidCallException("Error starting call: " + id, e);
      }
    } else {
      throw new CallNotFoundException("Call not found: " + id);
    }
  }

  /**
   * Start existing call.
   *
   * @param call the call
   * @param partId the participant id who started the call
   * @param clientId the client id
   * @throws ParticipantNotFoundException if call or its participants not found in storage
   * @throws CallSettingsException if call entry has wrong settings (room title, owner type etc)
   * @throws StorageException if storage exception happen
   * @throws CallNotFoundException if call not found in storage
   */
  protected void startCall(CallInfo call, String partId, String clientId) throws ParticipantNotFoundException,
                                                                          CallSettingsException,
                                                                          StorageException,
                                                                          CallNotFoundException {
    // TODO exception if user not a participant?
    String callId = call.getId();

    // We save call in a single tx, thus logic split on gathering the changes and saving them at the end
    call.setState(CallState.STARTED);

    // On call start we mark all parts LEAVED and then each of them will join and be marked as JOINED in
    // joinCall()

    // update participants
    if (call.getOwner().isGroup()) {
      Set<UserInfo> members = new HashSet<>(GroupInfo.class.cast(call.getOwner()).getMembers().values());
      if (members.size() >= call.getParticipants().size()) {
        members.removeAll(call.getParticipants());
        for (UserInfo p : members) {
          participantsStorage.create(createParticipantEntity(callId, p));
        }
        call.addParticipants(members);
      } else {
        Set<UserInfo> participants = new HashSet<>(call.getParticipants());
        participants.removeAll(members);
        call.removeParticipants(participants);
        for (UserInfo part : participants) {
          ParticipantEntity entity = participantsStorage.find(new ParticipantId(part.getId(), callId));
          if (entity != null) {
            participantsStorage.delete(entity);
          }
        }
      }
    }

    for (UserInfo part : call.getParticipants()) {
      if (UserInfo.TYPE_NAME.equals(part.getType()) && partId.equals(part.getId())) {
        part.setState(UserState.JOINED);
        part.setClientId(clientId);
      } else {
        part.setState(UserState.LEAVED);
        part.setClientId(null);
      }
    }

    updateCallAndParticipants(call);

    // Jul 26, 2020: Inform all group members about the call
    // For P2P call we need only inform another peer and this loop does the work perfectly
    Collection<UserInfo> parts = call.getOwner().isGroup() ? GroupInfo.class.cast(call.getOwner()).getMembers().values()
                                                           : call.getParticipants();
    for (UserInfo part : parts) {
      if (!partId.equals(part.getId())) {
        // Inform all except of the user who started the call
        fireUserCallStateChanged(part.getId(),
                                 callId,
                                 call.getProviderType(),
                                 CallState.STARTED,
                                 call.getOwner().getId(),
                                 call.getOwner().getType());
      }
    }
  }

  /**
   * Join a call if it is started or start already stopped one.
   *
   * @param id the id
   * @param partId the participant id
   * @param clientId the client id
   * @return the call info object of type {@link CallInfo}
   * @throws InvalidCallException if call in erroneous state and cannot be used, details are in caused
   *           exception
   * @throws CallNotFoundException if call not found
   */
  public CallInfo joinCall(String id, String partId, String clientId) throws InvalidCallException, CallNotFoundException {
    final long opStart = System.currentTimeMillis();
    // TODO exception if user not a participant?
    CallInfo call = getCall(id);
    if (call != null) {
      try {
        if (CallState.STARTED.equals(call.getState())) {
          UserInfo joined = null;
          // save Joined first
          for (UserInfo part : call.getParticipants()) {
            if (UserInfo.TYPE_NAME.equals(part.getType()) && partId.equals(part.getId())) {
              part.setState(UserState.JOINED);
              part.setClientId(clientId);
              joined = part;
              break;
            }
          }
          // then save if someone joined (it should but we preserve the logic)
          if (joined != null) {
            // First save the call with joined participant (in single tx)
            updateParticipant(id, joined);
            // Then fire this user joined to all parts, including the user itself
            for (UserInfo part : call.getParticipants()) {
              fireUserCallJoined(id,
                                 call.getProviderType(),
                                 call.getOwner().getId(),
                                 call.getOwner().getType(),
                                 partId,
                                 part.getId());
            }
          }
          // Log metrics - call joined
          LOG.info(metricMessage(partId, call, OPERATION_CALL_JOINED, STATUS_OK, System.currentTimeMillis() - opStart, null));
        } else {
          // TODO check should we use partId instead of the current user for the start
          // the partId it's current exo user in the request (Comet)
          String userId = currentUserId();
          startCall(call, userId, clientId);
          // Log metrics - call started
          LOG.info(metricMessage(userId, call, OPERATION_CALL_STARTED, STATUS_OK, System.currentTimeMillis() - opStart, null));
        }
      } catch (CallSettingsException | ParticipantNotFoundException | StorageException e) {
        throw new InvalidCallException("Error joining call: " + id, e);
      }
      return call;
    } else {
      throw new CallNotFoundException("Call not found: " + id);
    }
  }

  /**
   * If call started, then notify all its parties that given participant leaved. If call not found then
   * <code>null</code> will be returned.
   *
   * @param id the id
   * @param partId the participant id
   * @param clientId the client id
   * @return the call info or <code>null</code> if call not found
   * @throws InvalidCallException if call in erroneous state and cannot be used, details are in caused
   *           exception
   */
  public CallInfo leaveCall(String id, String partId, String clientId) throws InvalidCallException {
    final long opStart = System.currentTimeMillis();
    // TODO exception if user not a participant?
    CallInfo call = getCall(id);
    if (call != null) {
      try {
        if (CallState.STARTED.equals(call.getState()) || CallState.PAUSED.equals(call.getState())) {
          UserInfo leaved = null;
          int leavedNum = 0;
          // save Joined first
          for (UserInfo part : call.getParticipants()) {
            if (UserInfo.TYPE_NAME.equals(part.getType())) {
              if (partId.equals(part.getId())) {
                // Leave should not be called on a call session started after stopping an one previous of this
                // call.
                // if (part.hasSameClientId(clientId)) {
                part.setState(UserState.LEAVED);
                part.setClientId(null);
                leaved = part;
                leavedNum++;
                // } // otherwise we may meet this user running a new same call too quickly (before CometD will
                // unsubscribe this call previous channel), we ignore this leave so
              } else {
                // if null - user hasn't joined
                if (part.getState() == null || part.getState().equals(UserState.LEAVED)) {
                  leavedNum++;
                }
              }
            }
          }
          // then save if someone leaved
          if (leaved != null) {
            // First save the call with the participant (in single tx)
            updateParticipant(id, leaved);
            // Fire user leaved to all parts, including the user itself
            for (UserInfo part : call.getParticipants()) {
              // Fire user leaved to all parts, including the user itself
              fireUserCallLeaved(id,
                                 call.getProviderType(),
                                 call.getOwner().getId(),
                                 call.getOwner().getType(),
                                 partId,
                                 part.getId());
            }
            // Log metrics - call leaved
            LOG.info(metricMessage(partId, call, OPERATION_CALL_LEAVED, STATUS_OK, System.currentTimeMillis() - opStart, null));
            // Check if don't need stop the call if all parts leaved already
            if (call.getOwner().isGroup()) {
              if (leavedNum == call.getParticipants().size() || call.getParticipants().size() == 0
                  || call.getParticipants().stream().allMatch(p -> p.getState() == UserState.LEAVED)) {
                // Stop when all group members leave the call
                stopCall(call, partId, false);
                // Log metrics - call stopped
                LOG.info(metricMessage(partId,
                                       call,
                                       OPERATION_CALL_STOPPED,
                                       STATUS_OK,
                                       System.currentTimeMillis() - opStart,
                                       null));
              }
            } else if (call.getParticipants().size() - leavedNum <= 1) {
              // For P2P we remove the call when one of parts stand alone
              stopCall(call, partId, true);
              // Log metrics - call deleted
              LOG.info(metricMessage(partId,
                                     call,
                                     OPERATION_CALL_DELETED,
                                     STATUS_OK,
                                     System.currentTimeMillis() - opStart,
                                     null));
            }
          } // else, if no one leaved, we don't need any action (it may be leaved an user of already stopped
            // call, see comments above)
        } // It seems has no big sense to return error for already stopped call
      } catch (StorageException | ParticipantNotFoundException e) {
        throw new InvalidCallException("Error leaving call: " + id, e);
      }
    } else {
      LOG.warn("Call " + id + " not found to leave it " + partId);
    }
    return call;
  }

  /**
   * Gets the user calls.
   *
   * @param userId the user id
   * @return the user call states
   * @throws StorageException if persistence error happen
   */
  public CallState[] getUserCalls(String userId) throws StorageException {
    // TODO it's not efficient to read whole Call info when we need only its ID and state
    CallState[] states =
                       findUserGroupCalls(userId).stream()
                                                 .map(c -> new CallState(c.getId(),
                                                                         c.getState() != null ? c.getState() : CallState.STOPPED))
                                                 .toArray(size -> new CallState[size]);
    return states;
  }

  /**
   * Adds the user listener.
   *
   * @param listener the listener
   */
  public void addUserCallListener(UserCallListener listener) {
    final String userId = listener.getUserId();
    userListeners.computeIfAbsent(userId, k -> new LinkedHashSet<>()).add(listener);
  }

  /**
   * Removes the user listener.
   *
   * @param listener the listener
   */
  public void removeUserCallListener(UserCallListener listener) {
    final String userId = listener.getUserId();
    Set<UserCallListener> listeners = userListeners.get(userId);
    if (listeners != null) {
      listeners.remove(listener);
    }
  }

  /**
   * Fire user call state.
   *
   * @param userId the user id
   * @param callId the call id
   * @param providerType the provider type
   * @param callState the call state
   * @param ownerId the caller id
   * @param ownerType the caller type
   */
  protected void fireUserCallStateChanged(String userId,
                                          String callId,
                                          String providerType,
                                          String callState,
                                          String ownerId,
                                          String ownerType) {
    // Synchronize on userListeners to have a consistent list of listeners to fire
    Set<UserCallListener> listeners = userListeners.get(userId);
    if (listeners != null) {
      for (UserCallListener listener : listeners) {
        listener.onCallStateChanged(callId, providerType, callState, ownerId, ownerType);
      }
    }
  }

  /**
   * Fire user call joined a new part.
   *
   * @param callId the call id
   * @param providerType the provider type
   * @param ownerId the owner id
   * @param ownerType the owner type
   * @param partId the part id
   * @param userId the user id
   */
  protected void fireUserCallJoined(String callId,
                                    String providerType,
                                    String ownerId,
                                    String ownerType,
                                    String partId,
                                    String userId) {
    Set<UserCallListener> listeners = userListeners.get(userId);
    if (listeners != null) {
      for (UserCallListener listener : listeners) {
        listener.onPartJoined(callId, providerType, ownerId, ownerType, partId);
      }
    }
  }

  /**
   * Fire user call part leaved.
   *
   * @param callId the call id
   * @param providerType the provider type
   * @param ownerId the owner id
   * @param ownerType the owner type
   * @param partId the part id
   * @param userId the user id
   */
  protected void fireUserCallLeaved(String callId,
                                    String providerType,
                                    String ownerId,
                                    String ownerType,
                                    String partId,
                                    String userId) {
    Set<UserCallListener> listeners = userListeners.get(userId);
    if (listeners != null) {
      for (UserCallListener listener : listeners) {
        listener.onPartLeaved(callId, providerType, ownerId, ownerType, partId);
      }
    }
  }

  /**
   * Adds a provider plugin. This method is safe in runtime: if configured provider is not an instance of
   * {@link CallProvider} then it will log a warning and let server continue the start.
   *
   * @param plugin the plugin
   */
  public void addPlugin(ComponentPlugin plugin) {
    Class<CallProvider> pclass = CallProvider.class;
    if (pclass.isAssignableFrom(plugin.getClass())) {
      addProvider(pclass.cast(plugin));
    } else {
      LOG.warn("Web Conferencing provider plugin is not an instance of " + pclass.getName() + ". Skipped plugin: " + plugin);
    }
  }

  /**
   * Adds the provider.
   *
   * @param provider the provider
   */
  public void addProvider(CallProvider provider) {
    for (String type : provider.getSupportedTypes()) {
      CallProvider existing = providers.putIfAbsent(type, provider);
      if (existing != null) {
        LOG.warn("Web Conferencing provider type '" + existing.getType() + "' already registered. Skipped plugin: " + provider);
      }
    }
  }

  /**
   * Gets the provider.
   *
   * @param type the type
   * @return the provider
   */
  public CallProvider getProvider(String type) {
    CallProvider p = providers.get(type);
    if (p != null) {
      try {
        // Apply saved configurations (via Admin UI etc)
        CallProviderConfiguration conf = readProviderConfig(p.getType());
        if (conf != null) {
          p.setActive(conf.isActive());
        } else {
          p.setActive(true);
        }
      } catch (Exception e) {
        LOG.warn("Error reading provider configuration " + p.getType(), e);
        p.setActive(true);
      }
    }
    return p;
  }

  /**
   * Gets the provider configurations.
   *
   * @param locale the locale to apply to provider description, if <code>null</code> a default one will be
   *          used
   * @return the provider configurations
   */
  public Set<CallProviderConfiguration> getProviderConfigurations(Locale locale) {
    Set<CallProvider> allProviders = new LinkedHashSet<>();
    // Collect all registered providers via configuration
    for (CallProvider registeredProvider : providers.values()) {
      allProviders.add(registeredProvider);
    }
    // Read configurations saved in storage for each of them
    Set<CallProviderConfiguration> allConfs = new LinkedHashSet<>();
    for (Iterator<CallProvider> piter = allProviders.iterator(); piter.hasNext();) {
      CallProvider p = piter.next();
      boolean addDefault = false;
      try {
        CallProviderConfiguration conf = readProviderConfig(p.getType());
        if (conf != null) {
          conf.setTitle(p.getTitle());
          conf.setDescription(p.getDescription(locale));
          conf.setLogEnabled(p.isLogEnabled());
          allConfs.add(conf);
        } else {
          addDefault = true;
        }
      } catch (Exception e) {
        LOG.warn("Error reading providers configuration " + p.getType(), e);
        addDefault = true; // this way we let read and re-save the erroneous config
      }
      if (addDefault) {
        CallProviderConfiguration defaultConf = CallProviderConfiguration.fromProvider(p, locale);
        allConfs.add(defaultConf);
      }
    }
    return allConfs;
  }

  /**
   * Gets the provider configurations.
   *
   * @return the provider configurations
   */
  public Set<CallProviderConfiguration> getProviderConfigurations() {
    return this.getProviderConfigurations(Locale.getDefault());
  }

  /**
   * Gets the provider configuration.
   *
   * @param providerType the provider type
   * @param locale the locale to apply to provider description, if <code>null</code> a default one will be
   *          used
   * @return the provider configuration or <code>null</code> if provider not found
   */
  public CallProviderConfiguration getProviderConfiguration(String providerType, Locale locale) {
    CallProvider p = getProvider(providerType);
    if (p != null) {
      CallProviderConfiguration conf = readProviderConfig(p.getType());
      if (conf == null) {
        conf = CallProviderConfiguration.fromProvider(p, locale);
      } else {
        conf.setTitle(p.getTitle());
        conf.setDescription(p.getDescription(locale));
        conf.setLogEnabled(p.isLogEnabled());
      }
      return conf;
    }
    return null; // not found
  }

  /**
   * Save provider configuration.
   *
   * @param conf the configuration to save
   * @throws UnsupportedEncodingException if UTF8 not supported
   * @throws JSONException if cannot serialize to JSON
   */
  public void saveProviderConfiguration(CallProviderConfiguration conf) throws UnsupportedEncodingException, JSONException {
    final String initialGlobalId = Scope.GLOBAL.getId();
    try {
      JSONObject json = providerConfigToJson(conf);
      String safeType = URLEncoder.encode(conf.getType(), "UTF-8");
      settingService.set(Context.GLOBAL, Scope.GLOBAL.id(PROVIDER_SCOPE_NAME), safeType, SettingValue.create(json.toString()));
    } finally {
      Scope.GLOBAL.id(initialGlobalId);
    }
  }

  /**
   * Upload recording of the call.
   *
   * @param uploadInfo the upload info
   * @param request the request
   * @throws UploadFileException the upload recording exception
   * @throws RepositoryException the repository exception
   */
  public void uploadFile(UploadFileInfo uploadInfo, HttpServletRequest request) throws UploadFileException, RepositoryException {

    // final long opStart = System.currentTimeMillis();
    String uploadId = String.valueOf((long) (Math.random() * 100000L));
    try {
      uploadService.createUploadResource(uploadId, request);
    } catch (FileUploadException e) {
      LOG.error("Cannot create upload resource: " + e.getMessage());
      throw new UploadFileException("Cannot create upload resource", e);
    }
    UploadResource resource = uploadService.getUploadResource(uploadId);
    if (resource.getStatus() == UploadResource.UPLOADED_STATUS) {
      String owner = null;
      if (!uploadInfo.isSpace() && !uploadInfo.getIdentity().equals(uploadInfo.getUser())) {
        owner = uploadInfo.getUser();
      } else {
        owner = uploadInfo.getIdentity();
      }
      Node rootNode = getRootFolderNode(owner, uploadInfo.isSpace());
      saveFile(rootNode, resource, uploadInfo.getUser());
      uploadService.removeUploadResource(uploadId); // TODO should this be in try-finally for a cleanup in case of saving failure?
      // TODO Log metrics - call recording uploaded
      // LOG.info(metricMessage(userId, call, OPERATION_CALL_RECORDED, STATUS_OK, System.currentTimeMillis() - opStart, null));
    } else {
      uploadService.removeUploadResource(uploadId);
      throw new UploadFileException("The file " + resource.getFileName() + " cannot be uploaded. Status: "
          + resource.getStatus());
    }
  }

  /**
   * Save recording to JCR.
   *
   * @param parentNode the parent node
   * @param resource the recording
   * @throws RepositoryException the repository exception
   */
  private void saveFile(Node parent, UploadResource resource, String user) throws RepositoryException, UploadFileException {
    ConversationState state = null;
    try {
      state = createState(user);
      ConversationState.setCurrent(state);
    } catch (Exception e) {
      LOG.error("Cannot set conversation state for user: " + user, e);
      throw new UploadFileException("Cannot set conversation state for user: " + user);
    }

    ManageableRepository repository = repositoryService.getCurrentRepository();
    SessionProvider userProvider = sessionProviders.getSessionProvider(state);
    sessionProviders.setSessionProvider(null, userProvider);
    Session session = userProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
    // Get node under user session
    Node folder = (Node) session.getItem(parent.getPath());
    Node fileNode = folder.addNode(resource.getFileName(), "nt:file");
    if (!fileNode.hasProperty(EXO_TITLE_PROP)) {
      fileNode.addMixin(EXO_RSS_ENABLE_PROP);
    }
    fileNode.setProperty(EXO_TITLE_PROP, resource.getFileName());
    Node content = fileNode.addNode(JCR_CONTENT, NT_RESOURCE);
    try (FileInputStream fis = new FileInputStream(resource.getStoreLocation())) {
      content.setProperty(JCR_DATA, fis);
    } catch (IOException e) {
      LOG.error("Cannot set JCR_DATA to created node.", e.getMessage());
      throw new UploadFileException("Cannot set JCR_DATA for file node " + resource.getFileName());
    }
    content.setProperty(JCR_MIME_TYPE, resource.getMimeType());
    content.setProperty(JCR_LAST_MODIFIED_PROP, new GregorianCalendar());
    folder.save();
    try {
      ConversationState.setCurrent(null);
    } catch (Exception e) {
      LOG.warn("An error occured while cleaning the ConversationState", e);
    }
  }

  /**
   * Gets the root folder node.
   *
   * @param identity the identity
   * @param isSpace the is space
   * @param user the user
   * @return the root folder node
   * @throws RepositoryException the repository exception
   */
  private Node getRootFolderNode(String identity, boolean isSpace) throws RepositoryException {
    Node folderNode = null;
    ManageableRepository repository = repositoryService.getCurrentRepository();
    SessionProvider sessionProvider = sessionProviders.getSystemSessionProvider(null);
    Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
    if (isSpace) {
      Node rootSpace = null;
      rootSpace = (Node) session.getItem(nodeCreator.getJcrPath(CMS_GROUPS_PATH) + "/spaces/" + identity);
      folderNode = rootSpace.getNode("Documents");
    } else {
      String privateRelativePath = nodeCreator.getJcrPath("userPrivate");
      Node userNode = null;
      try {
        userNode = nodeCreator.getUserNode(sessionProvider, identity);
      } catch (Exception e) {
        LOG.error("Cannot find user node by id: " + identity, e.getMessage());
        throw new RepositoryException("Cannot find user node by id: " + identity, e);
      }
      folderNode = userNode.getNode(privateRelativePath);
    }
    return folderNode;
  }

  /**
   * Creates the state.
   *
   * @param userId the user id
   * @return the conversation state
   */
  private ConversationState createState(String userId) {
    Identity userIdentity = userIdentity(userId);

    if (userIdentity != null) {
      ConversationState state = new ConversationState(userIdentity);
      // Keep subject as attribute in ConversationState.
      state.setAttribute(ConversationState.SUBJECT, userIdentity.getSubject());
      return state;
    }
    LOG.warn("User identity not found " + userId + " for setting conversation state");
    return null;
  }

  /**
   * Find or create user identity.
   *
   * @param userId the user id
   * @return the identity can be null if not found and cannot be created via
   *         current authenticator
   */
  protected Identity userIdentity(String userId) {
    Identity userIdentity = identityRegistry.getIdentity(userId);
    if (userIdentity == null) {
      // We create user identity by authenticator, but not register it in the
      // registry
      try {
        if (LOG.isDebugEnabled()) {
          LOG.debug("User identity not registered, trying to create it for: " + userId);
        }
        userIdentity = authenticator.createIdentity(userId);
      } catch (Exception e) {
        LOG.warn("Failed to create user identity: " + userId, e);
      }
    }
    return userIdentity;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    // XXX we need reference SpaceService after the container start only, otherwise the servr startup fails
    this.spaceService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);

    // For a case when calls was active and server stopped, then calls wasn't marked as Stopped and need
    // remove them.
    LOG.info("Web Conferencing service started.");
    try {
      int cleaned = deleteAllUserCalls();
      if (cleaned > 0) {
        LOG.info("Cleaned " + cleaned + " expired user calls.");
      }
    } catch (Throwable e) {
      LOG.warn("Error cleaning calls from previous server execution", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    // nothing
  }

  /**
   * Checks if is space member.
   *
   * @param userName the user name
   * @param spacePrettyName the space pretty name
   * @return true, if is space member
   */
  protected boolean isSpaceMember(String userName, String spacePrettyName) {
    return getSpaceMembers(spacePrettyName).contains(userName);
  }

  /**
   * Gets the space members.
   *
   * @param spacePrettyName the space pretty name
   * @return the space members
   */
  protected Set<String> getSpaceMembers(String spacePrettyName) {
    Space space = spaceService.getSpaceByPrettyName(spacePrettyName);
    Set<String> spaceMembers = new HashSet<String>();
    for (String sm : space.getMembers()) {
      spaceMembers.add(sm);
    }
    return spaceMembers;
  }

  /**
   * Json to provider config.
   *
   * @param json the json
   * @return the call provider configuration
   * @throws JSONException the JSON exception
   */
  protected CallProviderConfiguration jsonToProviderConfig(JSONObject json) throws JSONException {
    String type = json.getString("type");
    boolean active = json.getBoolean("active");

    CallProviderConfiguration conf = new CallProviderConfiguration();
    conf.setActive(active);
    conf.setType(type);

    return conf;
  }

  /**
   * Provider config to json.
   *
   * @param conf the conf
   * @return the JSON object
   * @throws JSONException the JSON exception
   */
  protected JSONObject providerConfigToJson(CallProviderConfiguration conf) throws JSONException {
    JSONObject json = new JSONObject();

    json.put("type", conf.getType());
    json.put("active", conf.isActive());

    return json;
  }

  /**
   * Read saved call provider configuration.
   *
   * @param type the type
   * @return the configuration
   */
  protected CallProviderConfiguration readProviderConfig(String type) {
    final String initialGlobalId = Scope.GLOBAL.getId();
    try {
      String safeType = URLEncoder.encode(type, "UTF-8");
      SettingValue<?> val = settingService.get(Context.GLOBAL, Scope.GLOBAL.id(PROVIDER_SCOPE_NAME), safeType);
      if (val != null) {
        String str = String.valueOf(val.getValue());
        if (str.startsWith("{")) {
          // Assuming it's JSON
          CallProviderConfiguration conf = jsonToProviderConfig(new JSONObject(str));
          return conf;
        } else {
          LOG.warn("Cannot parse saved CallProviderConfiguration: " + str);
        }
      }
    } catch (UnsupportedEncodingException e) {
      LOG.warn("UTF8 encoding required to read provider config", e);
    } catch (JSONException e) {
      LOG.warn("Error reading provider config", e);
    } finally {
      Scope.GLOBAL.id(initialGlobalId);
    }
    return null;
  }

  /**
   * Current user id.
   *
   * @return the string
   */
  protected String currentUserId() {
    ConversationState contextState = ConversationState.getCurrent();
    if (contextState != null) {
      return contextState.getIdentity().getUserId();
    }
    return null; // IdentityConstants.ANONIM
  }

  /**
   * Notify user call state changed.
   *
   * @param call the call
   * @param initiatorId the initiator id
   * @param state the state
   */
  protected void notifyUserCallStateChanged(CallInfo call, String initiatorId, String state) {
    for (UserInfo part : call.getParticipants()) {
      if (UserInfo.TYPE_NAME.equals(part.getType())) {
        // We notify to other part, and in case of deletion including to one who may caused the update
        // for a case if several user clients listening.
        if (initiatorId == null || !initiatorId.equals(part.getId()) || CallState.STOPPED.equals(state)) {
          fireUserCallStateChanged(part.getId(),
                                   call.getId(),
                                   call.getProviderType(),
                                   state,
                                   call.getOwner().getId(),
                                   call.getOwner().getType());
        }
      }
    }
  }

  // ******* Call storage ******

  /**
   * Read call entity.
   *
   * @param savedCall the saved call
   * @param withParticipants the with participants
   * @return the call info
   * @throws CallSettingsException if call entry has wrong settings (Chat room call
   *           title too long or has bad value)
   * @throws IdentityStateException if error reading call owner or participant
   * @throws CallOwnerException if call owner is of wrong type
   */
  protected CallInfo readCallEntity(CallEntity savedCall, boolean withParticipants) throws CallSettingsException,
                                                                                    IdentityStateException,
                                                                                    CallOwnerException {
    if (savedCall != null) {
      IdentityInfo owner;
      String ownerId = savedCall.getOwnerId();
      if (OWNER_TYPE_CHATROOM.equals(savedCall.getOwnerType())) {
        String settings = savedCall.getSettings(); // we expect JSON here
        try {
          JSONObject json = new JSONObject(settings);
          String roomTitle = json.optString("roomTitle");
          if (roomTitle != null && roomTitle.length() > 0) {
            owner = roomInfo(ownerId, roomTitle, new String[0], savedCall.getId());
          } else {
            LOG.warn("Saved call doesn't have room settings: '" + settings + "'");
            throw new CallSettingsException("Saved call doesn't have room settings");
          }
        } catch (JSONException e) {
          LOG.warn("Saved call has wrong room settings format (bad JSON syntax): '" + settings + "'", e);
          throw new CallSettingsException("Saved call has wrong room settings format", e);
        }
      } else if (OWNER_TYPE_SPACE.equals(savedCall.getOwnerType())) {
        owner = spaceInfo(ownerId, savedCall.getId());
      } else if (UserInfo.TYPE_NAME.equals(savedCall.getOwnerType())) {
        owner = getUserInfo(ownerId);
      } else {
        throw new CallOwnerException("Unexpected call owner type: " + savedCall.getOwnerType() + " for " + ownerId);
      }

      String callId = savedCall.getId();

      //
      CallInfo call = new CallInfo(callId, savedCall.getTitle(), owner, savedCall.getProviderType());
      call.setState(savedCall.getState());
      call.setLastDate(savedCall.getLastDate());

      if (withParticipants) {
        // Note: in case of a space/room, here we'll add only the parts that were added on the call creation.
        // TODO should we add all current members as participants (and update their state from the DB) or
        // better to do this on client side?
        for (ParticipantEntity p : participantsStorage.findCallParts(callId)) {
          if (UserInfo.TYPE_NAME.equals(p.getType())) {
            UserInfo user = getUserInfo(p.getId());
            if (user == null) {
              // If user not found we treat it as external participant to work correctly
              // with what addCall() does.
              user = new ParticipantInfo(call.getProviderType(), p.getId());
            }
            user.setState(p.getState());
            user.setClientId(p.getClientId());
            call.addParticipant(user);
          } else {
            LOG.warn("Non user participant skipped for call " + callId + ": " + p.getId() + " (" + p.getType() + ")");
          }
        }
      }
      return call;
    } else {
      return null;
    }
  }

  /**
   * Creates the call entity.
   *
   * @param call the call
   * @return the call entity
   * @throws CallSettingsException if call entry has wrong settings (chat room title)
   */
  protected CallEntity createCallEntity(CallInfo call) throws CallSettingsException {
    CallEntity entity = new CallEntity();
    syncCallEntity(call, entity);
    return entity;
  }

  /**
   * Update call entity from given CallInfo instance.
   *
   * @param call the call
   * @param entity the entity
   * @throws CallSettingsException if call entry has wrong settings (room title)
   */
  protected void syncCallEntity(CallInfo call, CallEntity entity) throws CallSettingsException {
    if (call != null) {
      final String callId = call.getId();
      entity.setId(callId);
      entity.setProviderType(call.getProviderType());
      entity.setTitle(call.getTitle());
      IdentityInfo owner = call.getOwner();
      entity.setOwnerId(owner.getId());
      entity.setOwnerType(owner.getType());
      entity.setState(call.getState());
      entity.setLastDate(call.getLastDate());

      if (OWNER_TYPE_CHATROOM.equals(owner.getType())) {
        entity.setIsGroup(true);
        entity.setIsUser(false);
        RoomInfo room = RoomInfo.class.cast(owner);
        try {
          JSONObject json = new JSONObject();
          json.put("roomTitle", room.getTitle());
          String settings = json.toString();
          if (isValidData(settings)) {
            entity.setSettings(settings);
          } else {
            LOG.warn("Call settings too long: '" + settings + "'. Max value is " + DATA_MAX_LENGTH + " bytes in UTF8 encoding.");
            throw new CallSettingsException("Call settings too long (room title)");
          }
        } catch (UnsupportedEncodingException e) {
          throw new CallSettingsException("Cannot save call settings (UTF8 encoding required)", e);
        } catch (JSONException e) {
          throw new CallSettingsException("Cannot save call settings (title should be a text)", e);
        }
      } else if (OWNER_TYPE_SPACE.equals(owner.getType())) {
        entity.setIsGroup(true);
        entity.setIsUser(false);
      } else {
        entity.setIsGroup(false);
        entity.setIsUser(true);
      }
    } else {
      throw new NullPointerException("Call info is null");
    }
  }

  /**
   * Creates the participant entity.
   *
   * @param callId the call id
   * @param user the user
   * @return the participant entity
   */
  protected ParticipantEntity createParticipantEntity(String callId, UserInfo user) {
    ParticipantEntity part = new ParticipantEntity();
    part.setId(user.getId());
    part.setCallId(callId);
    part.setType(user.getType());
    part.setState(user.getState());
    part.setClientId(user.getClientId());
    return part;
  }

  /**
   * Find cause exception of given type.
   *
   * @param <C> the generic type
   * @param pe the pe
   * @param causeClass the cause class
   * @return the c
   */
  protected <C extends SQLException> C exceptionCause(PersistenceException pe, Class<C> causeClass) {
    Throwable e = pe;
    do {
      if (e != null && causeClass.isAssignableFrom(e.getClass())) {
        return causeClass.cast(e);
      } else {
        Throwable c = e.getCause();
        e = c != null && c != e ? c : null;
      }
    } while (e != null);

    return null;
  }

  // >>>>>>> Call storage: ExoTransactional managed

  /**
   * Creates the call in a single transaction.
   *
   * @param call the call
   * @throws IllegalArgumentException the illegal argument exception
   * @throws IllegalStateException the illegal state exception
   * @throws PersistenceException if persistence exception happen
   * @throws CallSettingsException if call entry has wrong settings (chat room title)
   * @see ExoTransactional
   */
  @ExoTransactional
  protected void txCreateCall(CallInfo call) throws IllegalArgumentException,
                                             IllegalStateException,
                                             PersistenceException,
                                             CallSettingsException {
    callStorage.create(createCallEntity(call));
    String callId = call.getId();
    for (UserInfo p : call.getParticipants()) {
      participantsStorage.create(createParticipantEntity(callId, p));
    }
  }

  /**
   * Save call. For use in {@link #updateCall(CallInfo)} and
   * {@link #updateCallAndParticipant(CallInfo, UserInfo)}.
   *
   * @param call the call
   * @throws CallNotFoundException if call not found
   * @throws IllegalArgumentException the illegal argument exception
   * @throws IllegalStateException the illegal state exception
   * @throws PersistenceException the persistence exception
   * @throws CallSettingsException if call entry has wrong settings (room call title)
   */
  private void saveCall(CallInfo call) throws CallNotFoundException,
                                       IllegalArgumentException,
                                       IllegalStateException,
                                       PersistenceException,
                                       CallSettingsException {
    CallEntity entity = callStorage.find(call.getId());
    if (entity != null) {
      syncCallEntity(call, entity);
      callStorage.update(entity);
    } else {
      throw new CallNotFoundException("Call not found: " + call.getId());
    }
  }

  /**
   * Update existing call (mark it started, stopped etc) in a single transaction.
   *
   * @param call the call
   * @throws CallNotFoundException if call not found in storage
   * @throws IllegalArgumentException the illegal argument exception
   * @throws IllegalStateException the illegal state exception
   * @throws PersistenceException the persistence exception
   * @throws CallSettingsException if call entry has wrong settings (room call title)
   */
  @ExoTransactional
  protected void txUpdateCall(CallInfo call) throws CallNotFoundException,
                                             IllegalArgumentException,
                                             IllegalStateException,
                                             PersistenceException,
                                             CallSettingsException {
    saveCall(call);
  }

  /**
   * Save participant, for use in {@link #updateParticipant(String, UserInfo)} and
   * {@link #updateCallAndParticipant(CallInfo, UserInfo)}.
   *
   * @param callId the call id
   * @param participant the participant
   * @throws IllegalArgumentException the illegal argument exception
   * @throws IllegalStateException the illegal state exception
   * @throws PersistenceException the persistence exception
   * @throws ParticipantNotFoundException if participant not found in storage
   */
  private void saveParticipant(String callId, UserInfo participant) throws IllegalArgumentException,
                                                                    IllegalStateException,
                                                                    PersistenceException,
                                                                    ParticipantNotFoundException {
    // Update participant
    ParticipantEntity part = participantsStorage.find(new ParticipantId(participant.getId(), callId));
    if (part != null) {
      part.setState(participant.getState());
      part.setClientId(participant.getClientId());
      participantsStorage.update(part);
    } else {
      throw new ParticipantNotFoundException("Call participant " + participant.getId() + " not found for " + callId);
    }
  }

  /**
   * Update call participant (for joined or leaved state) in a single transaction.
   *
   * @param callId the call id
   * @param participant the participant
   * @throws IllegalArgumentException the illegal argument exception
   * @throws IllegalStateException the illegal state exception
   * @throws PersistenceException the persistence exception
   * @throws ParticipantNotFoundException if call participant not found in storage
   */
  @ExoTransactional
  protected void txUpdateParticipant(String callId, UserInfo participant) throws IllegalArgumentException,
                                                                          IllegalStateException,
                                                                          PersistenceException,
                                                                          ParticipantNotFoundException {
    saveParticipant(callId, participant);
  }

  /**
   * Update call and all its participants in a single transaction.
   *
   * @param call the call
   * @throws IllegalArgumentException the illegal argument exception
   * @throws IllegalStateException the illegal state exception
   * @throws PersistenceException the persistence exception
   * @throws CallNotFoundException if call or its participants not found in storage
   * @throws CallSettingsException if call entry has wrong settings (room call title)
   * @throws ParticipantNotFoundException if call participant not found in storage
   */
  @ExoTransactional
  protected void txUpdateCallAndParticipants(CallInfo call) throws IllegalArgumentException,
                                                            IllegalStateException,
                                                            PersistenceException,
                                                            CallNotFoundException,
                                                            CallSettingsException,
                                                            ParticipantNotFoundException {
    // Update call
    saveCall(call);

    // Update participants
    // TODO this could be done with a single SQL UPDATE, but need ensure that JPA will not fail after that to
    // access the call due to presence in the session of participant entities with outdated state (like it was
    // faced in addCall() when deleted outdated call before creating a new one)
    String callId = call.getId();
    for (UserInfo p : call.getParticipants()) {
      saveParticipant(callId, p);
    }
  }

  /**
   * Delete call within a transaction.
   *
   * @param id the call id
   * @return true, if successful
   * @throws IllegalArgumentException the illegal argument exception
   * @throws IllegalStateException the illegal state exception
   * @throws PersistenceException the persistence exception
   */
  @ExoTransactional
  protected boolean txDeleteCall(String id) throws IllegalArgumentException, IllegalStateException, PersistenceException {
    CallEntity entity = callStorage.find(id);
    if (entity != null) {
      callStorage.delete(entity);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Delete all user calls (not group ones) within a single transaction.
   *
   * @return number of deleted calls
   * @throws IllegalArgumentException the illegal argument exception
   * @throws IllegalStateException the illegal state exception
   * @throws PersistenceException the persistence exception
   */
  @ExoTransactional
  protected int txDeleteAllUserCalls() throws IllegalArgumentException, IllegalStateException, PersistenceException {
    return callStorage.deleteAllUsersCalls();
  }

  // <<<<<<< Call storage: ExoTransactional managed

  // >>>>>>> Call storage: wrappers to catch JPA exceptions

  /**
   * Delete call.
   *
   * @param id the id
   * @return true, if successful
   * @throws StorageException if storage error happens
   */
  protected boolean deleteCall(String id) throws StorageException {
    try {
      return txDeleteCall(id);
    } catch (IllegalArgumentException | IllegalStateException | PersistenceException e) {
      throw new StorageException("Error deleting call " + id, e);
    }
  }

  /**
   * Delete all user calls. This will not touch any group call. For use on server start to cleanup the
   * storage.
   *
   * @return number of deleted calls
   * @throws StorageException if storage error happens
   * 
   */
  protected int deleteAllUserCalls() throws StorageException {
    try {
      return txDeleteAllUserCalls();
    } catch (IllegalArgumentException | IllegalStateException | PersistenceException e) {
      throw new StorageException("Error deleting all user calls", e);
    }
  }

  /**
   * Update call and all its participants.
   *
   * @param call the call
   * @throws StorageException if storage exception happen
   * @throws ParticipantNotFoundException if call or its participants not found in storage
   * @throws CallSettingsException if call entry has wrong settings (room call title)
   * @throws CallNotFoundException if call not found in storage
   */
  protected void updateCallAndParticipants(CallInfo call) throws StorageException,
                                                          ParticipantNotFoundException,
                                                          CallSettingsException,
                                                          CallNotFoundException {
    try {
      txUpdateCallAndParticipants(call);
    } catch (IllegalArgumentException | IllegalStateException | PersistenceException e) {
      throw new StorageException("Error updating call and participants: " + call.getId(), e);
    }
  }

  /**
   * Update call participant (for joined or leaved state) in a single transaction.
   *
   * @param callId the call id
   * @param participant the participant
   * @throws ParticipantNotFoundException if call participant not found in storage
   * @throws StorageException if storage exception happen
   */
  protected void updateParticipant(String callId, UserInfo participant) throws ParticipantNotFoundException, StorageException {
    try {
      txUpdateParticipant(callId, participant);
    } catch (IllegalArgumentException | IllegalStateException | PersistenceException e) {
      throw new StorageException("Error updating participant " + participant.getId() + " of call " + callId, e);
    }
  }

  /**
   * Update existing call (mark it started, stopped etc).
   *
   * @param call the call
   * @throws CallNotFoundException if call not found in storage
   * @throws CallSettingsException if call entry has wrong settings (room call title)
   * @throws StorageException if storage exception happen
   */
  protected void updateCall(CallInfo call) throws CallNotFoundException, CallSettingsException, StorageException {
    try {
      txUpdateCall(call);
    } catch (IllegalArgumentException | IllegalStateException | PersistenceException e) {
      throw new StorageException("Error updating call " + call.getId(), e);
    }
  }

  /**
   * Find call in storage by its ID.
   *
   * @param id the id
   * @return the call info
   * @throws IdentityStateException if error reading call owner or participant
   * @throws StorageException if persistent error happens
   * @throws CallSettingsException if call entry has wrong settings (Chat room call
   *           title too long or has bad value)
   * @throws CallOwnerException if call owner type of unknown type
   */
  protected CallInfo findCallById(String id) throws IdentityStateException,
                                             StorageException,
                                             CallSettingsException,
                                             CallOwnerException {
    try {
      CallEntity savedCall = callStorage.find(id);
      return readCallEntity(savedCall, true);
    } catch (IllegalArgumentException | IllegalStateException | PersistenceException e) {
      throw new StorageException("Error reading call " + id, e);
    }
  }

  /**
   * Find a call, owned by group with given ID, in calls storage.
   *
   * @param ownerId the owner id
   * @return the string or <code>null</code> if no call found
   * @throws StorageException the storage exception
   */
  protected String findGroupCallId(String ownerId) throws StorageException {
    // TODO it's not efficient read the whole entity when we need only an ID (or null)
    try {
      CallEntity savedCall = callStorage.findGroupCallByOwnerId(ownerId);
      if (savedCall != null) {
        return savedCall.getId();
      }
      return null;
    } catch (IllegalArgumentException | IllegalStateException | PersistenceException e) {
      throw new StorageException("Error reading owner group call ID by " + ownerId, e);
    }
  }

  /**
   * Find user group calls in calls storage.
   *
   * @param userId the user id
   * @return the collection
   * @throws StorageException if persistence error happen
   */
  protected Collection<CallInfo> findUserGroupCalls(String userId) throws StorageException {
    try {
      List<CallEntity> savedCalls = callStorage.findUserGroupCalls(userId);
      List<CallInfo> calls = new ArrayList<>();
      for (CallEntity c : savedCalls) {
        try {
          calls.add(readCallEntity(c, false));
        } catch (CallInfoException | IdentityStateException e) {
          // In this context we can skip erroneous calls abd let user to know only about valid ones
          // IdentityStateException if error reading call participant
          // CallInfoException if call has wrong settings (type or room settings)
          LOG.warn("Error reading user group call: " + c.getId(), e);
        }
      }
      return Collections.unmodifiableCollection(calls);
    } catch (IllegalArgumentException | IllegalStateException | PersistenceException e) {
      throw new StorageException("Error reading user group calls by " + userId, e);
    }
  }

  /**
   * Create the call in storage with all its participants. If such call (by ID) already exists a
   * {@link CallConflictException} will be raised.
   *
   * @param call the call info
   * @throws StorageException if storage exception happen
   * @throws CallConflictException the call conflicts with another (same ID) call
   * @throws CallSettingsException the call settings wrong (chat room title)
   */
  protected void createCall(CallInfo call) throws StorageException, CallConflictException, CallSettingsException {
    try {
      // Persist the call with all its participants
      txCreateCall(call);
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new StorageException("Error creating call " + call.getId(), e);
    } catch (PersistenceException pe) {
      // Check if it's not already existing call, inform user accordingly
      SQLIntegrityConstraintViolationException constEx = exceptionCause(pe, SQLIntegrityConstraintViolationException.class);
      if (constEx != null && constEx.getMessage().indexOf("PK_WBC_CALLID") >= 0) {
        CallEntity conflictedCallEntity = callStorage.find(call.getId());
        if (conflictedCallEntity != null) {
          // We can fail from here or return this already created, in second case we may return not
          // exactly what was originally requested (by data and participants).
          // Taking in account a check for existence in invalidateCall(), that should be used before this
          // method, we raise an error to the caller with details.
          if (CallState.STARTED.equals(call.getState())) {
            for (UserInfo savedPart : call.getParticipants()) {
              Set<UserCallListener> ulisteners = userListeners.get(savedPart.getId());
              if (ulisteners != null) {
                for (UserCallListener ul : ulisteners) {
                  if (savedPart.hasSameClientId(ul.getClientId())) {
                    // this part already joined and runs in the call
                    if (LOG.isDebugEnabled()) {
                      LOG.debug("Call already started and running: " + call.getId(), pe);
                    }
                    throw new CallConflictException("Call already started and running");
                  }
                }
              }
            }
            if (LOG.isDebugEnabled()) {
              LOG.debug("Call already started: " + call.getId(), pe);
            }
            throw new CallConflictException("Call already started");
          } else {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Call already created with state " + conflictedCallEntity.getState() + ": " + call.getId(), pe);
            }
            throw new CallConflictException("Call already created");
          }
        } else {
          LOG.warn("Call ID already found but cannot read the call: " + call.getId(), pe);
          throw new CallConflictException("Call ID already found", pe);
        }
      } else {
        // We cannot create this call
        LOG.error("Error creating call: " + call.getId(), pe);
        throw new StorageException("Error creating call", pe);
      }
    }
  }

  /**
   * Invalidate a call by an ID. If such call (by ID) already found and, it's P2P call but not active -
   * it will be removed, otherwise {@link CallConflictException} will be thrown. This method designed to be
   * called just before {@link #createCall(CallInfo)}.
   *
   * @param id the id
   * @param isGroup the is group
   * @throws CallConflictException if call conflict found and existing call cannot be removed
   * @throws StorageException if storage exception happen
   */
  protected void invalidateCall(String id, boolean isGroup) throws CallConflictException, StorageException {
    try {
      CallEntity existingCallEntity = callStorage.find(id);
      if (existingCallEntity != null) {
        if (isGroup) {
          // Group call: already exists, we return an error - it needs read/start existing call
          throw new CallConflictException("Call already created");
        } else {
          // P2P call: need check if it is STARTED and does someone already joined and actually
          // connected,
          // if have one - it's an error to return, otherwise we treat this call as outdated
          // (could be left not properly stopped on an error like server crash or network lose)
          // thus we try to delete it before creating a new one
          try {
            CallInfo existingCall = readCallEntity(existingCallEntity, true);
            if (CallState.STARTED.equals(existingCall.getState())) {
              for (UserInfo savedPart : existingCall.getParticipants()) {
                Set<UserCallListener> ulisteners = userListeners.get(savedPart.getId());
                if (ulisteners != null) {
                  for (UserCallListener ul : ulisteners) {
                    if (savedPart.hasSameClientId(ul.getClientId())) {
                      // this part already joined and runs in the call
                      throw new CallConflictException("Call already started");
                    }
                  }
                }
              }
              deleteCall(id);
              LOG.warn("Deleted not active call: " + id);
            } else {
              deleteCall(id);
              LOG.warn("Deleted outdated call: " + id);
            }
          } catch (CallInfoException | IdentityStateException e) {
            LOG.warn("Call in erroneous state: " + id, e);
            deleteCall(id);
            LOG.warn("Deleted erroneous call: " + id);
          }
        }
      }
    } catch (IllegalArgumentException | IllegalStateException | PersistenceException e) {
      LOG.warn("Error reading call by ID: " + id, e);
    }
  }

  /**
   * Metric message for reporting to the stats logger.
   *
   * @param userId the user id of the operation
   * @param call the call in the operation
   * @param operation the operation name
   * @param status the status of operation
   * @param duration the operation duration in millseconds (can be <code>null</code>), it's not a call duration
   * @param error the error if present (can be <code>null</code>)
   * @return the string combining all given parameters in specified format
   *         according
   *         https://community.exoplatform.com/portal/g/:spaces:exo_itop/exo_itop/wiki/SPEC_-_logging_for_monitoring
   */
  protected String metricMessage(String userId,
                                 // String clientId,
                                 CallInfo call,
                                 String operation,
                                 String status,
                                 Long duration,
                                 String error) {
    StringBuilder res = new StringBuilder("service=webconferencing");
    res.append(" operation=").append(operation);
    res.append(" status=").append(status);
    res.append(" parameters=");
    res.append("\"userId:").append(userId);
    // res.append(", clientId:").append(clientId);
    res.append(", isGroup:").append(call.getOwner().isGroup());
    res.append(", owner:").append(call.getOwner().getId());
    res.append(", ownerType:").append(call.getOwner().getType());
    res.append(", provider:").append(call.getProviderType());
    res.append(", state:").append(call.getState());
    res.append(", participantsCount:").append(call.getParticipants().size());
    if (call.getLastDate() != null) {
      long callDuration = Math.round((System.currentTimeMillis() - call.getLastDate().getTime()) / 1000);
      res.append(", callDuration_sec:").append(callDuration);
    }
    res.append("\"");
    if (error != null && error.length() > 0) {
      res.append(" error_msg=\"").append(error).append("\"");
    }
    res.append(" duration_ms=").append(duration != null ? duration : -1);
    return res.toString();
  }

  // <<<<<<< Call storage: wrappers to catch JPA exceptions

}
