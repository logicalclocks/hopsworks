/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.featurestore.trainingdataset;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.featurestore.FeaturestoreSubResource;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;

public abstract class TrainingDatasetSubResource extends FeaturestoreSubResource {
  private String featureViewName;
  private Integer featureViewVersion;
  private Integer trainingDatasetId;
  private Integer trainingDatasetVersion;

  public String getFeatureViewName() {
    return featureViewName;
  }

  public void setFeatureViewName(String featureViewName) {
    this.featureViewName = featureViewName;
  }

  public Integer getFeatureViewVersion() {
    return featureViewVersion;
  }

  public void setFeatureViewVersion(Integer featureViewVersion) {
    this.featureViewVersion = featureViewVersion;
  }

  public Integer getTrainingDatasetId() {
    return trainingDatasetId;
  }

  public void setTrainingDatasetId(Integer trainingDatasetId) {
    this.trainingDatasetId = trainingDatasetId;
  }

  public Integer getTrainingDatasetVersion() {
    return trainingDatasetVersion;
  }

  public void setTrainingDatasetVersion(Integer trainingDatasetVersion) {
    this.trainingDatasetVersion = trainingDatasetVersion;
  }

  public void setFeatureView(String name, Integer version) {
    this.featureViewName = name;
    this.featureViewVersion = version;
  }

  public FeatureView getFeatureView(Project project) throws FeaturestoreException, ProjectException {
    if (Strings.isNullOrEmpty(featureViewName) || featureViewVersion == null) {
      return null;
    }
    return getFeatureViewController().getByNameVersionAndFeatureStore(featureViewName, featureViewVersion,
      getFeaturestore(project));
  }

  public FeatureView getFeatureView(Featurestore featurestore) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(featureViewName) || featureViewVersion == null) {
      return null;
    }
    return getFeatureViewController().getByNameVersionAndFeatureStore(featureViewName, featureViewVersion,
      featurestore);
  }

  public FeatureView getFeatureView() throws ProjectException, FeaturestoreException {
    if (Strings.isNullOrEmpty(featureViewName) || featureViewVersion == null) {
      return null;
    }
    return getFeatureViewController().getByNameVersionAndFeatureStore(featureViewName, featureViewVersion,
      getFeaturestore());
  }

  public TrainingDataset getTrainingDataset(Project project) throws FeaturestoreException, ProjectException {
    if (trainingDatasetId != null) {
      return getTrainingDatasetController().getTrainingDatasetById(getFeaturestore(project), trainingDatasetId);
    } else if (trainingDatasetVersion != null) {
      return getTrainingDatasetController().getTrainingDatasetByFeatureViewAndVersion(getFeatureView(project),
        trainingDatasetVersion);
    }
    return null;
  }

  public TrainingDataset getTrainingDataset(Featurestore featurestore) throws FeaturestoreException {
    if (trainingDatasetId != null) {
      return getTrainingDatasetController().getTrainingDatasetById(featurestore, trainingDatasetId);
    } else if (trainingDatasetVersion != null) {
      return getTrainingDatasetController().getTrainingDatasetByFeatureViewAndVersion(getFeatureView(featurestore),
        trainingDatasetVersion);
    }
    return null;
  }

  public TrainingDataset getTrainingDataset() throws ProjectException, FeaturestoreException {
    if (trainingDatasetId != null) {
      return getTrainingDatasetController().getTrainingDatasetById(getFeaturestore(), trainingDatasetId);
    } else if (trainingDatasetVersion != null) {
      return getTrainingDatasetController().getTrainingDatasetByFeatureViewAndVersion(getFeatureView(),
        trainingDatasetVersion);
    }
    return null;
  }

  protected abstract FeatureViewController getFeatureViewController();
  protected abstract TrainingDatasetController getTrainingDatasetController();
}
