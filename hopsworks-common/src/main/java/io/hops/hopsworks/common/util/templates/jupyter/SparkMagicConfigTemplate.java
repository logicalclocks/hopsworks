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

package io.hops.hopsworks.common.util.templates.jupyter;

public class SparkMagicConfigTemplate extends JupyterTemplate {
  public static final String TEMPLATE_NAME = "config_template.json";
  public static final String FILE_NAME = "config.json";
  
  private final String livyIp;
  private final String jupyterHome;
  private final Integer driverCores;
  private final String driverMemory;
  private final Integer numExecutors;
  private final Integer executorCores;
  private final String executorMemory;
  private final String yarnQueue;
  private final String hadoopVersion;
  private final String sparkConfiguration;
  private final Integer livyStartupTimeout;
  
  public SparkMagicConfigTemplate(SparkMagicConfigTemplateBuilder builder) {
    super(builder.getHdfsUser(), builder.getHadoopHome(), null);
    this.livyIp = builder.getLivyIp();
    this.jupyterHome = builder.getJupyterHome();
    this.driverCores = builder.getDriverCores();
    this.driverMemory = builder.getDriverMemory();
    this.numExecutors = builder.getNumExecutors();
    this.executorCores = builder.getExecutorCores();
    this.executorMemory = builder.getExecutorMemory();
    this.yarnQueue = builder.getYarnQueue();
    this.hadoopVersion = builder.getHadoopVersion();
    this.sparkConfiguration = builder.getSparkConfiguration();
    this.livyStartupTimeout = builder.getLivyStartupTimeout();
  }
  
  public String getLivyIp() {
    return livyIp;
  }
  
  public String getJupyterHome() {
    return jupyterHome;
  }
  
  public Integer getDriverCores() {
    return driverCores;
  }
  
  public String getDriverMemory() {
    return driverMemory;
  }
  
  public Integer getNumExecutors() {
    return numExecutors;
  }
  
  public Integer getExecutorCores() {
    return executorCores;
  }
  
  public String getExecutorMemory() {
    return executorMemory;
  }
  
  public String getYarnQueue() {
    return yarnQueue;
  }
  
  public String getHadoopVersion() {
    return hadoopVersion;
  }
  
  public String getSparkConfiguration() {
    return sparkConfiguration;
  }
  
  public Integer getLivyStartupTimeout() {
    return livyStartupTimeout;
  }
}
