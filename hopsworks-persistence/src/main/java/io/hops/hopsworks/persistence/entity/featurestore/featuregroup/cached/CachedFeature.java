/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached;

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

/**
 * Entity class representing the cached_feature table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "cached_feature", catalog = "hopsworks")
@XmlRootElement
public class CachedFeature implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @JoinColumn(name = "cached_feature_group_id", referencedColumnName = "id")
  private CachedFeaturegroup cachedFeaturegroup;
  @Basic(optional = false)
  @Column(name = "name")
  private String name;
  @Basic(optional = false)
  @Column(name = "description")
  private String description;
  
  public CachedFeature() {}
  
  public CachedFeature(CachedFeaturegroup cachedFeaturegroup, String name, String description) {
    this.cachedFeaturegroup = cachedFeaturegroup;
    this.name = name;
    this.description = description;
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public CachedFeaturegroup getCachedFeaturegroup() {
    return cachedFeaturegroup;
  }
  
  public void setCachedFeaturegroup(
    CachedFeaturegroup cachedFeaturegroup) {
    this.cachedFeaturegroup = cachedFeaturegroup;
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
  
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    
    CachedFeature that = (CachedFeature) o;
    
    if (!id.equals(that.id)) return false;
    if (!cachedFeaturegroup.equals(that.cachedFeaturegroup)) return false;
    if (!name.equals(that.name)) return false;
    return description.equals(that.description);
  }
  
  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + cachedFeaturegroup.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + description.hashCode();
    return result;
  }
}
