/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.webconferencing.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webconferencing.CallInfo;
import org.exoplatform.webconferencing.GuestInfo;
import org.exoplatform.webconferencing.UserInfo;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.notification.utils.NotificationConstants;

/**
 * The recording notification names the meeting and its start, and a space or
 * space-event recording, saved in the space's Documents, is announced only to
 * the participants who are members of that space; chat-room and one-to-one
 * recordings, shared with each participant, keep notifying every participant.
 */
@ExtendWith(MockitoExtension.class)
class CallRecordingNotificationListenerTest {

  private static final String                 DOMAIN          = "http://localhost:8080";

  private static final String                 SPACE_IDENTITY  = "12";

  private static final String                 FILE_NAME       = "g_1234_20261022.mp4";

  private static final long                   LAST_DATE       = 1_792_764_000_000L;

  private static final long                   PLANNED_DATE    = 1_792_760_400_000L;

  @Mock
  private IdentityManager                     identityManager;

  @Mock
  private WebConferencingService              webConferencingService;

  @Mock
  private SpaceService                        spaceService;

  private MockedStatic<CommonsUtils>          commonsUtilsStatic;

  private final List<NotificationContext>     contexts        = new ArrayList<>();

  private CallRecordingNotificationListener   listener;

  /**
   * Hands out a fresh mocked notification context per notification (the real
   * one needs a running container), and a fixed current domain.
   */
  @BeforeEach
  void setUp() {
    commonsUtilsStatic = Mockito.mockStatic(CommonsUtils.class);
    commonsUtilsStatic.when(CommonsUtils::getCurrentDomain).thenReturn(DOMAIN);
    listener = new CallRecordingNotificationListener(identityManager, webConferencingService, spaceService) {
      @Override
      protected NotificationContext newNotificationContext() {
        NotificationContext ctx = mock(NotificationContext.class, RETURNS_DEEP_STUBS);
        contexts.add(ctx);
        return ctx;
      }
    };
  }

  /**
   * Releases the static mock.
   */
  @AfterEach
  void tearDown() {
    commonsUtilsStatic.close();
  }

  /**
   * A space-event recording is announced to the members of the space only: the
   * attendee invited directly and the guest, who cannot open the space's
   * Documents, are left out; the notification carries the event's title and the
   * moment the recorded session started.
   *
   * @throws Exception never
   */
  @Test
  void spaceEventRecordingNotifiesOnlySpaceMembers() throws Exception {
    Space space = mockSpaceEvent();
    when(spaceService.isMember(space, "alice")).thenReturn(true);
    when(spaceService.isMember(space, "bob")).thenReturn(false);
    when(spaceService.isMember(space, "guest@acme.com")).thenReturn(false);
    when(spaceService.isMember(space, "carol")).thenReturn(true);
    CallInfo call = call("Demo ACME", "alice", "bob", "guest@acme.com", "carol");
    call.setLastDate(new Date(LAST_DATE));
    call.setStartDate(new Date(PLANNED_DATE));

    listener.onEvent(recordedEvent(call, WebConferencingService.OWNER_TYPE_SPACEEVENT, SPACE_IDENTITY));

    assertEquals(1, contexts.size());
    NotificationContext ctx = contexts.get(0);
    assertEquals(List.of("alice", "carol"), appended(ctx, NotificationConstants.CALL_PARTICIPANTS));
    assertEquals("Demo ACME", appended(ctx, NotificationConstants.CALL_TITLE));
    assertEquals(String.valueOf(LAST_DATE), appended(ctx, NotificationConstants.CALL_START_DATE));
    assertEquals("Sales team", appended(ctx, NotificationConstants.CALL_OWNER));
  }

  /**
   * When no participant of a space-event call is a member of the space, nobody
   * is notified, rather than a notification with no recipient.
   *
   * @throws Exception never
   */
  @Test
  void spaceEventRecordingWithoutMemberParticipantNotifiesNobody() throws Exception {
    Space space = mockSpaceEvent();
    when(spaceService.isMember(space, "bob")).thenReturn(false);
    CallInfo call = call("Demo ACME", "bob");

    listener.onEvent(recordedEvent(call, WebConferencingService.OWNER_TYPE_SPACEEVENT, SPACE_IDENTITY));

    assertTrue(contexts.isEmpty());
    verify(webConferencingService, never()).getRecordingUrl(anyString(), anyString(), anyString());
  }

  /**
   * A space call's recording is filtered the same way; with no call title, the
   * space's name stands for the meeting, and with no session start the planned
   * start is used.
   *
   * @throws Exception never
   */
  @Test
  void spaceRecordingNotifiesOnlySpaceMembersAndFallsBackToSpaceName() throws Exception {
    Space space = new Space();
    space.setPrettyName("sales_team");
    space.setDisplayName("Sales team");
    when(spaceService.getSpaceByPrettyName("sales_team")).thenReturn(space);
    when(spaceService.isMember(space, "alice")).thenReturn(true);
    when(spaceService.isMember(space, "bob")).thenReturn(false);
    when(webConferencingService.getRecordingUrl("sales_team", FILE_NAME, WebConferencingService.OWNER_TYPE_SPACE))
                                                                                                          .thenReturn(DOMAIN + "/portal/dw/documents?documentPreviewId=1");
    CallInfo call = call(" ", "bob", "alice");
    call.setStartDate(new Date(PLANNED_DATE));

    listener.onEvent(recordedEvent(call, WebConferencingService.OWNER_TYPE_SPACE, "sales_team"));

    assertEquals(1, contexts.size());
    NotificationContext ctx = contexts.get(0);
    assertEquals(List.of("alice"), appended(ctx, NotificationConstants.CALL_PARTICIPANTS));
    assertEquals("Sales team", appended(ctx, NotificationConstants.CALL_TITLE));
    assertEquals(String.valueOf(PLANNED_DATE), appended(ctx, NotificationConstants.CALL_START_DATE));
    assertEquals("/portal/dw/documents?documentPreviewId=1", appended(ctx, NotificationConstants.RECORDED_FILE_URL));
  }

  /**
   * A chat-room recording is shared with each participant, so each one is
   * notified, member of a space or not, with the room's name as the meeting's.
   *
   * @throws Exception never
   */
  @Test
  void chatRoomRecordingNotifiesEveryParticipant() throws Exception {
    when(webConferencingService.getRecordingUrl(anyString(), eq(FILE_NAME), eq(WebConferencingService.OWNER_TYPE_CHATROOM)))
                                                                                                                       .thenReturn(DOMAIN + "/doc");
    CallInfo call = call("Weekly sync", "alice", "bob");
    call.setLastDate(new Date(LAST_DATE));

    listener.onEvent(recordedEvent(call, WebConferencingService.OWNER_TYPE_CHATROOM, "room1"));

    assertEquals(2, contexts.size());
    assertEquals(List.of("alice"), appended(contexts.get(0), NotificationConstants.CALL_PARTICIPANTS));
    assertEquals(List.of("bob"), appended(contexts.get(1), NotificationConstants.CALL_PARTICIPANTS));
    assertEquals("Weekly sync", appended(contexts.get(1), NotificationConstants.CALL_TITLE));
    assertEquals(String.valueOf(LAST_DATE), appended(contexts.get(1), NotificationConstants.CALL_START_DATE));
    verify(spaceService, never()).isMember(any(Space.class), anyString());
  }

  /**
   * A one-to-one recording names the conversation by the call owner's full
   * name, for every recipient, as the owner already did, and notifies each
   * participant.
   *
   * @throws Exception never
   */
  @Test
  void oneToOneRecordingNamesTheConversation() throws Exception {
    Identity identity = new Identity(OrganizationIdentityProvider.NAME, "alice");
    Profile profile = new Profile(identity);
    profile.setProperty(Profile.FULL_NAME, "Alice Martin");
    identity.setProfile(profile);
    when(identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "alice")).thenReturn(identity);
    when(webConferencingService.getRecordingUrl(anyString(), eq(FILE_NAME), eq(WebConferencingService.OWNER_TYPE_USER)))
                                                                                                                   .thenReturn(DOMAIN + "/doc");
    CallInfo call = call("alice", "alice", "bob");

    listener.onEvent(recordedEvent(call, WebConferencingService.OWNER_TYPE_USER, "alice"));

    assertEquals(2, contexts.size());
    assertEquals(appended(contexts.get(0), NotificationConstants.CALL_OWNER),
                 appended(contexts.get(0), NotificationConstants.CALL_TITLE));
    assertEquals(null, appended(contexts.get(0), NotificationConstants.CALL_START_DATE));
    verify(spaceService, never()).isMember(any(Space.class), anyString());
  }

  /**
   * A recorded event without a call (the call could not be loaded) sends
   * nothing instead of failing.
   *
   * @throws Exception never
   */
  @Test
  void recordingWithoutCallNotifiesNobody() throws Exception {
    listener.onEvent(recordedEvent(null, WebConferencingService.OWNER_TYPE_SPACEEVENT, SPACE_IDENTITY));

    assertTrue(contexts.isEmpty());
  }

  /**
   * Stubs the space-event resolution: the space identity, its space and the
   * recording URL.
   *
   * @return the space of the event
   * @throws Exception never
   */
  private Space mockSpaceEvent() throws Exception {
    Identity spaceIdentity = new Identity("space", "sales_team");
    when(identityManager.getIdentity(SPACE_IDENTITY)).thenReturn(spaceIdentity);
    Space space = new Space();
    space.setPrettyName("sales_team");
    space.setDisplayName("Sales team");
    when(spaceService.getSpaceByPrettyName("sales_team")).thenReturn(space);
    Mockito.lenient()
           .when(webConferencingService.getRecordingUrl(SPACE_IDENTITY, FILE_NAME, WebConferencingService.OWNER_TYPE_SPACEEVENT))
           .thenReturn(DOMAIN + "/portal/dw/documents?documentPreviewId=1");
    return space;
  }

  /**
   * Builds a call with the given title and participants; an id containing
   * {@code @} is a guest.
   *
   * @param title the call's title
   * @param participantIds the participants' ids
   * @return the call
   */
  private CallInfo call(String title, String... participantIds) {
    CallInfo call = new CallInfo("g_1234", title, null, "jitsi");
    for (String participantId : participantIds) {
      call.addParticipant(participantId.contains("@") ? new GuestInfo(participantId)
                                                      : new UserInfo(participantId, participantId, participantId));
    }
    return call;
  }

  /**
   * Builds the event {@code uploadFile} broadcasts once a recording is saved.
   *
   * @param call the recorded call
   * @param type the owner type of the upload
   * @param identity the owner identity of the upload
   * @return the event
   */
  private Event<CallInfo, Map<? extends String, ? extends String>> recordedEvent(CallInfo call, String type, String identity) {
    Map<String, String> info = new HashMap<>();
    info.put("status", "ok");
    info.put("file_name", FILE_NAME);
    info.put("type", type);
    info.put("identity", identity);
    return new Event<>(WebConferencingService.EVENT_CALL_RECORDED, call, info);
  }

  /**
   * Reads the value the listener appended to a notification context for an
   * argument.
   *
   * @param ctx the notification context
   * @param argument the argument
   * @return the appended value, or {@code null} when it was appended as null
   */
  private Object appended(NotificationContext ctx, ArgumentLiteral<?> argument) {
    // ArgumentLiteral.equals compares the value type only, so the argument is matched by its key
    ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
    verify(ctx).append(argThat(literal -> literal != null && argument.getKey().equals(literal.getKey())), captor.capture());
    return captor.getValue();
  }
}
