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

import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;


@Entity
@Table(name = "feature_group_alert",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "FeatureGroupAlert.findAll",
      query
      = "SELECT f FROM FeatureGroupAlert f")
  ,
    @NamedQuery(name = "FeatureGroupAlert.findById",
      query
      = "SELECT f FROM FeatureGroupAlert f WHERE f.id = :id")
  ,
    @NamedQuery(name = "FeatureGroupAlert.findByFeatureGroupAndId",
      query
        = "SELECT f FROM FeatureGroupAlert f WHERE f.featureGroup = :featureGroup AND f.id = :id")
  ,
    @NamedQuery(name = "FeatureGroupAlert.findByStatus",
      query
      = "SELECT f FROM FeatureGroupAlert f WHERE f.status = :status")
  ,
    @NamedQuery(name = "FeatureGroupAlert.findByFeatureGroupAndStatus",
      query
      = "SELECT f FROM FeatureGroupAlert f WHERE f.featureGroup = :featureGroup AND f.status = :status")
  ,
    @NamedQuery(name = "FeatureGroupAlert.findByType",
      query
      = "SELECT f FROM FeatureGroupAlert f WHERE f.alertType = :alertType")
  ,
    @NamedQuery(name = "FeatureGroupAlert.findBySeverity",
      query
      = "SELECT f FROM FeatureGroupAlert f WHERE f.severity = :severity")
  ,
    @NamedQuery(name = "FeatureGroupAlert.findByCreated",
      query
      = "SELECT f FROM FeatureGroupAlert f WHERE f.created = :created")})
public class FeatureGroupAlert implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 45)
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private ValidationRuleAlertStatus status;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 45)
  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private AlertType alertType;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 45)
  @Column(name = "severity")
  @Enumerated(EnumType.STRING)
  private AlertSeverity severity;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @JoinColumn(name = "feature_group_id",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Featuregroup featureGroup;

  public FeatureGroupAlert() {
  }

  public FeatureGroupAlert(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
  
  public ValidationRuleAlertStatus getStatus() {
    return status;
  }
  
  public void setStatus(ValidationRuleAlertStatus status) {
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
  
  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }
  
  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }
  
  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }
  
  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof FeatureGroupAlert)) {
      return false;
    }
    FeatureGroupAlert other = (FeatureGroupAlert) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "FeatureGroupExpectationAlert[ id=" + id + " ]";
  }

}
