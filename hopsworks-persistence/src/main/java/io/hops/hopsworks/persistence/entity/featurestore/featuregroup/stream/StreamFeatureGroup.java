/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 *  Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.stream;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeature;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeatureExtraConstraints;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

/**
 * Entity class representing the stream_feature_group table in Hopsworks database.
 * An instance of this class represents a row in the database.
 */
@Entity
@Table(name = "stream_feature_group", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "StreamFeatureGroup.findAll", query = "SELECT streamFg FROM " +
                "StreamFeatureGroup streamFg"),
        @NamedQuery(name = "StreamFeatureGroup.findById",
                query = "SELECT streamFg FROM StreamFeatureGroup streamFg WHERE streamFg.id = :id")
        })

public class StreamFeatureGroup  implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  // This is an hack to have something to write in the table. Without this column, the insert will be
  // empty and will fail. We don't need this table, but removing it requires re-wiring a several other
  // tables.
  // stream feature groups can't have a time travel format other than hudi, hence why we don't have a
  // getter/setter for this field. This is here purely to not make JPA fail.
  @Basic(optional = false)
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "timetravel_format")
  private TimeTravelFormat timeTravelFormat = TimeTravelFormat.HUDI;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "streamFeatureGroup")
  private Collection<CachedFeatureExtraConstraints> featuresExtraConstraints;
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "streamFeatureGroup")
  private Collection<CachedFeature> cachedFeatures;
  
  public StreamFeatureGroup() {};

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }
  
  public Collection<CachedFeature> getCachedFeatures() {
    return cachedFeatures;
  }
  
  public void setCachedFeatures(Collection<CachedFeature> cachedFeatures) {
    this.cachedFeatures = cachedFeatures;
  }
  
  public Collection<CachedFeatureExtraConstraints> getFeaturesExtraConstraints() {
    return featuresExtraConstraints;
  }

  public void setFeaturesExtraConstraints(Collection<CachedFeatureExtraConstraints> featuresExtraConstraints) {
    this.featuresExtraConstraints = featuresExtraConstraints;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StreamFeatureGroup that = (StreamFeatureGroup) o;

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
