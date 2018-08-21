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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.jobs.MutableJsonObject;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJobConfiguration;
import io.hops.hopsworks.common.util.Settings;

/**
 * Contains Flink-specific run information for a Flink job, on top of Yarn
 * configuration.
 */
public class FlinkJobConfiguration extends YarnJobConfiguration {

  private String jarPath;
  private String mainClass;
  private String args;
  private String flinkConfDir;
  private String flinkConfFile;

  private int numberOfTaskManagers = 1;
  private int slots = 1;
  private int taskManagerMemory = 768;
  private int parallelism = 1;
  private String flinkjobtype;

  protected static final String KEY_JARPATH = "JARPATH";
  protected static final String KEY_MAINCLASS = "MAINCLASS";
  protected static final String KEY_ARGS = "ARGS";
  //Number of TaskManagers
  protected static final String KEY_NUMTMS = "NUMTMS";
  //Number of slots per task
  protected static final String KEY_SLOTS = "SLOTS";
  //TaskManager memory
  protected static final String KEY_TMMEM = "TMMEM";
  //Job Type
  protected static final String KEY_FLINKJOBTYPE = "FLINKJOBTYPE";
  //Parallelism property
  protected static final String KEY_PARALLELISM = "PARALLELISM";

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
  public JobType getType() {
    return JobType.FLINK;
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = super.getReducedJsonObject();
    //First: fields that are possibly null or empty:
    if (!Strings.isNullOrEmpty(args)) {
      obj.set(KEY_ARGS, args);
    }
    if (!Strings.isNullOrEmpty(mainClass)) {
      obj.set(KEY_MAINCLASS, mainClass);
    }
    if (!Strings.isNullOrEmpty(jarPath)) {
      obj.set(KEY_JARPATH, jarPath);
    }
    //Then: fields that can never be null or emtpy.
    obj.set(KEY_SLOTS, "" + slots);
    obj.set(KEY_TMMEM, "" + taskManagerMemory);
    obj.set(KEY_NUMTMS, "" + numberOfTaskManagers);
    obj.set(KEY_FLINKJOBTYPE, "" + flinkjobtype);
    obj.set(KEY_PARALLELISM, "" + parallelism);
    obj.set(KEY_TYPE, JobType.FLINK.name());

    return obj;
  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException {
    JobType type;
    String jsonArgs, jsonJarpath, jsonMainclass, jsonNumtms, jsonJobType;
    int jsonTMmem, jsonSlots, jsonParallelism;
    try {
      String jsonType = json.getString(KEY_TYPE);
      type = JobType.valueOf(jsonType);
      if (type != JobType.FLINK) {
        throw new IllegalArgumentException("JobType must be FLINK.");
      }
      //First: fields that can be null or empty
      jsonArgs = json.getString(KEY_ARGS, null);
      jsonJarpath = json.getString(KEY_JARPATH, null);
      jsonMainclass = json.getString(KEY_MAINCLASS, null);
      //Then: fields that cannot be null or emtpy.
      jsonSlots = Integer.parseInt(json.getString(KEY_SLOTS));
      jsonTMmem = Integer.parseInt(json.getString(KEY_TMMEM));
      jsonNumtms = json.getString(KEY_NUMTMS);
      jsonJobType = json.getString(KEY_FLINKJOBTYPE);
      jsonParallelism = Integer.parseInt(json.getString(KEY_PARALLELISM));
    } catch (Exception e) {
      throw new IllegalArgumentException(
              "Cannot convert object into FlinkJobConfiguration.", e);
    }
    //Second: allow all superclasses to check validity. To do this: make sure 
    // that the type will get recognized correctly.
    json.set(KEY_TYPE, JobType.YARN.name());
    super.updateFromJson(json);
    //Third: we're now sure everything is valid: actually update the state
    this.args = jsonArgs;
    this.slots = jsonSlots;
    this.taskManagerMemory = jsonTMmem;
    this.flinkjobtype = jsonJobType;
    this.parallelism = jsonParallelism;
    this.jarPath = jsonJarpath;
    this.mainClass = jsonMainclass;
    this.numberOfTaskManagers = Integer.parseInt(jsonNumtms);
  }
}
