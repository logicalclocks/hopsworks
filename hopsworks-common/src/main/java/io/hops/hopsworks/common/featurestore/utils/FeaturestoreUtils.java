/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.utils;

import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import java.io.IOException;
import java.util.logging.Level;

@Stateless
public class FeaturestoreUtils {

  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService distributedFsService;
  @EJB
  private ProjectTeamFacade projectTeamFacade;

  /**
   * Verify that the user is allowed to execute the requested operation based on his/hers project role
   * <p>
   * Only data owners are allowed to update/delete feature groups/training datasets
   * created by someone else in the project
   *
   * @param trainingDataset the training dataset the operation concerns
   * @param featurestore the featurestore that the operation concerns
   * @param project the project of the featurestore
   * @param user the user requesting the operation
   * @throws FeaturestoreException
   */
  public void verifyUserRole(TrainingDataset trainingDataset, Featurestore featurestore, Users user, Project project)
      throws FeaturestoreException {
    String userRole = projectTeamFacade.findCurrentRole(project, user);
    if (!trainingDataset.getCreator().equals(user) &&
        !userRole.equalsIgnoreCase(AllowedRoles.DATA_OWNER)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.UNAUTHORIZED_FEATURESTORE_OPERATION, Level.FINE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId() +
              ", Training dataset: " + trainingDataset.getName() + ", userRole:" + userRole +
              ", creator of the featuregroup: " + trainingDataset.getCreator().getEmail());
    }
  }

  /**
   * Verify that the user is allowed to execute the requested operation based on his/hers project role.
   *
   * Only data owners are allowed to delete storage connectors for the feature store
   *
   * @param featurestore the featurestore that the operation concerns
   * @param user the user making the request
   * @param project the project of the featurestore
   * @param storageConnectorDTO the storage connector taht the operation concerns
   * @throws FeaturestoreException
   */
  public void verifyUserRole(Featurestore featurestore, Users user, Project project, FeaturestoreStorageConnectorDTO
      storageConnectorDTO)
      throws FeaturestoreException {
    String userRole = projectTeamFacade.findCurrentRole(project, user);
    if (!userRole.equalsIgnoreCase(AllowedRoles.DATA_OWNER)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.UNAUTHORIZED_FEATURESTORE_OPERATION, Level.FINE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId() +
              ", storageConnectorId: " + storageConnectorDTO.getId() + ", userRole:" + userRole);
    }
  }

  /**
   * Writes a string to a new file in HDFS
   *
   * @param project              project of the user
   * @param user                 user making the request
   * @param filePath             path of the file we want to write
   * @param content              the content to write
   * @throws IOException
   */
  public void writeToHDFS(Project project, Users user, Path filePath, String content) throws IOException {
    DistributedFileSystemOps udfso = null;
    try {
      String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
      udfso = distributedFsService.getDfsOps(hdfsUsername);
      try (FSDataOutputStream outStream = udfso.create(filePath)) {
        outStream.writeBytes(content);
        outStream.hflush();
      }
    } finally {
      if (udfso != null) {
        distributedFsService.closeDfsClient(udfso);
      }
    }
  }
}
