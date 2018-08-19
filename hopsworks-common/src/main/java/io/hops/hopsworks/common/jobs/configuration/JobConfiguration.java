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

package io.hops.hopsworks.common.jobs.configuration;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobs.JsonReduceable;
import java.util.EnumSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.jobs.MutableJsonObject;
import io.hops.hopsworks.common.jobs.erasureCode.ErasureCodeJobConfiguration;
import io.hops.hopsworks.common.jobs.flink.FlinkJobConfiguration;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;

/**
 * Represents the persistable configuration of a runnable job. To be persisted
 * as JSON, the getReducedJsonObject() method is called.
 */
@XmlRootElement
public abstract class JobConfiguration implements JsonReduceable {

  protected String appName;
  protected ScheduleDTO schedule;
  protected KafkaDTO kafka;
  
  protected final static String KEY_APPNAME = "APPNAME";
  protected final static String KEY_SCHEDULE = "SCHEDULE";
  protected final static String KEY_KAFKA = "KAFKA";

  protected JobConfiguration() {
    //Needed for JAXB
  }

  public JobConfiguration(String appname) {
    this.appName = appname;
  }

  /**
   * Return the JobType this JobConfiguration is meant for.
   * <p/>
   * @return
   */
  public abstract JobType getType();

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public ScheduleDTO getSchedule() {
    return schedule;
  }

  public void setSchedule(ScheduleDTO schedule) {
    this.schedule = schedule;
  }

  public KafkaDTO getKafka() {
    return kafka;
  }

  public void setKafka(KafkaDTO kafka) {
    this.kafka = kafka;
  }

  /**
   * As found in Effective Java, the equals contract cannot be satisfied for
   * inheritance hierarchies that add fields in subclasses. Since this is the
   * main goal of extension of this class, these objects should not be compared.
   * <p/>
   * @param o
   * @return
   */
  @Override
  public final boolean equals(Object o) {
    throw new UnsupportedOperationException(
            "JobConfiguration objects cannot be compared.");
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = new MutableJsonObject();
    if (!Strings.isNullOrEmpty(appName)) {
      obj.set(KEY_APPNAME, appName);
    }
    if (schedule != null) {
      obj.set(KEY_SCHEDULE, schedule);
    }
    if (kafka != null) {
      obj.set(KEY_KAFKA, kafka);
    }
    return obj;
  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException {
    //First: check correctness
    ScheduleDTO sch = null;
    if (json.containsKey(KEY_SCHEDULE)) {
      MutableJsonObject mj = json.getJsonObject(KEY_SCHEDULE);
      try {
        sch = new ScheduleDTO();
        sch.updateFromJson(mj);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
                "Failed to parse JobConfiguration: invalid schedule.", e);
      }
    }

    if (json.containsKey(KEY_KAFKA)) {
      MutableJsonObject mj = json.getJsonObject(KEY_KAFKA);
      try {
        KafkaDTO kafkaDTO = new KafkaDTO();
        kafkaDTO.updateFromJson(mj);
        this.kafka = kafkaDTO;
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
                "Failed to parse JobConfiguration: invalid kafka properties.", e);
      }
    }
    //Then: set values
    this.appName = json.getString(KEY_APPNAME, null);
    this.schedule = sch;

  }

  public static class JobConfigurationFactory {

    public static JobConfiguration getJobConfigurationFromJson(
            MutableJsonObject object)
            throws IllegalArgumentException {
      //First: check if null
      if (object == null) {
        throw new NullPointerException(
                "Cannot get a JobConfiguration object from null.");
      }
      //Get the type
      String jType = object.getString("type");
      JobType type = JobType.valueOf(jType);
      JobConfiguration conf;
      switch (type) {
        case SPARK:
        case PYSPARK:
          conf = new SparkJobConfiguration();
          break;
        case FLINK:
          conf = new FlinkJobConfiguration();
          break;
        case ERASURE_CODING:
          conf = new ErasureCodeJobConfiguration();
          break;
        default:
          throw new UnsupportedOperationException(
                  "The given jobtype is not recognized by this factory.");
      }
      //Update the object
      conf.updateFromJson(object);
      return conf;
    }

    /**
     * Get a new JobConfiguration object with the given type.
     * <p/>
     * @param type
     * @return
     */
    public static JobConfiguration getJobConfigurationTemplate(JobType type) {
      JobConfiguration conf;
      switch (type) {
        case SPARK:
        case PYSPARK:
          conf = new SparkJobConfiguration();
          break;
        case FLINK:
          conf = new FlinkJobConfiguration();
          break;
        case ERASURE_CODING:
          conf = new ErasureCodeJobConfiguration();
          break;
        default:
          throw new UnsupportedOperationException(
                  "The given jobtype is not recognized by this factory.");
      }
      return conf;
    }

    public static Set<JobType> getSupportedTypes() {
      return EnumSet.of(
              JobType.SPARK,
              JobType.PYSPARK,
              JobType.FLINK,
              JobType.ERASURE_CODING);
    }
  }
}
