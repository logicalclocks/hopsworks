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

package io.hops.hopsworks.api.admin.llap;

import io.hops.hopsworks.api.kibana.ProxyServlet;
import org.apache.http.client.utils.URIUtils;

import javax.ejb.Stateless;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class LlapMonitorProxyServlet extends ProxyServlet {

  protected static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{(.+?)\\}");
  private static final String ATTR_QUERY_STRING =
      LlapMonitorProxyServlet.class.getSimpleName() + ".queryString";

  protected String targetUriTemplate;//has {name} parts

  @Override
  protected void initTarget() throws ServletException {
    targetUriTemplate = getConfigParam(P_TARGET_URI);
    if (targetUriTemplate == null)
      throw new ServletException(P_TARGET_URI+" is required.");

    //leave this.target* null to prevent accidental mis-use
  }

  @Override
  protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
      throws ServletException, IOException {

    // Check if the user is logged in
    if (servletRequest.getUserPrincipal() == null) {
      servletResponse.sendError(403, "User is not logged in");
      return;
    }

    // Check that the user is an admin
    boolean isAdmin = servletRequest.isUserInRole("HOPS_ADMIN");
    if (!isAdmin) {
      servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
          "You don't have the access right for this application");
      return;
    }

    // The path we will receive is [host]/llapmonitor/llaphost/
    // We need to extract the llaphost to redirect the request
    String[] pathInfoSplits = servletRequest.getPathInfo().split("/");
    String llapHost = pathInfoSplits[1];

    //Now rewrite the URL
    StringBuffer urlBuf = new StringBuffer();//note: StringBuilder isn't supported by Matcher
    Matcher matcher = TEMPLATE_PATTERN.matcher(targetUriTemplate);
    if (matcher.find()) {
      matcher.appendReplacement(urlBuf, llapHost);
    }

    matcher.appendTail(urlBuf);
    String newTargetUri = urlBuf.toString();
    servletRequest.setAttribute(ATTR_TARGET_URI, newTargetUri);
    URI targetUriObj;
    try {
      targetUriObj = new URI(newTargetUri);
    } catch (Exception e) {
      throw new ServletException("Rewritten targetUri is invalid: " + newTargetUri,e);
    }
    servletRequest.setAttribute(ATTR_TARGET_HOST, URIUtils.extractHost(targetUriObj));

    super.service(servletRequest, servletResponse);
  }

  @Override
  protected String rewriteQueryStringFromRequest(HttpServletRequest servletRequest, String queryString) {
    return (String) servletRequest.getAttribute(ATTR_QUERY_STRING);
  }

    /**
   * Reads the request URI from {@code servletRequest} and rewrites it,
   * considering targetUri.
   * It's used to make the new request.
   *
   * The servlet name ends with llapmonitor/. This means that the llaphost will be
   * forwarded to with the request as part of the pathInfo.
   * We need to remove the llaphost from the info
   */
  @Override
  protected String rewriteUrlFromRequest(HttpServletRequest servletRequest) {
    StringBuilder uri = new StringBuilder(500);
    String targetUri = getTargetUri(servletRequest);
    uri.append(targetUri);
    // Handle the path given to the servlet


    String[] pathInfoSplits = servletRequest.getPathInfo().split("/");
    // Concatenate pathInfo without the llaphost
    StringBuilder newPathInfo = new StringBuilder();
    for (int i = 2; i<pathInfoSplits.length; i++){
      newPathInfo.append('/').append(pathInfoSplits[i]);
    }
    uri.append(encodeUriQuery(newPathInfo.toString()));

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
