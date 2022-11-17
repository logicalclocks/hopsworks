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
package io.hops.hopsworks.common.python.environment;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface DockerRegistryMngr {

  void gc() throws IOException, ServiceException, ProjectException;
  
  public Map<String, Future<ProcessResult>> backupImages(String backupId) 
          throws IOException, ServiceDiscoveryException;
  
  public Map<String, Future<ProcessResult>> resotreImages(String backupId) 
          throws IOException, ServiceDiscoveryException;
  
  public List<String> deleteBackup(String backupId)
          throws IOException, ServiceDiscoveryException;
}
