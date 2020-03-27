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
package io.hops.hopsworks.common.util;

import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.flink.FlinkJobConfiguration;
import io.hops.hopsworks.common.util.templates.ConfigProperty;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FlinkConfigurationUtil extends ConfigurationUtil {
  @Override
  public Map<String, String> setFrameworkProperties(Project project, JobConfiguration jobConfiguration,
                                                    Settings settings, String hdfsUser,
                                                    String tfLdLibraryPath, Map<String,
                                                    String> extraJavaOptions,
                                                    String kafkaBrokersString) throws IOException {
    FlinkJobConfiguration flinkJobConfiguration = (FlinkJobConfiguration) jobConfiguration;
    
    Map<String, ConfigProperty> flinkProps = new HashMap<>();
    flinkProps.put(Settings.JOB_LOG4J_CONFIG, new ConfigProperty(
      Settings.JOB_LOG4J_CONFIG, HopsUtils.IGNORE, settings.getFlinkConfDir() + Settings.JOB_LOG4J_PROPERTIES));
    flinkProps.put(Settings.JOB_LOG4J_PROPERTIES, new ConfigProperty(
      Settings.JOB_LOG4J_PROPERTIES, HopsUtils.IGNORE, settings.getFlinkConfDir() + Settings.JOB_LOG4J_PROPERTIES));
    flinkProps.put(Settings.FLINK_STATE_CHECKPOINTS_DIR, new ConfigProperty(
      Settings.FLINK_STATE_CHECKPOINTS_DIR, HopsUtils.OVERWRITE,
      "hdfs://" + Utils.getProjectPath(project.getName()) + Settings.PROJECT_STAGING_DIR + "/flink"));
    
    if (extraJavaOptions == null) {
      extraJavaOptions = new HashMap<>();
    }
    extraJavaOptions.put(Settings.JOB_LOG4J_CONFIG, settings.getFlinkConfDir() + Settings.JOB_LOG4J_PROPERTIES);
    extraJavaOptions.put(Settings.JOB_LOG4J_PROPERTIES, settings.getFlinkConfDir() + Settings.JOB_LOG4J_PROPERTIES);
    extraJavaOptions.put(Settings.HOPSWORKS_REST_ENDPOINT_PROPERTY, settings.getRestEndpoint());
    extraJavaOptions.put(Settings.HOPSUTIL_INSECURE_PROPERTY, String.valueOf(settings.isHopsUtilInsecure()));
    extraJavaOptions.put(Settings.SERVER_TRUSTSTORE_PROPERTY, Settings.SERVER_TRUSTSTORE_PROPERTY);
    extraJavaOptions.put(Settings.HOPSWORKS_ELASTIC_ENDPOINT_PROPERTY, settings.getElasticRESTEndpoint());
    extraJavaOptions.put(Settings.HOPSWORKS_PROJECTID_PROPERTY, Integer.toString(project.getId()));
    extraJavaOptions.put(Settings.HOPSWORKS_PROJECTNAME_PROPERTY, project.getName());
  
    extraJavaOptions.put(Settings.HOPSWORKS_PROJECTUSER_PROPERTY, hdfsUser);
    extraJavaOptions.put(Settings.KAFKA_BROKERADDR_PROPERTY, kafkaBrokersString);
    extraJavaOptions.put(Settings.HOPSWORKS_JOBTYPE_PROPERTY, jobConfiguration.getJobType().name());
    if (jobConfiguration.getAppName() != null) {
      extraJavaOptions.put(Settings.HOPSWORKS_JOBNAME_PROPERTY, jobConfiguration.getAppName());
    }
    
    StringBuilder extraJavaOptionsSb = new StringBuilder();
    for (String key : extraJavaOptions.keySet()) {
      extraJavaOptionsSb.append(" -D").append(key).append("=").append(extraJavaOptions.get(key));
    }
    flinkProps.put(Settings.FLINK_ENV_JAVA_OPTS, new ConfigProperty(
      Settings.FLINK_ENV_JAVA_OPTS, HopsUtils.APPEND_SPACE, extraJavaOptionsSb.toString()));
    
    
    Map<String, String> validatedFlinkProperties = HopsUtils.parseUserProperties(flinkJobConfiguration.getProperties());
    // Merge system and user defined properties
    return HopsUtils.mergeHopsworksAndUserParams(flinkProps, validatedFlinkProperties);
  }
}
