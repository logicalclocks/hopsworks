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

import io.hops.hopsworks.api.featurestore.featuremonitoring.result.FeatureMonitoringResultResource;
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
public class FeatureGroupFeatureMonitoringResultResource extends FeatureMonitoringResultResource {
  
  @EJB
  private FeaturegroupController featuregroupController;
  
  private Integer featureGroupId;
  
  public void setFeatureGroupId(Integer featureGroupId) throws FeaturestoreException {
    this.featureGroupId = featureGroupId;
  }

  private Featuregroup getFeatureGroup() throws ProjectException, FeaturestoreException {
    return featuregroupController.getFeaturegroupById(getFeaturestore(), featureGroupId);
  }

  @Override
  protected void check() throws ProjectException, FeaturestoreException {
    getFeatureGroup();
  }
}