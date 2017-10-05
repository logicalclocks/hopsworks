package io.hops.hopsworks.dela;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.old_dto.FileInfo;
import io.hops.hopsworks.dela.old_dto.ManifestJSON;
import io.hops.hopsworks.dela.util.DatasetHelper;
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
  private InodeFacade inodeFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private DistributedFsService dfs;

  public long datasetSize(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    return length(project, user, DatasetHelper.getDatasetPath(project, dataset));
  }

  public ManifestJSON readManifest(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    String datasetPath = DatasetHelper.getOwningDatasetPath(dataset, inodeFacade, projectFacade);
    String manifestPath = datasetPath + File.separator + Settings.MANIFEST_FILE;
    byte[] manifestBytes = read(project, user, manifestPath);
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
    String manifestPath = DatasetHelper.getDatasetPath(project, dataset) + File.separator + Settings.MANIFEST_FILE;
    delete(project, user, manifestPath);
    write(project, user, manifestPath, ManifestHelper.marshall(manifest));
    return manifest;
  }

  public void deleteManifest(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    String manifestPath = DatasetHelper.getDatasetPath(project, dataset) + File.separator + Settings.MANIFEST_FILE;
    delete(project, user, manifestPath);
  }

  public String getReadme(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "dela:hdfs:readme");
    String readmePath = DatasetHelper.getDatasetPath(project, dataset) + File.separator + Settings.README_FILE;
    String result = new String(read(project, user, readmePath));
    LOG.log(Settings.DELA_DEBUG, "dela:hdfs:readme:done");
    return result;
  }
  
  public FilePreviewDTO getPublicReadme(Dataset dataset) throws ThirdPartyException {
    LOG.log(Settings.DELA_DEBUG, "dela:hdfs:readme");
    Project ownerProject = DatasetHelper.getOwningProject(dataset, inodeFacade, projectFacade);
    String readmePath = DatasetHelper.getDatasetPath(ownerProject, dataset) + File.separator + Settings.README_FILE;
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    FilePreviewDTO result = new FilePreviewDTO("text", "md", new String(read(dfso, readmePath)));
    LOG.log(Settings.DELA_DEBUG, "dela:hdfs:readme");
    return result;
  }

  private ManifestJSON createManifest(Project project, Dataset dataset, Users user) throws ThirdPartyException {
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    DistributedFileSystemOps dfso = dfs.getDfsOps(hdfsUser);
    String datasetPath = DatasetHelper.getDatasetPath(project, dataset);

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
      String filePath = datasetPath + File.separator + fileName;
      fileInfo.setLength(dfso.getlength(filePath));
      if (avroFiles.containsKey(fileName + ".avro")) {
        fileInfo.setSchema(new String(read(project, user, filePath + ".avro")));
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

  public void write(Project project, Users user, String filePath, byte[] fileContent) throws ThirdPartyException {

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

  public byte[] read(Project project, Users user, String filePath) throws ThirdPartyException {
    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    DistributedFileSystemOps dfso = dfs.getDfsOps(hdfsUser);
    byte[] result = read(dfso, filePath);
    return result;
  }
  
  public byte[] read(DistributedFileSystemOps dfso, String filePath) throws ThirdPartyException {
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
      fdi = dfso.open(new Path(filePath));
      long fileLength = dfso.getlength(filePath);
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

  public void delete(Project project, Users user, String filePath) throws ThirdPartyException {

    String hdfsUser = hdfsUsersBean.getHdfsUserName(project, user);
    DistributedFileSystemOps dfso = dfs.getDfsOps(hdfsUser);
    try {
      if (!dfso.exists(filePath)) {
        return;
      }
      dfso.rm(new Path(filePath), true);
    } catch (IOException ex) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "cannot delete",
        ThirdPartyException.Source.HDFS, "access error");
    }
  }

  public long length(Project project, Users user, String filePath) throws ThirdPartyException {
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
      long fileLength = dfso.getDatasetSize(new Path(filePath));
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

  private long getLength(String pathToFile) {
    return dfs.getDfsOps().getlength(pathToFile);
  }
}
