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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.Set;
import java.util.logging.Logger;

@NoArgsConstructor
@Getter
@Setter
public class StatisticsFilters {
  
  private static final Logger LOGGER = Logger.getLogger(StatisticsFilters.class.getName());
  
  private Long computationTime;
  private Long windowStartCommitTime;
  private Long windowEndCommitTime;
  private Float rowPercentage;
  private Boolean beforeTransformation;
  private Integer transformedWithVersion;
  
  public StatisticsFilters(Set<AbstractFacade.FilterBy> filters) {
    // default values
    rowPercentage = Float.valueOf(Filters.ROW_PERCENTAGE_EQ.getDefaultParam());
    beforeTransformation = Boolean.valueOf(Filters.BEFORE_TRANSFORMATION_EQ.getDefaultParam());
    // parse filters
    if (filters != null) {
      for (AbstractFacade.FilterBy filter : filters) {
        if (filter.getValue().startsWith("COMPUTATION_TIME_")) {
          computationTime = Long.valueOf(filter.getParam());
        } else if (filter.getValue().startsWith("WINDOW_START_COMMIT_TIME_")) {
          windowStartCommitTime = Long.valueOf(filter.getParam());
        } else if (filter.getValue().startsWith("WINDOW_END_COMMIT_TIME_")) {
          windowEndCommitTime = Long.valueOf(filter.getParam());
        } else if (filter.getValue().startsWith(Filters.ROW_PERCENTAGE_EQ.getValue())) {
          rowPercentage = Float.valueOf(filter.getParam());
        } else if (filter.getValue().startsWith(Filters.BEFORE_TRANSFORMATION_EQ.getValue())) {
          beforeTransformation = Boolean.valueOf(filter.getParam());
        }
      }
    }
  }
  
  public static void setFilter(Set<? extends AbstractFacade.FilterBy> filter, Query q) {
    if (filter == null || filter.isEmpty()) {
      return;
    }
    Object paramValue;
    for (AbstractFacade.FilterBy aFilter : filter) {
      if (aFilter.getValue().startsWith("COMPUTATION_TIME_")) {
        // computation time is parsed as timestamp
        paramValue = new Timestamp(Long.parseLong(aFilter.getParam()));
      } else if (aFilter.getValue().equals(Filters.ROW_PERCENTAGE_EQ.getValue())) {
        // row percentage is parsed as integer
        paramValue = Float.parseFloat(aFilter.getParam());
      } else if (aFilter.getValue().startsWith("WINDOW_")) {
        // window start/end commit/events are parsed as long type
        paramValue = Long.parseLong(aFilter.getParam());
      } else if (aFilter.getValue().equals(Filters.BEFORE_TRANSFORMATION_EQ.getValue())) {
        // before transformation is parsed as boolean
        paramValue = Boolean.parseBoolean(aFilter.getParam());
      } else {
        paramValue = aFilter.getParam();
      }
      q.setParameter(aFilter.getField(), paramValue);
    }
  }
  
  public enum Sorts {
    COMPUTATION_TIME("COMPUTATION_TIME", "s.computationTime ", "DESC"),
    WINDOW_START_COMMIT_TIME("WINDOW_START_COMMIT_TIME", "s.windowStartCommitTime ", "ASC"),
    WINDOW_END_COMMIT_TIME("WINDOW_END_COMMIT_TIME", "s.windowEndCommitTime ", "DESC");
    
    private final String value;
    private final String sql;
    private final String defaultParam;
    
    Sorts(String value, String sql, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.defaultParam = defaultParam;
    }
    
    public String getValue() {
      return value;
    }
    
    public String getDefaultParam() {
      return defaultParam;
    }
    
    public String getSql() {
      return sql;
    }
    
    @Override
    public String toString() {
      return value;
    }
  }
  
  public enum Filters {
    ROW_PERCENTAGE_EQ("ROW_PERCENTAGE_EQ", "s.rowPercentage = :rowPercentage", "rowPercentage", "1.0"),
    // non-time-travel-enabled feature group, feature view and training dataset statistics
    COMPUTATION_TIME_GT("COMPUTATION_TIME_GT", "s.computationTime > :computationTime", "computationTime", ""),
    COMPUTATION_TIME_LT("COMPUTATION_TIME_LT", "s.computationTime < :computationTime", "computationTime", ""),
    COMPUTATION_TIME_EQ("COMPUTATION_TIME_EQ", "s.computationTime = :computationTime", "computationTime", ""),
    COMPUTATION_TIME_LTOEQ("COMPUTATION_TIME_LTOEQ", "s.computationTime <= :computationTime", "computationTime", ""),
    // commit time window
    WINDOW_START_COMMIT_TIME_GTOEQ("WINDOW_START_COMMIT_TIME_GTOEQ",
      "s.windowStartCommitTime >= :windowStartCommitTime", "windowStartCommitTime", ""),
    WINDOW_START_COMMIT_TIME_EQ("WINDOW_START_COMMIT_TIME_EQ",
      "s.windowStartCommitTime = :windowStartCommitTime", "windowStartCommitTime", ""),
    WINDOW_END_COMMIT_TIME_LTOEQ("WINDOW_END_COMMIT_TIME_LTOEQ",
      "s.windowEndCommitTime <= :windowEndCommitTime", "windowEndCommitTime", ""),
    WINDOW_END_COMMIT_TIME_EQ("WINDOW_END_COMMIT_TIME_EQ",
      "s.windowEndCommitTime = :windowEndCommitTime", "windowEndCommitTime", ""),
    // before transformation
    BEFORE_TRANSFORMATION_EQ("BEFORE_TRANSFORMATION_EQ", "s.beforeTransformation = :beforeTransformation",
      "beforeTransformation", "false");
    
    
    private final String value;
    private final String sql;
    private final String field;
    private final String defaultParam;
    
    private Filters(String value, String sql, String field, String defaultParam) {
      this.value = value;
      this.sql = sql;
      this.field = field;
      this.defaultParam = defaultParam;
    }
    
    public String getValue() {
      return value;
    }
    
    public String getDefaultParam() {
      return defaultParam;
    }
    
    public String getSql() {
      return sql;
    }
    
    public String getField() {
      return field;
    }
    
    @Override
    public String toString() {
      return value;
    }
  }
}

