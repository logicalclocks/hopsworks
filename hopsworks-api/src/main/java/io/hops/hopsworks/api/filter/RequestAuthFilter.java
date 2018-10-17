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

package io.hops.hopsworks.api.filter;

import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.util.JsonResponse;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Request filter that can be used to restrict users accesses to projects based
 * on the role they have for the project and the annotation on the method being
 * called.
 */
@Provider
public class RequestAuthFilter implements ContainerRequestFilter {
  
  @EJB
  private ProjectTeamFacade projectTeamBean;
  
  @EJB
  private ProjectFacade projectBean;
  
  @Context
  private ResourceInfo resourceInfo;
  
  private static final Logger LOGGER = Logger.getLogger(RequestAuthFilter.class.
    getName());
  
  @Override
  public void filter(ContainerRequestContext requestContext) {
    
    String path = requestContext.getUriInfo().getPath();
    Method method = resourceInfo.getResourceMethod();
    String[] pathParts = path.split("/");
    LOGGER.log(Level.FINEST, "Rest call to {0}, from {1}.", new Object[]{path,
      requestContext.getSecurityContext().getUserPrincipal()});
    LOGGER.log(Level.FINEST, "Filtering request path: {0}", pathParts[0]);
    LOGGER.log(Level.FINEST, "Method called: {0}", method.getName());
    //intercepted method must be a project operations on a specific project
    //with an id (/project/projectId/... or /activity/projectId/...). 
    if (pathParts.length > 1 && (pathParts[0].equalsIgnoreCase("project")
      || pathParts[0].equalsIgnoreCase("activity")
      || pathParts[0].equalsIgnoreCase("notebook")
      || pathParts[0].equalsIgnoreCase("interpreter"))) {
      
      JsonResponse jsonResponse = new RESTApiJsonResponse();
      Integer projectId;
      String userRole;
      try {
        projectId = Integer.valueOf(pathParts[1]);
      } catch (NumberFormatException ne) {
        //if the second pathparam is not a project id return.
        LOGGER.log(Level.INFO,"Call to {0} has no project id, leaving interceptor.",path);
        return;
      }
      
      Project project = projectBean.find(projectId);
      if (project == null) {
        jsonResponse.setErrorCode(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND.getCode());
        jsonResponse.setErrorMsg(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND.getMessage());
        requestContext.abortWith(Response.status(Response.Status.NOT_FOUND).entity(jsonResponse).build());
        
        return;
      }
      LOGGER.log(Level.FINEST, "Filtering project request path: {0}", project.getName());
      
      if (!method.isAnnotationPresent(AllowedProjectRoles.class)) {
        jsonResponse.setErrorCode(RESTCodes.GenericErrorCode.ENDPOINT_ANNOTATION_MISSING.getCode());
        jsonResponse.setErrorMsg(RESTCodes.GenericErrorCode.ENDPOINT_ANNOTATION_MISSING.getMessage());
        //Should throw exception if there is a method that is not annotated in this path.
        requestContext.abortWith(Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(jsonResponse).build());
        return;
      }
      AllowedProjectRoles rolesAnnotation = method.getAnnotation(AllowedProjectRoles.class);
      Set<String> rolesSet;
      rolesSet = new HashSet<>(Arrays.asList(rolesAnnotation.value()));
      
      //If the resource is allowed for all roles continue with the request. 
      if (rolesSet.contains(AllowedProjectRoles.ANYONE)) {
        LOGGER.log(Level.FINEST, "Accessing resource that is allowed for all");
        return;
      }
      
      if (requestContext.getSecurityContext().getUserPrincipal() == null) {
        jsonResponse.setErrorCode(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL.getCode());
        jsonResponse.setErrorMsg(RESTCodes.SecurityErrorCode.EJB_ACCESS_LOCAL.getMessage());
        requestContext.abortWith(Response.
          status(Response.Status.UNAUTHORIZED).entity(jsonResponse).build());
        return;
      }
      
      //if the resource is only allowed for some roles check if the user have the requierd role for the resource.
      String userEmail = requestContext.getSecurityContext().getUserPrincipal().
        getName();
      
      userRole = projectTeamBean.findCurrentRole(project, userEmail);
      
      if (userRole == null || userRole.isEmpty()) {
        LOGGER.log(Level.INFO,
          "Trying to access resource, but you dont have any role in this project");
        jsonResponse.setErrorCode(RESTCodes.UserErrorCode.NO_ROLE_FOUND.getCode());
        jsonResponse.setErrorMsg(RESTCodes.UserErrorCode.NO_ROLE_FOUND.getMessage());
        requestContext.abortWith(Response
          .status(Response.Status.FORBIDDEN)
          .entity(jsonResponse)
          .build());
      } else if (!rolesSet.contains(userRole)) {
        LOGGER.log(Level.INFO, "Trying to access resource that is only allowed for: {0}, But you are a: {1}",
          new Object[]{rolesSet, userRole});
        jsonResponse.setErrorCode(RESTCodes.ProjectErrorCode.PROJECT_ROLE_FORBIDDEN.getCode());
        jsonResponse.setErrorMsg(RESTCodes.ProjectErrorCode.PROJECT_ROLE_FORBIDDEN.getMessage());
        requestContext.abortWith(Response
          .status(Response.Status.FORBIDDEN)
          .entity(jsonResponse)
          .build());
      }
    }
  }
}
