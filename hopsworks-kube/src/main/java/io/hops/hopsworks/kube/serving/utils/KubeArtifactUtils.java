/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.utils;

import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeArtifactUtils {
  
  public final static String ARTIFACT_DIR = "Artifacts";
  private final static String ARTIFACT_FILE_EXTENSION = ".zip";
  
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private InodeController inodeController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private KubeModelUtils kubeModelUtils;
  @EJB
  private KubePredictorUtils kubePredictorUtils;
  @EJB
  private KubeTransformerUtils kubeTransformerUtils;
  
  public boolean createArtifact(Project project, Users user, Serving serving) throws DatasetException,
    HopsSecurityException, ServiceException, IOException {
    
    String username = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(username);
    
    Path artifactRootDirPath;
    Path artifactVersionDirPath;
    
    try {
      // Build folder paths
      artifactRootDirPath = datasetHelper.getDatasetPath(project, getArtifactRootDirPath(serving),
        DatasetType.DATASET).getFullPath();
      artifactVersionDirPath = new Path(getArtifactDirPath(serving, artifactRootDirPath.toUri().getRawPath()));
      
      // Create folders if they don't exist
      if (udfso.exists(artifactVersionDirPath)) {
        return false;
      }
      datasetController.createSubDirectory(project, artifactVersionDirPath, udfso);
    } finally {
      dfs.closeDfsClient(udfso);
    }
    
    // Copy assets into artifact version folder
    kubeModelUtils.copyModelFilesToArtifactDir(project, user, serving);
    if (serving.getPredictor() != null) {
      kubePredictorUtils.copyPredictorToArtifactDir(project, user, serving);
    }
    if (serving.getTransformer() != null) {
      kubeTransformerUtils.copyTransformerToArtifactDir(project, user, serving);
    }
    
    // Create new artifact
    Path artifactFilePath = new Path(getArtifactFilePath(serving, artifactVersionDirPath.toUri().getRawPath()));
    datasetController.zip(project, user, artifactVersionDirPath, artifactFilePath);
    
    return true; // artifact created
  }
  
  public Boolean checkArtifactDirExists(Serving serving)
    throws UnsupportedEncodingException {
    String hdfsPath = Utils.prepPath(getArtifactDirPath(serving));
    return inodeController.existsPath(hdfsPath);
  }
  
  public Boolean checkArtifactFileExists(Serving serving) {
    String hdfsPath = getArtifactFilePath(serving);
    return inodeController.existsPath(hdfsPath);
  }
  
  public String getArtifactDirPath(Serving serving) {
    return getArtifactDirPath(serving, getArtifactRootDirPath(serving));
  }
  
  public String getArtifactDirPath(Serving serving, String artifactRootDirPath) {
    // Format: /Project/<project>/Models/<model>/<version>/artifacts/<version>
    return artifactRootDirPath + "/" + serving.getArtifactVersion();
  }
  
  public String getArtifactFilePath(Serving serving) {
    return getArtifactFilePath(serving, getArtifactDirPath(serving));
  }
  
  public String getArtifactFilePath(Serving serving, String artifactRootDirPath) {
    // Format: /Project/<project>/Models/<model>/<version>/artifacts/<version>/<model>_<version>_<version>.zip
    return artifactRootDirPath + "/" + getArtifactFileName(serving);
  }
  
  public String getArtifactFileName(Serving serving) {
    // Format: <model_name>_<version>_<version>.zip
    return serving.getModelName() + "_" + serving.getModelVersion() + "_" + serving.getArtifactVersion()
      + ARTIFACT_FILE_EXTENSION;
  }
  
  public void createArtifactsRootDir(Project project, Users user, Serving serving)
    throws DatasetException, HopsSecurityException {
    String username = hdfsUsersController.getHdfsUserName(project, user);
    DistributedFileSystemOps udfso = dfs.getDfsOps(username);
    
    try {
      DatasetPath artifactRootDirPath = datasetHelper.getDatasetPath(project,
        getArtifactRootDirPath(serving), DatasetType.DATASET);
      datasetController.createSubDirectory(project, artifactRootDirPath.getFullPath(), udfso);
    } catch (DatasetException e) {
      if (e.getErrorCode() != RESTCodes.DatasetErrorCode.DATASET_SUBDIR_ALREADY_EXISTS) {
        throw e;
      }
    } finally {
      dfs.closeDfsClient(udfso);
    }
  }
  
  public List<Integer> getArtifactVersions(Serving serving) {
    ArrayList<Integer> versions = new ArrayList<>();
    for (Inode inode : dfs.getChildInodes(getArtifactRootDirPath(serving))) {
      if (inode.isDir()) {
        try {
          Integer version = Integer.parseInt(inode.getInodePK().getName());
          if (version > 0) {
            versions.add(version);
          }
        } catch (NumberFormatException e) { /* ignore non-parsable dir names */ }
      }
    }
    return versions;
  }
  
  public Boolean isSharedArtifact(Serving serving) {
    return serving.getPredictor() == null && serving.getTransformer() == null;
  }
  
  public Integer getNextArtifactVersion(Serving serving) {
    List<Integer> versions = getArtifactVersions(serving);
    return versions.isEmpty() ? 1 : Collections.max(versions) + 1;
  }
  
  public String getArtifactRootDirPath(Serving serving) {
    return getArtifactRootDirPath(serving.getModelVersionPath());
  }
  
  public String getArtifactRootDirPath(String modelVersionDirPath) {
    // Format: /Project/<project>/Models/<model>/<version>/artifacts
    return modelVersionDirPath + "/" + ARTIFACT_DIR;
  }
}

