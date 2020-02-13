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

import io.hops.hopsworks.common.util.templates.airflow.AirflowDAG;
import io.hops.hopsworks.common.util.templates.airflow.AirflowFeatureValidationResultOperator;
import io.hops.hopsworks.common.util.templates.airflow.AirflowJobLaunchOperator;
import io.hops.hopsworks.common.util.templates.airflow.AirflowJobSuccessSensor;
import io.hops.hopsworks.common.util.templates.airflow.AirflowOperator;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class AirflowDagDTO {
  private String name;
  private String scheduleInterval;
  private String apiKey;
  private List<AirflowOperatorDTO> operators;
  
  public AirflowDagDTO() {}
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getScheduleInterval() {
    return scheduleInterval;
  }
  
  public void setScheduleInterval(String scheduleInterval) {
    this.scheduleInterval = scheduleInterval;
  }
  
  public String getApiKey() {
    return apiKey;
  }
  
  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
  
  public List<AirflowOperatorDTO> getOperators() {
    return operators;
  }
  
  public void setOperators(List<AirflowOperatorDTO> operators) {
    this.operators = operators;
  }
  
  public static AirflowDAG toAirflowDagTemplate(AirflowDagDTO dagDefinition, Users owner, Project project) {
    AirflowDAG dag = new AirflowDAG(dagDefinition.getName(), owner.getUsername(), project.getName());
    dag.setScheduleInterval(dagDefinition.getScheduleInterval());
    if (dagDefinition.getApiKey() != null) {
      dag.setApiKey(dagDefinition.getApiKey());
    }
    
    List<AirflowOperator> operators = new ArrayList<>(dagDefinition.getOperators().size());
    
    for (AirflowOperatorDTO op : dagDefinition.getOperators()) {
      AirflowOperator operator = null;
      switch (op.getName()) {
        case AirflowJobLaunchOperator.NAME:
          operator = new AirflowJobLaunchOperator(project.getName(), op.getId(), op.getJobName());
          ((AirflowJobLaunchOperator)operator).setWait(op.isWait());
          break;
        case AirflowJobSuccessSensor.NAME:
          operator = new AirflowJobSuccessSensor(project.getName(), op.getId(), op.getJobName());
          break;
        case AirflowFeatureValidationResultOperator.NAME:
          operator = new AirflowFeatureValidationResultOperator(project.getName(), op.getId(),
              op.getFeatureGroupName());
          break;
        default:
      }
      if (operator != null) {
        if (op.getDependsOn() != null && !op.getDependsOn().isEmpty()) {
          StringBuilder sb = new StringBuilder();
          if (op.getDependsOn().size() == 1) {
            sb.append(op.getDependsOn().get(0));
          } else {
            sb.append("[");
            for (String dependency : op.getDependsOn()) {
              sb.append(dependency).append(",");
            }
            sb.append("]");
          }
          operator.setUpstream(sb.toString());
        }
        operators.add(operator);
      }
    }
    dag.setOperators(operators);
    return dag;
  }
}
