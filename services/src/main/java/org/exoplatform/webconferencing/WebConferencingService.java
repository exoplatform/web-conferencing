
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

import static org.exoplatform.webconferencing.IdentityInfo.isValidId;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webconferencing.UserInfo.IMInfo;
import org.exoplatform.webconferencing.dao.CallDAO;
import org.exoplatform.webconferencing.dao.ParticipantDAO;
import org.exoplatform.webconferencing.domain.CallEntity;
import org.exoplatform.webconferencing.domain.ParticipantEntity;
import org.exoplatform.webconferencing.domain.ParticipantId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.picocontainer.Startable;

import com.ibm.icu.util.Calendar;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: SkypeService.java 00000 Feb 22, 2017 pnedonosko $
 */
public class WebConferencingService implements Startable {

  /** The Constant SPACE_TYPE_NAME. */
  public static final String    SPACE_TYPE_NAME       = "space".intern();

  /** The Constant CHAT_ROOM_TYPE_NAME. */
  public static final String    CHAT_ROOM_TYPE_NAME   = "chat_room".intern();

  /** The Constant GROUP_CALL_TYPE. */
  protected static final String GROUP_CALL_TYPE       = "group".intern();

  /** The Constant CALL_OWNER_SCOPE_NAME. */
  protected static final String CALL_OWNER_SCOPE_NAME = "webconferencing.callOwner".intern();

  /** The Constant CALL_ID_SCOPE_NAME. */
  protected static final String CALL_ID_SCOPE_NAME    = "webconferencing.callId".intern();

  /** The Constant USER_CALLS_SCOPE_NAME. */
  protected static final String USER_CALLS_SCOPE_NAME = "webconferencing.user.calls".intern();

  /** The Constant PROVIDER_SCOPE_NAME. */
  protected static final String PROVIDER_SCOPE_NAME   = "webconferencing.provider".intern();

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

    /** The pretty name. */
    protected final String name;

    /**
     * Instantiates a new room info.
     *
     * @param id the id
     * @param name the name, it's pretty name of the room (like what spaces have)
     * @param title the title
     */
    public RoomInfo(String id, String name, String title) {
      super(id, title);
      this.name = name;
      this.profileLink = IdentityInfo.EMPTY;
    }

    /**
     * Gets the pretty name.
     *
     * @return the name
     */
    public String getName() {
      return name;
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
  public static final String                         OWNER_TYPE_SPACE    = "space";

  /** The Constant OWNER_TYPE_CHATROOM. */
  public static final String                         OWNER_TYPE_CHATROOM = "chat_room";

  /** The Constant LOG. */
  protected static final Log                         LOG                 = ExoLogger.getLogger(WebConferencingService.class);

  /** The jcr service. */
  protected final RepositoryService                  jcrService;

  /** The session providers. */
  protected final SessionProviderService             sessionProviders;

  /** The hierarchy owner. */
  protected final NodeHierarchyCreator               hierarchyCreator;

  /** The organization. */
  protected final OrganizationService                organization;

  /** The social identity manager. */
  protected final IdentityManager                    socialIdentityManager;

  /** The drive service. */
  protected final ManageDriveService                 driveService;

  /** The listener service. */
  protected final ListenerService                    listenerService;

  /** The settings service. */
  protected final SettingService                     settingService;

  /** The calls storage. */
  protected final CallDAO                            callStorage;

  /** The participants storage. */
  protected final ParticipantDAO                     participantsStorage;

  /** The providers. */
  protected final Map<String, CallProvider>          providers           = new ConcurrentHashMap<>();

  /** The space service. */
  protected SpaceService                             spaceService;

  /** The user listeners. */
  protected final Map<String, Set<UserCallListener>> userListeners       = new ConcurrentHashMap<>();

  /**
   * Instantiates a new web conferencing service.
   *
   * @param jcrService the jcr service
   * @param sessionProviders the session providers
   * @param hierarchyCreator the hierarchy creator
   * @param organization the organization
   * @param socialIdentityManager the social identity manager
   * @param driveService the drive service
   * @param listenerService the listener service
   * @param settingService the setting service
   * @param callStorage the call storage
   * @param participantsStorage the participants storage
   */
  public WebConferencingService(RepositoryService jcrService,
                                SessionProviderService sessionProviders,
                                NodeHierarchyCreator hierarchyCreator,
                                OrganizationService organization,
                                IdentityManager socialIdentityManager,
                                ManageDriveService driveService,
                                ListenerService listenerService,
                                SettingService settingService,
                                CallDAO callStorage,
                                ParticipantDAO participantsStorage) {
    this.jcrService = jcrService;
    this.sessionProviders = sessionProviders;
    this.hierarchyCreator = hierarchyCreator;
    this.organization = organization;
    this.socialIdentityManager = socialIdentityManager;
    this.driveService = driveService;
    this.listenerService = listenerService;
    this.settingService = settingService;
    this.callStorage = callStorage;
    this.participantsStorage = participantsStorage;
  }

  /**
   * Gets the user info.
   *
   * @param id the id
   * @return the user info
   * @throws Exception the exception
   */
  public UserInfo getUserInfo(String id) throws Exception {
    User user = organization.getUserHandler().findUserByName(id);
    if (user != null) {
      Identity userIdentity = socialIdentityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, id, true);
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
        // TODO exception here?
        LOG.warn("Social identity not found for " + user.getUserName() + " (" + user.getFirstName() + " " + user.getLastName()
            + ")");
      }
    } else {
      // TODO exception here?
      LOG.warn("User not found: " + id);
    }
    return null;
  }

  /**
   * Gets the space info.
   *
   * @param spacePrettyName the space pretty name
   * @return the space info
   * @throws Exception the exception
   */
  // @ExoTransactional // TODO it's read-only op, no need tx
  public SpaceInfo getSpaceInfo(String spacePrettyName) throws Exception {
    Space socialSpace = spaceService.getSpaceByPrettyName(spacePrettyName);
    SpaceInfo space = new SpaceInfo(socialSpace);
    for (String sm : socialSpace.getMembers()) {
      UserInfo user = getUserInfo(sm);
      if (user != null) {
        space.addMember(user);
      }
    }
    space.setProfileLink(socialSpace.getUrl());
    space.setAvatarLink(socialSpace.getAvatarUrl());
    space.setCallId(readOwnerCallId(spacePrettyName));
    return space;
  }

  /**
   * Gets the room info.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param members the room members
   * @return the room info
   * @throws Exception the exception
   */
  // @ExoTransactional // TODO it's read-only op, no need tx
  public RoomInfo getRoomInfo(String id, String name, String title, String[] members) throws Exception {
    RoomInfo room = roomInfo(id, name, title, members, readOwnerCallId(id));
    return room;
  };

  /**
   * Adds the call to list of active and fires STARTED event.
   *
   * @param id the id
   * @param ownerId the owner id
   * @param ownerType the owner type
   * @param title the title
   * @param providerType the provider type
   * @param parts the parts
   * @return the call info
   * @throws Exception the exception
   */
  public CallInfo addCall(String id,
                          String ownerId,
                          String ownerType,
                          String title,
                          String providerType,
                          Collection<String> parts) throws Exception {
    // Ensure call and owner IDs length is OK
    if (isValidId(id)) {
      if (isValidId(ownerId)) {
        final boolean isUser = UserInfo.TYPE_NAME.equals(ownerType);
        final boolean isSpace = OWNER_TYPE_SPACE.equals(ownerType);
        final boolean isRoom = OWNER_TYPE_CHATROOM.equals(ownerType);
        final IdentityInfo owner;
        if (isUser) {
          UserInfo userInfo = getUserInfo(ownerId);
          if (userInfo == null) {
            // if owner user not found, it's possibly an external user, thus treat it as a chat room
            owner = new RoomInfo(ownerId, ownerId, title);
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
            owner = new RoomInfo(ownerId, ownerId, title);
            owner.setAvatarLink(LinkProvider.SPACE_DEFAULT_AVATAR_URL);
          }
        } else if (isRoom) {
          owner = new RoomInfo(ownerId, ownerId, title);
          owner.setAvatarLink(LinkProvider.SPACE_DEFAULT_AVATAR_URL);
        } else {
          throw new CallInfoException("Wrong call owner type: " + ownerType);
        }
        Set<UserInfo> participants = new LinkedHashSet<>();
        for (String pid : parts) {
          if (isValidId(pid)) {
            UserInfo part = getUserInfo(pid);
            if (part != null) {
              // it's eXo user
              participants.add(part);
            } else {
              // external participant
              participants.add(new ParticipantInfo(providerType, pid));
            }
          } else {
            LOG.error("Cannot add call participant with too long ID: " + pid);
            throw new WrongIdException("Wrong participant ID (" + pid + ")");
          }
        }

        String prevId = readOwnerCallId(ownerId);

        // Save the call
        CallInfo call = new CallInfo(id, title, owner, providerType);
        call.addParticipants(participants);
        call.setState(CallState.STARTED);
        call.setLastDate(Calendar.getInstance().getTime());
        createCall(call);

        String userId = currentUserId();
        if (isSpace || isRoom) {
          // it's group call
          for (UserInfo part : participants) {
            if (UserInfo.TYPE_NAME.equals(part.getType())) {
              if (prevId != null) {
                // XXX For a case when some client failed to delete an existing (but outdated etc.) call but
                // already starting a new one
                // It's SfB usecase when browser client failed to delete outdated call (browser/plugin crashed
                // in IE11) and then starts a new one
                // removeUserGroupCallId(part.getId(), prevId); // TODO change state to leaved?!!
              }
              if (!userId.equals(part.getId())) {
                // fire group's user listener for incoming, except of the caller
                fireUserCallStateChanged(part.getId(), id, providerType, CallState.STARTED, ownerId, ownerType);
              }
            }
          }
        } else if (isUser) {
          // It's P2P call
          notifyUserCallStateChanged(call, userId, CallState.STARTED);
        }

        return call;
      } else {
        throw new WrongIdException("Wrong owner ID value");
      }
    } else {
      throw new WrongIdException("Wrong call ID value");
    }
  }

  /**
   * Gets an active call info.
   *
   * @param id the id
   * @return the call info or <code>null</code> if call not found
   * @throws Exception the exception
   */
  // @ExoTransactional // TODO it's read-only op, no need tx
  public CallInfo getCall(String id) throws Exception {
    return readCallById(id);
  }

  /**
   * Removes the call info from active and fires STOPPED event.
   *
   * @param id the id
   * @param remove the remove
   * @return the call info or <code>null</code> if call not found
   * @throws Exception the exception
   */
  public CallInfo stopCall(String id, boolean remove) throws Exception {
    String userId = currentUserId();
    CallInfo call = readCallById(id);
    if (call != null) {
      stopCall(call, userId, remove);
    } /*
       * TODO cleanup -- else if (remove) {
       * // XXX for a case of previous version storage format, cleanup saved call ID
       * if (userId != null && id.startsWith("g/")) {
       * removeUserGroupCallId(userId, id);
       * }
       * }
       */
    return call;
  }

  /**
   * Stop call.
   *
   * @param call the call
   * @param userId the user id
   * @param remove the remove
   * @return the call info
   * @throws Exception the exception
   */
  protected CallInfo stopCall(CallInfo call, String userId, boolean remove) throws Exception {
    // Delete or update in single tx
    if (remove) {
      deleteCall(call);
    } else {
      call.setState(CallState.STOPPED);
      updateCall(call);
    }
    // Then notify users
    if (call.getOwner().isGroup()) {
      String callId = call.getId();
      for (UserInfo part : call.getParticipants()) {
        if (UserInfo.TYPE_NAME.equals(part.getType())) {
          // It's eXo user: fire user listener for stopped call,
          // but, in case if stopped with removal (deleted call), not to the initiator (of deletion)
          // - a given user.
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
   * @return the call info or <code>null</code> if call not found
   * @throws Exception the exception
   */
  // @ExoTransactional // TODO it's read-only op, no need tx
  public CallInfo startCall(String id) throws Exception {
    CallInfo call = readCallById(id);
    if (call != null) {
      startCall(call);
    }
    return call;
  }

  /**
   * Start existing call.
   *
   * @param call the call
   * @throws Exception the exception
   */
  protected void startCall(CallInfo call) throws Exception {
    // TODO exception if user not a participant?

    String callId = call.getId();
    UserInfo joined = null;

    // We save call in a single tx, thus logic split on gathering the changes and saving them at the end
    call.setState(CallState.STARTED);

    if (call.getOwner().isGroup()) {
      // Find an user who joins (current user who starts a group call will join it first)
      String userId = currentUserId();
      for (UserInfo part : call.getParticipants()) {
        if (UserInfo.TYPE_NAME.equals(part.getType()) && userId.equals(part.getId())) {
          part.setState(UserState.JOINED);
          joined = part;
          break;
        }
      }
      if (joined != null) {
        // First save the group call with joined participant (in single tx)
        updateCallAndParticipant(call, joined);
      } else {
        // This should not be a case, but we preserve the logic - may be admin could start it?
        LOG.warn("Call started by not a participant: " + userId + ", call: " + callId);
        updateCall(call);
      }
      // Then fire this call started to all parts, including the user itself
      for (UserInfo part : call.getParticipants()) {
        fireUserCallStateChanged(part.getId(),
                                 callId,
                                 call.getProviderType(),
                                 CallState.STARTED,
                                 call.getOwner().getId(),
                                 call.getOwner().getType());
      }
    } else {
      updateCall(call);
    }
  }

  /**
   * If call started, then notify all its parties that given participant joined. If stopped, then
   * {@link InvalidCallStateException} will be thrown.
   *
   * @param id the id
   * @param userId the user id
   * @return the call info or <code>null</code> if call not found
   * @throws Exception the exception
   */
  public CallInfo joinCall(String id, String userId) throws Exception {
    // TODO exception if user not a participant?
    CallInfo call = readCallById(id);
    if (call != null) {
      if (CallState.STARTED.equals(call.getState())) {
        UserInfo joined = null;
        // save Joined first
        for (UserInfo part : call.getParticipants()) {
          if (UserInfo.TYPE_NAME.equals(part.getType()) && userId.equals(part.getId())) {
            part.setState(UserState.JOINED);
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
                               userId,
                               part.getId());
          }
        }
      } else {
        startCall(call);
      }
    } // TODO else an error?
    return call;
  }

  /**
   * If call started, then notify all its parties that given participant leaved. If stopped, then
   * {@link InvalidCallStateException} will be thrown.
   *
   * @param id the id
   * @param userId the user id
   * @return the call info or <code>null</code> if call not found
   * @throws Exception the exception
   */
  public CallInfo leaveCall(String id, String userId) throws Exception {
    // TODO exception if user not a participant?
    CallInfo call = readCallById(id);
    if (call != null) {
      if (CallState.STARTED.equals(call.getState()) || CallState.PAUSED.equals(call.getState())) {
        UserInfo leaved = null;
        int leavedNum = 0;
        // save Joined first
        for (UserInfo part : call.getParticipants()) {
          if (UserInfo.TYPE_NAME.equals(part.getType())) {
            if (userId.equals(part.getId())) {
              part.setState(UserState.LEAVED);
              leaved = part;
              leavedNum++;
            } else {
              // if null - user hasn't joined
              if (part.getState() == null || part.getState().equals(UserState.LEAVED)) {
                leavedNum++;
              }
            }
          }
        }
        // then save if someone joined (it should but we preserve the logic)
        if (leaved != null) {
          // First save the call with the participant (in single tx)
          updateParticipant(id, leaved);
          // Fire user joined to all parts, including the user itself
          for (UserInfo part : call.getParticipants()) {
            // Fire user leaved to all parts, including the user itself
            fireUserCallLeaved(id,
                               call.getProviderType(),
                               call.getOwner().getId(),
                               call.getOwner().getType(),
                               userId,
                               part.getId());
          }
          // Check if don't need stop the call if all parts leaved already
          if (leavedNum == call.getParticipants().size()) {
            stopCall(call, userId, false); // Stop in another tx
          }
        }
      } else {
        // It seems has no big sense to return error for already stopped call
        // XXX throw new InvalidCallStateException("Call not started");
      }
    }
    return call;
  }

  /**
   * Gets the user calls.
   *
   * @param userId the user id
   * @return the user call states
   * @throws Exception the exception
   */
  // @ExoTransactional // TODO it's read-only op, no need tx
  public CallState[] getUserCalls(String userId) throws Exception {
    CallState[] states =
                       readUserGroupCalls(userId).stream()
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
        // TODO we may lose events: when one request is completing pooling with some event, the listener will
        // stop listen and next one will be registered after some period - this time events will not be
        // delivered to the client.
        // As a solution, we need a temporal pool to save (deferred) events for given user
        // (listener.getUserId()) until it will send a new request or the pool expired
        if (listener.isListening()) {
          listener.onCallStateChanged(callId, providerType, callState, ownerId, ownerType);
        }
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
        if (listener.isListening()) {
          listener.onPartJoined(callId, providerType, ownerId, ownerType, partId);
        }
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
        if (listener.isListening()) {
          listener.onPartLeaved(callId, providerType, ownerId, ownerType, partId);
        }
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
        LOG.warn("Video Calls provider type '" + existing.getType() + "' already registered. Skipped plugin: " + provider);
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
   * @throws Exception the exception
   */
  public CallProviderConfiguration getProviderConfiguration(String providerType, Locale locale) throws Exception {
    CallProvider p = getProvider(providerType);
    if (p != null) {
      CallProviderConfiguration conf = readProviderConfig(p.getType());
      if (conf == null) {
        conf = CallProviderConfiguration.fromProvider(p, locale);
      } else {
        conf.setTitle(p.getTitle());
        conf.setDescription(p.getDescription(locale));
      }
      return conf;
    }
    return null; // not found
  }

  /**
   * Save provider configuration.
   *
   * @param config the config
   * @throws Exception the exception
   */
  public void saveProviderConfiguration(CallProviderConfiguration config) throws Exception {
    saveProviderConfig(config);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    // XXX SpaceService done in crappy way and we need reference it after the container start only, otherwise
    // it will fail the whole server start due to not found JCR service
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
      LOG.error("Error cleaning calls from previous server execution", e);
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
   * Read JSON from the string.
   *
   * @param str the str
   * @return the JSON object
   * @throws JSONException the JSON exception if string cannot be parsed to JSON
   * @throws CallInfoException if string doesn't start with '{'
   */
  protected JSONObject readJson(String str) throws JSONException, CallInfoException {
    if (str.startsWith("{")) {
      return new JSONObject(str);
    } else {
      throw new CallInfoException("String not in JSON format: " + str);
    }
  }

  /**
   * Read call by id.
   *
   * @param id the id
   * @return the call info
   * @throws Exception the exception
   */
  protected CallInfo readCallById(String id) throws Exception {
    CallEntity savedCall = callStorage.find(id);
    return readCallEntity(savedCall, true);
  }

  /**
   * Read owner call ID.
   *
   * @param ownerId the owner id
   * @return the string or <code>null</code> if no call found
   */
  protected String readOwnerCallId(String ownerId) {
    // TODO it's not efficient read the whole entity when we need only an ID (or null)
    CallEntity savedCall = callStorage.findGroupCallByOwnerId(ownerId);
    if (savedCall != null) {
      return savedCall.getId();
    }
    return null;
  }

  /**
   * Read user group call ids.
   *
   * @param userId the user id
   * @return the collection
   * @throws Exception the exception
   */
  protected Collection<CallInfo> readUserGroupCalls(String userId) throws Exception {
    List<CallEntity> savedCalls = callStorage.findUserGroupCalls(userId);
    List<CallInfo> calls = new ArrayList<>();
    for (CallEntity c : savedCalls) {
      calls.add(readCallEntity(c, false));
    }
    return Collections.unmodifiableCollection(calls);
  }

  /**
   * Save user group call id.
   *
   * @param userId the user id
   * @param callId the call id
   */
  @Deprecated
  protected void saveUserGroupCallId(String userId, String callId) {
    final String initialContextId = Context.USER.getId();
    final String initialScopeId = Scope.GLOBAL.getId();
    final Context userContext = userId != null ? Context.USER.id(userId) : Context.USER;
    final Scope userScope = Scope.GLOBAL.id(USER_CALLS_SCOPE_NAME);
    try {
      StringBuilder newVal = new StringBuilder();
      SettingValue<?> val = settingService.get(userContext, userScope, GROUP_CALL_TYPE);
      if (val != null) {
        String oldVal = String.valueOf(val.getValue());
        // XXX it may happen that user will list already deleted call IDs (if client failed to call delete but
        // started
        if (oldVal.indexOf(callId) >= 0) {
          return; // already contains this call ID
        } else {
          newVal.append(oldVal);
          newVal.append('\n');
        }
      }
      newVal.append(callId);
      settingService.set(userContext, userScope, GROUP_CALL_TYPE, SettingValue.create(newVal.toString()));
    } finally {
      Scope.GLOBAL.id(initialScopeId);
      Context.USER.id(initialContextId);
    }
  }

  /**
   * Removes the user group call id.
   *
   * @param userId the user id
   * @param callId the call id
   */
  @Deprecated
  protected void removeUserGroupCallId(String userId, String callId) {
    final String initialContextId = Context.USER.getId();
    final String initialScopeId = Scope.GLOBAL.getId();
    final Context userContext = userId != null ? Context.USER.id(userId) : Context.USER;
    final Scope userScope = Scope.GLOBAL.id(USER_CALLS_SCOPE_NAME);
    try {
      SettingValue<?> val = settingService.get(userContext, userScope, GROUP_CALL_TYPE);
      if (val != null) {
        String oldVal = String.valueOf(val.getValue());
        int start = oldVal.indexOf(callId);
        if (start >= 0) {
          StringBuilder newVal = new StringBuilder(oldVal);
          newVal.delete(start, start + callId.length() + 1); // also delete a \n as separator
          settingService.set(userContext, userScope, GROUP_CALL_TYPE, SettingValue.create(newVal.toString()));
        }
      }
    } finally {
      Scope.GLOBAL.id(initialScopeId);
      Context.USER.id(initialContextId);
    }
  }

  /**
   * Json to provider config.
   *
   * @param json the json
   * @return the call provider configuration
   * @throws Exception the exception
   */
  protected CallProviderConfiguration jsonToProviderConfig(JSONObject json) throws Exception {
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
   * @throws Exception the exception
   */
  protected CallProviderConfiguration readProviderConfig(String type) throws Exception {
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
      return null;
    } finally {
      Scope.GLOBAL.id(initialGlobalId);
    }
  }

  /**
   * Save provider configuration.
   *
   * @param conf the conf
   * @throws Exception the exception
   */
  protected void saveProviderConfig(CallProviderConfiguration conf) throws Exception {
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
   * Room info.
   *
   * @param id the id
   * @param name the name
   * @param title the title
   * @param members the members
   * @param callId the call id
   * @return the room info
   * @throws Exception the exception
   */
  protected RoomInfo roomInfo(String id, String name, String title, String[] members, String callId) throws Exception {
    RoomInfo room = new RoomInfo(id, name, title);
    for (String userName : members) {
      UserInfo user = getUserInfo(userName);
      if (user != null) {
        room.addMember(user);
      } else {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Skipped not found user: " + userName);
        }
        throw new IdentityNotFound("User " + userName + " not found or not accessible");
      }
    }
    room.setProfileLink(IdentityInfo.EMPTY);
    room.setAvatarLink(LinkProvider.SPACE_DEFAULT_AVATAR_URL);
    room.setCallId(callId);
    return room;
  };

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
   * @throws Exception the exception
   */
  protected CallInfo readCallEntity(CallEntity savedCall, boolean withParticipants) throws Exception {
    if (savedCall != null) {
      IdentityInfo owner;
      String ownerId = savedCall.getOwnerId();
      if (OWNER_TYPE_CHATROOM.equals(savedCall.getOwnerType())) {
        String settings = savedCall.getSettings(); // we expect JSON here
        JSONObject json = readJson(settings);
        String roomName = json.optString("roomName");
        String roomTitle = json.optString("roomTitle");
        if (roomName != null && roomName.length() > 0 && roomTitle != null && roomTitle.length() > 0) {
          owner = roomInfo(ownerId, roomName, roomTitle, new String[0], savedCall.getId());
        } else {
          LOG.error("Saved call doesn't contain room settings: '" + settings + "'");
          throw new CallInfoException("Saved call doesn't contain room settings");
        }
      } else if (OWNER_TYPE_SPACE.equals(savedCall.getOwnerType())) {
        owner = getSpaceInfo(ownerId);
      } else if (UserInfo.TYPE_NAME.equals(savedCall.getOwnerType())) {
        owner = getUserInfo(ownerId);
      } else {
        LOG.error("Unexpected call owner, type: " + savedCall.getOwnerType() + ", id: " + ownerId);
        throw new CallInfoException("Unexpected call owner: " + ownerId);
      }

      String callId = savedCall.getId();

      //
      CallInfo call = new CallInfo(callId, savedCall.getTitle(), owner, savedCall.getProviderType());
      call.setState(savedCall.getState());
      call.setLastDate(savedCall.getLastDate());

      if (withParticipants) {
        // TODO read parts
        // TODO read only if changed in DB (we could rely on call time or other incremental-on-update flag)
        for (ParticipantEntity p : participantsStorage.findCallParts(callId)) {
          UserInfo user = readParticipantEntity(p);
          if (user != null) {
            call.addParticipant(user);
          } else {
            LOG.warn("Non user participant skipped for call " + callId + ": " + p.getId() + " (" + p.getType() + ")");
          }
        }
      }

      call.setEntity(savedCall);
      return call;
    } else {
      return null;
    }
  }

  /**
   * Read participant entity.
   *
   * @param savedPart the saved part
   * @return the user info
   * @throws Exception the exception
   */
  protected UserInfo readParticipantEntity(ParticipantEntity savedPart) throws Exception {
    if (UserInfo.TYPE_NAME.equals(savedPart.getType())) {
      UserInfo user = getUserInfo(savedPart.getId());
      user.setState(savedPart.getState());
      return user;
    }
    return null;
  }

  /**
   * Creates the call entity.
   *
   * @param call the call
   * @return the call entity
   * @throws Exception the exception
   */
  protected CallEntity createCallEntity(CallInfo call) throws Exception {
    CallEntity entity = new CallEntity();
    updateCallEntity(call, entity);
    return entity;
  }

  /**
   * Update call entity.
   *
   * @param call the call
   * @param entity the entity
   * @throws Exception the exception
   */
  protected void updateCallEntity(CallInfo call, CallEntity entity) throws Exception {
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
        RoomInfo room = RoomInfo.class.cast(owner);
        JSONObject json = new JSONObject();
        json.put("roomName", room.getName());
        json.put("roomTitle", room.getTitle());
        entity.setSettings(json.toString());
        entity.setIsGroup(1);
        entity.setIsUser(0);
      } else if (OWNER_TYPE_SPACE.equals(owner.getType())) {
        entity.setIsGroup(1);
        entity.setIsUser(0);
      } else {
        entity.setIsGroup(0);
        entity.setIsUser(1);
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
    return part;
  }

  // >>>>>>> Call storage: ExoTransactional managed

  /**
   * Creates the call in a single transaction of the storage.
   *
   * @param call the call
   * @throws Exception the exception
   * @see ExoTransactional
   */
  @ExoTransactional
  protected void createCall(CallInfo call) throws Exception {
    CallEntity entity = callStorage.create(createCallEntity(call));
    call.setEntity(entity);
    // Save all call participants
    // TODO For update we need a marker of actually changed parts (whole collection - yes or no) for better
    // performance,
    // in some cases it's non need to update parts at all: e.g. stop of a call (but what about part state
    // then?)
    // if (partsUdpated) ...
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
   * @throws Exception the exception
   */
  private void saveCall(CallInfo call) throws Exception {
    CallEntity entity = call.getEntity();
    if (entity == null) {
      String callId = call.getId();
      LOG.warn("Saving call without persistent entity: " + callId);
      entity = callStorage.find(callId);
    }
    // Update call
    updateCallEntity(call, entity);
    entity = callStorage.update(entity);
    call.setEntity(entity);
  }

  /**
   * Update existing call (mark it started, stopped etc).
   *
   * @param call the call
   * @throws Exception the exception
   */
  @ExoTransactional
  protected void updateCall(CallInfo call) throws Exception {
    saveCall(call);
  }

  /**
   * Save participant, for use in {@link #updateParticipant(String, UserInfo)} and
   * {@link #updateCallAndParticipant(CallInfo, UserInfo)}.
   *
   * @param callId the call id
   * @param participant the participant
   * @throws Exception the exception
   */
  private void saveParticipant(String callId, UserInfo participant) throws Exception {
    // Update participant
    ParticipantEntity part = participantsStorage.find(new ParticipantId(participant.getId(), callId));
    if (part != null) {
      part.setState(participant.getState());
      participantsStorage.update(part);
    } else {
      throw new CallInfoException("Call participant has no saved entity " + participant.getId() + " for " + callId);
    }
  }

  /**
   * Update call participant (for joined or leaved state).
   *
   * @param callId the call id
   * @param participant the participant
   * @throws Exception the exception
   */
  @ExoTransactional
  protected void updateParticipant(String callId, UserInfo participant) throws Exception {
    saveParticipant(callId, participant);
  }

  @ExoTransactional
  protected void updateCallAndParticipant(CallInfo call, UserInfo participant) throws Exception {
    // Update call
    saveCall(call);

    // Update participant
    saveParticipant(call.getId(), participant);
  }

  /**
   * Delete call.
   *
   * @param call the call
   * @return true, if successful
   * @throws Exception the exception
   */
  @ExoTransactional
  protected boolean deleteCall(CallInfo call) throws Exception {
    CallEntity entity = call.getEntity();
    if (entity == null) {
      // This should not be case, thus log it
      LOG.warn("Deleting call without persistent entity: " + call.getId());
      entity = callStorage.find(call.getId());
    }
    if (entity != null) {
      callStorage.delete(entity);
      call.setEntity(null);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Delete all user calls. This will not touch any group call. For use on server start to cleanup the
   * storage.
   *
   * @return the int
   * @throws Exception the exception
   */
  @ExoTransactional
  protected int deleteAllUserCalls() throws Exception {
    return callStorage.deleteAllUsersCalls();
  }

  // <<<<<<< Call storage: ExoTransactional managed

}
