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

import com.predic8.membrane.core.http.Header;
import io.hops.hopsworks.api.kibana.MyRequestWrapper;
import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.jobs.flink.FlinkCompletedJobsCache;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.util.EntityUtils;
import org.apache.parquet.Strings;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

// https://ci.apache.org/projects/flink/flink-docs-stable/monitoring/historyserver.html
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FlinkHistoryServerProxyServlet extends ProxyServlet {
  
  private final static Logger LOGGER = Logger.getLogger(FlinkHistoryServerProxyServlet.class.getName());
  
  @EJB
  private FlinkCompletedJobsCache cache;
  @EJB
  private UserFacade userFacade;
  
  @Override
  protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
    throws ServletException, IOException {
    if (servletRequest.getUserPrincipal() == null) {
      servletResponse.sendError(401, "User is not logged in");
      return;
    }
    
    String email = servletRequest.getUserPrincipal().getName();
    
    String[] parts = servletRequest.getPathInfo().split("/");
    //Handle /jobs/<jobid>
    if (parts.length == 3
      && servletRequest.getPathInfo().startsWith("/jobs")
      && !servletRequest.getPathInfo().startsWith("/jobs/overview")) {
    
      String job = servletRequest.getPathInfo().split("/")[2];
      if (!Strings.isNullOrEmpty(job) && !cache.hasAccessToFlinkJob(job, email)) {
        servletResponse.sendError(403, "You are not authorized to access this Flink job");
      }
    }
    //Remove all jobs from JSON the user is not authorized to view
    
    MyRequestWrapper myRequestWrapper = new MyRequestWrapper((HttpServletRequest) servletRequest);
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
      LOGGER.log(Level.FINE, "proxy " + method + " uri: " + servletRequest.getRequestURI()
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
      
      
      // Send the content to the client
      if (!servletRequest.getPathInfo().startsWith("/jobs/overview")) {
        super.copyResponseEntity(proxyResponse, servletResponse);
      } else {
        copyResponseEntity(proxyResponse, servletResponse, email);
      }
      copyResponseHeaders(proxyResponse, servletRequest, servletResponse);
  
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
  
  protected void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse servletResponse,
    String email)
    throws IOException {
    
    HttpEntity entity = proxyResponse.getEntity();
    if (entity != null) {
      String resp = EntityUtils.toString(entity);
      BasicHttpEntity basic = new BasicHttpEntity();
      //Remove all jobs the user is not authorized to view
      JSONObject respJson = new JSONObject(resp);
      if(respJson.has("jobs")) {
        JSONArray jobs = respJson.getJSONArray("jobs");
        Users user = userFacade.findByEmail(email);
        for (int i = jobs.length() - 1; i >= 0; i--) {
          //Find if user is member of this project
          //Get project of job from cache
          if (!cache.hasAccessToFlinkJob(jobs.getJSONObject(i).getString("jid"), user)) {
            jobs.remove(i);
          }
        }
      }
      InputStream in = IOUtils.toInputStream(respJson.toString());
      
      OutputStream servletOutputStream = servletResponse.getOutputStream();
      basic.setContent(in);
      basic.writeTo(servletOutputStream);
      //Set Content-Length since we have might have changed the response length
      proxyResponse.setHeader(Header.CONTENT_LENGTH, String.valueOf(respJson.toString().length()));
    }
    
  }
  
}
