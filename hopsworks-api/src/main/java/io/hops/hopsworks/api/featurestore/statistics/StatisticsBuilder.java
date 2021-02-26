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
import io.hops.hopsworks.common.featurestore.statistics.FeaturestoreStatisticFacade;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsController;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.FeaturestoreStatistic;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.exceptions.FeaturestoreException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class StatisticsBuilder {

  @EJB
  private FeaturestoreStatisticFacade statisticFacade;
  @EJB
  private StatisticsController statisticsController;

  private UriBuilder uri(UriInfo uriInfo, Project project, Featurestore featurestore) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featurestore.getId()));
  }

  private URI uri(UriInfo uriInfo, Project project,
                  Featurestore featurestore, Featuregroup featuregroup) {
    return uri(uriInfo, project, featurestore)
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(featuregroup.getId()))
        .path(ResourceRequest.Name.STATISTICS.toString().toLowerCase())
        .build();
  }

  private URI uri(UriInfo uriInfo, Project project, Featurestore featurestore,
                  Featuregroup featuregroup, FeaturestoreStatistic statistics) {
    return uri(uriInfo, project, featurestore)
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(featuregroup.getId()))
        .path(ResourceRequest.Name.STATISTICS.toString().toLowerCase())
        .queryParam("filter_by", "commit_time_eq:" + statistics.getCommitTime())
        .queryParam("fields", "content")
        .build();
  }

  private URI uri(UriInfo uriInfo, Project project,
                  Featurestore featurestore, TrainingDataset trainingDataset) {
    return uri(uriInfo, project, featurestore)
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(trainingDataset.getId()))
        .path(ResourceRequest.Name.STATISTICS.toString().toLowerCase())
        .build();
  }

  private URI uri(UriInfo uriInfo, Project project, Featurestore featurestore,
                  TrainingDataset trainingDataset, FeaturestoreStatistic statistics) {
    return uri(uriInfo, project, featurestore)
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(trainingDataset.getId()))
        .path(ResourceRequest.Name.STATISTICS.toString().toLowerCase())
        .queryParam("filter_by", "commit_time_eq:" + statistics.getCommitTime())
        .queryParam("fields", "content")
        .build();
  }

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.STATISTICS);
  }

  public StatisticsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                             Project project, Users user,
                             Featuregroup featuregroup,
                             FeaturestoreStatistic featurestoreStatistic) throws FeaturestoreException {
    StatisticsDTO dto = new StatisticsDTO();
    dto.setHref(uri(uriInfo, project, featuregroup.getFeaturestore(), featuregroup, featurestoreStatistic));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      dto.setCommitTime(featurestoreStatistic.getCommitTime().getTime());
      if (featurestoreStatistic.getFeatureGroupCommit() != null) {
        dto.setFeatureGroupCommitId(
            featurestoreStatistic.getFeatureGroupCommit().getFeatureGroupCommitPK().getCommitId());
      }
      if (resourceRequest.getField() != null && resourceRequest.getField().contains("content")) {
        dto.setContent(statisticsController.readStatisticsContent(project, user, featurestoreStatistic));
      }
    }

    return dto;
  }

  public StatisticsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                             Project project, Users user,
                             TrainingDataset trainingDataset,
                             FeaturestoreStatistic featurestoreStatistic) throws FeaturestoreException {
    StatisticsDTO dto = new StatisticsDTO();
    dto.setHref(uri(uriInfo, project, trainingDataset.getFeaturestore(), trainingDataset, featurestoreStatistic));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      dto.setCommitTime(featurestoreStatistic.getCommitTime().getTime());
      if (resourceRequest.getField() != null && resourceRequest.getField().contains("content")) {
        dto.setContent(statisticsController.readStatisticsContent(project, user, featurestoreStatistic));
      }
    }

    return dto;
  }

  public StatisticsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                             Project project, Users user, Featurestore featurestore, Featuregroup featuregroup)
      throws FeaturestoreException {
    StatisticsDTO dto = new StatisticsDTO();
    dto.setHref(uri(uriInfo, project, featurestore, featuregroup));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = statisticFacade.findByFeaturegroup(resourceRequest.getOffset(),
          resourceRequest.getLimit(),
          resourceRequest.getSort(),
          resourceRequest.getFilter(),
          featuregroup);
      dto.setCount(collectionInfo.getCount());

      for (Object s : collectionInfo.getItems()) {
        dto.addItem(build(uriInfo, resourceRequest, project, user, featuregroup, (FeaturestoreStatistic) s));
      }
    }

    return dto;
  }

  public StatisticsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                             Project project, Users user, Featurestore featurestore, TrainingDataset trainingDataset)
      throws FeaturestoreException {
    StatisticsDTO dto = new StatisticsDTO();
    dto.setHref(uri(uriInfo, project, featurestore, trainingDataset));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = statisticFacade.findByTrainingDataset(resourceRequest.getOffset(),
          resourceRequest.getLimit(),
          resourceRequest.getSort(),
          resourceRequest.getFilter(),
          trainingDataset);
      dto.setCount(collectionInfo.getCount());

      for (Object s : collectionInfo.getItems()) {
        dto.addItem(build(uriInfo, resourceRequest, project, user, trainingDataset, (FeaturestoreStatistic) s));
      }
    }

    return dto;
  }
}
