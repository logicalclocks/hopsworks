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

import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlertStatus;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@XmlRootElement
public class FeatureGroupAlertValues {
  private List<FeatureStoreAlertStatus> status;
  private List<AlertType> alertType;
  private List<AlertSeverity> severity;

  public FeatureGroupAlertValues() {
    status = Arrays.asList(FeatureStoreAlertStatus.values());
    severity = Arrays.asList(AlertSeverity.values());
    alertType = new ArrayList<>(Arrays.asList(AlertType.values()));
    alertType.removeIf(a -> a.equals(AlertType.SYSTEM_ALERT));
  }
  
  public List<FeatureStoreAlertStatus> getStatus() {
    return status;
  }
  
  public void setStatus(List<FeatureStoreAlertStatus> status) {
    this.status = status;
  }

  public List<AlertType> getAlertType() {
    return alertType;
  }

  public void setAlertType(List<AlertType> alertType) {
    this.alertType = alertType;
  }

  public List<AlertSeverity> getSeverity() {
    return severity;
  }

  public void setSeverity(List<AlertSeverity> severity) {
    this.severity = severity;
  }
}
