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
  
  private String log;
  private String path;
  private LogType type;
  private Retriable retriable;
  
  public JobLogDTO() {
  }
  
  public JobLogDTO(LogType type) {
    this.type = type;
  }
  
  public String getLog() {
    return log;
  }
  
  public void setLog(String message) {
    this.log = message;
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
    OUT("out"),
    ERR("err");
    private final String name;
    
    LogType(String name) {
      this.name = name;
    }
    
    public static LogType fromString(String name) {
      return valueOf(name.toUpperCase());
    }
    
    public String getName() {
      return name;
    }
    
    @Override
    public String toString() {
      return name;
    }
  }
  
  public enum Retriable {
    RETRIEABLE_OUT("retriableOut"),
    RETRIABLE_ERR("retriableErr");
    
    private final String name;
    
    Retriable(String name) {
      this.name = name;
    }
    
    public static Retriable fromString(String name) {
      return valueOf(name.toUpperCase());
    }
    
    public String getName() {
      return name;
    }
    
    @Override
    public String toString() {
      return name;
    }
  }
}