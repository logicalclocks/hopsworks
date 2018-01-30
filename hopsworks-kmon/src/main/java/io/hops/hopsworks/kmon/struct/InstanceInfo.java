package io.hops.hopsworks.kmon.struct;

import io.hops.hopsworks.common.dao.host.Status;
import java.io.Serializable;

public class InstanceInfo implements Serializable {

  private String name;
  private String host;
  private String cluster;
  private String service;
  private String group;
  private Status status;
  private String health;

  public InstanceInfo(String cluster, String group, String service, String host,
          Status status, String health) {

    this.name = service + " @" + host;
    this.host = host;
    this.cluster = cluster;
    this.group = group;
    this.service = service;
    this.status = status;
    this.health = health;
  }

  public String getName() {
    return name;
  }

  public String getHost() {
    return host;
  }

  public Status getStatus() {
    return status;
  }

  public String getHealth() {
    return health;
  }

  public String getService() {
    return service;
  }

  public String getGroup() {
    return group;
  }

  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

}
