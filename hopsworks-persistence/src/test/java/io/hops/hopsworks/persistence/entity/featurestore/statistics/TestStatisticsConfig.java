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

package io.hops.hopsworks.persistence.entity.featurestore.statistics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class TestStatisticsConfig {

  // Test for FSTORE-814
  @Test
  public void testHashCodeWithColumns() {
    StatisticsConfig statisticsConfig = new StatisticsConfig(true, true, true, true);

    StatisticColumn statisticColumn = new StatisticColumn(statisticsConfig, "test");
    statisticColumn.setId(1);

    statisticsConfig.setId(1);
    statisticsConfig.setStatisticColumns(Arrays.asList(statisticColumn));

    // This should not throw an exception
    statisticsConfig.hashCode();
  }

  @Test
  public void testEqualsWithColumns() {
    StatisticsConfig statisticsConfig1 = new StatisticsConfig(true, true, true, true);
    StatisticColumn statisticColumn1 = new StatisticColumn(statisticsConfig1, "test");
    statisticColumn1.setId(1);
    statisticsConfig1.setId(1);
    statisticsConfig1.setStatisticColumns(Arrays.asList(statisticColumn1));

    StatisticsConfig statisticsConfig2 = new StatisticsConfig(true, true, true, true);
    StatisticColumn statisticColumn2 = new StatisticColumn(statisticsConfig2, "test");
    statisticColumn2.setId(1);
    statisticsConfig2.setId(1);
    statisticsConfig2.setStatisticColumns(Arrays.asList(statisticColumn2));

    Assert.assertTrue(statisticsConfig1.equals(statisticsConfig2));
  }
}
