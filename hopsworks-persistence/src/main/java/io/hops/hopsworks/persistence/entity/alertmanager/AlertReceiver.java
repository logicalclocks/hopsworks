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
package io.hops.hopsworks.persistence.entity.alertmanager;

import java.io.Serializable;
import java.util.Collection;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.alert.FeatureViewAlert;
import io.hops.hopsworks.persistence.entity.jobs.description.JobAlert;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlert;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.json.JSONObject;

@Entity
@Table(name = "alert_receiver",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "AlertReceiver.findAll",
      query = "SELECT a FROM AlertReceiver a")
  ,
    @NamedQuery(name = "AlertReceiver.findById",
      query = "SELECT a FROM AlertReceiver a WHERE a.id = :id")
  ,
    @NamedQuery(name = "AlertReceiver.findByName",
      query = "SELECT a FROM AlertReceiver a WHERE a.name = :name")})
public class AlertReceiver implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
      max = 128)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @NotNull
  @Column(name = "config")
  @Convert(converter = ConfigConverter.class)
  private JSONObject config;
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY,
      mappedBy = "receiver")
  private Collection<ProjectServiceAlert> projectServiceAlertCollection;
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY,
      mappedBy = "receiver")
  private Collection<JobAlert> jobAlertCollection;
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY,
      mappedBy = "receiver")
  private Collection<FeatureGroupAlert> featureGroupAlertCollection;
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY,
    mappedBy = "receiver")
  private Collection<FeatureViewAlert> featureViewAlertCollection;

  public AlertReceiver() {
  }

  public AlertReceiver(Integer id) {
    this.id = id;
  }

  public AlertReceiver(String name, JSONObject config) {
    this.name = name;
    this.config = config;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public JSONObject getConfig() {
    return config;
  }

  public void setConfig(JSONObject config) {
    this.config = config;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<ProjectServiceAlert> getProjectServiceAlertCollection() {
    return projectServiceAlertCollection;
  }

  public void setProjectServiceAlertCollection(Collection<ProjectServiceAlert> projectServiceAlertCollection) {
    this.projectServiceAlertCollection = projectServiceAlertCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<JobAlert> getJobAlertCollection() {
    return jobAlertCollection;
  }

  public void setJobAlertCollection(Collection<JobAlert> jobAlertCollection) {
    this.jobAlertCollection = jobAlertCollection;
  }

  @XmlTransient
  @JsonIgnore
  public Collection<FeatureGroupAlert> getFeatureGroupAlertCollection() {
    return featureGroupAlertCollection;
  }

  public void setFeatureGroupAlertCollection(Collection<FeatureGroupAlert> featureGroupAlertCollection) {
    this.featureGroupAlertCollection = featureGroupAlertCollection;
  }
  
  @XmlTransient
  @JsonIgnore
  public Collection<FeatureViewAlert> getFeatureViewAlertCollection() {
    return featureViewAlertCollection;
  }
  
  public void setFeatureViewAlertCollection(
    Collection<FeatureViewAlert> featureViewAlertCollection) {
    this.featureViewAlertCollection = featureViewAlertCollection;
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
    if (!(object instanceof AlertReceiver)) {
      return false;
    }
    AlertReceiver other = (AlertReceiver) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver[ id=" + id + " ]";
  }

}
