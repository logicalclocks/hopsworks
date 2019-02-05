/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Consumer for output streams of sub-processes used by {@link OSProcessExecutor}
 */
public class StreamGobbler implements Runnable {
  private final static int KB = 1024;
  
  private final InputStream in;
  private final OutputStream out;
  private final byte[] buffer;
  private final boolean ignoreStream;
  
  public StreamGobbler(InputStream in, OutputStream out, boolean ignoreStream) {
    this.in = in;
    this.out = out;
    this.ignoreStream = ignoreStream;
    this.buffer = new byte[4 * KB];
  }
  
  @Override
  public void run() {
    int bytesRead = 0;
    try (BufferedInputStream bis = new BufferedInputStream(in)) {
      while ((bytesRead = bis.read(buffer)) != -1) {
        if (!ignoreStream) {
          out.write(buffer, 0, bytesRead);
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace(new PrintStream(out));
    }
  }
}
