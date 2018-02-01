/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.jobs.jobhistory;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JobProposedConfigurationDTO implements Serializable {

  private String configType = "";
  private String message = "";
  private int amMemory;
  private int amVcores;
  private int numOfExecutors;
  private int executorCores;
  private int executorMemory;
  private String estimatedExecutionTime = "";

  public JobProposedConfigurationDTO() {
  }

  public JobProposedConfigurationDTO(String configType, String message,
          int amMemory, int amVcores, int numOfExecutors,
          int executorCores, int executorMemory) {
    this.configType = configType;
    this.message = message;
    this.amMemory = amMemory;
    this.amVcores = amVcores;
    this.numOfExecutors = numOfExecutors;
    this.executorCores = executorCores;
    this.executorMemory = executorMemory;
  }

  /**
   * @return the configType
   */
  public String getConfigType() {
    return configType;
  }

  /**
   * @param configType the configType to set
   */
  public void setConfigType(String configType) {
    this.configType = configType;
  }

  /**
   * @return the amMemory
   */
  public int getAmMemory() {
    return amMemory;
  }

  /**
   * @param amMemory the amMemory to set
   */
  public void setAmMemory(int amMemory) {
    this.amMemory = amMemory;
  }

  /**
   * @return the amVcores
   */
  public int getAmVcores() {
    return amVcores;
  }

  /**
   * @param amVcores the amVcores to set
   */
  public void setAmVcores(int amVcores) {
    this.amVcores = amVcores;
  }

  /**
   * @return the numOfExecutors
   */
  public int getNumOfExecutors() {
    return numOfExecutors;
  }

  /**
   * @param numOfExecutors the numOfExecutors to set
   */
  public void setNumOfExecutors(int numOfExecutors) {
    this.numOfExecutors = numOfExecutors;
  }

  /**
   * @return the executorCores
   */
  public int getExecutorCores() {
    return executorCores;
  }

  /**
   * @param executorCores the executorCores to set
   */
  public void setExecutorCores(int executorCores) {
    this.executorCores = executorCores;
  }

  /**
   * @return the executorMemory
   */
  public int getExecutorMemory() {
    return executorMemory;
  }

  /**
   * @param executorMemory the executorMemory to set
   */
  public void setExecutorMemory(int executorMemory) {
    this.executorMemory = executorMemory;
  }

  /**
   * @return the estimatedExecutionTime
   */
  public String getEstimatedExecutionTime() {
    return estimatedExecutionTime;
  }

  /**
   * @param estimatedExecutionTime the estimatedExecutionTime to set
   */
  public void setEstimatedExecutionTime(String estimatedExecutionTime) {
    this.estimatedExecutionTime = estimatedExecutionTime;
  }

  /**
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * @param message the message to set
   */
  public void setMessage(String message) {
    this.message = message;
  }

}
