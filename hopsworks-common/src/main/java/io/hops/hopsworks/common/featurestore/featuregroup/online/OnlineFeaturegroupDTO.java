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

package io.hops.hopsworks.common.featurestore.featuregroup.online;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.online.OnlineFeaturegroup;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * DTO containing the human-readable information of an online feature group in the Hopsworks feature store,
 * can be converted to JSON or XML representation using jaxb.
 */
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnlineFeaturegroupDTO {

  private Integer id;
  private String dbName;
  private String tableName;
  private String tableType;
  private Integer tableRows;
  private Double size;
  
  public OnlineFeaturegroupDTO() {
  }
  
  public OnlineFeaturegroupDTO(OnlineFeaturegroup onlineFeaturegroup) {
    this.id = onlineFeaturegroup.getId();
    this.dbName = onlineFeaturegroup.getDbName();
    this.tableName = onlineFeaturegroup.getTableName();
  }
  
  public OnlineFeaturegroupDTO(Integer id, String dbName, String tableName) {
    this.id = id;
    this.dbName = dbName;
    this.tableName = tableName;
  }
  
  @XmlElement
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  @XmlElement
  public String getDbName() {
    return dbName;
  }
  
  public void setDbName(String dbName) {
    this.dbName = dbName;
  }
  
  @XmlElement
  public String getTableName() {
    return tableName;
  }
  
  public void setTableName(String tableName) {
    this.tableName = tableName;
  }
  
  @XmlElement
  public String getTableType() {
    return tableType;
  }
  
  public void setTableType(String tableType) {
    this.tableType = tableType;
  }
  
  @XmlElement
  public Integer getTableRows() {
    return tableRows;
  }
  
  public void setTableRows(Integer tableRows) {
    this.tableRows = tableRows;
  }
  
  @XmlElement
  public Double getSize() {
    return size;
  }
  
  public void setSize(Double size) {
    this.size = size;
  }
  
  @Override
  public String toString() {
    return "OnlineFeaturegroupDTO{" +
      "id=" + id +
      ", dbName='" + dbName + '\'' +
      ", tableName='" + tableName + '\'' +
      ", tableType='" + tableType + '\'' +
      ", tableRows=" + tableRows +
      ", size=" + size +
      '}';
  }
}
