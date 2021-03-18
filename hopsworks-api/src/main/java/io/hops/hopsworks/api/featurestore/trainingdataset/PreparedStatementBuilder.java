/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.featurestore.trainingdataset;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.query.ServingPreparedStatementDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
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
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class PreparedStatementBuilder {
  @EJB
  private TrainingDatasetController trainingDatasetController;

  private URI uri(UriInfo uriInfo, Project project, Featurestore featurestore, TrainingDataset trainingDataset) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featurestore.getId()))
        .path(ResourceRequest.Name.TRAININGDATASETS.toString().toLowerCase())
        .path(Integer.toString(trainingDataset.getId()))
        .path(ResourceRequest.Name.PREPAREDSTATEMENTS.toString().toLowerCase()).build();
  }

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.PREPAREDSTATEMENTS);
  }

  public ServingPreparedStatementDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                                           Users user, Featurestore featurestore, Integer trainingDatasetId)
      throws FeaturestoreException {

    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);

    List<ServingPreparedStatementDTO> servingPreparedStatementDTOs =
        trainingDatasetController.getPreparedStatementDTO(trainingDataset, project, user);

    ServingPreparedStatementDTO servingPreparedStatementDTO = new ServingPreparedStatementDTO();
    servingPreparedStatementDTO.setHref(uri(uriInfo, project, featurestore, trainingDataset));
    servingPreparedStatementDTO.setExpand(expand(resourceRequest));
    if (servingPreparedStatementDTO.isExpand()) {
      servingPreparedStatementDTO.setItems(servingPreparedStatementDTOs);
      servingPreparedStatementDTO.setCount((long) servingPreparedStatementDTOs.size());
    }
    return servingPreparedStatementDTO;
  }
}
