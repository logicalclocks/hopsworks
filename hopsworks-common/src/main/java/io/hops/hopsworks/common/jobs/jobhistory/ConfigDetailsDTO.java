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
public class ConfigDetailsDTO implements Serializable {

  private String jobId;
  private String className;
  private String jarFile;
  private String arguments;
  private String totalDriverMemory;
  private String totalExecutorMemory;
  private String blocksInHdfs;
  private int amMemory;
  private int amVcores;
  private int numberOfExecutors;
  private int executorMemory;

  public ConfigDetailsDTO() {
  }

  public ConfigDetailsDTO(String jobId) {
    this.jobId = jobId;
  }

  /**
   * @return the className
   */
  public String getClassName() {
    return className;
  }

  /**
   * @param className the className to set
   */
  public void setClassName(String className) {
    this.className = className;
  }

  /**
   * @return the jarFile
   */
  public String getJarFile() {
    return jarFile;
  }

  /**
   * @param jarFile the jarFile to set
   */
  public void setJarFile(String jarFile) {
    this.jarFile = jarFile;
  }

  /**
   * @return the arguments
   */
  public String getArguments() {
    return arguments;
  }

  /**
   * @param arguments the arguments to set
   */
  public void setArguments(String arguments) {
    this.arguments = arguments;
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
   * @return the blocksInHdfs
   */
  public String getBlocksInHdfs() {
    return blocksInHdfs;
  }

  /**
   * @param blocksInHdfs the blocksInHdfs to set
   */
  public void setBlocksInHdfs(String blocksInHdfs) {
    this.blocksInHdfs = blocksInHdfs;
  }

  /**
   * @return the numberOfExecutors
   */
  public int getNumberOfExecutors() {
    return numberOfExecutors;
  }

  /**
   * @param numberOfExecutors the numberOfExecutors to set
   */
  public void setNumberOfExecutors(int numberOfExecutors) {
    this.numberOfExecutors = numberOfExecutors;
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
   * @return the jobId
   */
  public String getJobId() {
    return jobId;
  }

  /**
   * @param jobId the jobId to set
   */
  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  /**
   * @return the totalDriverMemory
   */
  public String getTotalDriverMemory() {
    return totalDriverMemory;
  }

  /**
   * @param totalDriverMemory the totalDriverMemory to set
   */
  public void setTotalDriverMemory(String totalDriverMemory) {
    this.totalDriverMemory = totalDriverMemory;
  }

  /**
   * @return the totalExecutorMemory
   */
  public String getTotalExecutorMemory() {
    return totalExecutorMemory;
  }

  /**
   * @param totalExecutorMemory the totalExecutorMemory to set
   */
  public void setTotalExecutorMemory(String totalExecutorMemory) {
    this.totalExecutorMemory = totalExecutorMemory;
  }

}
