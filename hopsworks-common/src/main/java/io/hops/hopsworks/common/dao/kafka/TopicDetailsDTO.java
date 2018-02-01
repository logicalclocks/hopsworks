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
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TopicDetailsDTO implements Serializable {

  private String name;

  private List<PartitionDetailsDTO> parrtitionDetails;

  public TopicDetailsDTO() {
  }

  public TopicDetailsDTO(String name) {
    this.name = name;
  }

  public TopicDetailsDTO(String topic,
          List<PartitionDetailsDTO> partitionDetails) {
    this.name = topic;
    this.parrtitionDetails = partitionDetails;
  }

  public String getName() {
    return name;
  }

  public List<PartitionDetailsDTO> getPartition() {
    return parrtitionDetails;
  }

  public void setName(String topic) {
    this.name = topic;
  }

  public void setPartition(List<PartitionDetailsDTO> partitionReplicas) {
    this.parrtitionDetails = partitionReplicas;
  }

}
