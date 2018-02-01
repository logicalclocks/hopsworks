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

package io.hops.hopsworks.api.admin;

import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.api.util.CustomSSLProtocolSocketFactory;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.utils.URIUtils;

@Stateless
public class HDFSUIProxyServlet extends ProxyServlet {

  @EJB
  private Settings settings;
  @EJB
  private BaseHadoopClientsService baseHadoopClientsService;

  private static final HashSet<String> PASS_THROUGH_HEADERS
      = new HashSet<String>(
          Arrays
          .asList("User-Agent", "user-agent", "Accept", "accept",
              "Accept-Encoding", "accept-encoding",
              "Accept-Language",
              "accept-language",
              "Accept-Charset", "accept-charset"));

  protected void initTarget() throws ServletException {
    // TODO - should get the Kibana URI from Settings.java
//    targetUri = Settings.getKibanaUri();
    targetUri = settings.getHDFSWebUIAddress();
  
    if (targetUri == null) {
      throw new ServletException(P_TARGET_URI + " is required.");
    }
    
    if (settings.getHopsRpcTls()) {
      if (!targetUri.contains("https://")) {
        targetUri = "https://" + targetUri;
      }
    } else {
      if (!targetUri.contains("http://")) {
        targetUri = "http://" + targetUri;
      }
    }
    
    //test it's valid
    try {
      targetUriObj = new URI(targetUri);
    } catch (Exception e) {
      throw new ServletException("Trying to process targetUri init parameter: "
          + e, e);
    }
    targetHost = URIUtils.extractHost(targetUriObj);
  }

  @Override
  protected void service(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {

    if (servletRequest.getUserPrincipal() == null) {
      servletResponse.sendError(403, "User is not logged in");
      return;
    }
    if (!servletRequest.isUserInRole("HOPS_ADMIN")) {
      servletResponse.sendError(Response.Status.BAD_REQUEST.getStatusCode(),
          "You don't have the access right for this service");
      return;
    }
    if (servletRequest.getAttribute(ATTR_TARGET_URI) == null) {
      servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
    }
    if (servletRequest.getAttribute(ATTR_TARGET_HOST) == null) {
      servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);
    }

    // Make the Request
    // note: we won't transfer the protocol version because I'm not 
    // sure it would truly be compatible
    String proxyRequestUri = rewriteUrlFromRequest(servletRequest);

    try {
      String[] targetHost_port = settings.getHDFSWebUIAddress().split(":");
      File keyStore = new File(baseHadoopClientsService.getSuperKeystorePath());
      File trustStore = new File(baseHadoopClientsService.getSuperTrustStorePath());
      // Assume that KeyStore password and Key password are the same
      Protocol httpsProto = new Protocol("https", new CustomSSLProtocolSocketFactory(keyStore,
          baseHadoopClientsService.getSuperKeystorePassword(), baseHadoopClientsService.getSuperKeystorePassword(),
          trustStore, baseHadoopClientsService.getSuperTrustStorePassword()), Integer.parseInt(targetHost_port[1]));
      Protocol.registerProtocol("https", httpsProto);
      // Execute the request
      HttpClientParams params = new HttpClientParams();
      params.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
      params.setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS,
          true);
      HttpClient client = new HttpClient(params);
      HostConfiguration config = new HostConfiguration();
      InetAddress localAddress = InetAddress.getLocalHost();
      config.setLocalAddress(localAddress);

      HttpMethod m = new GetMethod(proxyRequestUri);
      Enumeration<String> names = servletRequest.getHeaderNames();
      while (names.hasMoreElements()) {
        String headerName = names.nextElement();
        String value = servletRequest.getHeader(headerName);
        if (PASS_THROUGH_HEADERS.contains(headerName)) {
          //hdfs does not send back the js if encoding is not accepted
          //but we don't want to accept encoding for the html because we
          //need to be able to parse it
          if (headerName.equalsIgnoreCase("accept-encoding") && (servletRequest.getPathInfo() == null
              || !servletRequest.getPathInfo().contains(".js"))) {
            continue;
          } else {
            m.setRequestHeader(headerName, value);
          }
        }
      }
      String user = servletRequest.getRemoteUser();
      if (user != null && !user.isEmpty()) {
        m.setRequestHeader("Cookie", "proxy-user" + "="
            + URLEncoder.encode(user, "ASCII"));
      }

      client.executeMethod(config, m);

      // Process the response
      int statusCode = m.getStatusCode();

      // Pass the response code. This method with the "reason phrase" is 
      //deprecated but it's the only way to pass the reason along too.
      //noinspection deprecation
      servletResponse.setStatus(statusCode, m.getStatusLine().
          getReasonPhrase());

      copyResponseHeaders(m, servletRequest, servletResponse);

      // Send the content to the client
      copyResponseEntity(m, servletResponse);

    } catch (Exception e) {
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

    }
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
          while ((inputLine = br.readLine()) != null)  {
            String outputLine = hopify(inputLine, targetUri) + "\n";
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

    ui = ui.replaceAll("<a href='http://hadoop.apache.org/core'>Hadoop</a>, 2018.", "");
    ui = ui.replaceAll("(?<=(url=))(?=[a-z])", "/hopsworks-api/hdfsui/");
    ui = ui.replaceAll("(?<=(href|src)=\")/(?=[a-z])",
        "/hopsworks-api/hdfsui/" + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=\')/(?=[a-z])",
        "/hopsworks-api/hdfsui/" + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=\")//", "/hopsworks-api/hdfsui/");
    ui = ui.replaceAll("(?<=(href|src)=\')//", "/hopsworks-api/hdfsui/");
    ui = ui.replaceAll("(?<=(href|src)=\")(?=(http|https))",
        "/hopsworks-api/hdfsui/");
    ui = ui.replaceAll("(?<=(href|src)=\')(?=(http|https))",
        "/hopsworks-api/hdfsui/");
    ui = ui.replaceAll("(?<=(href|src)=\")(?=[a-z])",
        "/hopsworks-api/hdfsui/" + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=\')(?=[a-z])",
        "/hopsworks-api/hdfsui/" + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=)/(?=[a-z])",
        "/hopsworks-api/hdfsui/" + source + "/");
    ui = ui.replaceAll("(?<=(action)=\")(?=[a-zA-Z/]*.jsp)", "/hopsworks-api/hdfsui/" + source + "/");
    return ui;

  }

//  @Override
//  protected String rewriteUrlFromResponse(HttpServletRequest servletRequest,
//      String theUrl) {
//    //TODO document example paths
//    final String targetUri = getTargetUri(servletRequest);
//    String curUrl = servletRequest.getRequestURL().toString();//no query
//
//    String pathInfo = servletRequest.getPathInfo();
//    if (pathInfo != null) {
//      assert curUrl.endsWith(pathInfo);
//      curUrl = curUrl.substring(0, curUrl.length() - pathInfo.length());//take pathInfo off
//    }
//    if (theUrl.startsWith(targetUri)) {
//      if (curUrl.endsWith("/") || theUrl.substring(targetUri.length()).startsWith("/")) {
//        theUrl = curUrl + theUrl.substring(targetUri.length());
//      } else {
//        theUrl = curUrl + "/" + theUrl.substring(targetUri.length());
//      }
//    } else if (curUrl.endsWith("/") || theUrl.substring(targetUri.length()).startsWith("/")) {
//      theUrl = curUrl + theUrl;
//    } else {
//      theUrl = curUrl + "/" + theUrl;
//    }
//    return theUrl;
//  }
  protected String rewriteUrlFromRequest(HttpServletRequest servletRequest) {
    StringBuilder uri = new StringBuilder(500);
    if (servletRequest.getPathInfo() != null && servletRequest.getPathInfo().matches(
        "/http([a-z,:,/,.,0-9,-])+:([0-9])+(.)+")) {
      
      // Remove '/' from the beginning of the path
      String target = servletRequest.getPathInfo().substring(1);
      if (target.startsWith("https")) {
        target = "https:/" + target.substring(6);
      } else {
        target = "http:/" + target.substring(5);
      }
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
