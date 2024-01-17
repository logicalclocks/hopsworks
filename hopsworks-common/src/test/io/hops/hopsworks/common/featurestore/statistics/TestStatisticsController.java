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
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommitPK;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TestStatisticsController {
  
  @InjectMocks
  private StatisticsController target = new StatisticsController();
  
  @Before
  public void setup() throws Exception {
    MockitoAnnotations.openMocks(this);
  }
  
  @Test
  public void testBuildStatisticsQueryFiltersWithRowPercentage() {
    // without row percentage
    Set<StatisticsFilterBy> filters = target.buildStatisticsQueryFilters(null, null, null, null);
    Set<AbstractFacade.FilterBy> targetFilters = buildTargetStatisticsQueryFilters(null, null, null, null);
    assertStatisticsFilters(toAbstractFacadeFilterBy(filters), targetFilters);
    // with row percentage
    filters = target.buildStatisticsQueryFilters(null, null, 0.5f, null);
    targetFilters = buildTargetStatisticsQueryFilters(null, null, 0.5f, null);
    assertStatisticsFilters(toAbstractFacadeFilterBy(filters), targetFilters);
  }
  @Test
  public void testBuildStatisticsQueryFiltersWithWindowCommitTimes() {
    // with window commit times
    Set<StatisticsFilterBy> filters = target.buildStatisticsQueryFilters(1L, 2L, null, null);
    Set<AbstractFacade.FilterBy> targetFilters = buildTargetStatisticsQueryFilters(1L, 2L, null, null);
    assertStatisticsFilters(toAbstractFacadeFilterBy(filters), targetFilters);
  }
  
  @Test
  public void testBuildStatisticsQueryFiltersWithForTransformation() {
    // with commit window
    Set<StatisticsFilterBy> filters = target.buildStatisticsQueryFilters(null, null, null, true);
    Set<AbstractFacade.FilterBy> targetFilters = buildTargetStatisticsQueryFilters(null, null, null, true);
    assertStatisticsFilters(toAbstractFacadeFilterBy(filters), targetFilters);
  }
  
  @Test
  public void testExtractTimeWindowFromFilters() {
    Set<AbstractFacade.FilterBy> filters = buildTargetStatisticsQueryFilters(1L, 2L,null, null);
    Pair<Long, Long> windowTimes = target.extractTimeWindowFromFilters(filters,
      StatisticsFilters.Filters.WINDOW_START_COMMIT_TIME_EQ, StatisticsFilters.Filters.WINDOW_END_COMMIT_TIME_EQ);
    Assert.assertEquals(windowTimes.getValue0(), Long.valueOf(1L));
    Assert.assertEquals(windowTimes.getValue1(), Long.valueOf(2L));
  }
  
  @Test
  public void testOverwriteTimeWindowFilters() {
    Set<AbstractFacade.FilterBy> filters = buildTargetStatisticsQueryFilters(1L, 2L, 0.5f, false);
    target.overwriteTimeWindowFilters(filters, 3L, 4L, StatisticsFilters.Filters.WINDOW_START_COMMIT_TIME_EQ,
      StatisticsFilters.Filters.WINDOW_END_COMMIT_TIME_EQ);
    Set<AbstractFacade.FilterBy> targetFilters = buildTargetStatisticsQueryFilters(3L, 4L, 0.5f, false);
    assertStatisticsFilters(filters, targetFilters);
  }

  // Utils
  
  private Set<AbstractFacade.FilterBy> buildTargetStatisticsQueryFilters(Long commitWindowStartTime,
    Long commitWindowEndTime, Float rowPercentage, Boolean beforeTransformation) {
    Set<AbstractFacade.FilterBy> filters = new HashSet<>();
    filters.add(new StatisticsFilterBy(
      StatisticsFilters.Filters.ROW_PERCENTAGE_EQ,
      rowPercentage != null ? String.valueOf(rowPercentage) : null));
    if (commitWindowStartTime != null) {
      filters.add(new StatisticsFilterBy(
        StatisticsFilters.Filters.WINDOW_START_COMMIT_TIME_EQ,
        String.valueOf(commitWindowStartTime)));
    }
    if (commitWindowEndTime != null) {
      filters.add(new StatisticsFilterBy(
        StatisticsFilters.Filters.WINDOW_END_COMMIT_TIME_EQ,
        String.valueOf(commitWindowEndTime)));
    }
    if (beforeTransformation != null) {
      filters.add(new StatisticsFilterBy(
        StatisticsFilters.Filters.BEFORE_TRANSFORMATION_EQ, String.valueOf(beforeTransformation)));
    }
    return filters;
  }
  
  private Set<AbstractFacade.FilterBy> toAbstractFacadeFilterBy(Set<StatisticsFilterBy> filters) {
    return filters.stream().map(f -> (AbstractFacade.FilterBy)f).collect(Collectors.toSet());
  }
  
  private void assertStatisticsFilters(Set<AbstractFacade.FilterBy> filters, Set<AbstractFacade.FilterBy> targetFilters) {
    Assert.assertTrue(filters.size() == targetFilters.size());
    
    // we need to sort the sets
    List<AbstractFacade.FilterBy> filterList = filters.stream()
      .sorted(Comparator.comparing(AbstractFacade.FilterBy::getField)).collect(Collectors.toList());
    List<AbstractFacade.FilterBy> targetFilterList = targetFilters.stream()
      .sorted(Comparator.comparing(AbstractFacade.FilterBy::getField)).collect(Collectors.toList());
    Iterator<AbstractFacade.FilterBy> iter = filterList.iterator(), targetIter = targetFilterList.iterator();
    
    while(targetIter.hasNext()) {
      Assert.assertEquals(iter.next(), targetIter.next());
    }
    Assert.assertFalse(iter.hasNext());
  }
}
