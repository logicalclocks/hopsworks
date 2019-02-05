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

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.exception.FeaturestoreException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Utility functions for the featurestore service
 */
@Stateless
public class FeaturestoreUtil {

  @EJB
  private DatasetFacade datasetFacade;

  /**
   * Returns a String with Columns from a JSON featuregroup
   * that can be used for a HiveQL CREATE TABLE statement
   *
   * @param features list of featureDTOs
   * @return feature schema string for creating hive table
   */
  public String makeCreateTableColumnsStr(List<FeatureDTO> features) throws FeaturestoreException {
    StringBuilder stringBuilder = new StringBuilder();
    List<FeatureDTO> primaryKeys = features.stream().filter(f -> f.getPrimary()).collect(Collectors.toList());
    if(primaryKeys.isEmpty()){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.NO_PRIMARY_KEY_SPECIFIED, Level.SEVERE,
          "Out of the " + features.size() + " features provided, none is marked as primary");
    }
    FeatureDTO primaryKey = primaryKeys.get(0);
    for (int i = 0; i < features.size(); i++) {
      FeatureDTO feature = features.get(i);
      stringBuilder.append("`");
      stringBuilder.append(feature.getName());
      stringBuilder.append("` ");
      stringBuilder.append(feature.getType());
      stringBuilder.append(" COMMENT '");
      stringBuilder.append(feature.getDescription());
      stringBuilder.append("'");
      stringBuilder.append(", ");
      if (i == features.size() - 1){
        stringBuilder.append("PRIMARY KEY (`");
        stringBuilder.append(primaryKey.getName());
        stringBuilder.append("`) DISABLE NOVALIDATE");
      }
    }
    return stringBuilder.toString();
  }

  /**
   * Helper function that gets the Dataset where all the training dataset in the featurestore resides within the project
   *
   * @param project the project to get the dataset for
   * @return the training dataset for the project
   */
  public Dataset getTrainingDatasetFolder(Project project){
    return datasetFacade.findByNameAndProjectId(project, getTrainingDatasetFolderName(project));
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
}
