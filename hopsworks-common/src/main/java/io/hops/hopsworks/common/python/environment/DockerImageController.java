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
package io.hops.hopsworks.common.python.environment;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.python.library.LibraryInstaller;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.multiregion.MultiRegionController;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DockerImageController {
  private static final Logger LOG = Logger.getLogger(LibraryInstaller.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private MultiRegionController multiRegionController;
  
  @VisibleForTesting
  public DockerImageController(Settings settings,
                               ProjectUtils projectUtils,
                               OSProcessExecutor osProcessExecutor) {
    this.settings = settings;
    this.projectUtils = projectUtils;
    this.osProcessExecutor = osProcessExecutor;
  }
  
  public DockerImageController() {
  }
  
  public void buildImage(String dockerImageName,
                         String dockerFilePath,
                         File cwd)
    throws ServiceDiscoveryException, ServiceException {
    LOG.log(Level.FINEST, "project-dockerImage:" + dockerImageName);
  
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(settings.getSudoersDir() + "/dockerImage.sh")
      .addCommand("create")
      .addCommand(dockerFilePath)
      .addCommand(projectUtils.getRegistryURL() + "/" + dockerImageName)
      .addCommand(multiRegionController.isEnabled() ?
            (projectUtils.getRegistryURL(multiRegionController.getSecondaryRegionName()) + "/" + dockerImageName) : "")
      .redirectErrorStream(true)
      .setCurrentWorkingDirectory(cwd)
      .setWaitTimeout(1, TimeUnit.HOURS)
      .build();
  
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
          + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
        throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_IMAGE_CREATION_ERROR, Level.INFO, errorMsg);
      }
    } catch(IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.WARNING,
        "Could not initiate docker operation", "Error executing OSProcessExecutor-dockerImage.sh", e);
    }
  }

  public void buildImage(String dockerImageName,
                         String dockerFilePath,
                         File cwd,
                         ArrayList<String> dockerBuildOpts) throws ServiceException, ServiceDiscoveryException {
    buildImage(dockerImageName, dockerFilePath, cwd, dockerBuildOpts, null, null);
  }
  
  public void buildImage(String dockerImageName,
                         String dockerFilePath,
                         File cwd,
                         ArrayList<String> dockerBuildOpts,
                         String gitApiKeyName,
                         String gitApiToken)
    throws ServiceDiscoveryException, ServiceException {
    LOG.log(Level.FINEST, "project-nextDockerImageName:" + dockerImageName);
  
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(settings.getSudoersDir() + "/dockerImage.sh")
      .addCommand("create")
      .addCommand(dockerFilePath)
      .addCommand(projectUtils.getRegistryURL() + "/" + dockerImageName)
      .addCommand(multiRegionController.isEnabled() ?
          (projectUtils.getRegistryURL(multiRegionController.getSecondaryRegionName()) + "/" + dockerImageName) : "")
      .addCommand("'" + String.join(" ", dockerBuildOpts) + "'")
      .redirectErrorStream(true)
      .setCurrentWorkingDirectory(cwd)
      .setWaitTimeout(1, TimeUnit.HOURS)
      .build();
  
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
  
      if (processResult.getExitCode() != 0) {
        //Avoid leeking the apitoken in the error logs by replacing it with the name
        if (!Strings.isNullOrEmpty(gitApiToken)) {
          String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode();
          String stdout = processResult.getStdout();
          if (stdout != null) {
            errorMsg = errorMsg + " out: " + stdout.replaceAll(gitApiToken, gitApiKeyName + "_token");
          }
          String stderr = processResult.getStderr();
          if (stderr != null) {
            errorMsg = errorMsg + "\n err: " + stderr.replaceAll(gitApiToken, gitApiKeyName + "_token");
          }
          errorMsg = errorMsg + "||\n";
          throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_IMAGE_CREATION_ERROR, Level.INFO, errorMsg);
        } else {
          String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
            + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
          throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_IMAGE_CREATION_ERROR, Level.INFO, errorMsg);
        }
      }
    } catch(IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.WARNING,
        "Could not initiate docker operation", "Error executing OSProcessExecutor-dockerImage.sh", e);
    }
  }
  
  public String listLibraries(String dockerImage) throws ServiceException {
    String prog = settings.getSudoersDir() + "/dockerImage.sh";
  
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(prog)
      .addCommand("list")
      .addCommand(dockerImage)
      .redirectErrorStream(true)
      .setWaitTimeout(30, TimeUnit.MINUTES)
      .build();
  
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not list libraries in the docker image. " +
          "Retry the command or recreate the environment" +
          "\n Exit code: " + processResult.getExitCode() +
          "\nout: " + processResult.getStdout() +
          "\nerr: " + processResult.getStderr() + "||\n";
        throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.INFO, errorMsg);
      }
      return processResult.getStdout();
    } catch(IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.WARNING,
        "Could not initiate docker operation", "Error executing OSProcessExecutor-dockerImage.sh", e);
    }
  }
  
  public String exportImage(String dockerImage) throws ServiceException {
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(settings.getSudoersDir() + "/dockerImage.sh")
      .addCommand("export")
      .addCommand(dockerImage)
      .redirectErrorStream(true)
      .setWaitTimeout(30, TimeUnit.MINUTES)
      .build();
  
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not create the docker image. Exit code: " + processResult.getExitCode()
          + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr() + "||\n";
        throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.INFO, errorMsg);
      }
      return processResult.getStdout();
    } catch(IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.WARNING,
        "Could not initiate docker operation", "Error executing OSProcessExecutor-dockerImage.sh", e);
    }
  }
  
  public String checkImage(String dockerImage) throws PythonException, ServiceException {
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(settings.getSudoersDir() + "/dockerImage.sh")
      .addCommand("check")
      .addCommand(dockerImage)
      .redirectErrorStream(true)
      .setWaitTimeout(300L, TimeUnit.SECONDS)
      .build();
    
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
//From https://github.com/pypa/pip/blob/27d8687144bf38cdaeeb1d81aa72c892b1d0ab88/src/pip/_internal/commands/check.py#L35
      if (processResult.getExitCode() == 0) {
        return "";
      } else if (processResult.getStdout() != null &&
        (processResult.getStdout().contains("which is not installed")
          || processResult.getStdout().contains("has requirement"))) {
        return processResult.getStdout();
      } else {
        throw new PythonException(RESTCodes.PythonErrorCode.ANACONDA_PIP_CHECK_FAILED,
          Level.INFO, "Failed to run pip check: "
          + (Strings.isNullOrEmpty(processResult.getStdout()) ? "" : processResult.getStdout()));
      }
    } catch(IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.WARNING,
        "Could not initiate docker operation", "Error executing OSProcessExecutor-dockerImage.sh", e);
    }
  }
  
  /**
   *
   * @param dockerImage (no tags)
   * @return
   */
  public void deleteImage(String dockerImage) throws ServiceException {
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(settings.getSudoersDir() + "/dockerImage.sh")
      .addCommand("delete")
      .addCommand(dockerImage)
      .redirectErrorStream(true)
      .setWaitTimeout(1, TimeUnit.MINUTES)
      .build();
  
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not delete the docker image. Exit code: " +
          processResult.getExitCode() + " out: " + processResult.getStdout();
        throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.INFO, errorMsg);
      }
    } catch(IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.WARNING,
        "Could not initiate docker operation", "Error executing OSProcessExecutor-dockerImage.sh", e);
    }
  }
  
  public void gcImages() throws ServiceException {
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(settings.getSudoersDir() + "/dockerImage.sh")
      .addCommand("gc")
      .redirectErrorStream(true)
      .setWaitTimeout(5, TimeUnit.MINUTES)
      .build();
  
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not delete the docker image. Exit code: " + processResult.getExitCode()
          + " out: " + processResult.getStdout() + "\n err: " + processResult.getStderr();
        throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.INFO, errorMsg);
      }
    } catch(IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.WARNING,
        "Could not initiate docker operation", "Error executing OSProcessExecutor-dockerImage.sh", e);
    }
  }
  
  public List<String> deleteACR(String repositoryName, String imageTagPrefix)
    throws ServiceDiscoveryException, ServiceException {
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(settings.getSudoersDir() + "/dockerImage.sh")
      .addCommand("delete-acr")
      .addCommand(projectUtils.getRegistryAddress())
      .addCommand(repositoryName)
      .addCommand(imageTagPrefix)
      .redirectErrorStream(true)
      .setWaitTimeout(1, TimeUnit.MINUTES)
      .build();
  
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Could not delete docker images in " + repositoryName + " under prefix " + imageTagPrefix +
          ". Exit code: " + processResult.getExitCode() + " out: " + processResult.getStdout();
        throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.INFO, errorMsg);
      }
      return Arrays.asList(processResult.getStdout().split("\n"));
    } catch(IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.WARNING,
        "Could not initiate docker operation", "Error executing OSProcessExecutor-dockerImage.sh", e);
    }
  }
  
  public List<String> listTagsACR(String repositoryName, String filter)
    throws ServiceDiscoveryException, ServiceException {
    String prog = settings.getSudoersDir() + "/dockerImage.sh";
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(prog)
      .addCommand("list-tags-acr")
      .addCommand(projectUtils.getRegistryAddress())
      .addCommand(repositoryName)
      .addCommand(filter)
      .redirectErrorStream(true)
      .setWaitTimeout(1, TimeUnit.MINUTES)
      .build();
  
    try {
      ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
      if (processResult.getExitCode() != 0) {
        String errorMsg = "Failed to get the images tags from the repository";
        throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.INFO, errorMsg);
      }
      return Arrays.asList(processResult.getStdout().split("\n"));
    } catch(IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.WARNING,
        "Could not initiate docker operation", "Error executing OSProcessExecutor-dockerImage.sh", e);
    }
  }
  
  public Future<ProcessResult> tag(String baseImage, String targetImage)
    throws ServiceDiscoveryException, ServiceException {
    ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(settings.getSudoersDir() + "/dockerImage.sh")
      .addCommand("tag")
      .addCommand(projectUtils.getRegistryURL() + "/" + baseImage)
      .addCommand(projectUtils.getRegistryURL() + "/" + targetImage)
      .redirectErrorStream(true)
      .setWaitTimeout(10, TimeUnit.MINUTES)
      .build();
    
    try {
      return osProcessExecutor.submit(processDescriptor);
    } catch(IOException e) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.DOCKER_ERROR, Level.WARNING,
        "Could not initiate docker operation", "Error executing OSProcessExecutor-dockerImage.sh", e);
    }
  }
}
