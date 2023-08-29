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

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.stream.StreamFeatureGroup;

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
  @JoinColumn(name = "stream_feature_group_id", referencedColumnName = "id")
  private StreamFeatureGroup streamFeatureGroup;
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
  
  public CachedFeature(StreamFeatureGroup streamFeatureGroup, String name, String description) {
    this.streamFeatureGroup = streamFeatureGroup;
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
  
  public StreamFeatureGroup getStreamFeatureGroup() {
    return streamFeatureGroup;
  }
  
  public void setStreamFeatureGroup(
    StreamFeatureGroup streamFeatureGroup) {
    this.streamFeatureGroup = streamFeatureGroup;
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
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CachedFeature that = (CachedFeature) o;

    if (!Objects.equals(id, that.id)) return false;
    if (!Objects.equals(cachedFeaturegroup, that.cachedFeaturegroup))
      return false;
    if (!Objects.equals(streamFeatureGroup, that.streamFeatureGroup))
      return false;
    if (!Objects.equals(name, that.name)) return false;
    return Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (cachedFeaturegroup != null ? cachedFeaturegroup.hashCode() : 0);
    result = 31 * result + (streamFeatureGroup != null ? streamFeatureGroup.hashCode() : 0);
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    return result;
  }
}
