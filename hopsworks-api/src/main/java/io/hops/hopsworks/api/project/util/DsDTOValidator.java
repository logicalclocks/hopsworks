/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.project.util;

import io.hops.hopsworks.common.dao.dataset.DataSetDTO;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.DatasetException;
import io.hops.hopsworks.common.exception.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.logging.Level;

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
   * in case the dataset has not been found or operations cannot be done on it
   */
  public Dataset validateDTO(Project project, DataSetDTO dto,
                             boolean validatePrjIds) throws DatasetException {
    if (dto == null || dto.getName() == null || dto.getName().
            isEmpty()) {
      throw new IllegalArgumentException("Either dto or dto name were not provided");
    }

    // If the validation of the ProjectId(s) is requested, validate that either
    // ProjectId is set or ProjectIds is set and not empty
    if (validatePrjIds && dto.getProjectId() == null &&
        (dto.getProjectIds() == null || dto.getProjectIds().isEmpty())){
      throw new IllegalArgumentException("Project IDs were not provided");
    }

    // Check if the dataset exists and user can share it
    Dataset ds = datasetFacade.findByNameAndProjectId(project, dto.getName());

    if (ds == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE);
    } else if (ds.isShared() ||
        (ds.isPublicDs() && (!datasetController.getOwningProject(ds).equals(project)))) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_OWNER_ERROR, Level.FINE);
    }

    return ds;
  }
}
