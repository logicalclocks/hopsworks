/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.models.version;

import io.hops.hopsworks.persistence.entity.models.Model;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
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

/**
 * A ModelVersion is an instance of a Model.
 */
@Entity
@Table(name = "model_version", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "ModelVersion.findAll",
    query = "SELECT mv FROM ModelVersion mv"),
  @NamedQuery(name = "ModelVersion.findByProjectAndMlId",
    query
      = "SELECT mv FROM ModelVersion mv WHERE mv.modelVersionPK.version = :version" +
      " AND mv.modelVersionPK.modelId = :modelId")
  }
)
public class ModelVersion implements Serializable {

  private static final long serialVersionUID = 1L;

  @EmbeddedId
  private ModelVersionPK modelVersionPK;

  @ManyToOne(optional = false)
  @JoinColumn(name = "model_id",
              referencedColumnName = "id",
              insertable = false,
              updatable = false)
  private Model model;

  @JoinColumn(name = "user_id",
    referencedColumnName = "uid")
  @ManyToOne(optional = false)
  private Users creator;

  @Basic(optional = false)
  @NotNull
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @Size(max = 1000)
  @Column(name = "description")
  private String description;

  @Column(name = "metrics")
  @Convert(converter = ModelMetricsConverter.class)
  private Metrics metrics;

  @Size(max = 1000)
  @Column(name = "program")
  private String program;

  @Size(max = 128)
  @Column(name = "framework")
  private String framework;

  @Size(max = 1000)
  @Column(name = "environment")
  private String environment;

  @Size(max = 128)
  @Column(name = "experiment_id")
  private String experimentId;

  @Size(max = 128)
  @Column(name = "experiment_project_name")
  private String experimentProjectName;

  public ModelVersion() {
  }

  public Metrics getMetrics() {
    return metrics;
  }

  public void setMetrics(Metrics metrics) {
    this.metrics = metrics;
  }

  public String getProgram() {
    return program;
  }

  public void setProgram(String program) {
    this.program = program;
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public String getUserFullName() {
    return this.creator.getFname() + " " + this.creator.getLname();
  }

  public String getFramework() {
    return framework;
  }

  public void setFramework(String framework) {
    this.framework = framework;
  }

  public String getExperimentId() {
    return experimentId;
  }

  public void setExperimentId(String experimentId) {
    this.experimentId = experimentId;
  }

  public String getExperimentProjectName() {
    return experimentProjectName;
  }

  public void setExperimentProjectName(String experimentProjectName) {
    this.experimentProjectName = experimentProjectName;
  }

  public ModelVersionPK getModelVersionPK() {
    return modelVersionPK;
  }

  public void setModelVersionPK(ModelVersionPK modelVersionPK) {
    this.modelVersionPK = modelVersionPK;
  }

  public Model getModel() {
    return model;
  }

  public void setModel(Model model) {
    this.model = model;
  }

  public String getMlId() {
    return model.getName() + "_" + modelVersionPK.getVersion();
  }

  public Users getCreator() {
    return creator;
  }

  public void setCreator(Users creator) {
    this.creator = creator;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (getModelVersionPK() != null ? getModelVersionPK().hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof ModelVersion)) {
      return false;
    }
    ModelVersion other = (ModelVersion) object;
    if ((this.getModelVersionPK() == null && other.getModelVersionPK() != null) ||
      (this.getModelVersionPK() != null && !this.getModelVersionPK().equals(other.getModelVersionPK()))) {
      return false;
    }
    return true;
  }
}

