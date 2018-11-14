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

/**
 * Class representing the result of a launched sub-process
 */
public class ProcessResult {
  private final int exitCode;
  private final String stdout;
  private final String stderr;
  private final boolean processExited;
  
  public ProcessResult(int exitCode, boolean processExited) {
    this(exitCode, processExited, "", "");
  }
  
  public ProcessResult(int exitCode, boolean processExited, String stdout, String stderr) {
    this.exitCode = exitCode;
    this.processExited = processExited;
    this.stdout = stdout;
    this.stderr = stderr;
  }
  
  /**
   * Exit code of the process launched
   * @return exit code
   */
  public int getExitCode() {
    return exitCode;
  }
  
  /**
   * Indicates if the process managed to exit or was forcefully killed because
   * it exceeded the wait timeout.
   * @return true if it managed to exit before the timeout, false otherwise
   */
  public boolean processExited() {
    return processExited;
  }
  
  /**
   * Get process stdout
   * @return stdout
   */
  public String getStdout() {
    return stdout;
  }
  
  /**
   * Get process stderr
   * @return stderr
   */
  public String getStderr() {
    return stderr;
  }
}
