package io.hops.hopsworks.kmon.struct;

import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.host.Health;
import io.hops.hopsworks.common.dao.kagent.HostServicesInfo;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class GroupInfo {

  private final String name;
  private Health health;
  private final Set<String> services = new HashSet<>();
  private final Map<String, Integer> servicesCount = new HashMap<>();
  private final Set<String> badServices = new HashSet<>();
  private int started;
  private int stopped;
  private int timedOut;

  public GroupInfo(String name) {
    started = 0;
    stopped = 0;
    timedOut = 0;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Map getStatus() {

    Map<Status, Integer> statusMap = new TreeMap<Status, Integer>();
    if (started > 0) {
      statusMap.put(Status.Started, started);
    }
    if (stopped > 0) {
      statusMap.put(Status.Stopped, stopped);
    }
    if (timedOut > 0) {
      statusMap.put(Status.TimedOut, timedOut);
    }
    return statusMap;
  }

  public Health getHealth() {
    return health;
  }

  public Integer serviceCount(String service) {
    return servicesCount.get(service);
  }

  public String[] getServices() {
    return services.toArray(new String[servicesCount.size()]);
  }

  public Health serviceHealth(String service) {
    if (badServices.contains(service)) {
      return Health.Bad;
    }
    return Health.Good;
  }

  public Health addServices(List<HostServicesInfo> services) {
    for (HostServicesInfo serviceHostInfo : services) {
      if (serviceHostInfo.getHostServices().getService().equals("")) {
        continue;
      }
      this.services.add(serviceHostInfo.getHostServices().getService());
      if (serviceHostInfo.getStatus() == Status.Started) {
        started += 1;
      } else {
        badServices.add(serviceHostInfo.getHostServices().getService());
        if (serviceHostInfo.getStatus() == Status.Stopped) {
          stopped += 1;
        } else {
          timedOut += 1;
        }
      }
      addService(serviceHostInfo.getHostServices().getService());
    }
    health = (stopped + timedOut > 0) ? Health.Bad : Health.Good;
    return health;
  }

  private void addService(String service) {
    if (servicesCount.containsKey(service)) {
      Integer current = servicesCount.get(service);
      servicesCount.put(service, current + 1);
    } else {
      servicesCount.put(service, 1);
    }
  }
}
