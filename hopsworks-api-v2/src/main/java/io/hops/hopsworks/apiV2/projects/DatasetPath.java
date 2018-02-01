/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.common.dao.dataset.Dataset;

/**
 * This class is returned from the PathValidator which parses the PATHs
 * received from the DatasetService.java
 * It contains information related to the dataset involved in the REST call,
 * the full path of the file or directory involved
 * and the dsRelativePath, which is the path of the file/directory,
 * relative to the dataset path
 */
class DatasetPath {
  
  private final Dataset dataSet;
  private final String relativePath;
  
  DatasetPath(Dataset dataSet, String relativePath ){
    if (dataSet == null||
        relativePath == null){
      throw new RuntimeException("All parameters must be non-null");
    }
    this.dataSet = dataSet;
    this.relativePath = relativePath;
  }
  
  public Dataset getDataSet() {
    return dataSet;
  }
  
  public String getRelativePath() {
    return relativePath;
  }
}
