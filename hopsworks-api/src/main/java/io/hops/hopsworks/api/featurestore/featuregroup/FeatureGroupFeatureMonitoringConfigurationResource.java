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
package io.hops.hopsworks.api.featurestore.featuregroup;

import io.hops.hopsworks.api.featurestore.featuremonitoring.config.FeatureMonitoringConfigurationResource;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupFeatureMonitoringConfigurationResource extends FeatureMonitoringConfigurationResource {
  
  @EJB
  private FeaturegroupController featuregroupController;
  
  private Integer featureGroupId;
  
  /**
   * Sets the feature group of the tag resource
   *
   * @param featureGroupId
   */
  public void setFeatureGroupId(Integer featureGroupId) throws FeaturestoreException {
    this.featureGroupId = featureGroupId;
  }

  private Featuregroup getFeatureGroup() throws ProjectException, FeaturestoreException {
    return featuregroupController.getFeaturegroupById(getFeaturestore(), featureGroupId);
  }
  
  @Override
  protected Integer getItemId() {
    return this.featureGroupId;
  }
  
  @Override
  protected ResourceRequest.Name getItemType() {
    return ResourceRequest.Name.FEATUREGROUPS;
  }
  
  @Override
  protected String getItemName() throws ProjectException, FeaturestoreException {
    return getFeatureGroup().getName();
  }
  
  @Override
  protected Integer getItemVersion() throws ProjectException, FeaturestoreException {
    return getFeatureGroup().getVersion();
  }
}