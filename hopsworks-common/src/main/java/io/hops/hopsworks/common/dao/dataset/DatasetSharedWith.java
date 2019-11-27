/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.dao.dataset;

import io.hops.hopsworks.common.dao.project.Project;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
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
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "dataset_shared_with",
    catalog = "hopsworks",
    schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "DatasetSharedWith.findAll",
      query = "SELECT d FROM DatasetSharedWith d")
  ,
    @NamedQuery(name = "DatasetSharedWith.findById",
      query = "SELECT d FROM DatasetSharedWith d WHERE d.id = :id")
  ,
    @NamedQuery(name = "DatasetSharedWith.findByDataset",
      query = "SELECT d FROM DatasetSharedWith d WHERE d.dataset = :dataset")
  ,
    @NamedQuery(name = "DatasetSharedWith.findByProject",
      query = "SELECT d FROM DatasetSharedWith d WHERE d.project = :project")
  ,
    @NamedQuery(name = "DatasetSharedWith.findByProjectAndDataset",
      query = "SELECT d FROM DatasetSharedWith d WHERE d.project = :project AND d.dataset = :dataset")
  ,
    @NamedQuery(name = "DatasetSharedWith.findByAccepted",
      query
      = "SELECT d FROM DatasetSharedWith d WHERE d.accepted = :accepted")})
public class DatasetSharedWith implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @NotNull
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @NotNull
  @Column(name = "accepted")
  private boolean accepted;
  @Basic(optional = false)
  @NotNull
  @Temporal(TemporalType.DATE)
  @Column(name = "shared_on")
  private Date sharedOn;
  @JoinColumn(name = "dataset",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Dataset dataset;
  @JoinColumn(name = "project",
      referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;

  public DatasetSharedWith() {
  }

  public DatasetSharedWith(Integer id) {
    this.id = id;
  }

  public DatasetSharedWith(Project project, Dataset dataset, boolean accepted) {
    this.project = project;
    this.dataset = dataset;
    this.accepted = accepted;
    this.sharedOn = new Date();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public boolean getAccepted() {
    return accepted;
  }

  public void setAccepted(boolean accepted) {
    this.accepted = accepted;
  }
  
  public Date getSharedOn() {
    return sharedOn;
  }
  
  public void setSharedOn(Date sharedOn) {
    this.sharedOn = sharedOn;
  }
  
  public Dataset getDataset() {
    return dataset;
  }

  public void setDataset(Dataset dataset) {
    this.dataset = dataset;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
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
    if (!(object instanceof DatasetSharedWith)) {
      return false;
    }
    DatasetSharedWith other = (DatasetSharedWith) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "io.hops.hopsworks.common.dao.dataset.DatasetSharedWith[ id=" + id + " ]";
  }
  
}
