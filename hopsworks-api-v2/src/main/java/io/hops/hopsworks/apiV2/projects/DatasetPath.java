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
