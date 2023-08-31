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
import io.hops.hopsworks.api.provenance.ops.ProvUsageBuilder;
import io.hops.hopsworks.api.provenance.ops.ProvUsageBeanParams;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.api.provenance.ops.dto.ProvArtifactUsageParentDTO;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
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
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Logged
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Artifact Provenance Service", description = "Artifact Provenance Service")
public class ProvArtifactResource {
  @Inject
  private ProvUsageBuilder usageBuilder;
  @EJB
  private JWTHelper jWTHelper;
  
  private Project userProject;
  private DatasetPath targetEndpoint;
  private String artifactId;
  
  @Logged(logLevel = LogLevel.OFF)
  public void setContext(Project userProject, DatasetPath targetEndpoint) {
    this.userProject = userProject;
    this.targetEndpoint = targetEndpoint;
  }
  
  @Logged(logLevel = LogLevel.OFF)
  public void setArtifactId(String name, Integer version) {
    this.artifactId = name + "_" + version;
  }
  
  @GET
  @Path("usage")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Artifact usage", response = ProvArtifactUsageParentDTO.class)
  public Response status(@BeanParam ProvUsageBeanParams params,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc)
      throws ProvenanceException, GenericException, DatasetException, MetadataException, FeatureStoreMetadataException {
    Users user = jWTHelper.getUserPrincipal(sc);
    ProvArtifactUsageParentDTO status = usageBuilder.buildAccessible(uriInfo, user, targetEndpoint, artifactId,
      params.getUsageType());
    return Response.ok().entity(status).build();
  }
}
