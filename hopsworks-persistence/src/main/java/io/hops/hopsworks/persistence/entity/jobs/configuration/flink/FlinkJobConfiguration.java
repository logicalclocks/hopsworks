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

package io.hops.hopsworks.persistence.entity.jobs.configuration.flink;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.configuration.beam.BeamFlinkJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.yarn.YarnJobConfiguration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * Contains Flink-specific run information for a Flink job, on top of Yarn
 * configuration.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({BeamFlinkJobConfiguration.class})
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = BeamFlinkJobConfiguration.class, name = "BeamFlinkJobConfiguration")}
)
public class FlinkJobConfiguration extends YarnJobConfiguration {
  
  @XmlElement(name="jobmanager.heap.size")
  private int jobManagerMemory = 1024;
  @XmlElement
  private int numberOfTaskManagers = 1;
  @XmlElement(name="taskmanager.numberOfTaskSlots")
  private int numberOfTaskSlots = 1;
  @XmlElement(name="taskmanager.heap.size")
  private int taskManagerMemory = 1024;
  @XmlElement
  private String properties;

  public FlinkJobConfiguration() {
  }
  
  public int getJobManagerMemory() {
    return jobManagerMemory;
  }
  
  public void setJobManagerMemory(int jobManagerMemory) {
    this.jobManagerMemory = jobManagerMemory;
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

  public int getNumberOfTaskSlots() {
    return numberOfTaskSlots;
  }

  /**
   * Set the number of slots to be requested for each task manager.
   * <p/>
   * @param numberOfTaskSlots
   * @throws IllegalArgumentException If the number of slots is smaller than
   * 1.
   */
  public void setNumberOfTaskSlots(int numberOfTaskSlots) throws
          IllegalArgumentException {
    if (numberOfTaskSlots < 1) {
      throw new IllegalArgumentException(
              "Number of task manager slots has to be greater than or equal to 1.");
    }
    this.numberOfTaskSlots = numberOfTaskSlots;
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
              "TaskManager memory must be greater than 512MB.");
    }
    this.taskManagerMemory = taskManagerMemory;
  }

  @Override
  @XmlElement(name="jobType")
  public JobType getJobType() {
    return JobType.FLINK;
  }
  
  public String getProperties() {
    return properties;
  }
  
  public void setProperties(String properties) {
    this.properties = properties;
  }
}
