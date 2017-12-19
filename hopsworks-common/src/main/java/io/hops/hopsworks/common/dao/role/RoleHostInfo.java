package io.hops.hopsworks.common.dao.role;

import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.host.Hosts;

public class RoleHostInfo {

  private Roles role;
  private Hosts host;

  public RoleHostInfo(Roles role, Hosts host) {
    this.role = role;
    this.host = host;
  }

  public Roles getRole() {
    return role;
  }

  public Hosts getHost() {
    return host;
  }

  public Health getHealth() {
    if (role.getHealth() == Health.Good) {
      return Health.Good;
    }
    return Health.Bad;
  }

  public Status getStatus() {
    return role.getStatus();
  }
}
