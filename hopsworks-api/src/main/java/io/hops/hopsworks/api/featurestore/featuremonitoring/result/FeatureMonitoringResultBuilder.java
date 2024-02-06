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
package io.hops.hopsworks.api.featurestore.featuremonitoring.result;

import io.hops.hopsworks.api.featurestore.statistics.FeatureDescriptiveStatisticsBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.featuremonitoring.result.FeatureMonitoringResultDTO;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.result.FeatureMonitoringResult;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ws.rs.core.UriInfo;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.net.URI;
import java.util.Date;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureMonitoringResultBuilder {
  
  @EJB
  private FeatureDescriptiveStatisticsBuilder featureDescriptiveStatisticsBuilder;
  
  public URI uri(UriInfo uriInfo, Project project, Featurestore featureStore) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId())).path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
      .path(Integer.toString(featureStore.getId()))
      .path(ResourceRequest.Name.FEATURE_MONITORING.toString().toLowerCase()).path("result").build();
  }
  
  public FeatureMonitoringResultDTO buildWithStats(UriInfo uri, Project project, Featurestore featurestore,
    FeatureMonitoringResult result) {
    return build(uri, project, featurestore, result, new ResourceRequest(ResourceRequest.Name.STATISTICS));
  }
  
  public FeatureMonitoringResultDTO build(UriInfo uri, Project project, Featurestore featureStore,
    FeatureMonitoringResult result, ResourceRequest resourceRequest) {
    FeatureMonitoringResultDTO resultDto = new FeatureMonitoringResultDTO();
    resultDto.setHref(uri(uri, project, featureStore));
    
    resultDto.setId(result.getId());
    resultDto.setConfigId(result.getFeatureMonitoringConfig().getId());
    resultDto.setFeatureStoreId(featureStore.getId());
    resultDto.setExecutionId(result.getExecutionId());
    resultDto.setDifference(result.getDifference());
    resultDto.setMonitoringTime(result.getMonitoringTime().getTime());
    resultDto.setShiftDetected(result.getShiftDetected());
    resultDto.setFeatureName(result.getFeatureName());
    resultDto.setSpecificValue(result.getSpecificValue());
    resultDto.setRaisedException(result.getRaisedException());
    resultDto.setEmptyDetectionWindow(result.getEmptyDetectionWindow());
    resultDto.setEmptyReferenceWindow(result.getEmptyReferenceWindow());
    
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.STATISTICS)) {
      resultDto.setDetectionStatistics(featureDescriptiveStatisticsBuilder.build(result.getDetectionStatistics()));
      resultDto.setReferenceStatistics(featureDescriptiveStatisticsBuilder.build(result.getReferenceStatistics()));
    } else {
      resultDto.setDetectionStatisticsId(result.getDetectionStatsId());
      resultDto.setReferenceStatisticsId(result.getReferenceStatsId());
    }
    
    return resultDto;
  }
  
  public FeatureMonitoringResultDTO buildMany(UriInfo uri, Project project, Featurestore featureStore,
    AbstractFacade.CollectionInfo<FeatureMonitoringResult> results, ResourceRequest resourceRequest) {
    FeatureMonitoringResultDTO dtos = new FeatureMonitoringResultDTO();
    
    dtos.setCount(results.getCount());
    dtos.setHref(uri(uri, project, featureStore));
    
    dtos.setItems(results.getItems().stream().map(result -> build(uri, project, featureStore, result, resourceRequest))
      .collect(Collectors.toList()));
    
    return dtos;
  }
  
  public FeatureMonitoringResult buildFromDTO(FeatureMonitoringResultDTO dto) {
    FeatureMonitoringResult result = new FeatureMonitoringResult();
    
    result.setExecutionId(dto.getExecutionId());
    result.setShiftDetected(dto.getShiftDetected());
    result.setDifference(dto.getDifference());
    result.setMonitoringTime(new Date(dto.getMonitoringTime()));
    result.setFeatureName(dto.getFeatureName());
    result.setSpecificValue(dto.getSpecificValue());
    result.setRaisedException(dto.getRaisedException());
    result.setEmptyDetectionWindow(dto.getEmptyDetectionWindow());
    result.setEmptyReferenceWindow(dto.getEmptyReferenceWindow());
    
    result.setDetectionStatsId(dto.getDetectionStatisticsId());
    result.setReferenceStatsId(dto.getReferenceStatisticsId());
    
    return result;
  }
}