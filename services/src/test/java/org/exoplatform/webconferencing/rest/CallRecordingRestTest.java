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
package org.exoplatform.webconferencing.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.model.RecordingInfo;
import org.exoplatform.webconferencing.rest.model.RecordingEntity;
import org.exoplatform.webconferencing.util.RecordingDateUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The recordings REST: the viewer is the authenticated request's, and each
 * refusal maps to its status with its message code, never an engine's text.
 */
@ExtendWith(MockitoExtension.class)
class CallRecordingRestTest {

  private static final String    CALL_ID = "g_42-abc";

  @Mock
  private WebConferencingService webConferencingService;

  @Mock
  private HttpServletRequest     request;

  private CallRecordingRest      rest;

  /**
   * The resource, with an authenticated request of john.
   */
  @BeforeEach
  void setUp() {
    rest = new CallRecordingRest(webConferencingService);
    when(request.getRemoteUser()).thenReturn("john");
  }

  /**
   * The list is answered newest first as the service gives it, never cached.
   *
   * @throws Exception never
   */
  @Test
  void answersTheRecordingsOfTheAuthenticatedUser() throws Exception {
    RecordingInfo recording = new RecordingInfo("doc1", "ACME demo", Date.from(Instant.parse("2026-10-23T10:05:00Z")), null, 42,
                                                "video/mp4", 3482L);
    recording.setPreviewUrl("preview");
    when(webConferencingService.getCallRecordings(CALL_ID,
                                                  "john",
                                                  Date.from(Instant.parse("2026-10-23T09:00:00Z")),
                                                  Date.from(Instant.parse("2026-10-23T10:00:00Z"))))
                                                                                                  .thenReturn(List.of(recording));

    ResponseEntity<?> response = rest.getCallRecordings(request, CALL_ID, "2026-10-23T09:00:00Z", "2026-10-23T11:00:00+01:00");

    assertEquals(200, response.getStatusCode().value());
    assertEquals(List.of(new RecordingEntity("doc1", "ACME demo", "2026-10-23T10:05:00Z", null, 42, "video/mp4", 3482L, "preview")),
                 response.getBody());
    assertEquals("no-store", response.getHeaders().getCacheControl());
  }

  /**
   * The lookup by meeting link answers the same list.
   *
   * @throws Exception never
   */
  @Test
  void answersTheRecordingsOfAMeetingLink() throws Exception {
    when(webConferencingService.getCallRecordingsByConference(eq("https://host/jitsi/meet/" + CALL_ID), eq("john"), isNull(), isNull()))
                                                                                                                                    .thenReturn(List.of());

    ResponseEntity<?> response = rest.getRecordingsByConference(request, "https://host/jitsi/meet/" + CALL_ID, null, null);

    assertEquals(200, response.getStatusCode().value());
    assertEquals(List.of(), response.getBody());
  }

  /**
   * An unknown call or another provider is a 404, a refused viewer a 403,
   * each with its code.
   *
   * @throws Exception never
   */
  @Test
  void refusalsMapToTheirStatus() throws Exception {
    when(webConferencingService.getCallRecordings(eq("unknown"), anyString(), any(), any()))
                                                                                         .thenThrow(new ObjectNotFoundException(WebConferencingService.RECORDINGS_CALL_NOT_FOUND));
    when(webConferencingService.getCallRecordings(eq(CALL_ID), anyString(), any(), any()))
                                                                                        .thenThrow(new IllegalAccessException(WebConferencingService.RECORDINGS_NOT_SPACE_MEMBER));
    when(webConferencingService.getCallRecordingsByConference(eq("https://teams/x"), anyString(), any(), any()))
                                                                                                            .thenThrow(new ObjectNotFoundException(WebConferencingService.RECORDINGS_OTHER_PROVIDER));

    assertRefusal(rest.getCallRecordings(request, "unknown", null, null), 404, WebConferencingService.RECORDINGS_CALL_NOT_FOUND);
    assertRefusal(rest.getCallRecordings(request, CALL_ID, null, null), 403, WebConferencingService.RECORDINGS_NOT_SPACE_MEMBER);
    assertRefusal(rest.getRecordingsByConference(request, "https://teams/x", null, null),
                  404,
                  WebConferencingService.RECORDINGS_OTHER_PROVIDER);
  }

  /**
   * An unreadable date is a 400 with its code, before any lookup; an
   * argument refused with a text that is not a code answers the generic code.
   *
   * @throws Exception never
   */
  @Test
  void badRequestsCarryACode() throws Exception {
    assertRefusal(rest.getCallRecordings(request, CALL_ID, "yesterday", null), 400, RecordingDateUtils.RECORDINGS_INVALID_DATE);
    verify(webConferencingService, never()).getCallRecordings(anyString(), anyString(), any(), any());

    when(webConferencingService.getCallRecordings(eq("x"), anyString(), any(), any()))
                                                                                    .thenThrow(new IllegalArgumentException("/Groups/spaces/sales is broken"));
    assertRefusal(rest.getCallRecordings(request, "x", null, null), 400, CallRecordingRest.INVALID_REQUEST);
  }

  /**
   * A repository failure is a 500 with a fixed code.
   *
   * @throws Exception never
   */
  @Test
  void aStorageFailureIsAFixedServerError() throws Exception {
    when(webConferencingService.getCallRecordings(eq(CALL_ID), anyString(), any(), any()))
                                                                                        .thenThrow(new IllegalStateException(WebConferencingService.RECORDINGS_STORAGE_ERROR));

    assertRefusal(rest.getCallRecordings(request, CALL_ID, null, null), 500, WebConferencingService.RECORDINGS_STORAGE_ERROR);
  }

  /**
   * Checks a refusal's status and code.
   *
   * @param response the response
   * @param status the expected status
   * @param code the expected message code
   */
  private static void assertRefusal(ResponseEntity<?> response, int status, String code) {
    assertEquals(status, response.getStatusCode().value());
    assertEquals(Map.of("message", code), response.getBody());
  }

}
