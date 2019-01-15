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

import com.google.api.client.repackaged.com.google.common.base.Strings;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Authorizes Hopsworks users to access particular elasticsearch indices
 * to be displayed by Kibana.
 * <p>
 */
@Stateless
public class KibanaProxyServlet extends ProxyServlet {

  @EJB
  private ProjectController projectController;
  @EJB
  private ElasticController elasticController;
  private static final  Logger LOG = Logger.getLogger(KibanaProxyServlet.class.getName());

  private final HashMap<String, String> currentProjects = new HashMap<>();

  private final List<String> registeredKibanaSuffix = new ArrayList<String>() {{
      add("_logs");
      add("_serving");
      add("_experiments");
      add("_experiments_summary-search");
      add("_experiments_summary-dashboard");
    }
  };

  /**
   * Authorize user to access particular index.
   *
   * @param servletRequest
   * @param servletResponse
   * @throws ServletException
   * @throws IOException
   */
  @Override
  protected void service(HttpServletRequest servletRequest,
                         HttpServletResponse servletResponse) throws ServletException, IOException {
    if (servletRequest.getUserPrincipal() == null) {
      servletResponse.sendError(401, "User is not logged in");
      return;
    }
    String email = servletRequest.getUserPrincipal().getName();

    if (servletRequest.getParameterMap().containsKey("projectId")) {
      String projectId = servletRequest.getParameterMap().get("projectId")[0];
      try {
        ProjectDTO projectDTO = projectController.getProjectByID(Integer.parseInt(projectId));
        currentProjects.put(email, projectDTO.getProjectName().toLowerCase());
      } catch (ProjectException ex) {
        LOG.log(Level.SEVERE, null, ex);
        servletResponse.sendError(403,
                "Kibana was not accessed from Hopsworks, no current project information is available.");
        return;
      }
    }

    //Do not authorize admin
    if (email.equals(Settings.AGENT_EMAIL)) {
      super.service(servletRequest, servletResponse);
      return;
    }

    MyRequestWrapper myRequestWrapper = new MyRequestWrapper((HttpServletRequest) servletRequest);
    KibanaFilter kibanaFilter = null;
    //Filter requests based on path
    if (servletRequest.getRequestURI().contains("api/saved_objects")) {
      kibanaFilter = KibanaFilter.KIBANA_SAVED_OBJECTS_API;
    } else if (servletRequest.getRequestURI().contains("elasticsearch/*/_search")) {
      kibanaFilter = KibanaFilter.ELASTICSEARCH_SEARCH;
    } else if (servletRequest.getRequestURI().contains("legacy_scroll_start") ||
        servletRequest.getRequestURI().contains("settings/defaultIndex")) {
      return;
    }

    //initialize request attributes from caches if unset by a subclass by this point
    if (servletRequest.getAttribute(ATTR_TARGET_URI) == null) {
      servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
    }
    if (servletRequest.getAttribute(ATTR_TARGET_HOST) == null) {
      servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);
    }

    // Make the Request
    //note: we won't transfer the protocol version because I'm not sure it would truly be compatible
    String method = servletRequest.getMethod();
    String proxyRequestUri = rewriteUrlFromRequest(servletRequest);
    HttpRequest proxyRequest;
    //spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
    if (servletRequest.getHeader(HttpHeaders.CONTENT_LENGTH) != null
            || servletRequest.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
      HttpEntityEnclosingRequest eProxyRequest
              = new BasicHttpEntityEnclosingRequest(method, proxyRequestUri);
      // Add the input entity (streamed)
      //  note: we don't bother ensuring we close the servletInputStream since the container handles it
      eProxyRequest.setEntity(new InputStreamEntity(myRequestWrapper.
              getInputStream(), servletRequest.getContentLength()));
      proxyRequest = eProxyRequest;
    } else {
      proxyRequest = new BasicHttpRequest(method, proxyRequestUri);
    }

    copyRequestHeaders(servletRequest, proxyRequest);

    super.setXForwardedForHeader(servletRequest, proxyRequest);

    HttpResponse proxyResponse = null;
    try {
      // Execute the request
      LOG.log(Level.FINE, "proxy " + method + " uri: " + servletRequest.getRequestURI()
          + " -- " + proxyRequest.getRequestLine().getUri());

      proxyResponse = super.proxyClient.execute(super.getTargetHost(
              myRequestWrapper), proxyRequest);

      // Process the response
      int statusCode = proxyResponse.getStatusLine().getStatusCode();

      if (doResponseRedirectOrNotModifiedLogic(myRequestWrapper, servletResponse,
              proxyResponse, statusCode)) {
        //the response is already "committed" now without any body to send
        //TODO copy response headers?
        return;
      }

      // Pass the response code. This method with the "reason phrase" is 
      // deprecated but it's the only way to pass the reason along too.
      //noinspection deprecation
      servletResponse.setStatus(statusCode, proxyResponse.getStatusLine().
              getReasonPhrase());

      copyResponseHeaders(proxyResponse, servletRequest, servletResponse);

      // Send the content to the client
      copyResponseEntity(proxyResponse, servletResponse, kibanaFilter, email);


    } catch (Exception e) {
      //abort request, according to best practice with HttpClient
      if (proxyRequest instanceof AbortableHttpRequest) {
        AbortableHttpRequest abortableHttpRequest
                = (AbortableHttpRequest) proxyRequest;
        abortableHttpRequest.abort();
      }
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      if (e instanceof ServletException) {
        throw (ServletException) e;
      }
      //noinspection ConstantConditions
      if (e instanceof IOException) {
        throw (IOException) e;
      }
      throw new RuntimeException(e);

    } finally {
      // make sure the entire entity was consumed, so the connection is released
      if (proxyResponse != null) {
        consumeQuietly(proxyResponse.getEntity());
      }
      //Note: Don't need to close servlet outputStream:
      // http://stackoverflow.com/questions/1159168/should-one-call-close-on-
      //httpservletresponse-getoutputstream-getwriter
    }
  }

  /**
   * Copy response body data (the entity) from the proxy to the servlet client.
   *
   * @param proxyResponse
   * @param servletResponse
   * @param kibanaFilter
   * @param email
   */
  protected void copyResponseEntity(HttpResponse proxyResponse,
      HttpServletResponse servletResponse, KibanaFilter kibanaFilter, String email) throws
      IOException {
    if (kibanaFilter == null) {
      super.copyResponseEntity(proxyResponse, servletResponse);
    } else {
      switch (kibanaFilter) {

        case LEGACY_SCROLL_START:
        case KIBANA_DEFAULT_INDEX:
          return;
        case KIBANA_SAVED_OBJECTS_API:
        case ELASTICSEARCH_SEARCH:
          HttpEntity entity = proxyResponse.getEntity();
          if (entity != null) {
            GzipDecompressingEntity gzipEntity = new GzipDecompressingEntity(entity);
            String resp = EntityUtils.toString(gzipEntity);
            BasicHttpEntity basic = new BasicHttpEntity();
            //Remove all projects other than the current one and check
            //if user is authorizer to access it
            JSONObject indices = new JSONObject(resp);
            LOG.log(Level.FINE, "indices:{0}", indices.toString());
            JSONArray hits = null;

            String projectName = currentProjects.get(email);
            List<String> projects = new ArrayList();
            //If we don't have the current project, filter out based on all user's projects
            if (Strings.isNullOrEmpty(projectName)) {
              List<String> projectNames = projectController.findProjectNamesByUser(email, true);
              if(projectNames!= null && !projectNames.isEmpty()){
                projects.addAll(projectNames);
              }
            } else {
              projects.add(projectName);
            }
            if (kibanaFilter == KibanaFilter.ELASTICSEARCH_SEARCH && HopsUtils.jsonKeyExists(indices, "buckets")) {
              hits = indices.getJSONObject("aggregations").getJSONObject("indices").getJSONArray("buckets");
            } else if (kibanaFilter == KibanaFilter.KIBANA_SAVED_OBJECTS_API && indices.has("saved_objects")) {
              hits = indices.getJSONArray("saved_objects");
            }
            if (hits != null) {
              LOG.log(Level.FINE, "hits:{0}", hits);
              for (int i = hits.length() - 1; i >= 0; i--) {
                String objectId = null;
                switch (kibanaFilter) {
                  case ELASTICSEARCH_SEARCH:
                    objectId = hits.getJSONObject(i).getString("key");
                    break;
                  case KIBANA_SAVED_OBJECTS_API:
                    objectId = elasticController.getIndexFromKibana(hits.getJSONObject(i));
                    break;
                  default:
                    break;
                }
                if (!Strings.isNullOrEmpty(objectId) && (!isAuthorizedKibanaObject(objectId, email, projects))) {
                  hits.remove(i);
                  LOG.log(Level.FINE, "removed objectId:{0}", objectId);
                }
              }
            }

            InputStream in = IOUtils.toInputStream(indices.toString());

            OutputStream servletOutputStream = servletResponse.getOutputStream();
            basic.setContent(in);
            GzipCompressingEntity compress = new GzipCompressingEntity(basic);
            compress.writeTo(servletOutputStream);

          }
          break;
        default:
          super.copyResponseEntity(proxyResponse, servletResponse);
          break;
      }

    }
  }

  private boolean isAuthorizedKibanaObject(String objectId, String email, List<String> projects) {
    for (String objectSuffix : registeredKibanaSuffix) {
      if (projects != null && !projects.isEmpty()) {
        for (String name : projects) {
          if (objectId.startsWith(name + objectSuffix) || objectId.equals(Settings.KIBANA_DEFAULT_INDEX)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
