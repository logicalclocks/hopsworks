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

package io.hops.hopsworks.api.featurestore.featureview;

import io.hops.hopsworks.api.featurestore.FeaturestoreKeywordBuilder;
import io.hops.hopsworks.api.featurestore.FsQueryBuilder;
import io.hops.hopsworks.api.featurestore.tag.FeatureStoreTagBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.user.UsersDTO;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewDTO;
import io.hops.hopsworks.common.featurestore.featureview.ServingKeyDTO;
import io.hops.hopsworks.common.featurestore.keyword.KeywordDTO;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreKeywordControllerIface;
import io.hops.hopsworks.common.featurestore.metadata.FeatureStoreTagControllerIface;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryBuilder;
import io.hops.hopsworks.common.featurestore.query.QueryController;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.common.featurestore.query.pit.PitJoinController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFilter;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoin;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureViewBuilder {

  @EJB
  private FeatureViewController featureViewController;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private FsQueryBuilder fsQueryBuilder;
  @EJB
  private QueryBuilder queryBuilder;
  @EJB
  private QueryController queryController;
  @EJB
  private OnlineFeaturegroupController onlineFeaturegroupController;
  @Inject
  private FeatureStoreTagControllerIface tagCtrl;
  @Inject
  private FeatureStoreKeywordControllerIface keywordCtrl;
  @EJB
  private FeaturestoreKeywordBuilder featurestoreKeywordBuilder;
  @EJB
  private FeatureStoreTagBuilder tagsBuilder;
  @EJB
  private FeatureViewInputValidator featureViewInputValidator;
  @Inject
  private PitJoinController pitJoinController;

  public FeatureViewBuilder() {
  }

  public FeatureView convertFromDTO(Project project, Featurestore featurestore, Users user,
      FeatureViewDTO featureViewDTO) throws FeaturestoreException {
    featureViewInputValidator.validate(featureViewDTO, project, user);
    FeatureView featureView = new FeatureView();
    featureView.setName(featureViewDTO.getName());
    featureView.setFeaturestore(featurestore);
    featureView.setCreated(featureViewDTO.getCreated() == null ? new Date() : featureViewDTO.getCreated());
    featureView.setCreator(user);
    featureView.setVersion(featureViewDTO.getVersion());
    featureView.setDescription(featureViewDTO.getDescription());
    setQuery(project, user, featureViewDTO.getQuery(), featureView, featureViewDTO.getFeatures());
    featureView.setServingKeys(featureViewController.getServingKeys(project, user, featureView));
    return featureView;
  }

  private void setQuery(Project project, Users user, QueryDTO queryDTO, FeatureView featureView,
      List<TrainingDatasetFeatureDTO> featureDTOs)
      throws FeaturestoreException {
    if (queryDTO != null) {
      Query query = queryController.convertQueryDTO(project, user, queryDTO,
          pitJoinController.isPitEnabled(queryDTO));
      List<TrainingDatasetJoin> tdJoins = trainingDatasetController.collectJoins(query, null, featureView);
      featureView.setJoins(tdJoins);
      List<TrainingDatasetFeature> features = trainingDatasetController.collectFeatures(query, featureDTOs,
          null, featureView, 0, tdJoins, 0);
      featureView.setFeatures(features);
      List<TrainingDatasetFilter> filters = trainingDatasetController.collectFilters(query, featureView);
      featureView.setFilters(filters);
    }
  }

  public FeatureViewDTO build(List<FeatureView> featureViews, ResourceRequest resourceRequest, Project project,
      Users user, UriInfo uriInfo)
      throws FeaturestoreException, ServiceException, MetadataException, DatasetException,
             FeatureStoreMetadataException {
    FeatureViewDTO featureViewDTO = new FeatureViewDTO();
    featureViewDTO.setHref(uriInfo.getRequestUri());

    for (FeatureView featureView : featureViews) {
      FeatureViewDTO featureViewItem = build(featureView, resourceRequest, project, user, uriInfo);
      featureViewItem.setHref(uriInfo.getRequestUriBuilder()
          .path("version")
          .path(featureView.getVersion().toString())
          .build());
      featureViewDTO.addItem(featureViewItem);
    }
    if (featureViews.size() > 1) {
      featureViewDTO.setCount(Long.valueOf(featureViews.size()));
    }
    return featureViewDTO;
  }

  public FeatureViewDTO build(FeatureView featureView, ResourceRequest resourceRequest, Project project,
      Users user, UriInfo uriInfo)
      throws FeaturestoreException, ServiceException, MetadataException, DatasetException,
             FeatureStoreMetadataException {
    FeatureViewDTO base = convertToDTO(featureView);
    if (resourceRequest != null) {
      if (resourceRequest.contains(ResourceRequest.Name.QUERY_STRING)) {
        // For the overview page of UI
        base.setQueryString(fsQueryBuilder.build(uriInfo, project, user, featureView));
      }
      if (resourceRequest.contains(ResourceRequest.Name.QUERY)) {
        Query query = queryController.makeQuery(featureView, project, user, true, false, false, true, true, false);
        base.setQuery(queryBuilder.build(query, featureView.getFeaturestore(), project, user));
      }
      if (resourceRequest.contains(ResourceRequest.Name.FEATURES)) {
        base.setFeatures(makeFeatures(featureView));
      }
      if (resourceRequest.contains(ResourceRequest.Name.KEYWORDS)) {
        List<String> keywords = keywordCtrl.getKeywords(featureView);

        ResourceRequest keywordResourceRequest = new ResourceRequest(ResourceRequest.Name.KEYWORDS);
        KeywordDTO dto = featurestoreKeywordBuilder.build(uriInfo, keywordResourceRequest, project,
            featureView, keywords);
        base.setKeywords(dto);
      }
      base.setTags(tagsBuilder.build(uriInfo, resourceRequest,project.getId(), featureView.getFeaturestore().getId(),
        ResourceRequest.Name.FEATUREVIEW, featureView.getId(), tagCtrl.getTags(featureView)));
    }
    return base;
  }

  public FeatureViewDTO convertToDTO(FeatureView featureView) {
    FeatureViewDTO featureViewDTO = new FeatureViewDTO();
    featureViewDTO.setId(featureView.getId());
    featureViewDTO.setFeaturestoreId(featureView.getFeaturestore().getId());
    featureViewDTO.setFeaturestoreName(featureView.getFeaturestore().getProject().getName());
    featureViewDTO.setDescription(featureView.getDescription());
    featureViewDTO.setCreated(featureView.getCreated());
    featureViewDTO.setCreator(new UsersDTO(featureView.getCreator()));
    featureViewDTO.setVersion(featureView.getVersion());
    featureViewDTO.setName(featureView.getName());
    featureViewDTO.setId(featureView.getId());
    featureViewDTO.setServingKeys(
        featureView.getServingKeys().stream().map(ServingKeyDTO::new).collect(Collectors.toList()));
    return featureViewDTO;
  }

  private List<TrainingDatasetFeatureDTO> makeFeatures(FeatureView featureView) {
    List<TrainingDatasetFeature> tdFeatures = featureViewController.getFeaturesSorted(featureView.getFeatures());
    Map<Integer, String> fsLookupTable = trainingDatasetController.getFsLookupTableFeatures(tdFeatures);
    return tdFeatures
        .stream()
        .map(f -> new TrainingDatasetFeatureDTO(
            trainingDatasetController.checkPrefix(f),
            f.getType(),
            f.getFeatureGroup() != null ?
                new FeaturegroupDTO(f.getFeatureGroup().getFeaturestore().getId(),
                    fsLookupTable.get(f.getFeatureGroup().getFeaturestore().getId()),
                    f.getFeatureGroup().getId(),
                    f.getFeatureGroup().getName(),
                    f.getFeatureGroup().getVersion(),
                    f.getFeatureGroup().isDeprecated())
                : null,
             f.getName(), f.getIndex(), f.isLabel(), f.isInferenceHelperColumn(), f.isTrainingHelperColumn()))
        .collect(Collectors.toList());
  }
}
