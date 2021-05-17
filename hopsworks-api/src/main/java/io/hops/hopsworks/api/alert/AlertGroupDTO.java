/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.alert;

import io.hops.hopsworks.alerting.api.alert.dto.AlertGroup;
import io.hops.hopsworks.alerting.api.alert.dto.ReceiverName;
import io.hops.hopsworks.common.api.RestDTO;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlertGroupDTO extends RestDTO<AlertGroupDTO> {
  private Map<String, String> labels;
  private ReceiverName receiver;
  private List<AlertDTO> alertDTOs;
  
  public AlertGroupDTO() {
  }
  
  public AlertGroupDTO(AlertGroup alertGroup) {
    this.labels = alertGroup.getLabels();
    this.receiver = alertGroup.getReceiver();
    this.alertDTOs = alertGroup.getAlerts().stream().map(AlertDTO::new).collect(Collectors.toList());
  }
  
  public Map<String, String> getLabels() {
    return labels;
  }
  
  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }
  
  public ReceiverName getReceiver() {
    return receiver;
  }
  
  public void setReceiver(ReceiverName receiver) {
    this.receiver = receiver;
  }
  
  public List<AlertDTO> getAlertDTOs() {
    return alertDTOs;
  }
  
  public void setAlertDTOs(List<AlertDTO> alertDTOs) {
    this.alertDTOs = alertDTOs;
  }
  
  @Override
  public String toString() {
    return "AlertGroupDTO{" +
        "labels=" + labels +
        ", receiver=" + receiver +
        ", alertDTOs=" + alertDTOs +
        '}';
  }
}
