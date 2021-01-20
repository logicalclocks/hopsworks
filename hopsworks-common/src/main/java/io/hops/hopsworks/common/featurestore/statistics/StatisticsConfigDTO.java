/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticColumn;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticsConfig;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement
public class StatisticsConfigDTO {
  
  private Boolean enabled = true;
  private Boolean histograms = false;
  private Boolean correlations = false;
  private List<String> columns = new ArrayList<>();
  
  public StatisticsConfigDTO() {}
  
  public StatisticsConfigDTO(StatisticsConfig statisticsConfig) {
    this.enabled = statisticsConfig.isDescriptive();
    this.correlations = statisticsConfig.isCorrelations();
    this.histograms = statisticsConfig.isHistograms();
    this.columns = statisticsConfig.getStatisticColumns().stream()
      .map(StatisticColumn::getName).collect(Collectors.toList());
  }
  
  public Boolean getEnabled() {
    return enabled;
  }
  
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
  
  public Boolean getHistograms() {
    return histograms;
  }
  
  public void setHistograms(Boolean histograms) {
    this.histograms = histograms;
  }
  
  public Boolean getCorrelations() {
    return correlations;
  }
  
  public void setCorrelations(Boolean correlations) {
    this.correlations = correlations;
  }
  
  public List<String> getColumns() {
    return columns;
  }
  
  public void setColumns(List<String> columns) {
    this.columns = columns;
  }
  
  @Override
  public String toString() {
    return "StatisticsConfigDTO{" +
      "enabled=" + enabled +
      ", histograms=" + histograms +
      ", correlations=" + correlations +
      ", columns=" + columns +
      '}';
  }
}
