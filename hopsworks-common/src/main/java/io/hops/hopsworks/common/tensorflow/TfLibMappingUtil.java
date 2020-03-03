/*
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
 */

package io.hops.hopsworks.common.tensorflow;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.dao.python.CondaCommands;
import io.hops.hopsworks.common.dao.tensorflow.TfLibMapping;
import io.hops.hopsworks.common.dao.tensorflow.TfLibMappingFacade;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.util.Settings;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This code is here as it should be used both for PySpark apps running TensorFlow in both Jupyter and Jobs to set the
 * correct LD_LIBRARY_PATH for loading .so files required by different TF versions
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TfLibMappingUtil {

  private static final String LIB_PATH = "/usr/local";
  private static final String CUDA_BASE_PATH = LIB_PATH + "/cuda-";
  private static final String CUDNN_BASE_PATH = LIB_PATH + "/cudnn-";
  private static final String NCCL_BASE_PATH = LIB_PATH + "/nccl";
  private static final String ROCM_RCCL_PATH = "/site-packages/tensorflow" +
      "/include/external/local_config_rocm/rocm/rocm/lib";
  @EJB
  private TfLibMappingFacade tfLibMappingFacade;
  @EJB
  private Settings settings;
  @EJB
  private EnvironmentController environmentController;

  private String buildTfLdLibraryPath(TfLibMapping tfLibMapping, Project project) {

    StringBuilder ldPathBuilder = new StringBuilder();

    //.so library for CUDNN
    if(!Strings.isNullOrEmpty(tfLibMapping.getCudnnVersion())) {
      ldPathBuilder.append(CUDNN_BASE_PATH + tfLibMapping.getCudnnVersion() + "/lib64" + File.pathSeparator);
    }

    //.so libraries for CUDA
    if(!Strings.isNullOrEmpty(tfLibMapping.getCudaVersion())) {
      ldPathBuilder.append(CUDA_BASE_PATH + tfLibMapping.getCudaVersion() + "/lib64" + File.pathSeparator);
      ldPathBuilder.append(CUDA_BASE_PATH + tfLibMapping.getCudaVersion() + "/extras/CUPTI/lib64" + File.pathSeparator);
    }

    //.so library for NCCL
    if(!Strings.isNullOrEmpty(tfLibMapping.getNcclVersion())) {
      ldPathBuilder.append(NCCL_BASE_PATH + tfLibMapping.getNcclVersion() + "/lib" + File.pathSeparator);
    }

    //.so library for RCCL
    ldPathBuilder.append(settings.getAnacondaProjectDir(project) + "/lib/python" +
        project.getPythonVersion() + ROCM_RCCL_PATH
        + File.pathSeparator);

    return ldPathBuilder.toString();
  }

  public String getTfLdLibraryPath(Project project) {
    // Get information about which version of TensorFlow the user is running
    TfLibMapping tfLibMapping = findTfMappingForProject(project);
    // Not supported TF version
    if (tfLibMapping == null) {
      return "";
    }
    return buildTfLdLibraryPath(tfLibMapping, project);
  }

  public TfLibMapping findTfMappingForProject(Project project) {

    if (!project.getCondaEnv()) {
      return tfLibMappingFacade.findByTfVersion(settings.getTensorflowVersion());
    }

    CondaCommands command = environmentController.getOngoingEnvCreation(project);

    if(command == null) {
      return project.getPythonDepCollection().stream()
              .filter(dep -> dep.getDependency().equals("tensorflow")
                  || dep.getDependency().equals("tensorflow-rocm"))
              .findAny()
              .map(tfDep -> tfLibMappingFacade.findByTfVersion(tfDep.getVersion()))
              .orElse(null);
    } else if(command.getOp().compareTo(CondaCommandFacade.CondaOp.CREATE) == 0) {
      return tfLibMappingFacade.findByTfVersion(settings.getTensorflowVersion());
    } else if(command.getOp().compareTo(CondaCommandFacade.CondaOp.YML) == 0) {
      String envYml = command.getEnvironmentYml();

      Pattern tfPattern = Pattern.compile("(tensorflow==\\d*.\\d*.\\d*)");
      Matcher tfMatcher = tfPattern.matcher(envYml);

      if(tfMatcher.find()) {
        String [] libVersionPair = tfMatcher.group(0).split("==");
        return tfLibMappingFacade.findByTfVersion(libVersionPair[1]);
      }

      Pattern tfRocmPattern = Pattern.compile("(tensorflow-rocm==\\d*.\\d*.\\d*)");
      Matcher tfRocmMatcher = tfRocmPattern.matcher(envYml);

      if(tfRocmMatcher.find()) {
        String [] libVersionPair = tfRocmMatcher.group(0).split("==");
        return tfLibMappingFacade.findByTfVersion(libVersionPair[1]);
      }
    }
    return null;
  }
}
