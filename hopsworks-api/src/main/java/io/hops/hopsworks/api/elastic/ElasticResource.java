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
package io.hops.hopsworks.api.elastic;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.elastic.featurestore.ElasticFeaturestoreBuilder;
import io.hops.hopsworks.api.elastic.featurestore.ElasticFeaturestoreDTO;
import io.hops.hopsworks.api.elastic.featurestore.ElasticFeaturestoreRequest;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.elastic.FeaturestoreDocType;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.logging.Level;
import java.util.logging.Logger;

@Api(value = "Elastic Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ElasticResource {
  private static final Logger LOGGER = Logger.getLogger(ElasticResource.class.getName());
  
  @Inject
  private ElasticFeaturestoreBuilder elasticFeaturestoreBuilder;
  
  private Integer projectId;
  private String projectName;
  
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  /**
   * Searches for content inside all project accesible featurestores. Hits 'featurestore' index
   * <p/>
   * @param searchTerm
   * @param sc
   * @return
   */
  @ApiOperation(value = "Search project accesible featurestores",
    notes ="featurestore documents: featuregroups, features, training datasets",
    response = ElasticFeaturestoreDTO.class)
  @GET
  @Path("featurestore/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  // TODO ApiKey scope for search
  // @ApiKeyRequired( acceptedScopes = {ApiScope.DATASET_VIEW}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response featurestoreSearch(
    @PathParam("searchTerm") String searchTerm,
    @QueryParam("docType") @DefaultValue("ALL")
      FeaturestoreDocType docType,
    @QueryParam("from") @ApiParam(value="search pointer position, if none given, it defaults to 0") Integer from,
    @QueryParam("size") @ApiParam(value="search page size, if none give, it defaults to 100." +
      "Cannot be negative and cannot be bigger than 10000") Integer size,
    @Context SecurityContext sc)
    throws ServiceException, ElasticException, GenericException {
    if (Strings.isNullOrEmpty(searchTerm)) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.WARNING, "no search term provided");
    }
    if(from == null) {
      from = 0;
    }
    if(size == null) {
      size = 100;
    }
    
    ElasticFeaturestoreDTO dto = elasticFeaturestoreBuilder.build(
        new ElasticFeaturestoreRequest(searchTerm, docType, from, size), projectId);
    return Response.ok().entity(dto).build();
  }
}
