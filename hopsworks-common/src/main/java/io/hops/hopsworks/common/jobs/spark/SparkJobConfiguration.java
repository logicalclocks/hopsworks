/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.hops.hopsworks.common.jobs.spark;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJobConfiguration;
import io.hops.hopsworks.common.util.Settings;

/**
 * Contains Spark-specific run information for a Spark job, on top of Yarn configuration.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class SparkJobConfiguration extends YarnJobConfiguration {
  
  public SparkJobConfiguration() {
  }
  
  @XmlElement
  private String appPath;
  @XmlElement
  private String mainClass;
  @XmlElement
  private String args;
  @XmlElement
  private String properties;

  @XmlElement(name="spark.executor.instances")
  private int executorInstances = 1;

  @XmlElement(name="spark.executor.cores")
  private int executorCores = 1;

  @XmlElement(name="spark.executor.memory")
  private int executorMemory = 1024;

  @XmlElement(name="spark.executor.gpus")
  private int executorGpus = 0;

  @XmlElement(name="spark.dynamicAllocation.enabled")
  private boolean dynamicAllocationEnabled = false;

  @XmlElement(name="spark.dynamicAllocation.minExecutors")
  private int dynamicAllocationMinExecutors = Settings.SPARK_MIN_EXECS;

  @XmlElement(name="spark.dynamicAllocation.maxExecutors")
  private int dynamicAllocationMaxExecutors = Settings.SPARK_MAX_EXECS;

  @XmlElement(name="spark.dynamicAllocation.initialExecutors")
  private int dynamicAllocationInitialExecutors = Settings.SPARK_MIN_EXECS;

  public String getAppPath() {
    return appPath;
  }

  public void setAppPath(String appPath) {
    this.appPath = appPath;
  }

  public String getMainClass() {
    return mainClass;
  }

  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  public String getArgs() {
    return args;
  }

  public void setArgs(String args) {
    this.args = args;
  }

  public String getProperties() {
    return properties;
  }

  public void setProperties(String properties) {
    this.properties = properties;
  }

  public int getExecutorInstances() {
    return executorInstances;
  }

  public void setExecutorInstances(int executorInstances) {
    this.executorInstances = executorInstances;
  }

  public int getExecutorCores() {
    return executorCores;
  }

  public void setExecutorCores(int executorCores) {
    this.executorCores = executorCores;
  }

  public int getExecutorMemory() {
    return executorMemory;
  }

  public void setExecutorMemory(int executorMemory) {
    this.executorMemory = executorMemory;
  }

  public int getExecutorGpus() {
    return executorGpus;
  }

  public void setExecutorGpus(int executorGpus) {
    this.executorGpus = executorGpus;
  }

  public boolean isDynamicAllocationEnabled() {
    return dynamicAllocationEnabled;
  }

  public void setDynamicAllocationEnabled(boolean dynamicAllocationEnabled) {
    this.dynamicAllocationEnabled = dynamicAllocationEnabled;
  }

  public int getDynamicAllocationMinExecutors() {
    return dynamicAllocationMinExecutors;
  }

  public void setDynamicAllocationMinExecutors(int dynamicAllocationMinExecutors) {
    this.dynamicAllocationMinExecutors = dynamicAllocationMinExecutors;
  }

  public int getDynamicAllocationMaxExecutors() {
    return dynamicAllocationMaxExecutors;
  }

  public void setDynamicAllocationMaxExecutors(int dynamicAllocationMaxExecutors) {
    this.dynamicAllocationMaxExecutors = dynamicAllocationMaxExecutors;
  }

  public int getDynamicAllocationInitialExecutors() {
    return dynamicAllocationInitialExecutors;
  }

  public void setDynamicAllocationInitialExecutors(int dynamicAllocationInitialExecutors) {
    this.dynamicAllocationInitialExecutors = dynamicAllocationInitialExecutors;
  }

  @Override
  @XmlElement(name="jobType")
  public JobType getJobType() {
    if (this.mainClass == null) {
      return null;
    } else if(this.mainClass.equals(Settings.SPARK_PY_MAINCLASS)) {
      return JobType.PYSPARK;
    } else {
      return JobType.SPARK;
    }
  }
}
