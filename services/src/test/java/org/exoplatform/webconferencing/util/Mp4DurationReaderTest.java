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

import static org.exoplatform.webconferencing.util.Mp4Fixtures.box;
import static org.exoplatform.webconferencing.util.Mp4Fixtures.ftyp;
import static org.exoplatform.webconferencing.util.Mp4Fixtures.largeBox;
import static org.exoplatform.webconferencing.util.Mp4Fixtures.mdat;
import static org.exoplatform.webconferencing.util.Mp4Fixtures.mvhdV0;
import static org.exoplatform.webconferencing.util.Mp4Fixtures.mvhdV1;
import static org.exoplatform.webconferencing.util.Mp4Fixtures.write;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Reads a recording's length from its movie header.
 */
class Mp4DurationReaderTest {

  /**
   * The recorder's layout: the movie header after the media data. The
   * duration is the header's over its time scale, rounded to the second.
   *
   * @throws Exception never
   */
  @Test
  void readsTheMovieHeaderWrittenAfterTheMedia() throws Exception {
    File file = write(ftyp(), mdat(4096), box("moov", box("udta"), mvhdV0(1000, 3_482_400)));

    assertEquals(3482L, Mp4DurationReader.readSeconds(file.getAbsolutePath()));
  }

  /**
   * A faststart file carries its movie header first; half a second rounds
   * up.
   *
   * @throws Exception never
   */
  @Test
  void readsTheMovieHeaderWrittenFirst() throws Exception {
    File file = write(ftyp(), box("moov", mvhdV0(600, 10_500)), mdat(16));

    assertEquals(18L, Mp4DurationReader.readSeconds(file.getAbsolutePath()));
  }

  /**
   * A version 1 header writes 64-bit times, and a 64-bit box size is
   * followed.
   *
   * @throws Exception never
   */
  @Test
  void readsAVersionOneHeaderAfterALargeBox() throws Exception {
    File file = write(ftyp(), largeBox("mdat", new byte[32]), box("moov", mvhdV1(90_000, 90_000L * 5400)));

    assertEquals(5400L, Mp4DurationReader.readSeconds(file.getAbsolutePath()));
  }

  /**
   * A 32-bit duration above the signed range is read unsigned.
   *
   * @throws Exception never
   */
  @Test
  void readsAnUnsignedDuration() throws Exception {
    File file = write(ftyp(), box("moov", mvhdV0(1_000_000, (int) 3_000_000_000L)));

    assertEquals(3000L, Mp4DurationReader.readSeconds(file.getAbsolutePath()));
  }

  /**
   * A fragmented MP4 says 0 in its movie header, and the all-ones value says
   * the duration is unknown: neither is a length.
   *
   * @throws Exception never
   */
  @Test
  void aZeroOrUnknownDurationIsNoLength() throws Exception {
    assertNull(Mp4DurationReader.readSeconds(write(ftyp(), box("moov", mvhdV0(1000, 0))).getAbsolutePath()));
    assertNull(Mp4DurationReader.readSeconds(write(ftyp(), box("moov", mvhdV0(1000, -1))).getAbsolutePath()));
    assertNull(Mp4DurationReader.readSeconds(write(ftyp(), box("moov", mvhdV1(1000, -1L))).getAbsolutePath()));
    assertNull(Mp4DurationReader.readSeconds(write(ftyp(), box("moov", mvhdV0(0, 1000))).getAbsolutePath()));
  }

  /**
   * A movie header outside a {@code moov}, a {@code moov} without one, or a
   * file with no {@code moov} at all has no length.
   *
   * @throws Exception never
   */
  @Test
  void aMissingHeaderIsNoLength() throws Exception {
    assertNull(Mp4DurationReader.readSeconds(write(ftyp(), mvhdV0(1000, 60_000), mdat(8)).getAbsolutePath()));
    assertNull(Mp4DurationReader.readSeconds(write(ftyp(), box("moov", box("trak"))).getAbsolutePath()));
    assertNull(Mp4DurationReader.readSeconds(write(ftyp(), mdat(64)).getAbsolutePath()));
  }

  /**
   * A file cut inside its media data, a box declaring less than its own
   * header, a header cut short, or a file that is not an MP4 has no length,
   * and no exception escapes.
   *
   * @throws Exception never
   */
  @Test
  void aBrokenFileIsNoLength() throws Exception {
    byte[] whole = Mp4Fixtures.concat(ftyp(), mdat(4096), box("moov", mvhdV0(1000, 60_000)));
    assertNull(Mp4DurationReader.readSeconds(write(Arrays.copyOf(whole, 2048)).getAbsolutePath()));
    byte[] tooSmall = Mp4Fixtures.concat(ftyp(), new byte[] { 0, 0, 0, 4, 'm', 'd', 'a', 't' });
    assertNull(Mp4DurationReader.readSeconds(write(tooSmall).getAbsolutePath()));
    byte[] shortHeader = box("moov", box("mvhd", new byte[12]));
    assertNull(Mp4DurationReader.readSeconds(write(ftyp(), shortHeader).getAbsolutePath()));
    byte[] text = "WEBVTT\n\n00:00.000 --> 00:01.000\nhello\n".getBytes(StandardCharsets.UTF_8);
    assertNull(Mp4DurationReader.readSeconds(write(text).getAbsolutePath()));
    assertNull(Mp4DurationReader.readSeconds(new File("does-not-exist.mp4").getAbsolutePath()));
    assertNull(Mp4DurationReader.readSeconds(null));
  }

  /**
   * A movie header declaring more bytes than the {@code moov} holding it is
   * malformed: it has no length, even when the file goes on after it.
   *
   * @throws Exception never
   */
  @Test
  void aBoxOverrunningItsParentIsNoLength() throws Exception {
    byte[] mvhd = mvhdV0(1000, 60_000);
    ByteBuffer.wrap(mvhd).putInt(mvhd.length + 64);
    File file = write(ftyp(), box("moov", mvhd), mdat(64));

    assertNull(Mp4DurationReader.readSeconds(file.getAbsolutePath()));
  }

}
