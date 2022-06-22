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

package io.hops.hopsworks.common.featurestore.featureview;

import io.hops.hopsworks.common.dao.QueryParam;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreConnectorFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetFacade;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivityMeta;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.activity.ActivityFlag;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static io.hops.hopsworks.restutils.RESTCodes.FeaturestoreErrorCode.FEATURE_VIEW_NOT_FOUND;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureViewController {
  private static final String PATH_TO_FEATURE_VIEW = "%s" + Path.SEPARATOR + ".featureviews" + Path.SEPARATOR + "%s_%d";

  @EJB
  private FeatureViewFacade featureViewFacade;
  @EJB
  private InodeController inodeController;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private FeaturestoreConnectorFacade featurestoreConnectorFacade;
  @EJB
  private HopsFSProvenanceController fsProvenanceController;
  @EJB
  private FeaturestoreActivityFacade fsActivityFacade;
  @EJB
  private FeaturestoreUtils featurestoreUtils;
  @EJB
  private TrainingDatasetFacade trainingDatasetFacade;
  @EJB
  private ActivityFacade activityFacade;

  public FeatureView createFeatureView(Project project, Users user, FeatureView featureView, Featurestore featurestore)
      throws FeaturestoreException, ProvenanceException, IOException {
    // if version not provided, get latest and increment
    if (featureView.getVersion() == null) {
      // returns ordered list by desc version
      Integer latestVersion = featureViewFacade.findLatestVersion(featureView.getName(), featurestore);
      if (latestVersion != null) {
        featureView.setVersion(latestVersion + 1);
      } else {
        featureView.setVersion(1);
      }
    }

    // Check that feature view doesn't already exists
    if (!featureViewFacade
        .findByNameVersionAndFeaturestore(featureView.getName(), featureView.getVersion(), featurestore)
        .isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_VIEW_ALREADY_EXISTS, Level.FINE,
          "Feature view: " + featureView.getName() + ", version: " + featureView.getVersion());
    }

    // Since training dataset created by feature view shares the same name, need to make sure name of feature view
    // do not collide with existing training dataset created without feature view.
    List<TrainingDataset> trainingDatasets = trainingDatasetFacade
        .findByNameAndFeaturestoreExcludeFeatureView(featureView.getName(), featurestore);
    if (trainingDatasets != null && !trainingDatasets.isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_VIEW_ALREADY_EXISTS, Level.FINE,
          "Name of the feature view collides with an existing training dataset name : " + featureView.getName());
    }

    String connectorName =
        featurestore.getProject().getName() + "_" + Settings.ServiceDataset.TRAININGDATASETS.getName();
    FeaturestoreConnector featurestoreConnector =
        featurestoreConnectorFacade.findByFeaturestoreName(featurestore, connectorName)
          .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
            Level.FINE, "HOPSFS Connector: " + connectorName));

    Dataset datasetsFolder = featurestoreConnector.getHopsfsConnector().getHopsfsDataset();

    DistributedFileSystemOps udfso = null;
    String username = hdfsUsersBean.getHdfsUserName(project, user);
    try {
      Path path = new Path(String.format(PATH_TO_FEATURE_VIEW, inodeController.getPath(datasetsFolder.getInode()),
          featureView.getName(), featureView.getVersion()));

      udfso = dfs.getDfsOps(username);
      udfso.mkdirs(path, FsPermission.getDefault());

      Inode inode = inodeController.getInodeAtPath(path.toString());

      featureView.setInode(inode);
      featureView = featureViewFacade.update(featureView);

      // Log the metadata operation
      fsActivityFacade.logMetadataActivity(user, featureView, FeaturestoreActivityMeta.FV_CREATED);

      activityFacade.persistActivity(ActivityFacade.CREATED_FEATURE_VIEW + featureView.getName(), project, user,
          ActivityFlag.SERVICE);

      fsProvenanceController.featureViewAttachXAttr(path.toString(), featureView, udfso);
      return featureView;
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  public String getLocation(FeatureView featureView) {
    return inodeController.getPath(featureView.getInode());
  }

  public List<FeatureView> getAll() {
    return featureViewFacade.findAll();
  }

  public List<FeatureView> getByFeatureStore(Featurestore featurestore, QueryParam queryParam) {
    return featureViewFacade.findByFeaturestore(featurestore, queryParam);
  }

  public List<FeatureView> getByNameAndFeatureStore(String name, Featurestore featurestore, QueryParam queryParam)
      throws FeaturestoreException {
    List<FeatureView> featureViews =  featureViewFacade.findByNameAndFeaturestore(
        name, featurestore, queryParam);
    if (featureViews.isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_VIEW_NOT_FOUND,
          Level.FINE, String.format("There exists no feature view with the name %s.", name));
    }
    return featureViews;
  }

  public FeatureView getByNameVersionAndFeatureStore(String name, Integer version, Featurestore featurestore)
      throws FeaturestoreException {
    List<FeatureView> featureViews = featureViewFacade.findByNameVersionAndFeaturestore(name, version, featurestore);
    if (featureViews.isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_VIEW_NOT_FOUND,
          Level.FINE, String.format("There exists no feature view with the name %s and version %d.", name, version));
    }
    return featureViews.get(0);
  }

  public void delete(Users user, Project project, Featurestore featurestore, String name)
      throws FeaturestoreException {
    List<FeatureView> featureViews = featureViewFacade.findByNameAndFeaturestore(name, featurestore);
    delete(user, project, featurestore, featureViews);
  }

  public void delete(Users user, Project project, Featurestore featurestore, String name, Integer version)
      throws FeaturestoreException {
    List<FeatureView> featureViews = featureViewFacade.findByNameVersionAndFeaturestore(name, version, featurestore);
    delete(user, project, featurestore, featureViews);
  }

  private void delete(Users user, Project project, Featurestore featurestore, List<FeatureView> featureViews)
      throws FeaturestoreException {
    if (featureViews == null || featureViews.isEmpty()) {
      throw new FeaturestoreException(FEATURE_VIEW_NOT_FOUND, Level.FINE, "Provided feature view name or version " +
          "does not exist.");
    }
    for (FeatureView fv: featureViews) {
      featurestoreUtils.verifyUserRole(fv, featurestore, user, project);
    }
    for (FeatureView fv: featureViews) {
      featureViewFacade.remove(fv);
      activityFacade.persistActivity(ActivityFacade.DELETED_FEATURE_VIEW + fv.getName(),
          project, user, ActivityFlag.SERVICE);
    }
  }

  public FeatureView update(Users user, Project project, Featurestore featurestore, String name, Integer version,
                            String description)
      throws FeaturestoreException {
    FeatureView featureView = getByNameVersionAndFeatureStore(name, version, featurestore);

    featurestoreUtils.verifyUserRole(featureView, featurestore, user, project);

    // Update metadata
    featureView.setDescription(description);
    featureViewFacade.update(featureView);

    activityFacade.persistActivity(ActivityFacade.EDITED_FEATURE_VIEW + name, project, user, ActivityFlag.SERVICE);

    // Refetch the updated entry from the database
    return getByNameVersionAndFeatureStore(name, version, featurestore);
  }

  public List<TrainingDatasetFeature> getFeaturesSorted(Collection<TrainingDatasetFeature> features) {
    return features.stream()
        .sorted((t1, t2) -> {
          if (t1.getIndex() != null) {
            // compare based on index
            return t1.getIndex().compareTo(t2.getIndex());
          } else {
            // Old training dataset with no index. compare based on name
            return t1.getName().compareTo(t2.getName());
          }
        })
        .collect(Collectors.toList());
  }
}
