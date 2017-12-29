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
package org.exoplatform.webconferencing.cometd;

import static org.exoplatform.webconferencing.IdentityInfo.isValidId;
import static org.exoplatform.webconferencing.Utils.asJSON;
import static org.exoplatform.webconferencing.support.CallLog.DEBUG_LEVEL;
import static org.exoplatform.webconferencing.support.CallLog.ERROR_LEVEL;
import static org.exoplatform.webconferencing.support.CallLog.INFO_LEVEL;
import static org.exoplatform.webconferencing.support.CallLog.TRACE_LEVEL;
import static org.exoplatform.webconferencing.support.CallLog.WARN_LEVEL;
import static org.exoplatform.webconferencing.support.CallLog.validate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.cometd.annotation.Param;
import org.cometd.annotation.RemoteCall;
import org.cometd.annotation.ServerAnnotationProcessor;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.annotation.Subscription;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.BayeuxServer.ChannelListener;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerChannel.SubscriptionListener;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.eclipse.jetty.util.component.LifeCycle;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.CallInfoException;
import org.exoplatform.webconferencing.CallState;
import org.exoplatform.webconferencing.IdentityInfo;
import org.exoplatform.webconferencing.UserCallListener;
import org.exoplatform.webconferencing.UserState;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.client.ErrorInfo;
import org.exoplatform.webconferencing.support.CallLog;
import org.exoplatform.webconferencing.support.CallLogService;
import org.mortbay.cometd.continuation.EXoContinuationBayeux;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform SAS.
 *
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CometdWebConferencingService.java 00000 Aug 17, 2017 pnedonosko $
 */
public class CometdWebConferencingService implements Startable {

  /** The Constant CALLS_CHANNEL_NAME. */
  public static final String             CALLS_CHANNEL_NAME                    = "/webconferencing/calls";

  /** The Constant LOGS_CHANNEL_NAME. */
  public static final String             LOGS_CHANNEL_NAME                     = "/webconferencing/logs";

  /** The Constant CALL_SUBSCRIPTION_CHANNEL_NAME. */
  public static final String             CALL_SUBSCRIPTION_CHANNEL_NAME        = "/eXo/Application/WebConferencing/call";

  /** The Constant CALL_SUBSCRIPTION_CHANNEL_NAME_ALL. */
  public static final String             CALL_SUBSCRIPTION_CHANNEL_NAME_ALL    = CALL_SUBSCRIPTION_CHANNEL_NAME + "/**";

  /** The Constant CALL_SUBSCRIPTION_CHANNEL_NAME_PARAMS. */
  public static final String             CALL_SUBSCRIPTION_CHANNEL_NAME_PARAMS =
                                                                               "/eXo/Application/WebConferencing/call/{callType}/{callInfo}";

  /** The Constant USER_SUBSCRIPTION_CHANNEL_NAME. */
  public static final String             USER_SUBSCRIPTION_CHANNEL_NAME        = "/eXo/Application/WebConferencing/user";

  /** The Constant USER_SUBSCRIPTION_CHANNEL_PATTERN. */
  public static final String             USER_SUBSCRIPTION_CHANNEL_PATTERN     = USER_SUBSCRIPTION_CHANNEL_NAME + "/{userId}";

  /** The Constant COMMAND_GET. */
  public static final String             COMMAND_GET                           = "get";

  /** The Constant COMMAND_CREATE. */
  public static final String             COMMAND_CREATE                        = "create";

  /** The Constant COMMAND_UPDATE. */
  public static final String             COMMAND_UPDATE                        = "update";

  /** The Constant COMMAND_DELETE. */
  public static final String             COMMAND_DELETE                        = "delete";

  /** The Constant COMMAND_GET_CALLS_STATE. */
  public static final String             COMMAND_GET_CALLS_STATE               = "get_calls_state";

  /**
   * Remote call term (a meta information such as context, user ID, client ID, log level and prefix etc) max
   * length.
   */
  public static final int                TERM_MAX_LENGTH                       = 32;

  /** The Constant LOG. */
  private static final Log               LOG                                   =
                                             ExoLogger.getLogger(CometdWebConferencingService.class);

  /** The web conferencing. */
  protected final WebConferencingService webConferencing;

  /** The exo bayeux. */
  protected final EXoContinuationBayeux  exoBayeux;

  /** The service. */
  protected final CallService            service;

  /** The organization. */
  protected final OrganizationService    organization;

  /** The session providers. */
  protected final SessionProviderService sessionProviders;

  /** The identity registry. */
  protected final IdentityRegistry       identityRegistry;

  /** The call logs. */
  protected final CallLogService         callLogs;

  /**
   * The Class CallService.
   */
  @Service("webconferencing")
  public class CallService {

    abstract class ContainerCommand implements Runnable {

      final String containerName;

      /**
       * Instantiates a new container command.
       *
       * @param containerName the container name
       */
      ContainerCommand(String containerName) {
        this.containerName = containerName;
      }

      /**
       * Execute actual work of the commend (in extending class).
       *
       * @param exoContainer the exo container
       */
      abstract void execute(ExoContainer exoContainer);

      /**
       * Callback to execute on container error.
       *
       * @param error the error
       */
      abstract void onContainerError(String error);

      /**
       * {@inheritDoc}
       */
      @Override
      public void run() {
        if (isValidId(containerName)) {
          // Do the work under eXo container context (for proper work of eXo apps and JPA storage)
          ExoContainer exoContainer = ExoContainerContext.getContainerByName(containerName);
          if (exoContainer != null) {
            ExoContainer contextContainer = ExoContainerContext.getCurrentContainerIfPresent();
            try {
              // Container context
              ExoContainerContext.setCurrentContainer(exoContainer);
              RequestLifeCycle.begin(exoContainer);
              // do the work here
              execute(exoContainer);
            } finally {
              // Restore context
              RequestLifeCycle.end();
              ExoContainerContext.setCurrentContainer(contextContainer);
            }
          } else {
            // LOG.warn("Container not found " + containerName + " for remote call " + contextName);
            onContainerError("Container not found");
          }
        } else {
          onContainerError("Container required");
        }
      }
    }

    /**
     * The Class UserChannelContext.
     */
    class UserChannelContext {

      /** The clients. */
      final Set<String>      clients = ConcurrentHashMap.newKeySet();

      /** The listener. */
      final UserCallListener listener;

      /**
       * Instantiates a new channel context.
       *
       * @param listener the listener
       */
      UserChannelContext(UserCallListener listener) {
        super();
        this.listener = listener;
      }

      /**
       * Gets the listener.
       *
       * @return the listener
       */
      UserCallListener getListener() {
        return listener;
      }

      /**
       * Checks for no clients.
       *
       * @return true, if successful
       */
      boolean hasNoClients() {
        return clients.isEmpty();
      }

      /**
       * Checks for client.
       *
       * @param clientId the client id
       * @return true, if successful
       */
      boolean hasClient(String clientId) {
        return clients.contains(clientId);
      }

      /**
       * Removes the client.
       *
       * @param clientId the client id
       * @return true, if successful
       */
      boolean removeClient(String clientId) {
        boolean res = clients.remove(clientId);
        if (clients.size() == 0) {
          webConferencing.removeUserCallListener(listener);
          if (LOG.isDebugEnabled()) {
            LOG.debug("<<< Removed user call listener for " + listener.getUserId() + ", client:" + clientId);
          }
        } else if (res) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("<<< Removed user call client for " + listener.getUserId() + ", client:" + clientId);
          }
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("<<< User call client was not removed for " + listener.getUserId() + ", client:" + clientId);
          }
        }
        return res;
      }

      /**
       * Adds the client.
       *
       * @param clientId the client id
       * @return true, if successful
       */
      boolean addClient(String clientId) {
        boolean wasEmpty = clients.size() == 0;
        boolean res = clients.add(clientId);
        if (wasEmpty && res) {
          webConferencing.addUserCallListener(listener);
          if (LOG.isDebugEnabled()) {
            LOG.debug("<<< Added first user call listener for " + listener.getUserId() + ", client:" + clientId);
          }
        } else if (res) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("<<< Added user call client for " + listener.getUserId() + ", client:" + clientId);
          }
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("<<< User call client was not added for " + listener.getUserId() + ", client:" + clientId);
          }
        }
        return res;
      }
    }

    /**
     * The Class CallChannelContext.
     */
    class CallChannelContext {

      final String containerName;

      /**
       * Instantiates a new user channel context.
       *
       * @param containerName the container name
       */
      CallChannelContext(String containerName) {
        this.containerName = containerName;
      }

      /**
       * Gets the container name.
       *
       * @return the containerName
       */
      String getContainerName() {
        return containerName;
      }
    }

    /**
     * The listener interface for receiving channelSubscription events.
     * The class that is interested in processing a channelSubscription
     * event implements this interface, and the object created
     * with that class is registered with a component using the
     * component's <code>addChannelSubscriptionListener<code> method. When
     * the channelSubscription event occurs, that object's appropriate
     * method is invoked.
     */
    class ChannelSubscriptionListener implements SubscriptionListener {

      /**
       * {@inheritDoc}
       */
      @Override
      public void subscribed(ServerSession remote, ServerChannel channel, ServerMessage message) {
        // FYI message will be null for server-side subscription
        String clientId = remote.getId();
        String channelId = channel.getId();
        String currentUserId = currentUserId(message);
        String exoClientId = (String) message.get("exoClientId");
        String exoContainerName = (String) message.get("exoContainerName");
        if (LOG.isDebugEnabled()) {
          LOG.debug(">> Subscribed: " + currentUserId + ", client:" + clientId + " (" + exoContainerName + "@" + exoClientId
              + "), channel:" + channelId);
        }
        if (channelId.startsWith(USER_SUBSCRIPTION_CHANNEL_NAME)) {
          try {
            if (currentUserId != null) {
              String userId = channelUserId(channelId);
              if (currentUserId.equals(userId)) {
                // Tracking channel-to-clients mapping looks redundant, at first thought client-to-listener is
                // enough, but we keep channel's clients also for understanding consistency during development
                // and debug
                // Sep 8 2017: We need a single user listener per his channel, if add more then we'll have
                // multiple events for a single update

                UserChannelContext context = userChannelContext.computeIfAbsent(channelId, k -> {
                  UserCallListener listener = new UserCallListener(userId) {
                    @Override
                    public void onPartLeaved(String callId,
                                             String providerType,
                                             String ownerId,
                                             String ownerType,
                                             String partId) {
                      StringBuilder data = new StringBuilder();
                      data.append('{');
                      data.append("\"eventType\": \"call_leaved\",");
                      data.append("\"callId\": \"");
                      data.append(callId);
                      data.append("\",\"providerType\": \"");
                      data.append(providerType);
                      data.append("\",\"part\": {");
                      data.append("\"id\": \"");
                      data.append(partId);
                      data.append("\"},\"owner\": {");
                      data.append("\"id\": \"");
                      data.append(ownerId);
                      data.append("\",\"type\": \"");
                      data.append(ownerType);
                      data.append("\"}");
                      data.append('}');
                      bayeux.getChannel(channelId).publish(serverSession, data.toString());
                    }

                    @Override
                    public void onPartJoined(String callId,
                                             String providerType,
                                             String ownerId,
                                             String ownerType,
                                             String partId) {
                      StringBuilder data = new StringBuilder();
                      data.append('{');
                      data.append("\"eventType\": \"call_joined\",");
                      data.append("\"callId\": \"");
                      data.append(callId);
                      data.append("\",\"providerType\": \"");
                      data.append(providerType);
                      data.append("\",\"part\": {");
                      data.append("\"id\": \"");
                      data.append(partId);
                      data.append("\"},\"owner\": {");
                      data.append("\"id\": \"");
                      data.append(ownerId);
                      data.append("\",\"type\": \"");
                      data.append(ownerType);
                      data.append("\"}");
                      data.append('}');
                      bayeux.getChannel(channelId).publish(serverSession, data.toString());
                    }

                    @Override
                    public void onCallStateChanged(String callId,
                                                   String providerType,
                                                   String callState,
                                                   String ownerId,
                                                   String ownerType) {
                      StringBuilder data = new StringBuilder();
                      data.append('{');
                      data.append("\"eventType\": \"call_state\",");
                      data.append("\"callId\": \"");
                      data.append(callId);
                      data.append("\",\"providerType\": \"");
                      data.append(providerType);
                      data.append("\",\"callState\": \"");
                      data.append(callState);
                      data.append("\",\"owner\": {");
                      data.append("\"id\": \"");
                      data.append(ownerId);
                      data.append("\",\"type\": \"");
                      data.append(ownerType);
                      data.append("\"}");
                      data.append('}');
                      bayeux.getChannel(channelId).publish(serverSession, data.toString());
                      if (LOG.isDebugEnabled()) {
                        LOG.debug(">>>> Sent call state update to " + channelId + " by " + currentUserId(null));
                      }
                    }

                    @Override
                    public boolean isListening() {
                      // TODO change the flag ASAP when will know about unsubscribing or session disconnected
                      return true;
                    }
                  };
                  if (LOG.isDebugEnabled()) {
                    LOG.debug("<<< Created user channel context for " + userId + ", client:" + clientId + ", channel:"
                        + channelId);
                  }
                  return new UserChannelContext(listener);
                });
                context.addClient(clientId);
              } else {
                LOG.warn("Subscribing to other user not possible, was user " + currentUserId + ", channel:" + channelId);
                remote.deliver(serverSession,
                               channelId,
                               ErrorInfo.clientError("Subscribing to other user not possible").asJSON());
                if (!channel.unsubscribe(remote)) {
                  LOG.warn("Unable to unsubscribe user " + currentUserId + " from channel " + channelId);
                }
              }
            } else {
              LOG.warn("Subscribing by unauthorized user not possible, was channel: " + channelId);
              remote.deliver(serverSession, channelId, ErrorInfo.accessError("Unauthorized user").asJSON());
              if (!channel.unsubscribe(remote)) {
                LOG.warn("Unable to unsubscribe unauthorized user from channel " + channelId);
              }
            }
          } catch (IndexOutOfBoundsException e) {
            // Ignore channel w/o a subpath (userId here) at the end
            if (LOG.isDebugEnabled()) {
              LOG.debug(">> Ignore user channel w/o user ID at the end: " + channelId, e);
            }
          }
        } else if (channelId.startsWith(CALL_SUBSCRIPTION_CHANNEL_NAME)) {
          if (exoContainerName != null) {
            callChannelContext.computeIfAbsent(channelId, k -> {
              if (LOG.isDebugEnabled()) {
                LOG.debug("<<< Created call channel context by " + currentUserId + ", client:" + clientId + ", channel:"
                    + channelId);
              }
              return new CallChannelContext(exoContainerName);
            });
          } else {
            LOG.warn("eXo container not defined for " + currentUserId + ", client:" + clientId + ", channel:" + channelId);
          }
        }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void unsubscribed(ServerSession session, ServerChannel channel, ServerMessage message) {
        // FYI message will be null for server-side unsubscription
        String clientId = session.getId();
        String channelId = channel.getId();
        if (LOG.isDebugEnabled()) {
          LOG.debug("<< Unsubscribed client:" + clientId + ", channel:" + channelId);
        }
        cleanupChannelClient(channelId, clientId);
      }
    }

    /**
     * The listener interface for receiving client channel events.
     */
    class ClientChannelListener implements ChannelListener {

      /**
       * {@inheritDoc}
       */
      @Override
      public void configureChannel(ConfigurableServerChannel channel) {
        // TODO need something special?
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void channelAdded(ServerChannel channel) {
        // Add sub/unsub listener to WebConferencing channel
        final String channelId = channel.getId();
        if (LOG.isDebugEnabled()) {
          LOG.debug("> Channel added: " + channelId);
        }
        if (channelId.startsWith(USER_SUBSCRIPTION_CHANNEL_NAME) || channelId.startsWith(CALL_SUBSCRIPTION_CHANNEL_NAME)) {
          channel.addListener(subscriptionListener);
          if (LOG.isDebugEnabled()) {
            LOG.debug(">> Added subscription listener for channel: " + channelId);
          }
        }
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void channelRemoved(String channelId) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("< Channel removed: " + channelId);
        }
        if (channelId.startsWith(USER_SUBSCRIPTION_CHANNEL_NAME)) {
          // Channel already doesn't exist here, ensure all its client listeners were unregistered
          UserChannelContext context = userChannelContext.remove(channelId);
          if (context != null) {
            webConferencing.removeUserCallListener(context.getListener());
            if (LOG.isDebugEnabled()) {
              LOG.debug("<< Removed user call listener for channel: " + channelId);
            }
          } else {
            if (LOG.isDebugEnabled()) {
              LOG.debug("<< User call channel context not found for:" + channelId);
            }
          }
        } else if (channelId.startsWith(CALL_SUBSCRIPTION_CHANNEL_NAME)
            && channelId.length() > CALL_SUBSCRIPTION_CHANNEL_NAME.length()) {
          String callId = channelId.substring(CALL_SUBSCRIPTION_CHANNEL_NAME.length() + 1);
          // This call communications ended, in normal way or by network failure - we assume the call ended,
          // ensure its parties, including those who received incoming notification but not yet
          // accepted/rejected it, will be notified that the call stopped/removed.
          CallChannelContext context = callChannelContext.remove(channelId);
          if (context != null) {
            new Thread(new ContainerCommand(context.getContainerName()) {
              /**
               * {@inheritDoc}
               */
              @Override
              void execute(ExoContainer exoContainer) {
                try {

                  CallInfo call = webConferencing.getCall(callId);
                  if (call != null) {
                    // may be need leave all them and let that logic to stop the call?
                    webConferencing.stopCall(callId, !call.getOwner().isGroup());
                  }
                } catch (Exception e) {
                  LOG.error("Error reading call " + callId, e);
                }
              }

              /**
               * {@inheritDoc}
               */
              @Override
              void onContainerError(String error) {
                LOG.error("Container error: " + error + " (" + containerName + ") for channel removal " + channelId);
              }
            }).start();
          } else {
            LOG.warn("Call context not found for " + callId);
          }
        }
      }
    }

    /** The bayeux. */
    @Inject
    private BayeuxServer                          bayeux;

    /** The local session. */
    @Session
    private LocalSession                          localSession;

    /** The server session. */
    @Session
    private ServerSession                         serverSession;

    /** User channel context. */
    private final Map<String, UserChannelContext> userChannelContext   = new ConcurrentHashMap<>();

    /** Call channel context. */
    private final Map<String, CallChannelContext> callChannelContext   = new ConcurrentHashMap<>();

    /** The subscription listener. */
    private final ChannelSubscriptionListener     subscriptionListener = new ChannelSubscriptionListener();

    /** The channel listener. */
    private final ClientChannelListener           channelListener      = new ClientChannelListener();

    /**
     * Post construct.
     */
    @PostConstruct
    public void postConstruct() {
      bayeux.addListener(channelListener);
    }

    /**
     * Pre destroy.
     */
    @PreDestroy
    public void preDestroy() {
      // cleanup listeners
      bayeux.removeListener(channelListener);
      for (UserChannelContext context : userChannelContext.values()) {
        webConferencing.removeUserCallListener(context.getListener());
      }
      userChannelContext.clear();
    }

    /**
     * Subscribe calls. All data exchanged between peers of a call will go there (e.g. RPC media and
     * connectivity settings).
     *
     * @param message the message
     * @param callType the call type
     * @param callInfo the call info
     */
    @Subscription(CALL_SUBSCRIPTION_CHANNEL_NAME_PARAMS)
    public void subscribeCalls(Message message, @Param("callType") String callType, @Param("callInfo") String callInfo) {
      if (LOG.isDebugEnabled()) {
        String callId = callId(callType, callInfo);
        // TODO log this to a dedicated diagnostic log!
        LOG.debug("Call published in " + message.getChannel() + " by " + message.get("sender") + " callId: " + callId + " data: "
            + message.getJSON());
      }
    }

    /**
     * Subscribe user. All user updates about his state and call will go here.
     *
     * @param message the message
     * @param userId the user id
     */
    @Subscription(USER_SUBSCRIPTION_CHANNEL_NAME)
    public void subscribeUser(Message message, @Param("userId") String userId) {
      final String channelId = message.getChannel();
      if (LOG.isDebugEnabled()) {
        LOG.debug("User published in " + channelId + " by " + message.getClientId() + " userId: " + userId + " data: "
            + message.getJSON());
      }
    }

    /**
     * Remote calls from clients to Web Conferencing services.
     *
     * @param caller the caller
     * @param data the data
     */
    @RemoteCall(CALLS_CHANNEL_NAME)
    public void rcCalls(final RemoteCall.Caller caller, final Object data) {
      final ServerSession session = caller.getServerSession();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Remote call by " + session.getId() + " data: " + data);
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> arguments = (Map<String, Object>) data;
      String containerName = (String) arguments.get("exoContainerName");

      // TODO use thread pool (take in account CPUs number of cores etc)
      new Thread(new ContainerCommand(containerName) {
        /**
         * {@inheritDoc}
         */
        @Override
        void execute(ExoContainer exoContainer) {
          try {
            // String exoClientId = (String) arguments.get("exoClientId"); // TODO need it here, for logger
            // may be?
            String currentUserId = (String) arguments.get("exoId");
            if (isValidId(currentUserId)) {
              // Do all the job under actual (requester) user: set this user as current identity in eXo
              // XXX We rely on EXoContinuationBayeux.EXoSecurityPolicy for user security here (exoId above)
              // Use services acquired from context container
              IdentityRegistry identityRegistry = exoContainer.getComponentInstanceOfType(IdentityRegistry.class);
              SessionProviderService sessionProviders = exoContainer.getComponentInstanceOfType(SessionProviderService.class);
              WebConferencingService webConferencing = exoContainer.getComponentInstanceOfType(WebConferencingService.class);
              Identity userIdentity = identityRegistry.getIdentity(currentUserId);
              if (userIdentity != null) {
                ConversationState contextState = ConversationState.getCurrent();
                SessionProvider contextProvider = sessionProviders.getSessionProvider(null);
                try {
                  // User context (2)
                  ConversationState convState = new ConversationState(userIdentity);
                  convState.setAttribute(ConversationState.SUBJECT, userIdentity.getSubject());
                  ConversationState.setCurrent(convState);
                  SessionProvider userProvider = new SessionProvider(convState);
                  sessionProviders.setSessionProvider(null, userProvider);
                  // Process the request
                  String id = (String) arguments.get("id");
                  if (id != null) {
                    String command = (String) arguments.get("command");
                    if (command != null) {
                      if (COMMAND_GET.equals(command)) {
                        try {
                          CallInfo call = webConferencing.getCall(id);
                          if (call != null) {
                            caller.result(asJSON(call));
                          } else {
                            caller.failure(ErrorInfo.notFoundError("Call not found").asJSON());
                          }
                        } catch (Throwable e) {
                          LOG.error("Error reading call info '" + id + "' by '" + currentUserId + "'", e);
                          caller.failure(ErrorInfo.serverError("Error reading call record").asJSON());
                        }
                      } else if (COMMAND_UPDATE.equals(command)) {
                        String state = (String) arguments.get("state");
                        if (state != null) {
                          try {
                            boolean stateRecognized = true;
                            CallInfo call;
                            if (CallState.STARTED.equals(state)) {
                              call = webConferencing.startCall(id);
                            } else if (CallState.STOPPED.equals(state)) {
                              call = webConferencing.stopCall(id, false);
                            } else if (UserState.JOINED.equals(state)) {
                              call = webConferencing.joinCall(id, currentUserId);
                            } else if (UserState.LEAVED.equals(state)) {
                              call = webConferencing.leaveCall(id, currentUserId);
                            } else {
                              call = null;
                              stateRecognized = false;
                            }
                            if (stateRecognized) {
                              if (call != null) {
                                caller.result(asJSON(call));
                              } else {
                                caller.failure(ErrorInfo.notFoundError("Call not found").asJSON());
                              }
                            } else {
                              caller.failure(ErrorInfo.clientError("Wrong request parameters: state not recognized").asJSON());
                            }
                          } catch (Throwable e) {
                            LOG.error("Error updating call '" + id + "' by '" + currentUserId + "'", e);
                            caller.failure(ErrorInfo.serverError("Error updating call record").asJSON());
                          }
                        } else {
                          caller.failure(ErrorInfo.clientError("Wrong request parameters: state").asJSON());
                        }
                      } else if (COMMAND_CREATE.equals(command)) {
                        String ownerId = (String) arguments.get("owner");
                        if (ownerId != null) {
                          String ownerType = (String) arguments.get("ownerType");
                          if (ownerType != null) {
                            String providerType = (String) arguments.get("provider");
                            if (providerType != null) {
                              String title = (String) arguments.get("title"); // topic
                              if (title != null) {
                                String pstr = (String) arguments.get("participants");
                                if (pstr != null) {
                                  List<String> participants = Arrays.asList(pstr.split(";"));
                                  try {
                                    CallInfo call =
                                                  webConferencing.addCall(id,
                                                                          ownerId,
                                                                          ownerType,
                                                                          title,
                                                                          providerType,
                                                                          participants);
                                    caller.result(asJSON(call));
                                  } catch (CallInfoException e) {
                                    // aka BAD_REQUEST
                                    caller.failure(ErrorInfo.clientError(e.getMessage()).asJSON());
                                  } catch (Throwable e) {
                                    LOG.error("Error creating call for '" + id + "' by '" + currentUserId + "'", e);
                                    caller.failure(ErrorInfo.serverError("Error creating call record").asJSON());
                                  }
                                } else {
                                  caller.failure(ErrorInfo.clientError("Wrong request parameters: participants").asJSON());
                                }
                              } else {
                                caller.failure(ErrorInfo.clientError("Wrong request parameters: title").asJSON());
                              }
                            } else {
                              caller.failure(ErrorInfo.clientError("Wrong request parameters: provider").asJSON());
                            }
                          } else {
                            caller.failure(ErrorInfo.clientError("Wrong request parameters: ownerType").asJSON());
                          }
                        } else {
                          caller.failure(ErrorInfo.clientError("Wrong request parameters: owner").asJSON());
                        }
                      } else if (COMMAND_DELETE.equals(command)) {
                        try {
                          CallInfo call = webConferencing.stopCall(id, true);
                          if (call != null) {
                            caller.result(asJSON(call));
                          } else {
                            caller.failure(ErrorInfo.notFoundError("Call not found").asJSON());
                          }
                        } catch (Throwable e) {
                          LOG.error("Error deleting call '" + id + "' by '" + currentUserId + "'", e);
                          caller.failure(ErrorInfo.serverError("Error deleting call record").asJSON());
                        }
                      } else if (COMMAND_GET_CALLS_STATE.equals(command)) {
                        if (id.equals(currentUserId)) { // id it's user name for this command
                          try {
                            CallState[] calls = webConferencing.getUserCalls(id);
                            caller.result(asJSON(calls));
                          } catch (Throwable e) {
                            LOG.error("Error reading users calls for '" + id + "'", e);
                            caller.failure(ErrorInfo.serverError("Error reading users calls").asJSON());
                          }
                        } else {
                          // Don't let read other user calls
                          caller.failure(ErrorInfo.clientError("Wrong request parameters: id (does not match)").asJSON());
                        }
                      } else {
                        LOG.warn("Unknown call command " + command + " for '" + id + "' from '" + currentUserId + "'");
                        caller.failure(ErrorInfo.clientError("Unknown command").asJSON());
                      }
                    } else {
                      caller.failure(ErrorInfo.clientError("Wrong request parameters: command").asJSON());
                    }
                  } else {
                    caller.failure(ErrorInfo.clientError("Wrong request parameters: id").asJSON());
                  }
                } finally {
                  // Restore context (2)
                  ConversationState.setCurrent(contextState);
                  sessionProviders.setSessionProvider(null, contextProvider);
                }
              } else {
                LOG.warn("User identity not found " + currentUserId + " for remote call of " + CALLS_CHANNEL_NAME);
                caller.failure(ErrorInfo.clientError("User identity not found").asJSON());
              }
            } else {
              caller.failure(ErrorInfo.clientError("Unauthorized user").asJSON());
            }
          } catch (Throwable e) {
            LOG.error("Error processing call request from client " + session.getId() + " with data: " + data, e);
            caller.failure(ErrorInfo.serverError("Error processing call request: " + e.getMessage()).asJSON());
          }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void onContainerError(String error) {
          LOG.warn("Container error: " + error + " (" + containerName + ") for remote call of " + CALLS_CHANNEL_NAME);
          caller.failure(ErrorInfo.clientError(error).asJSON());
        }
      }).start();
    }

    /**
     * Remote calls from clients to Web Conferencing services.
     *
     * @param caller the caller
     * @param data the data
     */
    @RemoteCall(LOGS_CHANNEL_NAME)
    public void rcLogs(final RemoteCall.Caller caller, final Object data) {
      final ServerSession session = caller.getServerSession();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Remote log by " + session.getId() + " data: " + data);
      }

      // TODO use thread pool (take in account CPUs number of cores etc)
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            @SuppressWarnings("unchecked")
            Map<String, Object> arguments = (Map<String, Object>) data;
            // We will receive following entries:
            // exoId - string, eXo's username who logs the data
            // exoClientId - string, an ID generated by core web conferencing to distinguish running clients
            // data - string or object with actual diagnostic info
            // level - string, it's a log level: one of "trace", "debug", "info", "warn", "error"
            // prefix - string, a log record prefix (e.g. provider type or app context)
            // time - string, date in ISO format and UTC
            String currentUserId = (String) arguments.get("exoId");
            if (isValidId(currentUserId)) {
              // String containerName = (String) arguments.get("exoContainerName");
              // if (isValidArg(containerName)) {
              String clientId = (String) arguments.get("exoClientId");
              if (isValidArg(clientId)) {
                String level = (String) arguments.get("level");
                if (isValidArg(level)) {
                  String time = (String) arguments.get("time");
                  if (isValidArg(time)) {
                    String provider = (String) arguments.get("provider"); // can be null or non empty
                    if (isValidTerm(provider)) {
                      String prefix = (String) arguments.get("prefix"); // can be null or non empty
                      if (isValidTerm(prefix)) {
                        CallLog callLog = callLogs.getLog();
                        String data = validate((String) arguments.get("data"));

                        StringBuilder message = new StringBuilder();
                        if (provider != null) {
                          message.append(provider);
                        }
                        if (prefix != null) {
                          if (message.length() > 0) {
                            message.append('.');
                          }
                          message.append(prefix);
                        }
                        if (message.length() > 0) {
                          message.append(' ');
                        }
                        message.append(currentUserId);
                        message.append('-');
                        message.append(clientId);
                        message.append(' ');
                        message.append(data);
                        message.append(" -- ");
                        message.append(time);

                        if (ERROR_LEVEL.equals(level)) {
                          callLog.error(message.toString());
                        } else if (WARN_LEVEL.equals(level)) {
                          callLog.warn(message.toString());
                        } else if (INFO_LEVEL.equals(level)) {
                          callLog.info(message.toString());
                        } else if (DEBUG_LEVEL.equals(level)) {
                          callLog.debug(message.toString());
                        } else if (TRACE_LEVEL.equals(level)) {
                          callLog.trace(message.toString());
                        } else {
                          callLog.warn("Received not expected ");
                          caller.failure(ErrorInfo.clientError("Not expected request parameters: level").asJSON());
                        }
                      } else {
                        caller.failure(ErrorInfo.clientError("Wrong request parameters: prefix").asJSON());
                      }
                    } else {
                      caller.failure(ErrorInfo.clientError("Wrong request parameters: provider").asJSON());
                    }
                  } else {
                    caller.failure(ErrorInfo.clientError("Wrong request parameters: time").asJSON());
                  }
                } else {
                  caller.failure(ErrorInfo.clientError("Wrong request parameters: level").asJSON());
                }
              } else {
                caller.failure(ErrorInfo.clientError("Wrong request parameters: clientId").asJSON());
              }
              //
              // } else {
              // caller.failure(ErrorInfo.clientError("Container required").asJSON());
              // }
            } else {
              caller.failure(ErrorInfo.clientError("Unauthorized user").asJSON());
            }
          } catch (Throwable e) {
            LOG.error("Error processing call request from client " + session.getId() + " with data: " + data, e);
            caller.failure(ErrorInfo.serverError("Error processing call request: " + e.getMessage()).asJSON());
          }
        }
      }).start();
    }

    /**
     * Cleanup channel client.
     *
     * @param channelId the channel id
     * @param clientId the client id
     */
    void cleanupChannelClient(String channelId, String clientId) {
      if (channelId.startsWith(USER_SUBSCRIPTION_CHANNEL_NAME)) {
        // cleanup session stuff, note that disconnected session already unsubscribed and has not channels
        UserChannelContext context = userChannelContext.get(channelId);
        if (context != null) {
          context.removeClient(clientId);
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("<<< User call channel context not found for client: " + clientId + ", channel:" + channelId);
          }
        }
      }
    }
  }

  /**
   * Instantiates a new CometD interaction service for WebConferencing.
   *
   * @param sessionProviders the session providers
   * @param identityRegistry the identity registry
   * @param organization the organization
   * @param webConferencing the video calls
   * @param exoBayeux the exo bayeux
   * @param callLogs the call logs
   */
  public CometdWebConferencingService(SessionProviderService sessionProviders,
                                      IdentityRegistry identityRegistry,
                                      OrganizationService organization,
                                      WebConferencingService webConferencing,
                                      EXoContinuationBayeux exoBayeux,
                                      CallLogService callLogs) {
    this.sessionProviders = sessionProviders;
    this.identityRegistry = identityRegistry;
    this.organization = organization;
    this.webConferencing = webConferencing;
    this.exoBayeux = exoBayeux;
    this.callLogs = callLogs;
    this.service = new CallService();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    // instantiate processor after the eXo container start, to let start-dependent logic worked before us
    final AtomicReference<ServerAnnotationProcessor> processor = new AtomicReference<>();
    // need initiate process after Bayeux server starts
    exoBayeux.addLifeCycleListener(new LifeCycle.Listener() {
      @Override
      public void lifeCycleStarted(LifeCycle event) {
        ServerAnnotationProcessor p = new ServerAnnotationProcessor(exoBayeux);
        processor.set(p);
        p.process(service);
      }

      @Override
      public void lifeCycleStopped(LifeCycle event) {
        ServerAnnotationProcessor p = processor.get();
        if (p != null) {
          p.deprocess(service);
        }
      }

      @Override
      public void lifeCycleStarting(LifeCycle event) {
        // Nothing
      }

      @Override
      public void lifeCycleFailure(LifeCycle event, Throwable cause) {
        // Nothing
      }

      @Override
      public void lifeCycleStopping(LifeCycle event) {
        // Nothing
      }
    });

    if (PropertyManager.isDevelopping()) {
      // This listener not required for work, just for info during development
      exoBayeux.addListener(new BayeuxServer.SessionListener() {
        @Override
        public void sessionRemoved(ServerSession session, boolean timedout) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("sessionRemoved: " + session.getId() + " timedout:" + timedout + " channels: "
                + channelsAsString(session.getSubscriptions()));
          }
        }

        @Override
        public void sessionAdded(ServerSession session, ServerMessage message) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("sessionAdded: " + session.getId() + " channels: " + channelsAsString(session.getSubscriptions()));
          }
        }
      });
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    //
  }

  /**
   * Gets the cometd server path.
   *
   * @return the cometd server path
   */
  public String getCometdServerPath() {
    return new StringBuilder("/").append(exoBayeux.getCometdContextName()).append("/cometd").toString();
  }

  /**
   * Gets the user token.
   *
   * @param userId the user id
   * @return the user token
   */
  public String getUserToken(String userId) {
    return exoBayeux.getUserToken(userId);
  }

  /**
   * Current user id.
   *
   * @param message the message
   * @return the string, if user cannot be defined it will be {@link IdentityConstants#ANONIM}
   */
  protected String currentUserId(ServerMessage message) {
    if (message != null) {
      return (String) message.get("exoId");
    } else {
      // FIXME Should we rely at ConversationState current's here (it will not be set by something external in
      // Cometd thread) or only on message's exoId?
      ConversationState convo = ConversationState.getCurrent();
      if (convo != null) {
        return convo.getIdentity().getUserId();
      } else {
        return IdentityConstants.ANONIM;
      }
    }
  }

  /**
   * Channel user id. It's assumed an user channel {@link USER_SUBSCRIPTION_CHANNEL_NAME}.
   *
   * @param channelId the channel id
   * @return the string
   * @throws IndexOutOfBoundsException if channel id doesn't look like an user channel
   */
  protected String channelUserId(String channelId) throws IndexOutOfBoundsException {
    return channelId.substring(USER_SUBSCRIPTION_CHANNEL_NAME.length() + 1);
  }

  /**
   * Channels as string.
   *
   * @param channels the channels
   * @return the string
   */
  protected String channelsAsString(Set<ServerChannel> channels) {
    return channels.stream().map(c -> c.getId()).collect(Collectors.joining(", "));
  }

  /**
   * Call id.
   *
   * @param type the type
   * @param info the id
   * @return the string
   */
  protected String callId(String type, String info) {
    return new StringBuffer(type).append('/').append(info).toString();
  }

  /**
   * Checks if is valid term: <code>null</code>, or not empty and not longer of {@value #TERM_MAX_LENGTH}
   * bytes.
   *
   * @param term the term
   * @return true, if is valid term
   */
  protected boolean isValidTerm(String term) {
    return term == null || (term.length() > 0 && term.length() <= TERM_MAX_LENGTH);
  }

  /**
   * Checks if is valid argument: not null, not empty and not too long.
   *
   * @param argument the argument
   * @return true, if is valid arg
   * @see IdentityInfo#isValidId(String)
   */
  protected boolean isValidArg(String argument) {
    return isValidId(argument);
  }

}
