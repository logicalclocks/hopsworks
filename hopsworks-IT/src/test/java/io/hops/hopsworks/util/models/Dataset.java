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
package io.hops.hopsworks.util.models;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Dataset {
  private Integer id;
  private Long inodePid;
  private String inodeName;
  private Long partitionId;
  private String name;
  private String description;
  private boolean searchable;
  private int publicDs;
  private String publicDsId;
  private String dsType;
  private Integer featureStore;
  private Integer projectId;
  
  /**
   *   `id` int(11) NOT NULL AUTO_INCREMENT,
   *   `inode_pid` bigint(20) NOT NULL,
   *   `inode_name` varchar(255) COLLATE latin1_general_cs NOT NULL,
   *   `partition_id` bigint(20) NOT NULL,
   *   `projectId` int(11) NOT NULL,
   *   `description` varchar(2000) COLLATE latin1_general_cs DEFAULT NULL,
   *   `searchable` tinyint(1) NOT NULL DEFAULT '0',
   *   `public_ds` tinyint(1) NOT NULL DEFAULT '0',
   *   `public_ds_id` varchar(1000) COLLATE latin1_general_cs DEFAULT '0',
   *   `dstype` int(11) NOT NULL DEFAULT '0',
   *   `feature_store_id` int(11) DEFAULT NULL,
   * @param rs
   * @throws SQLException
   */
  public Dataset(ResultSet rs) throws SQLException {
    this.id = rs.getInt("id");
    this.inodePid = rs.getLong("inode_pid");
    this.inodeName = rs.getString("inode_name");
    this.partitionId = rs.getLong("partition_id");
    this.name = rs.getString("inode_name");
    this.description = rs.getString("description");
    this.searchable = rs.getBoolean("searchable");
    this.publicDs = rs.getInt("public_ds");
    this.publicDsId = rs.getString("public_ds_id");
    this.dsType = rs.getString("dstype");
    this.featureStore = rs.getInt("feature_store_id");
    this.projectId = rs.getInt("projectId");
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public Long getInodePid() {
    return inodePid;
  }
  
  public void setInodePid(Long inodePid) {
    this.inodePid = inodePid;
  }
  
  public String getInodeName() {
    return inodeName;
  }
  
  public void setInodeName(String inodeName) {
    this.inodeName = inodeName;
  }
  
  public Long getPartitionId() {
    return partitionId;
  }
  
  public void setPartitionId(Long partitionId) {
    this.partitionId = partitionId;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public boolean isSearchable() {
    return searchable;
  }
  
  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }
  
  public int getPublicDs() {
    return publicDs;
  }
  
  public void setPublicDs(int publicDs) {
    this.publicDs = publicDs;
  }
  
  public String getPublicDsId() {
    return publicDsId;
  }
  
  public void setPublicDsId(String publicDsId) {
    this.publicDsId = publicDsId;
  }
  
  public String getDsType() {
    return dsType;
  }
  
  public void setDsType(String dsType) {
    this.dsType = dsType;
  }
  
  public Integer getFeatureStore() {
    return featureStore;
  }
  
  public void setFeatureStore(Integer featureStore) {
    this.featureStore = featureStore;
  }
  
  public Integer getProjectId() {
    return projectId;
  }
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  @Override
  public String toString() {
    return "Dataset{" +
      "id=" + id +
      ", inodePid=" + inodePid +
      ", inodeName='" + inodeName + '\'' +
      ", partitionId=" + partitionId +
      ", name='" + name + '\'' +
      ", description='" + description + '\'' +
      ", searchable=" + searchable +
      ", publicDs=" + publicDs +
      ", publicDsId='" + publicDsId + '\'' +
      ", dstype='" + dsType + '\'' +
      ", featureStore=" + featureStore +
      ", projectId=" + projectId +
      '}';
  }
}
