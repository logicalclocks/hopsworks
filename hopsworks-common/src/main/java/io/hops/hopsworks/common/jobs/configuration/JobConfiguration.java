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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.hops.hopsworks.common.jobs.erasureCode.ErasureCodeJobConfiguration;
import io.hops.hopsworks.common.jobs.flink.FlinkJobConfiguration;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.jobs.yarn.YarnJobConfiguration;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents the persistable configuration of a runnable job.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso({YarnJobConfiguration.class})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = YarnJobConfiguration.class, name = "YarnJobConfiguration") }
)
public abstract class JobConfiguration {

  protected String appName;

  protected ScheduleDTO schedule;

  protected KafkaDTO kafka;

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
  public abstract JobType getJobType();

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

  @Override
  public final boolean equals(Object o) {
    return false;
  }
  
  @Override
  public int hashCode() {
    return ThreadLocalRandom.current().nextInt(0, 999999999 + 1);
  }
  
  public static class JobConfigurationFactory {

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
                  "The given JobType is not recognized by this factory.");
      }
      return conf;
    }
  }
}
