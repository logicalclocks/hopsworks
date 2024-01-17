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
package io.hops.hopsworks.api.featurestore.statistics;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.statistics.SplitStatisticsDTO;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsController;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsDTO;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsFilters;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeatureGroupStatistics;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.TrainingDatasetStatistics;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.split.SplitName;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class StatisticsBuilder {

  @EJB
  private StatisticsController statisticsController;
  @EJB
  private FeatureDescriptiveStatisticsBuilder featureDescriptiveStatisticsBuilder;

  private UriBuilder uriQueryParams(UriBuilder uriBuilder, ResourceRequest resourceRequest, Set<String> featureNames) {
    // feature names
    if (featureNames != null && !featureNames.isEmpty()) {
      uriBuilder.queryParam("feature_names", String.join(",", featureNames));
    }
    // filters
    if (resourceRequest.getFilter() != null && !resourceRequest.getFilter().isEmpty()) {
      for (AbstractFacade.FilterBy filterBy : resourceRequest.getFilter()) {
        uriBuilder.queryParam("filter_by", filterBy.getValue().toLowerCase() + ":" + filterBy.getParam().toLowerCase());
      }
    }
    // field
    if (resourceRequest.getField() != null && !resourceRequest.getField().isEmpty()) {
      uriBuilder.queryParam("fields", String.join(",", resourceRequest.getField()));
    }
    // offset and limit
    if (resourceRequest.getOffset() != null) {
      uriBuilder.queryParam("offset", resourceRequest.getOffset());
    }
    if (resourceRequest.getLimit() != null) {
      uriBuilder.queryParam("limit", resourceRequest.getLimit());
    }
    return uriBuilder;
  }
  
  private UriBuilder uri(UriInfo uriInfo, Project project, Featurestore featurestore) {
    return uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase()).path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase()).path(Integer.toString(featurestore.getId()));
  }
  
  private URI uri(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Featurestore featurestore,
      Featuregroup featuregroup, Set<String> featureNames) {
    UriBuilder uriBuilder = uri(uriInfo, project, featurestore)
      .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase()).path(Integer.toString(featuregroup.getId()))
      .path(ResourceRequest.Name.STATISTICS.toString().toLowerCase());
    return uriQueryParams(uriBuilder, resourceRequest, featureNames).build();
  }
  
  private URI uri(UriInfo uriInfo, Project project, Featurestore featurestore, Featuregroup featuregroup,
      Long computationTime) {
    String commitTimeEq = StatisticsFilters.Filters.COMPUTATION_TIME_EQ.getValue().toLowerCase();
    return uri(uriInfo, project, featurestore)
      .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase()).path(Integer.toString(featuregroup.getId()))
      .path(ResourceRequest.Name.STATISTICS.toString().toLowerCase())
      .queryParam("filter_by", commitTimeEq + ":" + computationTime)
      .queryParam("fields", "content")
      .queryParam("offset", 0L)
      .queryParam("limit", 1L)
      .build();
  }
  
  private URI uri(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Featurestore featurestore,
      TrainingDataset trainingDataset, Set<String> featureNames) {
    FeatureView featureView = trainingDataset.getFeatureView();
    UriBuilder uriBuilder = uri(uriInfo, project, featurestore)
      .path(ResourceRequest.Name.FEATUREVIEW.toString().toLowerCase())
      .path(featureView.getName())
      .path(ResourceRequest.Name.VERSION.toString().toLowerCase())
      .path(Integer.toString(featureView.getVersion()))
      .path(ResourceRequest.Name.TRAININGDATASETS.toString().toLowerCase())
      .path(Integer.toString(trainingDataset.getVersion()))
      .path(ResourceRequest.Name.STATISTICS.toString().toLowerCase());
    return uriQueryParams(uriBuilder, resourceRequest, featureNames).build();
  }
  
  private URI uri(UriInfo uriInfo, Project project, Featurestore featurestore, TrainingDataset trainingDataset,
      Long computationTime) {
    String commitTimeEq = StatisticsFilters.Filters.COMPUTATION_TIME_EQ.getValue().toLowerCase();
    FeatureView featureView = trainingDataset.getFeatureView();
    return uri(uriInfo, project, featurestore)
      .path(ResourceRequest.Name.FEATUREVIEW.toString().toLowerCase())
      .path(featureView.getName())
      .path(ResourceRequest.Name.VERSION.toString().toLowerCase())
      .path(Integer.toString(featureView.getVersion()))
      .path(ResourceRequest.Name.TRAININGDATASETS.toString().toLowerCase())
      .path(Integer.toString(trainingDataset.getVersion()))
      .path(ResourceRequest.Name.STATISTICS.toString().toLowerCase())
      .queryParam("filter_by", commitTimeEq + ":" + computationTime)
      .queryParam("fields", "content")
      .queryParam("offset", 0L)
      .queryParam("limit", 1L)
      .build();
  }
  
  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.STATISTICS);
  }
  
  // Feature Group
  
  public StatisticsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Users user,
    Featuregroup featuregroup, FeatureGroupStatistics statistics) throws FeaturestoreException {
    StatisticsDTO dto = new StatisticsDTO();
    Long computationTime = statistics.getComputationTime().getTime();
    dto.setHref(uri(uriInfo, project, featuregroup.getFeaturestore(), featuregroup, computationTime));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      dto.setComputationTime(computationTime);
      dto.setRowPercentage(statistics.getRowPercentage());
      dto.setFeatureGroupId(statistics.getFeatureGroup().getId());
      if (statistics.getWindowStartCommitTime() != null) {
        dto.setWindowStartCommitTime(statistics.getWindowStartCommitTime());
      }
      if (statistics.getWindowEndCommitTime() != null) {
        dto.setWindowEndCommitTime(statistics.getWindowEndCommitTime());
      }
      if (resourceRequest.getField() != null && resourceRequest.getField().contains("content")) {
        statisticsController.appendExtendedStatistics(project, user, statistics.getFeatureDescriptiveStatistics());
        dto.setFeatureDescriptiveStatistics(
          featureDescriptiveStatisticsBuilder.buildMany(statistics.getFeatureDescriptiveStatistics()));
      }
    }

    return dto;
  }

  public StatisticsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Users user,
    Featurestore featurestore, Featuregroup featuregroup, Set<String> featureNames) throws FeaturestoreException {
    StatisticsDTO dto = new StatisticsDTO();
    dto.setHref(uri(uriInfo, resourceRequest, project, featurestore, featuregroup, featureNames));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo;
      if (featureNames != null && !featureNames.isEmpty()) {
        // if a set of feature names is provided, filter statistics by feature name. It returns feature group
        // statistics with the filtered feature descriptive statistics
        collectionInfo = statisticsController.getStatisticsByFeatureGroupAndFeatureNames(resourceRequest.getOffset(),
          resourceRequest.getLimit(), resourceRequest.getSort(), resourceRequest.getFilter(), featureNames,
          featuregroup);
      } else {
        // otherwise, get all feature descriptive statistics by feature group.
        collectionInfo = statisticsController.getStatisticsByFeatureGroup(resourceRequest.getOffset(),
          resourceRequest.getLimit(),  resourceRequest.getSort(), resourceRequest.getFilter(), featuregroup);
      }
      dto.setCount(collectionInfo.getCount());
      for (Object s : collectionInfo.getItems()) {
        dto.addItem(build(uriInfo, resourceRequest, project, user, featuregroup, (FeatureGroupStatistics) s));
      }
    }
    return dto;
  }
  
  // Training Dataset
  
  public StatisticsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Users user,
      TrainingDataset trainingDataset, TrainingDatasetStatistics statistics) throws FeaturestoreException {
    Long computationTime = statistics.getComputationTime().getTime();
    StatisticsDTO dto = new StatisticsDTO();
    dto.setHref(uri(uriInfo, project, trainingDataset.getFeaturestore(), trainingDataset, computationTime));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      dto.setComputationTime(statistics.getComputationTime().getTime());
      dto.setRowPercentage(statistics.getRowPercentage());
      dto.setFeatureViewName(trainingDataset.getFeatureView().getName());
      dto.setFeatureViewVersion(trainingDataset.getFeatureView().getVersion());
      dto.setTrainingDatasetVersion(trainingDataset.getVersion());
      
      // check whether to include feature descriptive statistics
      if (resourceRequest.getField() != null && resourceRequest.getField().contains("content")) {
        // check whether to include split feature descriptive statistics
        if (statistics.getTestFeatureDescriptiveStatistics() == null
            || statistics.getTestFeatureDescriptiveStatistics().isEmpty()) { // Training dataset without splits
          statisticsController.appendExtendedStatistics(project, user,
            statistics.getTrainFeatureDescriptiveStatistics());
          dto.setFeatureDescriptiveStatistics(
            featureDescriptiveStatisticsBuilder.buildMany(statistics.getTrainFeatureDescriptiveStatistics()));
        } else { // Training dataset with splits
          List<SplitStatisticsDTO> splitStatistics = new ArrayList<>();
          // add train set statistics
          statisticsController.appendExtendedStatistics(project, user,
            statistics.getTrainFeatureDescriptiveStatistics());
          splitStatistics.add(new SplitStatisticsDTO(SplitName.TRAIN.getName(),
            featureDescriptiveStatisticsBuilder.buildMany(statistics.getTrainFeatureDescriptiveStatistics())));
          // add test set statistics
          statisticsController.appendExtendedStatistics(project, user,
            statistics.getTestFeatureDescriptiveStatistics());
          splitStatistics.add(new SplitStatisticsDTO(SplitName.TEST.getName(),
            featureDescriptiveStatisticsBuilder.buildMany(statistics.getTestFeatureDescriptiveStatistics())));
          if (statistics.getValFeatureDescriptiveStatistics() != null
              && !statistics.getValFeatureDescriptiveStatistics().isEmpty()) {
            // add validation set statistics
            statisticsController.appendExtendedStatistics(project, user,
              statistics.getValFeatureDescriptiveStatistics());
            splitStatistics.add(new SplitStatisticsDTO(SplitName.VALIDATION.getName(),
              featureDescriptiveStatisticsBuilder.buildMany(statistics.getValFeatureDescriptiveStatistics())));
          }
          dto.setSplitStatistics(splitStatistics);
        }
      }
    }
    return dto;
  }

  public StatisticsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Users user,
    Featurestore featurestore, TrainingDataset trainingDataset, Set<String> featureNames) throws FeaturestoreException {
    StatisticsDTO dto = new StatisticsDTO();
    dto.setHref(uri(uriInfo, resourceRequest, project, featurestore, trainingDataset, featureNames));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      if (featureNames != null && !featureNames.isEmpty()) {
        // if a set of feature names is provided, filter statistics by feature name. It returns training dataset
        // statistics with the filtered feature descriptive statistics
        AbstractFacade.CollectionInfo collectionInfo =
          statisticsController.getStatisticsByTrainingDatasetAndFeatureNames(resourceRequest.getFilter(), featureNames,
            trainingDataset);
        dto.setCount(collectionInfo.getCount());
        for (Object s : collectionInfo.getItems()) {
          dto.addItem(build(uriInfo, resourceRequest, project, user, trainingDataset, (TrainingDatasetStatistics) s));
        }
      } else {
        // otherwise, get all feature descriptive statistics by training dataset
        AbstractFacade.CollectionInfo collectionInfo =
          statisticsController.getStatisticsByTrainingDataset(resourceRequest.getFilter(), trainingDataset);
        dto.setCount(collectionInfo.getCount());
        for (Object s : collectionInfo.getItems()) {
          dto.addItem(build(uriInfo, resourceRequest, project, user, trainingDataset, (TrainingDatasetStatistics) s));
        }
      }
    }

    return dto;
  }
}
