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
package io.hops.hopsworks.persistence.entity.featurestore.alert;

import io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Objects;

@MappedSuperclass
public class FeatureStoreAlert {
  
  public FeatureStoreAlert(Integer id, FeatureStoreAlertStatus status, AlertType alertType, AlertSeverity severity,
    AlertReceiver receiver, Date created) {
    this.id = id;
    this.status = status;
    this.alertType = alertType;
    this.severity = severity;
    this.receiver = receiver;
    this.created = created;
  }
  
  public FeatureStoreAlert() {
  }
  
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
  private FeatureStoreAlertStatus status;
  
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
  
  @JoinColumn(name = "receiver",
    referencedColumnName = "id")
  @ManyToOne(optional = false)
  private AlertReceiver receiver;
  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public FeatureStoreAlertStatus getStatus() {
    return status;
  }
  
  public void setStatus(
    FeatureStoreAlertStatus status) {
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
  
  public AlertReceiver getReceiver() {
    return receiver;
  }
  
  public void setReceiver(AlertReceiver receiver) {
    this.receiver = receiver;
  }
  
  public Date getCreated() {
    return created;
  }
  
  public void setCreated(Date created) {
    this.created = created;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof FeatureStoreAlert)) {
      return false;
    }
    FeatureStoreAlert that = (FeatureStoreAlert) o;
    return Objects.equals(id, that.id) && status == that.status && alertType == that.alertType &&
      severity == that.severity && Objects.equals(receiver, that.receiver) &&
      Objects.equals(created, that.created);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id, status, alertType, severity, receiver, created);
  }
  
  @Override
  public String toString() {
    return "FeatureStoreAlert{" +
      "id=" + id +
      ", status=" + status +
      ", alertType=" + alertType +
      ", severity=" + severity +
      ", receiver=" + receiver +
      ", created=" + created +
      '}';
  }
}