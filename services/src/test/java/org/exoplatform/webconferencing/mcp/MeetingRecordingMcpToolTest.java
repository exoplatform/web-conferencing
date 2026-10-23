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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
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

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.mcp.model.MeetingRecordingsModel;
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
  private WebConferencingService webConferencingService;

  private MeetingRecordingMcpTool tool;

  /**
   * The tool, with an authenticated request of john.
   */
  @BeforeEach
  void setUp() {
    tool = new MeetingRecordingMcpTool(webConferencingService);
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
                                                Date.from(Instant.parse("2026-10-23T10:05:00Z")), 42, "video/mp4");
    recording.setPreviewUrl("https://host/portal/dw/documents?documentPreviewId=doc1");
    when(webConferencingService.getCallRecordingsByConference(URL,
                                                              "john",
                                                              Date.from(Instant.parse("2026-10-23T09:00:00Z")),
                                                              Date.from(Instant.parse("2026-10-23T23:59:59.999Z"))))
                                                                                                                  .thenReturn(List.of(recording));

    MeetingRecordingsModel answer = tool.getMeetingRecordings(URL, "2026-10-23T09:00:00Z", "2026-10-23");

    assertNull(answer.reason());
    assertEquals(1, answer.recordings().size());
    assertEquals("doc1", answer.recordings().get(0).documentId());
    assertEquals("2026-10-23T10:05:00Z", answer.recordings().get(0).recordedDate());
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

    MeetingRecordingsModel answer = tool.getMeetingRecordings(URL, null, null);

    assertTrue(answer.recordings().isEmpty());
    assertEquals(MeetingRecordingMcpTool.NO_RECORDING, answer.reason());
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

    assertEquals(MeetingRecordingMcpTool.OTHER_PROVIDER, tool.getMeetingRecordings("https://teams/x", null, null).reason());
    assertEquals(MeetingRecordingMcpTool.NO_RECORDING, tool.getMeetingRecordings(URL, null, null).reason());
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

    assertThrows(IllegalAccessException.class, () -> tool.getMeetingRecordings(URL, null, null));
  }

  /**
   * A missing link or an unreadable date is refused before any lookup.
   *
   * @throws Exception never
   */
  @Test
  void invalidArgumentsAreRefused() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> tool.getMeetingRecordings(" ", null, null));
    assertThrows(IllegalArgumentException.class, () -> tool.getMeetingRecordings(URL, "yesterday", null));
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
                                      () -> tool.getMeetingRecordings(URL, "yesterday", null)).getMessage();
    assertEquals("Invalid 'recorded_from' value 'yesterday'. Provide an RFC3339 date-time (e.g. 2026-10-23T09:00:00Z) "
        + "or a date yyyy-MM-dd (e.g. 2026-10-23), in UTC. Resolve a relative expression such as 'yesterday' or "
        + "'last week' into such a date yourself before calling the tool again.", fromMessage);
    String toMessage = assertThrows(IllegalArgumentException.class,
                                    () -> tool.getMeetingRecordings(URL, "2026-10-22", "last week")).getMessage();
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

    tool.getMeetingRecordings(URL, "2026-10-22T09:00:00+02:00", "2026-10-22T10:00:00");
    verify(webConferencingService).getCallRecordingsByConference(URL,
                                                                 "john",
                                                                 Date.from(Instant.parse("2026-10-22T07:00:00Z")),
                                                                 Date.from(Instant.parse("2026-10-22T10:00:00Z")));

    tool.getMeetingRecordings(URL, "2026-10-22", "2026-10-22");
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
   * are the method's, the link mandatory.
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
    assertEquals(Set.of("conference_url", "recorded_from", "recorded_to"), properties.keySet());
    assertEquals(List.of("conference_url"), schema.get("required"));
  }

  /**
   * The parameter names the server binds the arguments to survive
   * compilation: the module compiles with {@code -parameters}.
   *
   * @throws Exception never
   */
  @Test
  void parameterNamesAreKept() throws Exception {
    Method method = MeetingRecordingMcpTool.class.getMethod("getMeetingRecordings", String.class, String.class, String.class);
    assertEquals(List.of("conferenceUrl", "recordedFrom", "recordedTo"),
                 Stream.of(method.getParameters()).map(java.lang.reflect.Parameter::getName).toList());
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
