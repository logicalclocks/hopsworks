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

import io.hops.hopsworks.common.dao.tensorflow.TfLibMapping;

import javax.ejb.Stateless;
import java.io.File;

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

  public String buildTfLdLibraryPath(TfLibMapping tfLibMapping) {

    // Add cuDnn dependency
    String ldPathBuilder = CUDNN_BASE_PATH + tfLibMapping.getCudnnVersion() + "/lib64" + File.pathSeparator;

    // Add cuda version
    ldPathBuilder += CUDA_BASE_PATH + tfLibMapping.getCudaVersion() + "/lib64" + File.pathSeparator;

    // Add nccl version
    ldPathBuilder += NCCL_BASE_PATH + tfLibMapping.getNcclVersion() + "/lib" + File.pathSeparator;

    return ldPathBuilder;
  }

  public String buildCudaHome(TfLibMapping tfLibMapping) {
    return CUDA_BASE_PATH + tfLibMapping.getCudaVersion() + File.pathSeparator;
  }
}
