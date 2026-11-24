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
package org.exoplatform.webconferencing.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Whether one meeting, a conference link within a time window, has
 * recordings the user can open (EXO-95138): the answer of
 * {@code get_meeting_recordings} for each meeting it is given at once, so
 * that the assistant finds which of several meetings was recorded before it
 * reads any transcript.
 *
 * @param conferenceUrl the meeting link, as given
 * @param recordedFrom the window's start, as given
 * @param recordedTo the window's end, as given
 * @param recordingCount how many recordings the user can open in that window
 * @param reason when there are none: {@code NO_RECORDING} or
 *          {@code OTHER_PROVIDER}; absent otherwise
 */
@JsonInclude(value = Include.NON_NULL)
public record MeetingRecordingCheckModel(@JsonProperty("conference_url") String conferenceUrl,
                                         @JsonProperty("recorded_from") String recordedFrom,
                                         @JsonProperty("recorded_to") String recordedTo,
                                         @JsonProperty("recording_count") int recordingCount,
                                         String reason) {
}
