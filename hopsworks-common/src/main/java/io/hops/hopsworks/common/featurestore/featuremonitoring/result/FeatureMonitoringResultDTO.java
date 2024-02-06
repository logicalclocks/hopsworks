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

package io.hops.hopsworks.common.featurestore.featuremonitoring.result;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hops.hopsworks.common.api.RestDTO;
import lombok.Getter;
import lombok.Setter;
import io.hops.hopsworks.common.featurestore.statistics.FeatureDescriptiveStatisticsDTO;

@JsonTypeName("featureMonitoringResultDTO")
@Getter
@Setter
public class FeatureMonitoringResultDTO extends RestDTO<FeatureMonitoringResultDTO> {
  private Integer id;
  private Integer configId;
  private String featureName;
  private Integer featureStoreId;
  private Integer executionId;
  private Integer detectionStatisticsId;
  private Integer referenceStatisticsId;
  
  private Boolean shiftDetected;
  private Double difference;
  private Double specificValue;
  private Long monitoringTime;
  private Boolean raisedException;
  private Boolean emptyDetectionWindow;
  private Boolean emptyReferenceWindow;
  
  // with statistics expansion
  private FeatureDescriptiveStatisticsDTO detectionStatistics;
  private FeatureDescriptiveStatisticsDTO referenceStatistics;
}