/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
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
import java.util.Objects;

/**
 * Entity class representing the cashed_feature table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "cached_feature_extra_constraints", catalog = "hopsworks")
@XmlRootElement
public class CachedFeatureExtraConstraints implements Serializable {
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
  @Column(name = "primary_column")
  private Boolean primary = false;
  @Column(name = "hudi_precombine_key")
  private Boolean hudiPrecombineKey = false;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public CachedFeatureExtraConstraints() {}

  public CachedFeatureExtraConstraints(CachedFeaturegroup cachedFeaturegroup, String name, Boolean primary,
                                       Boolean hudiPrecombineKey) {
    this.cachedFeaturegroup = cachedFeaturegroup;
    this.name = name;
    this.primary = primary;
    this.hudiPrecombineKey = hudiPrecombineKey;
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

  public Boolean getPrimary() {
    return primary;
  }

  public void setPrimary(Boolean primary) {
    this.primary = primary;
  }

  public Boolean getHudiPrecombineKey() {
    return hudiPrecombineKey;
  }

  public void setHudiPrecombineKey(Boolean hudiPrecombineKey) {
    this.hudiPrecombineKey = hudiPrecombineKey;
  }

  public CachedFeaturegroup getCachedFeaturegroup() {
    return cachedFeaturegroup;
  }

  public void setCachedFeaturegroup(CachedFeaturegroup cachedFeaturegroup) {
    this.cachedFeaturegroup = cachedFeaturegroup;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CachedFeatureExtraConstraints that = (CachedFeatureExtraConstraints) o;

    if (!Objects.equals(id, that.id)) return false;
    if (!Objects.equals(name, that.name)) return false;
    if (!Objects.equals(hudiPrecombineKey, that.hudiPrecombineKey)) return false;
    return Objects.equals(primary, that.primary);
  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (hudiPrecombineKey != null ? hudiPrecombineKey.hashCode() : 0);
    result = 31 * result + (primary != null ? primary.hashCode() : 0);
    return result;
  }
}