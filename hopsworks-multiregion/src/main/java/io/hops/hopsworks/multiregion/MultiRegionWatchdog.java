/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.multiregion;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.httpclient.HttpConnectionManagerBuilder;
import io.hops.hopsworks.httpclient.HttpRetryableAction;
import io.hops.hopsworks.httpclient.ObjectResponseHandler;
import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class MultiRegionWatchdog {

  private static final Logger LOGGER = Logger.getLogger(MultiRegionWatchdog.class.getName());

  @EJB
  private MultiRegionConfiguration multiRegionConfiguration;

  @EJB
  private MultiRegionController multiRegionController;

  private PoolingHttpClientConnectionManager connectionManager;
  private CloseableHttpClient client;
  private ObjectMapper objectMapper;

  private HttpHost watchdogHost;

  @Resource
  private TimerService timerService;

  @PostConstruct
  public void init() {
    if (multiRegionConfiguration.getBoolean(
        MultiRegionConfiguration.MultiRegionConfKeys.MULTIREGION_WATCHDOG_ENABLED)) {
      setupWatchdog();
    }
  }

  private void setupWatchdog() {
    Long watchdogFrequency = multiRegionConfiguration.getInterval(
        MultiRegionConfiguration.MultiRegionConfKeys.MULTIREGION_WATCHDOG_INTERVAL);

    watchdogHost = HttpHost.create(
        multiRegionConfiguration.getString(MultiRegionConfiguration.MultiRegionConfKeys.MULTIREGION_WATCHDOG_URL));

    this.objectMapper = new ObjectMapper();
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    try {
      HttpConnectionManagerBuilder connectionManagerBuilder = new HttpConnectionManagerBuilder()
          .withTrustStore(Paths.get(
                  multiRegionConfiguration.getString(MultiRegionConfiguration.MultiRegionConfKeys.HOPSWORKS_DOMAIN_DIR),
                  "config", "cacerts.jks"),
              multiRegionConfiguration.getString(
                  MultiRegionConfiguration.MultiRegionConfKeys.HOPSWORKS_DEFAULT_SSL_MASTER_PASSWORD).toCharArray());
      connectionManager = createConnectionManager(connectionManagerBuilder);
      client = HttpClients.custom()
          .setConnectionManager(connectionManager)
          .setKeepAliveStrategy((httpResponse, httpContext) -> 300)
          .build();
      updateWatchdog();
      timerService.createTimer(0L, watchdogFrequency, "MultiRegion Watchdog frequency");
    } catch (IOException | GeneralSecurityException ex) {
      LOGGER.log(Level.SEVERE, "Could not setup watchdog", ex);
      throw new RuntimeException(ex);
    }
  }

  private PoolingHttpClientConnectionManager createConnectionManager(
      HttpConnectionManagerBuilder connectionManagerBuilder)
      throws IOException, GeneralSecurityException {
    PoolingHttpClientConnectionManager connectionManager =
        new PoolingHttpClientConnectionManager(connectionManagerBuilder.build());
    connectionManager.setMaxTotal(10);
    connectionManager.setDefaultMaxPerRoute(10);
    return connectionManager;
  }

  @Timeout
  public void updateWatchdog(Timer timer) throws IOException {
    updateWatchdog();
  }

  private void updateWatchdog() {
    try {
      MultiRegionWatchdogDTO multiRegionWatchdogDTO = pollJudge();
      multiRegionController.setPrimaryRegionName(multiRegionWatchdogDTO.getActive());
      multiRegionController.setSecondaryRegionName(multiRegionWatchdogDTO.getSecondary());
      multiRegionController.setPrimaryRegion(multiRegionWatchdogDTO.getActive().equalsIgnoreCase(
          multiRegionConfiguration.getString(MultiRegionConfiguration.MultiRegionConfKeys.MULTIREGION_WATCHDOG_REGION)
      ));
    } catch (IOException e)  {
      LOGGER.log(Level.SEVERE, "Could not poll Judge service", e);
      // exception has happened while contacting the judge, we should disable writes for safety
      multiRegionController.setPrimaryRegionName("could-not-poll-judge");
      multiRegionController.setSecondaryRegionName("could-not-poll-judge");
      multiRegionController.setPrimaryRegion(false);
    }
  }

  public MultiRegionWatchdogDTO pollJudge() throws IOException {
    HttpGet getRequest = new HttpGet();
    return new HttpRetryableAction<MultiRegionWatchdogDTO>() {
      @Override
      public MultiRegionWatchdogDTO performAction() throws ClientProtocolException, IOException {
        return client.execute(watchdogHost, getRequest,
            new ObjectResponseHandler<>(MultiRegionWatchdogDTO.class, objectMapper));
      }
    }.performAction();
  }

  public void setMultiRegionConfiguration(MultiRegionConfiguration multiRegionConfiguration) {
    this.multiRegionConfiguration = multiRegionConfiguration;
  }

  public void setMultiRegionController(MultiRegionController multiRegionController) {
    this.multiRegionController = multiRegionController;
  }
}
