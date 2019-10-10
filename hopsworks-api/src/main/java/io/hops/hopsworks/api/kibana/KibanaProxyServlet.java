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

import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.common.util.Settings;
import org.apache.http.HttpStatus;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.params.HttpParams;
import org.apache.http.ssl.SSLContexts;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class KibanaProxyServlet extends ProxyServlet {
  
  @EJB
  private Settings settings;
  @EJB
  private BaseHadoopClientsService clientsService;
  
  private static final Logger LOG = Logger.getLogger(KibanaProxyServlet.class.getName());
  
  @Override
  protected void initTarget() throws ServletException {
    targetUri = settings.getKibanaUri();
    
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
  protected HttpClient createHttpClient(HttpParams hcParams) {
    SSLContext sslCtx = null;
    if (settings.isElasticOpenDistroSecurityEnabled()) {
      Path trustStore = Paths
          .get(clientsService.getSuperTrustStorePath());
      char[] trustStorePassword =
          clientsService.getSuperTrustStorePassword().toCharArray();
      try {
        sslCtx = SSLContexts.custom()
            .loadTrustMaterial(trustStore.toFile(), trustStorePassword)
            .build();
      } catch (GeneralSecurityException | IOException e) {
        LOG.log(Level.SEVERE, e.getMessage(), e);
      }
    }
  
    HttpClientBuilder clientBuilder = HttpClients.custom();
    if(sslCtx != null){
      clientBuilder.setSSLContext(sslCtx);
    }
  
    RequestConfig config =
        RequestConfig.custom().setCookieSpec(
            CookieSpecs.IGNORE_COOKIES).build();
    
    return clientBuilder.setDefaultRequestConfig(config)
        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
  }
  
  @Override
  protected void service(HttpServletRequest servletRequest,
      HttpServletResponse servletResponse)
      throws ServletException, IOException {
    try {
      super.service(servletRequest, servletResponse);
    }catch (ClientProtocolException e){
      if(e.getCause() instanceof CircularRedirectException){
        servletResponse.sendError(HttpStatus.SC_UNAUTHORIZED, "UnAuthorized: " +
            "Authentication token has expired. Please open Kibana again.");
      }else{
        throw e;
      }
    }
  }
}
