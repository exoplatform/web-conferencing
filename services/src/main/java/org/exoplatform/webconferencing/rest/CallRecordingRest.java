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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.rest.model.RecordingEntity;
import org.exoplatform.webconferencing.rest.util.RecordingEntityBuilder;
import org.exoplatform.webconferencing.rest.util.RecordingsLookup;
import org.exoplatform.webconferencing.util.RecordingDateUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The recordings of a call, as the viewer can open them in Documents.
 * Served under {@code /webconferencing/rest/}.
 * <p>
 * Every refusal answers a JSON body {@code {"message": "<code>"}}, the code
 * being one of the {@code webconferencing.recordings.*} message codes of
 * {@link WebConferencingService} and {@link RecordingDateUtils}.
 */
@RestController
@Tag(name = "call-recordings", description = "The recordings of a call that the user can open")
public class CallRecordingRest {

  /** The prefix of the message codes a refusal may carry. */
  public static final String           CODE_PREFIX     = "webconferencing.recordings.";

  /** The message code of a request refused for a reason that has no code. */
  public static final String           INVALID_REQUEST = "webconferencing.recordings.invalidRequest";

  private static final Log             LOG             = ExoLogger.getLogger(CallRecordingRest.class);

  private final WebConferencingService webConferencingService;

  /**
   * Builds the resource.
   *
   * @param webConferencingService the service holding the recordings lookup
   */
  @Autowired
  public CallRecordingRest(WebConferencingService webConferencingService) {
    this.webConferencingService = webConferencingService;
  }

  /**
   * Lists the recordings of a call that the user can open, newest first.
   *
   * @param request the authenticated request
   * @param callId the call id
   * @param from the earliest recording date, ISO 8601
   * @param to the end of the range, ISO 8601: the server adds its grace
   * @return the recordings
   */
  @GetMapping("calls/{callId}/recordings")
  @Secured("users")
  @Operation(summary = "List the recordings of a call", method = "GET",
             description = "Answers the recordings of the call that the user can open in Documents, newest first."
                 + " The user must be a member of the space owning a space or space-event call, or a participant"
                 + " of any other call. The server adds its configured grace to the end of the range.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid call id or date"),
      @ApiResponse(responseCode = "403", description = "The user may not read the recordings of this call"),
      @ApiResponse(responseCode = "404", description = "Call not found"),
  })
  public ResponseEntity<?> getCallRecordings(HttpServletRequest request,
                                             @Parameter(description = "Call id") @PathVariable("callId") String callId,
                                             @Parameter(description = "Earliest recording date, ISO 8601") @RequestParam(name = "from", required = false) String from,
                                             @Parameter(description = "End of the range, ISO 8601") @RequestParam(name = "to", required = false) String to) {
    return answer(() -> webConferencingService.getCallRecordings(callId,
                                                                 request.getRemoteUser(),
                                                                 RecordingDateUtils.parseDate(from, false),
                                                                 RecordingDateUtils.parseDate(to, true)));
  }

  /**
   * Lists the recordings of the call a meeting link points to that the user
   * can open, newest first.
   *
   * @param request the authenticated request
   * @param url the meeting link, as an agenda event's conference stores it
   * @param from the earliest recording date, ISO 8601
   * @param to the end of the range, ISO 8601: the server adds its grace
   * @return the recordings
   */
  @GetMapping("recordings/byConference")
  @Secured("users")
  @Operation(summary = "List the recordings of the call behind a meeting link", method = "GET",
             description = "Reads the call id out of the link with the active providers, then answers as the call's"
                 + " recordings list does. A link no active provider recognises answers 404 with the code"
                 + " webconferencing.recordings.otherProvider.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Missing link or invalid date"),
      @ApiResponse(responseCode = "403", description = "The user may not read the recordings of this call"),
      @ApiResponse(responseCode = "404", description = "Call not found, or the link is held on another provider"),
  })
  public ResponseEntity<?> getRecordingsByConference(HttpServletRequest request,
                                                     @Parameter(description = "Meeting link") @RequestParam(name = "url", required = false) String url,
                                                     @Parameter(description = "Earliest recording date, ISO 8601") @RequestParam(name = "from", required = false) String from,
                                                     @Parameter(description = "End of the range, ISO 8601") @RequestParam(name = "to", required = false) String to) {
    return answer(() -> webConferencingService.getCallRecordingsByConference(url,
                                                                             request.getRemoteUser(),
                                                                             RecordingDateUtils.parseDate(from, false),
                                                                             RecordingDateUtils.parseDate(to, true)));
  }

  /**
   * Runs a lookup and maps its outcome to the response: the list, or the
   * refusal's status with its message code. Never cached: the list depends on
   * the viewer.
   *
   * @param lookup the lookup
   * @return the response
   */
  private ResponseEntity<?> answer(RecordingsLookup lookup) {
    try {
      List<RecordingEntity> recordings = lookup.get().stream().map(RecordingEntityBuilder::toEntity).toList();
      return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(recordings);
    } catch (ObjectNotFoundException e) {
      return refusal(HttpStatus.NOT_FOUND, e.getMessage());
    } catch (IllegalAccessException e) {
      return refusal(HttpStatus.FORBIDDEN, e.getMessage());
    } catch (IllegalArgumentException e) {
      LOG.debug("Recordings request refused", e);
      return refusal(HttpStatus.BAD_REQUEST, StringUtils.startsWith(e.getMessage(), CODE_PREFIX) ? e.getMessage() : INVALID_REQUEST);
    } catch (RuntimeException e) {
      LOG.warn("Cannot list the recordings of a call", e);
      return refusal(HttpStatus.INTERNAL_SERVER_ERROR, WebConferencingService.RECORDINGS_STORAGE_ERROR);
    }
  }

  /**
   * A refusal carrying its message code, never cached.
   *
   * @param status the status
   * @param code the message code
   * @return the response
   */
  private static ResponseEntity<Map<String, String>> refusal(HttpStatus status, String code) {
    return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("message", code));
  }

}
