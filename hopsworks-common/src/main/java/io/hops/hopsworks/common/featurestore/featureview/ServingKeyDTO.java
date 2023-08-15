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

package io.hops.hopsworks.common.featurestore.featureview;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.ServingKey;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@JsonTypeName("servingKeyDTO")
public class ServingKeyDTO {
  private String featureName;
  private FeaturegroupDTO featureGroup;
  private String prefix;
  private Boolean required;
  private String joinOn;
  private Integer joinIndex;

  public ServingKeyDTO() {
  }

  public ServingKeyDTO(ServingKey servingKey) {
    this.featureName = servingKey.getFeatureName();
    this.featureGroup = new FeaturegroupDTO(servingKey.getFeatureGroup());
    this.prefix = servingKey.getPrefix();
    this.required = servingKey.getRequired();
    this.joinOn = servingKey.getJoinOn();
    this.joinIndex = servingKey.getJoinIndex();
  }

  public String getFeatureName() {
    return featureName;
  }

  public void setFeatureName(String featureName) {
    this.featureName = featureName;
  }

  public FeaturegroupDTO getFeatureGroup() {
    return featureGroup;
  }

  public void setFeatureGroup(FeaturegroupDTO featureGroup) {
    this.featureGroup = featureGroup;
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
}
