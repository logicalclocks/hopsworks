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
package io.hops.hopsworks.apiV2.filter;

import io.hops.hopsworks.apiV2.projects.ProjectsResource;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;

import javax.ejb.EJB;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

@Provider
public class ProjectAuthFilter extends ApiV2FilterBase {
  
  private final static Logger log = Logger.getLogger(ProjectAuthFilter.class.getName());
  
  @Context
  private ResourceInfo resourceInfo;
  
  @EJB
  private ProjectTeamFacade projectTeamBean;
  
  @EJB
  private ProjectFacade projectBean;
  
  @EJB
  private UserFacade userFacade;
  
  @Override
  protected void filterInternal(ContainerRequestContext requestContext) throws IOException {
    if (resourceInfo.getResourceClass() == ProjectsResource.class) {
      //Only filter projects-endpoint calls.
      String path = requestContext.getUriInfo().getPath();
      String[] pathParts = path.split("/");
    
      //Only filter requests that refer to a specific project
      if (pathParts.length > 2) {
        Integer projectId;
        try {
          projectId = Integer.valueOf(pathParts[2]);
        } catch (NumberFormatException ne) {
          //not project-specific, let it through
          return;
        }
  
        Method method = resourceInfo.getResourceMethod();
        if (!method.isAnnotationPresent(AllowedProjectRoles.class)) {
          //All project specific endpoints should have allowed-roles annotation
          log.severe("Method missing AllowedRoles-annotation: " + method.getName());
          requestContext.abortWith(Response.
              status(Response.Status.SERVICE_UNAVAILABLE).build());
          return;
        }
  
        AllowedProjectRoles rolesAnnotation = method.getAnnotation(AllowedProjectRoles.class);
        Set<String> rolesSet = new HashSet<>(Arrays.asList(rolesAnnotation.value()));
  
        if (rolesSet.contains(AllowedProjectRoles.ANYONE)) {
          //Any Hops-user is allowed, let the request through.
          return;
        }
  
        if (requestContext.getSecurityContext().getUserPrincipal() == null) {
          requestContext.abortWith(Response.
              status(Response.Status.UNAUTHORIZED).build());
          return;
        }
  
        Project project = projectBean.find(projectId);
        if (project == null) {
          requestContext.abortWith(Response.status(Response.Status.NOT_FOUND).build());
        }
      
        String userEmail = requestContext.getSecurityContext().getUserPrincipal().getName();
        Users user = userFacade.findByEmail(userEmail);
  
        String currentRole = projectTeamBean.findCurrentRole(project, user);
  
        if (currentRole == null || currentRole.isEmpty() || !rolesSet.contains(currentRole)) {
          requestContext.abortWith(Response
              .status(Response.Status.FORBIDDEN)
              .build());
        }
      }
    }
  }
  
}
