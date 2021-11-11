/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.kube.serving.utils;

import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.serving.ModelServer;
import io.hops.hopsworks.persistence.entity.serving.Serving;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KubeModelUtils {
  
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private KubeArtifactUtils kubeArtifactUtils;
  
  public void copyModelFilesToArtifactDir(Project project, Users user, Serving serving) throws DatasetException {
    // Build folder paths
    Path modelVersionDirFullPath = datasetHelper.getDatasetPath(project, getModelVersionDirPath(serving),
      DatasetType.DATASET).getFullPath();
    String modelVersionDirPath = modelVersionDirFullPath.toUri().getRawPath();
    String artifactRootDirPath = kubeArtifactUtils.getArtifactRootDirPath(serving, modelVersionDirPath);
    String artifactDirPath = kubeArtifactUtils.getArtifactDirPath(serving, artifactRootDirPath);
    
    // Iterate over model files
    List<Inode> inodes = dfs.getChildInodes(modelVersionDirPath);
    for (Inode inode : inodes) {
      String filename = inode.getInodePK().getName();
      if (filename.equals(KubeArtifactUtils.ARTIFACT_DIR)) {
        continue; // Skip artifacts folder
      }
      // Get source file path
      String sourceFilePathStr = modelVersionDirPath + "/" + filename;
      DatasetPath sourceFilePath = datasetHelper.getDatasetPath(project, sourceFilePathStr, DatasetType.DATASET);
      // Get destination file path
      String destFilePathStr = artifactDirPath + "/" + filename;
      DatasetPath destFilePath = datasetHelper.getDatasetPath(project, destFilePathStr, DatasetType.DATASET);
      // Copy file
      datasetController.copy(project, user, sourceFilePath.getFullPath(), destFilePath.getFullPath(),
        sourceFilePath.getDataset(), destFilePath.getDataset());
    }
  }
  
  public String getModelVersionDirPath(Serving serving) {
    // Format: /Project/<project>/Models/<model>/<version>
    String path = serving.getModelPath();
    if (serving.getModelServer() == ModelServer.FLASK) {
      // If model server is FLASK, the model path points to a python script
      path = path.substring(0, path.lastIndexOf("/"));
    } else if (serving.getModelServer() == ModelServer.TENSORFLOW_SERVING) {
      if (!path.endsWith("/")) {
        path += "/";
      }
      path += serving.getModelVersion();
    }
    
    return path;
  }
}
