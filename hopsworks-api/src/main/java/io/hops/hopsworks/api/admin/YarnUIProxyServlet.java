/*
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
 *
 */

package io.hops.hopsworks.api.admin;

import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
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
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.utils.URIUtils;

@Stateless
public class YarnUIProxyServlet extends ProxyServlet {

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
    // TODO - should get the Kibana URI from Settings.java
//    targetUri = Settings.getKibanaUri();
    targetUri = settings.getYarnWebUIAddress();
    if (!targetUri.contains("http://")) {
      targetUri = "http://" + targetUri;
    }
    if (targetUri == null) {
      throw new ServletException(P_TARGET_URI + " is required.");
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
      // Execute the request

      HttpClientParams params = new HttpClientParams();
      params.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
      params.setBooleanParameter(HttpClientParams.ALLOW_CIRCULAR_REDIRECTS,
          true);
      HttpClient client = new HttpClient(params);
      HostConfiguration config = new HostConfiguration();
      InetAddress localAddress = InetAddress.getLocalHost();
      config.setLocalAddress(localAddress);

      String method = servletRequest.getMethod();
      HttpMethod m;
      if (method.equalsIgnoreCase("PUT")) {
        m = new PutMethod(proxyRequestUri);
        RequestEntity requestEntity = new InputStreamRequestEntity(servletRequest.getInputStream(), servletRequest.
            getContentType());
        ((PutMethod) m).setRequestEntity(requestEntity);
      } else {
        m = new GetMethod(proxyRequestUri);
      }
      Enumeration<String> names = servletRequest.getHeaderNames();
      while (names.hasMoreElements()) {
        String headerName = names.nextElement();
        String value = servletRequest.getHeader(headerName);
        if (PASS_THROUGH_HEADERS.contains(headerName)) {
          //yarn does not send back the js if encoding is not accepted
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
          String source = "http://" + method.getURI().getHost() + ":" + method.getURI().getPort();
          while ((inputLine = br.readLine()) != null)  {
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
        "/hopsworks-api/yarnui/" + source + "/" );
    ui = ui.replaceAll("(?<=(href|src)=\')(?=[a-zA-Z])",
        "/hopsworks-api/yarnui/" + source + "/" );
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
