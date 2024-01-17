/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.statistics;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hops.hopsworks.common.api.RestDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonTypeName("featureDescriptiveStatisticsDTO")
public class FeatureDescriptiveStatisticsDTO extends RestDTO<FeatureDescriptiveStatisticsDTO> {
  private Integer id;
  private String featureType;
  private String featureName;
  
  // for any feature type
  private Long count;
  private Double completeness;
  private Long numNonNullValues;
  private Long numNullValues;
  private Long approxNumDistinctValues;
  
  // for numerical features
  private Double min;
  private Double max;
  private Double sum;
  private Double mean;
  private Double stddev;
  private List<Double> percentiles;
  
  // with exact uniqueness
  private Double distinctness;
  private Double entropy;
  private Double uniqueness;
  private Long exactNumDistinctValues;
  
  // histogram, correlations, kll <- from hdfs file
  private String extendedStatistics;
}