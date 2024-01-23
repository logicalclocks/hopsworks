/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.statistics;

import io.hops.hopsworks.common.featurestore.statistics.FeatureDescriptiveStatisticsDTO;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeatureDescriptiveStatistics;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureDescriptiveStatisticsBuilder {
  
  public FeatureDescriptiveStatisticsDTO build(FeatureDescriptiveStatistics featureDescriptiveStatistics) {
    if (featureDescriptiveStatistics == null) {
      return null;
    }
    FeatureDescriptiveStatisticsDTO dsDto = new FeatureDescriptiveStatisticsDTO();
    
    // TODO: add correct href ?  /results/{id}/...
    
    dsDto.setId(featureDescriptiveStatistics.getId());
    dsDto.setFeatureName(featureDescriptiveStatistics.getFeatureName());
    dsDto.setFeatureType(featureDescriptiveStatistics.getFeatureType());
    dsDto.setFeatureName(featureDescriptiveStatistics.getFeatureName());
    
    dsDto.setCount(featureDescriptiveStatistics.getCount());
    dsDto.setCompleteness(featureDescriptiveStatistics.getCompleteness());
    dsDto.setNumNonNullValues(featureDescriptiveStatistics.getNumNonNullValues());
    dsDto.setNumNullValues(featureDescriptiveStatistics.getNumNullValues());
    dsDto.setApproxNumDistinctValues(featureDescriptiveStatistics.getApproxNumDistinctValues());
    
    dsDto.setMin(featureDescriptiveStatistics.getMin());
    dsDto.setMax(featureDescriptiveStatistics.getMax());
    dsDto.setSum(featureDescriptiveStatistics.getSum());
    dsDto.setMean(featureDescriptiveStatistics.getMean());
    dsDto.setStddev(featureDescriptiveStatistics.getStddev());
    dsDto.setPercentiles(featureDescriptiveStatistics.getPercentiles());
    
    dsDto.setDistinctness(featureDescriptiveStatistics.getDistinctness());
    dsDto.setEntropy(featureDescriptiveStatistics.getEntropy());
    dsDto.setUniqueness(featureDescriptiveStatistics.getUniqueness());
    dsDto.setExactNumDistinctValues(featureDescriptiveStatistics.getExactNumDistinctValues());
    
    dsDto.setExtendedStatistics(featureDescriptiveStatistics.getExtendedStatistics());
    
    return dsDto;
  }
  
  public Collection<FeatureDescriptiveStatisticsDTO> buildMany(Collection<FeatureDescriptiveStatistics> statistics) {
    return statistics.stream().map(this::build).collect(Collectors.toList());
  }
  
  public FeatureDescriptiveStatistics buildFromDTO(FeatureDescriptiveStatisticsDTO descStatsDTO) {
    if (descStatsDTO == null) {
      return null;
    }
    
    FeatureDescriptiveStatistics featureDescriptiveStatistics = new FeatureDescriptiveStatistics();
    
    featureDescriptiveStatistics.setFeatureName(descStatsDTO.getFeatureName());
    featureDescriptiveStatistics.setFeatureType(descStatsDTO.getFeatureType());
    
    featureDescriptiveStatistics.setCount(descStatsDTO.getCount());
    featureDescriptiveStatistics.setCompleteness(descStatsDTO.getCompleteness());
    featureDescriptiveStatistics.setNumNonNullValues(descStatsDTO.getNumNonNullValues());
    featureDescriptiveStatistics.setNumNullValues(descStatsDTO.getNumNullValues());
    featureDescriptiveStatistics.setApproxNumDistinctValues(descStatsDTO.getApproxNumDistinctValues());
    
    featureDescriptiveStatistics.setMin(descStatsDTO.getMin());
    featureDescriptiveStatistics.setMax(descStatsDTO.getMax());
    featureDescriptiveStatistics.setSum(descStatsDTO.getSum());
    featureDescriptiveStatistics.setMean(descStatsDTO.getMean());
    featureDescriptiveStatistics.setStddev(descStatsDTO.getStddev());
    featureDescriptiveStatistics.setPercentiles(descStatsDTO.getPercentiles());
    
    featureDescriptiveStatistics.setDistinctness(descStatsDTO.getDistinctness());
    featureDescriptiveStatistics.setEntropy(descStatsDTO.getEntropy());
    featureDescriptiveStatistics.setUniqueness(descStatsDTO.getUniqueness());
    featureDescriptiveStatistics.setExactNumDistinctValues(descStatsDTO.getExactNumDistinctValues());
    
    featureDescriptiveStatistics.setExtendedStatistics(descStatsDTO.getExtendedStatistics());
    
    return featureDescriptiveStatistics;
  }
  
  public FeatureDescriptiveStatistics buildFromDeequJson(JSONObject statsJson) {
    FeatureDescriptiveStatistics fds = new FeatureDescriptiveStatistics();
    fds.setFeatureName(statsJson.getString("column"));
    
    if (statsJson.has("dataType")) {
      fds.setFeatureType(statsJson.getString("dataType"));
    }
    
    if (statsJson.has("count") && statsJson.getLong("count") == 0) {
      // if empty data, ignore the rest of statistics
      fds.setCount(0L);
      return fds;
    }
    
    // common for all data types
    if (statsJson.has("numRecordsNull")) {
      fds.setNumNullValues(statsJson.getLong("numRecordsNull"));
    }
    if (statsJson.has("numRecordsNonNull")) {
      fds.setNumNonNullValues(statsJson.getLong("numRecordsNonNull"));
    }
    if (statsJson.has("numRecordsNull") && statsJson.has("numRecordsNonNull")) {
      fds.setCount(Long.valueOf(statsJson.getInt("numRecordsNull") + statsJson.getInt("numRecordsNonNull")));
    }
    if (statsJson.has("count")) {
      fds.setCount(statsJson.getLong("count"));
    }
    if (statsJson.has("completeness")) {
      fds.setCompleteness(Float.valueOf(statsJson.getString("completeness")));
    }
    if (statsJson.has("approximateNumDistinctValues")) {
      fds.setApproxNumDistinctValues(statsJson.getLong("approximateNumDistinctValues"));
    }
    
    // commmon for all data types if exact_uniqueness is enabled
    if (statsJson.has("uniqueness")) {
      fds.setUniqueness(Float.valueOf(statsJson.getString("uniqueness")));
    }
    if (statsJson.has("entropy")) {
      fds.setEntropy(Float.valueOf(statsJson.getString("entropy")));
    }
    if (statsJson.has("distinctness")) {
      fds.setDistinctness(Float.valueOf(statsJson.getString("distinctness")));
    }
    if (statsJson.has("exactNumDistinctValues")) {
      fds.setExactNumDistinctValues(statsJson.getLong("exactNumDistinctValues"));
    }
    
    // fractional / integral features
    if (statsJson.has("minimum")) {
      fds.setMin(statsJson.getDouble("minimum"));
    }
    if (statsJson.has("maximum")) {
      fds.setMax(statsJson.getDouble("maximum"));
    }
    if (statsJson.has("sum")) {
      fds.setSum(statsJson.getDouble("sum"));
    }
    if (statsJson.has("mean")) {
      fds.setMean(statsJson.getDouble("mean"));
    }
    if (statsJson.has("stdDev")) {
      fds.setStddev(statsJson.getDouble("stdDev"));
    }
    
    JSONObject extendedStatistics = new JSONObject();
    if (statsJson.has("correlations")) {
      extendedStatistics.append("correlations", statsJson.get("correlations"));
    }
    if (statsJson.has("histogram")) {
      extendedStatistics.append("histogram", statsJson.get("histogram"));
    }
    if (statsJson.has("kll")) {
      extendedStatistics.append("kll", statsJson.get("kll"));
    }
    if (statsJson.has("unique_values")) {
      extendedStatistics.append("unique_values", statsJson.get("unique_values"));
    }
    if (extendedStatistics.length() > 0) {
      fds.setExtendedStatistics(extendedStatistics.toString());
    }
    
    return fds;
  }
  
  public Collection<FeatureDescriptiveStatistics> buildManyFromContent(String content) {
    JSONArray columns = (new JSONObject(content)).getJSONArray("columns");
    List<FeatureDescriptiveStatistics> descStats = new ArrayList<>();
    for (int i = 0; i < columns.length(); i++) {
      JSONObject colStats = (JSONObject) columns.get(i);
      descStats.add(buildFromDeequJson(colStats));
    }
    return descStats;
  }
  
  public Collection<FeatureDescriptiveStatistics> buildManyFromDTO(
    Collection<FeatureDescriptiveStatisticsDTO> statisticsDTOs) {
    return statisticsDTOs.stream().map(this::buildFromDTO).collect(Collectors.toList());
  }
  
  public Collection<FeatureDescriptiveStatistics> buildManyFromContentOrDTO(
    Collection<FeatureDescriptiveStatisticsDTO> fdsDTOs, String content) {
    // For backwards compatibility, we need to check if content field is provided with serialized statistics and
    // parse it to a list of feature descriptive statistics
    return content != null && !content.isEmpty() ? buildManyFromContent(content) : buildManyFromDTO(fdsDTOs);
  }
}