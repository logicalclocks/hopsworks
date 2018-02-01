/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.filter;

import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
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

  private final static Logger log = Logger.getLogger(RequestAuthFilter.class.
          getName());

  @Override
  public void filter(ContainerRequestContext requestContext) {

    String path = requestContext.getUriInfo().getPath();
    Method method = resourceInfo.getResourceMethod();
    String[] pathParts = path.split("/");
    log.log(Level.FINEST, "Rest call to {0}, from {1}.", new Object[]{path,
      requestContext.getSecurityContext().getUserPrincipal()});
    log.log(Level.FINEST, "Filtering request path: {0}", pathParts[0]);
    log.log(Level.FINEST, "Method called: {0}", method.getName());
    //intercepted method must be a project operations on a specific project
    //with an id (/project/projectId/... or /activity/projectId/...). 
    if (pathParts.length > 1 && (pathParts[0].equalsIgnoreCase("project")
            || pathParts[0].equalsIgnoreCase("activity")
            || pathParts[0].equalsIgnoreCase("notebook")
            || pathParts[0].equalsIgnoreCase("interpreter"))) {

      JsonResponse json = new JsonResponse();
      Integer projectId;
      String userRole;
      try {
        projectId = Integer.valueOf(pathParts[1]);
      } catch (NumberFormatException ne) {
        //if the second pathparam is not a project id return.
        log.log(Level.INFO,
                "Call to {0} has no project id, leaving interceptor.",
                path);
        return;
      }

      Project project = projectBean.find(projectId);
      if (project == null) {
        requestContext.abortWith(Response.
                status(Response.Status.NOT_FOUND).build());
        return;
      }
      log.log(Level.FINEST, "Filtering project request path: {0}", project.
              getName());

      if (!method.isAnnotationPresent(AllowedProjectRoles.class)) {
        //Should throw exception if there is a method that is not annotated in this path.
        requestContext.abortWith(Response.
                status(Response.Status.SERVICE_UNAVAILABLE).build());
        return;
      }
      AllowedProjectRoles rolesAnnotation = method.getAnnotation(AllowedProjectRoles.class);
      Set<String> rolesSet;
      rolesSet = new HashSet<>(Arrays.asList(rolesAnnotation.value()));

      //If the resource is allowed for all roles continue with the request. 
      if (rolesSet.contains(AllowedProjectRoles.ANYONE)) {
        log.log(Level.FINEST, "Accessing resource that is allowed for all");
        return;
      }

      if (requestContext.getSecurityContext().getUserPrincipal() == null) {
        requestContext.abortWith(Response.
                status(Response.Status.UNAUTHORIZED).build());
        return;
      }

      //if the resource is only allowed for some roles check if the user have the requierd role for the resource.
      String userEmail = requestContext.getSecurityContext().getUserPrincipal().
              getName();

      userRole = projectTeamBean.findCurrentRole(project, userEmail);

      if (userRole == null || userRole.isEmpty()) {
        log.log(Level.INFO,
                "Trying to access resource, but you dont have any role in this project");
        json.setStatusCode(Response.Status.FORBIDDEN.getStatusCode());
        json.setErrorMsg("You do not have access to this project.");
        requestContext.abortWith(Response
                .status(Response.Status.FORBIDDEN)
                .entity(json)
                .build());
      } else if (!rolesSet.contains(userRole)) {
        log.log(Level.INFO,
                "Trying to access resource that is only allowed for: {0}, But you are a: {1}",
                new Object[]{rolesSet, userRole});
        json.setStatusCode(Response.Status.FORBIDDEN.getStatusCode());
        json.setErrorMsg(
                "Your role in this project is not authorized to perform this action.");
        requestContext.abortWith(Response
                .status(Response.Status.FORBIDDEN)
                .entity(json)
                .build());
      }
    }
  }
}
