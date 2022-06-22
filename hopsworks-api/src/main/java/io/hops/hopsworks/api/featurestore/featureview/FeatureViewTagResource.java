/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.featurestore.featureview;

import io.hops.hopsworks.api.featurestore.tag.FeatureStoreTagResource;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureViewTagResource extends FeatureStoreTagResource {
  
  private FeatureView featureView;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private FeatureViewController featureViewController;
  
  /**
   * Sets the feature view of the tag resource
   *
   * @param name
   * @param version
   */
  public void setFeatureView(String name, Integer version) throws FeaturestoreException {
    this.featureView = featureViewController.getByNameVersionAndFeatureStore(name, version, featureStore);
  }

  @Override
  protected DatasetPath getDatasetPath() throws DatasetException {
    return datasetHelper.getDatasetPath(project, featureViewController.getLocation(featureView),
        DatasetType.DATASET);
  }
  
  @Override
  protected Integer getItemId() {
    return featureView.getId();
  }
  
  @Override
  protected ResourceRequest.Name getItemType() {
    return ResourceRequest.Name.FEATUREVIEW;
  }
}
