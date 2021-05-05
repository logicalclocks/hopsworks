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

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlertStatus;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServiceEnum;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
public class ProjectAlertsDTO extends RestDTO<ProjectAlertsDTO> {
  private Integer id;
  private ProjectServiceAlertStatus status;
  private AlertType alertType;
  private AlertSeverity severity;
  private ProjectServiceEnum service;
  private Date created;

  public ProjectAlertsDTO() {
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public ProjectServiceAlertStatus getStatus() {
    return status;
  }

  public void setStatus(ProjectServiceAlertStatus status) {
    this.status = status;
  }

  public AlertType getAlertType() {
    return alertType;
  }

  public void setAlertType(AlertType alertType) {
    this.alertType = alertType;
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

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }
}