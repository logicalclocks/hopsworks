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

package io.hops.hopsworks.api.project.jobconfig;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.jobconfig.DefaultJobConfigurationFacade;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.jobs.DefaultJobConfiguration;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DefaultJobConfigurationBuilder {

  @EJB
  private DefaultJobConfigurationFacade projectJobConfigurationFacade;

  public DefaultJobConfigurationDTO uri(DefaultJobConfigurationDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.JOBCONFIG.toString().toLowerCase())
      .build());
    return dto;
  }

  public DefaultJobConfigurationDTO uri(DefaultJobConfigurationDTO dto, UriInfo uriInfo,
                                        DefaultJobConfiguration defaultJobConfiguration,
                                        JobType jobType) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(defaultJobConfiguration.getDefaultJobConfigurationPK().getProjectId()))
      .path(ResourceRequest.Name.JOBCONFIG.toString().toLowerCase())
      .path(jobType.name().toLowerCase())
      .build());
    return dto;
  }

  public DefaultJobConfigurationDTO expand(DefaultJobConfigurationDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.JOBCONFIG)) {
      dto.setExpand(true);
    }
    return dto;
  }

  public DefaultJobConfigurationDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                                          DefaultJobConfiguration defaultJobConfiguration, JobType jobType) {
    DefaultJobConfigurationDTO dto = new DefaultJobConfigurationDTO();
    uri(dto, uriInfo, defaultJobConfiguration, jobType);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setJobType(jobType);
      dto.setConfig(defaultJobConfiguration.getJobConfig());
    }
    return dto;
  }

  public DefaultJobConfigurationDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    DefaultJobConfigurationDTO dto = new DefaultJobConfigurationDTO();
    uri(dto, uriInfo, project);
    expand(dto, resourceRequest);
    if(dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo =
        projectJobConfigurationFacade.findByProject(resourceRequest.getOffset(),
        resourceRequest.getLimit(),
        resourceRequest.getFilter(),
        resourceRequest.getSort(), project);
      dto.setCount(collectionInfo.getCount());
      collectionInfo.getItems().forEach((config) -> dto.addItem(build(uriInfo, resourceRequest,
        (DefaultJobConfiguration) config,
        ((DefaultJobConfiguration) config).getDefaultJobConfigurationPK().getType())));
    }
    return dto;
  }
}