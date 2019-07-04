/*
 * Copyright 2012 predic8 GmbH, www.predic8.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.predic8.membrane.servlet.embedded;

import com.predic8.membrane.core.Router;
import com.predic8.membrane.core.rules.ServiceProxy;
import com.predic8.membrane.core.rules.ServiceProxyKey;
import io.hops.hopsworks.common.util.Ip;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apache.commons.collections4.map.CaseInsensitiveMap;

import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This embeds Membrane as a servlet.
 */
@SuppressWarnings({"serial"})
public class MembraneServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = Logger.getLogger(MembraneServlet.class.getName());

  @EJB
  Settings settings;

  private String jupyterHost;

  private static final String ORIGIN = "Origin";
  private static final String WEBSOCKET_ORIGIN = "Sec-Websocket-Origin";

  /*
   * For websockets, the following paths are used by JupyterHub:
   *
   * /(user/[^/]*)/(api/kernels/[^/]+/channels|terminals/websocket)/?
   * forward to ws(s)://servername:port_number
   *
   * <LocationMatch "/mypath/(user/[^/]*)/(api/kernels/[^/]+/channels|terminals/websocket)(.*)">
   *   ProxyPassMatch ws://localhost:8999/mypath/$1/$2$3
   *   ProxyPassReverse ws://localhost:8999 # this may be superfluous
   * </LocationMatch>
   * ProxyPass /api/kernels/ ws://192.168.254.23:8888/api/kernels/
   * ProxyPassReverse /api/kernels/ http://192.168.254.23:8888/api/kernels/
   */

  @Override
  public void init() throws ServletException {
    super.init();
    jupyterHost = settings.getJupyterHost();
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
          throws ServletException, IOException {

    String externalIp = Ip.getHost(req.getRequestURL().toString());

    // See HOPSWORKS-1185 for an explanation
    String requestUrl = req.getRequestURL().toString();
    MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(req);
    mutableRequest.putHeader(ORIGIN,
        requestUrl.substring(0, requestUrl.indexOf("/", 7)));
    mutableRequest.putHeader(WEBSOCKET_ORIGIN,
        requestUrl.substring(0, requestUrl.indexOf("/", 7)));

    StringBuilder urlBuf = new StringBuilder("http://");
    urlBuf.append(jupyterHost);
    urlBuf.append(":");

    String pathInfo = req.getPathInfo();
    int endPort = pathInfo.indexOf('/', 1);
    String portString = pathInfo.substring(1, endPort);
    Integer targetPort;
    try {
      targetPort = Integer.parseInt(portString);
    } catch (NumberFormatException ex) {
      LOGGER.log(Level.SEVERE, "Invalid target port in the URL: " + portString);
      return;
    }
    urlBuf.append(portString);
    urlBuf.append(req.getRequestURI());
    if (req.getQueryString() != null) {
      urlBuf.append('?').append(req.getQueryString());
    }

    URI targetUriObj = null;
    try {
      targetUriObj = new URI(urlBuf.toString());
    } catch (Exception e) {
      throw new ServletException("Rewritten targetUri is invalid: " + urlBuf.toString(), e);
    }

    ServiceProxy sp = new ServiceProxy(
        new ServiceProxyKey(externalIp, "*", "*", -1), jupyterHost, targetPort);
    sp.setTargetURL(urlBuf.toString());

    try {
      Router router = new HopsRouter();
      router.add(sp);
      router.init();
      new HopsServletHandler(mutableRequest, resp, router.getTransport(), targetUriObj).run();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  private class MutableHttpServletRequest extends HttpServletRequestWrapper {

    // holds custom header and value mapping
    private final CaseInsensitiveMap<String, String> customHeaders;

    public MutableHttpServletRequest(HttpServletRequest request) {
      super(request);
      this.customHeaders = new CaseInsensitiveMap<>();

      // now add the headers from the wrapped request object
      @SuppressWarnings("unchecked")
      Enumeration<String> e = request.getHeaderNames();
      while (e.hasMoreElements()) {
        String n = e.nextElement();
        customHeaders.put(n, request.getHeader(n));
      }
    }

    public void putHeader(String name, String value) {
      this.customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
      return customHeaders.get(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      Set<String> headers = new HashSet<>();
      if (customHeaders.containsKey(name)) {
        headers.add(customHeaders.get(name));
      }
      return Collections.enumeration(headers);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
      // create a set of the custom header names
      return Collections.enumeration(new HashSet<String>(customHeaders.keySet()));
    }
  }
}
