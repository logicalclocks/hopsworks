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
