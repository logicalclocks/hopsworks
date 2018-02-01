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

package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PartitionDetailsDTO implements Serializable {

  private int id;
  private String leader;
  private List<String> replicas = new ArrayList<>();
  private List<String> inSyncReplicas = new ArrayList<>();

  public PartitionDetailsDTO() {
  }

  public PartitionDetailsDTO(int id, String paritionLeader,
      List<String> replicas, List<String> inSyncReplicas) {
    this.id = id;
    this.leader = paritionLeader;
    this.replicas = replicas;
    this.inSyncReplicas = inSyncReplicas;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getLeader() {
    return leader;
  }

  public void setLeader(String leader) {
    this.leader = leader;
  }

  public List<String> getReplicas() {
    return replicas;
  }

  public void setReplicas(List<String> partitionReplicas) {
    this.replicas = partitionReplicas;
  }

  public List<String> getInSyncReplicas() {
    return inSyncReplicas;
  }

  public void setInSyncReplicas(List<String> inSyncReplicas) {
    this.inSyncReplicas = inSyncReplicas;
  }

}
