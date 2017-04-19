package io.hops.hopsworks.common.dao.role;

import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.host.Host;

public class RoleHostInfo {

  private Role role;
  private Host host;

  public RoleHostInfo(Role role, Host host) {
    this.role = role;
    this.host = host;
  }

  public Role getRole() {
    return role;
  }

  public Host getHost() {
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
