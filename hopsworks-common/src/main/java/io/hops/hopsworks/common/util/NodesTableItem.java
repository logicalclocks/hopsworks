/*
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
 *
 */

package io.hops.hopsworks.common.util;

import java.io.Serializable;

public class NodesTableItem implements Serializable {

  private Integer nodeId;
  private String status;
  private Long upTime;
  private Integer startPhase;
  private Integer configGeneration;

  public NodesTableItem(Integer nodeId, String status, Long uptime,
          Integer startPhase, Integer configGeneration) {

    this.nodeId = nodeId;
    this.status = status;
    this.upTime = uptime;
    this.startPhase = startPhase;
    this.configGeneration = configGeneration;
  }

  public Integer getNodeId() {
    return nodeId;
  }

  public void setNodeId(Integer nodeId) {
    this.nodeId = nodeId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getUpTime() {
    return FormatUtils.timeInSec(upTime);
  }

  public void setUpTime(Long upTime) {
    this.upTime = upTime;
  }

  public Integer getStartPhase() {
    return startPhase;
  }

  public void setStartPhase(Integer startPhase) {
    this.startPhase = startPhase;
  }

  public Integer getConfigGeneration() {
    return configGeneration;
  }

  public void setConfigGeneration(Integer configGeneration) {
    this.configGeneration = configGeneration;
  }

}
