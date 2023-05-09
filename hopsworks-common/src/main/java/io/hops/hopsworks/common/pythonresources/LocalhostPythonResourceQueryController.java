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

package io.hops.hopsworks.common.pythonresources;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.integrations.LocalhostStereotype;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.GlassfishTags;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.Map;

@LocalhostStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LocalhostPythonResourceQueryController implements PythonResourcesQueryController {

  @EJB
  private Settings settings;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;

  @Override
  public Map<String, String> getPrometheusQueries() throws ServiceDiscoveryException {
    if (settings.isDockerCgroupEnabled()) {
      //If cgroups are enabled we use metrics from cadvisor
      Map<String, String> queries = getCadvisorQueries();
      queries.putAll(getNodeExporterQueriesHeadNode());
      return queries;
    } else {
      //cgroups not enabled and no kubernetes: only get headnode metrics
      return getNodeExporterQueriesHeadNode();
    }
  }

  private Map<String, String> getNodeExporterQueriesHeadNode() throws ServiceDiscoveryException {
    Service glassfishService = serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
        HopsworksService.GLASSFISH.getNameWithTag(GlassfishTags.hopsworks));
    Service nodeExporterService =
        serviceDiscoveryController.getAnyAddressOfServiceWithDNS(HopsworksService.NODE_EXPORTER.getName());
    String headNodeQuery = "instance='" + glassfishService.getAddress() + ":" + nodeExporterService.getPort() + "'";
    return new HashMap<String, String>() {
      {
        put(CLUSTER_CURRENT_CPU_USAGE,
            "100 - ((sum((avg by (instance) (rate(node_cpu_seconds_total{mode='idle', " + headNodeQuery + "}[1m])) " +
                "* 100)))/(count(node_memory_Active_bytes{" + headNodeQuery + "})))");
        put(CLUSTER_CURRENT_MEMORY_USAGE, "sum(node_memory_Active_bytes{" + headNodeQuery + "})");
        put(CLUSTER_TOTAL_MEMORY_CAPACITY, "sum(node_memory_MemTotal_bytes{" + headNodeQuery + "})");
      }
    };
  }

  private Map<String, String> getCadvisorQueries() throws ServiceDiscoveryException {
    final String dockerCgroupParent = settings.getDockerCgroupParent();
    String cGroupParentForRegex = dockerCgroupParent.replaceAll("\\.", "\\\\\\\\.");

    Service glassfishService = serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
        HopsworksService.GLASSFISH.getNameWithTag(GlassfishTags.hopsworks));
    Service nodeExporterService =
        serviceDiscoveryController.getAnyAddressOfServiceWithDNS(HopsworksService.NODE_EXPORTER.getName());
    String headNodeInstanceQuery = "instance='" + glassfishService.getAddress() + ":"
        + nodeExporterService.getPort() + "'";
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

}
