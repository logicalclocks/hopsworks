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

public class SparkMagicConfigTemplateBuilder {
  private String hdfsUser;
  private String hadoopHome;
  
  private String livyIp;
  private String jupyterHome;
  private Integer driverCores;
  private String driverMemory;
  private Integer numExecutors;
  private Integer executorCores;
  private String executorMemory;
  private String yarnQueue;
  private String hadoopVersion;
  private String sparkConfiguration;
  private Integer livyStartupTimeout;
  
  private SparkMagicConfigTemplateBuilder() {}
  
  public static SparkMagicConfigTemplateBuilder newBuilder() {
    return new SparkMagicConfigTemplateBuilder();
  }
  
  public String getHdfsUser() {
    return hdfsUser;
  }
  
  public SparkMagicConfigTemplateBuilder setHdfsUser(String hdfsUser) {
    this.hdfsUser = hdfsUser;
    return this;
  }
  
  public String getHadoopHome() {
    return hadoopHome;
  }
  
  public SparkMagicConfigTemplateBuilder setHadoopHome(String hadoopHome) {
    this.hadoopHome = hadoopHome;
    return this;
  }
  
  public String getLivyIp() {
    return livyIp;
  }
  
  public SparkMagicConfigTemplateBuilder setLivyIp(String livyIp) {
    this.livyIp = livyIp;
    return this;
  }
  
  public SparkMagicConfigTemplateBuilder setLivyStartupTimeout(Integer livyStartupTimeout) {
    this.livyStartupTimeout = livyStartupTimeout;
    return this;
  }
  
  public Integer getLivyStartupTimeout() {
    return livyStartupTimeout;
  }
  
  public String getJupyterHome() {
    return jupyterHome;
  }
  
  public SparkMagicConfigTemplateBuilder setJupyterHome(String jupyterHome) {
    this.jupyterHome = jupyterHome;
    return this;
  }
  
  public Integer getDriverCores() {
    return driverCores;
  }
  
  public SparkMagicConfigTemplateBuilder setDriverCores(Integer driverCores) {
    this.driverCores = driverCores;
    return this;
  }
  
  public String getDriverMemory() {
    return driverMemory;
  }
  
  public SparkMagicConfigTemplateBuilder setDriverMemory(String driverMemory) {
    this.driverMemory = driverMemory;
    return this;
  }
  
  public Integer getNumExecutors() {
    return numExecutors;
  }
  
  public SparkMagicConfigTemplateBuilder setNumExecutors(Integer numExecutors) {
    this.numExecutors = numExecutors;
    return this;
  }
  
  public Integer getExecutorCores() {
    return executorCores;
  }
  
  public SparkMagicConfigTemplateBuilder setExecutorCores(Integer executorCores) {
    this.executorCores = executorCores;
    return this;
  }
  
  public String getExecutorMemory() {
    return executorMemory;
  }
  
  public SparkMagicConfigTemplateBuilder setExecutorMemory(String executorMemory) {
    this.executorMemory = executorMemory;
    return this;
  }
  
  public String getYarnQueue() {
    return yarnQueue;
  }
  
  public SparkMagicConfigTemplateBuilder setYarnQueue(String yarnQueue) {
    this.yarnQueue = yarnQueue;
    return this;
  }
  
  public String getHadoopVersion() {
    return hadoopVersion;
  }
  
  public SparkMagicConfigTemplateBuilder setHadoopVersion(String hadoopVersion) {
    this.hadoopVersion = hadoopVersion;
    return this;
  }
  
  public String getSparkConfiguration() {
    return sparkConfiguration;
  }
  
  public SparkMagicConfigTemplateBuilder setSparkConfiguration(String sparkConfiguration) {
    this.sparkConfiguration = sparkConfiguration;
    return this;
  }
  
  public SparkMagicConfigTemplate build() {
    return new SparkMagicConfigTemplate(this);
  }
}
