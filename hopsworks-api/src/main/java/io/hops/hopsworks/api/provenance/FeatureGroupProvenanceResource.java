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

import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.provenance.explicit.ProvExplicitLink;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupProvenanceResource extends ProvenanceResource<Featuregroup> {
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private FeaturegroupController featureGroupController;
  
  private Featurestore featureStore;
  private Integer featureGroupId;
  
  public FeatureGroupProvenanceResource() {}
  
  public void setFeatureStore(Featurestore featureStore) {
    this.featureStore = featureStore;
  }
  
  public void setFeatureGroupId(Integer featureGroupId) {
    this.featureGroupId = featureGroupId;
  }
  
  @Override
  protected Featuregroup getArtifact() throws FeaturestoreException {
    return featureGroupController.getFeaturegroupById(featureStore, featureGroupId);
  }
  
  @Override
  protected ProvExplicitLink<Featuregroup> getExplicitLinks(Project accessProject,
                                                            Integer upstreamLvls, Integer downstreamLvls)
    throws FeaturestoreException, GenericException, DatasetException {
    return provCtrl.featureGroupLinks(accessProject, getArtifact(), upstreamLvls, downstreamLvls);
  }
  
  @Override
  protected String getArtifactId() throws FeaturestoreException {
    Featuregroup featureGroup = featureGroupController.getFeaturegroupById(featureStore, featureGroupId);
    return featureGroup.getName() + "_" + featureGroup.getVersion();
  }
  
  @Override
  protected DatasetPath getArtifactDatasetPath() throws FeaturestoreException, DatasetException {
    Dataset targetEndpoint = featurestoreController.getProjectFeaturestoreDataset(featureStore.getProject());
    return datasetHelper.getTopLevelDatasetPath(project, targetEndpoint);
  }
}
