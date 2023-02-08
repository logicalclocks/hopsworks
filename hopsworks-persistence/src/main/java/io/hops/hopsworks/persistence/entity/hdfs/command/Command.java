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
package io.hops.hopsworks.persistence.entity.hdfs.command;

public enum Command {
  EXTRACT("EXTRACT", "extract"),
  COMPRESS("COMPRESS", "compress");
//  COPY("COPY", "copy"),
//  MOVE("MOVE", "move");
  
  private final String cmd;
  private final String jobCmd;
  
  Command(String cmd, String jobCmd) {
    this.cmd = cmd;
    this.jobCmd = jobCmd;
  }
  
  public String getCmd() {
    return cmd;
  }
  
  public String getJobCmd() {
    return jobCmd;
  }
}