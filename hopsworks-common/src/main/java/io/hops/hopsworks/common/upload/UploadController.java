/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.common.upload;

import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.AccessControlException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class UploadController {
  private static final Logger LOGGER = Logger.getLogger(UploadController.class.getName());

  @EJB
  private DistributedFsService dfs;
  @EJB
  private ResumableInfoStorage storage;
  
  /**
   * Check if user has permission to write to path. And destination exists.
   * Check is done only if chunk number == 1.
   * @param flowInfo
   * @param path
   * @param username
   * @throws IOException
   * @throws DatasetException
   */
  public void checkPermission(FlowInfo flowInfo, String path, String username) throws IOException, DatasetException {
    if (username != null) {
      DistributedFileSystemOps udfso = null;
      try {
        udfso = dfs.getDfsOps(username);
        checkPermission(udfso, flowInfo, path);
      } finally {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  /**
   * Upload a chunk
   * @param uploadedInputStream
   * @param flowInfo
   * @param hdfsPath
   * @param username
   * @return
   * @throws DatasetException
   */
  public boolean upload(InputStream uploadedInputStream, FlowInfo flowInfo, String hdfsPath, String username)
    throws DatasetException, AccessControlException {
    LOGGER.log(Level.FINE, "Uploading:- chunk: {0}, id: {1}", new Object[]{flowInfo.getChunkNumber(),
      flowInfo.getIdentifier()});
    try {
      return hdfsStagingUpload(uploadedInputStream, flowInfo, hdfsPath, username);
    } catch (AccessControlException ex) {
      throw new AccessControlException("Permission denied: You can not upload to this folder. ");
    } catch (IOException e) {
      //cleanup is not easy b/c we do not know when the client is done sending chunks
      LOGGER.log(Level.WARNING, "Failed to upload file: {0}", flowInfo.getFilename());
      throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_ERROR, Level.FINE,
        "Failed to upload." + e.getMessage(), e.getMessage());
    }
  }

  /**
   * check if chunk is uploaded
   * @param flowInfo
   * @return
   */
  public boolean isUploaded(FlowInfo flowInfo, String hdfsPath) {
    flowInfo.setFilePath(getTmpStagingDir(flowInfo.getFilename(), hdfsPath).toString());
    return storage.isUploaded(flowInfo.hashCode(), flowInfo.getChunkNumber());
  }

  private void checkPermission(DistributedFileSystemOps udfso, FlowInfo flowInfo, String path) throws IOException,
    DatasetException {
    String fileName = flowInfo.getFilename();
    Path dest = new Path(path, fileName);
    if (flowInfo.getChunkNumber() == 1) {
      try {
        if (udfso.exists(dest)) {
          throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS, Level.FINE,
            "Destination already exists. path: " + dest);
        }
        udfso.touchz(dest);
        udfso.rm(dest, false);//remove it to create staging dir. Dir can not overwrite file.
      } catch (AccessControlException ex) {
        throw new AccessControlException("Permission denied: You can not upload to this folder. ");
      }
    }
  }

  //save chunk hdfs
  private void saveChunk(DistributedFileSystemOps dfsOps, InputStream uploadedInputStream, FlowInfo info,
    int chunkNumber) throws IOException {
    Path location = new Path(info.getFilePath(), String.valueOf(chunkNumber));
    FSDataOutputStream out = null;
    try {
      out = dfsOps.create(location);
      // copy returns int: -1 if size > 2147483647L. Chunk should not be > 2147483647L
      IOUtils.copy(uploadedInputStream, out);
    } finally {
      IOUtils.closeQuietly(uploadedInputStream);
      IOUtils.closeQuietly(out);
    }
  }

  //hdfs mark chunk as uploaded and check if all chunks are uploaded
  private boolean markAsUploadedAndCheckFinished(DistributedFileSystemOps dfsOps, FlowInfo info, int chunkNumber,
    long contentLength) throws IOException {
    //Mark as uploaded and check if finished. Will remove the info if finished.
    if (storage.addChunkAndCheckIfFinished(info, chunkNumber, contentLength)) {
      //collect
      Path location = new Path(info.getFilePath());
      if (dfsOps.exists(location) && dfsOps.getFileStatus(location).isDirectory()) {
        FileStatus[] fileStatuses = dfsOps.listStatus(location);
        if (fileStatuses != null && fileStatuses.length > 0) {
          if (fileStatuses.length > 1) {
            Arrays.sort(fileStatuses, Comparator.comparingInt(o -> Integer.parseInt(o.getPath().getName())));
          }
          //Here we remove ".temp" to collect files in filename
          Path collected = fromTemp(location);
          FSDataOutputStream out = null;
          FSDataInputStream in = null;
          try {
            out = dfsOps.create(collected);
            for (FileStatus fileStatus : fileStatuses) {
              try {
                in = dfsOps.open(fileStatus.getPath());
                IOUtils.copy(in, out);
              } finally {
                IOUtils.closeQuietly(in);
              }
            }
            dfsOps.rm(location, true);
          } finally {
            IOUtils.closeQuietly(out);
          }
        }
      }
      return true;
    }
    return false;
  }

  private void copyToHdfs(DistributedFileSystemOps dfsOps, FlowInfo info, String hdfsPath) throws IOException {
    try {
      Path location = new Path(hdfsPath, info.getFilename());
      Path stagingFilePath = fromTemp(info.getFilePath());
      if (!stagingFilePath.equals(location)) {
        dfsOps.moveWithinHdfs(stagingFilePath, location, true);
        LOGGER.log(Level.INFO, "Move within Hdfs. {0}", new Object[] {stagingFilePath, location});
      }
      dfsOps.setPermission(location, dfsOps.getParentPermission(location));
      LOGGER.log(Level.INFO, "Copied to HDFS. {0}", location);
    } catch (AccessControlException ex) {
      throw new AccessControlException("Permission denied: You can not upload to this folder. ");
    }
  }

  //upload using hdfs as staging
  private boolean hdfsStagingUpload(InputStream uploadedInputStream, FlowInfo flowInfo, String hdfsPath,
    String username) throws DatasetException, IOException {
    boolean finished;
    DistributedFileSystemOps dfsOps = null;
    try {
      dfsOps = dfs.getDfsOps(username);
      //check permission and if file exists. Only the first chunk to fail early.
      checkPermission(dfsOps, flowInfo, hdfsPath);
      String resumableFilePath = getTmpStagingDir(dfsOps, flowInfo.getFilename(), hdfsPath);
      flowInfo.setFilePath(resumableFilePath);
      storage.put(flowInfo);
      saveChunk(dfsOps, uploadedInputStream, flowInfo, flowInfo.getChunkNumber());
      finished =
        markAsUploadedAndCheckFinished(dfsOps, flowInfo, flowInfo.getChunkNumber(), flowInfo.getCurrentChunkSize());
      if (finished) {
        copyToHdfs(dfsOps, flowInfo, hdfsPath);
      }
    } finally {
      if (dfsOps != null) {
        dfs.closeDfsClient(dfsOps);
      }
    }
    return finished;
  }

  private String getTmpStagingDir(DistributedFileSystemOps dfsOps, String filename, String hdfsPath)
    throws DatasetException {
    Path userTmpDir = getTmpStagingDir(hdfsPath, filename);
    try {
      if (!dfsOps.exists(userTmpDir)) {
        boolean created = dfsOps.mkdirs(userTmpDir, dfsOps.getParentPermission(userTmpDir));
        if (!created) {
          LOGGER.log(Level.WARNING, "Failed to create upload staging dir. Path: {0}", userTmpDir.toString());
        }
      }
      // No need to test if chunk exists. Should just overwrite it.
      // We check if the file exists in permission check.
      return userTmpDir.toString();
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to create upload staging dir. Path: {0}", userTmpDir.toString());
      throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_ERROR, Level.SEVERE,
        "Failed to create upload staging dir", e.getMessage(), e);
    }
  }

  private Path getTmpStagingDir(String hdfsPath, String filename) {
    return toTemp(hdfsPath, filename);
  }

  private Path toTemp(String baseDir, String filename) {
    return new Path(baseDir, filename + ".temp");
  }

  private Path fromTemp(Path path) {
    return fromTemp(path.toString().replace(".temp", ""));
  }

  private Path fromTemp(String path) {
    return new Path(path.replace(".temp", ""));
  }
}
