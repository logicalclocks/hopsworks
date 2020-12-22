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

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.query.filter.FilterLogicDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;
import java.util.Objects;

@XmlRootElement
public class QueryDTO {
  private FeaturegroupDTO leftFeatureGroup;
  private List<FeatureGroupFeatureDTO> leftFeatures;
  private String leftFeatureGroupStartTime;
  private String leftFeatureGroupEndTime;
  private FilterLogicDTO filter;

  // Recursively merge QueryDTOs
  private List<JoinDTO> joins;

  public QueryDTO(FeaturegroupDTO leftFeatureGroup, List<FeatureGroupFeatureDTO> leftFeatures,
                  String leftFeatureGroupStartTime, String leftFeatureGroupEndTime, List<JoinDTO> joins,
                  FilterLogicDTO filter) {
    this.leftFeatureGroup = leftFeatureGroup;
    this.leftFeatures = leftFeatures;
    this.leftFeatureGroupStartTime = leftFeatureGroupStartTime;
    this.leftFeatureGroupEndTime = leftFeatureGroupEndTime;
    this.joins = joins;
    this.filter = filter;
  }

  public QueryDTO(FeaturegroupDTO leftFeatureGroup, List<FeatureGroupFeatureDTO> leftFeatures,
                  String leftFeatureGroupEndTime, List<JoinDTO> joins) {
    this.leftFeatureGroup = leftFeatureGroup;
    this.leftFeatures = leftFeatures;
    this.leftFeatureGroupEndTime = leftFeatureGroupEndTime;
    this.joins = joins;
  }

  public QueryDTO(FeaturegroupDTO leftFeatureGroup, List<FeatureGroupFeatureDTO> leftFeatures,
                  List<JoinDTO> joins) {
    this.leftFeatureGroup = leftFeatureGroup;
    this.leftFeatures = leftFeatures;
    this.joins = joins;
  }

  public QueryDTO(FeaturegroupDTO leftFeatureGroup, List<FeatureGroupFeatureDTO> leftFeatures) {
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

  public List<FeatureGroupFeatureDTO> getLeftFeatures() {
    return leftFeatures;
  }

  public void setLeftFeatures(List<FeatureGroupFeatureDTO> leftFeatures) {
    this.leftFeatures = leftFeatures;
  }

  public String getLeftFeatureGroupStartTime() {
    return leftFeatureGroupStartTime;
  }

  public void setLeftFeatureGroupStartTime(String leftFeatureGroupStartTime) {
    this.leftFeatureGroupStartTime = leftFeatureGroupStartTime;
  }

  public String getLeftFeatureGroupEndTime() {
    return leftFeatureGroupEndTime;
  }

  public void setLeftFeatureGroupEndTime(String leftFeatureGroupEndTime) {
    this.leftFeatureGroupEndTime = leftFeatureGroupEndTime;
  }

  public List<JoinDTO> getJoins() {
    return joins;
  }

  public void setJoins(List<JoinDTO> joins) {
    this.joins = joins;
  }
  
  public FilterLogicDTO getFilter() {
    return filter;
  }
  
  public void setFilter(FilterLogicDTO filter) {
    this.filter = filter;
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
    if (!Objects.equals(filter, queryDTO.filter))
      return false;
    return Objects.equals(joins, queryDTO.joins);
  }

  @Override
  public int hashCode() {
    int result = leftFeatureGroup != null ? leftFeatureGroup.hashCode() : 0;
    result = 31 * result + (leftFeatures != null ? leftFeatures.hashCode() : 0);
    result = 31 * result + (joins != null ? joins.hashCode() : 0);
    result = 31 * result + (filter != null ? filter.hashCode() : 0);
    return result;
  }
}
