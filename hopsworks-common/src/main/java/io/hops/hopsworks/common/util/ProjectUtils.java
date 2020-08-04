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

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ProjectUtils {
  
  @EJB
  private Settings settings;
  
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  
  public boolean isReservedProjectName(String projectName) {
    for (String name : settings.getReservedProjectNames()) {
      if (name.equalsIgnoreCase(projectName)) {
        return true;
      }
    }
    return false;
  }

  public String getFullDockerImageName(Project project, boolean useBase) throws ServiceDiscoveryException {
    return getFullDockerImageName(project, settings, serviceDiscoveryController, useBase);
  }
  
  public static String getFullDockerImageName(Project project, Settings settings,
      ServiceDiscoveryController serviceDiscoveryController, boolean useBase) throws ServiceDiscoveryException {
    String imageName = getDockerImageName(project, settings, useBase);
    return getRegistryURL(serviceDiscoveryController) + "/" + imageName;
  }
  
  public static String getDockerImageName(Project project, Settings settings, boolean useBase) {
    if(useBase && isCondaEnabled(project)) {
      return settings.getBaseDockerImage();
    } else {
      if(!isCondaEnabled(project)) {
        throw new IllegalArgumentException("Error. Python has not been enabled for this project.");
      } else {
        return project.getDockerImage();
      }
    }
  }
  
  public String getFullDockerImageName(String imageName) throws ServiceDiscoveryException {
    return getRegistryURL(serviceDiscoveryController) + "/" + imageName;
  }

  public String getRegistryURL() throws
      ServiceDiscoveryException {
    return getRegistryURL(serviceDiscoveryController);
  }
  
  public static String getRegistryURL(ServiceDiscoveryController serviceDiscoveryController) throws
      ServiceDiscoveryException {
    com.logicalclocks.servicediscoverclient.service.Service registry = serviceDiscoveryController
        .getAnyAddressOfServiceWithDNS(ServiceDiscoveryController.HopsworksService.REGISTRY);
    return registry.getName() + ":" + registry.getPort();
  }

  public static boolean isCondaEnabled(Project project) {
    return project.getConda();
  }

  public String getInitialDockerImageName(Project project) {
    return project.getName().toLowerCase() + ":" + settings.getHopsworksVersion() + ".0";
  }

  public String getFullBaseImageName() throws ServiceDiscoveryException {
    return getRegistryURL(serviceDiscoveryController) + "/" + settings.getBaseDockerImage();
  }
}