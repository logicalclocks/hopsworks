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

import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.statistics.columns.StatisticColumn;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;

/**
 * Class controlling the interaction with the statistic_columns table and required business logic
 */
@Stateless
public class StatisticColumnController {
  
  @EJB
  private StatisticColumnFacade statisticColumnFacade;
  
  /**
   * Insert a list of statistic columns as statistic column entities linked to a feature group if they don't exist
   * already and drop the ones not present anymore
   *
   * @param featuregroup the featuregroup to link the jobs to
   * @param statisticColumns the columns to insert
   */
  public void persistStatisticColumns(Featuregroup featuregroup, List<String> statisticColumns){
    if(statisticColumns != null){
      statisticColumns.forEach(statisticColumn -> {
        if(!isColumnExists((List) featuregroup.getStatisticColumns(), statisticColumn)) {
          StatisticColumn sc = new StatisticColumn();
          sc.setFeaturegroup(featuregroup);
          sc.setName(statisticColumn);
          statisticColumnFacade.persist(sc);
        }
      });
      // drop all entities which are not in the list anymore
      List<StatisticColumn> columnEntities = (List) featuregroup.getStatisticColumns();
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
}
