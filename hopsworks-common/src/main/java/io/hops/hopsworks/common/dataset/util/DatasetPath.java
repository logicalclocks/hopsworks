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
package io.hops.hopsworks.common.dataset.util;


import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.apache.hadoop.fs.Path;

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
  
  private static final String FILE_SEPARATOR = "/";
  
  public DatasetPath(Path fullPath, Path datasetFullPath, Path relativePath) {
    this.fullPath = fullPath;
    this.datasetFullPath = datasetFullPath;
    this.relativePath = relativePath;
  }
  
  public DatasetPath(Project project, String path, String root) throws UnsupportedEncodingException {
    String p = Utils.prepPath(path);
    String pathStr = Utils.pathStripSlash(p);
    String pathRoot = Utils.pathStripSlash(root); // Projects or /apps/hive/warehouse
    boolean isProject = root.equals(Settings.DIR_ROOT);
    isShared = pathStr.contains(Settings.SHARED_FILE_SEPARATOR);
    String dataset = getDatasetFromPath(pathStr, pathRoot, project.getName(), isProject);
    String datasetName =
      !dataset.isEmpty() && dataset.contains(FILE_SEPARATOR) ? dataset.split(FILE_SEPARATOR)[0] : dataset;
    String projectName = isProject ? FILE_SEPARATOR + project.getName() + FILE_SEPARATOR : FILE_SEPARATOR;
    if (pathStr.startsWith(pathRoot + FILE_SEPARATOR) && !datasetName.contains(Settings.SHARED_FILE_SEPARATOR)) {
      // fullpath
      fullPath = new Path(FILE_SEPARATOR + pathStr);
    } else if (!datasetName.contains(Settings.SHARED_FILE_SEPARATOR)) {
      // relative
      fullPath = new Path(FILE_SEPARATOR + pathRoot + projectName + pathStr);
    } else {
      // shared
      String[] datasetNameParts = datasetName.split(Settings.SHARED_FILE_SEPARATOR);
      projectName = isProject ? FILE_SEPARATOR + datasetNameParts[0] + FILE_SEPARATOR : FILE_SEPARATOR;
      if (projectName.replaceAll(FILE_SEPARATOR, "").equals(project.getName())) {
        isShared = false; // if contains :: but the project name in path is the same as project remove ::
        dataset = dataset.replace(datasetName, datasetNameParts[1]);
      }
      fullPath = new Path(FILE_SEPARATOR + pathRoot + projectName + dataset.replace(datasetName, datasetNameParts[1]));
    }
    relativePath = new Path(FILE_SEPARATOR + dataset);
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
    this.isShared = true;
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
    return Utils.pathStripSlash(fullPath.toString().replace(datasetFullPath.toString(), ""));
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
      dataset = path.replace(root + FILE_SEPARATOR, "");
      if (isProject && dataset.startsWith(projectName)) {
        dataset = dataset.replace(projectName + FILE_SEPARATOR, "");
      } else if (isProject) {
        dataset = dataset.substring(dataset.indexOf(FILE_SEPARATOR) + 1);
      }
    }
    return dataset;
  }
  
  private Path getDatasetPath(String fullPath, Path root) {
    if (fullPath.startsWith(FILE_SEPARATOR)) {
      fullPath = fullPath.substring(1);
    }
    String[] parts = fullPath.split(FILE_SEPARATOR);
    String dsPath = String.join(FILE_SEPARATOR, Arrays.copyOfRange(parts, 0, root.depth() + 1));
    return new Path(FILE_SEPARATOR + dsPath);
  }
  
  public Project getAccessProject() {
    return isShared ? datasetSharedWith.getProject() : dataset.getProject();
  }
}
