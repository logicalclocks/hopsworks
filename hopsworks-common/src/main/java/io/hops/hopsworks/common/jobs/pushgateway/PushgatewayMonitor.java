/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.jobs.pushgateway;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;

import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
@DependsOn("Settings")
public class PushgatewayMonitor {

  private static final Logger LOGGER = Logger.getLogger(PushgatewayMonitor.class.getName());

  private static final String METRICS_ENDPOINT = "/api/v1/metrics";
  private static final Pattern applicationIdPattern = Pattern.compile("(application_.*?_\\d*)");

  @EJB
  private HttpClient httpClient;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private YarnClientService ycs;

  @Schedule(persistent = false, second = "0", minute = "*", hour = "*")
  public synchronized void monitor(Timer timer) {
    try {
      PushgatewayResults results = scrapeMetrics();
      Set<String> activeApplications = getActiveApplications(results);
      List<String> applicationsToRemove = getApplicationsToRemove(activeApplications);

      removeActiveApplications(results, applicationsToRemove);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error processing pushgateway timer", e);
    }
  }

  private PushgatewayResults scrapeMetrics() throws ServiceDiscoveryException, IOException {
    Service pushgatewayService =
        serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
            ServiceDiscoveryController.HopsworksService.PUSHGATEWAY);
    HttpHost pushgatewayHost = new HttpHost(pushgatewayService.getAddress(), pushgatewayService.getPort());
    return httpClient.execute(pushgatewayHost, new HttpGet(METRICS_ENDPOINT),
        new HttpClient.ObjectResponseHandler<>(PushgatewayResults.class, httpClient.getObjectMapper()));
  }

  // Iterate over all labels and extract the unique app_id currently exported in
  private Set<String> getActiveApplications(PushgatewayResults pushgatewayResults) {
    return pushgatewayResults.getData().stream().flatMap(m -> m.values().stream())
        .filter(serie -> serie.getMetrics() != null)
        .map(PushgatewaySerie::getMetrics)
        .map(metric -> metric.get(0).getLabels().get("job"))
        .filter(applicationId -> applicationIdPattern.matcher(applicationId).matches())
        .collect(Collectors.toSet());
  }

  private void removeActiveApplications(PushgatewayResults pushgatewayResults, List<String> applications)
      throws ServiceDiscoveryException {
    Set<Map<String, String>> groupsToRemove = new HashSet<>();
    // Get unique group information (labels) containing the application id to remove
    for (String application : applications) {
      groupsToRemove.addAll(pushgatewayResults.getData().stream().flatMap(m -> m.values().stream())
          .filter(serie -> serie.getMetrics() != null)
          .map(PushgatewaySerie::getMetrics)
          .map(metric -> metric.get(0).getLabels())
          .filter(labels -> labels.get("job").equalsIgnoreCase(application))
          .collect(Collectors.toSet()));
    }

    // For each group send a request to pushgateway to remove the group
    Service pushgatewayService = serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
            ServiceDiscoveryController.HopsworksService.PUSHGATEWAY);
    HttpHost pushgatewayHost = new HttpHost(pushgatewayService.getAddress(), pushgatewayService.getPort());
    String groupPath = "";
    for (Map<String, String> group : groupsToRemove) {
      // Job has to go first
      String job = group.remove("job");
      groupPath = "/metrics/job/" + job + "/" + group.entrySet().stream()
          .map(e -> e.getKey() + "/" + e.getValue())
          .collect(Collectors.joining("/"));
      HttpDelete httpDelete = new HttpDelete(groupPath);

      try {
        httpClient.execute(pushgatewayHost, httpDelete, new HttpClient.NoBodyResponseHandler<>());
      } catch (IOException e) {
        // Keep iterating if there is an issue with a group
        LOGGER.log(Level.SEVERE, "Error deleting group: " + groupPath, e);
      }
    }
  }

  private List<String> getApplicationsToRemove(Set<String> activeApplications) {
    List<String> applicationsToRemove = new ArrayList<>();
    YarnClientWrapper yarnClientWrapper = null;

    try {
      yarnClientWrapper = ycs.getYarnClientSuper();
      YarnClient yarnClient = yarnClientWrapper.getYarnClient();

      for (String application : activeApplications) {
        FinalApplicationStatus applicationStatus = null;
        try {
          applicationStatus = yarnClient
              .getApplicationReport(ApplicationId.fromString(application))
              .getFinalApplicationStatus();
        } catch (YarnException | IOException yex) {
          // Keep iterating if there is an issue with an application
          LOGGER.log(Level.SEVERE, "Error retrieving status for application: " + application, yex);
        }

        if (applicationStatus != null && applicationStatus != FinalApplicationStatus.UNDEFINED) {
          applicationsToRemove.add(application);
        }
      }
    } finally {
      ycs.closeYarnClient(yarnClientWrapper);
    }

    return applicationsToRemove;
  }
}
