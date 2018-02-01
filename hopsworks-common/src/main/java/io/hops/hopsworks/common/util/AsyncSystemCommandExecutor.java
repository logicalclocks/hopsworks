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

package io.hops.hopsworks.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class AsyncSystemCommandExecutor {

  private List<String> commandInformation;
  private String adminPassword;
  private ThreadedStreams inputStreamHandler;
  private ThreadedStreams errorStreamHandler;

  private static final Logger logger = LoggerFactory
          .getLogger(AsyncSystemCommandExecutor.class);

  public AsyncSystemCommandExecutor(final List<String> commandInformation) {
    if (commandInformation == null) {
      throw new NullPointerException("The commandInformation is required.");
    }
    this.commandInformation = commandInformation;
    this.adminPassword = null;
  }

  public int executeCommand()
          throws IOException, InterruptedException {
    int exitValue = -99;

    try {
      ProcessBuilder pb = new ProcessBuilder(commandInformation);
      Process process = pb.start();

      // you need this if you're going to write something to the command's input stream
      // (such as when invoking the 'sudo' command, and it prompts you for a password).
      OutputStream stdOutput = process.getOutputStream();

      // i'm currently doing these on a separate line here in case i need to set them to null
      // to get the threads to stop.
      // see http://java.sun.com/j2se/1.5.0/docs/guide/misc/threadPrimitiveDeprecation.html
      InputStream inputStream = process.getInputStream();
      InputStream errorStream = process.getErrorStream();

      // these need to run as java threads to get the standard output and error from the command.
      // the inputstream handler gets a reference to our stdOutput in case we need to write
      // something to it, such as with the sudo command
      inputStreamHandler = new ThreadedStreams(inputStream, stdOutput,
              adminPassword);
      errorStreamHandler = new ThreadedStreams(errorStream);

      // TODO the inputStreamHandler has a nasty side-effect of hanging if the given password is wrong; fix it
      inputStreamHandler.start();
      errorStreamHandler.start();

      // TODO a better way to do this?
//      exitValue = process.waitFor();

    } catch (IOException e) {
      // TODO deal with this here, or just throw it?
      throw e;
    }
    return exitValue;
  }

  /**
   * Get the standard output (stdout) from the command you just exec'd.
   */
  public String getStandardOutputFromCommand() {
    return inputStreamHandler.getOutputBuffer();
  }

  /**
   * Get the standard error (stderr) from the command you just exec'd.
   */
  public String getStandardErrorFromCommand() {
    return errorStreamHandler.getOutputBuffer();
  }

}
