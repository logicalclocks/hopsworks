/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.dataset.util;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWith;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.fs.Path;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class DatasetPath {
  private final Path fullPath;
  private final Path datasetFullPath;
  private final Path relativePath;
  private boolean isShared;
  private Dataset dataset;
  private DatasetSharedWith datasetSharedWith;
  private Inode inode;
  
  public DatasetPath(Project project, String path, String root) throws UnsupportedEncodingException {
    String p = Utils.prepPath(path);
    String pathStr = stripSlash(p);
    String pathRoot = stripSlash(root); // Projects or /apps/hive/warehouse
    boolean isProject = root.equals(Settings.DIR_ROOT);
    boolean isFullPath = pathStr.startsWith(pathRoot + File.separator);
    isShared = pathStr.contains(Settings.SHARED_FILE_SEPARATOR);
    if (isFullPath && isShared) {
      //make it relative
      pathStr = pathStr.replace(pathRoot + File.separator, "");
    }
    String dataset = getDatasetFromPath(pathStr, pathRoot, project.getName(), isProject);
    String datasetName =
      !dataset.isEmpty() && dataset.contains(File.separator) ? dataset.split(File.separator)[0] : dataset;
    String projectName = isProject ? File.separator + project.getName() + File.separator : File.separator;
    if (pathStr.startsWith(pathRoot + File.separator) && !datasetName.contains(Settings.SHARED_FILE_SEPARATOR)) {
      // fullpath
      fullPath = new Path(File.separator + pathStr);
    } else if (!datasetName.contains(Settings.SHARED_FILE_SEPARATOR)) {
      // relative
      fullPath = new Path(File.separator + pathRoot + projectName + pathStr);
    } else {
      // shared
      String[] datasetNameParts = datasetName.split(Settings.SHARED_FILE_SEPARATOR);
      projectName = isProject ? File.separator + datasetNameParts[0] + File.separator : File.separator;
      if (projectName.replaceAll(File.separator, "").equals(project.getName())) {
        isShared = false; // if contains :: but the project name in path is the same as project remove ::
        dataset = dataset.replace(datasetName, datasetNameParts[1]);
      }
      fullPath = new Path(File.separator + pathRoot + projectName + dataset.replace(datasetName, datasetNameParts[1]));
    }
    relativePath = new Path(File.separator + dataset);
    datasetFullPath = getDatasetPath(fullPath.toString(), new Path(pathRoot + projectName));
    
  }
  
  public Path getFullPath() {
    return fullPath;
  }
  
  public Path getDatasetFullPath() {
    return datasetFullPath;
  }
  
  public Path getRelativePath() {
    return relativePath;
  }
  
  public Dataset getDataset() {
    return dataset;
  }
  
  public void setDataset(Dataset dataset) {
    this.dataset = dataset;
  }
  
  public DatasetSharedWith getDatasetSharedWith() {
    return datasetSharedWith;
  }
  
  public void setDatasetSharedWith(DatasetSharedWith datasetSharedWith) {
    this.datasetSharedWith = datasetSharedWith;
  }
  
  public Inode getInode() {
    return inode;
  }
  
  public void setInode(Inode inode) {
    this.inode = inode;
  }
  
  public boolean isTopLevelDataset() {
    return fullPath.depth() == datasetFullPath.depth();
  }
  
  public String getDatasetRelativePath() {
    return stripSlash(fullPath.toString().replace(datasetFullPath.toString(), ""));
  }
  
  public String getDatasetName() {
    return datasetFullPath.getName();
  }
  
  public boolean isShared() {
    return isShared;
  }
  
  private String getDatasetFromPath(String path, String root, String projectName, boolean isProject) {
    String dataset = path;
    if (path.startsWith(root)) {
      dataset = path.replace(root + File.separator, "");
      if (isProject && dataset.startsWith(projectName)) {
        dataset = dataset.replace(projectName + File.separator, "");
      } else if (isProject) {
        dataset = dataset.substring(dataset.indexOf(File.separator) + 1);
      }
    }
    return dataset;
  }
  
  private Path getDatasetPath(String fullPath, Path root) {
    if (fullPath.startsWith(File.separator)) {
      fullPath = fullPath.substring(1);
    }
    String[] parts = fullPath.split(File.separator);
    String dsPath = String.join(File.separator, Arrays.copyOfRange(parts, 0, root.depth() + 1));
    return new Path(File.separator + dsPath);
  }
  
  private String stripSlash(String path) {
    while (path.startsWith(File.separator)) {
      path = path.substring(1);
    }
    while (path.endsWith(File.separator)) {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }
  
}
