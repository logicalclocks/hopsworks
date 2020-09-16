/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

package io.hops.hopsworks.api.airflow;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class AirflowOperatorDTO {
  private String name;
  private String id;
  private String jobName;
  private boolean wait;
  private List<String> dependsOn;
  private String featureGroupName;
  private String jobArgs;
  
  public AirflowOperatorDTO() {}
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public String getJobName() {
    return jobName;
  }
  
  public void setJobName(String jobName) {
    this.jobName = jobName;
  }
  
  public boolean isWait() {
    return wait;
  }
  
  public void setWait(boolean wait) {
    this.wait = wait;
  }
  
  public List<String> getDependsOn() {
    return dependsOn;
  }
  
  public void setDependsOn(List<String> dependsOn) {
    this.dependsOn = dependsOn;
  }
  
  public String getFeatureGroupName() {
    return featureGroupName;
  }
  
  public void setFeatureGroupName(String featureGroupName) {
    this.featureGroupName = featureGroupName;
  }
  
  public String getJobArgs() { return jobArgs; }
  
  public void setJobArgs(String jobArgs) { this.jobArgs = jobArgs; }
}
