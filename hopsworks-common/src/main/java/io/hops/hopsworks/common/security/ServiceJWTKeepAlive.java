/*
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
 */

package io.hops.hopsworks.common.security;

import io.hops.hopsworks.common.util.Settings;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@DependsOn("Settings")
public class ServiceJWTKeepAlive {

  @EJB
  private Settings settings;

  private CloseableHttpClient httpClient = HttpClients.createDefault();
  private URI caURI = null;

  private final static String CA_PATH = "hopsworks-ca/v2/token";
  private final static Logger LOGGER = Logger.getLogger(ServiceJWTKeepAlive.class.getName());

  @PostConstruct
  public void init() {
    try {
      caURI = new URIBuilder(settings.getRestEndpoint())
        .setPath(CA_PATH)
        .build();
    } catch (URISyntaxException e) {
      LOGGER.log(Level.SEVERE, "Could not build the URI for the CA", e);
    }
  }

  @Schedule(persistent = false, hour = "*")
  public void checkTokenValid() {
    HttpContext httpContext = HttpClientContext.create();

    HttpGet getRequest = new HttpGet(caURI);
    getRequest.setHeader(HttpHeaders.AUTHORIZATION, settings.getServiceJWT());

    CloseableHttpResponse response = null;
    try {
      response = httpClient.execute(getRequest, httpContext);

      if (response.containsHeader(HttpHeaders.AUTHORIZATION)) {
        settings.setServiceJWT(response.getFirstHeader(HttpHeaders.AUTHORIZATION).getValue());
      }

      response.close();
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to send keepalive request, I'll try again in 1 hour", e);
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (IOException e) {}
      }
    }
  }
}
