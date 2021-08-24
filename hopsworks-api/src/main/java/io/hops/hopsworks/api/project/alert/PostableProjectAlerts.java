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
package io.hops.hopsworks.api.project.alert;

import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlertStatus;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServiceEnum;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class PostableProjectAlerts {
  private ProjectServiceAlertStatus status;
  private AlertSeverity severity;
  private ProjectServiceEnum service;
  private String receiver;
  private List<PostableProjectAlerts> items;
  
  public PostableProjectAlerts() {
  }
  
  public ProjectServiceAlertStatus getStatus() {
    return status;
  }
  
  public void setStatus(ProjectServiceAlertStatus status) {
    this.status = status;
  }
  
  public AlertSeverity getSeverity() {
    return severity;
  }
  
  public void setSeverity(AlertSeverity severity) {
    this.severity = severity;
  }
  
  public ProjectServiceEnum getService() {
    return service;
  }
  
  public void setService(ProjectServiceEnum service) {
    this.service = service;
  }
  
  public String getReceiver() {
    return receiver;
  }
  
  public void setReceiver(String receiver) {
    this.receiver = receiver;
  }
  
  public List<PostableProjectAlerts> getItems() {
    return items;
  }
  
  public void setItems(List<PostableProjectAlerts> items) {
    this.items = items;
  }
}
