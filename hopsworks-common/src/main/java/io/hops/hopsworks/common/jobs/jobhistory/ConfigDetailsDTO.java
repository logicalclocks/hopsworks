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
