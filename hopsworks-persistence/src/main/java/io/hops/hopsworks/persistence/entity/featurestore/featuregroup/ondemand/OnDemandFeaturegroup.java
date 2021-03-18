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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand;

import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

/**
 * Entity class representing the on_demand_feature_group table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "on_demand_feature_group", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "OnDemandFeaturegroup.findAll", query = "SELECT onDmdFg FROM " +
      "OnDemandFeaturegroup onDmdFg"),
    @NamedQuery(name = "OnDemandFeaturegroup.findById",
        query = "SELECT onDmdFg FROM OnDemandFeaturegroup onDmdFg WHERE onDmdFg.id = :id")})
public class OnDemandFeaturegroup implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @Column(name = "query")
  private String query;
  @Column(name = "description")
  private String description;
  @Column(name = "data_format")
  @Enumerated(EnumType.ORDINAL)
  private OnDemandDataFormat dataFormat;
  @Column(name = "path")
  private String path;
  @JoinColumn(name = "connector_id", referencedColumnName = "id")
  private FeaturestoreConnector featurestoreConnector;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "onDemandFeaturegroup")
  private Collection<OnDemandFeature> features;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "onDemandFeaturegroup")
  private Collection<OnDemandOption> options;

  @JoinColumns({
      @JoinColumn(name = "inode_pid", referencedColumnName = "parent_id"),
      @JoinColumn(name = "inode_name", referencedColumnName = "name"),
      @JoinColumn(name = "partition_id", referencedColumnName = "partition_id")})
  @OneToOne(optional = false)
  private Inode inode;
  
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }
  
  public String getQuery() {
    return query;
  }
  
  public void setQuery(String query) {
    this.query = query;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }

  public FeaturestoreConnector getFeaturestoreConnector() {
    return featurestoreConnector;
  }

  public void setFeaturestoreConnector(FeaturestoreConnector featurestoreConnector) {
    this.featurestoreConnector = featurestoreConnector;
  }

  public Collection<OnDemandFeature> getFeatures() {
    return features;
  }
  
  public void setFeatures(Collection<OnDemandFeature> features) {
    this.features = features;
  }

  public Integer getId() {
    return id;
  }

  public Inode getInode() {
    return inode;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }

  public OnDemandDataFormat getDataFormat() {
    return dataFormat;
  }

  public void setDataFormat(OnDemandDataFormat dataFormat) {
    this.dataFormat = dataFormat;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Collection<OnDemandOption> getOptions() {
    return options;
  }

  public void setOptions(Collection<OnDemandOption> options) {
    this.options = options;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OnDemandFeaturegroup that = (OnDemandFeaturegroup) o;

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
