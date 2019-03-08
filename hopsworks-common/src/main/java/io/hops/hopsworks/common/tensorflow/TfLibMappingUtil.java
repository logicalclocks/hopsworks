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

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.tensorflow.TfLibMapping;
import io.hops.hopsworks.common.dao.tensorflow.TfLibMappingFacade;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.ServiceException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.util.logging.Level;

/**
 * Obs! This code is here as it should be used both for Jupyter and for running
 * notebooks as jobs
 */
@Stateless
public class TfLibMappingUtil {

  private static final String LIB_PATH = "/usr/local";
  private static final String CUDA_BASE_PATH = LIB_PATH + "/cuda-";
  private static final String CUDNN_BASE_PATH = LIB_PATH + "/cudnn-";
  private static final String NCCL_BASE_PATH = LIB_PATH + "/nccl";
  @EJB
  private TfLibMappingFacade tfLibMappingFacade;

  private String buildTfLdLibraryPath(TfLibMapping tfLibMapping) {

    // Add cuDnn dependency
    String ldPathBuilder = CUDNN_BASE_PATH + tfLibMapping.getCudnnVersion() + "/lib64" + File.pathSeparator;

    // Add cuda version
    ldPathBuilder += CUDA_BASE_PATH + tfLibMapping.getCudaVersion() + "/lib64" + File.pathSeparator;

    // Add nccl version
    ldPathBuilder += NCCL_BASE_PATH + tfLibMapping.getNcclVersion() + "/lib" + File.pathSeparator;

    return ldPathBuilder;
  }

  public String getTfLdLibraryPath(Project project) throws ServiceException {
    // Get information about which version of TensorFlow the user is running
    TfLibMapping tfLibMapping = tfLibMappingFacade.findTfMappingForProject(project);
    if (tfLibMapping == null) {
      // We are not supporting this version.
      throw new ServiceException(RESTCodes.ServiceErrorCode.TENSORFLOW_VERSION_NOT_SUPPORTED, Level.INFO);
    }
    return  buildTfLdLibraryPath(tfLibMapping);
  }
}
