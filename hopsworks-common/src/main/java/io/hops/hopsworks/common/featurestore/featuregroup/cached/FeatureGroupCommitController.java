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

import io.hops.hopsworks.common.featurestore.activity.FeaturestoreActivityFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.util.AbstractFacade;
import io.hops.hopsworks.restutils.RESTCodes;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

/**
 * Class controlling the interaction with the training_dataset table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupCommitController {
  @EJB
  private FeatureGroupCommitFacade featureGroupCommitFacade;
  @EJB
  private FeaturestoreActivityFacade fsActivityFacade;

  public FeatureGroupCommit createHudiFeatureGroupCommit(Users user, Featuregroup featuregroup,
                                                         Long commitTime, Long rowsUpdated, Long rowsInserted,
                                                         Long rowsDeleted, Integer validationId,
                                                         Long lastActiveCommitTime) {
    // commit id will be timestamp
    FeatureGroupCommit featureGroupCommit = new FeatureGroupCommit(featuregroup.getId(), commitTime);
    featureGroupCommit.setCommittedOn(new Timestamp(commitTime));
    featureGroupCommit.setNumRowsUpdated(rowsUpdated);
    featureGroupCommit.setNumRowsInserted(rowsInserted);
    featureGroupCommit.setNumRowsDeleted(rowsDeleted);
    featureGroupCommit.setArchived(false);

    // Find validation
    if (validationId != null && validationId > 0) {
      // we might want to add back GE validation tracking here
    }

    // write the current commit on the database
    featureGroupCommit = featureGroupCommitFacade.update(featureGroupCommit);

    if (lastActiveCommitTime != null) {
      // mark archived commits
      featureGroupCommitFacade.markArchived(featuregroup.getId(), new Timestamp(lastActiveCommitTime));
    }

    fsActivityFacade.logCommitActivity(user, featuregroup, featureGroupCommit);

    return featureGroupCommit;
  }

  public Optional<FeatureGroupCommit> findCommitByDate(Featuregroup featuregroup, Long commitTimestamp) {
    if (commitTimestamp != null){
      return featureGroupCommitFacade.findClosestDateCommit(featuregroup.getId(), commitTimestamp);
    } else {
      return featureGroupCommitFacade.findLatestDateCommit(featuregroup.getId());
    }
  }

  public Integer countCommitsInRange(Featuregroup featuregroup, Long startTimestamp, Long endTimestamp) {
    return featureGroupCommitFacade.countCommitsInRange(featuregroup.getId(), startTimestamp, endTimestamp);
  }
  
  public Optional<Pair<FeatureGroupCommit, FeatureGroupCommit>> findEarliestAndLatestCommitsInRange(
    Featuregroup featuregroup,Long startTimestamp, Long endTimestamp) {
    Optional<FeatureGroupCommit> earliestCommit =
      featureGroupCommitFacade.findEarliestCommitInRange(featuregroup.getId(), startTimestamp, endTimestamp);
    Optional<FeatureGroupCommit> latestCommit =
      featureGroupCommitFacade.findLatestCommitInRange(featuregroup.getId(), startTimestamp, endTimestamp);
    
    if (latestCommit.isPresent() && !earliestCommit.isPresent()) {
      earliestCommit = latestCommit;  // single commit window
    } else if (!latestCommit.isPresent() && earliestCommit.isPresent()) {
      latestCommit = earliestCommit;  // single commit window
    }
    
    return latestCommit.isPresent()
      ? Optional.of(new Pair<>(earliestCommit.get(), latestCommit.get()))  // commit window
      : Optional.empty();  // no commits within range
  }
  
  public Optional<FeatureGroupCommit> findFirstCommit(Featuregroup featuregroup) {
    return featureGroupCommitFacade.findEarliestDateCommit(featuregroup.getId());
  }
  
  public AbstractFacade.CollectionInfo getCommitDetails(Integer featureGroupId,
                                         Integer limit, Integer offset,
                                         Set<? extends AbstractFacade.SortBy> sort,
                                         Set<? extends AbstractFacade.FilterBy> filters) {

    if (filters == null || filters.isEmpty()) {
      return featureGroupCommitFacade.getCommitDetails(featureGroupId, limit, offset, sort);
    } else {
      return featureGroupCommitFacade.getCommitDetailsByDate(featureGroupId, limit, offset, sort, filters);
    }
  }

  // Commits for a feature group are unbounded. We can't rely on the database to clean it up
  // the transaction might be too big. Hence, before removing the feature group, we need to remove all the existing
  // commit metadata. In batches
  public void deleteFeatureGroupCommits(Featuregroup featuregroup) {
    while (true) {
      List<FeatureGroupCommit> featureGroupCommits =
          featureGroupCommitFacade.getCommitDetails(featuregroup.getId(), AbstractFacade.BATCH_SIZE, 0, null)
              .getItems();

      if (!featureGroupCommits.isEmpty()) {
        featureGroupCommitFacade.removeBatch(featureGroupCommits);
      } else {
        break;
      }
    }
  }
  
  public Pair<Long, Long> getStartEndCommitTimesByWindowTime(Featuregroup featuregroup,
    Long startTime, Long endTime) throws FeaturestoreException {
    // startTime can't be non-null if endTime is null, after validation by StatisticsInputValidation
    if (endTime == null || startTime == null || startTime == 0L) { // if one or both times are null
      startTime = 0L; // use 0L as window start commit time, instead of first commit time
      // get last commit or latest by endtime
      Optional<FeatureGroupCommit> lastCommit = findCommitByDate(featuregroup, endTime);
      if (!lastCommit.isPresent()) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_GROUP_COMMIT_NOT_FOUND, Level.WARNING,
          "No feature group commits found before commit time '" + endTime + "'");
      }
      return new Pair<>(startTime, lastCommit.get().getCommittedOn());
    }
    // otherwise, both times are non-null
    if (!endTime.equals(startTime)) {
      Optional<Pair<FeatureGroupCommit, FeatureGroupCommit>> startEndCommits =
        findEarliestAndLatestCommitsInRange(featuregroup, startTime, endTime);
      if (!startEndCommits.isPresent()) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_GROUP_COMMIT_NOT_FOUND, Level.WARNING,
          "The commit window provided does not contain any feature group commits");
      }
      return new Pair<>(
        startEndCommits.get().getValue0().getCommittedOn(), startEndCommits.get().getValue1().getCommittedOn());
    }
    // otherwise, start and end times are equal
    Optional<FeatureGroupCommit> commit = findCommitByDate(featuregroup, endTime);
    if (!commit.isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_GROUP_COMMIT_NOT_FOUND, Level.WARNING,
        "No feature group commits found before commit time '" + endTime + "'");
    }
    startTime = endTime = commit.get().getCommittedOn();
    return new Pair<>(startTime, endTime);
  }
}