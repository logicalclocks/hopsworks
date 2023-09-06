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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.hops.hopsworks.common.commands.featurestore.search.SearchFSCommandLogger;
import io.hops.hopsworks.common.dao.QueryParam;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.featurestore.app.FsJobManagerController;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreConnectorFacade;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetFacade;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.provenance.explicit.FeatureViewLinkController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.activity.FeaturestoreActivityMeta;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.ServingKey;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoin;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoinCondition;
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
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
  private Settings settings;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private FeaturestoreConnectorFacade featurestoreConnectorFacade;
  @EJB
  private FeaturestoreActivityFacade fsActivityFacade;
  @EJB
  private FeaturestoreUtils featurestoreUtils;
  @EJB
  private TrainingDatasetFacade trainingDatasetFacade;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private ActivityFacade activityFacade;
  @Inject
  private FsJobManagerController fsJobManagerController;
  @EJB
  private FeatureViewLinkController featureViewLinkController;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private SearchFSCommandLogger searchCommandLogger;

  public FeatureView createFeatureView(Project project, Users user, FeatureView featureView, Featurestore featurestore)
    throws FeaturestoreException, IOException {
    featurestoreUtils.verifyUserProjectEqualsFsProject(user, project, featurestore,
        FeaturestoreUtils.ActionMessage.CREATE_FEATURE_VIEW);

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

    DistributedFileSystemOps udfso = null;
    try {
      String path = getLocation(featureView);

      udfso = dfs.getDfsOps(project, user);
      udfso.mkdirs(path, FsPermission.getDefault());

      featureView = featureViewFacade.update(featureView);
      searchCommandLogger.create(featureView);

      // Log the metadata operation
      fsActivityFacade.logMetadataActivity(user, featureView, FeaturestoreActivityMeta.FV_CREATED);

      activityFacade.persistActivity(ActivityFacade.CREATED_FEATURE_VIEW + featureView.getName(), project, user,
          ActivityFlag.SERVICE);
      searchCommandLogger.updateMetadata(featureView);
      featureViewLinkController.createParentLinks(featureView);
      return featureView;
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  public String getLocation(FeatureView featureView) throws FeaturestoreException {
    Featurestore featurestore = featureView.getFeaturestore();
    String connectorName =
        featurestore.getProject().getName() + "_" + Settings.ServiceDataset.TRAININGDATASETS.getName();

    Dataset datasetsFolder =
        featurestoreConnectorFacade.findByFeaturestoreName(featurestore, connectorName)
            .orElseThrow(() ->
                new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.HOPSFS_CONNECTOR_NOT_FOUND,
                    Level.FINE, "HOPSFS Connector: " + connectorName))
            .getHopsfsConnector().getHopsfsDataset();

    return String.format(PATH_TO_FEATURE_VIEW, Utils.getDatasetPath(datasetsFolder, settings),
        featureView.getName(), featureView.getVersion());
  }

  public List<FeatureView> getAll() {
    return featureViewFacade.findAll();
  }

  public List<FeatureView> getByFeatureStore(Featurestore featurestore, QueryParam queryParam) {
    return featureViewFacade.findByFeaturestore(featurestore, queryParam);
  }

  public List<FeatureView> getByNameAndFeatureStore(String name, Featurestore featurestore, QueryParam queryParam)
      throws FeaturestoreException {
    List<FeatureView> featureViews = featureViewFacade.findByNameAndFeaturestore(
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
      throws FeaturestoreException, JobException {
    List<FeatureView> featureViews = featureViewFacade.findByNameAndFeaturestore(name, featurestore);
    delete(user, project, featurestore, featureViews);
  }

  public void delete(Users user, Project project, Featurestore featurestore, String name, Integer version)
      throws FeaturestoreException, JobException {
    List<FeatureView> featureViews = featureViewFacade.findByNameVersionAndFeaturestore(name, version, featurestore);
    delete(user, project, featurestore, featureViews);
  }

  private void delete(Users user, Project project, Featurestore featurestore, List<FeatureView> featureViews)
      throws FeaturestoreException, JobException {
    if (featureViews == null || featureViews.isEmpty()) {
      throw new FeaturestoreException(FEATURE_VIEW_NOT_FOUND, Level.FINE, "Provided feature view name or version " +
          "does not exist.");
    }
    for (FeatureView fv : featureViews) {
      featurestoreUtils.verifyFeatureViewDataOwnerOrSelf(user, project, fv,
          FeaturestoreUtils.ActionMessage.DELETE_FEATURE_VIEW);
    }
    for (FeatureView fv : featureViews) {
      trainingDatasetController.delete(user, project, featurestore, fv);
      searchCommandLogger.delete(fv);
      featureViewFacade.remove(fv);
      removeFeatureViewDir(project, user, fv);
      //Delete associated jobs
      fsJobManagerController.deleteJobs(project, user, fv);
      activityFacade.persistActivity(ActivityFacade.DELETED_FEATURE_VIEW + fv.getName(),
          project, user, ActivityFlag.SERVICE);
    }
  }

  private void removeFeatureViewDir(Project project, Users user, FeatureView featureView) throws FeaturestoreException {
    DistributedFileSystemOps udfso = dfs.getDfsOps(project, user);
    try {
      udfso.rm(getLocation(featureView), true);
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ERROR_DELETING_FEATURE_VIEW,
          Level.WARNING, "Error removing feature view directory", e.getMessage(), e);
    } finally {
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
  }

  public FeatureView update(Users user, Project project, Featurestore featurestore, String name, Integer version,
      String description)
      throws FeaturestoreException {
    FeatureView featureView = getByNameVersionAndFeatureStore(name, version, featurestore);

    featurestoreUtils.verifyFeatureViewDataOwnerOrSelf(user, project, featureView,
        FeaturestoreUtils.ActionMessage.UPDATE_FEATURE_VIEW);

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

  public List<ServingKey> getServingKeys(Project project, Users user, FeatureView featureView)
      throws FeaturestoreException {
    List<ServingKey> servingKeys = Lists.newArrayList();
    Set<String> prefixFeatureNames = Sets.newHashSet();
    Optional<TrainingDatasetJoin> leftJoin =
        featureView.getJoins().stream().filter(join -> join.getIndex().equals(0)).findFirst();
    // If a feature group is deleted, the corresponding join will be deleted by cascade.
    if (!leftJoin.isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND,
          Level.SEVERE, "Cannot construct serving because some feature groups which are used in the query are removed");
    }
    Set<String> leftPrimaryKeys = featuregroupController
        .getFeatures(leftJoin.get().getFeatureGroup(), project, user)
        .stream()
        .filter(FeatureGroupFeatureDTO::getPrimary)
        .map(FeatureGroupFeatureDTO::getName)
        .collect(Collectors.toSet());
    for (TrainingDatasetJoin join :
        featureView.getJoins().stream().sorted(Comparator.comparingInt(TrainingDatasetJoin::getIndex)).collect(
            Collectors.toList())) {
      // This contains join key and pk of feature group
      Set<String> tempPrefixFeatureNames = Sets.newHashSet();

      List<FeatureGroupFeatureDTO> primaryKeys = featuregroupController
          .getFeatures(join.getFeatureGroup(), project, user)
          .stream()
          .filter(FeatureGroupFeatureDTO::getPrimary)
          .collect(Collectors.toList());
      Set<String> primaryKeyNames =
          primaryKeys.stream().map(FeatureGroupFeatureDTO::getName).collect(Collectors.toSet());
      Collection<TrainingDatasetJoinCondition> joinConditions = join.getConditions() == null ?
          Lists.newArrayList() : join.getConditions();
      for (TrainingDatasetJoinCondition condition : joinConditions) {
        ServingKey servingKey = new ServingKey();
        String featureName = condition.getRightFeature();
        // Join on column, so ignore
        if (!primaryKeyNames.contains(featureName)) {
          continue;
        }
        servingKey.setFeatureName(featureName);
        String joinOn = condition.getLeftFeature();
        servingKey.setJoinOn(joinOn);
        // if join on (column of left fg) is not a primary key, set required to true
        servingKey.setRequired(!leftPrimaryKeys.contains(joinOn));
        servingKey.setFeatureGroup(join.getFeatureGroup());
        servingKey.setFeatureView(featureView);
        servingKey.setJoinIndex(join.getIndex());
        // Set new prefix only if the key is required
        if (servingKey.getRequired()) {
          servingKey.setPrefix(
              getPrefixCheckCollision(prefixFeatureNames, servingKey.getFeatureName(), join.getPrefix()));
        } else {
          servingKey.setPrefix(join.getPrefix());
        }
        prefixFeatureNames.add(
            (servingKey.getPrefix() == null ? "" : servingKey.getPrefix()) + servingKey.getFeatureName());
        // use this and mark it as processed primary key so that it do not need to be processed again in the next step.
        tempPrefixFeatureNames.add(
            (join.getPrefix() == null ? "" : join.getPrefix()) + servingKey.getFeatureName());
        servingKeys.add(servingKey);
      }

      for (FeatureGroupFeatureDTO pk : primaryKeys) {
        String prefixFeatureName = pk.getName();
        if (!Strings.isNullOrEmpty(join.getPrefix())) {
          prefixFeatureName = join.getPrefix() + pk.getName();
        }
        if (!tempPrefixFeatureNames.contains(prefixFeatureName)) {
          ServingKey servingKey = new ServingKey();
          servingKey.setFeatureName(pk.getName());
          servingKey.setPrefix(getPrefixCheckCollision(prefixFeatureNames, pk.getName(), join.getPrefix()));
          servingKey.setRequired(true);
          servingKey.setFeatureGroup(join.getFeatureGroup());
          servingKey.setJoinIndex(join.getIndex());
          servingKey.setFeatureView(featureView);
          servingKeys.add(servingKey);
          prefixFeatureNames.add(
              (servingKey.getPrefix() == null ? "" : servingKey.getPrefix()) + servingKey.getFeatureName());
          tempPrefixFeatureNames.add(
              (join.getPrefix() == null ? "" : join.getPrefix()) + servingKey.getFeatureName());
        }
      }
    }
    Set<Featuregroup> labelOnlyFgs = featureView.getFeatures().stream()
        .collect(Collectors.groupingBy(TrainingDatasetFeature::getFeatureGroup))
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue().stream().allMatch(TrainingDatasetFeature::isLabel))
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
    List<ServingKey> filteredServingKeys = Lists.newArrayList();
    for (ServingKey servingKey : servingKeys) {
      // pk from label only fg is not mandatory. But the pk needs to be kept if it is joint by another key
      // because the value of the pk is needed to propagate to the other key.
      if (labelOnlyFgs.contains(servingKey.getFeatureGroup())) {
        // Check if the serving key belongs to a left most fg by checking if the key was required by other fg.
        if (servingKeys.stream()
            .anyMatch(key -> ((servingKey.getPrefix() == null ? "" : servingKey.getPrefix())
                + servingKey.getFeatureName()).equals(key.getJoinOn()))) {
          filteredServingKeys.add(servingKey);
        }
      } else {
        filteredServingKeys.add(servingKey);
      }
    }
    return filteredServingKeys;
  }

  private String getPrefixCheckCollision(Set<String> prefixFeatureNames, String featureName, String prefix) {
    String prefixFeatureName = featureName;
    if (!Strings.isNullOrEmpty(prefix)) {
      prefixFeatureName = prefix + featureName;
    }
    if (prefixFeatureNames.contains(prefixFeatureName)) {
      // conflict with pk of other feature group, set new prefix
      String defaultPrefix;
      int i = 0;
      do {
        if (Strings.isNullOrEmpty(prefix)) {
          defaultPrefix = String.format("%d_", i);
        } else {
          defaultPrefix = String.format("%d_%s", i, prefix);
        }
        i++;
      } while (prefixFeatureNames.contains(defaultPrefix + featureName));
      return defaultPrefix;
    } else {
      return prefix;
    }
  }

  // For testing
  public void setFeaturegroupController(
      FeaturegroupController featuregroupController) {
    this.featuregroupController = featuregroupController;
  }
}
