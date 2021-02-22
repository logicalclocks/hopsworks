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
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.FsQueryDTO;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FsQueryBuilder {

  @EJB
  private ConstructorController constructorController;
  @EJB
  private TrainingDatasetController trainingDatasetController;

  private URI uri(UriInfo uriInfo, Project project) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(ResourceRequest.Name.QUERY.toString().toLowerCase()).build();
  }

  public FsQueryDTO build(UriInfo uriInfo, Project project, Users user, QueryDTO queryDTO)
      throws FeaturestoreException, ServiceException {
    FsQueryDTO dto = constructorController.construct(queryDTO, project, user);
    dto.setHref(uri(uriInfo, project));
    return dto;
  }

  public FsQueryDTO build(UriInfo uriInfo, Project project, Users user, Featurestore featurestore,
                          Integer trainingDatasetId, boolean withLabel)
      throws FeaturestoreException, ServiceException {
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);
    Query query = trainingDatasetController.getQuery(trainingDataset, withLabel, project, user);
    FsQueryDTO dto = constructorController.construct(query, project, user);
    dto.setHref(uri(uriInfo, project));
    return dto;
  }
}
