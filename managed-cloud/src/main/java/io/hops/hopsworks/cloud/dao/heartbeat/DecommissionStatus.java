/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.cloud.dao.heartbeat;

import io.hops.hopsworks.cloud.CloudNode;
import java.util.Collection;
import java.util.Collections;

public class DecommissionStatus {

  private final Collection<CloudNode> decommissioning;
  private final Collection<CloudNode> decommissioned;

  public DecommissionStatus() {
    this.decommissioning = Collections.EMPTY_LIST;
    this.decommissioned = Collections.EMPTY_LIST;
  }
  
  public DecommissionStatus(Collection<CloudNode> decommissioning, Collection<CloudNode> decommissioned) {
    this.decommissioning = decommissioning;
    this.decommissioned = decommissioned;
  }

  public Collection<CloudNode> getDecommissioning() {
    return decommissioning;
  }

  public Collection<CloudNode> getDecommissioned() {
    return decommissioned;
  }
  
  @Override
  public String toString() {
    return "DecommissionStatus{" +
        "decommissioning=" + decommissioning +
        ", decommissioned=" + decommissioned +
        '}';
  }
}
