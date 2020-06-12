/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.elastic;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.elastic.featurestore.ElasticFeaturestoreBuilder;
import io.hops.hopsworks.api.elastic.featurestore.ElasticFeaturestoreDTO;
import io.hops.hopsworks.api.elastic.featurestore.ElasticFeaturestoreRequest;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.ElasticJWTResponseDTO;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.elastic.FeaturestoreDocType;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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

@Path("/elastic")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Elastic Service", description = "Elastic Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ElasticService {

  private static final Logger logger = Logger.getLogger(ElasticService.class.getName());

  @EJB
  private ElasticHitsBuilder elasticHitsBuilder;
  @EJB
  private JWTHelper jWTHelper;
  @Inject
  private ElasticFeaturestoreBuilder elasticFeaturestoreBuilder;
  
  /**
   * Searches for content composed of projects and datasets. Hits two elastic
   * indices: 'project' and 'dataset'
   * <p/>
   * @param searchTerm
   * @param sc
   * @return
   */
  @GET
  @Path("globalsearch/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response globalSearch(@PathParam("searchTerm") String searchTerm, @Context SecurityContext sc)
    throws ServiceException, ElasticException {

    if (Strings.isNullOrEmpty(searchTerm)) {
      throw new IllegalArgumentException("searchTerm was not provided or was empty");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    ElasticHitDTO elasticHitDTO = elasticHitsBuilder.buildElasticHits(searchTerm, user);
    return Response.ok().entity(elasticHitDTO).build();
  }

  /**
   * Searches for content inside a specific project. Hits 'project' index
   * <p/>
   * @param projectId
   * @param searchTerm
   * @param sc
   * @return
   */
  @GET
  @Path("projectsearch/{projectId}/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response projectSearch(@PathParam("projectId") Integer projectId, @PathParam("searchTerm") String searchTerm,
    @Context SecurityContext sc) throws ServiceException, ElasticException {
    if (Strings.isNullOrEmpty(searchTerm) || projectId == null) {
      throw new IllegalArgumentException("One or more required parameters were not provided.");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    ElasticHitDTO elasticHitDTO = elasticHitsBuilder.buildElasticHits(projectId, searchTerm, user);
    return Response.ok().entity(elasticHitDTO).build();
  }

  /**
   * Searches for content inside a specific dataset. Hits 'dataset' index
   * <p/>
   * @param projectId
   * @param datasetName
   * @param searchTerm
   * @param sc
   * @return
   */
  @GET
  @Path("datasetsearch/{projectId}/{datasetName}/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response datasetSearch(
      @PathParam("projectId") Integer projectId,
      @PathParam("datasetName") String datasetName,
      @PathParam("searchTerm") String searchTerm, @Context SecurityContext sc)
      throws ServiceException, ElasticException {
  
    if (Strings.isNullOrEmpty(searchTerm) || Strings.isNullOrEmpty(datasetName) || projectId == null) {
      throw new IllegalArgumentException("One or more required parameters were not provided.");
    }
  
    Users user = jWTHelper.getUserPrincipal(sc);
    ElasticHitDTO elasticHitDTO = elasticHitsBuilder.buildElasticHits(projectId, datasetName, searchTerm, user);
    return Response.ok().entity(elasticHitDTO).build();
  }
  
  /**
   * Searches for content inside all featurestore. Hits 'featurestore' index
   * <p/>
   * @param searchTerm
   * @param sc
   * @return
   */
  @ApiOperation(value = "Search all featurestores",
    notes ="featurestore documents: featuregroups, features, training datasets",
    response = ElasticFeaturestoreDTO.class)
  @GET
  @Path("featurestore/{searchTerm}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response featurestoreSearch(
    @PathParam("searchTerm") String searchTerm,
    @QueryParam("docType") @DefaultValue("ALL") FeaturestoreDocType docType,
    @QueryParam("from") @ApiParam(value="search pointer position, if none given, it defaults to 0") Integer from,
    @QueryParam("size") @ApiParam(value="search page size, if none give, it defaults to 100." +
      "Cannot be negative and cannot be bigger than 10000") Integer size,
    @Context SecurityContext sc)
    throws ServiceException, ElasticException, GenericException {
    if (Strings.isNullOrEmpty(searchTerm)) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.WARNING, "no search term provided");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    
    if(from == null) {
      from = 0;
    }
    if(size == null) {
      size = 100;
    }
    ElasticFeaturestoreDTO dto = elasticFeaturestoreBuilder.build(user,
        new ElasticFeaturestoreRequest(searchTerm, docType, from, size));
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation( value = "Get a jwt token for elastic.")
  @GET
  @Path("jwt/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response createJwtToken(@Context SecurityContext sc, @PathParam(
      "projectId") Integer projectId) throws ElasticException {
    if (projectId == null) {
      throw new IllegalArgumentException("projectId was not provided.");
    }
    ElasticJWTResponseDTO
        jWTResponseDTO = jWTHelper.createTokenForELK(sc, projectId);
    return Response.ok().entity(jWTResponseDTO).build();
  }

}
