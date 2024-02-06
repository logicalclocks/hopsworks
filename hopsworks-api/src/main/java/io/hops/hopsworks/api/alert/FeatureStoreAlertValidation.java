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
package io.hops.hopsworks.api.alert;

import io.hops.hopsworks.alert.util.Constants;
import io.hops.hopsworks.api.featurestore.datavalidation.alert.PostableFeatureStoreAlerts;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupAlertFacade;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewAlertFacade;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlertStatus;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.alert.FeatureViewAlert;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureStoreAlertValidation {
  @EJB
  private FeatureGroupAlertFacade featureGroupAlertFacade;
  @EJB
  private FeatureViewAlertFacade featureViewAlertFacade;
  
  public void validate(PostableFeatureStoreAlerts dto, Featuregroup featuregroup, FeatureView featureView)
    throws FeaturestoreException {
    if (dto == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
        Constants.NO_PAYLOAD);
    }
    if (dto.getStatus() == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
        "Status can not be empty.");
    }
    if (dto.getSeverity() == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
        "Severity can not be empty.");
    }
    FeatureGroupAlert featureGroupAlert =
      featureGroupAlertFacade.findByFeatureGroupAndStatus(featuregroup, dto.getStatus());
    if (featureGroupAlert != null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ALREADY_EXISTS, Level.FINE,
        String.format("Feature Group Alert with FeatureGroupName=%s status=%s already exists.",
          featuregroup.getName(), dto.getStatus()));
    }
    FeatureViewAlert existingAlert =
      featureViewAlertFacade.findByFeatureViewAndStatus(featureView, dto.getStatus());
    if (existingAlert != null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ALREADY_EXISTS, Level.FINE,
        String.format("Feature View Alert with feature view=%s status=%s already exists.", featureView.getName(),
          dto.getStatus()));
    }
  }
  
  public void validateUpdate(FeatureViewAlert featureViewAlert,
    FeatureStoreAlertStatus status, FeatureView featureView)
    throws FeaturestoreException {
    if (featureViewAlert == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_NOT_FOUND, Level.FINE);
    }
    if (!status.equals(featureViewAlert.getStatus()) &&
      featureViewAlertFacade.findByFeatureViewAndStatus(featureView, status) != null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ALREADY_EXISTS, Level.FINE,
        "Feature Group Alert with name=" + featureView.getName() + " status=" +
          status + " already exists.");
    }
  }
  
  public void validateUpdate(FeatureGroupAlert featureGroupAlert,
    FeatureStoreAlertStatus status, Featuregroup featuregroup)
    throws FeaturestoreException {
    if (featureGroupAlert == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_NOT_FOUND, Level.FINE);
    }
    if (!status.equals(featureGroupAlert.getStatus()) &&
      featureGroupAlertFacade.findByFeatureGroupAndStatus(featuregroup, status) != null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ALREADY_EXISTS, Level.FINE,
        "Feature Group Alert with name=" + featuregroup.getName() + " status=" +
          status + " already exists.");
    }
  }
  
  public void validateBulk(PostableFeatureStoreAlerts featureGroupAlertDTO) throws FeaturestoreException {
    if (featureGroupAlertDTO.getItems() == null || featureGroupAlertDTO.getItems().isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
        Constants.NO_PAYLOAD);
    }
    Set<FeatureStoreAlertStatus> statusSet = new HashSet<>();
    for (PostableFeatureStoreAlerts dto : featureGroupAlertDTO.getItems()) {
      statusSet.add(dto.getStatus());
    }
    if (statusSet.size() < featureGroupAlertDTO.getItems().size()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
        "Duplicate alert.");
    }
  }
  
  public void validateFeatureViewRequest(PostableFeatureStoreAlerts dto, ResourceRequest.Name resourceName)
    throws AlertException {
    if (resourceName.equals(ResourceRequest.Name.FEATUREVIEW) &&
      !FeatureStoreAlertStatus.isFeatureMonitoringStatus(dto.getStatus())) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
        String.format("Alert status %s not allowed for feature view alerts", dto.getStatus()));
    }
  }
  
  public void validateEntityType(ResourceRequest.Name requestName, Featuregroup featuregroup, FeatureView featureView) {
    if (requestName.equals(ResourceRequest.Name.FEATUREGROUPS) && featuregroup == null) {
      throw new IllegalArgumentException(
        "Feature group id cannot be null if alert entity type is " + ResourceRequest.Name.FEATUREGROUPS);
    }
    if (requestName.equals(ResourceRequest.Name.FEATUREVIEW) && featureView == null) {
      throw new IllegalArgumentException(
        "Feature view cannot be null if alert entity type is " + ResourceRequest.Name.FEATUREVIEW);
    }
  }
}