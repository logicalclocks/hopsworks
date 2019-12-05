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

import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import org.apache.calcite.sql.JoinType;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Objects;

@XmlRootElement
public class QueryDTO {
  private FeaturegroupDTO leftFeatureGroup;
  private List<FeatureDTO> leftFeatures;

  // Recursively merge QueryDTOs
  private QueryDTO queryDTO;

  // Join with another featuregroup
  private FeaturegroupDTO rightFeatureGroup;
  private List<FeatureDTO> rightFeatures;

  private List<FeatureDTO> on;
  private List<FeatureDTO> leftOn;
  private List<FeatureDTO> rightOn;

  private JoinType type = JoinType.INNER;

  public QueryDTO(FeaturegroupDTO leftFeatureGroup, List<FeatureDTO> leftFeatures, QueryDTO queryDTO,
                  FeaturegroupDTO rightFeatureGroup, List<FeatureDTO> rightFeatures, List<FeatureDTO> on,
                  List<FeatureDTO> leftOn, List<FeatureDTO> rightOn, JoinType type) {
    this.leftFeatureGroup = leftFeatureGroup;
    this.leftFeatures = leftFeatures;
    this.queryDTO = queryDTO;
    this.rightFeatureGroup = rightFeatureGroup;
    this.rightFeatures = rightFeatures;
    this.on = on;
    this.leftOn = leftOn;
    this.rightOn = rightOn;
    this.type = type;
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

  public QueryDTO getQueryDTO() {
    return queryDTO;
  }

  public void setQueryDTO(QueryDTO queryDTO) {
    this.queryDTO = queryDTO;
  }

  public FeaturegroupDTO getRightFeatureGroup() {
    return rightFeatureGroup;
  }

  public void setRightFeatureGroup(FeaturegroupDTO rightFeatureGroup) {
    this.rightFeatureGroup = rightFeatureGroup;
  }

  public List<FeatureDTO> getRightFeatures() {
    return rightFeatures;
  }

  public void setRightFeatures(List<FeatureDTO> rightFeatures) {
    this.rightFeatures = rightFeatures;
  }

  public List<FeatureDTO> getOn() {
    return on;
  }

  public void setOn(List<FeatureDTO> on) {
    this.on = on;
  }

  public List<FeatureDTO> getLeftOn() {
    return leftOn;
  }

  public void setLeftOn(List<FeatureDTO> leftOn) {
    this.leftOn = leftOn;
  }

  public List<FeatureDTO> getRightOn() {
    return rightOn;
  }

  public void setRightOn(List<FeatureDTO> rightOn) {
    this.rightOn = rightOn;
  }

  public JoinType getType() {
    return type;
  }

  public void setType(JoinType type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    QueryDTO queryDTO1 = (QueryDTO) o;

    if (!Objects.equals(leftFeatureGroup, queryDTO1.leftFeatureGroup))
      return false;
    if (!Objects.equals(leftFeatures, queryDTO1.leftFeatures))
      return false;
    if (!Objects.equals(queryDTO, queryDTO1.queryDTO)) return false;
    if (!Objects.equals(rightFeatureGroup, queryDTO1.rightFeatureGroup))
      return false;
    if (!Objects.equals(rightFeatures, queryDTO1.rightFeatures))
      return false;
    if (!Objects.equals(on, queryDTO1.on)) return false;
    if (!Objects.equals(leftOn, queryDTO1.leftOn)) return false;
    if (!Objects.equals(rightOn, queryDTO1.rightOn)) return false;
    return type == queryDTO1.type;
  }

  @Override
  public int hashCode() {
    int result = leftFeatureGroup != null ? leftFeatureGroup.hashCode() : 0;
    result = 31 * result + (leftFeatures != null ? leftFeatures.hashCode() : 0);
    result = 31 * result + (queryDTO != null ? queryDTO.hashCode() : 0);
    result = 31 * result + (rightFeatureGroup != null ? rightFeatureGroup.hashCode() : 0);
    result = 31 * result + (rightFeatures != null ? rightFeatures.hashCode() : 0);
    result = 31 * result + (on != null ? on.hashCode() : 0);
    result = 31 * result + (leftOn != null ? leftOn.hashCode() : 0);
    result = 31 * result + (rightOn != null ? rightOn.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }
}
