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

package io.hops.hopsworks.common.featurestore.statistics.columns;

import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticColumn;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.StatisticsConfig;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the statistic_columns table and required business logic
 */
@Stateless
public class StatisticColumnController {
  
  @EJB
  private StatisticColumnFacade statisticColumnFacade;
  
  public void persistStatisticColumns(Featuregroup featureGroup, List<String> statisticColumns){
    persistStatisticColumns(featureGroup.getStatisticsConfig(), statisticColumns);
  }
  
  public void persistStatisticColumns(TrainingDataset trainingDataset, List<String> statisticColumns){
    persistStatisticColumns(trainingDataset.getStatisticsConfig(), statisticColumns);
  }
    
    /**
     * Insert a list of statistic columns as statistic column entities linked to a feature group if they don't exist
     * already and drop the ones not present anymore
     *
     * @param statisticsConfig the statistics config to link the columns to
     * @param statisticColumns the columns to insert
     */
  public void persistStatisticColumns(StatisticsConfig statisticsConfig, List<String> statisticColumns){
    if(statisticColumns != null){
      statisticColumns.forEach(statisticColumn -> {
        if(!isColumnExists((List) statisticsConfig.getStatisticColumns(), statisticColumn)) {
          StatisticColumn sc = new StatisticColumn();
          sc.setStatisticsConfig(statisticsConfig);
          sc.setName(statisticColumn);
          statisticColumnFacade.persist(sc);
        }
      });
      // drop all entities which are not in the list anymore
      List<StatisticColumn> columnEntities = (List) statisticsConfig.getStatisticColumns();
      columnEntities.forEach(scEntity -> {
        if(isEntityToBeDropped(scEntity, statisticColumns)) {
          statisticColumnFacade.remove(scEntity);
        }
      });
    }
  }
  
  /**
   * Check if a statistics column name exists in a list of statistic column entities
   *
   * @param statisticColumns
   * @param column
   * @return
   */
  public boolean isColumnExists(List<StatisticColumn> statisticColumns, String column) {
    return statisticColumns.stream().anyMatch(statisticColumn -> statisticColumn.getName().equals(column));
  }
  
  /**
   * Check if a statistic entity is not in a list of statistic column names, and therefore should be dropped from the
   * database
   *
   * @param entity
   * @param statisticColumns
   * @return
   */
  public boolean isEntityToBeDropped(StatisticColumn entity, List<String> statisticColumns) {
    return statisticColumns.stream().noneMatch(statisticColumn -> statisticColumn.equals(entity.getName()));
  }

  public void verifyStatisticColumnsExist(FeaturegroupDTO featureGroupDTO) throws FeaturestoreException {
    verifyStatisticColumnsExist(featureGroupDTO.getStatisticsConfig().getColumns(),
      featureGroupDTO.getFeatures().stream().map(FeatureGroupFeatureDTO::getName).collect(Collectors.toList()),
      "feature group " + featureGroupDTO.getName(), featureGroupDTO.getVersion());
  }
  
  public void verifyStatisticColumnsExist(FeaturegroupDTO featureGroupDTO, Featuregroup dbFeatureGroup,
                                          List<FeatureGroupFeatureDTO> dbFeatures) throws FeaturestoreException {
    verifyStatisticColumnsExist(featureGroupDTO.getStatisticsConfig().getColumns(),
      dbFeatures.stream().map(FeatureGroupFeatureDTO::getName).collect(Collectors.toList()),
      "feature group " + dbFeatureGroup.getName(), dbFeatureGroup.getVersion());
  }

  public void verifyStatisticColumnsExist(TrainingDatasetDTO trainingDatasetDTO, TrainingDataset trainingDataset)
    throws FeaturestoreException {
    verifyStatisticColumnsExist(trainingDatasetDTO.getStatisticsConfig().getColumns(),
      trainingDataset.getFeatures().stream().map(TrainingDatasetFeature::getName).collect(Collectors.toList()),
      "training dataset " + trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion());
  }

  public void verifyStatisticColumnsExist(TrainingDatasetDTO trainingDatasetDTO, Query query)
      throws FeaturestoreException {
    if (query != null) {
      verifyStatisticColumnsExist(trainingDatasetDTO.getStatisticsConfig().getColumns(),
        query.getFeatures().stream().map(Feature::getName).collect(Collectors.toList()),
        "training dataset " + trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion());
    } else {
      verifyStatisticColumnsExist(trainingDatasetDTO.getStatisticsConfig().getColumns(),
        trainingDatasetDTO.getFeatures().stream().map(TrainingDatasetFeatureDTO::getName).collect(Collectors.toList()),
        "training dataset " + trainingDatasetDTO.getName(), trainingDatasetDTO.getVersion());
    }
  }

  public void verifyStatisticColumnsExist(List<String> statisticColumns, List<String> featureNames, String entityName,
    Integer entityVersion)
    throws FeaturestoreException {
    if (statisticColumns != null) {
      // verify all statistic columns exist
      Optional<String> nonExistingStatColumn = statisticColumns.stream()
        .filter(statCol -> !featureNames.contains(statCol)).findAny();
      if (nonExistingStatColumn.isPresent()) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STATISTICS_CONFIG,
          Level.FINE, "statistic column: " + nonExistingStatColumn.get() + " is not part of "
          + entityName + " with version " + entityVersion);
      }
    }
  }
}
