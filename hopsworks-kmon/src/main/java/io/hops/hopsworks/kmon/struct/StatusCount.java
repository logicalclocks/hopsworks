package io.hops.hopsworks.kmon.struct;

import io.hops.hopsworks.common.dao.host.Status;

public class StatusCount {

  private Status status;
  private Long count;

  public StatusCount(Status status, Long count) {
    this.status = status;
    this.count = count;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Long getCount() {
    return count;
  }

  public void setCount(Long count) {
    this.count = count;
  }

}
