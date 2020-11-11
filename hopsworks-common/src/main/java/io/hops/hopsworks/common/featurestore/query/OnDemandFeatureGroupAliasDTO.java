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

package io.hops.hopsworks.common.featurestore.query;

import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;

public class OnDemandFeatureGroupAliasDTO {

  private String alias;
  private OnDemandFeaturegroupDTO onDemandFeatureGroup;

  public OnDemandFeatureGroupAliasDTO() {
  }

  public OnDemandFeatureGroupAliasDTO(String alias, OnDemandFeaturegroupDTO onDemandFeatureGroup) {
    this.alias = alias;
    this.onDemandFeatureGroup = onDemandFeatureGroup;
  }

  public OnDemandFeaturegroupDTO getOnDemandFeatureGroup() {
    return onDemandFeatureGroup;
  }

  public void setOnDemandFeatureGroup(OnDemandFeaturegroupDTO onDemandFeatureGroup) {
    this.onDemandFeatureGroup = onDemandFeatureGroup;
  }

  public String getAlias() {
    return alias;
  }

  public void setAlias(String alias) {
    this.alias = alias;
  }
}
