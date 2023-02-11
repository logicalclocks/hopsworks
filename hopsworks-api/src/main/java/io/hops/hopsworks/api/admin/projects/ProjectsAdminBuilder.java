/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.api.admin.projects;

import io.hops.hopsworks.api.admin.dto.ProjectAdminInfoDTO;
import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.project.ProjectQuotasController;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectsAdminBuilder {

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectQuotasController projectQuotasController;
  @EJB
  private UsersBuilder usersBuilder;

  public URI uri(UriInfo uriInfo) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.ADMIN.toString().toLowerCase())
        .path(ResourceRequest.Name.PROJECTS.toString().toLowerCase())
        .build();
  }

  public URI uri(UriInfo uriInfo, Project project) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.ADMIN.toString().toLowerCase())
        .path(ResourceRequest.Name.PROJECTS.toString().toLowerCase())
        .path(project.getId().toString())
        .build();
  }

  public ProjectAdminInfoDTO build(UriInfo uriInfo, Project project, ResourceRequest resourceRequest) {
    ProjectAdminInfoDTO projectAdminInfoDTO = new ProjectAdminInfoDTO();
    projectAdminInfoDTO.setHref(uri(uriInfo, project));

    projectAdminInfoDTO.setId(project.getId());
    projectAdminInfoDTO.setName(project.getName());
    projectAdminInfoDTO.setCreator(usersBuilder.build(uriInfo, resourceRequest, project.getOwner()));
    projectAdminInfoDTO.setCreated(project.getCreated());
    projectAdminInfoDTO.setPaymentType(project.getPaymentType());
    projectAdminInfoDTO.setLastQuotaUpdate(project.getLastQuotaUpdate());
    if (resourceRequest.contains(ResourceRequest.Name.QUOTAS)) {
      projectAdminInfoDTO.setProjectQuotas(projectQuotasController.getQuotas(project));
    }

    return projectAdminInfoDTO;
  }

  public ProjectAdminInfoDTO build(UriInfo uriInfo, ResourceRequest resourceRequest) {
    List<Project> projects = projectFacade.findAll();
    ProjectAdminInfoDTO projectAdminInfoDTO = new ProjectAdminInfoDTO();
    projectAdminInfoDTO.setHref(uri(uriInfo));
    projectAdminInfoDTO.setCount((long) projects.size());
    projectAdminInfoDTO.setItems(projects.stream()
        .map(p -> build(uriInfo, p, resourceRequest))
        .collect(Collectors.toList()));

    return projectAdminInfoDTO;
  }

}
