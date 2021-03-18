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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@Entity
@Table(name = "feature_group_expectation", catalog = "hopsworks")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FeatureGroupRule.findAll",
        query = "SELECT fge FROM FeatureGroupExpectation fge"),
    @NamedQuery(name = "FeatureGroupExpectation.findByFeatureGroupAndExpectation",
        query = "SELECT fge FROM FeatureGroupExpectation fge WHERE " +
            "fge.featuregroup = :featuregroup AND fge.featureStoreExpectation = :featureStoreExpectation")})
public class FeatureGroupExpectation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Featuregroup featuregroup;
  
  @JoinColumn(name = "feature_store_expectation_id", referencedColumnName = "id")
  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  private FeatureStoreExpectation featureStoreExpectation;
  
  public FeatureGroupExpectation(Featuregroup featuregroup,
    FeatureStoreExpectation featureStoreExpectation) {
    this.featuregroup = featuregroup;
    this.featureStoreExpectation = featureStoreExpectation;
  }
  
  public FeatureGroupExpectation() {

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

  public void setFeaturegroup(Featuregroup featureGroup) {
    this.featuregroup = featureGroup;
  }
  
  public FeatureStoreExpectation getFeatureStoreExpectation() {
    return featureStoreExpectation;
  }
  
  public void setFeatureStoreExpectation(
    FeatureStoreExpectation featureStoreExpectation) {
    this.featureStoreExpectation = featureStoreExpectation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FeatureGroupExpectation that = (FeatureGroupExpectation) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
