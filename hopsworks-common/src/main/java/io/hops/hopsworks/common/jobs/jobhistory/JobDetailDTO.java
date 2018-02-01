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
public class JobDetailDTO implements Serializable {

  private String className;

  private String selectedJar;

  private String inputArgs;

  private String jobType;

  private int projectId;

  private String jobName;

  private boolean filter;

  public JobDetailDTO() {
  }

  public JobDetailDTO(String className) {
    this.className = className;
  }

  public JobDetailDTO(String className, String selectedJar, String inputArgs,
          String jobType, int projectId, String jobName, boolean filter) {
    this.className = className;
    this.selectedJar = selectedJar;
    this.inputArgs = inputArgs;
    this.jobType = jobType;
    this.projectId = projectId;
    this.jobName = jobName;
    this.filter = filter;
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
   * @return the selectedJar
   */
  public String getSelectedJar() {
    return selectedJar;
  }

  /**
   * @param selectedJar the selectedJar to set
   */
  public void setSelectedJar(String selectedJar) {
    this.selectedJar = selectedJar;
  }

  /**
   * @return the inputArgs
   */
  public String getInputArgs() {
    return inputArgs;
  }

  /**
   * @param inputArgs the inputArgs to set
   */
  public void setInputArgs(String inputArgs) {
    this.inputArgs = inputArgs;
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
   * @return the filter
   */
  public boolean isFilter() {
    return filter;
  }

  /**
   * @param filter the filter to set
   */
  public void setFilter(boolean filter) {
    this.filter = filter;
  }

}
