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
