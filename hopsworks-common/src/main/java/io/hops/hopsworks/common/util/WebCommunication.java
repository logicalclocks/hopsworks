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

package io.hops.hopsworks.common.util;

import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.annotation.PreDestroy;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class WebCommunication {

  private static final Logger logger = Logger.getLogger(WebCommunication.class.
          getName());

  private static boolean DISABLE_CERTIFICATE_VALIDATION = true;
  private static String PROTOCOL = "https";
  private static int PORT = 8090;
  private static String NOT_AVAILABLE = "Not available.";
  private final ConcurrentLinkedQueue<Client> inUseClientPool =
      new ConcurrentLinkedQueue<>();
  private final ConcurrentLinkedQueue<Client> availableClientPool =
      new ConcurrentLinkedQueue<>();

  public WebCommunication() {
  }

  @PreDestroy
  private void cleanUp() {
    for (Client client : availableClientPool) {
      client.close();
      client = null;
    }
    for (Client client : inUseClientPool) {
      client.close();
      client = null;
    }
  }

  /**
   *
   * @param operation start | stop | restart
   * @param hostAddress
   * @param agentPassword
   * @param service
   * @param service
   * @return
   */
  public String serviceOp(String operation, String hostAddress,
      String agentPassword, String group, String service) throws GenericException {
    String url = createUrl(operation, hostAddress, group, service);
    return fetchContent(url, agentPassword);
  }

  @Asynchronous
  public Future<String> asyncServiceOp(String operation, String hostAddress,
      String agentPassword, String group, String service) throws GenericException {
    String url = createUrl(operation, hostAddress, group, service);
    return new AsyncResult<>(fetchContent(url, agentPassword));
  }

  private String createUrl(String context, String hostAddress, String... args) {
    String template = "%s://%s:%s/%s";
    StringBuilder url = new StringBuilder(String.format(template, PROTOCOL, hostAddress, PORT, context));
    for (String arg : args) {
      url.append("/").append(arg);
    }
    return url.toString();
  }

  private String fetchContent(String url, String agentPassword) throws GenericException {
    String content = NOT_AVAILABLE;
    try {
      Response response = getWebResource(url, agentPassword);
      int code = response.getStatus();
      Family res = Response.Status.Family.familyOf(code);
      content = response.readEntity(String.class);
      if (res == Response.Status.Family.SUCCESSFUL) {
        return content;
      } else {
        throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR,
          Level.SEVERE, content, "Error code:" + code + " Reason: " + content);
      }
    } catch (KeyManagementException | NoSuchAlgorithmException e) {
      logger.log(Level.SEVERE, null, e);
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE, null, e.getMessage(), e);
    }
  }

  private Response getWebResource(String url, String agentPassword) throws NoSuchAlgorithmException,
      KeyManagementException {
    return getWebResource(url, agentPassword, null);
  }

  private Response getWebResource(String url, String agentPassword, Map<String, String> args) throws
      NoSuchAlgorithmException, KeyManagementException {

    Client client = getClient();
    WebTarget webResource = client.target(url);

    webResource = webResource.queryParam("username", Settings.AGENT_EMAIL);
    webResource = webResource.queryParam("password", agentPassword);
    if (args != null) {
      for (Map.Entry<String, String> entrySet : args.entrySet()) {
        webResource = webResource.queryParam(entrySet.getKey(), entrySet.getValue());
      }
    }
    logger.log(Level.FINEST,
            "WebCommunication: Requesting url: {0} with password {1}",
            new Object[]{url, agentPassword});
    
    Response response = webResource.request()
            .header("Accept-Encoding", "gzip,deflate")
            .get(Response.class);
    discardClient(client);
    logger.log(Level.INFO, "WebCommunication: Requesting url: {0}", url);
    return response;
  }
  
  private Client getClient() throws NoSuchAlgorithmException, KeyManagementException {
    Client client = availableClientPool.poll();
    if (null == client) {
      client = createClient();
    }
    
    inUseClientPool.offer(client);
    return client;
  }
  
  private void discardClient(Client client) {
    inUseClientPool.remove(client);
    availableClientPool.offer(client);
  }
  
  // This method should never be invoked but from the getClient() method
  private Client createClient() throws NoSuchAlgorithmException,
      KeyManagementException {
    if (DISABLE_CERTIFICATE_VALIDATION) {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[]{
          new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }
            
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
          }};
      
      // Ignore differences between given hostname and certificate hostname
      HostnameVerifier hv = new HostnameVerifier() {
        public boolean verify(String hostAddress, SSLSession session) {
          return true;
        }
      };
      
      // Install the all-trusting trust manager
      SSLContext sc = SSLContext.getInstance("TLSv1.2");
      sc.init(null, trustAllCerts, new SecureRandom());
      ClientBuilder clientBuilder = ClientBuilder.newBuilder()
          .hostnameVerifier(hv)
          .sslContext(sc);
      return clientBuilder.build();
    } else {
      return ClientBuilder.newClient();
    }
  }
}
