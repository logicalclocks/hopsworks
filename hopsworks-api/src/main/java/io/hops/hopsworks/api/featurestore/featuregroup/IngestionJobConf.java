/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.featuregroup;

import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.IngestionDataFormat;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;

import java.util.List;

public class IngestionJobConf {
  private IngestionDataFormat dataFormat = IngestionDataFormat.CSV;
  private List<OptionDTO> dataOptions;
  private List<OptionDTO> writeOptions;

  private SparkJobConfiguration sparkJobConfiguration;

  public IngestionJobConf() {
  }

  public IngestionDataFormat getDataFormat() {
    return dataFormat;
  }

  public void setDataFormat(IngestionDataFormat dataFormat) {
    this.dataFormat = dataFormat;
  }

  public List<OptionDTO> getDataOptions() {
    return dataOptions;
  }

  public void setDataOptions(List<OptionDTO> dataOptions) {
    this.dataOptions = dataOptions;
  }

  public List<OptionDTO> getWriteOptions() {
    return writeOptions;
  }

  public void setWriteOptions(List<OptionDTO> writeOptions) {
    this.writeOptions = writeOptions;
  }

  public SparkJobConfiguration getSparkJobConfiguration() {
    return sparkJobConfiguration;
  }

  public void setSparkJobConfiguration(SparkJobConfiguration sparkJobConfiguration) {
    this.sparkJobConfiguration = sparkJobConfiguration;
  }
}
