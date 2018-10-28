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
package io.hops.hopsworks.api.airflow;

import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;

public class AirflowProxyServlet extends ProxyServlet {

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
    String email = servletRequest.getUserPrincipal().getName();

    if (servletRequest.getUserPrincipal() == null || (!servletRequest.isUserInRole("HOPS_ADMIN"))) {
      servletResponse.sendError(403, "User needs to made an admin (HOPS_ADMIN role)");
      return;
    }

    Users user = userFacade.findByEmail(email);

    super.service(servletRequest, servletResponse);

  }

  protected void copyResponseEntity(HttpMethod method,
      HttpServletResponse servletResponse) throws IOException {
    InputStream entity = method.getResponseBodyAsStream();
    if (entity != null) {
      OutputStream servletOutputStream = servletResponse.getOutputStream();
      if (servletResponse.getHeader("Content-Type") == null || servletResponse.getHeader("Content-Type").
          contains("html")) {
        String inputLine;
        BufferedReader br = new BufferedReader(new InputStreamReader(entity));

        try {
          int contentSize = 0;
          String source = "http://" + method.getURI().getHost() + ":" + method.getURI().getPort();
          while ((inputLine = br.readLine()) != null) {
            String outputLine = hopify(inputLine, source) + "\n";
            byte[] output = outputLine.getBytes(Charset.forName("UTF-8"));
            servletOutputStream.write(output);
            contentSize += output.length;
          }
          br.close();
          servletResponse.setHeader("Content-Length", Integer.toString(contentSize));
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        org.apache.hadoop.io.IOUtils.copyBytes(entity, servletOutputStream, 4096, doLog);
      }
    }
  }

  protected void copyResponseHeaders(HttpMethod method,
      HttpServletRequest servletRequest,
      HttpServletResponse servletResponse) {
    for (org.apache.commons.httpclient.Header header : method.getResponseHeaders()) {
      if (hopByHopHeaders.containsHeader(header.getName())) {
        continue;
      }
      if (header.getName().equalsIgnoreCase("Content-Length") && (method.getResponseHeader("Content-Type") == null
          || method.getResponseHeader("Content-Type").getValue().contains("html"))) {
        continue;
      }
      if (header.getName().
          equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE) || header.
          getName().equalsIgnoreCase(org.apache.http.cookie.SM.SET_COOKIE2)) {
        copyProxyCookie(servletRequest, servletResponse, header.getValue());
      } else {
        servletResponse.addHeader(header.getName(), header.getValue());
      }
    }
  }

  private String hopify(String ui, String source) {

    ui = ui.replaceAll("(?<=(href|src)=\")/(?=[a-zA-Z])",
        "/hopsworks-api/yarnui/" + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=\')/(?=[a-zA-Z])",
        "/hopsworks-api/yarnui/" + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=\")//", "/hopsworks-api/yarnui/");
    ui = ui.replaceAll("(?<=(href|src)=\')//", "/hopsworks-api/yarnui/");
    ui = ui.replaceAll("(?<=(href|src)=\")(?=http)",
        "/hopsworks-api/yarnui/");
    ui = ui.replaceAll("(?<=(href|src)=\')(?=http)",
        "/hopsworks-api/yarnui/");
    ui = ui.replaceAll("(?<=(href|src)=\")(?=[a-zA-Z])",
        "/hopsworks-api/yarnui/" + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=\')(?=[a-zA-Z])",
        "/hopsworks-api/yarnui/" + source + "/");
    ui = ui.replaceAll("(?<=(url: '))/(?=[a-zA-Z])", "/hopsworks-api/yarnui/");
    ui = ui.replaceAll("(?<=(location\\.href = '))/(?=[a-zA-Z])", "/hopsworks-api/yarnui/");
    return ui;

  }

  protected String rewriteUrlFromRequest(HttpServletRequest servletRequest) {
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

}
