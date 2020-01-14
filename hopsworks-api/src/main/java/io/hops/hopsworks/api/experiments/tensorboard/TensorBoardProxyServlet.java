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

package io.hops.hopsworks.api.experiments.tensorboard;

import com.google.common.base.Strings;
import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoard;
import io.hops.hopsworks.common.dao.tensorflow.TensorBoardFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.http.client.utils.URIUtils;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TensorBoardProxyServlet extends ProxyServlet {

  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private ProjectController projectController;
  @EJB
  private TensorBoardFacade tensorBoardFacade;
 
  private final static Logger LOGGER = Logger.getLogger(TensorBoardProxyServlet.class.getName());

  // A request will come in with the format: 
  // http://127.0.0.1:8080/hopsworks-api/tensorboard/application_1507065031551_0005/hopsworks0:59460/#graphs
  // 
  @Override
  protected void service(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    String email = servletRequest.getUserPrincipal().getName();

    if (Strings.isNullOrEmpty(email)) {
      servletResponse.sendError(Response.Status.FORBIDDEN.getStatusCode(),
          "You don't have access to this TensorBoard");
      return;
    }
    LOGGER.log(Level.FINE, "Request URL: {0}", servletRequest.getRequestURL());

    String uri = servletRequest.getRequestURI();
    // valid hostname regex:
    // https://stackoverflow.com/questions/106179/regular-expression-to-match-dns-hostname-or-ip-address
    Pattern urlPattern = Pattern.compile("([a-zA-Z0-9\\-\\.]{2,255}:[0-9]{4,6})(/.*$)");
    Matcher urlMatcher = urlPattern.matcher(uri);
    String hostPortPair = "";
    String uriToFinish = "/";
    if (urlMatcher.find()) {
      hostPortPair = urlMatcher.group(1);
      uriToFinish = urlMatcher.group(2);
    }
    if (hostPortPair.isEmpty()) {
      servletResponse.sendError(Response.Status.FORBIDDEN.getStatusCode(),
          "This TensorBoard is not accessible right now");
      return;
    }

    Pattern appPattern = Pattern.compile("(application_.*?_\\d*)");
    Matcher appMatcher = appPattern.matcher(servletRequest.getRequestURI());

    Pattern elasticPattern = Pattern.compile("(experiments)");
    Matcher elasticMatcher = elasticPattern.matcher(servletRequest.getRequestURI());
    if (elasticMatcher.find()) {

      List<TensorBoard> TBList = tensorBoardFacade.findByUserEmail(email);
      if(TBList == null) {
        servletResponse.sendError(Response.Status.FORBIDDEN.getStatusCode(),
            "This TensorBoard is not running right now");
        return;
      }
      boolean foundTB = false;
      for(TensorBoard tb: TBList) {
        if(tb.getEndpoint().equals(hostPortPair)) {
          foundTB = true;
          break;
        }
      }

      if(!foundTB) {
        servletResponse.sendError(Response.Status.FORBIDDEN.getStatusCode(),
                "This TensorBoard is not running right now");
        return;
      }

      targetUri = uriToFinish;

      String theHost = "http://" + hostPortPair;
      URI targetUriHost;
      try {
        targetUriObj = new URI(targetUri);
        targetUriHost = new URI(theHost);
      } catch (Exception e) {
        LOGGER.log(Level.FINE, "An error occurred serving the request", e);
        return;
      }
      targetHost = URIUtils.extractHost(targetUriHost);
      servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
      servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);
      servletRequest.setAttribute(ATTR_URI_FINISH, uriToFinish);
      servletRequest.setAttribute(ATTR_HOST_PORT, hostPortPair );

      try {
        super.service(servletRequest, servletResponse);
      } catch (IOException ex) {
        servletResponse.sendError(Response.Status.NOT_FOUND.getStatusCode(),
            "This TensorBoard is not ready to serve requests right now, " +
                "try refreshing the page");
        return;
      }


    } else if(appMatcher.find()) {
      String appId = appMatcher.group(1);
      YarnApplicationstate appState = yarnApplicationstateFacade.findByAppId(appId);
      if (appState == null) {
        servletResponse.sendError(Response.Status.FORBIDDEN.getStatusCode(),
                "You don't have the access right for this application");
        return;
      }
      String projectName = hdfsUsersBean.getProjectName(appState.getAppuser());
      ProjectDTO project;
      try {
        project = projectController.getProjectByName(projectName);
      } catch (ProjectException ex) {
        throw new ServletException(ex);
      }

      Users user = userFacade.findByEmail(email);

      boolean inTeam = false;
      for (ProjectTeam pt : project.getProjectTeam()) {
        if (pt.getUser().equals(user)) {
          inTeam = true;
          break;
        }
      }
      if (!inTeam) {
        servletResponse.sendError(Response.Status.FORBIDDEN.getStatusCode(),
                "You don't have the access right for this application");
        return;
      }
      if (appState.getAppsmstate() != null && (appState.getAppsmstate().equalsIgnoreCase(YarnApplicationState.FINISHED.
              toString()) || appState.getAppsmstate().equalsIgnoreCase(YarnApplicationState.KILLED.toString()))) {
        servletResponse.sendError(Response.Status.NOT_FOUND.getStatusCode(),
            "This TensorBoard has finished running.");
        return;
      }
      targetUri = uriToFinish;

      String theHost = "http://" + hostPortPair;
      URI targetUriHost;
      try {
        targetUriObj = new URI(targetUri);
        targetUriHost = new URI(theHost);
      } catch (Exception e) {
        servletResponse.sendError(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
            "An error occurred serving the request.");
        LOGGER.log(Level.FINE, "An error occurred serving the request", e);
        return;
      }
      targetHost = URIUtils.extractHost(targetUriHost);
      servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
      servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);
      servletRequest.setAttribute(ATTR_URI_FINISH, uriToFinish);
      servletRequest.setAttribute(ATTR_HOST_PORT, hostPortPair );

      try {
        super.service(servletRequest, servletResponse);
      } catch (IOException ex) {
        servletResponse.sendError(Response.Status.NOT_FOUND.getStatusCode(),
            "This TensorBoard is not running right now.");
        return;
      }

    } else {
      servletResponse.sendError(Response.Status.FORBIDDEN.getStatusCode(),
          "You don't have the access right for this application");
      return;
    }

  }

  /**
   * Reads the request URI from {@code servletRequest} and rewrites it,
   * considering targetUri.
   * It's used to make the new request.
   */
  @Override
  protected String rewriteUrlFromRequest(HttpServletRequest servletRequest) {
    StringBuilder uri = new StringBuilder(500);
    String theUri = getTargetUri(servletRequest);
    uri.append(theUri);
    // Handle the path given to the servlet
    if (servletRequest.getPathInfo() != null) {//ex: /my/path.html
      String pathInfo = servletRequest.getPathInfo();
      pathInfo = pathInfo.substring(1);
      String targetUrl = ((String) servletRequest.getAttribute(ATTR_HOST_PORT)) +
          ((String) servletRequest.getAttribute(ATTR_URI_FINISH));
      if (pathInfo.contains(targetUrl)) {
        pathInfo = pathInfo.substring(pathInfo.indexOf(targetUrl) + targetUrl.length());
      } else {
        pathInfo = "";
      }
      uri.append(encodeUriQuery(pathInfo));
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

  public static String getHTML(String urlToRead) throws Exception {
    StringBuilder result = new StringBuilder();
    URL url = new URL(urlToRead);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    String line;
    while ((line = rd.readLine()) != null) {
      result.append(line);
    }
    rd.close();
    conn.disconnect();
    return result.toString();
  }

}
