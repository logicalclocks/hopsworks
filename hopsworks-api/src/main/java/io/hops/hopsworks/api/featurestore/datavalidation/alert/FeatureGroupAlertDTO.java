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

package io.hops.hopsworks.api.featurestore.datavalidation.alert;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlertStatus;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
public class FeatureGroupAlertDTO extends RestDTO<FeatureGroupAlertDTO> {
  private Integer id;
  private Integer featureGroupId;
  private String featureStoreName;
  private String featureGroupName;
  private FeatureStoreAlertStatus status;
  private AlertType alertType;
  private AlertSeverity severity;
  private String receiver;
  private Date created;

  public FeatureGroupAlertDTO() {
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getFeatureGroupId() {
    return featureGroupId;
  }

  public void setFeatureGroupId(Integer featureGroupId) {
    this.featureGroupId = featureGroupId;
  }

  public String getFeatureStoreName() {
    return featureStoreName;
  }

  public void setFeatureStoreName(String featureStoreName) {
    this.featureStoreName = featureStoreName;
  }

  public String getFeatureGroupName() {
    return featureGroupName;
  }

  public void setFeatureGroupName(String featureGroupName) {
    this.featureGroupName = featureGroupName;
  }
  
  public FeatureStoreAlertStatus getStatus() {
    return status;
  }
  
  public void setStatus(FeatureStoreAlertStatus status) {
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

  public String getReceiver() {
    return receiver;
  }

  public void setReceiver(String receiver) {
    this.receiver = receiver;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }
}