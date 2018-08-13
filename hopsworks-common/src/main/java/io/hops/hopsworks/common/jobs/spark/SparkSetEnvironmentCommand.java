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

package io.hops.hopsworks.common.jobs.spark;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.util.ConverterUtils;
import io.hops.hopsworks.common.jobs.yarn.YarnRunner;
import io.hops.hopsworks.common.jobs.yarn.YarnSetupCommand;

/**
 * Extra command to be executed before submitting a Spark job to Yarn. It sets
 * environment variables pointing out the local resources so that Spark
 * executors can access them. It may actually be possible to do this by setting
 * just environment variables (in SparkController.java). However, moving it here
 * keeps that code a bit cleaner.
 */
public final class SparkSetEnvironmentCommand extends YarnSetupCommand {

  private static final Logger logger = Logger.getLogger(
          SparkSetEnvironmentCommand.class.toString());

  @Override
  public void execute(YarnRunner r) {
    ApplicationSubmissionContext appContext = r.getAppContext();
    ContainerLaunchContext launchContext = appContext.getAMContainerSpec();
    Map<String, String> env = launchContext.getEnvironment();
    Map<String, LocalResource> rescs = launchContext.getLocalResources();

    StringBuilder fileUris = new StringBuilder();
    StringBuilder fileTimestamps = new StringBuilder();
    StringBuilder fileSizes = new StringBuilder();
    StringBuilder fileVisibilities = new StringBuilder();
    StringBuilder fileTypes = new StringBuilder();

    StringBuilder archiveUris = new StringBuilder();
    StringBuilder archiveTimestamps = new StringBuilder();
    StringBuilder archiveSizes = new StringBuilder();
    StringBuilder archiveVisibilities = new StringBuilder();

    for (Entry<String, LocalResource> resource : rescs.entrySet()) {
      try {
        Path destPath = ConverterUtils.getPathFromYarnURL(resource.getValue().
                getResource());
        URI sparkUri = destPath.toUri();
        URI pathURI = new URI(sparkUri.getScheme(), sparkUri.getAuthority(),
                sparkUri.getPath(), null, resource.getKey());
//        if (resource.getValue().getType() == LocalResourceType.FILE) {
        fileUris.append(pathURI.toString()).append(",");
        fileTimestamps.append(resource.getValue().getTimestamp()).append(",");
        fileSizes.append(resource.getValue().getSize()).append(",");
        fileVisibilities.append(resource.getValue().getVisibility()).append(",");
        fileTypes.append(resource.getValue().getType().toString()).append(",");
//        } else {
//          archiveUris.append(pathURI.toString()).append(",");
//          archiveTimestamps.append(resource.getValue().getTimestamp()).append(
//                  ",");
//          archiveSizes.append(resource.getValue().getSize()).append(",");
//          archiveVisibilities.append(resource.getValue().getVisibility()).
//                  append(",");
//        }
      } catch (URISyntaxException e) {
        logger.log(Level.SEVERE, "Failed to add LocalResource " + resource
                + " to Spark Environment.", e);
      }
    }

//    if (fileUris.length() > 1) {
    env.put("CACHED_FILES", fileUris.substring(0, fileUris.length()
            - 1));
    env.put("CACHED_FILES_TIMESTAMPS", fileTimestamps.substring(0,
            fileTimestamps.length() - 1));
    env.put("CACHED_FILES_SIZES", fileSizes.substring(0,
            fileSizes.length() - 1));
    env.put("CACHED_FILES_VISIBILITIES", fileVisibilities.substring(
            0, fileVisibilities.length() - 1));
    env.put("CACHED_FILES_TYPES", fileTypes.substring(
            0, fileTypes.length() - 1));
//    }
//    if (archiveUris.length() > 1) {
//      env.put("SPARK_YARN_CACHE_ARCHIVES", archiveUris.substring(0, archiveUris.
//              length()
//              - 1));
//      env.put("SPARK_YARN_CACHE_ARCHIVES_TIME_STAMPS", archiveTimestamps.
//              substring(0,
//                      archiveTimestamps.length() - 1));
//      env.put("SPARK_YARN_CACHE_ARCHIVES_FILE_SIZES", archiveSizes.substring(0,
//              archiveSizes.length() - 1));
//      env.put("SPARK_YARN_CACHE_ARCHIVES_VISIBILITIES", archiveVisibilities.
//              substring(0,
//                      archiveVisibilities.length() - 1));
//    }
    ContainerLaunchContext amContainer = ContainerLaunchContext.newInstance(
            launchContext.getLocalResources(), env, launchContext.getCommands(),
            null, null, null);
    appContext.setAMContainerSpec(amContainer);
  }

}
