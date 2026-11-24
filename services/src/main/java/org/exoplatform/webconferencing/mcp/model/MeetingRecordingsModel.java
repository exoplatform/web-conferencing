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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The recordings of a meeting, newest first, or the reason there are none;
 * or, when several meetings were given at once, whether each one has
 * recordings (EXO-95138).
 *
 * @param recordings the recordings the user can open, newest first; absent
 *          when several meetings were given
 * @param reason when the list is empty: {@code NO_RECORDING} or
 *          {@code OTHER_PROVIDER}; absent otherwise
 * @param meetings for each meeting given at once, in the order given,
 *          whether it has recordings; absent for a single meeting
 */
@JsonInclude(value = Include.NON_NULL)
public record MeetingRecordingsModel(List<MeetingRecordingModel> recordings, String reason, List<MeetingRecordingCheckModel> meetings) {

  /**
   * The recordings of a single meeting.
   *
   * @param recordings the recordings the user can open, newest first
   * @param reason when the list is empty: {@code NO_RECORDING} or
   *          {@code OTHER_PROVIDER}; absent otherwise
   */
  public MeetingRecordingsModel(List<MeetingRecordingModel> recordings, String reason) {
    this(recordings, reason, null);
  }

}
