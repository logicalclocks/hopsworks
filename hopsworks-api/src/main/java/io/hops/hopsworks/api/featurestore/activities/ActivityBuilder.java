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

package io.hops.hopsworks.api.featurestore.activities;

import io.hops.hopsworks.api.featurestore.commit.CommitBuilder;
import io.hops.hopsworks.api.featurestore.datavalidation.validations.FeatureGroupValidationBuilder;
import io.hops.hopsworks.api.featurestore.statistics.StatisticsBuilder;
import io.hops.hopsworks.api.jobs.JobsBuilder;
import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.activity.ActivityType;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivity;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ActivityBuilder {

  @EJB
  private FeaturestoreActivityFacade activityFacade;
  @EJB
  private JobsBuilder jobsBuilder;
  @EJB
  private CommitBuilder commitBuilder;
  @EJB
  private StatisticsBuilder statisticsBuilder;
  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private FeatureGroupValidationBuilder validationBuilder;


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
        .path(ResourceRequest.Name.ACTIVITIES.toString().toLowerCase())
        .build();
  }

  private URI uri(UriInfo uriInfo, Project project,
                  Featurestore featurestore, TrainingDataset trainingDataset) {
    return uri(uriInfo, project, featurestore)
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(trainingDataset.getId()))
        .path(ResourceRequest.Name.ACTIVITIES.toString().toLowerCase())
        .build();
  }

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.ACTIVITIES);
  }

  public ActivityDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Users user,
                           Featuregroup featuregroup, FeaturestoreActivity featurestoreActivity)
      throws FeaturestoreException {
    ActivityDTO dto = new ActivityDTO();
    dto.setHref(uri(uriInfo, project, featuregroup.getFeaturestore(), featuregroup));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      dto.setType(featurestoreActivity.getType());
      dto.setUser(usersBuilder.build(uriInfo, resourceRequest, featurestoreActivity.getUser()));
      dto.setTimestamp(featurestoreActivity.getEventTime().getTime());

      if (featurestoreActivity.getType() == ActivityType.JOB) {
        dto.setJob(jobsBuilder.build(uriInfo, resourceRequest,
            featurestoreActivity.getExecution().getJob(), featurestoreActivity.getExecution()));
      } else if (featurestoreActivity.getType() == ActivityType.COMMIT) {
        dto.setCommit(commitBuilder.build(uriInfo, resourceRequest, project,
            featuregroup, featurestoreActivity.getCommit()));
      } else if (featurestoreActivity.getType() == ActivityType.STATISTICS) {
        dto.setStatistics(statisticsBuilder.build(uriInfo, resourceRequest, project, user,
            featuregroup, featurestoreActivity.getStatistics()));
      } else if (featurestoreActivity.getType() == ActivityType.VALIDATIONS) {
        dto.setValidations(validationBuilder.build(uriInfo, resourceRequest, user, project,
            featuregroup, featurestoreActivity.getValidation()));
      } else {
        // Metadata change
        String metadataMsg = featurestoreActivity.getActivityMeta().getValue();
        if (featurestoreActivity.getActivityMetaMsg() != null) {
          metadataMsg += " " + featurestoreActivity.getActivityMetaMsg();
        }
        dto.setMetadata(metadataMsg);
      }
    }

    return dto;
  }

  public ActivityDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Users user,
                           TrainingDataset trainingDataset, FeaturestoreActivity featurestoreActivity)
      throws FeaturestoreException {
    ActivityDTO dto = new ActivityDTO();
    dto.setHref(uri(uriInfo, project, trainingDataset.getFeaturestore(), trainingDataset));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      dto.setType(featurestoreActivity.getType());
      dto.setUser(usersBuilder.build(uriInfo, resourceRequest, featurestoreActivity.getUser()));
      dto.setTimestamp(featurestoreActivity.getEventTime().getTime());

      if (featurestoreActivity.getType() == ActivityType.JOB) {
        dto.setJob(jobsBuilder.build(uriInfo, resourceRequest,
            featurestoreActivity.getExecution().getJob(), featurestoreActivity.getExecution()));
      } else if (featurestoreActivity.getType() == ActivityType.STATISTICS) {
        dto.setStatistics(statisticsBuilder.build(uriInfo, resourceRequest, project, user,
            trainingDataset, featurestoreActivity.getStatistics()));
      } else {
        // Metadata change
        String metadataMsg = featurestoreActivity.getActivityMeta().getValue();
        if (featurestoreActivity.getActivityMetaMsg() != null) {
          metadataMsg += " " + featurestoreActivity.getActivityMetaMsg();
        }
        dto.setMetadata(metadataMsg);
      }
    }

    return dto;
  }

  public ActivityDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                           Project project, Users user, Featuregroup featuregroup) throws FeaturestoreException {
    ActivityDTO dto = new ActivityDTO();
    dto.setHref(uri(uriInfo, project, featuregroup.getFeaturestore(), featuregroup));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo<FeaturestoreActivity> collectionInfo =
          activityFacade.findByFeaturegroup(featuregroup,
              resourceRequest.getOffset(),
              resourceRequest.getLimit(),
              resourceRequest.getFilter(),
              resourceRequest.getSort());
      dto.setCount(collectionInfo.getCount());

      for (FeaturestoreActivity featurestoreActivity : collectionInfo.getItems()) {
        dto.addItem(build(uriInfo, resourceRequest, project, user, featuregroup, featurestoreActivity));
      }
    }

    return dto;
  }

  public ActivityDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                           Project project, Users user, TrainingDataset trainingDataset) throws FeaturestoreException {
    ActivityDTO dto = new ActivityDTO();
    dto.setHref(uri(uriInfo, project, trainingDataset.getFeaturestore(), trainingDataset));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo<FeaturestoreActivity> collectionInfo =
          activityFacade.findByTrainingDataset(trainingDataset,
              resourceRequest.getOffset(),
              resourceRequest.getLimit(),
              resourceRequest.getFilter(),
              resourceRequest.getSort());
      dto.setCount(collectionInfo.getCount());

      for (FeaturestoreActivity featurestoreActivity : collectionInfo.getItems()) {
        dto.addItem(build(uriInfo, resourceRequest, project, user, trainingDataset, featurestoreActivity));
      }
    }

    return dto;
  }
}
