/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.provenance.explicit.ExplicitProvenanceExpansionBeanParam;
import io.hops.hopsworks.api.provenance.explicit.ProvExplicitLinksBuilder;
import io.hops.hopsworks.api.provenance.ops.ProvLinksBeanParams;
import io.hops.hopsworks.api.provenance.ops.ProvUsageBeanParams;
import io.hops.hopsworks.api.provenance.ops.ProvUsageBuilder;
import io.hops.hopsworks.api.provenance.ops.dto.ProvArtifactUsageParentDTO;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.api.provenance.explicit.dto.ProvExplicitLinkDTO;
import io.hops.hopsworks.common.provenance.explicit.ProvExplicitControllerIface;
import io.hops.hopsworks.common.provenance.explicit.ProvExplicitLink;
import io.hops.hopsworks.common.provenance.ops.dto.ProvLinksDTO;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
import java.io.IOException;

@Logged
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Provenance Resource")
public abstract class ProvenanceResource<R> {
  @Inject
  protected ProvExplicitControllerIface provCtrl;
  @EJB
  private ProvExplicitLinksBuilder linksBuilder;
  @EJB
  private ProvUsageBuilder usageBuilder;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  protected DatasetHelper datasetHelper;
  
  protected Project project;
  
  protected abstract R getArtifact() throws FeaturestoreException;
  
  protected abstract ProvExplicitLink<R> getExplicitLinks(Project accessProject,
                                                          Integer upstreamLvls, Integer downstreamLvls)
    throws FeaturestoreException, GenericException, DatasetException;
  
  /**
   * this is id is used by implicit provenance and will be phased out in the future.
   * The expected values is <name>_<version>
   */
  @Deprecated
  protected abstract String getArtifactId() throws FeaturestoreException;
  
  /**
   * This is the parent Dataset of the Artifact. Access is granted at dataset level, not artifact level,
   * and we need this path for access control when dealing with shared datasets.
   */
  protected abstract DatasetPath getArtifactDatasetPath() throws FeaturestoreException, DatasetException;
  
  public ProvenanceResource() {}
  
  @Logged(logLevel = LogLevel.OFF)
  public void setProject(Project project) {
    this.project = project;
  }
  
  @GET
  @Path("links")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
               allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.FEATURESTORE},
                  allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiOperation(value = "Links Provenance query endpoint",
                response = ProvLinksDTO.class)
  public Response getLinks(
    @BeanParam ProvLinksBeanParams params,
    @BeanParam LinksPagination pagination,
    @BeanParam ExplicitProvenanceExpansionBeanParam explicitProvenanceExpansionBeanParam,
    @Context UriInfo uriInfo,
    @Context HttpServletRequest req,
    @Context SecurityContext sc)
    throws GenericException, FeaturestoreException, DatasetException, ServiceException, MetadataException,
           SchematizedTagException, IOException, CloudException {
    Users user = jwtHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.PROVENANCE);
    resourceRequest.setExpansions(explicitProvenanceExpansionBeanParam.getResources());
    ProvExplicitLinkDTO<?> result = linksBuilder.build(uriInfo, resourceRequest, project, user,
      getExplicitLinks(project, pagination.getUpstreamLvls(), pagination.getDownstreamLvls()));
    return Response.ok().entity(result).build();
  }
  
  @GET
  @Path("usage")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Artifact usage", response = ProvArtifactUsageParentDTO.class)
  public Response status(@BeanParam ProvUsageBeanParams params,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc)
    throws ProvenanceException, GenericException, DatasetException, MetadataException, SchematizedTagException,
           FeaturestoreException {
    Users user = jwtHelper.getUserPrincipal(sc);
    ProvArtifactUsageParentDTO status = usageBuilder.buildAccessible(uriInfo, user, getArtifactDatasetPath(),
      getArtifactId(), params.getUsageType());
    return Response.ok().entity(status).build();
  }
}
