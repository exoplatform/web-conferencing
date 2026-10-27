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

/**
 * What a recording's transcript holds before anyone reads it: its chapters
 * and the names of its speakers.
 *
 * @param chapters the chapters, in the order of the recording; empty when the
 *          transcript has none
 * @param speakers the speakers' names, in the order they first speak; empty
 *          when the transcript does not tell them apart
 */
public record RecordingOutlineModel(List<RecordingChapterModel> chapters, List<String> speakers) {

  /** The outline of a recording with no transcript, or none that can be read. */
  public static final RecordingOutlineModel EMPTY = new RecordingOutlineModel(List.of(), List.of());

}
