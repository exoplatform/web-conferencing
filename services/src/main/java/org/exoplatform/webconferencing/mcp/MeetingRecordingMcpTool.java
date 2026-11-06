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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.mcp.model.MeetingRecordingModel;
import org.exoplatform.webconferencing.mcp.model.MeetingRecordingsModel;
import org.exoplatform.webconferencing.mcp.model.RecordingOutlineModel;
import org.exoplatform.webconferencing.model.RecordingInfo;
import org.exoplatform.webconferencing.util.RecordingDateUtils;

import io.meeds.mcp.server.plugin.McpToolPlugin;

/**
 * The assistant tools of the web conferencing add-on: one public method per
 * tool, named in snake case, each described in this add-on's
 * {@code ai-tool-definitions.json}.
 * <p>
 * The acting user is the authenticated request's, never an argument: the
 * recordings come back with that user's own rights.
 */
@Service
@Profile("mcp-server")
public class MeetingRecordingMcpTool implements McpToolPlugin {

  /** The reason of an empty list: the meeting was not recorded, or not by this platform. */
  public static final String           NO_RECORDING   = "NO_RECORDING";

  /** The reason of an empty list: the link is not one of an active provider's meetings. */
  public static final String           OTHER_PROVIDER = "OTHER_PROVIDER";

  /**
   * The answer to a date argument that cannot be read: it names the argument,
   * the value, the accepted formats with an example, and asks the model to
   * resolve a relative expression itself.
   */
  public static final String           INVALID_DATE   =
                                                    "Invalid '%s' value '%s'. Provide an RFC3339 date-time (e.g. 2026-10-23T09:00:00Z) "
                                                        + "or a date yyyy-MM-dd (e.g. 2026-10-23), in UTC. Resolve a relative "
                                                        + "expression such as 'yesterday' or 'last week' into such a date yourself "
                                                        + "before calling the tool again.";

  /**
   * How many recordings, the newest, are listed with their chapters and
   * speakers: reading them costs one read of each transcript, and the assistant
   * narrows an older recording down by date anyway.
   */
  public static final int              MAX_OUTLINED   = 10;

  private final WebConferencingService webConferencingService;

  private final ObjectProvider<RecordingOutlineReader> outlineReader;

  /**
   * Builds the tool.
   *
   * @param webConferencingService the service holding the recordings lookup
   * @param outlineReader the reader of the recordings' chapters and speakers,
   *          absent without the Documents add-on
   */
  @Autowired
  public MeetingRecordingMcpTool(WebConferencingService webConferencingService,
                                 ObjectProvider<RecordingOutlineReader> outlineReader) {
    this.webConferencingService = webConferencingService;
    this.outlineReader = outlineReader;
  }

  /**
   * Lists the recordings of the meeting behind a conference link that the
   * current user can open, newest first ({@code get_meeting_recordings}). The
   * {@link #MAX_OUTLINED} newest come with the chapters of their transcript and
   * the names of its speakers, read with the same user's rights, so that the
   * assistant picks a recording and a chapter before reading any transcript.
   *
   * @param conferenceUrl the meeting link, as {@code get_event_conference}
   *          returns it
   * @param recordedFrom the earliest recording date, RFC3339 or
   *          {@code yyyy-MM-dd}, optional
   * @param recordedTo the end of the range, RFC3339 or {@code yyyy-MM-dd},
   *          optional: the server adds its grace, since a recording is saved
   *          after its meeting ends
   * @return the recordings, or an empty list with its reason
   * @throws IllegalArgumentException when the link is missing, or a date is not
   *           in an accepted format: its message, {@link #INVALID_DATE}, tells
   *           the model how to call again
   * @throws IllegalAccessException when the user may not read the meeting's
   *           recordings: not a member of its space, or not a participant
   */
  public MeetingRecordingsModel getMeetingRecordings(String conferenceUrl,
                                                     String recordedFrom,
                                                     String recordedTo) throws IllegalAccessException {
    if (StringUtils.isBlank(conferenceUrl)) {
      throw new IllegalArgumentException("conference_url is mandatory: pass the url get_event_conference returns");
    }
    Date from = parseDateArgument("recorded_from", recordedFrom, false);
    Date to = parseDateArgument("recorded_to", recordedTo, true);
    try {
      String username = getCurrentUserName();
      List<RecordingInfo> found = webConferencingService.getCallRecordingsByConference(conferenceUrl, username, from, to);
      RecordingOutlineReader reader = found.isEmpty() ? null : outlineReader.getIfAvailable();
      long identityId = reader == null ? 0 : reader.getIdentityId(username);
      List<MeetingRecordingModel> recordings = new ArrayList<>(found.size());
      for (RecordingInfo recording : found) {
        boolean outlined = identityId > 0 && recordings.size() < MAX_OUTLINED;
        RecordingOutlineModel outline = outlined ? reader.readOutline(recording.getDocumentId(), identityId)
                                                 : RecordingOutlineModel.EMPTY;
        recordings.add(toModel(recording, outline));
      }
      return new MeetingRecordingsModel(recordings, recordings.isEmpty() ? NO_RECORDING : null);
    } catch (ObjectNotFoundException e) {
      String reason = WebConferencingService.RECORDINGS_OTHER_PROVIDER.equals(e.getMessage()) ? OTHER_PROVIDER : NO_RECORDING;
      return new MeetingRecordingsModel(List.of(), reason);
    }
  }

  /**
   * Reads a date argument of the tool. An unreadable value is refused with a
   * message the model can act on, the shape the platform's own tools use for
   * bad input, rather than the REST layer's message code.
   *
   * @param name the argument's name, as the model sends it
   * @param value the argument's value, or a blank value for none
   * @param endOfRange true when the date closes the range
   * @return the date, or <code>null</code> when the value is blank
   * @throws IllegalArgumentException with {@link #INVALID_DATE} when the value
   *           is not in an accepted format
   */
  private static Date parseDateArgument(String name, String value, boolean endOfRange) {
    try {
      return RecordingDateUtils.parseDate(value, endOfRange);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(INVALID_DATE.formatted(name, value), e);
    }
  }

  /**
   * Maps a recording to the tool's answer.
   *
   * @param recording the recording
   * @param outline the chapters and speakers of its transcript
   * @return its model
   */
  private static MeetingRecordingModel toModel(RecordingInfo recording, RecordingOutlineModel outline) {
    return new MeetingRecordingModel(recording.getDocumentId(),
                                     recording.getTitle(),
                                     RecordingDateUtils.formatDate(recording.getRecordedDate()),
                                     RecordingDateUtils.formatDate(recording.getStartedDate()),
                                     recording.getSizeBytes(),
                                     recording.getMimeType(),
                                     recording.getDurationSeconds(),
                                     recording.getPreviewUrl(),
                                     outline.chapters(),
                                     outline.speakers());
  }

}
