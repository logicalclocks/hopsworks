/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.featurestore.datavalidation.alert;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupAlertFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupAlertBuilder {
  
  private static final Logger LOGGER = Logger.getLogger(FeatureGroupAlertBuilder.class.getName());
  
  @EJB
  private FeatureGroupAlertFacade featureGroupAlertFacade;
  
  public FeatureGroupAlertDTO uri(FeatureGroupAlertDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .build());
    return dto;
  }
  
  public FeatureGroupAlertDTO uri(FeatureGroupAlertDTO dto, UriInfo uriInfo,
      FeatureGroupAlert featureGroupAlert) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .path(featureGroupAlert.getId().toString())
        .build());
    return dto;
  }
  
  public FeatureGroupAlertDTO expand(FeatureGroupAlertDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.ALERTS)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public FeatureGroupAlertDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
      FeatureGroupAlert featureGroupAlert) {
    FeatureGroupAlertDTO dto = new FeatureGroupAlertDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(featureGroupAlert.getId());
      dto.setAlertType(featureGroupAlert.getAlertType());
      dto.setStatus(featureGroupAlert.getStatus());
      dto.setSeverity(featureGroupAlert.getSeverity());
      dto.setCreated(featureGroupAlert.getCreated());
    }
    return dto;
  }
  
  public FeatureGroupAlertDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest,
      FeatureGroupAlert featureGroupAlert) {
    FeatureGroupAlertDTO dto = new FeatureGroupAlertDTO();
    uri(dto, uriInfo, featureGroupAlert);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(featureGroupAlert.getId());
      dto.setAlertType(featureGroupAlert.getAlertType());
      dto.setStatus(featureGroupAlert.getStatus());
      dto.setSeverity(featureGroupAlert.getSeverity());
      dto.setCreated(featureGroupAlert.getCreated());
    }
    return dto;
  }
  
  /**
   * Build a single FeatureGroupAlert
   *
   * @param uriInfo
   * @param resourceRequest
   * @param id
   * @return
   */
  public FeatureGroupAlertDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Featuregroup featuregroup,
      Integer id) throws FeaturestoreException {
    FeatureGroupAlert featureGroupAlert = featureGroupAlertFacade.findByFeatureGroupAndId(featuregroup, id);
    if (featureGroupAlert == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_NOT_FOUND, Level.FINE);
    }
    return build(uriInfo, resourceRequest, featureGroupAlert);
  }
  
  /**
   * Build a list of FeatureGroupAlerts
   *
   * @param uriInfo
   * @param resourceRequest
   * @return
   */
  public FeatureGroupAlertDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Featuregroup featuregroup) {
    return items(new FeatureGroupAlertDTO(), uriInfo, resourceRequest, featuregroup);
  }
  
  private FeatureGroupAlertDTO items(FeatureGroupAlertDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      Featuregroup featuregroup) {
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo =
          featureGroupAlertFacade.findAllFeatureGroupAlerts(resourceRequest.getOffset()
              , resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort(),
              featuregroup);
      dto.setCount(collectionInfo.getCount());
      return items(dto, uriInfo, resourceRequest, collectionInfo.getItems());
    }
    return dto;
  }
  
  private FeatureGroupAlertDTO items(FeatureGroupAlertDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      List<FeatureGroupAlert> featureGroupAlerts) {
    if (featureGroupAlerts != null && !featureGroupAlerts.isEmpty()) {
      featureGroupAlerts
          .forEach((featureGroupAlert) -> dto.addItem(buildItems(uriInfo, resourceRequest, featureGroupAlert)));
    }
    return dto;
  }
}