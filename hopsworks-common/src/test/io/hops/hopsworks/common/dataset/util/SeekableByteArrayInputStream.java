/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.common.dataset.util;

import org.apache.hadoop.fs.PositionedReadable;
import org.apache.hadoop.fs.Seekable;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class SeekableByteArrayInputStream extends ByteArrayInputStream implements Seekable, PositionedReadable {

  public SeekableByteArrayInputStream(byte[] buf) {
    super(buf);
  }

  @Override
  public long getPos() {
    return pos;
  }

  @Override
  public void seek(long pos) throws IOException {
    if (mark != 0) {
      throw new IllegalStateException();
    }

    reset();
    long skipped = skip(pos);

    if (skipped != pos) {
      throw new IOException();
    }
  }

  @Override
  public boolean seekToNewSource(long targetPos) {
    return false;
  }

  @Override
  public int read(long position, byte[] buffer, int offset, int length) {

    if (position >= buf.length) {
      throw new IllegalArgumentException();
    }
    if (position + length > buf.length) {
      throw new IllegalArgumentException();
    }
    if (length > buffer.length) {
      throw new IllegalArgumentException();
    }

    System.arraycopy(buf, (int) position, buffer, offset, length);
    return length;
  }

  @Override
  public void readFully(long position, byte[] buffer) {
    read(position, buffer, 0, buffer.length);

  }

  @Override
  public void readFully(long position, byte[] buffer, int offset, int length) {
    read(position, buffer, offset, length);
  }

}
