/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.provenance;

import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.provenance.explicit.ProvExplicitLink;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

@Logged
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TrainingDatasetProvenanceResource extends ProvenanceResource<TrainingDataset> {
  @EJB
  private TrainingDatasetController trainingDatasetController;
  
  private Featurestore featureStore;
  private FeatureView featureView;
  private Integer trainingDatasetVersion;
  
  public TrainingDatasetProvenanceResource() {}
  
  @Logged(logLevel = LogLevel.OFF)
  public void setFeatureStore(Featurestore featureStore) {
    this.featureStore = featureStore;
  }
  
  @Logged(logLevel = LogLevel.OFF)
  public void setFeatureView(FeatureView featureView) {
    this.featureView = featureView;
  }
  
  @Logged(logLevel = LogLevel.OFF)
  public void setTrainingDatasetVersion(Integer trainingDatasetVersion) {
    this.trainingDatasetVersion = trainingDatasetVersion;
  }
  
  @Override
  protected TrainingDataset getArtifact() throws FeaturestoreException {
    return trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(featureView, trainingDatasetVersion);
  }
  
  @Override
  protected ProvExplicitLink<TrainingDataset> getExplicitLinks(Project accessProject, Integer upstreamLvls,
                                                               Integer downstreamLvls)
    throws FeaturestoreException, GenericException, DatasetException {
    return provCtrl.trainingDatasetLinks(accessProject, getArtifact(), upstreamLvls, downstreamLvls);
  }
  
  @Override
  protected String getArtifactId() throws FeaturestoreException {
    TrainingDataset trainingDataset
      = trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(featureView, trainingDatasetVersion);
    return trainingDataset.getName() + "_" + trainingDataset.getVersion();
  }
  
  @Override
  protected DatasetPath getArtifactDatasetPath() throws FeaturestoreException, DatasetException {
    TrainingDataset trainingDataset
      = trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(featureView, trainingDatasetVersion);
    return datasetHelper.getDatasetPath(project, trainingDatasetController.getTrainingDatasetInodePath(trainingDataset),
      DatasetType.DATASET);
  }
}
