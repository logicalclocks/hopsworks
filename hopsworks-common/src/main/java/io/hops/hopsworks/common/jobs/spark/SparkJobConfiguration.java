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

import com.google.common.base.Strings;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.jobs.MutableJsonObject;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.yarn.YarnJobConfiguration;
import io.hops.hopsworks.common.util.Settings;

/**
 * Contains Spark-specific run information for a Spark job, on top of Yarn
 * configuration.
 */
@XmlRootElement
public class SparkJobConfiguration extends YarnJobConfiguration {

  private String appPath;
  private String mainClass;
  private String args;
  private String historyServerIp;
  private String anacondaDir;
  private String properties;

  private int numberOfExecutors = 1;
  private int executorCores = 1;
  private int executorMemory = 1024;

  private boolean dynamicExecutors;
  private int minExecutors = Settings.SPARK_MIN_EXECS;
  private int maxExecutors = Settings.SPARK_MAX_EXECS;
  private int selectedMinExecutors = Settings.SPARK_INIT_EXECS;
  private int selectedMaxExecutors = Settings.SPARK_INIT_EXECS;
  private int numberOfExecutorsInit = Settings.SPARK_INIT_EXECS;

  private int numberOfGpusPerExecutor = 0;

  protected static final String KEY_JARPATH = "JARPATH";
  protected static final String KEY_MAINCLASS = "MAINCLASS";
  protected static final String KEY_ARGS = "ARGS";
  protected static final String KEY_PROPERTIES = "PROPERTIES";
  protected static final String KEY_NUMEXECS = "NUMEXECS";
  //Dynamic executors properties
  protected static final String KEY_DYNEXECS = "DYNEXECS";
  protected static final String KEY_DYNEXECS_MIN = "DYNEXECSMIN";
  protected static final String KEY_DYNEXECS_MAX = "DYNEXECSMAX";
  protected static final String KEY_DYNEXECS_MIN_SELECTED
      = "DYNEXECSMINSELECTED";
  protected static final String KEY_DYNEXECS_MAX_SELECTED
      = "DYNEXECSMAXSELECTED";
  protected static final String KEY_DYNEXECS_INIT = "DYNEXECSINIT";

  protected static final String KEY_EXECCORES = "EXECCORES";
  protected static final String KEY_EXECMEM = "EXECMEM";
  protected static final String KEY_HISTORYSERVER = "HISTORYSERVER";
// See: https://www.continuum.io/blog/developer-blog/using-anaconda-
  //    pyspark-distributed-language-processing-hadoop-cluster
  protected static final String KEY_PYSPARK_PYTHON_DIR = "PYSPARK_PYTHON";
  protected static final String KEY_PYSPARK_PYLIB = "PYLIB";
  protected static final String KEY_GPUS = "NUM_GPUS";

  public SparkJobConfiguration() {
    super();
  }

  public String getAppPath() {
    return appPath;
  }

  /**
   * Set the path to the main executable jar. No default value.
   * <p/>
   * @param appPath
   */
  public void setAppPath(String appPath) {
    this.appPath = appPath;
  }

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

  /**
   *
   * @return
   */
  public String getProperties() {
    return properties;
  }

  /**
   *
   * @param properties
   */
  public void setProperties(String properties) {
    this.properties = properties;
  }

  public int getNumberOfExecutors() {
    return numberOfExecutors;
  }

  public String getAnacondaDir() {
    return anacondaDir;
  }

  public void setAnacondaDir(String anacondaDir) {
    this.anacondaDir = anacondaDir;
  }

  public int getNumberOfGpusPerExecutor() {
    return numberOfGpusPerExecutor;
  }

  public void setNumberOfGpusPerExecutor(int numberOfGpusPerExecutor) {
    this.numberOfGpusPerExecutor = numberOfGpusPerExecutor;
  }

  /**
   * Set the number of executors to be requested for this job. This should be
   * greater than or equal to 1.
   * <p/>
   * @param numberOfExecutors
   * @throws IllegalArgumentException If the argument is smaller than 1.
   */
  public void setNumberOfExecutors(int numberOfExecutors) throws
      IllegalArgumentException {
    if (numberOfExecutors < 1) {
      throw new IllegalArgumentException(
          "Number of executors has to be greater than or equal to 1.");
    }
    this.numberOfExecutors = numberOfExecutors;
  }

  public int getExecutorCores() {
    return executorCores;
  }

  /**
   * Set the number of cores to be requested for each executor.
   * <p/>
   * @param executorCores
   * @throws IllegalArgumentException If the number of cores is smaller than 1.
   */
  public void setExecutorCores(int executorCores) throws
      IllegalArgumentException {
    if (executorCores < 1) {
      throw new IllegalArgumentException(
          "Number of executor cores has to be greater than or equal to 1.");
    }
    this.executorCores = executorCores;
  }

  public int getExecutorMemory() {
    return executorMemory;
  }

  /**
   * Set the memory requested for each executor in MB.
   * <p/>
   * @param executorMemory
   * @throws IllegalArgumentException If the given value is not strictly
   * positive.
   */
  public void setExecutorMemory(int executorMemory) throws
      IllegalArgumentException {
    if (executorMemory < 1) {
      throw new IllegalArgumentException(
          "Executor memory must be greater than 1MB.");
    }
    this.executorMemory = executorMemory;
  }

  public String getHistoryServerIp() {
    return historyServerIp;
  }

  public void setHistoryServerIp(String historyServerIp) {
    this.historyServerIp = historyServerIp;
  }

  public boolean isDynamicExecutors() {
    return dynamicExecutors;
  }

  public void setDynamicExecutors(boolean dynamicExecutors) {
    this.dynamicExecutors = dynamicExecutors;
  }

  public int getMinExecutors() {
    return minExecutors;
  }

  public void setMinExecutors(int minExecutors) {
    this.minExecutors = minExecutors;
  }

  public int getMaxExecutors() {
    return maxExecutors;
  }

  public void setMaxExecutors(int maxExecutors) {
    this.maxExecutors = maxExecutors;
  }

  public int getSelectedMinExecutors() {
    return selectedMinExecutors;
  }

  public void setSelectedMinExecutors(int selectedMinExecutors) {
    this.selectedMinExecutors = selectedMinExecutors;
  }

  public int getSelectedMaxExecutors() {
    return selectedMaxExecutors;
  }

  public void setSelectedMaxExecutors(int selectedMaxExecutors) {
    this.selectedMaxExecutors = selectedMaxExecutors;
  }

  public int getNumberOfExecutorsInit() {
    return numberOfExecutorsInit;
  }

  public void setNumberOfExecutorsInit(int numberOfExecutorsInit) {
    this.numberOfExecutorsInit = numberOfExecutorsInit;
  }

  @Override
  public JobType getType() {
    if (this.mainClass.equals(Settings.SPARK_PY_MAINCLASS)) {
      return JobType.PYSPARK;
    } else {
      return JobType.SPARK;
    }
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
    if (!Strings.isNullOrEmpty(appPath)) {
      obj.set(KEY_JARPATH, appPath);
    }
    if (!Strings.isNullOrEmpty(properties)) {
      obj.set(KEY_PROPERTIES, properties);
    }
    //Then: fields that can never be null or emtpy.
    obj.set(KEY_EXECCORES, "" + executorCores);
    obj.set(KEY_EXECMEM, "" + executorMemory);
    obj.set(KEY_NUMEXECS, "" + numberOfExecutors);
    obj.set(KEY_DYNEXECS, "" + dynamicExecutors);
    obj.set(KEY_DYNEXECS_MIN, "" + minExecutors);
    obj.set(KEY_DYNEXECS_MAX, "" + maxExecutors);
    obj.set(KEY_DYNEXECS_MIN_SELECTED, "" + selectedMinExecutors);
    obj.set(KEY_DYNEXECS_MAX_SELECTED, "" + selectedMaxExecutors);
    obj.set(KEY_DYNEXECS_INIT, "" + numberOfExecutorsInit);

    obj.set(KEY_TYPE, JobType.SPARK.name());
    obj.set(KEY_HISTORYSERVER, getHistoryServerIp());
    obj.set(KEY_PYSPARK_PYTHON_DIR, getAnacondaDir() + "/bin/python");
    obj.set(KEY_PYSPARK_PYLIB, getAnacondaDir() + "/lib");

    if (getType() == JobType.PYSPARK) {
      obj.set(KEY_GPUS, "" + numberOfGpusPerExecutor);
    }
    return obj;
  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
      IllegalArgumentException {
    JobType type;
    String jsonArgs, jsonJarpath, jsonMainclass, jsonNumexecs, hs, jsonExecmem,
        jsonExeccors, jsonProperties;
    String jsonNumexecsMin = "";
    String jsonNumexecsMax = "";
    String jsonNumexecsMinSelected = "";
    String jsonNumexecsMaxSelected = "";
    String jsonNumexecsInit = "";
    String jsonDynexecs = "NOT_AVAILABLE";
    String jsonPySpark = "";
    String jsonNumOfGPUs = "0";
    try {
      String jsonType = json.getString(KEY_TYPE);
      type = JobType.valueOf(jsonType);
      if (type != JobType.SPARK && type != JobType.PYSPARK) {
        throw new IllegalArgumentException("JobType must be SPARK.");
      }
      //First: fields that can be null or empty
      jsonArgs = json.getString(KEY_ARGS, null);
      jsonProperties = json.getString(KEY_PROPERTIES, null);
      jsonJarpath = json.getString(KEY_JARPATH, null);
      jsonMainclass = json.getString(KEY_MAINCLASS, null);
      //Then: fields that cannot be null or emtpy.
      jsonExeccors = json.getString(KEY_EXECCORES);
      jsonExecmem = json.getString(KEY_EXECMEM);
      jsonNumexecs = json.getString(KEY_NUMEXECS);
      if (json.containsKey(KEY_DYNEXECS)) {
        jsonDynexecs = json.getString(KEY_DYNEXECS);
        jsonNumexecsMin = json.getString(KEY_DYNEXECS_MIN);
        jsonNumexecsMax = json.getString(KEY_DYNEXECS_MAX);
        jsonNumexecsMinSelected = json.getString(KEY_DYNEXECS_MIN_SELECTED);
        jsonNumexecsMaxSelected = json.getString(KEY_DYNEXECS_MAX_SELECTED);
        jsonNumexecsInit = json.getString(KEY_DYNEXECS_INIT);
      }
      if (jsonMainclass.compareToIgnoreCase(Settings.SPARK_PY_MAINCLASS) == 0) {
        jsonNumOfGPUs = json.getString(KEY_GPUS);
      }

      hs = json.getString(KEY_HISTORYSERVER);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Cannot convert object into SparkJobConfiguration.", e);
    }
    //Second: allow all superclasses to check validity. To do this: make sure that 
    //the type will get recognized correctly.

    json.set(KEY_TYPE, JobType.YARN.name());
    super.updateFromJson(json);
    //Third: we're now sure everything is valid: actually update the state

    this.args = jsonArgs;

    this.executorCores = Integer.parseInt(jsonExeccors);

    this.executorMemory = Integer.parseInt(jsonExecmem);

    this.appPath = jsonJarpath;

    this.mainClass = jsonMainclass;

    this.numberOfExecutors = Integer.parseInt(jsonNumexecs);

    this.properties = jsonProperties;

    if (jsonDynexecs.equals(
        "true") || jsonDynexecs.equals("false")) {
      this.dynamicExecutors = Boolean.parseBoolean(jsonDynexecs);
      this.minExecutors = Integer.parseInt(jsonNumexecsMin);
      this.maxExecutors = Integer.parseInt(jsonNumexecsMax);
      this.selectedMinExecutors = Integer.parseInt(jsonNumexecsMinSelected);
      this.selectedMaxExecutors = Integer.parseInt(jsonNumexecsMaxSelected);
      this.numberOfExecutorsInit = Integer.parseInt(jsonNumexecsInit);
    }

    if (!jsonNumOfGPUs.equals("")) {
      this.numberOfGpusPerExecutor = Integer.parseInt(jsonNumOfGPUs);
    }

    this.historyServerIp = hs;

  }

}
