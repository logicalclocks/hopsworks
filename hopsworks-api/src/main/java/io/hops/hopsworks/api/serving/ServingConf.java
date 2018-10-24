/*
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
 */

package io.hops.hopsworks.api.serving;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@ApiModel(value = "Represent configuration for serving UI")
public class ServingConf {

  private Integer maxNumInstances;

  private String kafkaTopicSchema;
  private Integer kafkaTopicVersion;

  public ServingConf() {
  }

  public ServingConf(Integer maxNumInstances, String kafkaTopicSchema, Integer kafkaTopicVersion) {
    this.maxNumInstances = maxNumInstances;
    this.kafkaTopicSchema = kafkaTopicSchema;
    this.kafkaTopicVersion = kafkaTopicVersion;
  }

  @ApiModelProperty(value = "Schema name for the Kafka topic used for logging", readOnly = true)
  public String getKafkaTopicSchema() {
    return kafkaTopicSchema;
  }

  public void setKafkaTopicSchema(String kafkaTopicSchema) {
    this.kafkaTopicSchema = kafkaTopicSchema;
  }

  @ApiModelProperty(value = "Schema version for the Kafka topic used for logging", readOnly = true)
  public Integer getKafkaTopicVersion() {
    return kafkaTopicVersion;
  }

  public void setKafkaTopicVersion(Integer kafkaTopicVersion) {
    this.kafkaTopicVersion = kafkaTopicVersion;
  }

  @ApiModelProperty(value = "Max number of serving instances serving a model", readOnly = true)
  public Integer getMaxNumInstances() {
    return maxNumInstances;
  }

  public void setMaxNumInstances(Integer maxNumInstances) {
    this.maxNumInstances = maxNumInstances;
  }
}
