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

package io.hops.hopsworks.common.featurestore.featuremonitoring.result;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.featuremonitoring.alert.FeatureMonitoringAlertController;
import io.hops.hopsworks.common.featurestore.featuremonitoring.config.FeatureMonitoringConfigurationController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.result.FeatureMonitoringResult;
import io.hops.hopsworks.restutils.RESTCodes.FeaturestoreErrorCode;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureMonitoringResultController {
  private static final Logger LOGGER = Logger.getLogger(FeatureMonitoringResultController.class.getName());
  
  @EJB
  FeatureMonitoringResultFacade featureMonitoringResultFacade;
  @EJB
  FeatureMonitoringConfigurationController featureMonitoringConfigurationController;
  @EJB
  FeatureMonitoringAlertController featureMonitoringAlertController;
  
  ////////////////////////////////////////
  ////  CRD Operations
  ////////////////////////////////////////
  public FeatureMonitoringResult createFeatureMonitoringResult(FeatureMonitoringResult result) {
    featureMonitoringResultFacade.save(result);
    try {
      featureMonitoringAlertController.triggerAlertsByStatus(result.getFeatureMonitoringConfig(), result);
    } catch (FeaturestoreException e) {
      LOGGER.log(Level.SEVERE,
        String.format("Error triggering alerts for Feature Monitoring Result with id: %d error message: %s",
          result.getId(), e.getUsrMsg()), e);
    }
    return result;
  }
  
  public FeatureMonitoringResult getFeatureMonitoringResultById(Integer resultId)
    throws FeaturestoreException {
    
    Optional<FeatureMonitoringResult> optResult = featureMonitoringResultFacade.findById(resultId);
    
    if (!optResult.isPresent()) {
      throw new FeaturestoreException(FeaturestoreErrorCode.FEATURE_MONITORING_ENTITY_NOT_FOUND, Level.WARNING,
        String.format("Feature Monitoring Result with id %d not found.", resultId));
    }
    
    return optResult.get();
  }
  
  public void deleteFeatureMonitoringResult(Integer resultId) throws FeaturestoreException {
    Optional<FeatureMonitoringResult> optResult = featureMonitoringResultFacade.findById(resultId);
    
    if (optResult.isPresent()) {
      featureMonitoringResultFacade.remove(optResult.get());
    } else {
      throw new FeaturestoreException(FeaturestoreErrorCode.FEATURE_MONITORING_ENTITY_NOT_FOUND, Level.WARNING,
        String.format("Feature Monitoring Result with id %d not found.", resultId));
    }
  }
  
  public AbstractFacade.CollectionInfo<FeatureMonitoringResult> getAllFeatureMonitoringResultByConfigId(
    Integer offset, Integer limit, Set<? extends AbstractFacade.SortBy> sorts,
    Set<? extends AbstractFacade.FilterBy> filters, Integer configId)
      throws FeaturestoreException {
    
    FeatureMonitoringConfiguration config =
      featureMonitoringConfigurationController.getFeatureMonitoringConfigurationByConfigId(configId);
    
    return featureMonitoringResultFacade.findByConfigId(offset, limit, sorts, filters, config);
  }
}