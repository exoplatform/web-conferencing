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
package org.exoplatform.webconferencing.model;

import java.util.Date;

/**
 * A recording of a call, as the viewer who asked for it can open it in
 * Documents. The transcript state is not carried here: it belongs to the
 * documents add-on, which the client asks per recording.
 */
public class RecordingInfo {

  /** The JCR identifier of the recording's file. */
  private final String documentId;

  /** The title displayed in Documents. */
  private final String title;

  /** When the recording was saved. */
  private final Date   recordedDate;

  /** When the recording started, as its file name tells, or null. */
  private final Date   startedDate;

  /** The size of the recording, in bytes, or -1 when unknown. */
  private final long   sizeBytes;

  /** The media type of the recording. */
  private final String mimeType;

  /** The link opening the recording in the Documents preview. */
  private String       previewUrl;

  /**
   * Builds a recording.
   *
   * @param documentId the JCR identifier of the recording's file
   * @param title the title displayed in Documents
   * @param recordedDate when the recording was saved
   * @param sizeBytes the size of the recording, in bytes, or -1 when unknown
   * @param mimeType the media type of the recording
   */
  public RecordingInfo(String documentId, String title, Date recordedDate, long sizeBytes, String mimeType) {
    this(documentId, title, recordedDate, null, sizeBytes, mimeType);
  }

  /**
   * Builds a recording that knows when it started.
   *
   * @param documentId the JCR identifier of the recording's file
   * @param title the title displayed in Documents
   * @param recordedDate when the recording was saved
   * @param startedDate when the recording started, or null when unknown
   * @param sizeBytes the size of the recording, in bytes, or -1 when unknown
   * @param mimeType the media type of the recording
   */
  public RecordingInfo(String documentId,
                       String title,
                       Date recordedDate,
                       Date startedDate,
                       long sizeBytes,
                       String mimeType) {
    this.documentId = documentId;
    this.title = title;
    this.recordedDate = recordedDate == null ? null : new Date(recordedDate.getTime());
    this.startedDate = startedDate == null ? null : new Date(startedDate.getTime());
    this.sizeBytes = sizeBytes;
    this.mimeType = mimeType;
  }

  /**
   * Gets the JCR identifier of the recording's file.
   *
   * @return the document id
   */
  public String getDocumentId() {
    return documentId;
  }

  /**
   * Gets the title displayed in Documents.
   *
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets when the recording was saved.
   *
   * @return a copy of the recorded date, or <code>null</code> when unknown
   */
  public Date getRecordedDate() {
    return recordedDate == null ? null : new Date(recordedDate.getTime());
  }

  /**
   * Gets when the recording started, as the recorder named its file.
   *
   * @return the start, or null when the file name does not tell it
   */
  public Date getStartedDate() {
    return startedDate == null ? null : new Date(startedDate.getTime());
  }

  /**
   * Gets the size of the recording.
   *
   * @return the size in bytes, or -1 when unknown
   */
  public long getSizeBytes() {
    return sizeBytes;
  }

  /**
   * Gets the media type of the recording.
   *
   * @return the mime type
   */
  public String getMimeType() {
    return mimeType;
  }

  /**
   * Gets the link opening the recording in the Documents preview.
   *
   * @return the preview url
   */
  public String getPreviewUrl() {
    return previewUrl;
  }

  /**
   * Sets the link opening the recording in the Documents preview.
   *
   * @param previewUrl the preview url
   */
  public void setPreviewUrl(String previewUrl) {
    this.previewUrl = previewUrl;
  }

}
