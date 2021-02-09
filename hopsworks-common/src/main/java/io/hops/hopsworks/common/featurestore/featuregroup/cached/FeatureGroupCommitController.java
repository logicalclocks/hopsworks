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
import io.hops.hopsworks.common.dao.AbstractFacade.CollectionInfo;
import io.hops.hopsworks.common.featurestore.datavalidation.deequ.FeatureGroupValidationFacade;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HiveTbls;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
  private InodeController inodeController;

  private static final String HOODIE_METADATA_DIR = ".hoodie";
  private static final String HOODIE_COMMIT_METADATA_FILE = ".commit";
  private static final Logger LOGGER = Logger.getLogger(FeatureGroupCommitController.class.getName());
  private static final HashMap<Pattern, String> DATE_FORMAT_PATTERNS = new HashMap<Pattern, String>() {{
      put(Pattern.compile("^([0-9]{4})([0-9]{2})([0-9]{2})$"), "yyyyMMdd");
      put(Pattern.compile("^([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})$"), "yyyyMMddHH");
      put(Pattern.compile("^([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})$"), "yyyyMMddHHmm");
      put(Pattern.compile("^([0-9]{4})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})([0-9]{2})$"), "yyyyMMddHHmmss");
    }};

  public FeatureGroupCommit createHudiFeatureGroupCommit(Featuregroup featuregroup, String commitDateString,
                                                         Long rowsUpdated, Long rowsInserted, Long rowsDeleted,
                                                         Integer validationId)
      throws FeaturestoreException {
    // Compute HUDI commit path
    String commitPath = computeHudiCommitPath(featuregroup, commitDateString);

    Inode inode = inodeController.getInodeAtPath(commitPath);
    // commit id will be timestamp
    FeatureGroupCommit featureGroupCommit = new FeatureGroupCommit(featuregroup.getId(),
        getTimeStampFromDateString(commitDateString));
    featureGroupCommit.setInode(inode);
    featureGroupCommit.setCommittedOn(new Timestamp(getTimeStampFromDateString(commitDateString)));
    featureGroupCommit.setNumRowsUpdated(rowsUpdated);
    featureGroupCommit.setNumRowsInserted(rowsInserted);
    featureGroupCommit.setNumRowsDeleted(rowsDeleted);
    // Find validation
    if (validationId != null && validationId > 0) {
      featureGroupCommit.setValidation(featureGroupValidationFacade.findById(validationId));
    }
    featureGroupCommitFacade.createFeatureGroupCommit(featureGroupCommit);

    return featureGroupCommit;
  }

  public FeatureGroupCommit findCommitByDate(Featuregroup featuregroup, String wallclocktime)
      throws FeaturestoreException {
    Optional<FeatureGroupCommit> featureGroupCommit;
    if (wallclocktime != null){
      Long wallclockTimestamp =  getTimeStampFromDateString(wallclocktime);
      featureGroupCommit = featureGroupCommitFacade.findClosestDateCommit(featuregroup.getId(),  wallclockTimestamp);
    } else {
      featureGroupCommit = featureGroupCommitFacade.findLatestDateCommit(featuregroup.getId());
    }

    return featureGroupCommit.orElseThrow(() -> new FeaturestoreException(
        RESTCodes.FeaturestoreErrorCode.NO_DATA_AVAILABLE_FEATUREGROUP_COMMITDATE, Level.FINE, "featureGroup: "
        + featuregroup.getName() + " version " + featuregroup.getVersion()));
  }

  public CollectionInfo getCommitDetails(Integer featureGroupId, Integer limit, Integer offset,
                                                        Set<? extends AbstractFacade.SortBy> sort) {
    return featureGroupCommitFacade.getCommitDetails(featureGroupId, limit, offset, sort);
  }

  public Long getTimeStampFromDateString(String inputDate) throws FeaturestoreException {
    String tempDate = inputDate.replace("/", "")
        .replace("-", "").replace(" ", "")
        .replace(":","");
    String dateFormatPattern = null;

    for (Pattern pattern : DATE_FORMAT_PATTERNS.keySet()) {
      if (pattern.matcher(tempDate).matches()) {
        dateFormatPattern = DATE_FORMAT_PATTERNS.get(pattern);
      }
    }

    if (dateFormatPattern == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.PROVIDED_DATE_FORMAT_NOT_SUPPORTED,
          Level.FINE, "Unable to identify format of the provided date value : " + inputDate);
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatPattern);
    Long commitTimeStamp = null;
    try {
      commitTimeStamp = dateFormat.parse(tempDate).getTime();
    } catch (ParseException e) {
      LOGGER.log(Level.FINE, "Unable to convert provided date value : " + tempDate + " to timestamp", e);
    }

    return commitTimeStamp;
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