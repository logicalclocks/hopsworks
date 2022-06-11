/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.jobs.executions;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationAttemptStateFacade;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MonitoringUrlBuilder {
  
  private static final String FLINK_MASTER = "flinkmaster/";
  private static final String FLINK_HISTORY_SERVER = "flinkhistoryserver/";
  private static final String YARN_UI = "yarnui/";
  private static final String YARN_UI_CLUSTER = YARN_UI + "cluster/app/";
  private static final String GRAFANA = "grafana/d/spark/spark?var-applicationId=";
  private static final String TENSOR_BOARD = "/tensorboard/";
  
  private static final String PROJECT_ID_PLACEHOLDER = "%%projectId%%";
  private static final String PROJECT_NAME_PLACEHOLDER = "%%projectName%%";
  private static final String APP_ID_PLACEHOLDER = "%%appId%%";
  private static final String KIBANA_LIVY =
      "projectId=%%projectId%%#/discover?security_tenant=private&_g=()&" +
          "_a=(columns:!(logdate,host,priority,logger_name," +
          "log_message),filters:!(('$state':(store:appState),meta:(alias:!n,disabled:!f," +
          "index:'%%projectName%%_logs-*'" +
          ",key:jobid,negate:!f,params:(query:notebook,type:phrase),type:phrase,value:notebook)," +
          "query:(match:(jobid:(query:notebook,type:phrase)))),('$state':(store:appState),meta:(alias:!n,disabled:!f," +
          "index:'%%projectName%%_logs-*',key:jobname,negate:!f,params:(query:jupyter,type:phrase)," +
          "type:phrase,value:jupyter),query:(match:(jobname:(query:jupyter,type:phrase)))))," +
          "index:'%%projectName%%_logs-*',interval:auto,query:(language:lucene,query:''),sort:!(logdate,desc))";
  
  private static final String KIBANA =
      "#/discover?security_tenant=private&_g=(filters:!())&" +
          "_a=(columns:!(logdate,host,priority,logger_name,log_message)," +
          "filters:!(('$state':(store:appState),meta:(alias:!n,disabled:!f,index:'%%projectName%%_logs-*'," +
          "key:application,negate:!f,params:(query:%%appId%%),type:phrase,value:%%appId%%),query:(match:" +
          "(application:(query:%%appId%%,type:phrase))))),index:'%%projectName%%_logs-*',interval:auto,query:" +
          "(language:kuery,query:''),sort:!(logdate,desc))";
  
  private static final String KIBANA_KUBE = "#/discover?security_tenant=private&_g=()" +
          "&_a=(columns:!('@timestamp',file,log_message)," +
          "filters:!(('$state':(store:appState)," +
          "meta:(alias:!n,disabled:!f,index:'%%projectName%%_logs-*',key:application," +
          "negate:!f,params:(query:%%appId%%),type:phrase,value:%%appId%%)," +
          "query:(match:(application:(query:%%appId%%,type:phrase))))),index:'%%projectName%%_logs-*',interval:auto," +
          "query:(language:kuery,query:''),sort:!('@timestamp',desc))";
  
  @EJB
  private YarnApplicationAttemptStateFacade yarnApplicationAttemptStateFacade;
  
  public MonitoringUrlDTO build(Execution execution) {
    if (execution == null || Strings.isNullOrEmpty(execution.getAppId())) {
      return null;
    }
    MonitoringUrlDTO monitoringUrlDTO = new MonitoringUrlDTO();
    monitoringUrlDTO.setYarnUrl(YARN_UI_CLUSTER + execution.getAppId());
    monitoringUrlDTO.setGrafanaUrl(GRAFANA + execution.getAppId());
    monitoringUrlDTO.setTfUrl(TENSOR_BOARD + execution.getAppId());
    String kibanaUrl;
    if (!execution.getAppId().contains("application")) {
      kibanaUrl = KIBANA_KUBE.replace(PROJECT_NAME_PLACEHOLDER, execution.getJob().getProject().getName().toLowerCase())
          .replace(APP_ID_PLACEHOLDER, execution.getAppId());
    } else {
      kibanaUrl = KIBANA.replace(PROJECT_NAME_PLACEHOLDER, execution.getJob().getProject().getName().toLowerCase())
          .replace(APP_ID_PLACEHOLDER, execution.getAppId());
    }
    monitoringUrlDTO.setKibanaUrl(kibanaUrl);
    String trackingUrl = yarnApplicationAttemptStateFacade.findTrackingUrlByAppId(execution.getAppId());
    if (trackingUrl != null && !trackingUrl.isEmpty()) {
      trackingUrl = YARN_UI + trackingUrl;
      monitoringUrlDTO.setSparkUrl(trackingUrl);
    }
    if (execution.getJob().getJobType() == JobType.FLINK) {
      if(!execution.getState().isFinalState() && !Strings.isNullOrEmpty(execution.getAppId())) {
        monitoringUrlDTO.setFlinkMasterUrl(FLINK_MASTER + execution.getAppId());
      }
      monitoringUrlDTO.setFlinkHistoryServerUrl(FLINK_HISTORY_SERVER);
    }
    return monitoringUrlDTO;
  }
  
  public MonitoringUrlDTO build(String appId, Project project) {
    if (Strings.isNullOrEmpty(appId)) {
      return null;
    }
    MonitoringUrlDTO monitoringUrlDTO = new MonitoringUrlDTO();
    monitoringUrlDTO.setYarnUrl(YARN_UI_CLUSTER + appId);
    monitoringUrlDTO.setGrafanaUrl(GRAFANA + appId);
    monitoringUrlDTO.setTfUrl(TENSOR_BOARD + appId);
    String kibanaUrl = KIBANA_LIVY.replace(PROJECT_ID_PLACEHOLDER, project.getId().toString())
        .replace(PROJECT_NAME_PLACEHOLDER, project.getName().toLowerCase())
        .replace(APP_ID_PLACEHOLDER, appId);
    monitoringUrlDTO.setKibanaUrl(kibanaUrl);
    String trackingUrl = yarnApplicationAttemptStateFacade.findTrackingUrlByAppId(appId);
    if (trackingUrl != null && !trackingUrl.isEmpty()) {
      trackingUrl = YARN_UI + trackingUrl;
      monitoringUrlDTO.setSparkUrl(trackingUrl);
    }
    return monitoringUrlDTO;
  }
}
