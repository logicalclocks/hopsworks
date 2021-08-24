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
package io.hops.hopsworks.persistence.entity.project.alert;

import io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServiceEnum;

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
@Table(name = "project_service_alert",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ProjectServiceAlert.findAll",
      query
      = "SELECT p FROM ProjectServiceAlert p")
  ,
    @NamedQuery(name = "ProjectServiceAlert.findById",
      query
      = "SELECT p FROM ProjectServiceAlert p WHERE p.id = :id")
  ,
    @NamedQuery(name = "ProjectServiceAlert.findByProjectAndId",
      query
      = "SELECT p FROM ProjectServiceAlert p WHERE p.project = :project AND p.id = :id")
  ,
    @NamedQuery(name = "ProjectServiceAlert.findByService",
      query
      = "SELECT p FROM ProjectServiceAlert p WHERE p.service = :service")
  ,
    @NamedQuery(name = "ProjectServiceAlert.findByProjectAndService",
      query
      = "SELECT p FROM ProjectServiceAlert p WHERE p.project = :project AND p.service = :service")
  ,
    @NamedQuery(name = "ProjectServiceAlert.findByStatus",
      query
      = "SELECT p FROM ProjectServiceAlert p WHERE p.status = :status")
  ,
    @NamedQuery(name = "ProjectServiceAlert.findByProjectAndStatus",
      query
      = "SELECT p FROM ProjectServiceAlert p WHERE p.project = :project AND p.status = :status")
  ,
    @NamedQuery(name = "ProjectServiceAlert.findByType",
      query
      = "SELECT p FROM ProjectServiceAlert p WHERE p.alertType = :alertType")
  ,
    @NamedQuery(name = "ProjectServiceAlert.findBySeverity",
      query
      = "SELECT p FROM ProjectServiceAlert p WHERE p.severity = :severity")
  ,
    @NamedQuery(name = "ProjectServiceAlert.findByCreated",
      query
      = "SELECT p FROM ProjectServiceAlert p WHERE p.created = :created")})
public class ProjectServiceAlert implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 32)
  @Column(name = "service")
  @Enumerated(EnumType.STRING)
  private ProjectServiceEnum service;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 45)
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private ProjectServiceAlertStatus status;
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
  @JoinColumn(name = "project_id",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;
  @JoinColumn(name = "receiver",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  private AlertReceiver receiver;

  public ProjectServiceAlert() {
  }
  
  public ProjectServiceAlert(Integer id, ProjectServiceEnum service,
      ProjectServiceAlertStatus status, AlertType alertType,
      AlertSeverity severity, Date created, Project project,
      AlertReceiver receiver) {
    this.id = id;
    this.service = service;
    this.status = status;
    this.alertType = alertType;
    this.severity = severity;
    this.created = created;
    this.project = project;
    this.receiver = receiver;
  }
  
  public ProjectServiceAlert(Integer id) {
    this.id = id;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
  
  public ProjectServiceEnum getService() {
    return service;
  }
  
  public void setService(ProjectServiceEnum service) {
    this.service = service;
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
  
  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }
  
  public Project getProject() {
    return project;
  }
  
  public void setProject(Project project) {
    this.project = project;
  }
  
  public AlertReceiver getReceiver() {
    return receiver;
  }
  
  public void setReceiver(AlertReceiver receiver) {
    this.receiver = receiver;
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
    if (!(object instanceof ProjectServiceAlert)) {
      return false;
    }
    ProjectServiceAlert other = (ProjectServiceAlert) object;
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
