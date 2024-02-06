/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.persistence.entity.featurestore.featureview.alert;

import io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlert;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlertStatus;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name="feature_view_alert", catalog="hopsworks")
@NamedQueries({
  @NamedQuery(name = "FeatureViewAlert.findByFeatureViewAndId",
    query = "select f from FeatureViewAlert f where f.id = :id and f.featureView = :featureView"),
  @NamedQuery(name = "FeatureViewAlert.findByFeatureViewAndStatus",
    query = "select f from FeatureViewAlert f where f.featureView = :featureView and f.status = :status")
  })
@XmlRootElement
public class FeatureViewAlert extends FeatureStoreAlert implements Serializable {
  
  public FeatureViewAlert(Integer id, FeatureStoreAlertStatus status, AlertType alertType, AlertSeverity severity,
    Date created, AlertReceiver receiver, FeatureView featureView) {
    super(id, status, alertType, severity, receiver, created);
    this.featureView = featureView;
  }
  public FeatureViewAlert() {
  }
  
  @ManyToOne
  @JoinColumn(name = "feature_view_id")
  private FeatureView featureView;
  
  public FeatureView getFeatureView() {
    return featureView;
  }
  
  public void setFeatureView(FeatureView featureView) {
    this.featureView = featureView;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FeatureViewAlert)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    FeatureViewAlert that = (FeatureViewAlert) o;
    return Objects.equals(featureView, that.featureView);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), featureView);
  }
  
  @Override
  public String toString() {
    return "FeatureViewAlert{" +
      "featureView=" + featureView +
      '}';
  }
}