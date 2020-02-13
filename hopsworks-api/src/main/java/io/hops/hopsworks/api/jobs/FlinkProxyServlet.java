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
package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.jobs.flink.FlinkMasterAddrCache;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsUsers;
import io.hops.hopsworks.persistence.entity.jobs.history.YarnApplicationstate;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.http.client.utils.URIUtils;
import org.apache.parquet.Strings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class FlinkProxyServlet extends ProxyServlet {
  
  @EJB
  private FlinkMasterAddrCache flinkMasterAddrCache;
  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
  
  // A request will come in with the format:
  // hopsworks-api/flink/<yarnappid>
  private final static Logger LOGGER = Logger.getLogger(FlinkProxyServlet.class.getName());
  
  @Override
  protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
    throws ServletException, IOException {
    if (servletRequest.getUserPrincipal() == null) {
      servletResponse.sendError(401, "User is not logged in");
      return;
    }
    String uri = servletRequest.getRequestURI();
    Pattern appPattern = Pattern.compile("(application_.*?_\\d*)");
    Matcher appMatcher = appPattern.matcher(uri);
    String appId;
    String flinkMasterURL;
    if (appMatcher.find()) {
      appId = appMatcher.group(1);
      
      // Validate user is authorized to access to this yarn app
      YarnApplicationstate appState = yarnApplicationstateFacade.findByAppId(appId);
      // If job is not running, show relevant message
      if (!Strings.isNullOrEmpty(appState.getAppsmstate()) && (
        YarnApplicationState.valueOf(appState.getAppsmstate()) == YarnApplicationState.FAILED
          || YarnApplicationState.valueOf(appState.getAppsmstate()) == YarnApplicationState.FINISHED
          || YarnApplicationState.valueOf(appState.getAppsmstate()) == YarnApplicationState.KILLED)) {
        servletResponse.sendError(404,
          "This Flink cluster is not running. You can navigate to YARN and Logs for historical information on this " +
            "Flink cluster.");
        return;
      }
      HdfsUsers hdfsUser = hdfsUsersFacade.findByName(appState.getAppuser());
      Users user = userFacade.findByEmail(servletRequest.getUserPrincipal().getName());
      if (!projectTeamFacade.isUserMemberOfProject(projectFacade.findByName(hdfsUser.getProject()), user)) {
        servletResponse.sendError(403, "You are not authorized to access this Flink cluster");
      }
      
      //Is this user member of the project?
      flinkMasterURL = flinkMasterAddrCache.get(appId);
      if (Strings.isNullOrEmpty(flinkMasterURL)) {
        servletResponse.sendError(404,
          "This Flink cluster is not running. You can navigate to YARN and Logs for historical information on this" +
            " Flink cluster.");
        return;
      }
      String theHost = "http://" + flinkMasterURL;
      URI targetUriHost;
      targetUri = theHost;
      try {
        targetUriObj = new URI(targetUri);
        targetUriHost = new URI(theHost);
      } catch (Exception e) {
        LOGGER.log(Level.INFO, "An error occurred serving the request", e);
        return;
      }
      targetHost = URIUtils.extractHost(targetUriHost);
      servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
      servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);
      servletRequest.setAttribute(ATTR_HOST_PORT, flinkMasterURL);
      super.service(servletRequest, servletResponse);
      
    } else {
      servletResponse.sendError(404,
        "This Flink cluster is not running. You can navigate to YARN and Logs for historical information on this " +
          "Flink cluster.");
    }
    
  }
  

  @Override
  protected String rewriteUrlFromRequest(HttpServletRequest servletRequest) {
    StringBuilder uri = new StringBuilder(500);
    String targetUri = getTargetUri(servletRequest);
    uri.append(targetUri);
    
    // Handle the path given to the servlet
    if (servletRequest.getPathInfo() != null) {//ex: /my/path.html
      Pattern appPattern = Pattern.compile("(application_.*?_\\d*)");
      Matcher appMatcher = appPattern.matcher(servletRequest.getPathInfo().replaceFirst("/", ""));
      if (!appMatcher.find()) {
        uri.append(encodeUriQuery(servletRequest.getPathInfo()));
      } else {
        String appId = appMatcher.group(1);
        uri.append(encodeUriQuery(servletRequest.getPathInfo().replaceFirst("/", "").replace(appId, "")));
      }
    }
    
    // Handle the query string & fragment
    //ex:(following '?'): name=value&foo=bar#fragment
    String queryString = servletRequest.getQueryString();
    String fragment = null;
    //split off fragment from queryString, updating queryString if found
    if (queryString != null) {
      int fragIdx = queryString.indexOf('#');
      if (fragIdx >= 0) {
        fragment = queryString.substring(fragIdx + 2); // '#!', not '#'
        //        fragment = queryString.substring(fragIdx + 1);
        queryString = queryString.substring(0, fragIdx);
      }
    }
    
    queryString = rewriteQueryStringFromRequest(servletRequest, queryString);
    if (queryString != null && queryString.length() > 0) {
      uri.append('?');
      uri.append(encodeUriQuery(queryString));
    }
    
    if (doSendUrlFragment && fragment != null) {
      uri.append('#');
      uri.append(encodeUriQuery(fragment));
    }
    return uri.toString();
  }
}
