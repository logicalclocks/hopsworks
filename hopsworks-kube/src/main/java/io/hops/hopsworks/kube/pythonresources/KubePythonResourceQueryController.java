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

package io.hops.hopsworks.kube.pythonresources;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.pythonresources.PythonResourcesQueryController;
import io.hops.hopsworks.kube.common.KubeClientService;
import io.hops.hopsworks.kube.common.KubeStereotype;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@KubeStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubePythonResourceQueryController implements PythonResourcesQueryController {

  @EJB
  private KubeClientService kubeClientService;

  private final static String ALLOCATABLE_METRIC = "kube_node_status_allocatable";
  private final static String REQUESTED_METRIC = "kube_pod_container_resource_requests";
  private final static String RESOURCE_CPU = "resource='cpu'";
  private final static String RESOURCE_MEMORY = "resource='memory'";
  private final static String RESOURCE_GPU = "resource='nvidia_com_gpu'";

  @Override
  public Map<String, String> getPrometheusQueries() throws ServiceDiscoveryException {
    String nodes = kubeClientService.getNodeList().stream()
        .filter(n -> n.getSpec().getTaints().stream().noneMatch(t -> t.getEffect().equals("NoSchedule")))
        .map(n -> n.getMetadata().getName())
        .collect(Collectors.joining("|"));

    return new HashMap<String, String>() {
      {
        put (CLUSTER_CURRENT_CPU_USAGE,
            "100 * (sum by (job) (" + REQUESTED_METRIC + "{node=~'" + nodes + "', " + RESOURCE_CPU + "}) /" +
                " sum by (job) (" + ALLOCATABLE_METRIC + "{node=~'" + nodes + "', " + RESOURCE_CPU + "}))");
        put(CLUSTER_CURRENT_MEMORY_USAGE,
            "sum(" + REQUESTED_METRIC + "{node=~'" + nodes + "', " + RESOURCE_MEMORY +"})");
        put(CLUSTER_TOTAL_MEMORY_CAPACITY,
            "sum(" + ALLOCATABLE_METRIC + "{node=~'" + nodes + "', " + RESOURCE_MEMORY + "})");
        put(CLUSTER_CURRENT_GPU_USAGE,
            "sum(" + REQUESTED_METRIC + "{node=~'" + nodes + "', " + RESOURCE_GPU +"})");
        put(CLUSTER_TOTAL_GPU_CAPACITY,
            "sum(" + ALLOCATABLE_METRIC + "{node=~'" + nodes + "', " + RESOURCE_GPU+ "})");
      }
    };
  }
}
