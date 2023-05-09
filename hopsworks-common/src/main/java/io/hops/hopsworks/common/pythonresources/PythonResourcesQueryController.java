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

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Map;

@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public interface PythonResourcesQueryController {

  String CLUSTER_CURRENT_CPU_USAGE = "cluster_current_cpu_usage";
  String DOCKER_TOTAL_ALLOCATABLE_CPU_KEY = "docker_allocatable_cpu";
  String DOCKER_CURRENT_CPU_USAGE_KEY = "docker_current_cpu_usage";
  String DOCKER_TOTAL_ALLOCATABLE_MEMORY_KEY = "docker_total_memory";
  String DOCKER_CURRENT_MEMORY_USAGE_KEY = "docker_current_memory_usage";
  String CLUSTER_TOTAL_MEMORY_CAPACITY = "cluster_total_memory";
  String CLUSTER_TOTAL_CPU_CAPACITY = "cluster_total_cpu";
  String CLUSTER_CURRENT_MEMORY_USAGE = "cluster_current_memory_usage";

  Map<String, String> getPrometheusQueries() throws ServiceDiscoveryException;

}
