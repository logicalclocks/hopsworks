/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.project.util;

import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.DataSetDTO;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.AppException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.Response;

@Stateless
public class DsDTOValidator {

  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private DatasetController datasetController;

  /**
   * Validate a DataSetDTO object passed by the frontend.
   * It checks that the required fields are included in the object.
   * It retrieves the Dataset object from the database, ensure it exists
   * and that operations can be done on it.
   *
   * @param project the project
   * @param dto the DataSetDTO to check
   * @param validatePrjIds whether the function should ensure the presence of
   *                      projectId or projectIds
   * @return The dataset object
   * @throws AppException In case of problems with DataSetDTO object or
   * in case the dataset has not been found or operations cannot be done on it
   */
  public Dataset validateDTO(Project project, DataSetDTO dto,
                             boolean validatePrjIds) throws AppException {
    if (dto == null || dto.getName() == null || dto.getName().
            isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.DATASET_NAME_EMPTY);
    }

    // If the validation of the ProjectId(s) is requested, validate that either
    // ProjectId is set or ProjectIds is set and not empty
    if (validatePrjIds && dto.getProjectId() == null &&
        (dto.getProjectIds() == null || dto.getProjectIds().isEmpty())){
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_SELECTED);
    }

    // Check if the dataset exists and user can share it
    Dataset ds = datasetFacade.findByNameAndProjectId(project, dto.getName());

    if (ds == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_NOT_FOUND);
    } else if (ds.isShared() ||
        (ds.isPublicDs() && (!datasetController.getOwningProject(ds).equals(project)))) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_OWNER_ERROR);
    }

    return ds;
  }
}
