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

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
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
import io.hops.hopsworks.common.util.Ip;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

/**
 * This embeds Membrane as a servlet.
 */
@SuppressWarnings({"serial"})
public class MembraneServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final Log logger = LogFactory.getLog(MembraneServlet.class);

  @Override
  public void init(ServletConfig config) throws ServletException {
  }

  @Override
  public void destroy() {
  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
          throws ServletException, IOException {
    String queryString = req.getQueryString() == null ? "" : "?" + req.
            getQueryString();

    Router router;

// For websockets, the following paths are used by JupyterHub:
//  /(user/[^/]*)/(api/kernels/[^/]+/channels|terminals/websocket)/?
// forward to ws(s)://servername:port_number
//<LocationMatch "/mypath/(user/[^/]*)/(api/kernels/[^/]+/channels|terminals/websocket)(.*)">
//    ProxyPassMatch ws://localhost:8999/mypath/$1/$2$3
//    ProxyPassReverse ws://localhost:8999 # this may be superfluous
//</LocationMatch>
//        ProxyPass /api/kernels/ ws://192.168.254.23:8888/api/kernels/
//        ProxyPassReverse /api/kernels/ http://192.168.254.23:8888/api/kernels/
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

    int x = ctxPath.indexOf("/jupyter");
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
      Logger.getLogger(MembraneServlet.class.getName()).log(Level.SEVERE, null,
              ex);
    }

  }

}
