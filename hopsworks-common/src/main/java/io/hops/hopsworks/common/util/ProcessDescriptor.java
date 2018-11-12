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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Describe an OS process to be executed by {@link OSProcessExecutor}
 */
public class ProcessDescriptor {
  private final List<String> subcommands;
  private final File cwd;
  private final Map<String, String> env;
  private final long waitTimeout;
  private final TimeUnit timeoutUnit;
  private final boolean redirectErrorStream;
  private final boolean ignoreOutErrStreams;
  
  private ProcessDescriptor(Builder builder) {
    this.subcommands = builder.subcommands;
    this.cwd = builder.cwd;
    this.env = builder.env;
    this.waitTimeout = builder.waitTimeout;
    this.timeoutUnit = builder.timeoutUnit;
    this.redirectErrorStream = builder.redirectErrorStream;
    this.ignoreOutErrStreams = builder.ignoreOutErrStreams;
  }
  
  public List<String> getSubcommands() {
    return subcommands;
  }
  
  public File getCwd() {
    return cwd;
  }
  
  public Map<String, String> getEnvironmentVariables() {
    return env;
  }
  
  public long getWaitTimeout() {
    return waitTimeout;
  }
  
  public TimeUnit getTimeoutUnit() {
    return timeoutUnit;
  }
  
  public boolean redirectErrorStream() {
    return redirectErrorStream;
  }
  
  public boolean ignoreOutErrStreams() {
    return ignoreOutErrStreams;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("COMMAND: ");
    for (String sc : subcommands) {
      sb.append(sc).append(" ");
    }
    
    return sb.toString();
  }
  
  /**
   * Builder class for {@link ProcessDescriptor}
   */
  public static class Builder {
    private final List<String> subcommands;
    private File cwd = null;
    private final Map<String, String> env;
    private long waitTimeout = 20;
    private TimeUnit timeoutUnit = TimeUnit.SECONDS;
    private boolean redirectErrorStream = false;
    private boolean ignoreOutErrStreams = false;
    
    public Builder() {
      subcommands = new ArrayList<>();
      env = new HashMap<>();
    }
  
    /**
     * Add a sub-command or argument to Process
     * @param subcommand Command string
     * @return Builder object
     */
    public Builder addCommand(String subcommand) {
      subcommands.add(subcommand);
      return this;
    }
  
    /**
     * Make an environment variable available to the new process. A new process inherits
     * the environment variables of its parent.
     * @param key name of the variable
     * @param value value
     * @return Builder object
     */
    public Builder addEnvironmentVariable(String key, String value) {
      env.put(key, value);
      return this;
    }
  
    /**
     * Sets the working directory of the new process. If not specified it will be the working
     * directory of its parent process.
     * @param cwd An existing directory
     * @return Builder object
     */
    public Builder setCurrentWorkingDirectory(File cwd) {
      this.cwd = cwd;
      return this;
    }
  
    /**
     * Sets a time to wait for the process to exit. If the process has not finished and the
     * timeout has elapsed, the process is killed.
     * @param waitTimeout Time to wait
     * @param timeoutUnit Time unit to wait for
     * @return Builder object
     */
    public Builder setWaitTimeout(long waitTimeout, TimeUnit timeoutUnit) {
      this.waitTimeout = waitTimeout;
      this.timeoutUnit = timeoutUnit;
      return this;
    }
  
    /**
     * Redirect stderr to stdout. This helps correlating errors and has a performance impact
     * as the process will launch one thread to consume the stdout stream instead of two.
     * @param redirectErrorStream
     * @return Builder object
     */
    public Builder redirectErrorStream(boolean redirectErrorStream) {
      this.redirectErrorStream = redirectErrorStream;
      return this;
    }
  
    /**
     * If you are not interested in parsing the output of a process, setting this property will
     * improve the performance overall. It will redirect stderr to stdout in order to spawn one
     * consumer thread instead of two, and it will not use any memory for an output stream to return
     * to the user.
     * @param ignoreOutErrStreams
     * @return Builder object
     */
    public Builder ignoreOutErrStreams(boolean ignoreOutErrStreams) {
      this.ignoreOutErrStreams = ignoreOutErrStreams;
      if (ignoreOutErrStreams) {
        this.redirectErrorStream = true;
      }
      return this;
    }
  
    /**
     * Build a {@link ProcessDescriptor}
     * @return description of a process
     */
    public ProcessDescriptor build() {
      if (subcommands.isEmpty()) {
        throw new IllegalArgumentException("Commands cannot be empty");
      }
      
      return new ProcessDescriptor(this);
    }
  }
}
