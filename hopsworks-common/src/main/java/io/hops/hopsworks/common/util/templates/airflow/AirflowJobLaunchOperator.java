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

package io.hops.hopsworks.common.util.templates.airflow;

public class AirflowJobLaunchOperator extends AirflowOperator {
  public static final String NAME = "HopsworksLaunchOperator";
  
  private final String jobName;
  private boolean wait;
  private final String jobArgs;
  
  public AirflowJobLaunchOperator(String projectName, String id, String jobName, String jobArgs) {
    super(projectName, id);
    this.jobName = jobName;
    this.wait = false;
    this.jobArgs = jobArgs;
  }
  
  public String getJobName() {
    return jobName;
  }
  
  public boolean isWait() {
    return wait;
  }
  
  public void setWait(boolean wait) {
    this.wait = wait;
  }
  
  public String getJobArgs() {
    return jobArgs;
  }
}
