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
package io.hops.hopsworks.api.project.alert;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.alert.ProjectServiceAlertsFacade;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlert;
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
public class ProjectAlertsBuilder {

  private static final Logger LOGGER = Logger.getLogger(ProjectAlertsBuilder.class.getName());

  @EJB
  private ProjectServiceAlertsFacade projectServiceAlertsFacade;

  public ProjectAlertsDTO uri(ProjectAlertsDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .build());
    return dto;
  }

  public ProjectAlertsDTO uri(ProjectAlertsDTO dto, UriInfo uriInfo, ProjectServiceAlert alert) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .path(alert.getId().toString())
        .build());
    return dto;
  }

  public ProjectAlertsDTO uriAll(ProjectAlertsDTO dto, UriInfo uriInfo, ProjectServiceAlert alert) {
    dto.setHref(uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString())
        .path(Integer.toString(alert.getProject().getId()))
        .path("service")
        .path(ResourceRequest.Name.ALERTS.toString())
        .path(Integer.toString(alert.getId()))
        .build());
    return dto;
  }

  public ProjectAlertsDTO expand(ProjectAlertsDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.ALERTS)) {
      dto.setExpand(true);
    }
    return dto;
  }

  private void setValues(ProjectAlertsDTO dto, ResourceRequest resourceRequest, ProjectServiceAlert alert) {
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(alert.getId());
      dto.setAlertType(alert.getAlertType());
      dto.setStatus(alert.getStatus());
      dto.setSeverity(alert.getSeverity());
      dto.setService(alert.getService());
      dto.setCreated(alert.getCreated());
      dto.setProjectName(alert.getProject().getName());
      dto.setReceiver(alert.getReceiver().getName());
    }
  }

  public ProjectAlertsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, ProjectServiceAlert alert) {
    ProjectAlertsDTO dto = new ProjectAlertsDTO();
    uri(dto, uriInfo);
    setValues(dto, resourceRequest, alert);
    return dto;
  }

  public ProjectAlertsDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, ProjectServiceAlert alert) {
    ProjectAlertsDTO dto = new ProjectAlertsDTO();
    uri(dto, uriInfo, alert);
    setValues(dto, resourceRequest, alert);
    return dto;
  }

  public ProjectAlertsDTO buildItemAll(UriInfo uriInfo, ResourceRequest resourceRequest, ProjectServiceAlert alert) {
    ProjectAlertsDTO dto = new ProjectAlertsDTO();
    uriAll(dto, uriInfo, alert);
    setValues(dto, resourceRequest, alert);
    return dto;
  }

  /**
   * Build a single Alert
   *
   * @param uriInfo
   * @param resourceRequest
   * @param id
   * @return
   */
  public ProjectAlertsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Integer id)
      throws ProjectException {
    ProjectServiceAlert projectServiceAlert = projectServiceAlertsFacade.findByProjectAndId(project, id);
    if (projectServiceAlert == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_NOT_FOUND, Level.FINE,
          "Alert not found. Id=" + id.toString());
    }
    return build(uriInfo, resourceRequest, projectServiceAlert);
  }

  public ProjectAlertsDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    return items(new ProjectAlertsDTO(), uriInfo, resourceRequest, project);
  }

  public ProjectAlertsDTO buildItemsAll(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    return itemsAll(new ProjectAlertsDTO(), uriInfo, resourceRequest, project);
  }

  private ProjectAlertsDTO items(ProjectAlertsDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      Project project) {
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = projectServiceAlertsFacade.findAllProjectAlerts(
          resourceRequest.getOffset(), resourceRequest.getLimit(), resourceRequest.getFilter(),
          resourceRequest.getSort(), project);
      dto.setCount(collectionInfo.getCount());
      return items(dto, uriInfo, resourceRequest, collectionInfo.getItems());
    }
    return dto;
  }

  private ProjectAlertsDTO itemsAll(ProjectAlertsDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      Project project) {
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = projectServiceAlertsFacade.findAllProjectAlerts(
          resourceRequest.getOffset(), resourceRequest.getLimit(), resourceRequest.getFilter(),
          resourceRequest.getSort(), project);
      dto.setCount(collectionInfo.getCount());
      return itemsAll(dto, uriInfo, resourceRequest, collectionInfo.getItems());
    }
    return dto;
  }

  private ProjectAlertsDTO items(ProjectAlertsDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      List<ProjectServiceAlert> alerts) {
    if (alerts != null && !alerts.isEmpty()) {
      alerts.forEach((alert) -> dto.addItem(buildItems(uriInfo, resourceRequest, alert)));
    }
    return dto;
  }

  private ProjectAlertsDTO itemsAll(ProjectAlertsDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      List<ProjectServiceAlert> alerts) {
    if (alerts != null && !alerts.isEmpty()) {
      alerts.forEach((alert) -> dto.addItem(buildItemAll(uriInfo, resourceRequest, alert)));
    }
    return dto;
  }
}