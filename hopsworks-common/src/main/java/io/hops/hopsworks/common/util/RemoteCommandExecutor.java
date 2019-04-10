/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalTime;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RemoteCommandExecutor {
  
  private final static Logger LOGGER = Logger.getLogger(RemoteCommandExecutor.class.getName());
  
  /**
   * Execute a command on a remote host over SSH
   * @param command Command description
   * @return Result of command
   * @throws ServiceException
   */
  public RemoteCommandResult execute(RemoteCommand command) throws ServiceException {
    return executeInternal(command);
  }
  
  /**
   * Asynchronously execute a command on a remote host over SSH
   * @param command Command description
   * @return A future of the result
   * @throws ServiceException
   */
  @Asynchronous
  public Future<RemoteCommandResult> submit(RemoteCommand command) throws ServiceException {
    RemoteCommandResult asyncResult = executeInternal(command);
    return new AsyncResult<>(asyncResult);
  }
  
  private RemoteCommandResult executeInternal(RemoteCommand command) throws ServiceException {
    Channel channel = null;
    Session session = null;
    try {
      LOGGER.log(Level.FINE, "Executing remote command: " + command);
      JSch jsch = new JSch();
      jsch.addIdentity(command.getIdentity().toString());
      session = jsch.getSession(command.getUser(), command.getHost(), command.getPort());
      session.setConfig(command.getSSHConfig());
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect(command.getConnectTimeout());
    
      channel = session.openChannel("exec");
      ((ChannelExec) channel).setCommand(command.getCommand());
      String stdout = waitForCommandToFinish(channel, command);
      int exitCode = channel.getExitStatus();
      return new RemoteCommandResult(stdout, exitCode);
    } catch (JSchException | IOException ex) {
      LOGGER.log(Level.WARNING, "Error executing remote command " + command, ex);
      throw new ServiceException(RESTCodes.ServiceErrorCode.ERROR_EXECUTING_REMOTE_COMMAND, Level.WARNING,
          "Error while executing remote command", "Error executing remote command: " + command, ex);
    } catch (RemoteCommandTimeoutException ex) {
      return new RemoteCommandResult("Command time-out: " + ex.getMessage(), 20);
    } finally {
      if (channel != null) {
        channel.disconnect();
      }
      if (session != null) {
        session.disconnect();
      }
    }
  }
  
  private String waitForCommandToFinish(Channel channel, RemoteCommand command)
      throws JSchException, IOException, RemoteCommandTimeoutException {
    byte[] buffer = new byte[1024];
    channel.connect(command.getConnectTimeout());
    int executionTimeout = command.getExecutionTimeout();
    LocalTime start = LocalTime.now();
    try (BufferedInputStream bin = new BufferedInputStream(channel.getInputStream());
         ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
      while (true) {
        if (bin.available() != 0) {
          int read = bin.read(buffer);
          if (read > 0) {
            bout.write(buffer, 0, read);
          }
          if (read < 0) {
            break;
          }
        }
        if (channel.isClosed()) {
          if (bin.available() > 0) {
            continue;
          } else {
            break;
          }
        }
        if (executionTimeout > 0
            && !Duration.between(start, LocalTime.now()).minusSeconds(executionTimeout).isNegative()) {
          throw new RemoteCommandTimeoutException("Remote command timed-out after 20 seconds");
        }
        try {
          TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException ex) {
          throw new RemoteCommandTimeoutException("Remote command was interrupted while waiting for I/O", ex);
        }
      }
      return bout.toString(Charset.defaultCharset().displayName());
    }
  }
  
  private class RemoteCommandTimeoutException extends Exception {
    private RemoteCommandTimeoutException(String reason) {
      super(reason);
    }
    
    private RemoteCommandTimeoutException(String reason, Throwable throwable) {
      super(reason, throwable);
    }
  }
}
