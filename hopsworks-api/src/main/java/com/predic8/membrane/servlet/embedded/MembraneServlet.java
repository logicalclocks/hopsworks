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
import io.hops.hopsworks.common.jupyter.JupyterManager;
import io.hops.hopsworks.common.util.Ip;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This embeds Membrane as a servlet.
 */
@SuppressWarnings({"serial"})
@Dependent
public class MembraneServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = Logger.getLogger(MembraneServlet.class.getName());

  @Inject
  private JupyterManager jupyterManager;
  
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
  protected void service(HttpServletRequest req, HttpServletResponse resp)
          throws ServletException, IOException {

    String externalIp = Ip.getHost(req.getRequestURL().toString());
    String jupyterHost = jupyterManager.getJupyterHost();
    
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
        new ServiceProxyKey(externalIp, "*", "*", -1), jupyterHost,
        targetPort);
    sp.setTargetURL(urlBuf.toString());

    try {
      Router router = new HopsRouter();
      router.add(sp);
      router.init();
      new HopsServletHandler(req, resp, router.getTransport(), targetUriObj).run();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }
}
