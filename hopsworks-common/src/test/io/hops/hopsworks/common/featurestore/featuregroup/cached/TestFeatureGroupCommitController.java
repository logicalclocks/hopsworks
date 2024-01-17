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

import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

public class TestFeatureGroupCommitController {
  
  @InjectMocks
  private FeatureGroupCommitController target = new FeatureGroupCommitController();
  
  @Mock
  private FeatureGroupCommitFacade featureGroupCommitFacade;
  
  private Featuregroup featuregroup;
  private FeatureGroupCommit firstFGCommit, secondFGCommit, thirdFGCommit;
  
  @Before
  public void setup() throws Exception {
    MockitoAnnotations.openMocks(this);
    
    // simulate feature group with commit IDs 1L, 5L, 10L
    featuregroup = Mockito.mock(Featuregroup.class);
    firstFGCommit = Mockito.mock(FeatureGroupCommit.class);
    secondFGCommit = Mockito.mock(FeatureGroupCommit.class);
    thirdFGCommit = Mockito.mock(FeatureGroupCommit.class);
    setupFeatureGroupCommitController(featuregroup, firstFGCommit, secondFGCommit, thirdFGCommit);
  }
  
  @Test
  public void testFindEarliestAndLatestCommitsInRange() {
    // no times provided
    Optional<Pair<FeatureGroupCommit, FeatureGroupCommit>> commits =
      target.findEarliestAndLatestCommitsInRange(featuregroup, null, null);
    Assert.assertEquals(commits.get().getValue0(), firstFGCommit);
    Assert.assertEquals(commits.get().getValue1(), thirdFGCommit);
    // no times provided
    commits = target.findEarliestAndLatestCommitsInRange(featuregroup, null, 10L);
    Assert.assertTrue(commits.isPresent());
    Assert.assertEquals(commits.get().getValue0(), firstFGCommit);
    Assert.assertEquals(commits.get().getValue1(), thirdFGCommit);
    // both times provided
    commits = target.findEarliestAndLatestCommitsInRange(featuregroup, 1L, 5L);
    Assert.assertTrue(commits.isPresent());
    Assert.assertEquals(commits.get().getValue0(), firstFGCommit);
    Assert.assertEquals(commits.get().getValue1(), secondFGCommit);
    // both times provided - mismatched end time
    commits = target.findEarliestAndLatestCommitsInRange(featuregroup, 3L, 7L);
    Assert.assertTrue(commits.isPresent());
    Assert.assertEquals(commits.get().getValue0(), secondFGCommit);
    Assert.assertEquals(commits.get().getValue1(), secondFGCommit);
    // both times provided - same time
    commits = target.findEarliestAndLatestCommitsInRange(featuregroup, 5L, 5L);
    Assert.assertTrue(commits.isPresent());
    Assert.assertEquals(commits.get().getValue0(), secondFGCommit);
    Assert.assertEquals(commits.get().getValue1(), secondFGCommit);
    // end time out of range
    commits = target.findEarliestAndLatestCommitsInRange(featuregroup, 5L, 12L);
    Assert.assertTrue(commits.isPresent());
    Assert.assertEquals(commits.get().getValue0(), secondFGCommit);
    Assert.assertEquals(commits.get().getValue1(), thirdFGCommit);
    // no commits in range
    commits = target.findEarliestAndLatestCommitsInRange(featuregroup, 12L, 14L);
    Assert.assertFalse(commits.isPresent());
  }
  
  @Test
  public void testGetStartEndCommitsByWindowTime() throws FeaturestoreException {
    Long originCommitTime = 0L; // if window start time is the beginning of a feature group, we set 0L.
    // no times provided
    Pair<Long, Long> commitTimes =
      target.getStartEndCommitTimesByWindowTime(featuregroup,null, null);
    Assert.assertEquals(commitTimes.getValue0(), originCommitTime);
    Assert.assertEquals(commitTimes.getValue1(), thirdFGCommit.getCommittedOn());
    // with end time only
    commitTimes = target.getStartEndCommitTimesByWindowTime(featuregroup,null, 5L);
    Assert.assertEquals(commitTimes.getValue0(), originCommitTime);
    Assert.assertEquals(commitTimes.getValue1(), secondFGCommit.getCommittedOn());
    // with different start and end times
    commitTimes = target.getStartEndCommitTimesByWindowTime(featuregroup,1L, 10L);
    Assert.assertEquals(commitTimes.getValue0(), firstFGCommit.getCommittedOn());
    Assert.assertEquals(commitTimes.getValue1(), thirdFGCommit.getCommittedOn());
    commitTimes = target.getStartEndCommitTimesByWindowTime(featuregroup,3L, 7L);
    Assert.assertEquals(commitTimes.getValue0(), secondFGCommit.getCommittedOn());
    Assert.assertEquals(commitTimes.getValue1(), secondFGCommit.getCommittedOn());
    commitTimes = target.getStartEndCommitTimesByWindowTime(featuregroup,5L, 12L);
    Assert.assertEquals(commitTimes.getValue0(), secondFGCommit.getCommittedOn());
    Assert.assertEquals(commitTimes.getValue1(), thirdFGCommit.getCommittedOn());
    // with equal start and end times
    commitTimes = target.getStartEndCommitTimesByWindowTime(featuregroup,5L, 5L);
    Assert.assertEquals(commitTimes.getValue0(), secondFGCommit.getCommittedOn());
    Assert.assertEquals(commitTimes.getValue1(), secondFGCommit.getCommittedOn());
    // no commits in range
    Assert.assertThrows(FeaturestoreException.class, () -> {
      target.getStartEndCommitTimesByWindowTime(featuregroup, 12L, 14L);
    });
  }
  
  private void setupFeatureGroupCommitController(Featuregroup featuregroup, FeatureGroupCommit firstFGCommit,
    FeatureGroupCommit secondFGCommit, FeatureGroupCommit thirdFGCommit) {
    Integer fgId = 1;
    Mockito.when(featuregroup.getId()).thenReturn(fgId);
    
    Mockito.when(firstFGCommit.getCommittedOn()).thenReturn(1L);
    Mockito.when(secondFGCommit.getCommittedOn()).thenReturn(5L);
    Mockito.when(thirdFGCommit.getCommittedOn()).thenReturn(10L);
    
    Mockito.when(featureGroupCommitFacade.findEarliestCommitInRange(fgId, null, null)).thenReturn(Optional.of(firstFGCommit));
    Mockito.when(featureGroupCommitFacade.findEarliestCommitInRange(fgId, null, 5L)).thenReturn(Optional.of(firstFGCommit));
    Mockito.when(featureGroupCommitFacade.findEarliestCommitInRange(fgId, null, 10L)).thenReturn(Optional.of(firstFGCommit));
    Mockito.when(featureGroupCommitFacade.findEarliestCommitInRange(fgId, 1L, 5L)).thenReturn(Optional.of(firstFGCommit));
    Mockito.when(featureGroupCommitFacade.findEarliestCommitInRange(fgId, 1L, 10L)).thenReturn(Optional.of(firstFGCommit));
    Mockito.when(featureGroupCommitFacade.findEarliestCommitInRange(fgId, 3L, 7L)).thenReturn(Optional.of(secondFGCommit));
    Mockito.when(featureGroupCommitFacade.findEarliestCommitInRange(fgId, 5L, 5L)).thenReturn(Optional.of(secondFGCommit));
    Mockito.when(featureGroupCommitFacade.findEarliestCommitInRange(fgId, 5L, 12L)).thenReturn(Optional.of(secondFGCommit));
    Mockito.when(featureGroupCommitFacade.findEarliestCommitInRange(fgId, 12L, 14L)).thenReturn(Optional.empty());
    
    Mockito.when(featureGroupCommitFacade.findLatestCommitInRange(fgId, null, null)).thenReturn(Optional.of(thirdFGCommit));
    Mockito.when(featureGroupCommitFacade.findLatestCommitInRange(fgId, null, 5L)).thenReturn(Optional.of(secondFGCommit));
    Mockito.when(featureGroupCommitFacade.findLatestCommitInRange(fgId, null, 10L)).thenReturn(Optional.of(thirdFGCommit));
    Mockito.when(featureGroupCommitFacade.findLatestCommitInRange(fgId, 1L, 5L)).thenReturn(Optional.of(secondFGCommit));
    Mockito.when(featureGroupCommitFacade.findLatestCommitInRange(fgId, 1L, 10L)).thenReturn(Optional.of(thirdFGCommit));
    Mockito.when(featureGroupCommitFacade.findLatestCommitInRange(fgId, 3L, 7L)).thenReturn(Optional.of(secondFGCommit));
    Mockito.when(featureGroupCommitFacade.findLatestCommitInRange(fgId, 5L, 5L)).thenReturn(Optional.of(secondFGCommit));
    Mockito.when(featureGroupCommitFacade.findLatestCommitInRange(fgId, 5L, 12L)).thenReturn(Optional.of(thirdFGCommit));
    Mockito.when(featureGroupCommitFacade.findLatestCommitInRange(fgId, 12L, 14L)).thenReturn(Optional.empty());
    
    Mockito.when(target.findFirstCommit(featuregroup)).thenReturn(Optional.of(firstFGCommit));
    
    Mockito.when(target.findCommitByDate(featuregroup, 1L)).thenReturn(Optional.of(firstFGCommit));
    Mockito.when(target.findCommitByDate(featuregroup, 3L)).thenReturn(Optional.of(firstFGCommit));
    Mockito.when(target.findCommitByDate(featuregroup, 5L)).thenReturn(Optional.of(secondFGCommit));
    Mockito.when(target.findCommitByDate(featuregroup, 7L)).thenReturn(Optional.of(secondFGCommit));
    Mockito.when(target.findCommitByDate(featuregroup, 10L)).thenReturn(Optional.of(thirdFGCommit));
    Mockito.when(target.findCommitByDate(featuregroup, 12L)).thenReturn(Optional.empty()); // no commit
    Mockito.when(target.findCommitByDate(featuregroup, null)).thenReturn(Optional.of(thirdFGCommit)); // last commit
  }
}
