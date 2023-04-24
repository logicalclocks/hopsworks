/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.pythonresources;

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.util.PrometheusClient;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PythonResourcesController {

  private final static Logger LOGGER = Logger.getLogger(PythonResourcesController.class.getName());

  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  @EJB
  private PrometheusClient client;
  @EJB
  private Settings settings;

  private static JSONObject pythonResources = new JSONObject();


  private final String DOCKER_TOTAL_ALLOCATABLE_CPU_KEY = "docker_allocatable_cpu";
  private final String DOCKER_CURRENT_CPU_USAGE_KEY = "docker_current_cpu_usage";
  private final String DOCKER_TOTAL_ALLOCATABLE_MEMORY_KEY = "docker_total_memory";
  private final String DOCKER_CURRENT_MEMORY_USAGE_KEY = "docker_current_memory_usage";
  private final String CLUSTER_TOTAL_MEMORY_CAPACITY = "cluster_total_memory";
  private final String CLUSTER_TOTAL_CPU_CAPACITY = "cluster_total_cpu";
  private final String CLUSTER_CURRENT_MEMORY_USAGE = "cluster_current_memory_usage";
  private final String CLUSTER_CURRENT_CPU_USAGE = "cluster_current_cpu_usage";

  private Integer nodeExporterPort;
  private String glassfishIp;

  @PostConstruct
  public void init() {
    try {
      Service nodeExporterService =
          serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
              ServiceDiscoveryController.HopsworksService.NODE_EXPORTER);
      nodeExporterPort = nodeExporterService.getPort();
      Service glassfishService = serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
          ServiceDiscoveryController.HopsworksService.HOPSWORKS_APP);
      glassfishIp = glassfishService.getAddress();
    } catch (ServiceDiscoveryException e) {
      LOGGER.log(Level.INFO, e.getMessage());
    }
  }

  public JSONObject getPythonResources() throws ServiceDiscoveryException {
    Map<String, String>  pythonResourcesTypesQueries = updatePrometheusQueries();
    pythonResourcesTypesQueries.forEach((key, query) -> getResourceValue(key, query));

    pythonResources.put(CLUSTER_TOTAL_CPU_CAPACITY, 100);
    if (!settings.isDockerCgroupEnabled() || settings.getKubeInstalled()) {
      //use the same values as the cluster
      pythonResources.put(DOCKER_TOTAL_ALLOCATABLE_CPU_KEY, 100);
      pythonResources.put(DOCKER_TOTAL_ALLOCATABLE_MEMORY_KEY, pythonResources.get(CLUSTER_TOTAL_MEMORY_CAPACITY));
      pythonResources.put(DOCKER_CURRENT_MEMORY_USAGE_KEY, pythonResources.get(CLUSTER_CURRENT_MEMORY_USAGE));
      pythonResources.put(DOCKER_CURRENT_CPU_USAGE_KEY, pythonResources.get(CLUSTER_CURRENT_CPU_USAGE));
    }
    return pythonResources;
  }

  private void getResourceValue(String resource, String query) {
    try {
      JSONObject queryResult = client.execute(query);
      JSONArray resultObject = queryResult.getJSONObject("data").getJSONArray("result");
      if (resultObject.length() > 0) {
        pythonResources.put(resource, resultObject.getJSONObject(0).getJSONArray("value").getString(1));
      } else {
        pythonResources.put(resource, "");
      }
    } catch (ServiceException e) {
      pythonResources.put(resource, "");
    }
  }

  private Map<String, String> updatePrometheusQueries() throws ServiceDiscoveryException {
    if (nodeExporterPort == null) {
      Service nodeExporterService =
          serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
              ServiceDiscoveryController.HopsworksService.NODE_EXPORTER);
      nodeExporterPort = nodeExporterService.getPort();
    } else if (Strings.isNullOrEmpty(glassfishIp)) {
      Service glassfishService = serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
          ServiceDiscoveryController.HopsworksService.HOPSWORKS_APP);
      glassfishIp = glassfishService.getAddress();
    }

    if (settings.isDockerCgroupEnabled() && !settings.getKubeInstalled()) {
      //If cgroups are enabled we use metrics from cadvisor
      Map<String, String> queries = getCadvisorQueries();
      queries.putAll(getNodeExporterQueriesHeadNode());
      return queries;
    } else if (!settings.isDockerCgroupEnabled() && !settings.getKubeInstalled()) {
      //cgroups not enabled and no kubernetes: only get headnode metrics
      return getNodeExporterQueriesHeadNode();
    } else {
      //On kubernetes get from node exporter and exclude tainted nodes
      return getNodeExporterQueriesKube();
    }
  }

  private Map<String, String> getNodeExporterQueriesKube() {
    String nodeQuery = getExcludedNodesInResourceQuery();
    String nodeQueryNoAppend = nodeQuery.replaceAll(",", "");
    return new HashMap<String, String>() {
      {
        put (CLUSTER_CURRENT_CPU_USAGE,
            "100 - ((sum((avg by (instance) (rate(node_cpu_seconds_total{mode='idle'" + nodeQuery + "}[1m])) " +
                "* 100)))/(count(node_memory_Active_bytes{" + nodeQueryNoAppend + "})))");
        put(CLUSTER_CURRENT_MEMORY_USAGE, "sum(node_memory_Active_bytes{" + nodeQueryNoAppend + "})");
        put(CLUSTER_TOTAL_MEMORY_CAPACITY, "sum(node_memory_MemTotal_bytes{" + nodeQueryNoAppend + "})");
      }
    };
  }

  private Map<String, String> getCadvisorQueries() {
    final String dockerCgroupParent = settings.getDockerCgroupParent();
    String cGroupParentForRegex = dockerCgroupParent.replaceAll("\\.", "\\\\\\\\.");
    final String headNodeInstanceQuery = "instance='" + glassfishIp + ":" + nodeExporterPort + "'";
    return new HashMap<String, String>() {
      {
        put(DOCKER_CURRENT_CPU_USAGE_KEY,
            "sum(avg by (cpu) (rate(container_cpu_usage_seconds_total{id=~'.*/" + cGroupParentForRegex
                + "/.*'}[60s]) * 100))");
        put(DOCKER_TOTAL_ALLOCATABLE_CPU_KEY,
            "(sum(container_spec_cpu_quota{id='/" + dockerCgroupParent + "'})/("
                + settings.getDockerCgroupCpuPeriod()
                + " * (count(count(node_cpu_seconds_total{" + headNodeInstanceQuery + "}) without (mode,instance,job))"
                + "without (cpu))))*100");
        put(DOCKER_CURRENT_MEMORY_USAGE_KEY, "sum(container_memory_working_set_bytes{id=~'.*/" + cGroupParentForRegex
            + "/.*'})");
        put(DOCKER_TOTAL_ALLOCATABLE_MEMORY_KEY, "container_spec_memory_limit_bytes{id='/" + dockerCgroupParent
            + "'}");
      }
    };
  }

  private Map<String, String> getNodeExporterQueriesHeadNode() {
    final String headNodeQuery = "instance='" + glassfishIp + ":" + nodeExporterPort + "'";
    return new HashMap<String, String>() {
      {
        put(CLUSTER_CURRENT_CPU_USAGE,
            "100 - ((sum((avg by (instance) (rate(node_cpu_seconds_total{mode='idle', " + headNodeQuery + "}[1m])) "
                + "* 100)))/(count(node_memory_Active_bytes{" + headNodeQuery + "})))");
        put(CLUSTER_CURRENT_MEMORY_USAGE, "sum(node_memory_Active_bytes{" + headNodeQuery + "})");
        put(CLUSTER_TOTAL_MEMORY_CAPACITY, "sum(node_memory_MemTotal_bytes{" + headNodeQuery + "})");
      }
    };
  }

  private String getExcludedNodesInResourceQuery() {
    if (!settings.getKubeInstalled()) {
      return "";
    }
    String taintedNodesStr = settings.getKubeTaintedNodes();
    List<String> nodes =
        new ArrayList<>(Arrays.asList(taintedNodesStr.split(","))
            .stream().filter(n -> !Strings.isNullOrEmpty(n)).collect(Collectors.toList()));
    String taintedNodesQuery = "";
    for (int i = 0; i < nodes.size(); i++) {
      if (i == 0) {
        taintedNodesQuery += ", instance !~ '";
      }
      if (i < nodes.size() - 1) {
        taintedNodesQuery += nodes.get(i) + ":" + nodeExporterPort + "|";
      } else {
        taintedNodesQuery += nodes.get(i) + ":" + nodeExporterPort + "'";
      }
    }
    return taintedNodesQuery;
  }
}
