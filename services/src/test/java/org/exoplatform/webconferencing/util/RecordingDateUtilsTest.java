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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;

/**
 * The dates a client gives to list recordings.
 */
class RecordingDateUtilsTest {

  /**
   * A date-time with an offset, without one (UTC), and a bare date, which
   * starts or ends its UTC day depending on the bound it is.
   */
  @Test
  void readsTheAcceptedForms() {
    assertEquals(Date.from(Instant.parse("2026-10-23T08:00:00Z")), RecordingDateUtils.parseDate("2026-10-23T10:00:00+02:00", false));
    assertEquals(Date.from(Instant.parse("2026-10-23T10:00:00Z")), RecordingDateUtils.parseDate("2026-10-23T10:00:00Z", true));
    assertEquals(Date.from(Instant.parse("2026-10-23T10:00:00Z")), RecordingDateUtils.parseDate("2026-10-23T10:00:00", false));
    assertEquals(Date.from(Instant.parse("2026-10-23T00:00:00Z")), RecordingDateUtils.parseDate("2026-10-23", false));
    assertEquals(Date.from(Instant.parse("2026-10-23T23:59:59.999Z")), RecordingDateUtils.parseDate("2026-10-23", true));
    assertNull(RecordingDateUtils.parseDate(" ", false));
    assertNull(RecordingDateUtils.parseDate(null, true));
  }

  /**
   * Anything else is refused with the code the REST answers.
   */
  @Test
  void refusesAnythingElse() {
    assertEquals(RecordingDateUtils.RECORDINGS_INVALID_DATE,
                 assertThrows(IllegalArgumentException.class, () -> RecordingDateUtils.parseDate("yesterday", false)).getMessage());
  }

  /**
   * A date is written as a UTC instant.
   */
  @Test
  void writesAnInstant() {
    assertEquals("2026-10-23T10:05:00Z", RecordingDateUtils.formatDate(Date.from(Instant.parse("2026-10-23T10:05:00Z"))));
    assertNull(RecordingDateUtils.formatDate(null));
  }

}
