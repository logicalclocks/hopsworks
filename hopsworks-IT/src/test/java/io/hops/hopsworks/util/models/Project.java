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
import java.util.Date;

public class Project {
  private Integer id;
  private String name;
  private String owner;
  private Date created;
  private Date retentionPeriod;
  private Boolean deleted;
  private String paymentType;
  private String pythonVersion;
  private String description;
  private Integer kafkaMaxNumTopics;
  private Date lastQuotaUpdate;
  private Long inodePid;
  private String inodeName;
  private Long partitionId;
  private Boolean condaEnv;
  
  /**
   *   `id` int(11) NOT NULL AUTO_INCREMENT,
   *   `inode_pid` bigint(20) NOT NULL,
   *   `inode_name` varchar(255) COLLATE latin1_general_cs NOT NULL,
   *   `partition_id` bigint(20) NOT NULL,
   *   `projectname` varchar(100) COLLATE latin1_general_cs NOT NULL,
   *   `username` varchar(150) COLLATE latin1_general_cs NOT NULL,
   *   `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
   *   `retention_period` date DEFAULT NULL,
   *   `archived` tinyint(1) DEFAULT '0',
   *   `conda` tinyint(1) DEFAULT '0',
   *   `logs` tinyint(1) DEFAULT '0',
   *   `deleted` tinyint(1) DEFAULT '0',
   *   `python_version` varchar(25) COLLATE latin1_general_cs DEFAULT NULL,
   *   `description` varchar(2000) COLLATE latin1_general_cs DEFAULT NULL,
   *   `payment_type` varchar(255) COLLATE latin1_general_cs NOT NULL DEFAULT 'PREPAID',
   *   `last_quota_update` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
   *   `kafka_max_num_topics` int(11) NOT NULL DEFAULT '100',
   *   `conda_env` tinyint(1) DEFAULT '0',
   */
  public Project(ResultSet rs) throws SQLException {
    this.id = rs.getInt("id");
    this.name = rs.getString("projectname");
    this.owner = rs.getString("username");
    this.created = rs.getDate("created");
    this.retentionPeriod = rs.getDate("retention_period");
    this.deleted = rs.getBoolean("deleted");
    this.paymentType = rs.getString("payment_type");
    this.pythonVersion = rs.getString("python_version");
    this.description = rs.getString("description");
    this.kafkaMaxNumTopics = rs.getInt("kafka_max_num_topics");
    this.lastQuotaUpdate = rs.getDate("last_quota_update");
    this.inodePid = rs.getLong("inode_pid");
    this.inodeName = rs.getString("inode_name");
    this.partitionId = rs.getLong("partition_id");
    this.condaEnv = rs.getBoolean("conda_env");
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
  
  public String getOwner() {
    return owner;
  }
  
  public void setOwner(String owner) {
    this.owner = owner;
  }
  
  public Date getCreated() {
    return created;
  }
  
  public void setCreated(Date created) {
    this.created = created;
  }
  
  public Date getRetentionPeriod() {
    return retentionPeriod;
  }
  
  public void setRetentionPeriod(Date retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
  }
  
  public Boolean getDeleted() {
    return deleted;
  }
  
  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }
  
  public String getPaymentType() {
    return paymentType;
  }
  
  public void setPaymentType(String paymentType) {
    this.paymentType = paymentType;
  }
  
  public String getPythonVersion() {
    return pythonVersion;
  }
  
  public void setPythonVersion(String pythonVersion) {
    this.pythonVersion = pythonVersion;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public Integer getKafkaMaxNumTopics() {
    return kafkaMaxNumTopics;
  }
  
  public void setKafkaMaxNumTopics(Integer kafkaMaxNumTopics) {
    this.kafkaMaxNumTopics = kafkaMaxNumTopics;
  }
  
  public Date getLastQuotaUpdate() {
    return lastQuotaUpdate;
  }
  
  public void setLastQuotaUpdate(Date lastQuotaUpdate) {
    this.lastQuotaUpdate = lastQuotaUpdate;
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
  
  public Boolean getCondaEnv() {
    return condaEnv;
  }
  
  public void setCondaEnv(Boolean condaEnv) {
    this.condaEnv = condaEnv;
  }
  
  @Override
  public String toString() {
    return "Project{" +
      "id=" + id +
      ", name='" + name + '\'' +
      ", owner='" + owner + '\'' +
      ", created=" + created +
      ", retentionPeriod=" + retentionPeriod +
      ", deleted=" + deleted +
      ", paymentType='" + paymentType + '\'' +
      ", pythonVersion='" + pythonVersion + '\'' +
      ", description='" + description + '\'' +
      ", kafkaMaxNumTopics=" + kafkaMaxNumTopics +
      ", lastQuotaUpdate=" + lastQuotaUpdate +
      ", inodePid=" + inodePid +
      ", inodeName='" + inodeName + '\'' +
      ", partitionId=" + partitionId +
      ", condaEnv=" + condaEnv +
      '}';
  }
}
