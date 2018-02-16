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
