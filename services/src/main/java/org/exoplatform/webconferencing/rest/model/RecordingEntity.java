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
package org.exoplatform.webconferencing.rest.model;

/**
 * A recording of a call, as the recordings REST answers it.
 *
 * @param documentId the JCR identifier of the recording's file, the id the
 *          documents add-on and its transcription REST take
 * @param title the title displayed in Documents
 * @param recordedDate when the recording was saved, an ISO 8601 instant in UTC
 * @param startedDate when the recording started, an ISO 8601 instant in UTC,
 *          or null when its file name does not tell it
 * @param sizeBytes the size of the recording in bytes, -1 when unknown
 * @param mimeType the media type of the recording
 * @param durationSeconds how long the recording lasts, in seconds, or null
 *          when its file does not tell it
 * @param previewUrl the link opening the recording in the Documents preview
 */
public record RecordingEntity(String documentId,
                              String title,
                              String recordedDate,
                              String startedDate,
                              long sizeBytes,
                              String mimeType,
                              Long durationSeconds,
                              String previewUrl) {
}
