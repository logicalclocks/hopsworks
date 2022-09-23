/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.featuregroup;

import io.hops.hopsworks.common.featurestore.statistics.StatisticsConfigDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import java.util.ArrayList;

public class ImportFgJobConf {

  private String featureGroupName;
  private ArrayList<String> primaryKey;
  private ArrayList<String> partitionKey;
  private String query;
  private boolean onlineEnabled;
  private FeaturestoreStorageConnectorDTO storageConnectorDTO;
  private String description;
  private String eventTime;
  private String table;
  private StatisticsConfigDTO statisticsConfigDTO;
  
  public ImportFgJobConf() {
  }
  
  public StatisticsConfigDTO getStatisticsConfigDTO() {
    return statisticsConfigDTO;
  }
  
  public void setStatisticsConfigDTO(StatisticsConfigDTO statisticsConfigDTO) {
    this.statisticsConfigDTO = statisticsConfigDTO;
  }
  
  public FeaturestoreStorageConnectorDTO getStorageConnectorDTO() {
    return storageConnectorDTO;
  }
  
  public void setStorageConnectorDTO(
    FeaturestoreStorageConnectorDTO storageConnectorDTO) {
    this.storageConnectorDTO = storageConnectorDTO;
  }
  
  public String getDescription() {
    return description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getEventTime() {
    return eventTime;
  }
  
  public void setEventTime(String eventTime) {
    this.eventTime = eventTime;
  }
  
  public String getTable() {
    return table;
  }
  
  public void setTable(String table) {
    this.table = table;
  }
  
  public String getFeatureGroupName() {
    return featureGroupName;
  }

  public void setFeatureGroupName(String featureGroupName) {
    this.featureGroupName = featureGroupName;
  }

  public ArrayList<String> getPrimaryKey() {
    return primaryKey;
  }

  public void setPrimaryKey(ArrayList<String> primaryKey) {
    this.primaryKey = primaryKey;
  }

  public ArrayList<String> getPartitionKey() {
    return partitionKey;
  }

  public void setPartitionKey(ArrayList<String> partitionKey) {
    this.partitionKey = partitionKey;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public boolean isOnlineEnabled() {
    return onlineEnabled;
  }

  public void setOnlineEnabled(boolean onlineEnabled) {
    this.onlineEnabled = onlineEnabled;
  }
  
}
