/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore;

import io.hops.hopsworks.common.dao.featurestore.storageconnector.hopsfs.FeaturestoreHopsfsConnector;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.s3.FeaturestoreS3Connector;
import io.hops.hopsworks.common.dao.project.Project;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

/**
 * Entity class representing the feature_store table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "feature_store", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Featurestore.findAll", query = "SELECT f FROM Featurestore f"),
    @NamedQuery(name = "Featurestore.findByProject", query = "SELECT f FROM Featurestore f " +
        "WHERE f.project = :project"),
    @NamedQuery(name = "Featurestore.findById", query = "SELECT f FROM Featurestore f WHERE f.id = :id")})
public class Featurestore implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "project_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Project project;
  @Basic(optional = false)
  @Column(name = "created")
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;
  @Basic(optional = false)
  @NotNull
  @Column(name = "hive_db_id")
  private Long hiveDbId;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featurestore")
  private Collection<FeaturestoreJdbcConnector> featurestoreJdbcConnectorConnections;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featurestore")
  private Collection<FeaturestoreS3Connector> featurestoreS3ConnectorConnections;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "featurestore")
  private Collection<FeaturestoreHopsfsConnector> hopsfsConnections;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Long getHiveDbId() {
    return hiveDbId;
  }

  public void setHiveDbId(Long hiveDbId) {
    this.hiveDbId = hiveDbId;
  }
  
  public Collection<FeaturestoreJdbcConnector> getFeaturestoreJdbcConnectorConnections() {
    return featurestoreJdbcConnectorConnections;
  }
  
  public void setFeaturestoreJdbcConnectorConnections(
    Collection<FeaturestoreJdbcConnector> featurestoreJdbcConnectorConnections) {
    this.featurestoreJdbcConnectorConnections = featurestoreJdbcConnectorConnections;
  }
  
  public Collection<FeaturestoreS3Connector> getFeaturestoreS3ConnectorConnections() {
    return featurestoreS3ConnectorConnections;
  }
  
  public void setFeaturestoreS3ConnectorConnections(
    Collection<FeaturestoreS3Connector> featurestoreS3ConnectorConnections) {
    this.featurestoreS3ConnectorConnections = featurestoreS3ConnectorConnections;
  }
  
  public Collection<FeaturestoreHopsfsConnector> getHopsfsConnections() {
    return hopsfsConnections;
  }
  
  public void setHopsfsConnections(
    Collection<FeaturestoreHopsfsConnector> hopsfsConnections) {
    this.hopsfsConnections = hopsfsConnections;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Featurestore)) return false;

    Featurestore that = (Featurestore) o;

    if (id != null)
      if (!id.equals(that.id)) return false;
    if (!project.equals(that.project)) return false;
    if (created != null && that.created != null)
      if (!created.equals(that.created)) return false;
    return hiveDbId.equals(that.hiveDbId);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + project.hashCode();
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + hiveDbId.hashCode();
    return result;
  }
}
