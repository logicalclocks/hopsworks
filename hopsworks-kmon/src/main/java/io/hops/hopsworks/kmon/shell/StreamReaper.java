/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.kmon.shell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

class StreamReaper {

  private static final Logger logger = Logger.getLogger(StreamReaper.class.
          getName());
  private InputStream inStream;
  private File outputFile;

  public StreamReaper(InputStream inStream, File outputFile) {
    this.inStream = inStream;
    this.outputFile = outputFile;
  }

  public void doWork() throws Exception {
    BufferedReader bufInStream = null;
    BufferedWriter bufOutStream = null;
    try {
      bufInStream = new BufferedReader(new InputStreamReader(inStream));
      FileWriter fileStream = new FileWriter(outputFile);
      bufOutStream = new BufferedWriter(fileStream);

      String line;
      while (true) {
        try {
          line = bufInStream.readLine();
        } catch (IOException ex) {
          logger.log(Level.WARNING, "stream break, assume pipe is broken", ex);
          break;
        }
        if (line == null) {
          break;
        }
        bufOutStream.write(line);
        bufOutStream.newLine();
        /*
         * some overhead here, fork a thread to flush periodically if it's a
         * problem.
         */
        bufOutStream.flush();
      }
      bufOutStream.flush();
    } finally {
      if (bufInStream != null) {
        try {
          bufInStream.close();
        } catch (IOException e) {
          logger.log(Level.WARNING, "falied to close input stream", e);
        }
      }

      if (bufOutStream != null) {
        try {
          bufOutStream.close();
        } catch (IOException e) {
          logger.log(Level.WARNING, "falied to close output stream: "
                  + outputFile, e);
        }
      }
    }
  }

  public void onStart() {
    logger.log(Level.INFO, "start dumping: " + outputFile);
  }

  public void onException(Throwable t) {
    logger.log(Level.WARNING, "falied to dump the stream: " + outputFile);
  }

  public void onFinish() {
    logger.log(Level.INFO, "finish dumping: " + outputFile);
  }
}
