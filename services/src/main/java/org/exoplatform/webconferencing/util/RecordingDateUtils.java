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
package org.exoplatform.webconferencing.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

/**
 * Reads and writes the dates a client gives and gets when it lists the
 * recordings of a call.
 */
public final class RecordingDateUtils {

  /** The message code of a date that cannot be read. */
  public static final String RECORDINGS_INVALID_DATE = "webconferencing.recordings.invalidDate";

  /**
   * No instance: static helpers only.
   */
  private RecordingDateUtils() {
  }

  /**
   * Reads a date: an ISO 8601 date-time with an offset ({@code 2026-10-23T10:00:00Z},
   * {@code 2026-10-23T10:00:00+02:00}), a date-time without one, read in UTC,
   * or a bare date, which stands for the start of that day in UTC, or for its
   * end when it closes a range.
   *
   * @param value the date, or a blank value for none
   * @param endOfRange true when the date closes a range
   * @return the date, or <code>null</code> when the value is blank
   * @throws IllegalArgumentException with {@value #RECORDINGS_INVALID_DATE}
   *           when the value is not a date
   */
  public static Date parseDate(String value, boolean endOfRange) {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    String trimmed = value.trim();
    try {
      return Date.from(OffsetDateTime.parse(trimmed).toInstant());
    } catch (DateTimeParseException e) {
      // Not a date-time with an offset: try the other accepted forms
    }
    try {
      return Date.from(LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC));
    } catch (DateTimeParseException e) {
      // Not a local date-time: try a bare date
    }
    try {
      LocalDate date = LocalDate.parse(trimmed);
      Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
      return Date.from(endOfRange ? date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1) : start);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(RECORDINGS_INVALID_DATE, e);
    }
  }

  /**
   * Writes a date as an ISO 8601 instant in UTC.
   *
   * @param date the date, or <code>null</code>
   * @return the instant, or <code>null</code>
   */
  public static String formatDate(Date date) {
    return date == null ? null : date.toInstant().toString();
  }

}
