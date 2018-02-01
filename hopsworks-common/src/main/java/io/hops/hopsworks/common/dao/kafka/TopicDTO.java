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
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TopicDTO implements Serializable {

  private String name;

  private Integer numOfReplicas;

  private Integer numOfPartitions;

  private String schemaName;

  private int schemaVersion;

  public TopicDTO() {
  }

  public TopicDTO(String name) {
    this.name = name;
  }

  public TopicDTO(String name, Integer numOfReplicas, Integer numOfPartitions) {
    this.name = name;
    this.numOfReplicas = numOfReplicas;
    this.numOfPartitions = numOfPartitions;
  }

  public TopicDTO(String name, String schemaName, int schemaVersion) {
    this.name = name;
    this.schemaName = schemaName;
    this.schemaVersion = schemaVersion;
  }

  public TopicDTO(String name, Integer numOfReplicas, Integer numOfPartitions,
          String schemaName, int schemaVersion) {
    this.name = name;
    this.numOfReplicas = numOfReplicas;
    this.numOfPartitions = numOfPartitions;
    this.schemaName = schemaName;
    this.schemaVersion = schemaVersion;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getNumOfPartitions() {
    return numOfPartitions;
  }

  public Integer getNumOfReplicas() {
    return numOfReplicas;
  }

  public void setNumOfPartitions(Integer numOfPartitions) {
    this.numOfPartitions = numOfPartitions;
  }

  public void setNumOfReplicas(Integer numOfReplicas) {
    this.numOfReplicas = numOfReplicas;
  }

  public String getSchemaName() {
    return schemaName;
  }

  public int getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaName(String schemaName) {
    this.schemaName = schemaName;
  }

  public void setSchemaVersion(int schemaVersion) {
    this.schemaVersion = schemaVersion;
  }
}
