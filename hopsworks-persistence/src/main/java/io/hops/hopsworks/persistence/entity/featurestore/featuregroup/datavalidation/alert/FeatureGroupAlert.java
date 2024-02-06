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
package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert;

import io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlert;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlertStatus;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

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
@Table(name = "feature_group_alert",
  catalog = "hopsworks",
  schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "FeatureGroupAlert.findByFeatureGroupAndId",
    query
      = "SELECT f FROM FeatureGroupAlert f WHERE f.featureGroup = :featureGroup AND f.id = :id")
  ,
  @NamedQuery(name = "FeatureGroupAlert.findByFeatureGroupAndStatus",
    query
      = "SELECT f FROM FeatureGroupAlert f WHERE f.featureGroup = :featureGroup AND f.status = :status")
  })
public class FeatureGroupAlert extends FeatureStoreAlert implements Serializable {
  
  public FeatureGroupAlert(Integer id, FeatureStoreAlertStatus status, AlertType alertType, AlertSeverity severity,
    Date created, Featuregroup featureGroup, AlertReceiver receiver) {
    super(id, status, alertType, severity, receiver, created);
    this.featureGroup = featureGroup;
  }
  
  public FeatureGroupAlert() {
  
  }
  @JoinColumn(name = "feature_group_id",
    referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Featuregroup featureGroup;
  
  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }

  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FeatureGroupAlert)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    FeatureGroupAlert that = (FeatureGroupAlert) o;
    return Objects.equals(featureGroup, that.featureGroup);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), featureGroup);
  }
  
  @Override
  public String toString() {
    return "FeatureGroupAlert{" +
      "featureGroup=" + featureGroup +
      '}';
  }
}