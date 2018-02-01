/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

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
