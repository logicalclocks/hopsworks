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

package io.hops.hopsworks.common.featurestore.statistics.columns;

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.statistics.StatisticsConfigDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticColumn;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticsConfig;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;

public class TestStatisticColumnController {
  
  private StatisticColumnController statisticColumnController;
  
  @Rule
  public ExpectedException thrown = ExpectedException.none();
  
  @Before
  public void setup() {
    statisticColumnController = new StatisticColumnController();
  }
  
  @Test
  public void testIsColumnExists() {
    StatisticsConfig statisticsConfig = new StatisticsConfig();
    List<StatisticColumn> statisticColumnList = Arrays.asList(new StatisticColumn(statisticsConfig, "ft1"),
      new StatisticColumn(statisticsConfig, "ft2"));
    
    Assert.assertTrue(statisticColumnController.isColumnExists(statisticColumnList, "ft1"));
    Assert.assertFalse(statisticColumnController.isColumnExists(statisticColumnList, "ft3"));
  }
  
  @Test
  public void testIsEntityToBeDropped() {
    StatisticsConfig statisticsConfig = new StatisticsConfig();
    StatisticColumn toDrop = new StatisticColumn(statisticsConfig, "ft1");
    StatisticColumn notToDrop = new StatisticColumn(statisticsConfig, "ft2");
    List<String> columns = Arrays.asList("ft2", "ft3");
    
    Assert.assertTrue(statisticColumnController.isEntityToBeDropped(toDrop, columns));
    Assert.assertFalse(statisticColumnController.isEntityToBeDropped(notToDrop, columns));
  }
  
  @Test
  public void testVerifyStatisticColumnsExistFg() throws Exception {
    FeaturegroupDTO featuregroupDTO = new FeaturegroupDTO();
    featuregroupDTO.setName("fg1");
    featuregroupDTO.setVersion(1);
    StatisticsConfigDTO statisticsConfig = new StatisticsConfigDTO();
    featuregroupDTO.setStatisticsConfig(statisticsConfig);
    statisticsConfig.setColumns(Arrays.asList("ft1", "ft4"));
    featuregroupDTO.setFeatures(Arrays.asList(new FeatureGroupFeatureDTO("ft1", null, ""),
      new FeatureGroupFeatureDTO("ft2", null, ""), new FeatureGroupFeatureDTO("ft3", null, "")));
    
    // should throw exception
    thrown.expect(FeaturestoreException.class);
    statisticColumnController.verifyStatisticColumnsExist(featuregroupDTO);
    
    // should not throw exception
    statisticsConfig.setColumns(Arrays.asList("ft1", "ft2"));
    statisticColumnController.verifyStatisticColumnsExist(featuregroupDTO);
  }
  
  @Test
  public void testVerifyStatisticColumnsExistTD() throws Exception {
    TrainingDataset trainingDataset = new TrainingDataset();
    StatisticsConfigDTO statisticsConfig = new StatisticsConfigDTO();
    statisticsConfig.setColumns(Arrays.asList("ft1", "ft4"));
    trainingDataset.setFeatures(Arrays.asList(new TrainingDatasetFeature(trainingDataset, "ft1", null, null, false),
      new TrainingDatasetFeature(trainingDataset, "ft2", null, null, false),
      new TrainingDatasetFeature(trainingDataset, "ft3", null, null, false)));
    TrainingDatasetDTO trainingDatasetDTO = new TrainingDatasetDTO();
    trainingDatasetDTO.setName("td1");
    trainingDatasetDTO.setVersion(1);
    trainingDatasetDTO.setStatisticsConfig(statisticsConfig);
    
    // should throw exception
    thrown.expect(FeaturestoreException.class);
    statisticColumnController.verifyStatisticColumnsExist(trainingDatasetDTO, trainingDataset);
    
    // should not throw exception
    statisticsConfig.setColumns(Arrays.asList("ft1", "ft2"));
    statisticColumnController.verifyStatisticColumnsExist(trainingDatasetDTO, trainingDataset);
  }
}
