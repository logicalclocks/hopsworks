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

import io.hops.hopsworks.common.dao.jobs.JobsHistory;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JobHeuristicDTO implements Serializable {

  private String message = "";
  private int projectId = 0;
  private String jobName = "";
  private String degreeOfSimilarity = "";
  private int numberOfResults;           // number of history records that examined
  private String jobType = "";           // the Job Type
  private String estimatedTime;             // estimated complition time
  private int inputBlocks = 0;
  private List<String> similarAppIds = new ArrayList<>();
  private List<JobProposedConfigurationDTO> jobProposedConfig
          = new ArrayList<>();
  private List<JobHeuristicDetailsDTO> jobHeuristicDetails = new ArrayList<>();

  public JobHeuristicDTO() {
  }

  public JobHeuristicDTO(int numberOfResults, String message,
          String estimatedTime, int projectId, String jobName, String jobType) {
    this.numberOfResults = numberOfResults;
    this.message = message;
    this.estimatedTime = estimatedTime;
    this.projectId = projectId;
    this.jobName = jobName;
    this.jobType = jobType;
  }

  public JobHeuristicDTO(int numberOfResults, String message,
          String estimatedTime, String degreeOfSimilarity, int inputBlocks) {
    this.numberOfResults = numberOfResults;
    this.message = message;
    this.estimatedTime = estimatedTime;
    this.degreeOfSimilarity = degreeOfSimilarity;
    this.inputBlocks = inputBlocks;
  }

  /**
   * @return the numberOfResults
   */
  public int getNumberOfResults() {
    return numberOfResults;
  }

  /**
   * @param numberOfResults the numberOfResults to set
   */
  public void setNumberOfResults(int numberOfResults) {
    this.numberOfResults = numberOfResults;
  }

  /**
   * @return the jobType
   */
  public String getJobType() {
    return jobType;
  }

  /**
   * @param jobType the jobType to set
   */
  public void setJobType(String jobType) {
    this.jobType = jobType;
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

  /**
   * @return the degreeOfSimilarity
   */
  public String getDegreeOfSimilarity() {
    return degreeOfSimilarity;
  }

  /**
   * @param degreeOfSimilarity the degreeOfSimilarity to set
   */
  public void setDegreeOfSimilarity(String degreeOfSimilarity) {
    this.degreeOfSimilarity = degreeOfSimilarity;
  }

  /**
   * @return the estimatedTime
   */
  public String getEstimatedTime() {
    return estimatedTime;
  }

  /**
   * @param estimatedTime the estimatedTime to set
   */
  public void setEstimatedTime(String estimatedTime) {
    this.estimatedTime = estimatedTime;
  }

  /**
   * @return the inputBlocks
   */
  public int getInputBlocks() {
    return inputBlocks;
  }

  /**
   * @param inputBlocks the inputBlocks to set
   */
  public void setInputBlocks(int inputBlocks) {
    this.inputBlocks = inputBlocks;
  }

  /**
   * @return the similarAppIds
   */
  public List<String> getSimilarAppIds() {
    return similarAppIds;
  }

  /**
   * @return the projectId
   */
  public int getProjectId() {
    return projectId;
  }

  /**
   * @param projectId the projectId to set
   */
  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  /**
   * @return the jobName
   */
  public String getJobName() {
    return jobName;
  }

  /**
   * @param jobName the jobName to set
   */
  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  /**
   * @return the jobProposedConfig
   */
  public List<JobProposedConfigurationDTO> getJobProposedConfig() {
    return jobProposedConfig;
  }

  /**
   * @param jobProposedConfig the jobProposedConfig to set
   */
  public void setJobProposedConfig(
          List<JobProposedConfigurationDTO> jobProposedConfig) {
    this.jobProposedConfig = jobProposedConfig;
  }

  /**
   * @param similarAppIds the similarAppIds to set
   */
  public void setSimilarAppIds(List<String> similarAppIds) {
    this.similarAppIds = similarAppIds;
  }

  public void addSimilarAppId(List<JobsHistory> jobsHistory) {
    Iterator<JobsHistory> itr = jobsHistory.iterator();

    while (itr.hasNext()) {
      JobsHistory element = itr.next();
      getSimilarAppIds().add(element.getAppId());
    }
  }

  /**
   * @return the jobHeuristicDetails
   */
  public List<JobHeuristicDetailsDTO> getJobHeuristicDetails() {
    return jobHeuristicDetails;
  }

  /**
   * @param jobHeuristicDetails the jobHeuristicDetails to set
   */
  public void setJobHeuristicDetails(
          List<JobHeuristicDetailsDTO> jobHeuristicDetails) {
    this.jobHeuristicDetails = jobHeuristicDetails;
  }

  public void addJobHeuristicDetails(JobHeuristicDetailsDTO jhDetail) {
    getJobHeuristicDetails().add(jhDetail);
  }

  public void addProposal(JobProposedConfigurationDTO config) {
    getJobProposedConfig().add(config);
  }

}
