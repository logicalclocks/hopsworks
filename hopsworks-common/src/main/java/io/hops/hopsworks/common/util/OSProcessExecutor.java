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

import javax.annotation.Resource;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executor for OS processes
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class OSProcessExecutor {
  private static final Logger LOGGER = Logger.getLogger(OSProcessExecutor.class.getName());
  
  @Resource(lookup = "concurrent/hopsExecutorService")
  private ManagedExecutorService executorService;
  
  /**
   * Blocking call to launch a sub-process
   * @param processDescriptor A description of the process to be launched
   * @return Result of the launched process
   * @throws IOException
   */
  public ProcessResult execute(ProcessDescriptor processDescriptor) throws IOException {
    try {
      return runProcess(processDescriptor);
    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
      throw new IOException(ex);
    }
  }
  
  /**
   * Asynchronous call to launch a sub-process
   * @param processDescriptor A description of the process to be launched
   * @return A {@link Future} representing the result of the process when it will finish
   * @throws IOException
   */
  @Asynchronous
  public Future<ProcessResult> submit(ProcessDescriptor processDescriptor) throws IOException {
    try {
      ProcessResult processResult = runProcess(processDescriptor);
      return new AsyncResult<>(processResult);
    } catch (InterruptedException | ExecutionException | TimeoutException ex) {
      throw new IOException(ex);
    }
  }
  
  private ProcessResult runProcess(ProcessDescriptor processDescriptor) throws IOException, InterruptedException,
      ExecutionException, TimeoutException {
    ProcessBuilder processBuilder = new ProcessBuilder(processDescriptor.getSubcommands());
    processBuilder.directory(processDescriptor.getCwd());
    Map<String, String> env = processBuilder.environment();
    for (Map.Entry<String, String> entry : processDescriptor.getEnvironmentVariables().entrySet()) {
      env.put(entry.getKey(), entry.getValue());
    }
    processBuilder.redirectErrorStream(processDescriptor.redirectErrorStream());
    
    Process process = processBuilder.start();
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    ByteArrayOutputStream errStream = new ByteArrayOutputStream();
    boolean ignoreStreams = processDescriptor.ignoreOutErrStreams();
    
    StreamGobbler stderrGobbler;
    Future stderrGobblerFuture = null;
    
    if (!processDescriptor.redirectErrorStream()) {
      stderrGobbler = new StreamGobbler(process.getErrorStream(), errStream, ignoreStreams);
      stderrGobblerFuture = executorService.submit(stderrGobbler);
    }
    
    StreamGobbler stdoutGobbler = new StreamGobbler(process.getInputStream(), outStream, ignoreStreams);
    Future stdoutGobblerFuture = executorService.submit(stdoutGobbler);
    
    boolean exited = process.waitFor(processDescriptor.getWaitTimeout(), processDescriptor.getTimeoutUnit());
    
    if (exited) {
      waitForGobbler(stdoutGobblerFuture);
      if (stderrGobblerFuture != null) {
        waitForGobbler(stderrGobblerFuture);
      }
      return new ProcessResult(process.exitValue(), true, stringifyStream(outStream, ignoreStreams),
          stringifyStream(errStream, ignoreStreams));
    } else {
      process.destroyForcibly();
      stdoutGobblerFuture.cancel(true);
      if (stderrGobblerFuture != null) {
        stderrGobblerFuture.cancel(true);
      }
      return new ProcessResult(process.exitValue(), false, stringifyStream(outStream, ignoreStreams),
          "Process timed-out");
    }
  }
  
  private void waitForGobbler(Future gobbler) throws InterruptedException, ExecutionException {
    try {
      gobbler.get(500L, TimeUnit.MILLISECONDS);
    } catch (TimeoutException ex) {
      LOGGER.log(Level.WARNING, "Waited enough for StreamGobbler to finish, killing it...");
      gobbler.cancel(true);
    }
  }
  
  private String stringifyStream(OutputStream outputStream, boolean ignoreStream) {
    if (ignoreStream) {
      return "";
    }
    return outputStream.toString();
  }
}
