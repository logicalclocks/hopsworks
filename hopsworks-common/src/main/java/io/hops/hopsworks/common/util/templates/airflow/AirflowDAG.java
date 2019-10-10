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

import java.util.List;

public class AirflowDAG {
  public static final String TEMPLATE_NAME = "airflow_dag.py";
  
  private final String id;
  private final String owner;
  private final String projectName;
  private String apiKey;
  private String scheduleInterval;
  private List<AirflowOperator> operators;
  
  public AirflowDAG(String id, String owner, String projectName) {
    this.id = id;
    this.owner = owner;
    this.projectName = projectName;
  }
  
  public String getId() {
    return id;
  }
  
  public String getOwner() {
    return owner;
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public String getApiKey() {
    return apiKey;
  }
  
  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
  
  public String getScheduleInterval() {
    return scheduleInterval;
  }
  
  public void setScheduleInterval(String scheduleInterval) {
    this.scheduleInterval = scheduleInterval;
  }
  
  public List<AirflowOperator> getOperators() {
    return operators;
  }
  
  public void setOperators(List<AirflowOperator> operators) {
    this.operators = operators;
  }
}
