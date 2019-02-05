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

package io.hops.hopsworks.common.serving.tf;

import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.serving.TfServing;

public class TfServingWrapper {
  private TfServing tfServing;

  private TfServingStatusEnum status;
  private Integer availableReplicas;
  private Integer nodePort;

  private TopicDTO kafkaTopicDTO;

  public TfServingWrapper(TfServing tfServing) {
    this.tfServing = tfServing;
  }

  public TfServing getTfServing() {
    return tfServing;
  }

  public void setTfServing(TfServing tfServing) {
    this.tfServing = tfServing;
  }

  public TfServingStatusEnum getStatus() {
    return status;
  }

  public void setStatus(TfServingStatusEnum status) {
    this.status = status;
  }

  public Integer getAvailableReplicas() {
    return availableReplicas;
  }

  public void setAvailableReplicas(Integer availableReplicas) {
    this.availableReplicas = availableReplicas;
  }

  public Integer getNodePort() {
    return nodePort;
  }

  public void setNodePort(Integer nodePort) {
    this.nodePort = nodePort;
  }

  public TopicDTO getKafkaTopicDTO() {
    return kafkaTopicDTO;
  }

  public void setKafkaTopicDTO(TopicDTO kafkaTopicDTO) {
    this.kafkaTopicDTO = kafkaTopicDTO;
  }
}
