/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.statistics;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.restutils.RESTCodes;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class StatisticsInputValidation {
  
  public void validateFeatureDescriptiveStatistics(FeatureDescriptiveStatisticsDTO fdsDto) {
    if (fdsDto.getFeatureName() == null) {
      throw new IllegalArgumentException("Feature name is missing in descriptive statistics");
    }
    if (fdsDto.getExtendedStatistics() != null) {
      try {
        new JSONObject(fdsDto.getExtendedStatistics());
      } catch (JSONException e) {
        throw new IllegalArgumentException("Extended statistics are not a valid json string");
      }
    }
  }
  
  public void validateRegisterFeatureDescriptiveStatistics(Collection<FeatureDescriptiveStatisticsDTO> fds,
      String content) {
    if (fds != null && content != null) {
      throw new IllegalArgumentException("Both descriptive statistics and content cannot be provided together");
    }
    Set<String> featureNames =
      fds.stream().map(FeatureDescriptiveStatisticsDTO::getFeatureName).collect(Collectors.toSet());
    if (featureNames.size() != fds.size()) {
      throw new IllegalArgumentException(
        "Feature descriptive statistics provided cannot contain two or more statistics for the same feature name");
    }
    for (FeatureDescriptiveStatisticsDTO fdsDTO : fds) {
      validateFeatureDescriptiveStatistics(fdsDTO);
    }
  }
  
  public void validateRegisterStatistics(StatisticsDTO statisticsDTO) {
    if (statisticsDTO.getFeatureDescriptiveStatistics() != null || statisticsDTO.getContent() != null) {
      if (statisticsDTO.getSplitStatistics() != null) {
        throw new IllegalArgumentException(
          "Both descriptive statistics and split statistics cannot be provided together");
      }
      validateRegisterFeatureDescriptiveStatistics(statisticsDTO.getFeatureDescriptiveStatistics(),
        statisticsDTO.getContent());
    } else {
      if (statisticsDTO.getSplitStatistics() == null) {
        throw new IllegalArgumentException("Descriptive statistics not provided.");
      } else {
        for (SplitStatisticsDTO splitStats : statisticsDTO.getSplitStatistics()) {
          validateRegisterFeatureDescriptiveStatistics(splitStats.getFeatureDescriptiveStatistics(),
            splitStats.getContent());
        }
      }
    }
  }
  
  private void validateCommitAndWindowTimesOccurance(Long commitTime, Long windowStartTime, Long windowEndTime) {
    // only for feature groups and feature views
    if (commitTime != null) {
      if ((windowStartTime != null && commitTime < windowStartTime)
        || (windowEndTime != null && commitTime < windowEndTime)) {
        throw new IllegalArgumentException("Statistics computation time cannot be lower than window times");
      }
    }
  }
  
  public void validateRegisterForFeatureGroup(Featuregroup featuregroup, StatisticsDTO statisticsDTO)
    throws FeaturestoreException {
    this.validateRegisterStatistics(statisticsDTO);
    if (!FeaturegroupController.isTimeTravelEnabled(featuregroup)) { // time-travel not enabled
      if (statisticsDTO.getWindowStartCommitTime() != null || statisticsDTO.getWindowEndCommitTime() != null) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_STATISTICS_WINDOW_TIMES, Level.FINE,
          "Commit window is only supported on time-travel enabled or stream feature groups");
      }
    } else { // time-travel enabled
      // if start commit is not provided, the first commit will be used.
      // if end commit is not provided, the last commit will be used.
      if (statisticsDTO.getWindowStartCommitTime() != null && statisticsDTO.getWindowEndCommitTime() == null) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_STATISTICS_WINDOW_TIMES, Level.FINE,
          "Commit window end time is required if start time is provided");
      }
    }
    this.validateCommitAndWindowTimesOccurance(statisticsDTO.getComputationTime(),
      statisticsDTO.getWindowStartCommitTime(), statisticsDTO.getWindowEndCommitTime());
  }
  
  public void validateRegisterForFeatureView(FeatureView featureView, StatisticsDTO statisticsDTO)
    throws FeaturestoreException {
    this.validateRegisterStatistics(statisticsDTO);
    // validate inputs based on left feature group in the feature view query
    Featuregroup featuregroup = FeatureViewController.getLeftFeatureGroup(featureView);
    this.validateRegisterForFeatureGroup(featuregroup, statisticsDTO);
  }
  
  public void validateGetForFeatureGroup(Featuregroup featuregroup, StatisticsFilters fgsFilters)
    throws FeaturestoreException {
    if (!FeaturegroupController.isTimeTravelEnabled(featuregroup)) { // time-travel not enabled
      if (fgsFilters.getWindowStartCommitTime() != null || fgsFilters.getWindowEndCommitTime() != null) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_STATISTICS_WINDOW_TIMES, Level.FINE,
          "Commit window is only supported on time-travel enabled or stream feature groups");
      }
    } else { // time-travel enabled
      // if start commit is not provided, the first commit will be used.
      // if end commit is not provided, the last commit will be used.
      if (fgsFilters.getWindowStartCommitTime() != null && fgsFilters.getWindowEndCommitTime() == null) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_STATISTICS_WINDOW_TIMES, Level.FINE,
          "Commit window end time is required if start time is provided");
      }
    }
    this.validateCommitAndWindowTimesOccurance(fgsFilters.getComputationTime(), fgsFilters.getWindowStartCommitTime(),
      fgsFilters.getWindowEndCommitTime());
  }
  
  public void validateGetForFeatureView(FeatureView featureView, StatisticsFilters fvsFilters)
    throws FeaturestoreException {
    // validate inputs based on left feature group in the feature view query
    Featuregroup featuregroup = FeatureViewController.getLeftFeatureGroup(featureView);
    validateGetForFeatureGroup(featuregroup, fvsFilters);
  }
  
  public void validateRegisterForTrainingDataset(TrainingDataset trainingDataset, StatisticsDTO statisticsDTO)
    throws FeaturestoreException {
    this.validateRegisterStatistics(statisticsDTO);
    if (statisticsDTO.getWindowStartCommitTime() != null || statisticsDTO.getWindowEndCommitTime() != null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_STATISTICS_WINDOW_TIMES, Level.FINE,
        "Commit time window not supported for training dataset statistics");
    }
  }
  
  public void validateStatisticsFiltersForFeatureGroup(Set<AbstractFacade.FilterBy> filters) {
    validateStatisticsFilters(filters, "Feature Group", true, true, false);
  }
  
  public void validateStatisticsFiltersForFeatureView(Set<AbstractFacade.FilterBy> filters) {
    validateStatisticsFilters(filters, "Feature View", true, true, false);
  }
  
  public void validateStatisticsFiltersForTrainingDataset(Set<AbstractFacade.FilterBy> filters) {
    validateStatisticsFilters(filters, "Training Dataset", false, false, true);
  }
  
  public void validateStatisticsFilters(Set<AbstractFacade.FilterBy> filters, String statisticsType,
    boolean windowCommitTimes, boolean rowPercentage, boolean beforeTransformation) {
    // validate that filters are supported for the specific statistics type (FG, FV, TD)
    // validate that the filters provided do not contain overlapping rules.
    // - only one of GT|LT or GTOEQ|LTOEQ can be provided
    // - only one of EQ or GT|LT|GTOEQ|LTOEQ can be provided
    
    // flags
    boolean computationTimeLt = false, computationTimeEq = false, computationTimeGt = false;
    boolean windowStartCT = false, windowEndCT = false;
    
    String overlappingErrMsg = "Overlapping filters are not supported";
    String notSupportedErrMsg = "Filter '%s' not supported for %s statistics";
    
    if (filters != null) {
      for (AbstractFacade.FilterBy filter : filters) {
        
        // computation time
        if (filter.getValue().startsWith(StatisticsFilters.Filters.COMPUTATION_TIME_LT.getValue())) {
          if (computationTimeLt || computationTimeEq) {
            throw new IllegalArgumentException(overlappingErrMsg);
          }
          computationTimeLt = true;
        } else if (filter.getValue().startsWith(StatisticsFilters.Filters.COMPUTATION_TIME_LTOEQ.getValue())) {
          if (computationTimeLt) {
            throw new IllegalArgumentException(overlappingErrMsg);
          }
          computationTimeLt = true;
        } else if (filter.getValue().startsWith(StatisticsFilters.Filters.COMPUTATION_TIME_GT.getValue())) {
          if (computationTimeGt || computationTimeEq) {
            throw new IllegalArgumentException(overlappingErrMsg);
          }
          computationTimeGt = true;
        } else if (filter.getValue().startsWith(StatisticsFilters.Filters.COMPUTATION_TIME_EQ.getValue())) {
          if (computationTimeEq || computationTimeLt || computationTimeGt) {
            throw new IllegalArgumentException(overlappingErrMsg);
          }
          computationTimeEq = true;
          
          // window commit times
        } else if (filter.getValue().startsWith(StatisticsFilters.Filters.WINDOW_START_COMMIT_TIME_GTOEQ.getValue()) ||
          filter.getValue().startsWith(StatisticsFilters.Filters.WINDOW_START_COMMIT_TIME_EQ.getValue())) {
          if (!windowCommitTimes) {
            throw new IllegalArgumentException(String.format(notSupportedErrMsg, filter.getValue(), statisticsType));
          }
          if (windowStartCT) {
            throw new IllegalArgumentException(overlappingErrMsg);
          }
          windowStartCT = true;
        } else if (filter.getValue().startsWith(StatisticsFilters.Filters.WINDOW_END_COMMIT_TIME_LTOEQ.getValue()) ||
          filter.getValue().startsWith(StatisticsFilters.Filters.WINDOW_END_COMMIT_TIME_EQ.getValue())) {
          if (!windowCommitTimes) {
            throw new IllegalArgumentException(String.format(notSupportedErrMsg, filter.getValue(), statisticsType));
          }
          if (windowEndCT) {
            throw new IllegalArgumentException(overlappingErrMsg);
          }
          windowEndCT = true;
          
          // row percentage
        } else if (!rowPercentage &&
          filter.getValue().startsWith(StatisticsFilters.Filters.ROW_PERCENTAGE_EQ.getValue())) {
          throw new IllegalArgumentException(String.format(notSupportedErrMsg, filter.getValue(), statisticsType));
          
          // before transformation
        } else if (!beforeTransformation &&
          filter.getValue().startsWith(StatisticsFilters.Filters.BEFORE_TRANSFORMATION_EQ.getValue())) {
          throw new IllegalArgumentException(String.format(notSupportedErrMsg, filter.getValue(), statisticsType));
        }
      }
    }
  }
}