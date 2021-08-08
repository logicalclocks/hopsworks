/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.code;

import io.hops.hopsworks.common.featurestore.code.FeaturestoreCodeFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupCommitController;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.code.FeaturestoreCode;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.lang.NotImplementedException;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.Timestamp;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CodeController {

  @EJB
  private Settings settings;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private InodeController inodeController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private JupyterController jupyterController;
  @EJB
  private FeaturestoreCodeFacade featurestoreCodeFacade;
  @EJB
  private FeatureGroupCommitController featureGroupCommitCommitController;

  private static final String CODE = "code";
  private static final String JAR = ".jar";
  private static final String IPYNB = ".ipynb";

  public String readCodeContent(Project project, Users user, String path, JupyterController.NotebookConversion format)
          throws FeaturestoreException {
    String extension = Utils.getExtension(path).get();
    //returns empty contents in the case of jar file
    if(extension.equals(JAR)){
      return "";
    }

    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, user);

    try {
      switch(format) {
        case HTML:
          return jupyterController.convertIPythonNotebook(hdfsUsername, path, project, "", format);
        default:
          //returns empty contents in the case of not supported notebookConversion
          return "";
      }
    } catch (ServiceException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.CODE_READ_ERROR,
              Level.WARNING, e.getMessage(), e.getMessage(), e);
    }
  }

  public FeaturestoreCode registerCode(Project project, Users user, Long codeCommitTimeStamp,
                                       Long fgCommitId, String applicationId, Featuregroup featuregroup,
                                       String kernelId, CodeActions.RunType type)
          throws FeaturestoreException, ServiceException {

    Inode codeInode = saveCode(project, user, applicationId, featuregroup, kernelId, type);

    Timestamp commitTime = new Timestamp(codeCommitTimeStamp);
    FeaturestoreCode featurestoreCode = new FeaturestoreCode(commitTime, codeInode, featuregroup, applicationId);
    if (featuregroup.getFeaturegroupType() == FeaturegroupType.CACHED_FEATURE_GROUP &&
            featuregroup.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI) {
      FeatureGroupCommit featureGroupCommit =
              featureGroupCommitCommitController.findCommitByDate(featuregroup, fgCommitId);
      featurestoreCode.setFeatureGroupCommit(featureGroupCommit);
    }

    return featurestoreCodeFacade.update(featurestoreCode);
  }

  public FeaturestoreCode registerCode(Project project, Users user, Long codeCommitTimeStamp,
                                       String applicationId, TrainingDataset trainingDataset,
                                       String kernelId, CodeActions.RunType type)
          throws ServiceException {

    Inode codeInode = saveCode(project, user, applicationId, trainingDataset, kernelId, type);

    Timestamp commitTime = new Timestamp(codeCommitTimeStamp);
    FeaturestoreCode featurestoreCode = new FeaturestoreCode(commitTime, codeInode, trainingDataset, applicationId);

    return featurestoreCodeFacade.update(featurestoreCode);
  }

  private Inode saveCode(Project project, Users user, String applicationId,
                         Featuregroup featureGroup, String kernelId,
                         CodeActions.RunType type)
          throws ServiceException {

    Path datasetDir = new Path(Utils.getFeaturestorePath(project, settings));
    String datasetName = Utils.getFeaturegroupName(featureGroup);

    return saveCode(project, user, applicationId, kernelId, datasetDir, datasetName, type);
  }

  private Inode saveCode(Project project, Users user, String applicationId,
                         TrainingDataset trainingDataset, String kernelId,
                         CodeActions.RunType type)
          throws ServiceException {

    Path datasetDir = new Path(Utils.getProjectPath(project.getName()),
            project.getName() + "_" + Settings.ServiceDataset.TRAININGDATASETS.getName());
    String datasetName = Utils.getTrainingDatasetName(trainingDataset);

    return saveCode(project, user, applicationId, kernelId, datasetDir, datasetName, type);
  }

  private Inode saveCode(Project project, Users user, String applicationId,
                         String kernelId, Path datasetDir, String datasetName,
                         CodeActions.RunType type)
          throws ServiceException {

    // Construct the directory path
    Path codeDir = new Path(datasetDir, CODE);
    Path dirPath = new Path(codeDir, datasetName);
    Path filePath = new Path(dirPath, applicationId + IPYNB);

    switch(type) {
      case JUPYTER: {
        jupyterController.versionProgram(project, user, kernelId, filePath);
      } break;
      default: {
        throw new NotImplementedException();
      }
    }

    return inodeController.getInodeAtPath(filePath.toString());
  }
}
