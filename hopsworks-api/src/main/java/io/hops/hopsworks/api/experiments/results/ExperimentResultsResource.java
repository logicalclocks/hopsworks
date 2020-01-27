/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.experiments.results;

import io.hops.hopsworks.api.experiments.dto.results.ExperimentResultSummaryDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.exceptions.ExperimentsException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
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
import java.util.logging.Level;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ExperimentResultsResource {

  private Project project;
  private String experimentId;

  @EJB
  private ExperimentResultsBuilder experimentResultsBuilder;
  
  public ExperimentResultsResource setProject(Project project, String experimentId) {
    this.project = project;
    this.experimentId = experimentId;
    return this;
  }
  
  public Project getProject() {
    return project;
  }

  @ApiOperation(value = "Get results information", response = ExperimentResultSummaryDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getResults(@Context UriInfo uriInfo,
                             @BeanParam Pagination pagination,
                             @BeanParam ExperimentResultsBeanParam experimentResultsBeanParam,
    @Context SecurityContext sc) throws ExperimentsException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.RESULTS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(experimentResultsBeanParam.getSortBySet());

    ExperimentResultSummaryDTO dto = experimentResultsBuilder.build(uriInfo, resourceRequest, project, experimentId);
    if(dto == null) {
      throw new ExperimentsException(RESTCodes.ExperimentsErrorCode.RESULTS_NOT_FOUND, Level.FINE);
    }
    return Response.ok().entity(dto).build();
  }
}
