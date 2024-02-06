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
package io.hops.hopsworks.api.featurestore.featureview;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewAlertFacade;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.alert.FeatureViewAlert;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureViewAlertBuilder {
  private static final Logger LOGGER = Logger.getLogger(FeatureViewAlertBuilder.class.getName());
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private FeatureViewAlertFacade featureViewAlertFacade;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  
  public FeatureViewAlertDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
    FeatureViewAlert featureViewAlert) {
    FeatureViewAlertDTO dto = new FeatureViewAlertDTO();
    dto.setHref(uriInfo.getAbsolutePathBuilder().path(featureViewAlert.getId().toString()).build());
    setDtoValues(dto, resourceRequest, featureViewAlert);
    return dto;
  }
  
  public FeatureViewAlertDTO expand(FeatureViewAlertDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.ALERTS)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  
  public FeatureViewAlertDTO buildFeatureViewAlertDto(UriInfo uriInfo, ResourceRequest resourceRequest,
    FeatureViewAlert featureViewAlert) {
    FeatureViewAlertDTO dto = new FeatureViewAlertDTO();
    dto.setHref(uriInfo.getAbsolutePathBuilder().build());
    setDtoValues(dto, resourceRequest, featureViewAlert);
    return dto;
  }
  
  private void setDtoValues(FeatureViewAlertDTO dto, ResourceRequest resourceRequest,
    FeatureViewAlert featureViewAlert) {
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setFeatureViewId(featureViewAlert.getFeatureView().getId());
      dto.setFeatureViewName(featureViewAlert.getFeatureView().getName());
      dto.setFeatureViewVersion(featureViewAlert.getFeatureView().getVersion());
      dto.setFeatureStoreName(
        featurestoreController.getOfflineFeaturestoreDbName(featureViewAlert.getFeatureView().getFeaturestore()));
      dto.setId(featureViewAlert.getId());
      dto.setAlertType(featureViewAlert.getAlertType());
      dto.setStatus(featureViewAlert.getStatus());
      dto.setSeverity(featureViewAlert.getSeverity());
      dto.setCreated(featureViewAlert.getCreated());
      dto.setReceiver(featureViewAlert.getReceiver().getName());
    }
  }
  
  public FeatureViewAlertDTO projectUri(FeatureViewAlertDTO dto, UriInfo uriInfo,
    FeatureViewAlert featureViewAlert) {
    UriBuilder uri = uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString());
    uri.path(Integer.toString(featureViewAlert.getFeatureView().getFeaturestore().getProject().getId()))
      .path(ResourceRequest.Name.FEATURESTORES.toString())
      .path(Integer.toString(featureViewAlert.getFeatureView().getFeaturestore().getId()))
      .path(ResourceRequest.Name.FEATUREVIEW.toString()).path(featureViewAlert.getFeatureView().getName())
      .path(ResourceRequest.Name.VERSION.toString())
      .path(String.valueOf(featureViewAlert.getFeatureView().getVersion()))
      .path(ResourceRequest.Name.ALERTS.toString()).path(featureViewAlert.getId().toString());
    dto.setHref(uri.build());
    return dto;
  }
  
  public FeatureViewAlertDTO buildProjectItems(UriInfo uriInfo, ResourceRequest resourceRequest,
    FeatureViewAlert featureViewAlert) {
    FeatureViewAlertDTO dto = new FeatureViewAlertDTO();
    dto = projectUri(dto, uriInfo, featureViewAlert);
    setDtoValues(dto, resourceRequest, featureViewAlert);
    return dto;
  }
  
  public FeatureViewAlertDTO buildMany(UriInfo uriInfo, ResourceRequest resourceRequest,
    List<FeatureViewAlert> featureViewAlerts) {
    FeatureViewAlertDTO dto = new FeatureViewAlertDTO();
    dto.setHref(uriInfo.getAbsolutePathBuilder().build());
    expand(dto, resourceRequest);
    if (dto.isExpand() && featureViewAlerts != null) {
      dto.setCount((long) featureViewAlerts.size());
      featureViewAlerts.forEach(featureViewAlert -> dto.addItem(build(uriInfo, resourceRequest, featureViewAlert)));
      return dto;
    }
    return dto;
  }
  
  public FeatureViewAlertDTO buildManyProjectAlerts(UriInfo uriInfo, ResourceRequest resourceRequest,
    List<FeatureViewAlert> projectAlerts) {
    FeatureViewAlertDTO dto = new FeatureViewAlertDTO();
    dto.setHref(uriInfo.getAbsolutePathBuilder().build());
    expand(dto, resourceRequest);
    if (dto.isExpand() && projectAlerts != null) {
      dto.setCount((long) projectAlerts.size());
      projectAlerts.forEach(
        featureViewAlert -> dto.addItem(buildProjectItems(uriInfo, resourceRequest, featureViewAlert)));
      return dto;
    }
    return dto;
  }
}