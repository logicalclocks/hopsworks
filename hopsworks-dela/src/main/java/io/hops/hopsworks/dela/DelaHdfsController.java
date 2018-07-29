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
package io.hops.hopsworks.dela;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.old_dto.FileInfo;
import io.hops.hopsworks.dela.old_dto.ManifestJSON;
import io.hops.hopsworks.dela.util.ManifestHelper;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.Response;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DelaHdfsController {

  private Logger LOG = Logger.getLogger(DelaHdfsController.class.getName());

  @EJB
  private DatasetController datasetCtrl;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private DistributedFsService dfs;

  public long datasetSize(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    return length(project, user, datasetCtrl.getDatasetPath(dataset));
  }

  private Path manifestPath(Dataset dataset) {
    Path datasetPath = datasetCtrl.getDatasetPath(dataset);
    Path manifestPath = new Path(datasetPath, Settings.MANIFEST_FILE);
    return manifestPath;
  }

  public ManifestJSON readManifest(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    byte[] manifestBytes = read(project, user, manifestPath(dataset));
    ManifestJSON manifest = ManifestHelper.unmarshall(manifestBytes);
    return manifest;
  }

  public ManifestJSON writeManifest(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    if (inodeFacade.getChildren(dataset.getInode()).isEmpty()) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "dataset empty",
        ThirdPartyException.Source.LOCAL, "bad request");
    }
    LOG.log(Settings.DELA_DEBUG, "{0} - writing manifest", dataset.getPublicDsId());
    ManifestJSON manifest = createManifest(project, dataset, user);
    Path manifestPath = manifestPath(dataset);
    delete(project, user, manifestPath);
    write(project, user, manifestPath, ManifestHelper.marshall(manifest));
    return manifest;
  }

  public void deleteManifest(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    delete(project, user, manifestPath(dataset));
  }

  private Path readmePath(Dataset dataset) {
    Path datasetPath = datasetCtrl.getDatasetPath(dataset);
    Path readmePath = new Path(datasetPath, Settings.README_FILE);
    return readmePath;
  }
  
  public String getReadme(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "dela:hdfs:readme");
    String result = new String(read(project, user, readmePath(dataset)));
    LOG.log(Settings.DELA_DEBUG, "dela:hdfs:readme:done");
    return result;
  }

  public FilePreviewDTO getPublicReadme(Dataset dataset) throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "dela:hdfs:readme");
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    FilePreviewDTO result = new FilePreviewDTO("text", "md", new String(read(dfso, readmePath(dataset))));
    LOG.log(Settings.DELA_DEBUG, "dela:hdfs:readme");
    return result;
  }

  private ManifestJSON createManifest(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    DistributedFileSystemOps dfso = dfs.getDfsOps(hdfsUser);
    Path datasetPath = datasetCtrl.getDatasetPath(dataset);

    ManifestJSON manifest = new ManifestJSON();
    manifest.setDatasetName(dataset.getName());
    manifest.setDatasetDescription(dataset.getDescription());
    manifest.setKafkaSupport(false);

    List<Inode> datasetFiles = new LinkedList<>();
    Map<String, Inode> avroFiles = new HashMap<>();
    for (Inode i : inodeFacade.getChildren(dataset.getInode())) {
      if (i.isDir()) {
        throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "subdirectories not supported",
          ThirdPartyException.Source.LOCAL, "bad request");
      }
      if (isAvro(i.getInodePK().getName())) {
        avroFiles.put(i.getInodePK().getName(), i);
      } else {
        datasetFiles.add(i);
      }
    }

    List<FileInfo> fileInfos = new LinkedList<>();
    for (Inode i : datasetFiles) {
      String fileName = i.getInodePK().getName();
      FileInfo fileInfo = new FileInfo();
      fileInfo.setFileName(fileName);
      Path filePath = new Path(datasetPath, fileName);
      try {
        fileInfo.setLength(dfso.getLength(filePath));
      } catch (IOException ex) {
        throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "file:" + filePath.toString(),
          ThirdPartyException.Source.HDFS, "access error");
      }
      if (avroFiles.containsKey(fileName + ".avro")) {
        Path avroSchemaPath = new Path(datasetPath, filePath + ".avro");
        fileInfo.setSchema(new String(read(project, user, avroSchemaPath)));
        manifest.setKafkaSupport(true);
      } else {
        fileInfo.setSchema("");
      }
      fileInfos.add(fileInfo);
    }
    for (Inode i : avroFiles.values()) {
      String fileName = i.getInodePK().getName();
      FileInfo fileInfo = new FileInfo();
      fileInfo.setFileName(fileName);
      fileInfo.setSchema("");
      String filePath = datasetPath + File.separator + fileName;
      fileInfo.setLength(dfso.getlength(filePath));
      fileInfos.add(fileInfo);
    }
    manifest.setFileInfos(fileInfos);

    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    manifest.setCreatorDate(dateFormat.format(new Date()));
    manifest.setCreatorEmail(user.getEmail());

    //TODO other schemas
    manifest.setMetaDataJsons(new ArrayList<>());
    return manifest;
  }

  public void write(Project project, Users user, Path filePath, byte[] fileContent) throws ThirdPartyException {

    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    DistributedFileSystemOps dfso = dfs.getDfsOps(hdfsUser);
    try {
      if (dfso.exists(filePath)) {
        throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "file exists",
          ThirdPartyException.Source.HDFS, "access error");
      }
    } catch (IOException ex) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "cannot read",
        ThirdPartyException.Source.HDFS, "access error");
    }

    FSDataOutputStream out = null;
    try {
      out = dfso.create(filePath);
      out.write(fileContent);
      out.flush();
    } catch (IOException ex) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "cannot write",
        ThirdPartyException.Source.HDFS, "access error");
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException ex) {
          throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "cannot close",
            ThirdPartyException.Source.HDFS, "access error");
        }
      }
    }
  }

  public byte[] read(Project project, Users user, Path filePath) throws ThirdPartyException {
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    DistributedFileSystemOps dfso = dfs.getDfsOps(hdfsUser);
    byte[] result = read(dfso, filePath);
    return result;
  }

  public byte[] read(DistributedFileSystemOps dfso, Path filePath) throws ThirdPartyException {
    try {
      if (!dfso.exists(filePath)) {
        throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "file does not exist",
          ThirdPartyException.Source.HDFS, "access error");
      }
    } catch (IOException ex) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "cannot read",
        ThirdPartyException.Source.HDFS, "access error");
    }

    FSDataInputStream fdi = null;
    try {
      fdi = dfso.open(filePath);
      long fileLength = dfso.getLength(filePath);
      byte[] fileContent = new byte[(int) fileLength];
      fdi.readFully(fileContent);
      return fileContent;
    } catch (IOException ex) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "cannot read",
        ThirdPartyException.Source.HDFS, "access error");
    } finally {
      if (fdi != null) {
        try {
          fdi.close();
        } catch (IOException ex) {
          throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "cannot close",
            ThirdPartyException.Source.HDFS, "access error");
        }
      }
    }
  }

  public void delete(Project project, Users user, Path filePath) throws ThirdPartyException {

    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    DistributedFileSystemOps dfso = dfs.getDfsOps(hdfsUser);
    try {
      if (!dfso.exists(filePath)) {
        return;
      }
      dfso.rm(filePath, true);
    } catch (IOException ex) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "cannot delete",
        ThirdPartyException.Source.HDFS, "access error");
    }
  }

  public long length(Project project, Users user, Path filePath) throws ThirdPartyException {
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    DistributedFileSystemOps dfso = dfs.getDfsOps(hdfsUser);
    try {
      if (!dfso.exists(filePath)) {
        throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "dataset does not exist",
          ThirdPartyException.Source.HDFS, "access error");
      }
    } catch (IOException ex) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "cannot read",
        ThirdPartyException.Source.HDFS, "access error");
    }

    try {
      long fileLength = dfso.getDatasetSize(filePath);
      return fileLength;
    } catch (IOException ex) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "cannot read dataset",
        ThirdPartyException.Source.HDFS, "access error");
    }
  }

  private boolean isAvro(String s) {
    String remove_spaces = s.replaceAll(" ", "");
    String[] split = remove_spaces.split("\\.");
    if (split.length == 2) {
      return split[1].equals("avro");
    } else {
      return false;
    }
  }
}
