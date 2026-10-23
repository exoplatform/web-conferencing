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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.webconferencing.WebConferencingService;
import org.exoplatform.webconferencing.mcp.model.MeetingRecordingModel;
import org.exoplatform.webconferencing.mcp.model.MeetingRecordingsModel;
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

  private final WebConferencingService webConferencingService;

  /**
   * Builds the tool.
   *
   * @param webConferencingService the service holding the recordings lookup
   */
  @Autowired
  public MeetingRecordingMcpTool(WebConferencingService webConferencingService) {
    this.webConferencingService = webConferencingService;
  }

  /**
   * Lists the recordings of the meeting behind a conference link that the
   * current user can open, newest first ({@code get_meeting_recordings}).
   *
   * @param conferenceUrl the meeting link, as {@code get_event_conference}
   *          returns it
   * @param recordedFrom the earliest recording date, ISO 8601, optional
   * @param recordedTo the end of the range, ISO 8601, optional: the server adds
   *          its grace, since a recording is saved after its meeting ends
   * @return the recordings, or an empty list with its reason
   * @throws IllegalAccessException when the user may not read the meeting's
   *           recordings: not a member of its space, or not a participant
   */
  public MeetingRecordingsModel getMeetingRecordings(String conferenceUrl,
                                                     String recordedFrom,
                                                     String recordedTo) throws IllegalAccessException {
    if (StringUtils.isBlank(conferenceUrl)) {
      throw new IllegalArgumentException("conference_url is mandatory: pass the url get_event_conference returns");
    }
    try {
      List<MeetingRecordingModel> recordings =
                                             webConferencingService.getCallRecordingsByConference(conferenceUrl,
                                                                                                  getCurrentUserName(),
                                                                                                  RecordingDateUtils.parseDate(recordedFrom,
                                                                                                                               false),
                                                                                                  RecordingDateUtils.parseDate(recordedTo,
                                                                                                                               true))
                                                                   .stream()
                                                                   .map(MeetingRecordingMcpTool::toModel)
                                                                   .toList();
      return new MeetingRecordingsModel(recordings, recordings.isEmpty() ? NO_RECORDING : null);
    } catch (ObjectNotFoundException e) {
      String reason = WebConferencingService.RECORDINGS_OTHER_PROVIDER.equals(e.getMessage()) ? OTHER_PROVIDER : NO_RECORDING;
      return new MeetingRecordingsModel(List.of(), reason);
    }
  }

  /**
   * Maps a recording to the tool's answer.
   *
   * @param recording the recording
   * @return its model
   */
  private static MeetingRecordingModel toModel(RecordingInfo recording) {
    return new MeetingRecordingModel(recording.getDocumentId(),
                                     recording.getTitle(),
                                     RecordingDateUtils.formatDate(recording.getRecordedDate()),
                                     recording.getSizeBytes(),
                                     recording.getMimeType(),
                                     recording.getPreviewUrl());
  }

}
