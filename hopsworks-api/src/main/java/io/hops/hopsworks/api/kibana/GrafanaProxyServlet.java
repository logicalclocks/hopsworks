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

package io.hops.hopsworks.api.kibana;

import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

@Stateless
public class GrafanaProxyServlet extends ProxyServlet {

  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private ProjectController projectController;

  @Override
  protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) 
      throws ServletException, IOException {
    if (servletRequest.getUserPrincipal() == null || 
       (!servletRequest.isUserInRole("HOPS_ADMIN") && !servletRequest.isUserInRole("HOPS_USER"))) {
      servletResponse.sendError(403, "User is not logged in");
      return;
    }
    if (servletRequest.getRequestURI().contains("query")) {
      String email = servletRequest.getUserPrincipal().getName();
      Pattern pattern = Pattern.compile("(application_.*?_.\\d*)");
      Users user = userFacade.findByEmail(email);
      Matcher matcher = pattern.matcher(servletRequest.getQueryString());
      if (matcher.find()) {
        String appId = matcher.group(1);
        YarnApplicationstate appState = yarnApplicationstateFacade.findByAppId(
                appId);
        if (appState == null) {
          servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
                  "You don't have the access right for this application");
          return;
        }
        String projectName = hdfsUsersBean.getProjectName(appState.getAppuser());
        ProjectDTO project;
        try {
          project = projectController.getProjectByName(projectName);
        } catch (AppException ex) {
          throw new ServletException(ex);
        }
        
        
        boolean inTeam = false;
        for(ProjectTeam pt: project.getProjectTeam()){
          if(pt.getUser().equals(user)){
            inTeam = true;
            break;
          }
        }
        if(!inTeam){
          servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
                  "You don't have the access right for this application");
          return;
        }
      } else {
        boolean userRole = servletRequest.isUserInRole("HOPS_ADMIN");
        if (!userRole) {
          servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
              "You don't have the access right for this application");
          return;
        }
      }
    }
    super.service(servletRequest, servletResponse);

  }
}
