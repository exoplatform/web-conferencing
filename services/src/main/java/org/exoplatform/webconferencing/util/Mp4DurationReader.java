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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Reads how long an MP4 recording lasts from its movie header ({@code mvhd},
 * inside {@code moov}), without decoding it and without ffmpeg.
 * <p>
 * The top-level boxes are walked by seeking over them, so a {@code moov}
 * written after the media data, as the recorder writes it, costs a few reads
 * whatever the size of the file. Anything that is not a readable,
 * non-fragmented MP4 has no duration.
 */
public final class Mp4DurationReader {

  /** The most boxes read at one level: a guard against a file that is not an MP4. */
  static final int        MAX_BOXES = 1024;

  private static final Log LOG      = ExoLogger.getLogger(Mp4DurationReader.class);

  /**
   * No instance: static helpers only.
   */
  private Mp4DurationReader() {
  }

  /**
   * Reads the duration of an MP4 file.
   *
   * @param location the path of the file
   * @return the duration in seconds, rounded to the nearest second, or
   *         <code>null</code> when the file is not a readable MP4 or does not
   *         tell its duration (a fragmented MP4 says 0)
   */
  public static Long readSeconds(String location) {
    if (location == null) {
      return null;
    }
    try (RandomAccessFile file = new RandomAccessFile(new File(location), "r")) {
      return readSeconds(file, file.length());
    } catch (IOException e) {
      LOG.debug("Cannot read the duration of the media file {}", location, e);
      return null;
    }
  }

  /**
   * Reads the duration of an open MP4 file.
   *
   * @param file the file, read from its start
   * @param length the length of the file
   * @return the duration in seconds, or <code>null</code> when unknown
   * @throws IOException when the file cannot be read
   */
  static Long readSeconds(RandomAccessFile file, long length) throws IOException {
    long[] moov = findBox(file, 0, length, "moov");
    if (moov == null) {
      return null;
    }
    long[] mvhd = findBox(file, moov[0], moov[1], "mvhd");
    if (mvhd == null) {
      return null;
    }
    return readMovieHeader(file, mvhd[0], mvhd[1]);
  }

  /**
   * Finds the first box of a type among the boxes between two offsets.
   *
   * @param file the file
   * @param start the offset of the first box
   * @param end the offset the boxes end at
   * @param type the four-character type looked for
   * @return {payload start, payload end} of the box, or <code>null</code>
   *         when it is absent or a box header is not readable
   * @throws IOException when the file cannot be read
   */
  private static long[] findBox(RandomAccessFile file, long start, long end, String type) throws IOException {
    long position = start;
    for (int i = 0; i < MAX_BOXES && position + 8 <= end; i++) {
      file.seek(position);
      long size = Integer.toUnsignedLong(file.readInt());
      byte[] typeBytes = new byte[4];
      file.readFully(typeBytes);
      long header = 8;
      if (size == 1) {
        if (position + 16 > end) {
          return null;
        }
        size = file.readLong();
        header = 16;
      } else if (size == 0) {
        size = end - position;
      }
      if (size < header || position + size > end) {
        return null;
      }
      if (type.equals(new String(typeBytes, StandardCharsets.ISO_8859_1))) {
        return new long[] { position + header, position + size };
      }
      position += size;
    }
    return null;
  }

  /**
   * Reads the duration a movie header gives: its duration over its time
   * scale. Version 0 writes both times on 32 bits, version 1 on 64.
   *
   * @param file the file
   * @param start the offset of the header's payload
   * @param end the offset the payload ends at
   * @return the duration in seconds, or <code>null</code> when the header
   *         gives none (0, the all-ones « unknown », or no time scale)
   * @throws IOException when the file cannot be read
   */
  private static Long readMovieHeader(RandomAccessFile file, long start, long end) throws IOException {
    file.seek(start);
    int version = file.readUnsignedByte();
    file.skipBytes(3);
    long timescale;
    long duration;
    if (version == 1) {
      if (start + 32 > end) {
        return null;
      }
      file.skipBytes(16);
      timescale = Integer.toUnsignedLong(file.readInt());
      duration = file.readLong();
    } else if (version == 0) {
      if (start + 20 > end) {
        return null;
      }
      file.skipBytes(8);
      timescale = Integer.toUnsignedLong(file.readInt());
      long raw = Integer.toUnsignedLong(file.readInt());
      duration = raw == 0xFFFFFFFFL ? -1 : raw;
    } else {
      return null;
    }
    if (timescale == 0 || duration <= 0) {
      return null;
    }
    return Math.round((double) duration / timescale);
  }

}
