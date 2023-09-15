/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.multiregion;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

public class TestMultiRegionWatchdog {

  @Mock
  private MultiRegionWatchdog multiRegionWatchdog;

  @Mock
  private MultiRegionConfiguration multiRegionConfiguration;

  @Before
  public void before() throws Exception {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void pollJudgeSuccessPrimary() throws IOException {
    // Setup
    Mockito.when(multiRegionWatchdog.pollJudge()).thenReturn(new MultiRegionWatchdogDTO("regionA"));
    Mockito.doCallRealMethod().when(multiRegionWatchdog).updateWatchdog(Mockito.any());
    Mockito.doCallRealMethod().when(multiRegionWatchdog).setMultiRegionController(Mockito.any());
    Mockito.doCallRealMethod().when(multiRegionWatchdog).setMultiRegionConfiguration(Mockito.any());

    MultiRegionController multiRegionController = new MultiRegionController();
    multiRegionWatchdog.setMultiRegionController(multiRegionController);

    Mockito.when(multiRegionConfiguration.getString(MultiRegionConfiguration.MultiRegionConfKeys.MULTIREGION_WATCHDOG_REGION))
        .thenReturn("regionA");
    multiRegionWatchdog.setMultiRegionConfiguration(multiRegionConfiguration);

    // Act
    multiRegionWatchdog.updateWatchdog(null);

    // Verify
    Assert.assertTrue(multiRegionController.isPrimaryRegion());
  }

  @Test
  public void pollJudgeSuccessSecondary() throws IOException {
    // Setup
    Mockito.when(multiRegionWatchdog.pollJudge()).thenReturn(new MultiRegionWatchdogDTO("regionA"));
    Mockito.doCallRealMethod().when(multiRegionWatchdog).updateWatchdog(Mockito.any());
    Mockito.doCallRealMethod().when(multiRegionWatchdog).setMultiRegionController(Mockito.any());
    Mockito.doCallRealMethod().when(multiRegionWatchdog).setMultiRegionConfiguration(Mockito.any());

    MultiRegionController multiRegionController = new MultiRegionController();
    multiRegionWatchdog.setMultiRegionController(multiRegionController);

    Mockito.when(multiRegionConfiguration.getString(MultiRegionConfiguration.MultiRegionConfKeys.MULTIREGION_WATCHDOG_REGION))
        .thenReturn("regionB");
    multiRegionWatchdog.setMultiRegionConfiguration(multiRegionConfiguration);

    // Act
    multiRegionWatchdog.updateWatchdog(null);

    // Verify
    Assert.assertFalse(multiRegionController.isPrimaryRegion());
  }

  @Test
  public void pollJudgeFailure() throws IOException {
    // Setup
    Mockito.when(multiRegionWatchdog.pollJudge()).thenThrow(new IOException());
    Mockito.doCallRealMethod().when(multiRegionWatchdog).updateWatchdog(Mockito.any());
    Mockito.doCallRealMethod().when(multiRegionWatchdog).setMultiRegionController(Mockito.any());
    Mockito.doCallRealMethod().when(multiRegionWatchdog).setMultiRegionConfiguration(Mockito.any());

    MultiRegionController multiRegionController = new MultiRegionController();
    multiRegionController.setPrimaryRegion(true);
    multiRegionWatchdog.setMultiRegionController(multiRegionController);

    Mockito.when(multiRegionConfiguration.getString(MultiRegionConfiguration.MultiRegionConfKeys.MULTIREGION_WATCHDOG_REGION))
        .thenReturn("regionB");
    multiRegionWatchdog.setMultiRegionConfiguration(multiRegionConfiguration);

    // Act
    multiRegionWatchdog.updateWatchdog(null);

    // Verify
    Assert.assertFalse(multiRegionController.isPrimaryRegion());
  }
}
