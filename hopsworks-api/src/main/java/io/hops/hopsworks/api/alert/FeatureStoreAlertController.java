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

import com.google.common.base.Strings;
import io.hops.hopsworks.alert.dao.AlertReceiverFacade;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.api.featurestore.datavalidation.alert.FeatureGroupAlertDTO;
import io.hops.hopsworks.api.featurestore.datavalidation.alert.PostableFeatureStoreAlerts;
import io.hops.hopsworks.api.featurestore.featureview.FeatureViewAlertDTO;
import io.hops.hopsworks.common.alert.AlertController;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupAlertFacade;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewAlertFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.alert.FeatureViewAlert;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureStoreAlertController {
  @EJB
  FeatureViewAlertFacade featureViewAlertFacade;
  @EJB
  AlertController alertController;
  @EJB
  AlertReceiverFacade alertReceiverFacade;
  @EJB
  FeatureGroupAlertFacade featureGroupAlertFacade;
  
  private AlertReceiver getReceiver(String name) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(name)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
        "Receiver can not be empty.");
    }
    Optional<AlertReceiver> alertReceiver = alertReceiverFacade.findByName(name);
    if (alertReceiver.isPresent()) {
      return alertReceiver.get();
    }
    throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
      "Alert receiver not found " + name);
  }
  
  public void createRoute(Project project, FeatureGroupAlert featureGroupAlert) throws FeaturestoreException {
    try {
      alertController.createRoute(project, featureGroupAlert);
    } catch (AlertManagerClientCreateException | AlertManagerConfigReadException |
             AlertManagerConfigCtrlCreateException | AlertManagerConfigUpdateException |
             AlertManagerNoSuchElementException |
             AlertManagerAccessControlException | AlertManagerUnreachableException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FAILED_TO_CREATE_ROUTE, Level.FINE,
        e.getMessage());
    }
  }
  
  public void createRoute(Project project, FeatureViewAlert featureViewAlert) throws FeaturestoreException {
    try {
      alertController.createRoute(project, featureViewAlert);
    } catch (AlertManagerClientCreateException | AlertManagerConfigReadException |
             AlertManagerConfigCtrlCreateException | AlertManagerConfigUpdateException |
             AlertManagerNoSuchElementException |
             AlertManagerAccessControlException | AlertManagerUnreachableException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FAILED_TO_CREATE_ROUTE, Level.FINE,
        e.getMessage());
    }
  }
  
  public void deleteRoute(FeatureGroupAlert featureGroupAlert, Project project) throws FeaturestoreException {
    try {
      alertController.deleteRoute(project, featureGroupAlert);
    } catch (AlertManagerUnreachableException | AlertManagerAccessControlException | AlertManagerConfigUpdateException |
             AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException |
             AlertManagerClientCreateException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FAILED_TO_DELETE_ROUTE, Level.FINE,
        e.getMessage());
    }
  }
  
  public void deleteRoute(FeatureViewAlert featureViewAlert, Project project) throws FeaturestoreException {
    try {
      alertController.deleteRoute(project, featureViewAlert);
    } catch (AlertManagerUnreachableException | AlertManagerAccessControlException | AlertManagerConfigUpdateException |
             AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException |
             AlertManagerClientCreateException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FAILED_TO_DELETE_ROUTE, Level.FINE,
        e.getMessage());
    }
  }
  
  public FeatureGroupAlert updateAlert(FeatureGroupAlertDTO dto, FeatureGroupAlert featureGroupAlert,
    Project project)
    throws FeaturestoreException {
    if (dto.getStatus() != null) {
      featureGroupAlert.setStatus(dto.getStatus());
    }
    if (dto.getSeverity() != null) {
      featureGroupAlert.setSeverity(dto.getSeverity());
    }
    if (!featureGroupAlert.getReceiver().getName().equals(dto.getReceiver())) {
      deleteRoute(featureGroupAlert, project);
      featureGroupAlert.setReceiver(getReceiver(dto.getReceiver()));
      createRoute(project, featureGroupAlert);
    }
    featureGroupAlert.setAlertType(alertController.getAlertType(featureGroupAlert.getReceiver()));
    featureGroupAlert = featureGroupAlertFacade.update(featureGroupAlert);
    return featureGroupAlert;
  }
  
  public FeatureViewAlert updateAlert(FeatureViewAlertDTO dto, FeatureViewAlert featureViewAlert, Project project)
    throws FeaturestoreException {
    if (dto.getStatus() != null) {
      featureViewAlert.setStatus(dto.getStatus());
    }
    if (dto.getSeverity() != null) {
      featureViewAlert.setSeverity(dto.getSeverity());
    }
    if (!featureViewAlert.getReceiver().getName().equals(dto.getReceiver())) {
      deleteRoute(featureViewAlert, project);
      featureViewAlert.setReceiver(getReceiver(dto.getReceiver()));
      createRoute(project, featureViewAlert);
    }
    featureViewAlert.setAlertType(alertController.getAlertType(featureViewAlert.getReceiver()));
    featureViewAlert = featureViewAlertFacade.update(featureViewAlert);
    return featureViewAlert;
  }
  
  
  
  public FeatureViewAlert persistFeatureViewEntityValues(PostableFeatureStoreAlerts dto,
    FeatureView featureView) throws FeaturestoreException {
    FeatureViewAlert featureViewAlert = new FeatureViewAlert();
    featureViewAlert.setStatus(dto.getStatus());
    featureViewAlert.setSeverity(dto.getSeverity());
    featureViewAlert.setCreated(new Date());
    featureViewAlert.setFeatureView(featureView);
    featureViewAlert.setReceiver(getReceiver(dto.getReceiver()));
    featureViewAlert.setAlertType(alertController.getAlertType(featureViewAlert.getReceiver()));
    featureViewAlert = featureViewAlertFacade.update(featureViewAlert);
    return featureViewAlert;
  }
  
  public FeatureGroupAlert persistFeatureGroupEntityValues(PostableFeatureStoreAlerts dto,
    Featuregroup featureGroup) throws FeaturestoreException {
    FeatureGroupAlert featureGroupAlert = new FeatureGroupAlert();
    featureGroupAlert.setStatus(dto.getStatus());
    featureGroupAlert.setSeverity(dto.getSeverity());
    featureGroupAlert.setCreated(new Date());
    featureGroupAlert.setFeatureGroup(featureGroup);
    featureGroupAlert.setReceiver(getReceiver(dto.getReceiver()));
    featureGroupAlert.setAlertType(alertController.getAlertType(featureGroupAlert.getReceiver()));
    featureGroupAlert = featureGroupAlertFacade.update(featureGroupAlert);
    return featureGroupAlert;
  }
  
  public FeatureViewAlert retrieveSingleAlert( Integer id  , FeatureView featureView) throws FeaturestoreException {
    
    FeatureViewAlert featureViewAlert = featureViewAlertFacade.findByFeatureViewAndId(featureView, id);
    if (featureViewAlert == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_NOT_FOUND, Level.FINE);
    }
    return featureViewAlert;
  }
  
  public List<FeatureViewAlert> retrieveManyAlerts(ResourceRequest resourceRequest, FeatureView featureView)
    throws FeaturestoreException {
    AbstractFacade.CollectionInfo collectionInfo;
    collectionInfo =
      featureViewAlertFacade.findFeatureViewAlerts(resourceRequest.getOffset(), resourceRequest.getLimit(),
        resourceRequest.getFilter(), resourceRequest.getSort(), featureView);
    if (collectionInfo.getItems().isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_NOT_FOUND, Level.FINE, "No alerts found " +
        "for feature view "+featureView.getName());
    }
    return collectionInfo.getItems();
  }
  
  public List<FeatureViewAlert> retrieveAllFeatureViewAlerts(Project project) {
    List<FeatureViewAlert> projectAlerts = featureViewAlertFacade.findAll();
    projectAlerts = projectAlerts.stream()
      .filter(featureViewAlert -> featureViewAlert.getFeatureView().getFeaturestore().getProject().equals(project))
      .collect(Collectors.toList());
    return projectAlerts;
    
  }
}