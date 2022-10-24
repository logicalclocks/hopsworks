/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.databricks.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class DbCluster {

  @Getter @Setter
  @JsonProperty("cluster_id")
  private String id;

  @Getter @Setter
  @JsonProperty("cluster_name")
  private String name;

  @Getter @Setter
  @JsonProperty("creator_user_name")
  private String creatorUserName;

  @Getter @Setter
  @JsonProperty("driver")
  private Object driver;

  @Getter @Setter
  @JsonProperty("executor")
  private Object executor;

  @Getter @Setter
  private String state;

  @Getter @Setter
  @JsonProperty("spark_version")
  private String sparkVersion;

  @Getter @Setter
  @JsonProperty("num_workers")
  private Integer numWorkers;

  @Getter @Setter
  @JsonProperty("autoscale")
  private Object autoScale;

  @Getter @Setter
  @JsonProperty("node_type_id")
  private String nodeTypeId;

  @Getter @Setter
  @JsonProperty("driver_node_type_id")
  private String driverNodeTypeId;

  @Getter @Setter
  @JsonProperty("spark_conf")
  private Map<String, String> sparkConfiguration;

  @Getter @Setter
  @JsonProperty("spark_context_id")
  private Long sparkContextId;

  @Getter @Setter
  @JsonProperty("autotermination_minutes")
  private Integer autoTerminationMinutes;

  @Getter @Setter
  @JsonProperty("init_scripts")
  private List<DbInitScriptInfo> initScripts;

  @Getter @Setter
  @JsonProperty("custom_tags")
  private Map<String, String> tags;

  @Getter @Setter
  @JsonProperty("jdbc_port")
  private Integer jdbcPort;

  @Getter @Setter
  @JsonProperty("aws_attributes")
  private Object awsAttributes;

  @Getter @Setter
  @JsonProperty("ssh_public_keys")
  private List<String> sshPublicKeys;

  @Getter @Setter
  @JsonProperty("cluster_log_conf")
  private Object clusterLogConf;

  @Getter @Setter
  @JsonProperty("docker_image")
  private Object dockerImage;

  @Getter @Setter
  @JsonProperty("spark_env_vars")
  private Object sparkEnvVars;

  @Getter @Setter
  @JsonProperty("enable_elastic_disk")
  private Boolean enableElasticDisk;

  @Getter @Setter
  @JsonProperty("instance_pool_id")
  private String instancePoolId;

  @Getter @Setter
  @JsonProperty("cluster_source")
  private Object clusterSource;

  @Getter @Setter
  @JsonProperty("state_message")
  private String stateMessage;

  @Getter @Setter
  @JsonProperty("cluster_memory_mb")
  private Integer clusterMemoryMb;

  @Getter @Setter
  @JsonProperty("cluster_cores")
  private Float clusterCores;
}
