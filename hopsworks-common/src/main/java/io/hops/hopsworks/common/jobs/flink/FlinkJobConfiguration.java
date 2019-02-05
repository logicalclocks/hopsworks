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

package io.hops.hopsworks.common.jobs.flink;

import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJobConfiguration;
import io.hops.hopsworks.common.util.Settings;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Contains Flink-specific run information for a Flink job, on top of Yarn
 * configuration.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class FlinkJobConfiguration extends YarnJobConfiguration {

  @XmlElement
  private String jarPath;
  @XmlElement
  private String mainClass;
  @XmlElement
  private String args;
  @XmlElement
  private String flinkConfDir;
  @XmlElement
  private String flinkConfFile;

  @XmlElement
  private int numberOfTaskManagers = 1;
  @XmlElement
  private int slots = 1;
  @XmlElement
  private int taskManagerMemory = 768;
  @XmlElement
  private int parallelism = 1;
  @XmlElement
  private String flinkjobtype;

  public FlinkJobConfiguration() {
    super();
    super.setAmMemory(Settings.FLINK_APP_MASTER_MEMORY);
  }

  /**
   * Get the path to the main executable jar.
   *
   * @return
   */
  public String getJarPath() {
    return jarPath;
  }

  /**
   * Set the path to the main executable jar. No default value.
   * <p/>
   * @param jarPath
   */
  public void setJarPath(String jarPath) {
    this.jarPath = jarPath;
  }

  /**
   * Get the name of the main class to be executed.
   *
   * @return
   */
  public String getMainClass() {
    return mainClass;
  }

  /**
   * Set the name of the main class to be executed. No default value.
   * <p/>
   * @param mainClass
   */
  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  public String getArgs() {
    return args;
  }

  /**
   * Set the arguments to be passed to the job. No default value.
   * <p/>
   * @param args
   */
  public void setArgs(String args) {
    this.args = args;
  }

  public int getNumberOfTaskManagers() {
    return numberOfTaskManagers;
  }

  /**
   * Set the number of task managers to be requested for this job. This should
   * be greater than or equal to 1.
   * <p/>
   * @param numberOfTaskManagers
   * @throws IllegalArgumentException If the argument is smaller than 1.
   */
  public void setNumberOfTaskManagers(int numberOfTaskManagers) throws
          IllegalArgumentException {
    if (numberOfTaskManagers < 1) {
      throw new IllegalArgumentException(
              "Number of task managers has to be greater than or equal to 1.");
    }
    this.numberOfTaskManagers = numberOfTaskManagers;
  }

  public int getSlots() {
    return slots;
  }

  /**
   * Set the number of slots to be requested for each task manager.
   * <p/>
   * @param slots
   * @throws IllegalArgumentException If the number of slots is smaller than
   * 1.
   */
  public void setSlots(int slots) throws
          IllegalArgumentException {
    if (slots < 1) {
      throw new IllegalArgumentException(
              "Number of task manager slots has to be greater than or equal to 1.");
    }
    this.slots = slots;
  }

  public int getTaskManagerMemory() {
    return taskManagerMemory;
  }

  /**
   * Set the memory requested for each executor in MB.
   * <p/>
   * @param taskManagerMemory
   * @throws IllegalArgumentException If the given value is not strictly
   * positive.
   */
  public void setTaskManagerMemory(int taskManagerMemory) throws
          IllegalArgumentException {
    if (taskManagerMemory < 1) {
      throw new IllegalArgumentException(
              "TaskManager memory must be greater than 1MB.");
    }
    this.taskManagerMemory = taskManagerMemory;
  }

  public String getFlinkjobtype() {
    return flinkjobtype;
  }

  public void setFlinkjobtype(String flinkjobtype) {
    this.flinkjobtype = flinkjobtype;
  }

  public int getParallelism() {
    return parallelism;
  }

  public void setParallelism(int parallelism) {
    this.parallelism = parallelism;
  }

  /**
   * Get Flink Configuration Directory
   *
   * @return
   */
  public String getFlinkConfDir() {
    return flinkConfDir;
  }

  /**
   * Set Flink Configuration Directory
   *
   * @param flinkConfDir
   */
  public void setFlinkConfDir(String flinkConfDir) {
    this.flinkConfDir = flinkConfDir;
  }

  /**
   * Get Flink Configuration file path
   *
   * @return
   */
  public String getFlinkConfFile() {
    return flinkConfFile;
  }

  /**
   * Set Flink Configuration file path
   *
   * @param flinkConfFile
   */
  public void setFlinkConfFile(String flinkConfFile) {
    this.flinkConfFile = flinkConfFile;
  }

  @Override
  @XmlElement(name="jobType")
  public JobType getJobType() {
    return JobType.FLINK;
  }
}
