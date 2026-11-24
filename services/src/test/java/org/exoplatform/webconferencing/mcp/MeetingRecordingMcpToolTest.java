/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
package org.exoplatform.webconferencing.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.mcp.model.MeetingRecordingModel;
import org.exoplatform.webconferencing.mcp.model.MeetingRecordingsModel;
import org.exoplatform.webconferencing.mcp.model.RecordingChapterModel;
import org.exoplatform.webconferencing.mcp.model.RecordingOutlineModel;
import org.exoplatform.webconferencing.model.RecordingInfo;

import tools.jackson.databind.ObjectMapper;

/**
 * The {@code get_meeting_recordings} tool: the acting user comes from the
 * authenticated request, an empty answer carries its reason, and the tool is
 * declared as the read it is.
 */
@ExtendWith(MockitoExtension.class)
class MeetingRecordingMcpToolTest {

  private static final String    URL         = "https://host/jitsi/meet/g_42-abc?inviteId=x";

  private static final String    DEFINITIONS = "ai-tool-definitions.json";

  @Mock
  private WebConferencingService                 webConferencingService;

  @Mock
  private ObjectProvider<RecordingOutlineReader> outlineReaderProvider;

  @Mock
  private RecordingOutlineReader                 outlineReader;

  private MeetingRecordingMcpTool                tool;

  /**
   * The tool, with an authenticated request of john.
   */
  @BeforeEach
  void setUp() {
    tool = new MeetingRecordingMcpTool(webConferencingService, outlineReaderProvider);
    ConversationState.setCurrent(new ConversationState(new Identity("john")));
  }

  /**
   * Cleans the request's identity.
   */
  @AfterEach
  void tearDown() {
    ConversationState.setCurrent(null);
  }

  /**
   * The recordings are listed with the authenticated user's rights and the
   * range read from the arguments; a bare date ends at the end of that day.
   *
   * @throws Exception never
   */
  @Test
  void listsTheRecordingsAsTheAuthenticatedUser() throws Exception {
    RecordingInfo recording = new RecordingInfo("doc1", "ACME demo – 2026-10-23 10:05",
                                                Date.from(Instant.parse("2026-10-23T10:05:00Z")), null, 42, "video/mp4", 3482L);
    recording.setPreviewUrl("https://host/portal/dw/documents?documentPreviewId=doc1");
    when(webConferencingService.getCallRecordingsByConference(URL,
                                                              "john",
                                                              Date.from(Instant.parse("2026-10-23T09:00:00Z")),
                                                              Date.from(Instant.parse("2026-10-23T23:59:59.999Z"))))
                                                                                                                  .thenReturn(List.of(recording));

    MeetingRecordingsModel answer = tool.getMeetingRecordings(URL, "2026-10-23T09:00:00Z", "2026-10-23", null);

    assertNull(answer.reason());
    assertEquals(1, answer.recordings().size());
    assertEquals("doc1", answer.recordings().get(0).documentId());
    assertEquals("2026-10-23T10:05:00Z", answer.recordings().get(0).recordedDate());
    assertEquals(3482L, answer.recordings().get(0).durationSeconds());
    assertEquals("https://host/portal/dw/documents?documentPreviewId=doc1", answer.recordings().get(0).previewUrl());
  }

  /**
   * A meeting without a recording answers NO_RECORDING.
   *
   * @throws Exception never
   */
  @Test
  void anEmptyListSaysNoRecording() throws Exception {
    when(webConferencingService.getCallRecordingsByConference(eq(URL), eq("john"), isNull(), isNull())).thenReturn(List.of());

    MeetingRecordingsModel answer = tool.getMeetingRecordings(URL, null, null, null);

    assertTrue(answer.recordings().isEmpty());
    assertEquals(MeetingRecordingMcpTool.NO_RECORDING, answer.reason());
    verify(outlineReaderProvider, never()).getIfAvailable();
  }

  /**
   * Each recording comes with the chapters and the speakers of its transcript,
   * read with the identity of the user listing them.
   *
   * @throws Exception never
   */
  @Test
  void eachRecordingComesWithItsChaptersAndSpeakers() throws Exception {
    when(webConferencingService.getCallRecordingsByConference(eq(URL), eq("john"), isNull(), isNull()))
                                                                                                    .thenReturn(List.of(recording("doc1")));
    when(outlineReaderProvider.getIfAvailable()).thenReturn(outlineReader);
    when(outlineReader.getIdentityId("john")).thenReturn(7L);
    RecordingOutlineModel outline = new RecordingOutlineModel(List.of(new RecordingChapterModel(0, "00:00", "Welcome"),
                                                                      new RecordingChapterModel(1380, "23:00", "Pricing")),
                                                              List.of("John Smith", "Speaker 2"));
    when(outlineReader.readOutline("doc1", 7L)).thenReturn(outline);

    MeetingRecordingModel recording = tool.getMeetingRecordings(URL, null, null, null).recordings().get(0);

    assertEquals(outline.chapters(), recording.chapters());
    assertEquals(List.of("John Smith", "Speaker 2"), recording.speakers());
    String json = new ObjectMapper().writeValueAsString(recording);
    assertTrue(json.contains("\"chapters\":[{\"start\":0,\"time\":\"00:00\",\"title\":\"Welcome\"}"), json);
    assertTrue(json.contains("\"speakers\":[\"John Smith\",\"Speaker 2\"]"), json);
  }

  /**
   * Without the Documents add-on, or for a recording whose transcript has no
   * outline, the recording is listed as before: no chapters, no speakers, not
   * even empty ones.
   *
   * @throws Exception never
   */
  @Test
  void aRecordingWithoutOutlineIsListedAsBefore() throws Exception {
    when(webConferencingService.getCallRecordingsByConference(eq(URL), eq("john"), isNull(), isNull()))
                                                                                                    .thenReturn(List.of(recording("doc1")));

    MeetingRecordingModel withoutDocuments = tool.getMeetingRecordings(URL, null, null, null).recordings().get(0);

    when(outlineReaderProvider.getIfAvailable()).thenReturn(outlineReader);
    when(outlineReader.getIdentityId("john")).thenReturn(7L);
    when(outlineReader.readOutline("doc1", 7L)).thenReturn(RecordingOutlineModel.EMPTY);
    MeetingRecordingModel withoutOutline = tool.getMeetingRecordings(URL, null, null, null).recordings().get(0);

    for (MeetingRecordingModel recording : List.of(withoutDocuments, withoutOutline)) {
      String json = new ObjectMapper().writeValueAsString(recording);
      assertFalse(json.contains("chapters"), json);
      assertFalse(json.contains("speakers"), json);
      assertTrue(json.contains("\"documentId\":\"doc1\""), json);
    }
  }

  /**
   * Only the newest recordings are outlined, the older ones listed alone.
   *
   * @throws Exception never
   */
  @Test
  void onlyTheNewestRecordingsAreOutlined() throws Exception {
    List<RecordingInfo> found = new ArrayList<>();
    for (int i = 0; i < MeetingRecordingMcpTool.MAX_OUTLINED + 2; i++) {
      found.add(recording("doc" + i));
    }
    when(webConferencingService.getCallRecordingsByConference(eq(URL), eq("john"), isNull(), isNull())).thenReturn(found);
    when(outlineReaderProvider.getIfAvailable()).thenReturn(outlineReader);
    when(outlineReader.getIdentityId("john")).thenReturn(7L);
    RecordingOutlineModel outline = new RecordingOutlineModel(List.of(new RecordingChapterModel(0, "00:00", "Welcome")),
                                                              List.of());
    when(outlineReader.readOutline(anyString(), eq(7L))).thenReturn(outline);

    List<MeetingRecordingModel> recordings = tool.getMeetingRecordings(URL, null, null, null).recordings();

    assertEquals(MeetingRecordingMcpTool.MAX_OUTLINED + 2, recordings.size());
    verify(outlineReader, times(MeetingRecordingMcpTool.MAX_OUTLINED)).readOutline(anyString(), anyLong());
    assertEquals(outline.chapters(), recordings.get(MeetingRecordingMcpTool.MAX_OUTLINED - 1).chapters());
    verify(outlineReader, never()).readOutline("doc" + MeetingRecordingMcpTool.MAX_OUTLINED, 7L);
    assertTrue(recordings.get(MeetingRecordingMcpTool.MAX_OUTLINED).chapters().isEmpty());
  }

  /**
   * A user without an identity reads no outline: the Documents reads would
   * have no rights to apply.
   *
   * @throws Exception never
   */
  @Test
  void aUserWithoutIdentityReadsNoOutline() throws Exception {
    when(webConferencingService.getCallRecordingsByConference(eq(URL), eq("john"), isNull(), isNull()))
                                                                                                    .thenReturn(List.of(recording("doc1")));
    when(outlineReaderProvider.getIfAvailable()).thenReturn(outlineReader);
    when(outlineReader.getIdentityId("john")).thenReturn(0L);

    assertEquals(1, tool.getMeetingRecordings(URL, null, null, null).recordings().size());
    verify(outlineReader, never()).readOutline(anyString(), anyLong());
  }

  /**
   * A link of another provider answers OTHER_PROVIDER, an unknown call
   * NO_RECORDING.
   *
   * @throws Exception never
   */
  @Test
  void anUnknownLinkOrCallSaysWhy() throws Exception {
    when(webConferencingService.getCallRecordingsByConference(eq("https://teams/x"), anyString(), any(), any()))
                                                                                                            .thenThrow(new ObjectNotFoundException(WebConferencingService.RECORDINGS_OTHER_PROVIDER));
    when(webConferencingService.getCallRecordingsByConference(eq(URL), anyString(), any(), any()))
                                                                                                .thenThrow(new ObjectNotFoundException(WebConferencingService.RECORDINGS_CALL_NOT_FOUND));

    assertEquals(MeetingRecordingMcpTool.OTHER_PROVIDER, tool.getMeetingRecordings("https://teams/x", null, null, null).reason());
    assertEquals(MeetingRecordingMcpTool.NO_RECORDING, tool.getMeetingRecordings(URL, null, null, null).reason());
  }

  /**
   * A user who may not read the meeting's recordings is refused, never told
   * there are none.
   *
   * @throws Exception never
   */
  @Test
  void aRefusalIsNotAnEmptyList() throws Exception {
    when(webConferencingService.getCallRecordingsByConference(eq(URL), eq("john"), any(), any()))
                                                                                               .thenThrow(new IllegalAccessException(WebConferencingService.RECORDINGS_NOT_SPACE_MEMBER));

    assertThrows(IllegalAccessException.class, () -> tool.getMeetingRecordings(URL, null, null, null));
  }

  /**
   * A missing link or an unreadable date is refused before any lookup.
   *
   * @throws Exception never
   */
  @Test
  void invalidArgumentsAreRefused() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> tool.getMeetingRecordings(" ", null, null, null));
    assertThrows(IllegalArgumentException.class, () -> tool.getMeetingRecordings(URL, "yesterday", null, null));
    verify(webConferencingService, never()).getCallRecordingsByConference(anyString(), anyString(), any(), any());
  }

  /**
   * A relative date is not read: the refusal names the argument and its value,
   * the accepted formats with an example, and asks the model to resolve the
   * expression itself, instead of the REST layer's message code.
   */
  @Test
  void anUnreadableDateTellsTheModelHowToCallAgain() {
    String fromMessage = assertThrows(IllegalArgumentException.class,
                                      () -> tool.getMeetingRecordings(URL, "yesterday", null, null)).getMessage();
    assertEquals("Invalid 'recorded_from' value 'yesterday'. Provide an RFC3339 date-time (e.g. 2026-10-23T09:00:00Z) "
        + "or a date yyyy-MM-dd (e.g. 2026-10-23), in UTC. Resolve a relative expression such as 'yesterday' or "
        + "'last week' into such a date yourself before calling the tool again.", fromMessage);
    String toMessage = assertThrows(IllegalArgumentException.class,
                                    () -> tool.getMeetingRecordings(URL, "2026-10-22", "last week", null)).getMessage();
    assertTrue(toMessage.startsWith("Invalid 'recorded_to' value 'last week'."), toMessage);
  }

  /**
   * The accepted formats: an RFC3339 date-time with an offset, a date-time
   * without one read in UTC, and a bare date standing for the start of its day,
   * or for its end when it closes the range.
   *
   * @throws Exception never
   */
  @Test
  void theAcceptedDateFormatsAreRead() throws Exception {
    when(webConferencingService.getCallRecordingsByConference(any(), any(), any(), any())).thenReturn(List.of());

    tool.getMeetingRecordings(URL, "2026-10-22T09:00:00+02:00", "2026-10-22T10:00:00", null);
    verify(webConferencingService).getCallRecordingsByConference(URL,
                                                                 "john",
                                                                 Date.from(Instant.parse("2026-10-22T07:00:00Z")),
                                                                 Date.from(Instant.parse("2026-10-22T10:00:00Z")));

    tool.getMeetingRecordings(URL, "2026-10-22", "2026-10-22", null);
    verify(webConferencingService).getCallRecordingsByConference(URL,
                                                                 "john",
                                                                 Date.from(Instant.parse("2026-10-22T00:00:00Z")),
                                                                 Date.from(Instant.parse("2026-10-22T23:59:59.999Z")));
  }

  /**
   * The date arguments' descriptions name the accepted formats with an
   * example, and tell the model to resolve relative expressions itself.
   */
  @Test
  void theDateArgumentsDescribeTheirFormat() {
    Map<String, Object> definition = definitions().stream()
                                                  .filter(tool -> "get_meeting_recordings".equals(tool.get("name")))
                                                  .findFirst()
                                                  .orElseThrow();
    @SuppressWarnings("unchecked")
    Map<String, Map<String, Object>> properties =
                                                (Map<String, Map<String, Object>>) ((Map<String, Object>) definition.get("input_schema")).get("properties");
    for (String argument : List.of("recorded_from", "recorded_to")) {
      String description = (String) properties.get(argument).get("description");
      assertTrue(description.contains("RFC3339 date-time (2026-10-23T"), description);
      assertTrue(description.contains("yyyy-MM-dd (2026-10-23"), description);
      assertTrue(description.contains("resolve a relative expression such as 'yesterday'"), description);
    }
  }

  /**
   * Every public method of the tool has an entry in the definitions file, and
   * every entry a method: the server drops a tool without one silently.
   */
  @Test
  void everyToolHasADefinitionAndEveryDefinitionATool() {
    Set<String> declared = Stream.of(MeetingRecordingMcpTool.class.getDeclaredMethods())
                                 .filter(method -> Modifier.isPublic(method.getModifiers()))
                                 .filter(method -> !method.isSynthetic())
                                 .map(Method::getName)
                                 .map(MeetingRecordingMcpToolTest::toSnakeCase)
                                 .collect(Collectors.toCollection(TreeSet::new));
    Set<String> defined = definitions().stream()
                                       .map(definition -> (String) definition.get("name"))
                                       .collect(Collectors.toCollection(TreeSet::new));
    assertEquals(declared, defined);
  }

  /**
   * The tool is a read: no approval, read-only annotations, and its arguments
   * are the method's, none required: a link or several meetings.
   */
  @Test
  void theToolIsDeclaredAsARead() {
    Map<String, Object> definition = definitions().stream()
                                                  .filter(tool -> "get_meeting_recordings".equals(tool.get("name")))
                                                  .findFirst()
                                                  .orElseThrow();
    @SuppressWarnings("unchecked")
    Map<String, Object> annotations = (Map<String, Object>) definition.get("annotations");
    assertEquals(Boolean.TRUE, annotations.get("readOnlyHint"));
    assertEquals(Boolean.FALSE, annotations.get("destructiveHint"));
    assertTrue(definition.get("require_approval") == null || Boolean.FALSE.equals(definition.get("require_approval")));
    @SuppressWarnings("unchecked")
    Map<String, Object> schema = (Map<String, Object>) definition.get("input_schema");
    @SuppressWarnings("unchecked")
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    assertEquals(Set.of("conference_url", "recorded_from", "recorded_to", "meetings"), properties.keySet());
    // a link or several meetings: neither alone is required (EXO-95138)
    assertNull(schema.get("required"));
  }

  /**
   * The parameter names the server binds the arguments to survive
   * compilation: the module compiles with {@code -parameters}.
   *
   * @throws Exception never
   */
  @Test
  void parameterNamesAreKept() throws Exception {
    Method method = MeetingRecordingMcpTool.class.getMethod("getMeetingRecordings", String.class, String.class, String.class, List.class);
    assertEquals(List.of("conferenceUrl", "recordedFrom", "recordedTo", "meetings"),
                 Stream.of(method.getParameters()).map(java.lang.reflect.Parameter::getName).toList());
  }

  /**
   * EXO-95138: several meetings checked at once, each looked up as a single
   * meeting is, with the authenticated user's rights and its own window:
   * each comes back with its count, or the reason it has none, in the order
   * given, and no transcript outline is read. A meeting whose recordings the
   * user may not read, another user's, answers NO_RECORDING, as one with
   * none, never its existence. Mutants: the refusal propagated; the
   * windows not passed; the outline read.
   *
   * @throws Exception never
   */
  @Test
  void severalMeetingsAreCheckedAtOnceWithTheUsersRights() throws Exception {
    String other = "https://host/jitsi/meet/g_43-def";
    String someoneElses = "https://host/jitsi/meet/u_mary-private";
    when(webConferencingService.getCallRecordingsByConference(URL,
                                                              "john",
                                                              Date.from(Instant.parse("2026-11-16T09:00:00Z")),
                                                              Date.from(Instant.parse("2026-11-16T10:00:00Z"))))
                                                                                                                 .thenReturn(List.of(recording("d1"),
                                                                                                                                     recording("d2")));
    when(webConferencingService.getCallRecordingsByConference(eq(other), eq("john"), any(), any())).thenReturn(List.of());
    when(webConferencingService.getCallRecordingsByConference(eq("https://teams/x"), eq("john"), any(), any()))
                                                                                                         .thenThrow(new ObjectNotFoundException(WebConferencingService.RECORDINGS_OTHER_PROVIDER));
    when(webConferencingService.getCallRecordingsByConference(eq(someoneElses), eq("john"), any(), any()))
                                                                                                    .thenThrow(new IllegalAccessException(WebConferencingService.RECORDINGS_NOT_SPACE_MEMBER));

    MeetingRecordingsModel checked = tool.getMeetingRecordings(null,
                                                               null,
                                                               null,
                                                               List.of(Map.of("conference_url",
                                                                              URL,
                                                                              "recorded_from",
                                                                              "2026-11-16T09:00:00Z",
                                                                              "recorded_to",
                                                                              "2026-11-16T10:00:00Z"),
                                                                       Map.of("conference_url", other),
                                                                       Map.of("conference_url", "https://teams/x"),
                                                                       Map.of("conference_url", someoneElses)));

    assertNull(checked.recordings());
    assertEquals(List.of(2, 0, 0, 0), checked.meetings().stream().map(meeting -> meeting.recordingCount()).toList());
    assertEquals(Arrays.asList(null,
                               MeetingRecordingMcpTool.NO_RECORDING,
                               MeetingRecordingMcpTool.OTHER_PROVIDER,
                               MeetingRecordingMcpTool.NO_RECORDING),
                 checked.meetings().stream().map(meeting -> meeting.reason()).toList());
    assertEquals("2026-11-16T09:00:00Z", checked.meetings().get(0).recordedFrom());
    verify(outlineReaderProvider, never()).getIfAvailable();
  }

  /**
   * EXO-95138: with no authenticated user, several meetings are refused, as
   * a single one is by the lookup, never answered « no recording » each.
   * Every meeting is looked up as the request's user (the John case above).
   * Mutant: the identity guard removed, every refusal then reading as
   * NO_RECORDING.
   *
   * @throws Exception never
   */
  @Test
  void severalMeetingsWithNoAuthenticatedUserAreRefused() throws Exception {
    ConversationState.setCurrent(null);

    assertThrows(IllegalAccessException.class,
                 () -> tool.getMeetingRecordings(null, null, null, List.of(Map.of("conference_url", URL))));
    verify(webConferencingService, never()).getCallRecordingsByConference(any(), any(), any(), any());
  }

  /**
   * EXO-95138: a user the lookup finds no identity for is refused for
   * several meetings, as for one, never answered « no recording » each.
   * Mutant: the no-identity refusal read as another user's meeting.
   *
   * @throws Exception never
   */
  @Test
  void severalMeetingsForAUserWithNoIdentityAreRefused() throws Exception {
    when(webConferencingService.getCallRecordingsByConference(eq(URL), eq("john"), any(), any()))
                                                                                               .thenThrow(new IllegalAccessException(WebConferencingService.RECORDINGS_NO_IDENTITY));

    assertThrows(IllegalAccessException.class,
                 () -> tool.getMeetingRecordings(null, null, null, List.of(Map.of("conference_url", URL))));
  }

  /**
   * EXO-95138: more meetings than {@link MeetingRecordingMcpTool#MAX_MEETINGS},
   * a meeting with no link, or an unreadable date are refused before any
   * lookup, the refusal naming the meeting. Mutants: the cap removed; the
   * entry's link not checked.
   */
  @Test
  void invalidMeetingsAreRefusedBeforeAnyLookup() throws Exception {
    List<Object> tooMany = new ArrayList<>();
    for (int i = 0; i <= MeetingRecordingMcpTool.MAX_MEETINGS; i++) {
      tooMany.add(Map.of("conference_url", URL));
    }
    assertThrows(IllegalArgumentException.class, () -> tool.getMeetingRecordings(null, null, null, tooMany));
    String noLink = assertThrows(IllegalArgumentException.class,
                                 () -> tool.getMeetingRecordings(null, null, null, List.of(Map.of("recorded_from", "2026-11-16")))).getMessage();
    assertTrue(noLink.startsWith("meetings[0]"), noLink);
    String badDate = assertThrows(IllegalArgumentException.class,
                                  () -> tool.getMeetingRecordings(null,
                                                                  null,
                                                                  null,
                                                                  List.of(Map.of("conference_url", URL),
                                                                          Map.of("conference_url", URL, "recorded_from", "yesterday"))))
                                                                                                                                 .getMessage();
    assertTrue(badDate.startsWith("Invalid 'meetings[1].recorded_from' value 'yesterday'"), badDate);
    verify(webConferencingService, never()).getCallRecordingsByConference(anyString(), anyString(), any(), any());
  }

  /**
   * EXO-95138: the definition upgrade lists, for this tool, the digests of
   * the definitions it replaces and never the shipped one's, so that an
   * instance that saved an earlier text takes the new one at its restart.
   * Mutant: the upgrade file dropped from the jar's resources.
   *
   * @throws Exception never
   */
  @Test
  void theDefinitionUpgradeListsTheReplacedDefinitions() throws Exception {
    try (var stream = getClass().getClassLoader().getResourceAsStream("ai-tool-definition-upgrades.json")) {
      assertTrue(stream != null, "ai-tool-definition-upgrades.json is shipped");
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> upgrades = (List<Map<String, Object>>) new ObjectMapper().readValue(stream, Map.class).get("tools");
      Map<String, Object> upgrade = upgrades.stream().filter(tool -> "get_meeting_recordings".equals(tool.get("name"))).findFirst().orElseThrow();
      @SuppressWarnings("unchecked")
      List<String> replaced = (List<String>) upgrade.get("replaced_definitions");
      assertEquals(3, replaced.size());
      assertFalse(replaced.contains("7626b7ca6eefdf01af02f3a09edfe50f7b42bb15d455fee1226701d95d16d0d9"), "the shipped definition replaces nothing");
    }
  }

  /**
   * A recording of the meeting.
   *
   * @param documentId the id of its document
   * @return the recording
   */
  private static RecordingInfo recording(String documentId) {
    return new RecordingInfo(documentId, "ACME demo", Date.from(Instant.parse("2026-10-23T10:05:00Z")), 42, "video/mp4");
  }

  /**
   * Reads this add-on's definitions file.
   *
   * @return the tool entries
   */
  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> definitions() {
    try (var input = MeetingRecordingMcpToolTest.class.getClassLoader().getResourceAsStream(DEFINITIONS)) {
      Map<String, Object> root = new ObjectMapper().readValue(input, Map.class);
      return (List<Map<String, Object>>) root.get("tools");
    } catch (java.io.IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * The snake-case name the server gives a tool method.
   *
   * @param name the method name
   * @return its snake-case form
   */
  private static String toSnakeCase(String name) {
    return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
  }

}
