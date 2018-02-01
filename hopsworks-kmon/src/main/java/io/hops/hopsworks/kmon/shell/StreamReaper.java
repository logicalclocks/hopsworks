/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
