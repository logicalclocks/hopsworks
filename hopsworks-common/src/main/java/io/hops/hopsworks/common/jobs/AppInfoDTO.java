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
import java.util.List;
import java.util.Map;

@XmlRootElement
public class AppInfoDTO {
  
  private String appId;
  private long startTime;
  private boolean now;
  private long endTime;
  private int nbExecutors;
  private Map<Integer, List<String>> executorInfo;
  
  public AppInfoDTO() {
  }
  
  public AppInfoDTO(String appId, long startTime, boolean now, long endTime,
    int nbExecutors, Map<Integer, List<String>> executorInfo) {
    this.appId = appId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.now = now;
    this.nbExecutors = nbExecutors;
    this.executorInfo = executorInfo;
  }
  
  public void setAppId(String appId) {
    this.appId = appId;
  }
  
  public String getAppId() {
    return appId;
  }
  
  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }
  
  public long getStartTime() {
    return startTime;
  }
  
  public void setNow(boolean now) {
    this.now = now;
  }
  
  public boolean isNow() {
    return now;
  }
  
  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }
  
  public long getEndTime() {
    return endTime;
  }
  
  public void setNbExecutors(int nbExecutors) {
    this.nbExecutors = nbExecutors;
  }
  
  public int getNbExecutors() {
    return nbExecutors;
  }
  
  public Map<Integer, List<String>> getExecutorInfo() { return executorInfo; }
  
  public void setExecutorInfo(Map<Integer, List<String>> executorInfo) { this.executorInfo = executorInfo; }
  
  @Override
  public String toString() {
    return "AppInfoDTO{" + "appId=" + appId + ", startTime=" + startTime
      + ", now=" + now + ", endTime=" + endTime + ", nbExecutors="
      + nbExecutors + "executorInfo=" + executorInfo + '}';
  }
}
