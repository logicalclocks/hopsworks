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

package io.hops.hopsworks.common.featurestore.query;

import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;

public class HudiFeatureGroupAliasDTO {

  private String alias;
  private FeaturegroupDTO featureGroup;
  private Long leftFeatureGroupStartTimestamp;
  private Long leftFeatureGroupEndTimestamp;

  public HudiFeatureGroupAliasDTO() {
  }

  public HudiFeatureGroupAliasDTO(String alias, FeaturegroupDTO featureGroup, Long leftFeatureGroupEndTimestamp) {
    this.alias = alias;
    this.featureGroup = featureGroup;
    this.leftFeatureGroupEndTimestamp = leftFeatureGroupEndTimestamp;
  }

  public HudiFeatureGroupAliasDTO(String alias, FeaturegroupDTO featureGroup, Long leftFeatureGroupStartTimestamp,
                                  Long leftFeatureGroupEndTimestamp) {
    this.alias = alias;
    this.featureGroup = featureGroup;
    this.leftFeatureGroupStartTimestamp = leftFeatureGroupStartTimestamp;
    this.leftFeatureGroupEndTimestamp = leftFeatureGroupEndTimestamp;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }

  public FeaturegroupDTO getFeatureGroup() {
    return featureGroup;
  }

  public void setFeatureGroup(FeaturegroupDTO featureGroup) {
    this.featureGroup = featureGroup;
  }

  public Long getLeftFeatureGroupStartTimestamp() {
    return leftFeatureGroupStartTimestamp;
  }

  public void setLeftFeatureGroupStartTimestamp(Long leftFeatureGroupStartTimestamp) {
    this.leftFeatureGroupStartTimestamp = leftFeatureGroupStartTimestamp;
  }

  public Long getLeftFeatureGroupEndTimestamp() {
    return leftFeatureGroupEndTimestamp;
  }

  public void setLeftFeatureGroupEndTimestamp(Long leftFeatureGroupEndTimestamp) {
    this.leftFeatureGroupEndTimestamp = leftFeatureGroupEndTimestamp;
  }
}
