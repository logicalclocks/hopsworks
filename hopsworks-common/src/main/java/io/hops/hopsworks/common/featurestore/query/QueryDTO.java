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

import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Objects;

@XmlRootElement
public class QueryDTO {
  private FeaturegroupDTO leftFeatureGroup;
  private List<FeatureDTO> leftFeatures;

  // Recursively merge QueryDTOs
  private List<JoinDTO> joins;

  public QueryDTO(FeaturegroupDTO leftFeatureGroup, List<FeatureDTO> leftFeatures,
                  List<JoinDTO> joins) {
    this.leftFeatureGroup = leftFeatureGroup;
    this.leftFeatures = leftFeatures;
    this.joins = joins;
  }

  public QueryDTO(FeaturegroupDTO leftFeatureGroup, List<FeatureDTO> leftFeatures) {
    this.leftFeatureGroup = leftFeatureGroup;
    this.leftFeatures = leftFeatures;
  }

  public QueryDTO() {
  }

  public FeaturegroupDTO getLeftFeatureGroup() {
    return leftFeatureGroup;
  }

  public void setLeftFeatureGroup(FeaturegroupDTO leftFeatureGroup) {
    this.leftFeatureGroup = leftFeatureGroup;
  }

  public List<FeatureDTO> getLeftFeatures() {
    return leftFeatures;
  }

  public void setLeftFeatures(List<FeatureDTO> leftFeatures) {
    this.leftFeatures = leftFeatures;
  }

  public List<JoinDTO> getJoins() {
    return joins;
  }

  public void setJoins(List<JoinDTO> joins) {
    this.joins = joins;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    QueryDTO queryDTO = (QueryDTO) o;

    if (!Objects.equals(leftFeatureGroup, queryDTO.leftFeatureGroup))
      return false;
    if (!Objects.equals(leftFeatures, queryDTO.leftFeatures))
      return false;
    return Objects.equals(joins, queryDTO.joins);
  }

  @Override
  public int hashCode() {
    int result = leftFeatureGroup != null ? leftFeatureGroup.hashCode() : 0;
    result = 31 * result + (leftFeatures != null ? leftFeatures.hashCode() : 0);
    result = 31 * result + (joins != null ? joins.hashCode() : 0);
    return result;
  }
}
