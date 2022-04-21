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
import io.hops.hopsworks.api.featurestore.tag.FeatureStoreTagUri;
import io.hops.hopsworks.api.tags.TagBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.online.OnlineFeaturegroupController;
import io.hops.hopsworks.common.featurestore.keyword.KeywordControllerIface;
import io.hops.hopsworks.common.featurestore.keyword.KeywordDTO;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
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
  private FsQueryBuilder queryBuilder;
  @EJB
  private OnlineFeaturegroupController onlineFeaturegroupController;
  @Inject
  private KeywordControllerIface keywordControllerIface;
  @EJB
  private FeaturestoreKeywordBuilder featurestoreKeywordBuilder;
  @EJB
  private TagBuilder tagsBuilder;

  public FeatureViewBuilder() {
  }

  public FeatureViewDTO build(List<FeatureView> featureViews, ResourceRequest resourceRequest, Project project,
      Users user, UriInfo uriInfo)
      throws FeaturestoreException, ServiceException, IOException, MetadataException, DatasetException,
      SchematizedTagException {
    FeatureViewDTO featureViewDTO = new FeatureViewDTO();
    featureViewDTO.setHref(uriInfo.getRequestUri());

    if (featureViews.size() == 1) {
      return buildSingle(featureViews.get(0), resourceRequest, project, user, uriInfo);
    }

    for (FeatureView featureView : featureViews) {
      FeatureViewDTO featureViewItem = buildSingle(featureView, resourceRequest, project, user, uriInfo);
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

  private FeatureViewDTO buildSingle(FeatureView featureView, ResourceRequest resourceRequest, Project project,
      Users user, UriInfo uriInfo) throws FeaturestoreException, ServiceException, IOException, MetadataException,
      DatasetException, SchematizedTagException {
    FeatureViewDTO base = convertToDTO(featureView);
    if (resourceRequest != null) {
      if (resourceRequest.contains(ResourceRequest.Name.QUERY)) {
        // For the overview page of UI
        base.setQueryString(queryBuilder.build(uriInfo, project, user, featureView));
      }
      if (resourceRequest.contains(ResourceRequest.Name.FEATURES)) {
        base.setFeatures(makeFeatures(featureView, project));
      }
      if (resourceRequest.contains(ResourceRequest.Name.KEYWORDS)) {
        // TODO feature view: revisit after implementation of keyword endpoint
        List<String> keywords = keywordControllerIface.getAll(project, user, featureView);

        ResourceRequest keywordResourceRequest = new ResourceRequest(ResourceRequest.Name.KEYWORDS);
        KeywordDTO dto = featurestoreKeywordBuilder.build(uriInfo, keywordResourceRequest, project,
          featureView, keywords);
        base.setKeywords(dto);
      }
      //TODO feature view: revisit after implementation of tag endpoint
      //TODO add correct feature view path
      DatasetPath path = null;
      FeatureStoreTagUri tagUri = new FeatureStoreTagUri(uriInfo, featureView.getFeaturestore().getId(),
        ResourceRequest.Name.FEATUREVIEW, featureView.getId());
      base.setTags(tagsBuilder.build(tagUri, resourceRequest, user, path));
    }
    return base;
  }
  
  public FeatureViewDTO convertToDTO(FeatureView featureView) {
    FeatureViewDTO featureViewDTO = new FeatureViewDTO();
    featureViewDTO.setFeaturestoreId(featureView.getFeaturestore().getId());
    featureViewDTO.setFeaturestoreName(featureView.getFeaturestore().getProject().getName());
    featureViewDTO.setDescription(featureView.getDescription());
    featureViewDTO.setCreated(featureView.getCreated());
    featureViewDTO.setCreator(new UserDTO(featureView.getCreator()));
    featureViewDTO.setVersion(featureView.getVersion());
    featureViewDTO.setName(featureView.getName());
    featureViewDTO.setId(featureView.getId());
    featureViewDTO.setLabel(featureView.getLabel());
    return featureViewDTO;
  }

  private List<TrainingDatasetFeatureDTO> makeFeatures(FeatureView featureView, Project project) {
    List<TrainingDatasetFeature> tdFeatures = featureViewController.getFeaturesSorted(featureView.getFeatures());
    Map<Integer, String> fsLookupTable = trainingDatasetController.getFsLookupTableFeatures(tdFeatures);
    return tdFeatures
        .stream()
        .map(f -> new TrainingDatasetFeatureDTO(trainingDatasetController.checkPrefix(f), f.getType(),
            f.getFeatureGroup() != null ?
                new FeaturegroupDTO(f.getFeatureGroup().getFeaturestore().getId(),
                    fsLookupTable.get(f.getFeatureGroup().getFeaturestore().getId()),
                    f.getFeatureGroup().getId(), f.getFeatureGroup().getName(),
                    f.getFeatureGroup().getVersion(),
                    onlineFeaturegroupController.onlineFeatureGroupTopicName(project.getId(),
                        f.getFeatureGroup().getId(), Utils.getFeaturegroupName(f.getFeatureGroup())))
                : null,
            f.getIndex(), f.isLabel()))
        .collect(Collectors.toList());
  }
}
