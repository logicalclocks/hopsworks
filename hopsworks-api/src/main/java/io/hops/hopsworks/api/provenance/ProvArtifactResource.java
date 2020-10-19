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
package io.hops.hopsworks.api.provenance;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.provenance.ops.ProvOpsBeanParams;
import io.hops.hopsworks.api.provenance.ops.ProvUsageParams;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.provenance.ops.ProvOpsElasticComm;
import io.hops.hopsworks.common.provenance.ops.ProvOpsParams;
import io.hops.hopsworks.common.provenance.ops.ProvUsageBuilderIface;
import io.hops.hopsworks.common.provenance.ops.dto.ProvArtifactUsageParentDTO;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashSet;

@Logged
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Artifact Provenance Service", description = "Artifact Provenance Service")
public class ProvArtifactResource {
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private JWTHelper jWTHelper;
  @Inject
  private ProvUsageBuilderIface usageBuilder;
  
  private Project project;
  private String artifactId;
  
  @Logged(logLevel = LogLevel.OFF)
  public void setProjectId(Integer projectId) {
    this.project = projectFacade.find(projectId);
  }
  
  @Logged(logLevel = LogLevel.OFF)
  public void setProject(Project project) {
    this.project = project;
  }
  
  @Logged(logLevel = LogLevel.OFF)
  public void setArtifactId(String name, Integer version) {
    this.artifactId = name + "_" + version;
  }
  
  @GET
  @Path("usage")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Artifact usage", response = ProvArtifactUsageParentDTO.class)
  public Response status(
    @BeanParam ProvUsageParams params,
    @Context HttpServletRequest req)
    throws ProvenanceException, GenericException {
    ProvOpsBeanParams opsParams = getUsageOpsParams(artifactId);
    ProvArtifactUsageParentDTO status = usageBuilder.build(project, artifactId, opsParams, params.getUsageType());
    return Response.ok().entity(status).build();
  }
  
  private ProvOpsBeanParams getUsageOpsParams(String mlId) {
    ProvOpsBeanParams params = new ProvOpsBeanParams();
    params.setFileOpsFilterBy(new HashSet<>(Arrays.asList(
      "ML_ID:" + mlId
    )));
    params.setAggregations(new HashSet<>(Arrays.asList(ProvOpsElasticComm.Aggregations.APP_USAGE.toString())));
    params.setReturnType(ProvOpsParams.ReturnType.AGGREGATIONS);
    return params;
  }
}
