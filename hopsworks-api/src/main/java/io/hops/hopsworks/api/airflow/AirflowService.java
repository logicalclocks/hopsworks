/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 */
package io.hops.hopsworks.api.airflow;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.project.ProjectSubResource;
import io.hops.hopsworks.common.airflow.AirflowController;
import io.hops.hopsworks.common.airflow.AirflowDagDTO;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.AirflowException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Airflow related endpoints")
public class AirflowService extends ProjectSubResource {
  @EJB
  private ProjectController projectController;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private AirflowController airflowController;

  public AirflowService() {
  }

  @Override
  protected ProjectController getProjectController() {
    return projectController;
  }

  @POST
  @Path("/dag")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Generate an Airflow Python DAG file from a DAG definition")
  public Response composeDAG(AirflowDagDTO dagDefinition,
                             @Context HttpServletRequest req,
                             @Context SecurityContext sc) throws AirflowException, ProjectException {
    Users user = jwtHelper.getUserPrincipal(sc);
    airflowController.composeDAG(getProject(), user, dagDefinition);
    return Response.ok().build();
  }
}
