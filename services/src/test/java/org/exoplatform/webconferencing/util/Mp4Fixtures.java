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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Builds the boxes of minimal MP4 files, the way a recorder lays them out:
 * an {@code ftyp}, the media data and the {@code moov} holding the movie
 * header, in any order.
 */
public final class Mp4Fixtures {

  /**
   * No instance: static helpers only.
   */
  private Mp4Fixtures() {
  }

  /**
   * Builds a box: its 32-bit size, its type and its payload.
   *
   * @param type the four-character type
   * @param payload the payload
   * @return the box's bytes
   */
  public static byte[] box(String type, byte[]... payload) {
    byte[] content = concat(payload);
    return ByteBuffer.allocate(8 + content.length)
                     .putInt(8 + content.length)
                     .put(type.getBytes(StandardCharsets.ISO_8859_1))
                     .put(content)
                     .array();
  }

  /**
   * Builds a box with a 64-bit size: a 32-bit size of 1, the type, then the
   * real size.
   *
   * @param type the four-character type
   * @param payload the payload
   * @return the box's bytes
   */
  public static byte[] largeBox(String type, byte[]... payload) {
    byte[] content = concat(payload);
    return ByteBuffer.allocate(16 + content.length)
                     .putInt(1)
                     .put(type.getBytes(StandardCharsets.ISO_8859_1))
                     .putLong(16L + content.length)
                     .put(content)
                     .array();
  }

  /**
   * Builds a version 0 movie header: 32-bit times.
   *
   * @param timescale the units per second
   * @param duration the duration in those units, as an unsigned 32-bit value
   * @return the {@code mvhd} box
   */
  public static byte[] mvhdV0(int timescale, int duration) {
    return box("mvhd",
               ByteBuffer.allocate(96)
                         .putInt(0)
                         .putInt(0)
                         .putInt(0)
                         .putInt(timescale)
                         .putInt(duration)
                         .array());
  }

  /**
   * Builds a version 1 movie header: 64-bit times.
   *
   * @param timescale the units per second
   * @param duration the duration in those units
   * @return the {@code mvhd} box
   */
  public static byte[] mvhdV1(int timescale, long duration) {
    return box("mvhd",
               ByteBuffer.allocate(108)
                         .putInt(1 << 24)
                         .putLong(0)
                         .putLong(0)
                         .putInt(timescale)
                         .putLong(duration)
                         .array());
  }

  /**
   * Builds the {@code ftyp} box a recorder writes first.
   *
   * @return the box
   */
  public static byte[] ftyp() {
    return box("ftyp", "isom".getBytes(StandardCharsets.ISO_8859_1), new byte[4]);
  }

  /**
   * Builds a media data box of a given payload size.
   *
   * @param size the payload size
   * @return the box
   */
  public static byte[] mdat(int size) {
    return box("mdat", new byte[size]);
  }

  /**
   * Writes the given bytes to a new temporary file, deleted on exit.
   *
   * @param parts the parts of the file
   * @return the file
   * @throws IOException when the file cannot be written
   */
  public static File write(byte[]... parts) throws IOException {
    File file = File.createTempFile("recording", ".mp4");
    file.deleteOnExit();
    Files.write(file.toPath(), concat(parts));
    return file;
  }

  /**
   * Concatenates byte arrays.
   *
   * @param parts the arrays
   * @return their concatenation
   */
  public static byte[] concat(byte[]... parts) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    for (byte[] part : parts) {
      out.writeBytes(part);
    }
    return out.toByteArray();
  }

}
