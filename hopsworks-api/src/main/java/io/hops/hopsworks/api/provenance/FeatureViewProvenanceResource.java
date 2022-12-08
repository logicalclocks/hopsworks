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
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.provenance.explicit.ProvExplicitLink;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

@Logged
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureViewProvenanceResource extends ProvenanceResource<FeatureView> {
  @EJB
  private FeatureViewController featureViewController;
  
  private Featurestore featureStore;
  private String featureViewName;
  private Integer featureViewVersion;
  
  public FeatureViewProvenanceResource() {}
  
  @Logged(logLevel = LogLevel.OFF)
  public void setFeatureStore(Featurestore featureStore) {
    this.featureStore = featureStore;
  }
  
  @Logged(logLevel = LogLevel.OFF)
  public void setFeatureViewName(String featureViewName) {
    this.featureViewName = featureViewName;
  }
  
  @Logged(logLevel = LogLevel.OFF)
  public void setFeatureViewVersion(Integer featureViewVersion) {
    this.featureViewVersion = featureViewVersion;
  }
  
  @Override
  protected FeatureView getArtifact() throws FeaturestoreException {
    return featureViewController.getByNameVersionAndFeatureStore(featureViewName, featureViewVersion, featureStore);
  }
  
  @Override
  protected ProvExplicitLink<FeatureView> getExplicitLinks(Project accessProject,
                                                           Integer upstreamLvls, Integer downstreamLvls)
    throws FeaturestoreException, GenericException, DatasetException {
    return provCtrl.featureViewLinks(accessProject, getArtifact(), upstreamLvls, downstreamLvls);
  }
  
  @Override
  protected String getArtifactId() {
    return featureViewName + "_" + featureViewVersion;
  }
  
  @Override
  protected DatasetPath getArtifactDatasetPath() throws FeaturestoreException, DatasetException {
    FeatureView featureView
      = featureViewController.getByNameVersionAndFeatureStore(featureViewName, featureViewVersion, featureStore);
    return datasetHelper.getDatasetPath(project, featureViewController.getLocation(featureView),
      DatasetType.DATASET);
  }
}
