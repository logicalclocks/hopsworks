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

package io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.external;

import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entity class representing the external_training_dataset table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "external_training_dataset", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ExternalTrainingDataset.findAll", query = "SELECT s3Td FROM " +
      "ExternalTrainingDataset s3Td"),
    @NamedQuery(name = "ExternalTrainingDataset.findById",
        query = "SELECT s3Td FROM ExternalTrainingDataset s3Td WHERE s3Td.id = :id")})
public class ExternalTrainingDataset implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Column(name = "path")
  private String path;
  @JoinColumn(name = "connector_id", referencedColumnName = "id")
  private FeaturestoreConnector featurestoreConnector;
  @JoinColumns({
    @JoinColumn(name = "inode_pid", referencedColumnName = "parent_id"),
    @JoinColumn(name = "inode_name", referencedColumnName = "name"),
    @JoinColumn(name = "partition_id", referencedColumnName = "partition_id")})
  @OneToOne(optional = false)
  private Inode inode;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getPath() { return path; }

  public void setPath(String path) {
    this.path = path;
  }

  public FeaturestoreConnector getFeaturestoreConnector() {
    return featurestoreConnector;
  }

  public void setFeaturestoreConnector(FeaturestoreConnector featurestoreConnector) {
    this.featurestoreConnector = featurestoreConnector;
  }

  public Inode getInode() {
    return inode;
  }

  public void setInode(Inode inode) {
    this.inode = inode;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ExternalTrainingDataset that = (ExternalTrainingDataset) o;

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
