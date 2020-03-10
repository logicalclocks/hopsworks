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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.online.OnlineFeaturegroup;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Entity class representing the cached_feature_group table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "cached_feature_group", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "CachedFeaturegroup.findAll", query = "SELECT cachedFg FROM " +
      "CachedFeaturegroup cachedFg"),
    @NamedQuery(name = "CachedFeaturegroup.findById",
        query = "SELECT cachedFg FROM CachedFeaturegroup cachedFg WHERE cachedFg.id = :id")})
public class CachedFeaturegroup implements Serializable {
  private static final long serialVersionUID = 1L;
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @Column(name = "offline_feature_group")
  private Long hiveTableId;
  @JoinColumn(name = "online_feature_group", referencedColumnName = "id")
  @OneToOne
  private OnlineFeaturegroup onlineFeaturegroup;

  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  public Long getHiveTableId() {
    return hiveTableId;
  }

  public void setHiveTableId(Long hiveTableId) {
    this.hiveTableId = hiveTableId;
  }

  public Integer getId() {
    return id;
  }
  
  public OnlineFeaturegroup getOnlineFeaturegroup() {
    return onlineFeaturegroup;
  }
  
  public void setOnlineFeaturegroup(
    OnlineFeaturegroup onlineFeaturegroup) {
    this.onlineFeaturegroup = onlineFeaturegroup;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CachedFeaturegroup that = (CachedFeaturegroup) o;

    if (!id.equals(that.id)) return false;
    return hiveTableId.equals(that.hiveTableId);
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + hiveTableId.hashCode();
    return result;
  }
}
