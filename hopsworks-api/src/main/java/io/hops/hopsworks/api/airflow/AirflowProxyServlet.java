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
package io.hops.hopsworks.api.airflow;

import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.Settings;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

public class AirflowProxyServlet extends ProxyServlet {
  
  private static final Pattern TRIGGER_DAG_PATTERN = Pattern.compile(".+/trigger/?$");
  
  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private ProjectFacade projectFacade;

  private AtomicInteger barrier = new AtomicInteger(1);

  private final static Logger LOGGER = Logger.getLogger(AirflowProxyServlet.class.getName());

  private HashMap<String, String> currentProjects = new HashMap<>();

  @EJB
  private Settings settings;

  private static final HashSet<String> PASS_THROUGH_HEADERS
      = new HashSet<String>(
          Arrays
              .asList("User-Agent", "user-agent", "Accept", "accept",
                  "Accept-Encoding", "accept-encoding",
                  "Accept-Language",
                  "accept-language",
                  "Accept-Charset", "accept-charset"));

  protected void initTarget() throws ServletException {
    super.initTarget();
  }

  /**
   * A request will come in with the format:
   * http://127.0.0.1:8080/hopsworks-api/airflow/
   * and be sent to:
   * http://localhost:12358/admin/airflow/login?next=%2Fadmin%2F
   * <p>
   * <p>
   * A 2-step process is needed. First you load the
   * http://localhost:12358/admin
   * page to get the csrf token.
   * http://localhost:12358/admin/airflow/login?username=admin&password=admin&
   * _csrf_token=.eJw9zEELgjAUAOC_Eu_coQleBA_BQDq8B4tt8rwIaaVbK6jAOfG_5ym--7dAO_ZQLLC7QAGN9oJ
   * 1d0B3j5RUToncZsDM5CT7B1UcSVtP6VjCuofu876135e_Pv8FShVRngRWPFPAmZ0duVai0UpwjQldM2AyEwUb2BlB2Tn
   * gVG7d-gNbGi3A.DoTYbQ.Cls2s0eODEuWxAWTogoTuWoOyM0
   * <p>
   * <p>
   * Reference:
   * https://flask-wtf.readthedocs.io/en/latest/csrf.html
   * <p>
   * <p>
   * <script type="text/javascript">
   * var csrf_token = "{{ csrf_token() }}";
   * <p>
   * $.ajaxSetup({
   * beforeSend: function(xhr, settings) {
   * if (!/^(GET|HEAD|OPTIONS|TRACE)$/i.test(settings.type) && !this.crossDomain) {
   * xhr.setRequestHeader("X-CSRFToken", csrf_token);
   * }
   * }
   * });
   * </script>
   * <p>
   * <p>
   *
   */
  @Override
  protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
      throws ServletException, IOException {

    if (servletRequest.getUserPrincipal() == null) {
      servletResponse.sendError(403, "User is not logged in");
      return;
    }

    super.service(servletRequest, servletResponse);

  }
  
  protected String rewriteUrlFromRequest(HttpServletRequest servletRequest) {
    Matcher triggerDAGMatcher = TRIGGER_DAG_PATTERN.matcher(servletRequest.getRequestURI());
    String queryString = servletRequest.getQueryString();
    if (triggerDAGMatcher.matches()) {
      List<NameValuePair> queries = URLEncodedUtils.parse(servletRequest.getQueryString(), Charset.defaultCharset());
      // If there are no query params, queries list is an immutable collection
      // Normally there should be the dag id to trigger, but just to be safe
      if (queries.isEmpty()) {
        queries = new ArrayList<>(1);
      }
      NameValuePair origin = new BasicNameValuePair("origin", "admin/");
      queries.add(origin);
      queryString = URLEncodedUtils.format(queries, Charset.defaultCharset());
    }
    
    StringBuilder uri = new StringBuilder(500);
    if (servletRequest.getPathInfo() != null && servletRequest.getPathInfo().matches(
        "/http([a-zA-Z,:,/,.,0-9,-])+:([0-9])+(.)+")) {
      String target = "http://" + servletRequest.getPathInfo().substring(7);
      servletRequest.setAttribute(ATTR_TARGET_URI, target);
      uri.append(target);
    } else {
      uri.append(getTargetUri(servletRequest));
      // Handle the path given to the servlet
      if (servletRequest.getPathInfo() != null) {//ex: /my/path.html
        uri.append(encodeUriQuery(servletRequest.getPathInfo()));
      }
    }
    // Handle the query string & fragment
    //ex:(following '?'): name=value&foo=bar#fragment
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

  @Override
  protected void copyRequestHeaders(HttpServletRequest servletRequest,
      HttpRequest proxyRequest) {
    // Get an Enumeration of all of the header names sent by the client
    Enumeration enumerationOfHeaderNames = servletRequest.getHeaderNames();
    while (enumerationOfHeaderNames.hasMoreElements()) {
      String headerName = (String) enumerationOfHeaderNames.nextElement();
      //Instead the content-length is effectively set via InputStreamEntity
      if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) {
        continue;
      }
      if (hopByHopHeaders.containsHeader(headerName)) {
        continue;
      }
      if (headerName.equalsIgnoreCase("accept-encoding") && (servletRequest.getPathInfo() == null || !servletRequest.
          getPathInfo().contains(".js"))) {
        continue;
      }
      Enumeration headers = servletRequest.getHeaders(headerName);
      while (headers.hasMoreElements()) {//sometimes more than one value
        String headerValue = (String) headers.nextElement();
        // In case the proxy host is running multiple virtual servers,
        // rewrite the Host header to ensure that we get content from
        // the correct virtual server
        if (headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
          HttpHost host = getTargetHost(servletRequest);
          headerValue = host.getHostName();
          if (host.getPort() != -1) {
            headerValue += ":" + host.getPort();
          }
        } else if (headerName.equalsIgnoreCase(org.apache.http.cookie.SM.COOKIE)) {
          headerValue = getRealCookie(headerValue);
        }
        proxyRequest.addHeader(headerName, headerValue);
      }
    }
  }

  @Override
  protected boolean doResponseRedirectOrNotModifiedLogic(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse, HttpResponse proxyResponse, int statusCode)
    throws ServletException, IOException {
    // Basically the same as in ProxySevlet, additionally copy response headers
    
    // Check if the proxy response is a redirect
    // The following code is adapted from org.tigris.noodle.filters.CheckForRedirect
    if (statusCode >= HttpServletResponse.SC_MULTIPLE_CHOICES /*
         * 300
         */
        && statusCode < HttpServletResponse.SC_NOT_MODIFIED /*
         * 304
         */) {
      Header locationHeader = proxyResponse.getLastHeader(HttpHeaders.LOCATION);
      if (locationHeader == null) {
        throw new ServletException("Received status code: " + statusCode
            + " but no " + HttpHeaders.LOCATION
            + " header was found in the response");
      }
      // Modify the redirect to go to this proxy servlet rather that the proxied host
      String locStr = rewriteUrlFromResponse(servletRequest, locationHeader.
          getValue());
    
      copyResponseHeaders(proxyResponse, servletRequest, servletResponse);
      servletResponse.sendRedirect(locStr);
      return true;
    }
    // 304 needs special handling.  See:
    // http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
    // We get a 304 whenever passed an 'If-Modified-Since'
    // header and the data on disk has not changed; server
    // responds w/ a 304 saying I'm not going to send the
    // body because the file has not changed.
    if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
      servletResponse.setIntHeader(HttpHeaders.CONTENT_LENGTH, 0);
      servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return true;
    }
    return false;
  }
}
