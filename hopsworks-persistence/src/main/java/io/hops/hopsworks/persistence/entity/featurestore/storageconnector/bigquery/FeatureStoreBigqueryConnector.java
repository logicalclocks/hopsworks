/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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


package io.hops.hopsworks.persistence.entity.featurestore.storageconnector.bigquery;

import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "feature_store_bigquery_connector", catalog = "hopsworks")
@XmlRootElement
public class FeatureStoreBigqueryConnector implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;
  
  @Column(name = "parent_project", nullable = false, length = 1000)
  private String parentProject;
  
  @Column(name = "dataset", length = 1000)
  private String dataset;
  
  @Column(name = "query_project", length = 1000)
  private String queryProject;
  
  @Column(name = "materialization_dataset", length = 1000)
  private String materializationDataset;
  
  @Column(name = "query_table", length = 1000)
  private String queryTable;
  
  @Column(name = "arguments", length = 2000)
  private String arguments;
  
  @ManyToOne
  @JoinColumns({
    @JoinColumn(name = "key_inode_pid", referencedColumnName = "parent_id"),
    @JoinColumn(name = "key_inode_name", referencedColumnName = "name"),
    @JoinColumn(name = "key_partition_id", referencedColumnName = "partition_id")
    })
  private Inode keyInode;
  
  public Inode getKeyInode() {
    return keyInode;
  }
  
  public void setKeyInode(Inode hdfsInodes) {
    this.keyInode = hdfsInodes;
  }
  
  public String getQueryTable() {
    return queryTable;
  }
  
  public void setQueryTable(String queryTable) {
    this.queryTable = queryTable;
  }
  
  public String getMaterializationDataset() {
    return materializationDataset;
  }
  
  public void setMaterializationDataset(String materializationDataset) {
    this.materializationDataset = materializationDataset;
  }
  
  public String getQueryProject() {
    return queryProject;
  }
  
  public void setQueryProject(String project) {
    this.queryProject = project;
  }
  
  public String getDataset() {
    return dataset;
  }
  
  public void setDataset(String dataset) {
    this.dataset = dataset;
  }
  
  public String getParentProject() {
    return parentProject;
  }
  
  public void setParentProject(String parentProject) {
    this.parentProject = parentProject;
  }
  
  public Integer getId() {
    return id;
  }
  
  
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public String getArguments() {
    return arguments;
  }
  
  public void setArguments(String arguments) {
    this.arguments = arguments;
  }
  
  public FeatureStoreBigqueryConnector() {
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeatureStoreBigqueryConnector that = (FeatureStoreBigqueryConnector) o;
    return id.equals(that.id) && parentProject.equals(that.parentProject) &&
      Objects.equals(dataset, that.dataset) && Objects.equals(queryProject, that.queryProject) &&
      Objects.equals(materializationDataset, that.materializationDataset) &&
      Objects.equals(queryTable, that.queryTable) && keyInode.equals(that.keyInode);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id, parentProject, dataset, queryProject, materializationDataset, queryTable, keyInode);
  }
}
