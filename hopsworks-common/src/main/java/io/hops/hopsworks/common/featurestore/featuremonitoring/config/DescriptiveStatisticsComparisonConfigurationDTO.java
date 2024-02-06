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
package io.hops.hopsworks.common.featurestore.featuremonitoring.config;

import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.descriptivestatistics.MetricDescriptiveStatistics;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DescriptiveStatisticsComparisonConfigurationDTO {
  private Integer id;
  private MetricDescriptiveStatistics metric;
  private Double threshold;
  private Boolean relative;
  private Boolean strict;
}