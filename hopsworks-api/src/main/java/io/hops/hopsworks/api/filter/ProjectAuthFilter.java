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
 */
package io.hops.hopsworks.api.filter;

import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.util.JsonResponse;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Request filter that can be used to restrict users accesses to projects based
 * on the role they have in the project and the annotation on the method being
 * called.
 */
@Provider
@AllowedProjectRoles
@Priority(Priorities.AUTHORIZATION)
public class ProjectAuthFilter implements ContainerRequestFilter {

  @EJB
  private ProjectTeamFacade projectTeamBean;

  @EJB
  private ProjectFacade projectBean;

  @EJB
  private JWTHelper jWTHelper;

  @Context
  private ResourceInfo resourceInfo;

  private static final Logger LOGGER = Logger.getLogger(ProjectAuthFilter.class.getName());

  @Override
  public void filter(ContainerRequestContext requestContext) {
    MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
    String projectId = pathParameters.getFirst("projectId");
    String projectName = pathParameters.getFirst("projectName");
    String path = requestContext.getUriInfo().getPath();
    Class<?> resourceClass = resourceInfo.getResourceClass();
    Method method = resourceInfo.getResourceMethod();

    if (projectId == null && projectName == null) {
      LOGGER.log(Level.WARNING, "Annotated with AllowedProjectRoles but no project identifier "
          + "(projectId or projectName) found in the requesed path: {0}", path);
      return;
    }
    JsonResponse jsonResponse = new RESTApiJsonResponse();
    Integer id = null;
    String userRole;
    try {
      id = Integer.valueOf(projectId);
    } catch (NumberFormatException ne) {
      //
    }

    Project project = id != null ? projectBean.find(id) : projectBean.findByName(projectName);
    if (project == null) {
      jsonResponse.setErrorCode(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND.getCode());
      jsonResponse.setErrorMsg(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND.getMessage());
      requestContext.abortWith(Response.status(Response.Status.NOT_FOUND).entity(jsonResponse).build());
      return;
    }
    LOGGER.log(Level.FINEST, "Filtering project request path: {0}", project.getName());

    AllowedProjectRoles methodProjectRolesAnnotation = method.getAnnotation(AllowedProjectRoles.class);
    AllowedProjectRoles classProjectRolesAnnotation = resourceClass.getAnnotation(AllowedProjectRoles.class);

    AllowedProjectRoles rolesAnnotation = methodProjectRolesAnnotation != null ? methodProjectRolesAnnotation
        : classProjectRolesAnnotation;
    Set<String> rolesSet;
    rolesSet = new HashSet<>(Arrays.asList(rolesAnnotation.value()));

    Users user = jWTHelper.getUserPrincipal(requestContext);
    if (requestContext.getSecurityContext().getUserPrincipal() == null && user == null) {
      LOGGER.log(Level.WARNING, "Authentication not done. No user found.");
      jsonResponse.setErrorCode(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL.getCode());
      jsonResponse.setErrorMsg(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL.getMessage());
      requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(jsonResponse).build());
      return;
    }

    String email = user != null ? user.getEmail() : requestContext.getSecurityContext().getUserPrincipal().getName();
    userRole = projectTeamBean.findCurrentRole(project, email);

    //If the resource is allowed for all roles check if user is a member of the project. 
    if (userRole != null && !userRole.isEmpty() && rolesSet.contains(AllowedProjectRoles.ANYONE)) {
      LOGGER.log(Level.FINEST, "Accessing resource that is allowed for all members.");
      return;
    }

    //if the resource is only allowed for some roles check if the user have the requierd role for the resource.
    if (userRole == null || userRole.isEmpty()) {
      LOGGER.log(Level.INFO, "Trying to access resource, but you dont have any role in this project");
      jsonResponse.setErrorCode(RESTCodes.UserErrorCode.NO_ROLE_FOUND.getCode());
      jsonResponse.setErrorMsg(RESTCodes.UserErrorCode.NO_ROLE_FOUND.getMessage());
      requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).entity(jsonResponse).build());
    } else if (!rolesSet.contains(userRole)) {
      LOGGER.log(Level.INFO, "Trying to access resource that is only allowed for: {0}, But you are a: {1}",
          new Object[]{rolesSet, userRole});
      jsonResponse.setErrorCode(RESTCodes.ProjectErrorCode.PROJECT_ROLE_FORBIDDEN.getCode());
      jsonResponse.setErrorMsg(RESTCodes.ProjectErrorCode.PROJECT_ROLE_FORBIDDEN.getMessage());
      requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).entity(jsonResponse).build());
    }
  }
}
