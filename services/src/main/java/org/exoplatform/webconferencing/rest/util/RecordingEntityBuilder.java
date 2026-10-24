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
package org.exoplatform.webconferencing.rest.util;

import org.exoplatform.webconferencing.model.RecordingInfo;
import org.exoplatform.webconferencing.rest.model.RecordingEntity;
import org.exoplatform.webconferencing.util.RecordingDateUtils;

/**
 * Maps the recordings of a call to their REST form.
 */
public final class RecordingEntityBuilder {

  /**
   * No instance: static helpers only.
   */
  private RecordingEntityBuilder() {
  }

  /**
   * Maps a recording to its REST form.
   *
   * @param recording the recording
   * @return its REST form
   */
  public static RecordingEntity toEntity(RecordingInfo recording) {
    return new RecordingEntity(recording.getDocumentId(),
                               recording.getTitle(),
                               RecordingDateUtils.formatDate(recording.getRecordedDate()),
                               RecordingDateUtils.formatDate(recording.getStartedDate()),
                               recording.getSizeBytes(),
                               recording.getMimeType(),
                               recording.getPreviewUrl());
  }

}
