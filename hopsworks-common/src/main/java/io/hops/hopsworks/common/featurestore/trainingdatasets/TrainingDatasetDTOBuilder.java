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

package io.hops.hopsworks.common.featurestore.trainingdatasets;

import com.google.common.collect.Lists;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TrainingDatasetDTOBuilder {

  @EJB
  private TrainingDatasetController trainingDatasetController;

  public TrainingDatasetDTO build(Users user, Project project, TrainingDataset trainingDataset, UriInfo uriInfo) throws
      FeaturestoreException, ServiceException {
    TrainingDatasetDTO trainingDatasetDTO = trainingDatasetController.convertTrainingDatasetToDTO(user, project,
        trainingDataset, true);
    return trainingDatasetDTO;
  }

  public TrainingDatasetDTO build(Users user, Project project, List<TrainingDataset> trainingDatasets,
      UriInfo uriInfo) throws FeaturestoreException, ServiceException {
    TrainingDatasetDTO trainingDatasetDTO = new TrainingDatasetDTO();
    trainingDatasetDTO.setCount((long) trainingDatasets.size());
    trainingDatasetDTO.setHref(uriInfo.getRequestUri());
    trainingDatasetDTO.setItems(Lists.newArrayList());
    for (TrainingDataset trainingDataset: trainingDatasets) {
      TrainingDatasetDTO trainingDatasetDTOItem = build(user, project, trainingDataset, uriInfo);
      trainingDatasetDTOItem.setHref(uriInfo.getRequestUriBuilder()
              .path("version")
              .path(trainingDataset.getVersion().toString())
              .build());
      trainingDatasetDTO.getItems().add(trainingDatasetDTOItem);
    }
    return trainingDatasetDTO;
  }

}
