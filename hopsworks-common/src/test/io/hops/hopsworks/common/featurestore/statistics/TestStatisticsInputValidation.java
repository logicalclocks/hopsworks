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
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.FeaturegroupType;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class TestStatisticsInputValidation {
  
  @InjectMocks
  private StatisticsInputValidation target = new StatisticsInputValidation();
  
  @Mock
  private Featuregroup ondemandFG, streamFG;
  
  @Mock
  private TrainingDatasetJoin ondemandFgTrainingDatasetJoin, streamFgTrainingDatasetJoin;
  @Mock
  private FeatureView onDemandFgFV, streamFgFV;
  
  @Mock
  private TrainingDataset trainingDataset;
  
  @Before
  public void setup() throws Exception {
    MockitoAnnotations.openMocks(this);
    Mockito.when(ondemandFG.getFeaturegroupType()).thenReturn(FeaturegroupType.ON_DEMAND_FEATURE_GROUP);
    Mockito.when(streamFG.getFeaturegroupType()).thenReturn(FeaturegroupType.STREAM_FEATURE_GROUP);
    
    Mockito.when(ondemandFgTrainingDatasetJoin.getFeatureGroup()).thenReturn(ondemandFG);
    Mockito.when(streamFgTrainingDatasetJoin.getFeatureGroup()).thenReturn(streamFG);
    Mockito.when(onDemandFgFV.getJoins()).thenReturn(Collections.singleton(ondemandFgTrainingDatasetJoin));
    Mockito.when(streamFgFV.getJoins()).thenReturn(Collections.singleton(streamFgTrainingDatasetJoin));
  }
  
  @Test
  public void testValidateStatisticsFilters() {
    // not valid - duplicated filter
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      Set<AbstractFacade.FilterBy> filters = new HashSet<>();
      filters.add(new StatisticsFilterBy(StatisticsFilters.Filters.COMPUTATION_TIME_LT, String.valueOf(1L)));
      filters.add(new StatisticsFilterBy(StatisticsFilters.Filters.COMPUTATION_TIME_LT, String.valueOf(2L)));
      target.validateStatisticsFilters(filters, "any", false, false, false);
    });
    // not valid - overlapping commit time filter
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      Set<AbstractFacade.FilterBy> filters = new HashSet<>();
      filters.add(new StatisticsFilterBy(StatisticsFilters.Filters.COMPUTATION_TIME_EQ, String.valueOf(1L)));
      filters.add(new StatisticsFilterBy(StatisticsFilters.Filters.COMPUTATION_TIME_GT, String.valueOf(2L)));
      target.validateStatisticsFilters(filters, "any", false, false, false);
    });
    // not valid - window commit time not expected (windowCommitTime: false)
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      Set<AbstractFacade.FilterBy> filters = new HashSet<>();
      filters.add(new StatisticsFilterBy(StatisticsFilters.Filters.WINDOW_START_COMMIT_TIME_GTOEQ, String.valueOf(1L)));
      target.validateStatisticsFilters(filters, "any", false, false, false);
    });
    // not valid - row percentage not expected (rowPercentage: false)
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      Set<AbstractFacade.FilterBy> filters = new HashSet<>();
      filters.add(new StatisticsFilterBy(StatisticsFilters.Filters.ROW_PERCENTAGE_EQ, String.valueOf(1L)));
      target.validateStatisticsFilters(filters, "any", false, false, false);
    });
    // not valid - before transformation not expected (beforeTransformation: false)
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      Set<AbstractFacade.FilterBy> filters = new HashSet<>();
      filters.add(new StatisticsFilterBy(StatisticsFilters.Filters.BEFORE_TRANSFORMATION_EQ, String.valueOf(1L)));
      target.validateStatisticsFilters(filters, "any", false, false, false);
    });
    // valid - commit times
    Set<AbstractFacade.FilterBy> filters = new HashSet<>();
    filters.add(new StatisticsFilterBy(StatisticsFilters.Filters.COMPUTATION_TIME_LT, String.valueOf(1L)));
    filters.add(new StatisticsFilterBy(StatisticsFilters.Filters.COMPUTATION_TIME_GT, String.valueOf(5L)));
    target.validateStatisticsFilters(filters, "any", false, false, false);
    // valid - window commit times (windowCommitTimes: true)
    filters = new HashSet<>();
    filters.add(new StatisticsFilterBy(StatisticsFilters.Filters.WINDOW_START_COMMIT_TIME_GTOEQ, String.valueOf(1L)));
    target.validateStatisticsFilters(filters, "any", true, false, false);
    // valid - row percentage (rowPercentage: true)
    filters = new HashSet<>();
    filters.add(new StatisticsFilterBy(StatisticsFilters.Filters.ROW_PERCENTAGE_EQ, String.valueOf(1L)));
    target.validateStatisticsFilters(filters, "any", false, true, false);
    // valid - before transformation (beforeTransformation: true)
    filters = new HashSet<>();
    filters.add(new StatisticsFilterBy(StatisticsFilters.Filters.BEFORE_TRANSFORMATION_EQ, String.valueOf(1L)));
    target.validateStatisticsFilters(filters, "any", false, false, true);
  }
  
  @Test
  public void testValidateRegisterForFeatureGroup() {
    // statistics computation time (commit time) cannot be lower than window times
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(0L, 1L, null);
      target.validateRegisterForFeatureGroup(ondemandFG, stats);
    });
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(1L, 0L, 2L);
      target.validateRegisterForFeatureGroup(ondemandFG, stats);
    });
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(1L, null, 2L);
      target.validateRegisterForFeatureGroup(ondemandFG, stats);
    });
    // window times not supported in non-time-travel enabled FGs
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(null, 1L, null);
      target.validateRegisterForFeatureGroup(ondemandFG, stats);
    });
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(null, null, 1L);
      target.validateRegisterForFeatureGroup(ondemandFG, stats);
    });
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(null, 1L, 2L);
      target.validateRegisterForFeatureGroup(ondemandFG, stats);
    });
    // window start commit time can't be provided without end time
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(null, 1L, null);
      target.validateRegisterForFeatureGroup(streamFG, stats);
    });
  }
  
  @Test
  public void testValidateGetForFeatureGroup() {
    // window times not supported in non-time-travel enabled FGs
    Assert.assertThrows(FeaturestoreException.class, () -> {
      StatisticsFilters filters = buildStatisticsFilters(null, 1L, null);
      target.validateGetForFeatureGroup(ondemandFG, filters);
    });
    Assert.assertThrows(FeaturestoreException.class, () -> {
      StatisticsFilters filters = buildStatisticsFilters(null, null, 2L);
      target.validateGetForFeatureGroup(ondemandFG, filters);
    });
    Assert.assertThrows(FeaturestoreException.class, () -> {
      StatisticsFilters filters = buildStatisticsFilters(null, 1L, 2L);
      target.validateGetForFeatureGroup(ondemandFG, filters);
    });
    // window start commit time can't be provided without end time
    Assert.assertThrows(FeaturestoreException.class, () -> {
      StatisticsFilters filters = buildStatisticsFilters(null, 1L, null);
      target.validateGetForFeatureGroup(streamFG, filters);
    });
  }
  
  @Test
  public void testValidateRegisterForFeatureView() {
    // statistics computation time (commit time) cannot be lower than window times
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(0L, 1L, null);
      target.validateRegisterForFeatureView(onDemandFgFV, stats);
    });
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(1L, 0L, 2L);
      target.validateRegisterForFeatureView(onDemandFgFV, stats);
    });
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(1L, null, 2L);
      target.validateRegisterForFeatureView(onDemandFgFV, stats);
    });
    // window times not supported in non-time-travel enabled FGs
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(null, 1L, null);
      target.validateRegisterForFeatureView(onDemandFgFV, stats);
    });
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(null, null, 1L);
      target.validateRegisterForFeatureView(onDemandFgFV, stats);
    });
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(null, 1L, 2L);
      target.validateRegisterForFeatureView(onDemandFgFV, stats);
    });
    // window start commit time can't be provided without end time
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(null, 1L, null);
      target.validateRegisterForFeatureView(streamFgFV, stats);
    });
  }
  
  @Test
  public void testValidateGetForFeatureView() {
    // window times not supported in non-time-travel enabled left FGs
    Assert.assertThrows(FeaturestoreException.class, () -> {
      StatisticsFilters filters = buildStatisticsFilters(null, 1L, null);
      target.validateGetForFeatureView(onDemandFgFV, filters);
    });
    Assert.assertThrows(FeaturestoreException.class, () -> {
      StatisticsFilters filters = buildStatisticsFilters(null, null, 2L);
      target.validateGetForFeatureView(onDemandFgFV, filters);
    });
    Assert.assertThrows(FeaturestoreException.class, () -> {
      StatisticsFilters filters = buildStatisticsFilters(null, 1L, 2L);
      target.validateGetForFeatureView(onDemandFgFV, filters);
    });
    // window start commit time can't be provided without end time
    Assert.assertThrows(FeaturestoreException.class, () -> {
      StatisticsFilters filters = buildStatisticsFilters(null, 1L, null);
      target.validateGetForFeatureView(streamFgFV, filters);
    });
  }
  
  @Test
  public void testValidateRegisterForTrainingDataset() {
    // window times are not supported
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(null, 1L, 2L);
      target.validateRegisterForTrainingDataset(trainingDataset, stats);
    });
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(null, 1L, null);
      target.validateRegisterForTrainingDataset(trainingDataset, stats);
    });
    Assert.assertThrows(IllegalArgumentException.class, () -> {
      StatisticsDTO stats = buildStatisticsDTO(null, null, 2L);
      target.validateRegisterForTrainingDataset(trainingDataset, stats);
    });
  }
  
  // Utils
  
  private StatisticsDTO buildStatisticsDTO(Long computationTime, Long windowStartCommitTime, Long windowEndCommitTime) {
    StatisticsDTO stats = new StatisticsDTO();
    stats.setComputationTime(computationTime);
    stats.setWindowStartCommitTime(windowStartCommitTime);
    stats.setWindowEndCommitTime(windowEndCommitTime);
    return stats;
  }
  
  private StatisticsFilters buildStatisticsFilters(Long computationTime, Long windowStartCommitTime, Long windowEndCommitTime) {
    StatisticsFilters filters = new StatisticsFilters();
    filters.setComputationTime(computationTime);
    filters.setWindowStartCommitTime(windowStartCommitTime);
    filters.setWindowEndCommitTime(windowEndCommitTime);
    return filters;
  }
}
