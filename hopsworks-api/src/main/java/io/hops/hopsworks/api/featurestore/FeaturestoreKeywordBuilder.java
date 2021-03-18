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

package io.hops.hopsworks.api.featurestore;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreKeywordBuilder {

  private URI uri(UriInfo uriInfo, Project project) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(ResourceRequest.Name.KEYWORDS.toString().toLowerCase())
        .build();
  }

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
        .path(ResourceRequest.Name.KEYWORDS.toString().toLowerCase())
        .build();
  }

  private URI uri(UriInfo uriInfo, Project project,
                  Featurestore featurestore, TrainingDataset trainingDataset) {
    return uri(uriInfo, project, featurestore)
        .path(ResourceRequest.Name.FEATUREGROUPS.toString().toLowerCase())
        .path(Integer.toString(trainingDataset.getId()))
        .path(ResourceRequest.Name.KEYWORDS.toString().toLowerCase())
        .build();
  }

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.KEYWORDS);
  }

  public KeywordDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, List<String> keywords) {
    KeywordDTO dto = new KeywordDTO();
    dto.setHref(uri(uriInfo, project));

    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      dto.setKeywords(keywords);
    }
    return dto;
  }

  public KeywordDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                          Featuregroup featureGroup, TrainingDataset trainingDataset, List<String> keywords) {
    KeywordDTO dto = new KeywordDTO();
    if (featureGroup != null) {
      dto.setHref(uri(uriInfo, project, featureGroup.getFeaturestore(), featureGroup));
    } else {
      dto.setHref(uri(uriInfo, project, trainingDataset.getFeaturestore(), trainingDataset));
    }

    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      dto.setKeywords(keywords);
    }
    return dto;
  }
}
