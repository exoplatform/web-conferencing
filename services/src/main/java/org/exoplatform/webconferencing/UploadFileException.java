/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.webconferencing;

/**
 * The SaveRecordingException is thrown when the call recording cannot be saved.
 */
public class UploadFileException extends WebConferencingException {

  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -487927094288308338L;

  /**
   * Instantiates a new save recording exception.
   *
   * @param message the message
   * @param cause the cause
   */
  public UploadFileException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new save recording exception.
   *
   * @param message the message
   */
  public UploadFileException(String message) {
    super(message);
  }

}
