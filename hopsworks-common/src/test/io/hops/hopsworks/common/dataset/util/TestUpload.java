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
package io.hops.hopsworks.common.dataset.util;

import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.FsPermissions;
import io.hops.hopsworks.common.upload.FlowInfo;
import io.hops.hopsworks.common.upload.ResumableInfoStorage;
import io.hops.hopsworks.common.upload.StagingManager;
import io.hops.hopsworks.common.upload.UploadController;
import io.hops.hopsworks.exceptions.DatasetException;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

public class TestUpload {
  private final static Logger LOGGER = Logger.getLogger(TestUpload.class.getName());
  private final static Level LEVEL = Level.FINE;
  private final static String HDFS_USERNAME = "hdfs_user";
  private final static int CHUNK_SIZE = 1024 * 2;
  private final Path testUploadPath = new Path(getFileFromResources("upload").getPath());
  private final Path srcPath = new Path(testUploadPath, "src");
  private final Path destPath = new Path(testUploadPath, "dest");
  private final Path srcFilePath = new Path(testUploadPath, "src/random_file.txt");
  private final Path destFilePath = new Path(testUploadPath, "dest/random_file.txt");
  private final Path stagingDir = new Path(testUploadPath, "dest/random_file.txt.temp");
  private final Path destExistingFilePath = new Path(testUploadPath, "dest/random.txt");
  private final DistributedFsService dfs = mock(DistributedFsService.class);
  private final DistributedFileSystemOps distributedFileSystemOps = mock(DistributedFileSystemOps.class);
  private final StagingManager stagingManager = mock(StagingManager.class);

  private UploadController uploadController;

  @Before
  public void setUp() throws IOException {
    Mockito.doAnswer((Answer<String>) invocation -> {
      LOGGER.log(LEVEL, "Get StagingPath: {0}.", "");
      return "";
    }).when(stagingManager).getStagingPath();

    ResumableInfoStorage storage = new ResumableInfoStorage();
    storage.initialize();
    setUpDistributedFileSystemOps();
    setUpDistributedFsService();
    uploadController = new UploadController(stagingManager, dfs, storage);
  }

  @After
  public void cleanUpFiles() throws IOException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(HDFS_USERNAME);
    FileStatus[] fileStatuses = udfso.listStatus(destPath);
    for (FileStatus fileStatus : fileStatuses) {
      if (!fileStatus.getPath().equals(destExistingFilePath)) {
        udfso.rm(fileStatus.getPath(), !fileStatus.isFile());
      }
    }
  }

  @Test
  public void test_resources() throws IOException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(HDFS_USERNAME);
    Assert.assertTrue(udfso.exists(testUploadPath));
    Assert.assertTrue(udfso.getFileStatus(testUploadPath).isDirectory());

    Assert.assertTrue(udfso.exists(srcPath));
    Assert.assertTrue(udfso.getFileStatus(destPath).isDirectory());

    Assert.assertTrue(udfso.exists(srcFilePath));
    Assert.assertTrue(udfso.getFileStatus(destExistingFilePath).isFile());
  }

  @Test
  public void testUploadCreatePlaceholder() throws IOException, DatasetException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(HDFS_USERNAME);
    File file = getFileFromResources(srcFilePath);
    int chunkNumber = 1;
    try (InputStream is = readChunk(file, chunkNumber)) {
      FlowInfo flowInfo = getFlowInfo(file, chunkNumber, is.available());
      LOGGER.log(LEVEL, "Flowinfo: {0}", flowInfo);
      uploadController.upload(is, flowInfo, destPath.toString(), HDFS_USERNAME);

      Assert.assertTrue(udfso.getFileStatus(destFilePath).isFile());
    }
  }

  @Test
  public void testUploadFileExist() throws IOException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(HDFS_USERNAME);
    File file = getFileFromResources(srcFilePath);
    int chunkNumber = 1;
    try (InputStream is = readChunk(file, chunkNumber)) {
      FlowInfo flowInfo = getFlowInfo(file, chunkNumber, is.available());
      flowInfo.setFilename("random.txt");
      LOGGER.log(LEVEL, "Flowinfo: {0}", flowInfo);
      DatasetException exception = assertThrows(DatasetException.class, () -> uploadController.upload(is,
        flowInfo, destPath.toString(), HDFS_USERNAME));
      Assert.assertEquals("Destination already exists. path: " + destExistingFilePath, exception.getUsrMsg());

      Assert.assertFalse(udfso.exists(new Path(testUploadPath, "dest/random.txt.temp")));
    }
  }

  @Test
  public void testStagingDirExist() throws IOException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(HDFS_USERNAME);
    File file = getFileFromResources(srcFilePath);
    int chunkNumber = 1;
    try (InputStream is = readChunk(file, chunkNumber)) {
      FlowInfo flowInfo = getFlowInfo(file, chunkNumber, is.available());
      LOGGER.log(LEVEL, "Flowinfo: {0}", flowInfo);
      udfso.create(stagingDir).close();
      DatasetException exception = assertThrows(DatasetException.class, () -> uploadController.upload(is,
        flowInfo, destPath.toString(), HDFS_USERNAME));
      Assert.assertEquals("Failed to create upload staging dir. path: " + stagingDir, exception.getUsrMsg());

      Assert.assertTrue(udfso.getFileStatus(stagingDir).isFile());
    }
  }

  @Test
  public void testUploadFileExistChunk2() throws IOException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(HDFS_USERNAME);
    File file = getFileFromResources(srcFilePath);
    int chunkNumber = 2;
    try (InputStream is = readChunk(file, chunkNumber)) {
      FlowInfo flowInfo = getFlowInfo(file, chunkNumber, is.available());
      flowInfo.setFilename("random.txt");
      LOGGER.log(LEVEL, "Flowinfo: {0}", flowInfo);
      DatasetException exception = assertThrows(DatasetException.class, () -> uploadController.upload(is,
        flowInfo, destPath.toString(), HDFS_USERNAME));
      Assert.assertEquals("Destination already exists. path: " + new Path(testUploadPath, "dest/random.txt"),
        exception.getUsrMsg());

      Assert.assertFalse(udfso.exists(new Path(testUploadPath, "dest/random.txt.temp")));
    }
  }

  @Test
  public void testDeletePlaceholder() throws IOException, DatasetException {
    Mockito.doThrow(new IOException("Failed for test.")).when(distributedFileSystemOps).create((Path) Mockito.any());

    DistributedFileSystemOps udfso = dfs.getDfsOps(HDFS_USERNAME);
    File file = getFileFromResources(srcFilePath);
    int chunkNumber = 1;
    try (InputStream is = readChunk(file, chunkNumber)) {
      FlowInfo flowInfo = getFlowInfo(file, chunkNumber, is.available());
      LOGGER.log(LEVEL, "Flowinfo: {0}", flowInfo);

      assertThrows(DatasetException.class,
        () -> uploadController.upload(is, flowInfo, destPath.toString(), HDFS_USERNAME));
      Assert.assertFalse(udfso.exists(destFilePath));

      //Retry chunk 1
      Mockito.doAnswer((Answer<FSDataOutputStream>) invocation -> {
        Object[] args = invocation.getArguments();
        LOGGER.log(LEVEL, "Create file: {0}.", args[0]);
        OutputStream outputStream = Files.newOutputStream(getFileFromResources((Path) args[0]).toPath());
        return new FSDataOutputStream(outputStream, new FileSystem.Statistics(""));
      }).when(distributedFileSystemOps).create((Path) Mockito.any());

      uploadController.upload(is, flowInfo, destPath.toString(), HDFS_USERNAME);
      Assert.assertTrue(udfso.getFileStatus(destFilePath).isFile());
    }
  }

  @Test
  public void testUploadChunkNotFile() throws IOException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(HDFS_USERNAME);
    udfso.mkdirs(new Path(stagingDir, "1"), FsPermissions.rwxrwx___);
    File file = getFileFromResources(srcFilePath);
    int chunkNumber = 1;
    try (InputStream is = readChunk(file, chunkNumber)) {
      FlowInfo flowInfo = getFlowInfo(file, chunkNumber, is.available());
      LOGGER.log(LEVEL, "Flowinfo: {0}", flowInfo);

      DatasetException exception = assertThrows(DatasetException.class, () -> uploadController.upload(is,
        flowInfo, destPath.toString(), HDFS_USERNAME));
      Assert.assertEquals("Failed to upload. " + new Path(stagingDir, "1") + ": Is a directory", exception.getUsrMsg());
    }
  }

  @Test
  public void testUpload() throws IOException, DatasetException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(HDFS_USERNAME);
    File file = getFileFromResources(srcFilePath);
    long totalChunks = getTotalChunks(file.length());

    for (int i = 1; i <= totalChunks; i++) {
      try (InputStream is = readChunk(file, i)) {
        FlowInfo flowInfo = getFlowInfo(file, i, is.available());
        LOGGER.log(LEVEL, "Flowinfo: {0}", flowInfo);
        uploadController.upload(is, flowInfo, destPath.toString(), HDFS_USERNAME);
      }
    }
    File dest = getFileFromResources(new Path(destPath, file.getName()));
    String srcContent = readFile(file.getPath(), Charset.defaultCharset());
    String destContent = readFile(dest.getPath(), Charset.defaultCharset());
    Assert.assertEquals(srcContent, destContent);
    Assert.assertFalse(udfso.exists(stagingDir));
  }

  private int getTotalChunks(long length) {
    return (int) Math.ceil((double) length / CHUNK_SIZE);
  }

  private String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }

  private InputStream readChunk(File file, long chunkNumber) throws IOException {
    byte[] chunk;
    try(FileInputStream is = new FileInputStream(file)) {
      try (FileChannel position = is.getChannel().position(CHUNK_SIZE * (chunkNumber - 1))) {
        chunk = new byte[Math.min(is.available(), CHUNK_SIZE)];
        position.read(ByteBuffer.wrap(chunk));
      }
    }
    return new ByteArrayInputStream(chunk);
  }
  private FlowInfo getFlowInfo(File file, int chunkNumber, int currentChunkSize) {
    String relativePath = file.getPath();
    String identifier = file.length() + "-" + file.getName().replace("/[^0-9a-zA-Z_-]/img", "");
    long totalChunks =  getTotalChunks(file.length());
    return new FlowInfo(String.valueOf(chunkNumber), String.valueOf(CHUNK_SIZE), String.valueOf(currentChunkSize),
      file.getName(), identifier, relativePath, String.valueOf(totalChunks), String.valueOf(file.length()));
  }

  private void setUpDistributedFileSystemOps() throws IOException {

    Mockito.doAnswer((Answer<Boolean>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(LEVEL, "Make dir: {0}.", args[0]);
      File f = getFileFromResources((Path) args[0]);
      return f.mkdirs();
    }).when(distributedFileSystemOps).mkdirs((Path) Mockito.any(), Mockito.any());

    Mockito.doAnswer((Answer<Boolean>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(LEVEL, "Check if file exists: {0}.", args);
      File f = getFileFromResources((Path) args[0]);
      return f.exists();
    }).when(distributedFileSystemOps).exists((Path) Mockito.any());

    Mockito.doAnswer((Answer<Boolean>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(LEVEL, "Check if file exists: {0}.", args);
      File f = getFileFromResources((String) args[0]);
      return f.exists();
    }).when(distributedFileSystemOps).exists((String) Mockito.any());

    Mockito.doAnswer((Answer<FSDataOutputStream>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(LEVEL, "Create file: {0}.", args[0]);
      OutputStream outputStream = Files.newOutputStream(getFileFromResources((Path) args[0]).toPath());
      return new FSDataOutputStream(outputStream, new FileSystem.Statistics(""));
    }).when(distributedFileSystemOps).create((Path) Mockito.any());

    Mockito.doAnswer((Answer<Void>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(LEVEL, "touchz file: {0}.", args[0]);
      PrintWriter pw = new PrintWriter(getFileFromResources((Path) args[0]));
      pw.close();
      return null;
    }).when(distributedFileSystemOps).touchz((Path) Mockito.any());

    Mockito.doAnswer((Answer<FSDataInputStream>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(LEVEL, "Open file: {0}.", args[0]);
      return toFSDataInputStream(getFileFromResources((Path) args[0]));
    }).when(distributedFileSystemOps).open((Path) Mockito.any());


    Mockito.doAnswer((Answer<Boolean>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(LEVEL, "Delete dir: {0}.", args[0]);
      File f = getFileFromResources((Path) args[0]);
      FileUtils.deleteDirectory(f);
      return true;
    }).when(distributedFileSystemOps).rm((Path) Mockito.any(), eq(true));

    Mockito.doAnswer((Answer<Boolean>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(LEVEL, "Delete dir: {0}.", args[0]);
      File f = getFileFromResources((Path) args[0]);
      return f.delete();
    }).when(distributedFileSystemOps).rm((Path) Mockito.any(), eq(false));

    Mockito.doAnswer((Answer<FileStatus>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(LEVEL, "Get FileStatus: {0}.", args[0]);
      return getFileStatus((Path) args[0]);
    }).when(distributedFileSystemOps).getFileStatus(Mockito.any());

    Mockito.doAnswer((Answer<FileStatus[]>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(LEVEL, "List Status: {0}.", args[0]);
      File file = getFileFromResources((Path) args[0]);
      File[] children = file.listFiles();
      List<FileStatus> fileStatuses = new ArrayList<>();
      if (children != null) {
        for (File f : children) {
          fileStatuses.add(getFileStatus(f));
        }
        return fileStatuses.toArray(new FileStatus[0]);
      }
      return new FileStatus[0];
    }).when(distributedFileSystemOps).listStatus(Mockito.any());

  }

  private FileStatus getFileStatus(Path path) {
    return getFileStatus(path.toString());
  }

  private FileStatus getFileStatus(String path) {
    File f = getFileFromResources(path);
    return getFileStatus(f);
  }

  private FileStatus getFileStatus(File f) {
    return new FileStatus(f.length(), f.isDirectory(), 0, 1024, f.lastModified(), new Path(f.getPath()));
  }

  private void setUpDistributedFsService() {
    Mockito.doAnswer((Answer<DistributedFileSystemOps>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(LEVEL, "Get user FileSystem operation: {0}.", args[0]);
      return distributedFileSystemOps;
    }).when(dfs).getDfsOps((String) Mockito.any());
    Mockito.doAnswer((Answer<Void>) invocation -> {
      Object[] args = invocation.getArguments();
      LOGGER.log(LEVEL, "Close FileSystem client: {0}.", args[0]);
      return null;
    }).when(dfs).closeDfsClient(Mockito.any());

  }

  private File getFileFromResources(Path path) {
    return getFileFromResources(path.toString());
  }

  private File getFileFromResources(String fileName) {
    String path = "/" + fileName;
    URL fileUrl = TestUpload.class.getResource(path);
    if (fileUrl == null) {
      return new File(path);
    }
    return new File(fileUrl.getFile());
  }

  private FSDataInputStream toFSDataInputStream(File file) throws IOException {
    SeekableByteArrayInputStream inputStream = new SeekableByteArrayInputStream(Files.readAllBytes(file.toPath()));
    return new FSDataInputStream(inputStream);
  }
}
