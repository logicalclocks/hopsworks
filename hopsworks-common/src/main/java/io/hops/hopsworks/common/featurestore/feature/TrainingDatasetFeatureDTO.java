/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.feature;

import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;

public class TrainingDatasetFeatureDTO {

  private String name;
  private String type;
  private FeaturegroupDTO featuregroup;
  private Integer index;
  private Boolean label = false;

  public TrainingDatasetFeatureDTO() {
  }

  public TrainingDatasetFeatureDTO(String name, String type, FeaturegroupDTO featuregroupDTO, Integer index,
                                   Boolean label) {
    this.name = name;
    this.type = type;
    this.featuregroup = featuregroupDTO;
    this.index = index;
    this.label = label;
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

  public FeaturegroupDTO getFeaturegroup() {
    return featuregroup;
  }

  public void setFeaturegroup(FeaturegroupDTO featuregroupDTO) {
    this.featuregroup = featuregroupDTO;
  }

  public Integer getIndex() {
    return index;
  }

  public void setIndex(Integer index) {
    this.index = index;
  }

  public Boolean getLabel() {
    return label;
  }

  public void setLabel(Boolean label) {
    this.label = label;
  }
}
