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
package io.hops.hopsworks.api.jobs.alert;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobAlertsFacade;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.persistence.entity.jobs.description.JobAlert;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
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
public class JobAlertsBuilder {
  
  private static final Logger LOGGER = Logger.getLogger(JobAlertsBuilder.class.getName());
  
  @EJB
  private JobAlertsFacade jobalertsFacade;
  
  public JobAlertsDTO uri(JobAlertsDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .build());
    return dto;
  }
  
  public JobAlertsDTO uri(JobAlertsDTO dto, UriInfo uriInfo, JobAlert jobalert) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .path(jobalert.getId().toString())
        .build());
    return dto;
  }
  
  public JobAlertsDTO expand(JobAlertsDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.ALERTS)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public JobAlertsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, JobAlert jobAlert) {
    JobAlertsDTO dto = new JobAlertsDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(jobAlert.getId());
      dto.setAlertType(jobAlert.getAlertType());
      dto.setStatus(jobAlert.getStatus());
      dto.setSeverity(jobAlert.getSeverity());
      dto.setCreated(jobAlert.getCreated());
    }
    return dto;
  }
  
  public JobAlertsDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, JobAlert jobAlert) {
    JobAlertsDTO dto = new JobAlertsDTO();
    uri(dto, uriInfo, jobAlert);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(jobAlert.getId());
      dto.setAlertType(jobAlert.getAlertType());
      dto.setStatus(jobAlert.getStatus());
      dto.setSeverity(jobAlert.getSeverity());
      dto.setCreated(jobAlert.getCreated());
    }
    return dto;
  }
  
  /**
   * Build a single JobAlert
   *
   * @param uriInfo
   * @param resourceRequest
   * @param id
   * @return
   */
  public JobAlertsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Jobs jobs, Integer id)
      throws JobException {
    JobAlert jobAlert = jobalertsFacade.findByJobAndId(jobs, id);
    if (jobAlert == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_ALERT_NOT_FOUND, Level.FINE,
          "Job alert not found. Id=" + id.toString());
    }
    return build(uriInfo, resourceRequest, jobAlert);
  }
  
  public JobAlertsDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Jobs jobs) {
    return items(new JobAlertsDTO(), uriInfo, resourceRequest, jobs);
  }
  
  private JobAlertsDTO items(JobAlertsDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, Jobs jobs) {
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = jobalertsFacade.findAllJobAlerts(resourceRequest.getOffset()
          , resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort(), jobs);
      dto.setCount(collectionInfo.getCount());
      return items(dto, uriInfo, resourceRequest, collectionInfo.getItems());
    }
    return dto;
  }
  
  private JobAlertsDTO items(JobAlertsDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      List<JobAlert> jobAlerts) {
    if (jobAlerts != null && !jobAlerts.isEmpty()) {
      jobAlerts.forEach((jobAlert) -> dto.addItem(buildItems(uriInfo, resourceRequest, jobAlert)));
    }
    return dto;
  }
}