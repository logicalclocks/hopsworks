/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.hops.hopsworks.api.admin;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/admin")
@RolesAllowed({"HOPS_ADMIN"})
@Api(value = "Admin")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectsAdmin {
  private final Logger LOG = Logger.getLogger(ProjectsAdmin.class.getName());
  
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private UserManager userManager;
  
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/projects/{id}")
  public Response deleteProject(@Context SecurityContext sc, @Context HttpServletRequest req,
      @PathParam("id") Integer id) throws AppException {
    Project project = projectFacade.find(id);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Project with id " + id + " does not exist");
    }
    
    String sessionId = req.getSession().getId();
    projectController.removeProject(project.getOwner().getEmail(), id, sessionId);
    LOG.log(Level.INFO, "Deleted project with id: " + id);
  
    JsonResponse response = new JsonResponse();
    response.setStatus(Response.Status.OK.toString());
    response.setSuccessMessage("Project with id " + id + " has been successfully deleted");
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
  
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/projects/createas")
  public Response createProjectAsUser(@Context SecurityContext sc, @Context HttpServletRequest request,
      ProjectDTO projectDTO) throws AppException {
    String userEmail = sc.getUserPrincipal().getName();
    Users user = userManager.findByEmail(userEmail);
    if (user == null || !user.getEmail().equals(Settings.SITE_EMAIL)) {
      LOG.log(Level.WARNING, "Unauthorized or unknown user tried to create a Project as another user");
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
          ResponseMessages.AUTHENTICATION_FAILURE);
    }
    
    String ownerEmail = projectDTO.getOwner();
    if (ownerEmail == null) {
      LOG.log(Level.WARNING, "Owner username is null");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Owner email cannot be null");
    }
    
    Users owner = userManager.findByEmail(ownerEmail);
    if (owner == null) {
      LOG.log(Level.WARNING, "Owner is not in the database");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Unknown owner user " + ownerEmail);
    }
    
    List<String> failedMembers = null;
    projectController.createProject(projectDTO, owner, failedMembers, request.getSession().getId());
    
    JsonResponse response = new JsonResponse();
    response.setStatus("201");
    response.setSuccessMessage(ResponseMessages.PROJECT_CREATED);
    
    if (failedMembers != null && !failedMembers.isEmpty()) {
      response.setFieldErrors(failedMembers);
    }
    
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED)
        .entity(response).build();
  }
}
