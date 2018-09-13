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
public class JobHeuristicDetailsDTO implements Serializable {

  private String appId;
  private int amMemory, amVcores;
  private long executionTime;

  // The total severity of that job
  private String totalSeverity;

  // Memory Limit Heuristic
  private String memorySeverity;
  private String totalDriverMemory, totalExecutorMemory, memoryForStorage;
  private int numberOfExecutors, executorMemory;

  // Stage Runtime Heuristic
  private String stageRuntimeSeverity;
  private String averageStageFailure, problematicStages, completedStages, failedStages;

  // Job Runtime Heuristic
  private String jobRuntimeSeverity;
  private String averageJobFailure, completedJobsNumber, failedJobsNumber;

  //Executor Load Balance Heuristic
  private String loadBalanceSeverity;

  public JobHeuristicDetailsDTO() {
  }

  public JobHeuristicDetailsDTO(String appId, String totalSeverity) {
    this.appId = appId;
    this.totalSeverity = totalSeverity;
  }

  public JobHeuristicDetailsDTO(String appId, String totalSeverity,
          String totalDriverMemory,
          String totalExecutorMemory, String memoryForStorage) {
    this.appId = appId;
    this.totalSeverity = totalSeverity;
    this.totalDriverMemory = totalDriverMemory;
    this.totalExecutorMemory = totalExecutorMemory;
    this.memoryForStorage = memoryForStorage;
  }

  /**
   * @return the appId
   */
  public String getAppId() {
    return appId;
  }

  /**
   * @param appId the appId to set
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * @return the totalSeverity
   */
  public String getTotalSeverity() {
    return totalSeverity;
  }

  /**
   * @param totalSeverity the totalSeverity to set
   */
  public void setTotalSeverity(String totalSeverity) {
    this.totalSeverity = totalSeverity;
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

  /**
   * @return the memoryForStorage
   */
  public String getMemoryForStorage() {
    return memoryForStorage;
  }

  /**
   * @param memoryForStorage the memoryForStorage to set
   */
  public void setMemoryForStorage(String memoryForStorage) {
    this.memoryForStorage = memoryForStorage;
  }

  /**
   * @return the numberOfExecutors
   */
  public int getNumberOfExecutors() {
    return numberOfExecutors;
  }

  /**
   * @param numberOfExecutors the executorCores to set
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
   * @return the memorySeverity
   */
  public String getMemorySeverity() {
    return memorySeverity;
  }

  /**
   * @param memorySeverity the memorySeverity to set
   */
  public void setMemorySeverity(String memorySeverity) {
    this.memorySeverity = memorySeverity;
  }

  /**
   * @return the stageRuntimeSeverity
   */
  public String getStageRuntimeSeverity() {
    return stageRuntimeSeverity;
  }

  /**
   * @param stageRuntimeSeverity the stageRuntimeSeverity to set
   */
  public void setStageRuntimeSeverity(String stageRuntimeSeverity) {
    this.stageRuntimeSeverity = stageRuntimeSeverity;
  }

  /**
   * @return the averageStageFailure
   */
  public String getAverageStageFailure() {
    return averageStageFailure;
  }

  /**
   * @param averageStageFailure the averageStageFailure to set
   */
  public void setAverageStageFailure(String averageStageFailure) {
    this.averageStageFailure = averageStageFailure;
  }

  /**
   * @return the problematicStages
   */
  public String getProblematicStages() {
    return problematicStages;
  }

  /**
   * @param problematicStages the problematicStages to set
   */
  public void setProblematicStages(String problematicStages) {
    this.problematicStages = problematicStages;
  }

  /**
   * @return the completedStages
   */
  public String getCompletedStages() {
    return completedStages;
  }

  /**
   * @param completedStages the completedStages to set
   */
  public void setCompletedStages(String completedStages) {
    this.completedStages = completedStages;
  }

  /**
   * @return the failedStages
   */
  public String getFailedStages() {
    return failedStages;
  }

  /**
   * @param failedStages the failedStages to set
   */
  public void setFailedStages(String failedStages) {
    this.failedStages = failedStages;
  }

  /**
   * @return the jobRuntimeSeverity
   */
  public String getJobRuntimeSeverity() {
    return jobRuntimeSeverity;
  }

  /**
   * @param jobRuntimeSeverity the jobRuntimeSeverity to set
   */
  public void setJobRuntimeSeverity(String jobRuntimeSeverity) {
    this.jobRuntimeSeverity = jobRuntimeSeverity;
  }

  /**
   * @return the averageJobFailure
   */
  public String getAverageJobFailure() {
    return averageJobFailure;
  }

  /**
   * @param averageJobFailure the averageJobFailure to set
   */
  public void setAverageJobFailure(String averageJobFailure) {
    this.averageJobFailure = averageJobFailure;
  }

  /**
   * @return the completedJobsNumber
   */
  public String getCompletedJobsNumber() {
    return completedJobsNumber;
  }

  /**
   * @param completedJobsNumber the completedJobsNumber to set
   */
  public void setCompletedJobsNumber(String completedJobsNumber) {
    this.completedJobsNumber = completedJobsNumber;
  }

  /**
   * @return the failedJobsNumber
   */
  public String getFailedJobsNumber() {
    return failedJobsNumber;
  }

  /**
   * @param failedJobsNumber the failedJobsNumber to set
   */
  public void setFailedJobsNumber(String failedJobsNumber) {
    this.failedJobsNumber = failedJobsNumber;
  }

  /**
   * @return the loadBalanceSeverity
   */
  public String getLoadBalanceSeverity() {
    return loadBalanceSeverity;
  }

  /**
   * @param loadBalanceSeverity the loadBalanceSeverity to set
   */
  public void setLoadBalanceSeverity(String loadBalanceSeverity) {
    this.loadBalanceSeverity = loadBalanceSeverity;
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
   * @return the executionTime
   */
  public long getExecutionTime() {
    return executionTime;
  }

  /**
   * @param executionTime the executionTime to set
   */
  public void setExecutionTime(long executionTime) {
    this.executionTime = executionTime;
  }

}
