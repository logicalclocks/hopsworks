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

package io.hops.hopsworks.common.dao.featurestore.dependencies;

import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class controlling the interaction with the featurestore_dependency table and required business logic
 */
@Stateless
public class FeaturestoreDependencyController {
  @EJB
  private FeaturestoreDependencyFacade featurestoreDependencyFacade;
  @EJB
  private InodeFacade inodeFacade;
  private static final Logger LOGGER = Logger.getLogger(FeaturestoreDependencyController.class.getName());

  /**
   * Updates the list of dependencies for a featuregroup or training dataset
   *
   * @param featuregroup the featuregroup to update dependencies for
   * @param trainingDataset the training dataset to update dependencies for
   * @param fileNames the list of filenames of the new dependencies
   */
  public void updateFeaturestoreDependencies(
      Featuregroup featuregroup, TrainingDataset trainingDataset, List<String> fileNames) {
    if(fileNames == null) {
      return;
    }
    if(featuregroup != null){
      removeFeaturestoreDependencies((List) featuregroup.getDependencies());
    }
    if(trainingDataset != null){
      removeFeaturestoreDependencies((List) trainingDataset.getDependencies());
    }
    insertFeaturestoreDependencies(featuregroup, trainingDataset, fileNames);
  }

  /**
   * Removes a list of dependencies from the database
   *
   * @param featurestoreDependencies the lits of dependencies to remove
   */
  private void removeFeaturestoreDependencies(List<FeaturestoreDependency> featurestoreDependencies) {
    featurestoreDependencies.stream().forEach(fd -> featurestoreDependencyFacade.remove(fd));
  }

  /**
   * Inserts a list of dependencies in the database, linked to a featuregroup or training dataset
   *
   * @param featuregroup the featuregroup that the dependencies are linked to
   * @param trainingDataset the training dataset that the dependencies are linked to
   * @param fileNames the list of file names of the new dependencies
   */
  private void insertFeaturestoreDependencies(
      Featuregroup featuregroup, TrainingDataset trainingDataset,
      List<String> fileNames) {
    List<FeaturestoreDependency> featurestoreDependencies = convertFileNamesToFeaturestoreDependencies(
        featuregroup, trainingDataset, fileNames);
    featurestoreDependencies.forEach(fd -> featurestoreDependencyFacade.persist(fd));
  }

  /**
   * Converts a list of file names into FeatureStoreDependency entities that can be inserted in the database
   *
   * @param featuregroup the featuregroup that the dependencies are linked to
   * @param trainingDataset the training dataset that the dependencies are linked to
   * @param fileNames the list of filenames for the dependencies
   * @return a list of FeaturestoreDependency entities
   */
  private List<FeaturestoreDependency> convertFileNamesToFeaturestoreDependencies(
      Featuregroup featuregroup, TrainingDataset trainingDataset,
      List<String> fileNames) {
    List<FeaturestoreDependency> featurestoreDependencies = new ArrayList<>();
    for (String fileName: fileNames) {
      Inode inode = inodeFacade.getInodeAtPath(preProcessDependencyPath(fileName));
      if(inode == null){
        LOGGER.log(Level.WARNING, "Could not find inode for feature store dependency with name: " + fileName);
      } else {
        FeaturestoreDependency featurestoreDependency = new FeaturestoreDependency();
        featurestoreDependency.setFeaturegroup(featuregroup);
        featurestoreDependency.setTrainingDataset(trainingDataset);
        featurestoreDependency.setInode(inode);
        featurestoreDependencies.add(featurestoreDependency);
      }
    }
    return featurestoreDependencies;
  }

  /**
   * Drops the "hdfs://" prefix from the dependnecy path
   *
   * @param dependencyPath the path to drop the prefix from
   * @return the path without the prefix
   */
  private String preProcessDependencyPath(String dependencyPath){
    if(dependencyPath.length() > 7){
      if(dependencyPath.substring(0, 7).equalsIgnoreCase("hdfs://")){
        dependencyPath = dependencyPath.substring(7);
      }
    }
    return dependencyPath;
  }
}
