/*
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
 */

package io.hops.hopsworks.api.featurestore.util;

import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreEntityDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.logging.Level;

/**
 * Utility functions for the featurestore service
 */
@Stateless
public class FeaturestoreUtil {

  @EJB
  private DatasetController datasetController;
  @EJB
  private ProjectTeamFacade projectTeamFacade;

  /**
   * Helper function that gets the Dataset where all the training dataset in the featurestore resides within the project
   *
   * @param project the project to get the dataset for
   * @return the training dataset for the project
   */
  public Dataset getTrainingDatasetFolder(Project project){
    return datasetController.getByProjectAndDsName(project,null, getTrainingDatasetFolderName(project));
  }

  /**
   * Returns the training dataset folder name of a project (projectname_Training_Datasets)
   *
   * @param project the project to get the folder name for
   * @return the name of the folder
   */
  public String getTrainingDatasetFolderName(Project project){
    return project.getName() + "_" + Settings.ServiceDataset.TRAININGDATASETS.getName();
  }

  /**
   * Helper function that gets the training dataset path from a folder and training dataset name.
   * (path_to_folder/trainingdatasetName_version)
   *
   * @param trainingDatasetsFolderPath the path to the dataset folder
   * @param trainingDatasetName the name of the training dataset
   * @param version the version of the training dataset
   * @return the path to the training dataset as a child-file of the training dataset folder
   */
  public String getTrainingDatasetPath(String trainingDatasetsFolderPath, String trainingDatasetName, Integer version){
    return trainingDatasetsFolderPath + "/" + trainingDatasetName + "_" + version;
  }

  /**
   * Verify that the user is allowed to execute the requested operation based on his/hers project role
   * <p>
   * Only data owners are allowed to update/delete feature groups/training datasets
   * created by someone else in the project
   *
   * @param featurestoreEntityDTO the featurestore entity that the operation concerns (feature group or training
   *                              dataset)
   * @param featurestore the featurestore that the operation concerns
   * @param project the project of the featurestore
   * @param user the user requesting the operation
   * @throws FeaturestoreException
   */
  public void verifyUserRole(FeaturestoreEntityDTO featurestoreEntityDTO,
                             Featurestore featurestore, Users user, Project project)
      throws FeaturestoreException {
    String userRole = projectTeamFacade.findCurrentRole(project, user);
    if (!featurestoreEntityDTO.getCreator().equals(user.getEmail()) &&
        !userRole.equalsIgnoreCase(AllowedRoles.DATA_OWNER)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.UNAUTHORIZED_FEATURESTORE_OPERATION, Level.FINE,
          "project: " + project.getName() + ", featurestoreId: " + featurestore.getId() +
              ", featuregroupId: " + featurestoreEntityDTO.getId() + ", userRole:" + userRole +
              ", creator of the featuregroup: " + featurestoreEntityDTO.getCreator());
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
   * Return default updateMetadata query parameter value if not specified
   *
   * @param updateMetadata the query parameter provided by the user
   * @return the default value
   */
  public Boolean updateMetadataGetOrDefault(Boolean updateMetadata) {
    if(updateMetadata == null){
      return false;
    } else {
      return updateMetadata;
    }
  }

  /**
   * Return default updateStats query parameter value if not specified
   *
   * @param updateStats the query parameter provided by the user
   * @return the default value
   */
  public Boolean updateStatsGetOrDefault(Boolean updateStats) {
    if(updateStats == null){
      return false;
    } else {
      return updateStats;
    }
  }
}
