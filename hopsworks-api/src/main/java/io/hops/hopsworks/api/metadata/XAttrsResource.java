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
package io.hops.hopsworks.api.metadata;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.hdfs.xattrs.XAttrsController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

@Api(value = "Extended Attributes Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class XAttrsResource {
  
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private XAttrsController xattrsController;
  @EJB
  private XAttrsBuilder xattrsBuilder;
  
  private Project project;
  
  public void setProject(Integer projectId) {
    this.project = projectFacade.find(projectId);
  }
  
  
  @ApiOperation( value = "Create or Update an extended attribute for a path."
      , response = XAttrDTO.class)
  @PUT
  @Path("{path: .+}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response put(@Context
      SecurityContext sc, @Context
      UriInfo uriInfo, @PathParam("path") String path,
      @QueryParam("name") String xattrName,
      String metaObj) throws DatasetException,
      MetadataException {
    Users user = jWTHelper.getUserPrincipal(sc);
    
    Response.Status status = Response.Status.OK;
    if(xattrsController.addXAttr(project, user, path, xattrName,
        metaObj)){
      status = Response.Status.CREATED;
    }
    
    ResourceRequest resourceRequest =
        new ResourceRequest(ResourceRequest.Name.XATTRS);
    XAttrDTO dto = xattrsBuilder.build(uriInfo, resourceRequest, project,
        path, xattrName);
    
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(status == Response.Status.CREATED) {
      return Response.created(builder.build()).entity(dto).build();
    } else {
      return Response.ok(builder.build()).entity(dto).build();
    }
  }
  
  @ApiOperation( value = "Get extended attributes attached to a path.",
      response = XAttrDTO.class)
  @GET
  @Path("{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response get(@Context SecurityContext sc, @Context UriInfo uriInfo,
      @PathParam("path") String path, @QueryParam("name") String xattrName)
      throws DatasetException, MetadataException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Map<String, String> result = xattrsController.getXAttrs(project, user,
        path, xattrName);
    
    Response.Status status = result.isEmpty() ?
        Response.Status.NOT_FOUND : Response.Status.ACCEPTED;
    ResourceRequest resourceRequest =
        new ResourceRequest(ResourceRequest.Name.XATTRS);
    XAttrDTO dto = xattrsBuilder.build(uriInfo, resourceRequest, project,
        path, result);
    return Response.status(status).entity(dto).build();
  }
  
  
  @ApiOperation( value = "Delete the extended attributes attached to a path.")
  @DELETE
  @Path("{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response delete(@Context SecurityContext sc,
      @PathParam("path") String path, @QueryParam("name") String xattrName)
      throws DatasetException, MetadataException {
    
    Users user = jWTHelper.getUserPrincipal(sc);
    Response.Status status = Response.Status.NOT_FOUND;
    if(xattrsController.removeXAttr(project, user, path, xattrName)){
      status = Response.Status.NO_CONTENT;
    }
    return Response.status(status).build();
  }
}
