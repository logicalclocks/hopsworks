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

package io.hops.hopsworks.common.featurestore.utils;

import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import java.io.IOException;

@Stateless
public class FeaturestoreUtils {

  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DistributedFsService distributedFsService;

  /**
   * Writes a string to a new file in HDFS
   *
   * @param project              project of the user
   * @param user                 user making the request
   * @param filePath             path of the file we want to write
   * @param content              the content to write
   * @throws IOException
   */
  public void writeToHDFS(Project project, Users user, Path filePath, String content) throws IOException {
    DistributedFileSystemOps udfso = null;
    try {
      String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);
      udfso = distributedFsService.getDfsOps(hdfsUsername);
      try (FSDataOutputStream outStream = udfso.create(filePath)) {
        outStream.writeBytes(content);
        outStream.hflush();
      }
    } finally {
      if (udfso != null) {
        distributedFsService.closeDfsClient(udfso);
      }
    }
  }
}
