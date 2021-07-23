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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entity class representing the feature_store_feature table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "on_demand_feature", catalog = "hopsworks")
@XmlRootElement
public class OnDemandFeature implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "on_demand_feature_group_id", referencedColumnName = "id")
  private OnDemandFeaturegroup onDemandFeaturegroup;
  @Basic(optional = false)
  @Column(name = "description")
  private String description;
  @Basic(optional = false)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @Column(name = "type")
  private String type;
  @Basic(optional = false)
  @Column(name = "primary_column")
  private Boolean primary = false;
  @Basic(optional = false)
  @Column(name = "idx")
  private Integer idx;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public OnDemandFeature() {}

  public OnDemandFeature(OnDemandFeaturegroup onDemandFeaturegroup, String name, String type,
                         String description, Boolean primary, Integer idx) {
    this.onDemandFeaturegroup = onDemandFeaturegroup;
    this.name = name;
    this.type = type;
    this.description = description;
    this.primary = primary;
    this.idx = idx;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Boolean getPrimary() {
    return primary;
  }

  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }

  public OnDemandFeaturegroup getOnDemandFeaturegroup() {
    return onDemandFeaturegroup;
  }

  public void setOnDemandFeaturegroup(OnDemandFeaturegroup onDemandFeaturegroup) {
    this.onDemandFeaturegroup = onDemandFeaturegroup;
  }
  
  public Integer getIdx() {
    return idx;
  }
  
  public void setIdx(Integer idx) {
    this.idx = idx;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OnDemandFeature that = (OnDemandFeature) o;

    if (!Objects.equals(id, that.id)) return false;
    if (!Objects.equals(description, that.description)) return false;
    if (!Objects.equals(name, that.name)) return false;
    if (!Objects.equals(type, that.type)) return false;
    if (!Objects.equals(idx, that.idx)) return false;
    return Objects.equals(primary, that.primary);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (primary != null ? primary.hashCode() : 0);
    result = 31 * result + (idx != null ? idx.hashCode() : 0);
    return result;
  }
}
