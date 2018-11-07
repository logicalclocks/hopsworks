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
package io.hops.hopsworks.common.jobs;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JobLogDTO {
  
  private String message;
  private String path;
  private LogType type;
  private Retriable retriable;
  
  public JobLogDTO() {
  }
  
  public JobLogDTO(LogType type) {
    this.type = type;
  }
  
  public String getMessage() {
    return message;
  }
  
  public void setMessage(String message) {
    this.message = message;
  }
  
  public String getPath() {
    return path;
  }
  
  public void setPath(String path) {
    this.path = path;
  }
  
  public LogType getType() {
    return type;
  }
  
  public void setType(LogType type) {
    this.type = type;
  }
  
  public Retriable getRetriable() {
    return retriable;
  }
  
  public void setRetriable(Retriable retriable) {
    this.retriable = retriable;
  }
  
  public enum LogType {
    out,
    err;
  }
  
  public enum Retriable {
    retriableOut,
    retriableErr;
  }
}