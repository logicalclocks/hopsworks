/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.trainingdataset;

import com.google.common.collect.Lists;
import io.hops.hopsworks.api.featurestore.FeaturestoreKeywordBuilder;
import io.hops.hopsworks.api.featurestore.tag.FeatureStoreTagBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.keyword.KeywordDTO;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreKeywordControllerIface;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreTagControllerIface;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryBuilder;
import io.hops.hopsworks.common.featurestore.query.QueryController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TrainingDatasetDTOBuilder {

  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private FeatureStoreTagBuilder tagsBuilder;
  @Inject
  private FeatureStoreKeywordControllerIface keywordController;
  @Inject
  private FeatureStoreTagControllerIface tagController;
  @EJB
  private FeaturestoreKeywordBuilder featurestoreKeywordBuilder;
  @EJB
  private QueryController queryController;
  @EJB
  private QueryBuilder queryBuilder;

  public TrainingDatasetDTO build(Users user, Project project, TrainingDataset trainingDataset,
      UriInfo uriInfo,ResourceRequest resourceRequest)
      throws FeaturestoreException, ServiceException, FeatureStoreMetadataException, MetadataException,
             DatasetException, IOException, CloudException {
    TrainingDatasetDTO trainingDatasetDTO = trainingDatasetController.convertTrainingDatasetToDTO(user, project,
        trainingDataset, true);
    if (resourceRequest != null) {
      if (resourceRequest.contains(ResourceRequest.Name.KEYWORDS)) {
        List<String> keywords = keywordController.getKeywords(trainingDataset);
        ResourceRequest keywordResourceRequest = new ResourceRequest(ResourceRequest.Name.KEYWORDS);
        KeywordDTO dto = featurestoreKeywordBuilder.build(uriInfo, keywordResourceRequest, project,
            trainingDataset, keywords);
        trainingDatasetDTO.setKeywords(dto);
      }
      if (resourceRequest.contains(ResourceRequest.Name.TDDATA)) {
        trainingDatasetDTO.setDataAvailable(
            trainingDatasetController.isTrainingDatasetAvailable(trainingDataset, user));
      }
      if (resourceRequest.contains(ResourceRequest.Name.EXTRAFILTER)) {
        FeatureView featureView = trainingDataset.getFeatureView();
        featureView.setFilters(trainingDataset.getFilters());
        Query query = queryController.makeQuery(featureView, project, user, true, true);
        trainingDatasetDTO.setExtraFilter(
            queryBuilder.build(query, trainingDataset.getFeaturestore(), project, user).getFilter()
        );
      }
      if(resourceRequest.contains(ResourceRequest.Name.TAGS)) {
        trainingDatasetDTO.setTags(tagsBuilder.build(uriInfo, resourceRequest, project.getId(),
          trainingDataset.getFeaturestore().getId(), ResourceRequest.Name.TRAININGDATASETS, trainingDataset.getId(),
          tagController.getTags(trainingDataset)));
      }
    }
    return trainingDatasetDTO;
  }

  public TrainingDatasetDTO build(Users user, Project project, List<TrainingDataset> trainingDatasets,
      UriInfo uriInfo, ResourceRequest resourceRequest)
      throws FeaturestoreException, ServiceException, FeatureStoreMetadataException, MetadataException,
             DatasetException, IOException, CloudException {
    TrainingDatasetDTO trainingDatasetDTO = new TrainingDatasetDTO();
    trainingDatasetDTO.setCount((long) trainingDatasets.size());
    trainingDatasetDTO.setHref(uriInfo.getRequestUri());
    trainingDatasetDTO.setItems(Lists.newArrayList());
    for (TrainingDataset trainingDataset: trainingDatasets) {
      TrainingDatasetDTO trainingDatasetDTOItem = build(user, project, trainingDataset, uriInfo, resourceRequest);
      trainingDatasetDTOItem.setHref(uriInfo.getRequestUriBuilder()
              .path("version")
              .path(trainingDataset.getVersion().toString())
              .build());
      trainingDatasetDTO.getItems().add(trainingDatasetDTOItem);
    }
    return trainingDatasetDTO;
  }

}
