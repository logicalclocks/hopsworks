/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.python.conflicts;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

@Api(value = "Python Environment Conflicts Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EnvironmentConflictsResource {

  @EJB
  private ConflictBuilder conflictBuilder;
  @EJB
  private EnvironmentController environmentController;
  
  private Project project;
  private String pythonVersion;

  public EnvironmentConflictsResource setProject(Project project, String pythonVersion) {
    this.project = project;
    this.pythonVersion = pythonVersion;
    return this;
  }

  public Project getProject() {
    return project;
  }

  @ApiOperation(value = "Get conflicts for this environment")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response get(@BeanParam ConflictBeanParam environmentConflictBeanParam,
    @Context UriInfo uriInfo, @Context SecurityContext sc)
      throws IOException, ServiceDiscoveryException, PythonException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CONFLICTS);
    resourceRequest.setFilter(environmentConflictBeanParam.getFilter());

    environmentController.checkCondaEnabled(project, pythonVersion, true);
    ConflictDTO dto = conflictBuilder.build(uriInfo, resourceRequest, project);
    return Response.ok().entity(dto).build();
  }
}
