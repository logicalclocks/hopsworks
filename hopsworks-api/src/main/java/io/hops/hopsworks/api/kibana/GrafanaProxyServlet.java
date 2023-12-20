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

package io.hops.hopsworks.api.kibana;

import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.proxy.ModifyHeaderHttpServletRequest;
import io.hops.hopsworks.api.proxy.MultiReadHttpServletRequest;
import io.hops.hopsworks.api.proxy.ProxyServlet;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.jobs.history.YarnApplicationstate;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import joptsimple.internal.Strings;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.utils.URIUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class GrafanaProxyServlet extends ProxyServlet {
  private static final Logger LOGGER = Logger.getLogger(GrafanaProxyServlet.class.getName());
  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private UsersController userController;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  
  private final String YARN_APP_PATTERN_KEY = "yarnApp";
  private final String FG_KAFKA_TOPIC_KEY = "fgKafkaTopic";
  private final String DEPLOYMENT_METRICS_KEY = "deploymentMetrics";
  private final String USER_STATEMENT_SUMMARIES = "user_statement_summaries";
  
  private final Map<String, Pattern> patterns = new HashMap<String, Pattern>() {
    {
      put(YARN_APP_PATTERN_KEY, Pattern.compile("(application_.*?_.\\d*)"));
      put(FG_KAFKA_TOPIC_KEY, Pattern.compile(
        "(?<projectid>[0-9]+)_(?<fgid>[0-9]+)_(?<fgname>[a-z0-9_]+)_(?<fgversion>[0-9]+)_onlinefs"));
      put(DEPLOYMENT_METRICS_KEY,
          Pattern.compile("(namespace_name|exported_namespace|container_label_io_kubernetes_pod_namespace)" +
              "=\\\\\"(?<projectname>[0-9a-z-]+)\\\\\""));
      put(USER_STATEMENT_SUMMARIES, Pattern.compile("user=\\\\\"(?<dbuser>[0-9a-z_]+)\\\\\""));
    }
  };
  
  private final List<String> openQueries = Collections.singletonList("onlinefs_clusterj_success_write_counter_total");
  
  @Override
  protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
    throws ServletException, IOException {
    Users user = jwtHelper.validateAndRenewToken(servletRequest, servletResponse,
      new HashSet<>(Arrays.asList("HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER")));
    if (user == null) {
      return;
    }
    
    boolean isAdmin = userController.isUserInRole(user, "HOPS_ADMIN");
    ModifyHeaderHttpServletRequest addParamsToHeader = new ModifyHeaderHttpServletRequest(servletRequest);
    addParamsToHeader.addHeader("X-WEBAUTH-USER", user.getUsername());
    addParamsToHeader.addHeader("X-WEBAUTH-NAME", user.getFname());
    addParamsToHeader.addHeader("X-WEBAUTH-EMAIL", user.getEmail());
    addParamsToHeader.addHeader("X-WEBAUTH-ROLE", isAdmin ? "Admin" : "Viewer");
    
    //check /query iff the user is not an admin
    if (!isAdmin && "POST".equalsIgnoreCase(addParamsToHeader.getMethod()) &&
      addParamsToHeader.getRequestURI().contains("query")) {
      boolean isAuthorized = false;
      // wrap request for multi read
      MultiReadHttpServletRequest multiReadHttpServletRequest = new MultiReadHttpServletRequest(addParamsToHeader);
      // read body
      String bodyString = IOUtils.toString(multiReadHttpServletRequest.getInputStream(), StandardCharsets.UTF_8);
      try {
        for (String key : patterns.keySet()) {
          Matcher matcher = patterns.get(key).matcher(bodyString);
          if (matcher.find()) {
            if (Objects.equals(key, YARN_APP_PATTERN_KEY)) {
              String appId = matcher.group(1);
              validateUserForSparkAppId(user, appId);
              isAuthorized = true;
            } else if (Objects.equals(key, FG_KAFKA_TOPIC_KEY)) {
              validateUserForOnlineFG(user, matcher.group("projectid"));
              isAuthorized = true;
            } else if (Objects.equals(key, DEPLOYMENT_METRICS_KEY)) {
              String projectName = matcher.group("projectname").replaceAll("-", "_");
              validateProjectForUser(projectFacade.findByName(projectName), user);
              isAuthorized = true;
            } else if (Objects.equals(key, USER_STATEMENT_SUMMARIES)) {
              String projectName = getProjectNameFromDatabaseUsername(user, matcher.group("dbuser"));
              validateProjectForUser(projectFacade.findByName(projectName), user);
              isAuthorized = true;
            }
            break;
          }
        }
      } catch (ServiceException e) {
        servletResponse.sendError(Response.Status.FORBIDDEN.getStatusCode(), e.getMessage());
        return;
      }
      // if /query is not authorized, and it is not open
      if (!isAuthorized && !isQueryOpen(bodyString)) {
        servletResponse.sendError(Response.Status.FORBIDDEN.getStatusCode(), "Unauthorized to execute query "
          + bodyString);
        return;
      }
      super.service(multiReadHttpServletRequest, servletResponse);
      return;
    }
    super.service(addParamsToHeader, servletResponse);
  }
  
  public void validateUserForSparkAppId(Users user, String appId) throws ServiceException {
    YarnApplicationstate appState = yarnApplicationstateFacade.findByAppId(appId);
    if (appState == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GRAFANA_PROXY_ERROR, Level.SEVERE,
        "You don't have the access right for this application");
    }
    String projectName = hdfsUsersBean.getProjectName(appState.getAppuser());
    Project project = projectFacade.findByName(projectName);
    validateProjectForUser(project, user);
  }
  
  public void validateUserForOnlineFG(Users user, String projectId) throws ServiceException {
    try {
      Project project = projectFacade.findById(Integer.parseInt(projectId)).orElse(null);
      validateProjectForUser(project, user);
    } catch (NumberFormatException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GRAFANA_PROXY_ERROR, Level.SEVERE,
        "Invalid project id:  " + projectId);
    }
  }
  
  public void validateProjectForUser(Project project, Users user) throws ServiceException {
    if (project == null) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GRAFANA_PROXY_ERROR, Level.SEVERE, "Project does not " +
        "exists");
    } else if (!projectTeamFacade.isUserMemberOfProject(project, user)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GRAFANA_PROXY_ERROR, Level.SEVERE,
        "User not a member of project");
    }
  }
  
  public String getProjectNameFromDatabaseUsername(Users user, String dbUsername) throws ServiceException {
    String toReplace = "_" + user.getUsername();
    int start = dbUsername.lastIndexOf(toReplace);
    if (start < 0) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GRAFANA_PROXY_ERROR, Level.SEVERE,
        "User not found");
    }
    return dbUsername.substring(0, start) +
      dbUsername.substring(start + toReplace.length());
  }
  
  /**
   * Checks query is open
   *
   * @param query
   * @return
   */
  public boolean isQueryOpen(String query) {
    return !Strings.isNullOrEmpty(query) && openQueries.stream().anyMatch(q -> query.contains(q));
  }
}
