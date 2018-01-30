package io.hops.hopsworks.common.dao.kagent;

import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.host.Hosts;

public class HostServicesInfo {

  private HostServices hostServices;
  private Hosts host;

  public HostServicesInfo(HostServices hostServices, Hosts host) {
    this.hostServices = hostServices;
    this.host = host;
  }

  public HostServices getHostServices() {
    return hostServices;
  }

  public Hosts getHost() {
    return host;
  }

  public Health getHealth() {
    if (hostServices.getHealth() == Health.Good) {
      return Health.Good;
    }
    return Health.Bad;
  }

  public Status getStatus() {
    return hostServices.getStatus();
  }
}
