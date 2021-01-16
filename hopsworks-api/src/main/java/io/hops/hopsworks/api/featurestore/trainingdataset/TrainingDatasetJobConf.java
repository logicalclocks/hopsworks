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

package io.hops.hopsworks.api.featurestore.trainingdataset;

import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;

import java.util.List;

public class TrainingDatasetJobConf {
  private QueryDTO query;
  private Boolean overwrite;
  private SparkJobConfiguration sparkJobConfiguration;
  private List<OptionDTO> writeOptions;

  public TrainingDatasetJobConf() {}

  public QueryDTO getQuery() {
    return query;
  }

  public void setQuery(QueryDTO query) {
    this.query = query;
  }

  public Boolean getOverwrite() {
    return overwrite;
  }

  public void setOverwrite(Boolean overwrite) {
    this.overwrite = overwrite;
  }

  public SparkJobConfiguration getSparkJobConfiguration() {
    return sparkJobConfiguration;
  }

  public void setSparkJobConfiguration(SparkJobConfiguration sparkJobConfiguration) {
    this.sparkJobConfiguration = sparkJobConfiguration;
  }

  public List<OptionDTO> getWriteOptions() {
    return writeOptions;
  }

  public void setWriteOptions(List<OptionDTO> writeOptions) {
    this.writeOptions = writeOptions;
  }
}
