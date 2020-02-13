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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.online;

import com.google.common.base.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Entity class representing the online_feature_group table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "online_feature_group", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "OnlineFeaturegroup.findAll", query = "SELECT onlineFg FROM " +
      "OnlineFeaturegroup onlineFg"),
    @NamedQuery(name = "OnlineFeaturegroup.findById",
        query = "SELECT onlineFg FROM OnlineFeaturegroup onlineFg WHERE onlineFg.id = :id")})
public class OnlineFeaturegroup implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @Column(name = "db_name")
  private String dbName;
  @Basic(optional = false)
  @Column(name = "table_name")
  private String tableName;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Integer getId() {
    return id;
  }
  
  public String getDbName() {
    return dbName;
  }
  
  public void setDbName(String dbName) {
    this.dbName = dbName;
  }
  
  public String getTableName() {
    return tableName;
  }
  
  public void setTableName(String tableName) {
    this.tableName = tableName;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OnlineFeaturegroup)) {
      return false;
    }
    OnlineFeaturegroup that = (OnlineFeaturegroup) o;
    return Objects.equal(id, that.id) &&
      Objects.equal(dbName, that.dbName) &&
      Objects.equal(tableName, that.tableName);
  }
  
  @Override
  public int hashCode() {
    return Objects.hashCode(id, dbName, tableName);
  }
}
