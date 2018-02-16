/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package io.hops.hopsworks.common.dao.jobs.description;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.HashMap;
import java.util.List;

@XmlRootElement
public class AppInfoDTO {

  String appId;
  long startTime;
  boolean now;
  long endTime;
  int nbExecutors;
  HashMap<Integer, List<String>> executorInfo;

  public AppInfoDTO() {
  }

  public AppInfoDTO(String appId, long startTime, boolean now, long endTime,
                    int nbExecutors, HashMap<Integer, List<String>> executorInfo) {
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

  public HashMap<Integer, List<String>> getExecutorInfo() { return executorInfo; }

  public void setExecutorInfo(HashMap<Integer, List<String>> executorInfo) { this.executorInfo = executorInfo; }

  @Override
  public String toString() {
    return "AppInfoDTO{" + "appId=" + appId + ", startTime=" + startTime
            + ", now=" + now + ", endTime=" + endTime + ", nbExecutors="
            + nbExecutors + "executorInfo=" + executorInfo + '}';
  }
}
