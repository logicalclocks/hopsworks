/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
