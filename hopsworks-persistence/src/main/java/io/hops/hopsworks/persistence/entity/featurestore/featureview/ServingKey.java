/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.featureview;

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "serving_key", catalog = "hopsworks")
public class ServingKey {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;
  @Basic(optional = false)
  @Column(name = "feature_name")
  private String featureName;
  @JoinColumn(name = "feature_group_id", referencedColumnName = "id")
  private Featuregroup featureGroup;
  @Basic(optional = false)
  @Column(name = "prefix")
  private String prefix;
  @Basic(optional = false)
  @Column(name = "required")
  private Boolean required;
  @Basic(optional = true)
  @Column(name = "join_on")
  private String joinOn;
  @Column(name = "join_index")
  private Integer joinIndex;
  @JoinColumn(name = "feature_view_id", referencedColumnName = "id")
  private FeatureView featureView;

  public ServingKey() {
  }

  public ServingKey(Integer id, String featureName,
      Featuregroup featureGroup, String prefix, Boolean required, String joinOn, Integer joinIndex,
      FeatureView featureView) {
    this.id = id;
    this.featureName = featureName;
    this.featureGroup = featureGroup;
    this.prefix = prefix;
    this.required = required;
    this.joinOn = joinOn;
    this.joinIndex = joinIndex;
    this.featureView = featureView;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getFeatureName() {
    return featureName;
  }

  public void setFeatureName(String featureName) {
    this.featureName = featureName;
  }

  public Featuregroup getFeatureGroup() {
    return featureGroup;
  }

  public void setFeatureGroup(Featuregroup featureGroup) {
    this.featureGroup = featureGroup;
  }

  public FeatureView getFeatureView() {
    return featureView;
  }

  public void setFeatureView(FeatureView featureView) {
    this.featureView = featureView;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public String getJoinOn() {
    return joinOn;
  }

  public void setJoinOn(String joinOn) {
    this.joinOn = joinOn;
  }

  public Integer getJoinIndex() {
    return joinIndex;
  }

  public void setJoinIndex(Integer joinIndex) {
    this.joinIndex = joinIndex;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServingKey that = (ServingKey) o;
    return Objects.equals(id, that.id) && Objects.equals(featureName, that.featureName)
        && Objects.equals(featureGroup, that.featureGroup) && Objects.equals(prefix, that.prefix)
        && Objects.equals(required, that.required) && Objects.equals(joinOn, that.joinOn)
        && Objects.equals(joinIndex, that.joinIndex) && Objects.equals(featureView, that.featureView);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, featureName, featureGroup, prefix, required, joinOn, joinIndex, featureView);
  }
}
