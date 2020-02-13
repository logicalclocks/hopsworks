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

package io.hops.hopsworks.persistence.entity.featurestore.statistics.columns;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.Null;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entity class representing the statistic_columns table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "statistic_columns", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "StatisticColumn.findAll", query = "SELECT statscolumn FROM StatisticColumn statscolumn"),
  @NamedQuery(name = "StatisticColumn.findById",
    query = "SELECT statscolumn FROM StatisticColumn statscolumn WHERE statscolumn.id = :id"),
  @NamedQuery(name = "StatisticColumn.findByFeaturegroup",
    query = "SELECT statscolumn FROM StatisticColumn statscolumn WHERE statscolumn.featuregroup =" +
      ":feature_group")})
public class StatisticColumn implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  private Featuregroup featuregroup;
  @Null
  @Column(name = "name")
  @Basic(optional = false)
  private String name;
  
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public Featuregroup getFeaturegroup() {
    return featuregroup;
  }
  
  public void setFeaturegroup(Featuregroup featuregroup) {
    this.featuregroup = featuregroup;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StatisticColumn that = (StatisticColumn) o;
    return Objects.equals(id, that.id) &&
      Objects.equals(featuregroup, that.featuregroup) &&
      Objects.equals(name, that.name);
  }
  
  @Override
  public int hashCode() {
    return Objects.hash(id, featuregroup, name);
  }
}
