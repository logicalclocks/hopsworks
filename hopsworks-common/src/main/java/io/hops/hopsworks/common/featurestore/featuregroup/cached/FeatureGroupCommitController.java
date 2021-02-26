/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.featuregroup.cached;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.common.dao.AbstractFacade.CollectionInfo;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupValidationFacade;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HiveTbls;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class controlling the interaction with the training_dataset table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupCommitController {
  @EJB
  private FeatureGroupCommitFacade featureGroupCommitFacade;
  @EJB
  private FeatureGroupValidationFacade featureGroupValidationFacade;
  @EJB
  private FeaturestoreActivityFacade fsActivityFacade;
  @EJB
  private InodeController inodeController;

  private static final String HOODIE_METADATA_DIR = ".hoodie";
  private static final String HOODIE_COMMIT_METADATA_FILE = ".commit";
  private static final Logger LOGGER = Logger.getLogger(FeatureGroupCommitController.class.getName());

  public FeatureGroupCommit createHudiFeatureGroupCommit(Users user, Featuregroup featuregroup, String commitDateString,
                                                         Long commitTime, Long rowsUpdated, Long rowsInserted,
                                                         Long rowsDeleted, Integer validationId)
      throws FeaturestoreException {
    // Compute HUDI commit path
    String commitPath = computeHudiCommitPath(featuregroup, commitDateString);

    Inode inode = inodeController.getInodeAtPath(commitPath);
    // commit id will be timestamp
    FeatureGroupCommit featureGroupCommit = new FeatureGroupCommit(featuregroup.getId(), commitTime);
    featureGroupCommit.setInode(inode);
    featureGroupCommit.setCommittedOn(new Timestamp(commitTime));
    featureGroupCommit.setNumRowsUpdated(rowsUpdated);
    featureGroupCommit.setNumRowsInserted(rowsInserted);
    featureGroupCommit.setNumRowsDeleted(rowsDeleted);

    // Find validation
    if (validationId != null && validationId > 0) {
      featureGroupCommit.setValidation(featureGroupValidationFacade.findById(validationId));
    }

    featureGroupCommit = featureGroupCommitFacade.update(featureGroupCommit);
    fsActivityFacade.logCommitActivity(user, featuregroup, featureGroupCommit);

    return featureGroupCommit;
  }

  public FeatureGroupCommit findCommitByDate(Featuregroup featuregroup, Long commitTimestamp)
      throws FeaturestoreException {
    Optional<FeatureGroupCommit> featureGroupCommit;
    if (commitTimestamp != null){
      featureGroupCommit = featureGroupCommitFacade.findClosestDateCommit(featuregroup.getId(),  commitTimestamp);
    } else {
      featureGroupCommit = featureGroupCommitFacade.findLatestDateCommit(featuregroup.getId());
    }

    return featureGroupCommit.orElseThrow(() -> new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.NO_DATA_AVAILABLE_FEATUREGROUP_COMMITDATE, Level.FINE, "featureGroup: "
        + featuregroup.getName() + " version " + featuregroup.getVersion()));
  }

  public CollectionInfo getCommitDetails(Integer featureGroupId, Integer limit, Integer offset,
                                         Set<? extends AbstractFacade.SortBy> sort,
                                         Set<? extends AbstractFacade.FilterBy> filters) {

    if (filters == null || filters.isEmpty()) {
      return featureGroupCommitFacade.getCommitDetails(featureGroupId, limit, offset, sort);
    } else {
      return featureGroupCommitFacade.getCommitDetailsByDate(featureGroupId, limit, offset, sort, filters);
    }
  }

  protected String computeHudiCommitPath(Featuregroup featuregroup, String commitDateString) {
    // Check if CommitDateString matches pattern to "yyyyMMddHHmmss"
    try {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
      dateFormat.parse(commitDateString).getTime();
    } catch (ParseException e) {
      LOGGER.log(Level.SEVERE, "Unable to recognize provided HUDI commitDateString ", e);
    }

    HiveTbls hiveTbls = featuregroup.getCachedFeaturegroup().getHiveTbls();
    String dbLocation = hiveTbls.getSdId().getLocation();
    Path commitMetadataPath = new Path(HOODIE_METADATA_DIR,commitDateString
        + HOODIE_COMMIT_METADATA_FILE);
    Path commitPath = new Path(dbLocation, commitMetadataPath);
    return commitPath.toString();
  }

}