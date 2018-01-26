package io.hops.hopsworks.common.admin.llap;

import javax.xml.bind.annotation.XmlEnum;
import java.io.Serializable;
import java.util.List;

public class LlapClusterStatus implements Serializable {

  private Status clusterStatus;
  private List<String> hosts;

  // Cluster spec - default values
  private Integer instanceNumber = 1;
  private Long executorsMemory = 1024L;
  private Long cacheMemory = 1024L;
  private Integer executorsPerInstance = 1;
  private Integer IOThreadsPerInstance = 1;

  public LlapClusterStatus() { }

  public LlapClusterStatus(Status clusterStatus, List<String> hosts,
                           Integer instanceNumber, Long executorsMemory,
                           Long cacheMemory, Integer executorsPerInstance,
                           Integer IOThreadsPerInstance) {
    this.clusterStatus = clusterStatus;
    this.hosts = hosts;
    this.instanceNumber = instanceNumber;
    this.executorsMemory = executorsMemory;
    this.cacheMemory = cacheMemory;
    this.executorsPerInstance = executorsPerInstance;
    this.IOThreadsPerInstance = IOThreadsPerInstance;
  }

  public Status getClusterStatus() { return clusterStatus; }

  public void setClusterStatus(Status clusterStatus) {
    this.clusterStatus = clusterStatus;
  }

  public List<String> getHosts() { return hosts; }

  public void setHosts(List<String> hosts) {
    this.hosts = hosts;
  }

  public Integer getInstanceNumber() { return instanceNumber; }

  public void setInstanceNumber(Integer instanceNumber) {
    this.instanceNumber = instanceNumber;
  }

  public Long getExecutorsMemory() { return executorsMemory; }

  public void setExecutorsMemory(Long executorsMemory) {
    this.executorsMemory = executorsMemory;
  }

  public Long getCacheMemory() { return cacheMemory; }

  public void setCacheMemory(Long cacheMemory) {
    this.cacheMemory = cacheMemory;
  }

  public Integer getExecutorsPerInstance() { return executorsPerInstance; }

  public void setExecutorsPerInstance(Integer executorsPerInstance) {
    this.executorsPerInstance = executorsPerInstance;
  }

  public Integer getIOThreadsPerInstance() {
    return IOThreadsPerInstance;
  }

  public void setIOThreadsPerInstance(Integer IOThreadsPerInstance) {
    this.IOThreadsPerInstance = IOThreadsPerInstance;
  }

  @XmlEnum
  public enum Status {
    DOWN("down"),
    UP("up"),
    LAUNCHING("launching");

    private final String readable;

    private Status(String readable) { this.readable = readable; }

    @Override
    public String toString() {
      return readable;
    }
  }
}
