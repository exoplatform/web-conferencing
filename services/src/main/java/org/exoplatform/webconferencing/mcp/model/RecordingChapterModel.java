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

/**
 * A chapter of a recording's transcript, as the recordings tool lists it.
 *
 * @param start the moment the chapter starts, in whole seconds from the
 *          beginning of the recording
 * @param time the same moment as {@code mm:ss}, or {@code h:mm:ss} past an
 *          hour
 * @param title the chapter's title
 */
public record RecordingChapterModel(long start, String time, String title) {
}
