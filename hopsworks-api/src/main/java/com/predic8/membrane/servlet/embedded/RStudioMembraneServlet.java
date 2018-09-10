/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.predic8.membrane.servlet.embedded;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.predic8.membrane.core.Router;
import com.predic8.membrane.core.RuleManager;
import com.predic8.membrane.core.rules.ProxyRule;
import com.predic8.membrane.core.rules.ProxyRuleKey;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;
import io.hops.hopsworks.api.kibana.ProxyServlet;
import io.hops.hopsworks.common.util.Ip;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * This embeds Membrane as a servlet.
 */
@SuppressWarnings({"serial"})
public class RStudioMembraneServlet extends ProxyServlet {

  private static final long serialVersionUID = 1L;
  private static final Log logger = LogFactory.getLog(RStudioMembraneServlet.class);

  private static final HashSet<String> PASS_THROUGH_HEADERS
      = new HashSet<String>(
          Arrays
              .asList("User-Agent", "user-agent", "Accept", "accept",
                  "Accept-Encoding", "accept-encoding",
                  "Accept-Language",
                  "accept-language",
                  "Accept-Charset", "accept-charset"));

  @Override
  public void init(ServletConfig config) throws ServletException {
  }

  @Override
  public void destroy() {
  }

  /**
   * *
   * https://support.rstudio.com/hc/en-us/articles/200552326-Running-RStudio-Server-with-a-Proxy
   * <p>
   * <p>
   * https://github.com/rstudio/rstudio/issues/1676
   * <p>
   * This example hosts RStudio under the example subdirectory /rstudio.
   * <p>
   * <VirtualHost *:80>
   * <p>
ProxyPreserveHost on
   * <p>
   * # Some required redirects for rstudio to work under a subdirectory
   * Redirect /rstudio /rstudio/
   * Redirect /auth-sign-in /rstudio/auth-sign-in
   * Redirect /auth-sign-out /rstudio/auth-sign-out
   * Redirect /s /rstudio/s
   * Redirect /admin /rstudio/admin
   * <p>
   * # Catch RStudio redirecting improperly from the auth-sign-in page
   * <If "%{HTTP_REFERER} =~ /auth-sign-in/">
   * RedirectMatch ^/$	/rstudio/
   * </If>
   * <p>
RewriteEngine on
   * RewriteCond %{HTTP:Upgrade} =websocket
   * RewriteRule /rstudio/(.*) ws://localhost:8787/$1 [P,L]
   * RewriteCond %{HTTP:Upgrade} !=websocket
   * RewriteRule /rstudio/(.*) http://localhost:8787/$1 [P,L]
   * ProxyPass /rstudio/ http://localhost:8787/
   * ProxyPassReverse /rstudio/ http://localhost:8787/
   * <p>
   * </VirtualHost>
   * <p>
   * <p>
   */
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String queryString = req.getQueryString() == null ? "" : "?" + req.
        getQueryString();

    Router router;

    List<NameValuePair> pairs;
    try {
      //note: HttpClient 4.2 lets you parse the string without building the URI
      pairs = URLEncodedUtils.parse(new URI(queryString), "UTF-8");
    } catch (URISyntaxException e) {
      throw new ServletException("Unexpected URI parsing error on "
          + queryString, e);
    }
    LinkedHashMap<String, String> params = new LinkedHashMap<>();
    for (NameValuePair pair : pairs) {
      params.put(pair.getName(), pair.getValue());
    }

    String externalIp = Ip.getHost(req.getRequestURL().toString());

    StringBuffer urlBuf = new StringBuffer("http://localhost:");

    String ctxPath = req.getRequestURI();

    int x = ctxPath.indexOf("/rstudio");
    int firstSlash = ctxPath.indexOf('/', x + 1);
    int secondSlash = ctxPath.indexOf('/', firstSlash + 1);
    String portString = ctxPath.substring(firstSlash + 1, secondSlash);
    Integer targetPort;
    try {
      targetPort = Integer.parseInt(portString);
    } catch (NumberFormatException ex) {
      logger.error("Invalid target port in the URL: " + portString);
      return;
    }
    urlBuf.append(portString);

    String newTargetUri = urlBuf.toString() + req.getRequestURI();

    StringBuilder newQueryBuf = new StringBuilder();
    newQueryBuf.append(newTargetUri);
    newQueryBuf.append(queryString);

    URI targetUriObj = null;
    try {
      targetUriObj = new URI(newQueryBuf.toString());
    } catch (Exception e) {
      throw new ServletException("Rewritten targetUri is invalid: "
          + newTargetUri, e);
    }
    ServiceProxy sp = new ServiceProxy(
        new ServiceProxyKey(
            externalIp, "*", "*", -1),
        "localhost", targetPort);
//    ServiceProxy sp = new ServiceProxy(
//            new ServiceProxyKey(
//                    externalIp, "*", "*", -1),
//            "localhost", targetPort);
    sp.setTargetURL(newQueryBuf.toString());
    // only set external hostname in case admin console is used
    try {
      router = new HopsRouter(targetUriObj);
      router.add(sp);
      router.init();
      ProxyRule proxy = new ProxyRule(new ProxyRuleKey(-1));
      router.getRuleManager().addProxy(proxy,
          RuleManager.RuleDefinitionSource.MANUAL);
      router.getRuleManager().addProxy(sp,
          RuleManager.RuleDefinitionSource.MANUAL);
      new HopsServletHandler(req, resp, router.getTransport(),
          targetUriObj).run();
    } catch (Exception ex) {
      Logger.getLogger(RStudioMembraneServlet.class.getName()).log(Level.SEVERE, null,
          ex);
    }

  }

  private String urlRewrite(String ui, String source, String port) {

    ui = ui.replaceAll("(?<=(href|src)=\")/(?=[a-zA-Z])", "/hopsworks-api/rstudio/" + port + "/" + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=\')/(?=[a-zA-Z])", "/hopsworks-api/rstudio/" + port + "/" + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=\")//", "/hopsworks-api/rstudio/" + port + "/");
    ui = ui.replaceAll("(?<=(href|src)=\')//", "/hopsworks-api/rstudio/" + port + "/");
    ui = ui.replaceAll("(?<=(href|src)=\")(?=http)", "/hopsworks-api/rstudio/" + port + "/");
    ui = ui.replaceAll("(?<=(href|src)=\')(?=http)", "/hopsworks-api/rstudio/" + port + "/");
    ui = ui.replaceAll("(?<=(href|src)=\")(?=[a-zA-Z])", "/hopsworks-api/rstudio/" + port + "/" + source + "/");
    ui = ui.replaceAll("(?<=(href|src)=\')(?=[a-zA-Z])", "/hopsworks-api/rstudio/" + port + "/" + source + "/");
    ui = ui.replaceAll("(?<=(url: '))/(?=[a-zA-Z])", "/hopsworks-api/rstudio/" + port + "/");
    ui = ui.replaceAll("(?<=(location\\.href = '))/(?=[a-zA-Z])", "/hopsworks-api/rstudio/" + port + "/");
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
