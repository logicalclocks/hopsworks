package io.hops.hopsworks.kmon.struct;

import io.hops.hopsworks.common.dao.host.Status;
import io.hops.hopsworks.common.dao.host.Health;
import java.util.SortedMap;
import java.util.TreeMap;

public class ServiceInstancesInfo {

  private String fullName;
  private String serviceName;
  private SortedMap<Status, Integer> statusMap = new TreeMap<>();
  private SortedMap<Health, Integer> healthMap = new TreeMap<>();

  public ServiceInstancesInfo(String fullName, ServiceType service) {
    this.fullName = fullName;
    this.serviceName = service.toString();
  }

  public String getFullName() {
    return fullName;
  }

  public String getServiceName() {
    return serviceName;
  }

  public Health getServiceHealth() {
    if (!healthMap.containsKey(Health.Bad)) {
      return Health.Good;
    }
    return Health.Bad;
  }

  public String[] getStatusEntries() {
    return statusMap.entrySet().toArray(new String[statusMap.size()]);
  }

  public Integer getStatusCount(Status status) {
    return statusMap.get(status);
  }

  public SortedMap<Status, Integer> getStatusMap() {
    return statusMap;
  }

  public SortedMap<Health, Integer> getHealthMap() {
    return healthMap;
  }

  public void addInstanceInfo(Status status, Health health) {
    if (statusMap.containsKey(status)) {
      statusMap.put(status, statusMap.get(status) + 1);
    } else {
      statusMap.put(status, 1);
    }
    if (healthMap.containsKey(health)) {
      Integer count = healthMap.get(health) + 1;
      healthMap.put(health, count);
    } else {
      healthMap.put(health, 1);
    }
  }

  public Health getOverallHealth() {
    if (healthMap.containsKey(Health.Bad)) {
      return Health.Bad;
    } else {
      return Health.Good;
    }
  }
}
