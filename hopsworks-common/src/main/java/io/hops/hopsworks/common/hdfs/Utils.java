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

package io.hops.hopsworks.common.hdfs;

import com.google.common.base.CharMatcher;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.common.util.Settings;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;
import org.apache.hadoop.fs.Path;

public final class Utils {

  private static final Logger logger = Logger.getLogger(Utils.class.getName());

  public static String getFileName(String path) {
    int lastSlash = path.lastIndexOf("/");
    int startName = (lastSlash > -1) ? lastSlash + 1 : 0;
    return path.substring(startName);
  }

  public static Optional<String> getExtension(String filename) {
    return Optional.ofNullable(filename)
            .filter(f -> f.contains("."))
            .map(f -> f.substring(filename.lastIndexOf(".") + 1));
  }

  public static String getDirectoryPart(String path) {
    int lastSlash = path.lastIndexOf("/");
    int startName = (lastSlash > -1) ? lastSlash + 1 : 0;
    return path.substring(0, startName);
  }

  public static String getProjectPath(String projectName) {
    return "/" + Settings.DIR_ROOT + "/" + projectName + "/";
  }
  
  public static String getFileSystemDatasetPath(Dataset dataset, Settings settings) {
    if(DatasetType.FEATURESTORE.equals(dataset.getDsType())) {
      return getFeaturestorePath(dataset.getProject(), settings);
    } else if(DatasetType.HIVEDB.equals(dataset.getDsType())) {
      return getHiveDBPath(dataset.getProject(), settings);
    } else {
      return "/" + Settings.DIR_ROOT + "/" + dataset.getProject().getName() + "/" + dataset.getName() + "/";
    }
  }

  public static Path getDatasetPath(Dataset ds, Settings settings) {
    Path path = null;
    switch (ds.getDsType()) {
      case DATASET:
        path = new Path(Utils.getProjectPath(ds.getProject().getName()), ds.getName());
        break;
      case FEATURESTORE:
      case HIVEDB:
        path = new Path(settings.getHiveWarehouse(), ds.getName());
    }
    return path;
  }
  
  /**
   * Assemble the log output path of Hopsworks jobs.
   *
   * @param projectName projectName
   * @param jobType jobType
   * @return String array with out and err log output path
   */
  public static String[] getJobLogLocation(String projectName, JobType jobType) {
    String defaultOutputPath;
    switch (jobType) {
      case SPARK:
      case PYSPARK:
      case FLINK:
      case YARN:
      case PYTHON:
      case DOCKER:
        defaultOutputPath = Settings.BaseDataset.LOGS.getName() + "/" + jobType.getName() +"/";
        break;
      default:
        defaultOutputPath =  "Logs/";
    }
  
    String stdOutFinalDestination = getProjectPath(projectName) + defaultOutputPath;
    String stdErrFinalDestination = getProjectPath(projectName) + defaultOutputPath;
    return new String[]{stdOutFinalDestination, stdErrFinalDestination};
  }
  
  public static String getHiveDBName(Project project) {
    return project.getName().toLowerCase() + ".db";
  }
  
  public static String getHiveDBPath(Project project, Settings settings) {
    return settings.getHiveWarehouse() + "/" + getHiveDBName(project);
  }
  
  public static String getFeaturestoreName(Project project) {
    return project.getName().toLowerCase() + "_featurestore.db";
  }
  
  public static String getFeaturestorePath(Project project, Settings settings) {
    return settings.getHiveWarehouse() + "/" + getFeaturestoreName(project);
  }

  public static String getFeaturegroupName(Featuregroup featuregroup) {
    return getFeatureStoreEntityName(featuregroup.getName(), featuregroup.getVersion());
  }

  public static String getTrainingDatasetName(TrainingDataset trainingDataset) {
    return getFeatureStoreEntityName(trainingDataset.getName(), trainingDataset.getVersion());
  }

  public static String getFeatureViewName(FeatureView featureView) {
    return getFeatureStoreEntityName(featureView.getName(), featureView.getVersion());
  }
  
  public static String getFeatureStoreEntityName(String entityName, Integer version) {
    return entityName + "_" + version.toString();
  }

  /**
   * The root '/' is considered '0', so the answer is incorrect for root, but
   * that doesn't matter. '/blah.txt' should return '1'.
   *
   * @param path
   * @return
   */
  public static int pathLen(String path) {
    return CharMatcher.is('/').countIn(path);
  }

  /**
   * take a String corresponding to a file uri and return only the path part of the it.
   * And transform shared path into there full path.
   * @param path the string from which to extract the path
   * @return the path
   * @throws UnsupportedEncodingException 
   */
  public static String prepPath(String path) throws UnsupportedEncodingException {
    Path p;
    String result = path;
    if (path.contains(Settings.SHARED_FILE_SEPARATOR)) {
      String[] parts = path.split(Settings.SHARED_FILE_SEPARATOR);
      p = new Path(parts[0]);
      //remove hdfs://10.0.2.15:8020//
      p = new Path(URLDecoder.decode(p.toUri().getRawPath(), StandardCharsets.UTF_8.toString()));
      result = p.toString() + Settings.SHARED_FILE_SEPARATOR + parts[1];
    } else {
      p = new Path(path);
      //remove hdfs://10.0.2.15:8020//
      p = new Path(URLDecoder.decode(p.toUri().getRawPath(), StandardCharsets.UTF_8.toString()));
      result = p.toString();
    }
    return result;
  }
  
  public static String pathStripSlash(String path) {
    while (path.startsWith(File.separator)) {
      path = path.substring(1);
    }
    while (path.endsWith(File.separator)) {
      path = path.substring(0, path.length() - 1);
    }
    return path;
  }
}
