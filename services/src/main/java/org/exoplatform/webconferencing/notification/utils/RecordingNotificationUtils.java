/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.webconferencing.notification.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * Formatting helpers of the call recording notification.
 */
public class RecordingNotificationUtils {

  /**
   * Prevents instantiation of this utility class.
   */
  private RecordingNotificationUtils() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Formats the start date of a recorded meeting for a mail: the date and time
   * in the recipient's language, in the given time zone, followed by that zone's
   * short name, so the reader knows which zone the time is in.
   *
   * @param callStartDate the start date in milliseconds since the epoch, as the
   *          notification stores it
   * @param language the recipient's language, as {@code fr} or {@code pt_BR}
   * @param zoneId the time zone to show the time in
   * @return the formatted date, or an empty string when the date is absent or
   *         not a number
   */
  public static String formatCallStartDate(String callStartDate, String language, ZoneId zoneId) {
    if (StringUtils.isBlank(callStartDate)) {
      return "";
    }
    long epochMillis;
    try {
      epochMillis = Long.parseLong(callStartDate.trim());
    } catch (NumberFormatException e) {
      return "";
    }
    Locale locale = StringUtils.isBlank(language) ? Locale.ENGLISH : Locale.forLanguageTag(language.replace('_', '-'));
    ZonedDateTime dateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId);
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale).format(dateTime)
        + " " + DateTimeFormatter.ofPattern("z", locale).format(dateTime);
  }
}
