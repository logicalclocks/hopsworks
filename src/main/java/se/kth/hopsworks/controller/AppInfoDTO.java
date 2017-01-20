package se.kth.hopsworks.controller;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AppInfoDTO {

  String appId;
  long startTime;
  boolean now;
  long endTime;
  int nbExecutors;

  public AppInfoDTO() {
  }

  public AppInfoDTO(String appId, long startTime, boolean now, long endTime,
          int nbExecutors) {
    this.appId = appId;
    this.startTime = startTime;
    this.endTime = endTime;
    this.now = now;
    this.nbExecutors = nbExecutors;
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

  @Override
  public String toString() {
    return "AppInfoDTO{" + "appId=" + appId + ", startTime=" + startTime
            + ", now=" + now + ", endTime=" + endTime + ", nbExecutors="
            + nbExecutors + '}';
  }
}
