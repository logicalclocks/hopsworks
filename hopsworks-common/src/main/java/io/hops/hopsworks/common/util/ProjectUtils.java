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

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ProjectUtils {
  
  @EJB
  private Settings settings;
  
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;

  private final static Logger LOGGER = Logger.getLogger(ProjectUtils.class.getName());
  
  public boolean isReservedProjectName(String projectName) {
    for (String name : settings.getReservedProjectNames()) {
      if (name.equalsIgnoreCase(projectName)) {
        return true;
      }
    }
    return false;
  }

  public boolean isOldDockerImage(String dockerImage) throws ProjectException {
    Pattern versionPattern = Pattern.compile("(\\d+[.]\\d+[.]\\d+)");

    //Extract the version number as NUMBER.NUMBER.NUMBER and ignore the -SNAPSHOT part
    Matcher projectDockerImageMatcher = versionPattern.matcher(dockerImage);
    String hopsworksVersion = settings.getHopsworksVersion();
    Matcher installationVersionMatcher = versionPattern.matcher(hopsworksVersion);

    if(!projectDockerImageMatcher.find()) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_DOCKER_VERSION_EXTRACT_ERROR,
          Level.SEVERE, "dockerImage: " + dockerImage + " version: " + hopsworksVersion);
    }

    if(!installationVersionMatcher.find()) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_DOCKER_VERSION_EXTRACT_ERROR,
          Level.SEVERE, "dockerImage: " + dockerImage + " version: " + hopsworksVersion);
    }

    String[] projectDockerImageParts = projectDockerImageMatcher.group().split("\\.");
    String[] installationDockerImageParts = installationVersionMatcher.group().split("\\.");
    for(int i = 0; i < installationDockerImageParts.length; i++) {
      if(Integer.parseInt(installationDockerImageParts[i]) > Integer.parseInt(projectDockerImageParts[i])) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if given docker image follows the format of base:2.1.0 or base:2.1.0-SNAPSHOT.
   * This check does not validate that the hopsworks version of the tag is the same as the installed hopsworks version
   * as projects may use images with older versions.
   * @param image
   * @return
   */
  private static boolean isBaseDockerImage(String image) {
    Pattern basePattern = Pattern.compile("^(base:\\d+[.]\\d+[.]\\d+(|-SNAPSHOT))$");
    Matcher baseMatcher = basePattern.matcher(image);
    return baseMatcher.matches();
  }

  /**
   * Checks if given docker image is a base image and follows the format of
   *
   * On-premise: python37:2.1.0
   * Cloud:      base:python37_2.1.0
   *
   * This check does not validate that the hopsworks version of the tag is the same as the installed hopsworks version
   * as projects may use images with older versions.
   * @param image
   * @return
   */
  private boolean isPythonDockerImage(String image) {
    Pattern pythonPattern;
    if(settings.isManagedDockerRegistry()) {
      pythonPattern = Pattern.compile("^(base:python\\d{2}_\\d+[.]\\d+[.]\\d+(|-SNAPSHOT))$");
      Matcher pythonMatcher = pythonPattern.matcher(image);
      return pythonMatcher.matches();
    } else {
      pythonPattern = Pattern.compile("^(python\\d{2}:\\d+[.]\\d+[.]\\d+(|-SNAPSHOT))$");
      Matcher pythonMatcher = pythonPattern.matcher(image);
      return pythonMatcher.matches();
    }
  }

  /**
   * Check if the image and tag is one of the preinstalled docker images
   * @param image
   * @return
   */
  public boolean dockerImageIsPreinstalled(String image) {
    return isBaseDockerImage(image) || isPythonDockerImage(image);
  }

  public String getFullDockerImageName(Project project, boolean useBase) throws ServiceDiscoveryException {
    return getFullDockerImageName(project, settings, serviceDiscoveryController, useBase);
  }
  
  public static String getFullDockerImageName(Project project, Settings settings,
      ServiceDiscoveryController serviceDiscoveryController, boolean useBase) throws ServiceDiscoveryException {
    String imageName = getDockerImageName(project, settings, useBase);
    return getRegistryURL(settings, serviceDiscoveryController) + "/" + imageName;
  }
  
  public static String getDockerImageName(Project project, Settings settings, boolean useBase) {
    if (project.getPythonEnvironment() != null && (useBase || Strings.isNullOrEmpty(project.getDockerImage()))) {
      // if conda enabled is true and usebase is true
      // or as a fall back in case the project image name hasn't been set (i.e. during upgrades)
      return settings.getBaseDockerImagePythonName();
    } else if(project.getPythonEnvironment() == null && isBaseDockerImage(project.getDockerImage())) {
      return project.getDockerImage();
    } else if(project.getPythonEnvironment() == null) {
      throw new IllegalArgumentException("Error. Python has not been enabled for this project.");
    } else {
      return project.getDockerImage();
    }
  }
  
  public String getFullDockerImageName(String imageName) throws ServiceDiscoveryException {
    return getRegistryURL(settings, serviceDiscoveryController) + "/" + imageName;
  }

  public String getRegistryURL() throws
      ServiceDiscoveryException {
    return getRegistryURL(settings, serviceDiscoveryController);
  }
  
  public static String getRegistryURL(Settings settings,
      ServiceDiscoveryController serviceDiscoveryController) throws
      ServiceDiscoveryException {
    com.logicalclocks.servicediscoverclient.service.Service registry = serviceDiscoveryController
        .getAnyAddressOfServiceWithDNSSRVOnly(ServiceDiscoveryController.HopsworksService.REGISTRY);
    if(settings.isManagedDockerRegistry()){
      String registryUrl = registry.getAddress();
      String dockerNamespace = settings.getDockerNamespace();
      if(!dockerNamespace.isEmpty()){
        registryUrl += "/" + dockerNamespace;
      }
      return registryUrl;
    }
    return registry.getName() + ":" + registry.getPort();
  }
  
  public String getInitialDockerImageName(Project project) {
    String initialImageTag =
        System.currentTimeMillis() + "-" + settings.getHopsworksVersion() +
            ".0";
    if (settings.isManagedDockerRegistry()) {
      return settings.getBaseNonPythonDockerImageWithNoTag() + ":" +
          project.getName().toLowerCase() + "_" + initialImageTag;
    } else {
      return project.getName().toLowerCase() + ":" + initialImageTag;
    }
  }
  
  public String getFullBaseImageName() throws ServiceDiscoveryException {
    return getRegistryURL(settings, serviceDiscoveryController) + "/" +
        settings.getBaseDockerImagePythonName();
  }
  
  public String getProjectNameFromDockerImageName(String imageName) {
    if (settings.isManagedDockerRegistry()) {
      return imageName.split(":")[1].split("_")[0];
    } else {
      return getProjectDockerRepoName(imageName);
    }
  }
  
  public String getProjectDockerRepoName(String imageName){
    return imageName.split(":")[0];
  }
}