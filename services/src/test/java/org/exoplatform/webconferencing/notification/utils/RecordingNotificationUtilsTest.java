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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

/**
 * The mail shows the meeting's start in the recipient's language, with the
 * time zone it is expressed in.
 */
class RecordingNotificationUtilsTest {

  /** 2026-10-23T14:00:00Z. */
  private static final String START = "1792764000000";

  /**
   * The date and time are localized and followed by the zone's short name.
   */
  @Test
  void formatsDateTimeWithZone() {
    String english = RecordingNotificationUtils.formatCallStartDate(START, "en", ZoneOffset.UTC);
    assertTrue(english.startsWith("Oct 23, 2026"), english);
    assertTrue(english.contains("2:00"), english);
    assertTrue(english.endsWith(" Z") || english.endsWith(" UTC"), english);

    String french = RecordingNotificationUtils.formatCallStartDate(START, "fr", ZoneId.of("Europe/Paris"));
    assertTrue(french.startsWith("23 oct. 2026"), french);
    assertTrue(french.contains("16:00"), french);
    assertTrue(french.endsWith(" CEST"), french);
  }

  /**
   * A language written with an underscore, as the platform stores it, is read.
   */
  @Test
  void readsUnderscoreLanguage() {
    String brazilian = RecordingNotificationUtils.formatCallStartDate(START, "pt_BR", ZoneOffset.UTC);
    assertTrue(brazilian.contains("2026"), brazilian);
    assertTrue(brazilian.contains("out"), brazilian);
  }

  /**
   * An absent or malformed date gives an empty text, so the template shows no
   * date line.
   */
  @Test
  void absentOrMalformedDateGivesEmptyText() {
    assertEquals("", RecordingNotificationUtils.formatCallStartDate(null, "en", ZoneOffset.UTC));
    assertEquals("", RecordingNotificationUtils.formatCallStartDate(" ", "en", ZoneOffset.UTC));
    assertEquals("", RecordingNotificationUtils.formatCallStartDate("tomorrow", "en", ZoneOffset.UTC));
  }
}
