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

import com.google.common.annotations.VisibleForTesting;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.AccessControlException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.io.InputStream;
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

  public UploadController() {
  }

  @VisibleForTesting
  public UploadController(DistributedFsService dfs, ResumableInfoStorage storage) {
    this.dfs = dfs;
    this.storage = storage;
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
      LOGGER.log(Level.WARNING, "Failed to upload file: {0} {1}", new Object[]{flowInfo.getFilename(), e.getMessage()});
      throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_ERROR, Level.FINE,
        "Failed to upload. " + e.getMessage(), e.getMessage());
    }
  }

  /**
   * check if chunk is uploaded
   * @param flowInfo
   * @return
   */
  public boolean uploaded(FlowInfo flowInfo, String hdfsPath) {
    flowInfo.setFilePath(getTmpStagingDir(flowInfo.getFilename(), hdfsPath).toString());
    return storage.uploaded(flowInfo.hashCode(), flowInfo.getChunkNumber());
  }

  private void checkPermission(DistributedFileSystemOps udfso, FlowInfo flowInfo, String path, Path stagingDir)
      throws IOException, DatasetException {
    String fileName = flowInfo.getFilename();
    Path dest = new Path(path, fileName);
    if (flowInfo.getChunkNumber() == 1) {
      try {
        if (udfso.exists(dest)) {
          throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS, Level.FINE,
            "Destination already exists. path: " + dest);
        }
        //if chunk 1 creates placeholder but not staging dir other chunks will throw destination exist exception
        createStagingDirIfNotExist(udfso, stagingDir);
        udfso.touchz(dest);
      } catch (AccessControlException ex) {
        throw new AccessControlException("Permission denied: You can not upload to this folder. ");
      }
    }
  }

  //save chunk hdfs
  private void saveChunk(DistributedFileSystemOps dfsOps, InputStream uploadedInputStream, FlowInfo info)
      throws IOException {
    Path location = new Path(info.getFilePath(), String.valueOf(info.getChunkNumber()));
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
  private boolean markAsUploadedAndCheckFinished(DistributedFileSystemOps dfsOps, FlowInfo info)
      throws IOException, DatasetException {
    //Mark as uploaded and check if finished. Will remove the info if finished.
    if (storage.addChunkAndCheckIfFinished(info, info.getChunkNumber(), info.getCurrentChunkSize())) {
      //collect
      mergeChunks(info, dfsOps);
      return true;
    }
    return false;
  }

  private void mergeChunks(FlowInfo info, DistributedFileSystemOps dfsOps) throws IOException, DatasetException {
    Path location = new Path(info.getFilePath());
    if (dfsOps.exists(location) && dfsOps.getFileStatus(location).isDirectory()) {
      //Here we remove ".temp" to collect files in filename
      Path collected = fromTemp(location);
      FSDataOutputStream out = null;
      FSDataInputStream in = null;
      try {
        out = dfsOps.create(collected);
        for (int i = 1; i <= info.getTotalChunks(); i++) {
          try {
            Path chunk = new Path(info.getFilePath(), String.valueOf(i));
            if (!dfsOps.exists(chunk) || !dfsOps.getFileStatus(chunk).isFile()) {
              throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_ERROR, Level.SEVERE,
                "Could not find chunk: " + i);
            }
            in = dfsOps.open(chunk);
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
      Path stagingDir = getTmpStagingDir(hdfsPath, flowInfo.getFilename());
      //check permission and if file exists. Only the first chunk to fail early.
      checkPermission(dfsOps, flowInfo, hdfsPath, stagingDir);
      checkStagingDir(dfsOps, flowInfo, hdfsPath, stagingDir);
      flowInfo.setFilePath(stagingDir.toString());
      storage.put(flowInfo);
      finished = saveChunkAndCheckFinished(uploadedInputStream, dfsOps, flowInfo, hdfsPath);
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

  private boolean saveChunkAndCheckFinished(InputStream uploadedInputStream, DistributedFileSystemOps dfsOps,
      FlowInfo flowInfo, String hdfsPath) throws IOException, DatasetException {
    try {
      saveChunk(dfsOps, uploadedInputStream, flowInfo);
      return markAsUploadedAndCheckFinished(dfsOps, flowInfo);
    } catch (Exception e) {
      // if chunk number 1 we should clean up the placeholder to allow retry
      if (flowInfo.getChunkNumber() == 1) {
        LOGGER.log(Level.INFO, "Failed on chunk 1. Cleaning up temporary placeholder.");
        dfsOps.rm(new Path(hdfsPath, flowInfo.getFilename()), false);
      }
      throw e;
    }
  }

  private void checkStagingDir(DistributedFileSystemOps dfsOps, FlowInfo flowInfo, String hdfsPath, Path stagingDir)
      throws DatasetException {
    Path dest = new Path(hdfsPath, flowInfo.getFilename());
    try {
      //Chunk 1 will create dest as a placeholder. So do not let other chunks that might come before chunk 1
      // create the staging dir if the destination exists.
      if (!dfsOps.exists(dest)) {
        createStagingDirIfNotExist(dfsOps, stagingDir);
      } else if (!dfsOps.exists(stagingDir)) { //stagingDir should have be created before placeholder (dest) by chunk 1
        throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS, Level.FINE,
          "Destination already exists. path: " + dest);
      }

      if (dfsOps.exists(stagingDir) && !dfsOps.getFileStatus(stagingDir).isDirectory()) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS, Level.FINE,
          "Failed to create upload staging dir. path: " + stagingDir);
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to create upload staging dir. Path: {0}", stagingDir.toString());
      throw new DatasetException(RESTCodes.DatasetErrorCode.UPLOAD_ERROR, Level.SEVERE,
        "Failed to create upload staging dir", e.getMessage(), e);
    }
  }

  private void createStagingDirIfNotExist(DistributedFileSystemOps dfsOps, Path stagingDir) throws IOException {
    if (!dfsOps.exists(stagingDir)) {
      boolean created = dfsOps.mkdirs(stagingDir, dfsOps.getParentPermission(stagingDir));
      if (!created) {
        //another chunk may have created it
        LOGGER.log(Level.INFO, "Failed to create upload staging dir. Path: {0}", stagingDir.toString());
      }
    }
  }

  private Path getTmpStagingDir(String hdfsPath, String filename) {
    return toTemp(hdfsPath, filename);
  }

  private Path toTemp(String baseDir, String filename) {
    return new Path(baseDir, filename + ".temp");
  }

  private Path fromTemp(Path path) {
    return fromTemp(path.toString());
  }

  private Path fromTemp(String path) {
    int start = path.lastIndexOf(".temp");
    return new Path(path.substring(0, start));
  }
}
