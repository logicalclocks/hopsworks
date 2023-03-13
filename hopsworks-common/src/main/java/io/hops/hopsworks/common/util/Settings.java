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
package io.hops.hopsworks.common.util;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.dto.ProvTypeDTO;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.project.PaymentType;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.util.Variables;
import io.hops.hopsworks.persistence.entity.util.VariablesVisibility;
import io.hops.hopsworks.restutils.RESTLogLevel;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.protocol.HdfsConstants;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class Settings implements Serializable {

  private static final Logger LOGGER = Logger.getLogger(Settings.class.
      getName());

  @EJB
  private UserFacade userFacade;
  @EJB
  private OSProcessExecutor osProcessExecutor;

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  private static final Map<String, TimeUnit> TIME_SUFFIXES;

  static {
    TIME_SUFFIXES = new HashMap<>(5);
    TIME_SUFFIXES.put("ms", TimeUnit.MILLISECONDS);
    TIME_SUFFIXES.put("s", TimeUnit.SECONDS);
    TIME_SUFFIXES.put("m", TimeUnit.MINUTES);
    TIME_SUFFIXES.put("h", TimeUnit.HOURS);
    TIME_SUFFIXES.put("d", TimeUnit.DAYS);
  }
  private static final Pattern TIME_CONF_PATTERN = Pattern.compile("([0-9]+)([a-z]+)?");

  public static final String AGENT_EMAIL = "kagent@hops.io";
  /**
   * Global Variables taken from the DB
   */
  private static final String VARIABLE_ADMIN_EMAIL = "admin_email";
  private static final String VARIABLE_PYPI_REST_ENDPOINT = "pypi_rest_endpoint";
  private static final String VARIABLE_PYPI_INDEXER_TIMER_INTERVAL = "pypi_indexer_timer_interval";
  private static final String VARIABLE_PYPI_INDEXER_TIMER_ENABLED = "pypi_indexer_timer_enabled";
  private static final String VARIABLE_PYPI_SIMPLE_ENDPOINT = "pypi_simple_endpoint";
  private static final String VARIABLE_PYTHON_LIBRARY_UPDATES_MONITOR_INTERVAL =
    "python_library_updates_monitor_interval";
  private static final String VARIABLE_HADOOP_VERSION = "hadoop_version";
  private static final String VARIABLE_KIBANA_IP = "kibana_ip";
  private static final String VARIABLE_LOCALHOST = "localhost";
  private static final String VARIABLE_REQUESTS_VERIFY = "requests_verify";
  private static final String VARIABLE_CLOUD= "cloud";
  private static final String VARIABLE_OPENSEARCH_IP = "elastic_ip";
  private static final String VARIABLE_OPENSEARCH_PORT = "elastic_port";
  private static final String VARIABLE_OPENSEARCH_REST_PORT = "elastic_rest_port";
  private static final String VARIABLE_OPENSEARCH_LOGS_INDEX_EXPIRATION = "elastic_logs_index_expiration";
  private static final String VARIABLE_SPARK_USER = "spark_user";
  private static final String VARIABLE_HDFS_SUPERUSER = "hdfs_user";
  private static final String VARIABLE_HOPSWORKS_USER = "hopsworks_user";
  private static final String VARIABLE_JUPYTER_GROUP = "jupyter_group";
  private static final String VARIABLE_STAGING_DIR = "staging_dir";
  private static final String VARIABLE_AIRFLOW_DIR = "airflow_dir";
  private static final String VARIABLE_JUPYTER_DIR = "jupyter_dir";
  private static final String VARIABLE_JUPYTER_WS_PING_INTERVAL = "jupyter_ws_ping_interval";
  private static final String VARIABLE_SPARK_DIR = "spark_dir";
  private static final String VARIABLE_FLINK_DIR = "flink_dir";
  private static final String VARIABLE_HADOOP_DIR = "hadoop_dir";
  private static final String VARIABLE_HOPSWORKS_DIR = "hopsworks_dir";
  private static final String VARIABLE_SUDOERS_DIR = "sudoers_dir";
  private static final String VARIABLE_YARN_DEFAULT_QUOTA = "yarn_default_quota";
  private static final String VARIABLE_HDFS_DEFAULT_QUOTA = "hdfs_default_quota";
  private static final String VARIABLE_PROJECT_PAYMENT_TYPE = "yarn_default_payment_type";
  private static final String VARIABLE_HDFS_BASE_STORAGE_POLICY = "hdfs_base_storage_policy";
  private static final String VARIABLE_HDFS_LOG_STORAGE_POLICY = "hdfs_log_storage_policy";
  private static final String VARIABLE_MAX_NUM_PROJ_PER_USER
      = "max_num_proj_per_user";
  private static final String VARIABLE_RESERVED_PROJECT_NAMES = "reserved_project_names";
  private static final String VARIABLE_HOPSWORKS_ENTERPRISE = "hopsworks_enterprise";
  private static final String VARIABLE_SPARK_EXECUTOR_MIN_MEMORY = "spark_executor_min_memory";
  
  // HIVE configuration variables
  private static final String VARIABLE_HIVE_SUPERUSER = "hive_superuser";
  private static final String VARIABLE_HIVE_WAREHOUSE = "hive_warehouse";
  private static final String VARIABLE_HIVE_SCRATCHDIR = "hive_scratchdir";
  private static final String VARIABLE_HIVE_SCRATCHDIR_DELAY = "hive_scratchdir_delay";
  private static final String VARIABLE_HIVE_SCRATCHDIR_CLEANER_INTERVAL = "hive_scratchdir_cleaner_interval";
  private static final String VARIABLE_HIVE_DEFAULT_QUOTA = "hive_default_quota";

  private static final String VARIABLE_TWOFACTOR_AUTH = "twofactor_auth";
  private static final String VARIABLE_TWOFACTOR_EXCLUD = "twofactor-excluded-groups";
  private static final String VARIABLE_KAFKA_DIR = "kafka_dir";
  private static final String VARIABLE_KAFKA_USER = "kafka_user";
  private static final String VARIABLE_KAFKA_MAX_NUM_TOPICS = "kafka_max_num_topics";
  private static final String VARIABLE_FILE_PREVIEW_IMAGE_SIZE
      = "file_preview_image_size";
  private static final String VARIABLE_FILE_PREVIEW_TXT_SIZE
      = "file_preview_txt_size";
  private static final String VARIABLE_HOPS_RPC_TLS = "hops_rpc_tls";

  private static final String VARIABLE_KAFKA_NUM_PARTITIONS
      = "kafka_num_partitions";
  private static final String VARIABLE_KAFKA_NUM_REPLICAS = "kafka_num_replicas";
  private static final String VARIABLE_HOPSWORKS_SSL_MASTER_PASSWORD = "hopsworks_master_password";

  private static final String VARIABLE_ANACONDA_DIR = "anaconda_dir";
  private static final String VARIABLE_ANACONDA_ENABLED = "anaconda_enabled";
  private static final String VARIABLE_ANACONDA_DEFAULT_REPO = "conda_default_repo";

  private static final String VARIABLE_DOWNLOAD_ALLOWED = "download_allowed";
  private static final String VARIABLE_HOPSEXAMPLES_VERSION = "hopsexamples_version";

  private static final String VARIABLE_KAGENT_USER = "kagent_user";
  private static final String VARIABLE_KAGENT_LIVENESS_MONITOR_ENABLED = "kagent_liveness_monitor_enabled";
  private static final String VARIABLE_KAGENT_LIVENESS_THRESHOLD = "kagent_liveness_threshold";
  private static final String VARIABLE_RESOURCE_DIRS = "resources";
  private static final String VARIABLE_CERTS_DIRS = "certs_dir";
  private static final String VARIABLE_MAX_STATUS_POLL_RETRY = "max_status_poll_retry";
  private static final String VARIABLE_CERT_MATER_DELAY = "cert_mater_delay";
  private static final String VARIABLE_WHITELIST_USERS_LOGIN = "whitelist_users";
  private static final String VARIABLE_FIRST_TIME_LOGIN = "first_time_login";
  private static final String VARIABLE_SERVICE_DISCOVERY_DOMAIN = "service_discovery_domain";

  private static final String VARIABLE_ZOOKEEPER_VERSION = "zookeeper_version";
  private static final String VARIABLE_GRAFANA_VERSION = "grafana_version";
  private static final String VARIABLE_LOGSTASH_VERSION = "logstash_version";
  private static final String VARIABLE_KIBANA_VERSION = "kibana_version";
  private static final String VARIABLE_FILEBEAT_VERSION = "filebeat_version";
  private static final String VARIABLE_NDB_VERSION = "ndb_version";
  private static final String VARIABLE_LIVY_VERSION = "livy_version";
  private static final String VARIABLE_HIVE2_VERSION = "hive2_version";
  private static final String VARIABLE_TEZ_VERSION = "tez_version";
  private static final String VARIABLE_SPARK_VERSION = "spark_version";
  private static final String VARIABLE_FLINK_VERSION = "flink_version";
  private static final String VARIABLE_EPIPE_VERSION = "epipe_version";
  private static final String VARIABLE_KAFKA_VERSION = "kafka_version";
  private static final String VARIABLE_OPENSEARCH_VERSION = "elastic_version";
  private static final String VARIABLE_TENSORFLOW_VERSION = "tensorflow_version";

  private static final String VARIABLE_KUBE_KSERVE_TENSORFLOW_VERSION = "kube_kserve_tensorflow_version";
  private static final String VARIABLE_HOPSWORKS_VERSION = "hopsworks_version";
  private final static String VARIABLE_LIVY_STARTUP_TIMEOUT = "livy_startup_timeout";
  
  private final static String VARIABLE_USER_SEARCH = "enable_user_search";
  private final static String VARIABLE_REJECT_REMOTE_USER_NO_GROUP = "reject_remote_user_no_group";

  //Used by RESTException to include devMsg or not in response
  private static final String VARIABLE_HOPSWORKS_REST_LOG_LEVEL = "hopsworks_rest_log_level";

  /*
   * -------------------- Data science profile ------------------
   */

  private final static String VARIABLE_ENABLE_DATA_SCIENCE_PROFILE = "enable_data_science_profile";

  /*
   * -------------------- Serving ---------------
   */
  private static final String VARIABLE_SERVING_MONITOR_INT = "serving_monitor_int";
  private static final String VARIABLE_SERVING_CONNECTION_POOL_SIZE = "serving_connection_pool_size";
  private static final String VARIABLE_SERVING_MAX_ROUTE_CONNECTIONS = "serving_max_route_connections";

  /*
   * -------------------- TensorBoard ---------------
   */
  private static final String VARIABLE_TENSORBOARD_MAX_RELOAD_THREADS = "tensorboard_max_reload_threads";

  /*
   * -------------------- Kubernetes ---------------
   */
  private static final String VARIABLE_KUBEMASTER_URL = "kube_master_url";
  private static final String VARIABLE_KUBE_USER = "kube_user";
  private static final String VARIABLE_KUBE_HOPSWORKS_USER = "kube_hopsworks_user";
  private static final String VARIABLE_KUBE_CA_CERTFILE = "kube_ca_certfile";
  private static final String VARIABLE_KUBE_CLIENT_KEYFILE = "kube_client_keyfile";
  private static final String VARIABLE_KUBE_CLIENT_CERTFILE = "kube_client_certfile";
  private static final String VARIABLE_KUBE_CLIENT_KEYPASS = "kube_client_keypass";
  private static final String VARIABLE_KUBE_TRUSTSTORE_PATH = "kube_truststore_path";
  private static final String VARIABLE_KUBE_TRUSTSTORE_KEY = "kube_truststore_key";
  private static final String VARIABLE_KUBE_KEYSTORE_PATH = "kube_keystore_path";
  private static final String VARIABLE_KUBE_KEYSTORE_KEY = "kube_keystore_key";
  private static final String VARIABLE_KUBE_PULL_POLICY = "kube_img_pull_policy";
  private static final String VARIABLE_KUBE_API_MAX_ATTEMPTS = "kube_api_max_attempts";
  private static final String VARIABLE_KUBE_DOCKER_MAX_MEMORY_ALLOCATION = "kube_docker_max_memory_allocation";
  private static final String VARIABLE_KUBE_DOCKER_MAX_GPUS_ALLOCATION = "kube_docker_max_gpus_allocation";
  private static final String VARIABLE_KUBE_DOCKER_MAX_CORES_ALLOCATION = "kube_docker_max_cores_allocation";
  // This property is duplicated in CAConf.java
  private static final String VARIABLE_KUBE_INSTALLED = "kubernetes_installed";
  private static final String VARIABLE_KUBE_KSERVE_INSTALLED = "kube_kserve_installed";
  private static final String VARIABLE_KUBE_KNATIVE_DOMAIN_NAME = "kube_knative_domain_name";
  private static final String VARIABLE_KUBE_SERVING_NODE_LABELS = "kube_serving_node_labels";
  private static final String VARIABLE_KUBE_SERVING_NODE_TOLERATIONS = "kube_serving_node_tolerations";
  private static final String VARIABLE_KUBE_SERVING_MAX_MEMORY_ALLOCATION = "kube_serving_max_memory_allocation";
  private static final String VARIABLE_KUBE_SERVING_MAX_CORES_ALLOCATION = "kube_serving_max_cores_allocation";
  private static final String VARIABLE_KUBE_SERVING_MAX_GPUS_ALLOCATION = "kube_serving_max_gpus_allocation";
  private static final String VARIABLE_KUBE_SERVING_MAX_NUM_INSTANCES = "kube_serving_max_num_instances";
  private static final String VARIABLE_KUBE_SERVING_MIN_NUM_INSTANCES = "kube_serving_min_num_instances";
  private static final String VARIABLE_KUBE_TAINTED_NODES = "kube_tainted_nodes";
  private static final String VARIABLE_KUBE_TAINTED_NODES_MONITOR_INTERVAL =
      "kube_node_taints_monitor_interval";

  /*
   * -------------------- Jupyter ---------------
   */
  private static final String VARIABLE_JUPYTER_HOST = "jupyter_host";
  private static final String VARIABLE_JUPYTER_ORIGIN_SCHEME = "jupyter_origin_scheme";

  private static final String VARIABLE_ENABLE_JUPYTER_PYTHON_KERNEL_NON_KUBERNETES =
    "enable_jupyter_python_kernel_non_kubernetes";

  // JWT Variables
  private static final String VARIABLE_JWT_SIGNATURE_ALGORITHM = "jwt_signature_algorithm";
  private static final String VARIABLE_JWT_LIFETIME_MS = "jwt_lifetime_ms";
  private static final String VARIABLE_JWT_EXP_LEEWAY_SEC = "jwt_exp_leeway_sec";
  private static final String VARIABLE_JWT_SIGNING_KEY_NAME = "jwt_signing_key_name";
  private static final String VARIABLE_JWT_ISSUER_KEY = "jwt_issuer";

  private static final String VARIABLE_SERVICE_MASTER_JWT = "service_master_jwt";
  private static final String VARIABLE_SERVICE_JWT_LIFETIME_MS = "service_jwt_lifetime_ms";
  private static final String VARIABLE_SERVICE_JWT_EXP_LEEWAY_SEC = "service_jwt_exp_leeway_sec";

  private static final String VARIABLE_CONNECTION_KEEPALIVE_TIMEOUT = "keepalive_timeout";

  /* -------------------- Featurestore --------------- */
  private static final String VARIABLE_FEATURESTORE_DEFAULT_QUOTA = "featurestore_default_quota";
  private static final String VARIABLE_FEATURESTORE_DEFAULT_STORAGE_FORMAT = "featurestore_default_storage_format";
  private static final String VARIABLE_FEATURESTORE_JDBC_URL = "featurestore_jdbc_url";
  private static final String VARIABLE_ONLINE_FEATURESTORE = "featurestore_online_enabled";
  private static final String VARIABLE_FG_PREVIEW_LIMIT = "fg_preview_limit";
  private static final String VARIABLE_ONLINE_FEATURESTORE_TS = "featurestore_online_tablespace";
  private static final String VARIABLE_ONLINEFS_THREAD_NUMBER = "onlinefs_service_thread_number";

  private static final String VARIABLE_HIVE_CONF_PATH = "hive_conf_path";
  private static final String VARIABLE_FS_PY_JOB_UTIL_PATH = "fs_py_job_util";
  private static final String VARIABLE_FS_JAVA_JOB_UTIL_PATH = "fs_java_job_util";
  private static final String VARIABLE_HDFS_FILE_OP_JOB_UTIL = "hdfs_file_op_job_util";
  private static final String VARIABLE_HDFS_FILE_OP_JOB_DRIVER_MEM = "hdfs_file_op_job_driver_mem";
  
  // Storage connectors

  private final static String VARIABLE_ENABLE_REDSHIFT_STORAGE_CONNECTORS = "enable_redshift_storage_connectors";
  private final static String VARIABLE_ENABLE_ADLS_STORAGE_CONNECTORS = "enable_adls_storage_connectors";
  private final static String VARIABLE_ENABLE_SNOWFLAKE_STORAGE_CONNECTORS = "enable_snowflake_storage_connectors";
  private final static String VARIABLE_ENABLE_KAFKA_STORAGE_CONNECTORS = "enable_kafka_storage_connectors";
  private final static String VARIABLE_ENABLE_GCS_STORAGE_CONNECTORS = "enable_gcs_storage_connectors";
  private final static String VARIABLE_ENABLE_BIGQUERY_STORAGE_CONNECTORS = "enable_bigquery_storage_connectors";

  //OpenSearch Security
  private static final String VARIABLE_OPENSEARCH_SECURITY_ENABLED = "elastic_opendistro_security_enabled";
  private static final String VARIABLE_OPENSEARCH_HTTPS_ENABLED = "elastic_https_enabled";
  private static final String VARIABLE_OPENSEARCH_ADMIN_USER = "elastic_admin_user";
  private static final String VARIABLE_OPENSEARCH_SERVICE_LOG_USER = "kibana_service_log_viewer";
  private static final String VARIABLE_OPENSEARCH_ADMIN_PASSWORD = "elastic_admin_password";
  private static final String VARIABLE_KIBANA_HTTPS_ENABLED = "kibana_https_enabled";
  private static final String VARIABLE_OPENSEARCH_JWT_ENABLED = "elastic_jwt_enabled";
  private static final String VARIABLE_OPENSEARCH_JWT_URL_PARAMETER = "elastic_jwt_url_parameter";
  private static final String VARIABLE_OPENSEARCH_JWT_EXP_MS = "elastic_jwt_exp_ms";
  private static final String VARIABLE_KIBANA_MULTI_TENANCY_ENABLED = "kibana_multi_tenancy_enabled";

  private static final String VARIABLE_CLIENT_PATH = "client_path";
  
  //Cloud
  private static final String VARIABLE_CLOUD_EVENTS_ENDPOINT=
      "cloud_events_endpoint";
  private static final String VARIABLE_CLOUD_EVENTS_ENDPOINT_API_KEY=
      "cloud_events_endpoint_api_key";

  /*-----------------------Yarn Docker------------------------*/
  private final static String VARIABLE_YARN_RUNTIME = "yarn_runtime";
  private final static String VARIABLE_DOCKER_MOUNTS = "docker_mounts";
  private final static String VARIABLE_DOCKER_JOB_MOUNTS_LIST = "docker_job_mounts_list";
  private final static String VARIABLE_DOCKER_JOB_MOUNT_ALLOWED = "docker_job_mounts_allowed";
  private final static String VARIABLE_DOCKER_JOB_UID_STRICT = "docker_job_uid_strict";
  private final static String VARIABLE_DOCKER_BASE_IMAGE_PYTHON_NAME = "docker_base_image_python_name";
  private final static String VARIABLE_DOCKER_BASE_IMAGE_PYTHON_VERSION = "docker_base_image_python_version";
  private final static String VARIABLE_YARN_APP_UID = "yarn_app_uid";

  /*-----------------------Jobs - Executions-------------------*/
  private final static String VARIABLE_EXECUTIONS_PER_JOB_LIMIT = "executions_per_job_limit";
  private final static String VARIABLE_EXECUTIONS_CLEANER_BATCH_SIZE = "executions_cleaner_batch_size";
  private final static String VARIABLE_EXECUTIONS_CLEANER_INTERVAL_MS = "executions_cleaner_interval_ms";

  /*----------------------Yarn Nodemanager status------------*/
  private static final String VARIABLE_CHECK_NODEMANAGERS_STATUS = "check_nodemanagers_status";

  /*----------------------- Python ------------------------*/
  private final static String VARIABLE_MAX_ENV_YML_BYTE_SIZE = "max_env_yml_byte_size";

  //Git
  private static final String VARIABLE_GIT_IMAGE_VERSION = "git_image_version";
  private static final String VARIABLE_GIT_COMMAND_TIMEOUT_MINUTES_DEFAULT = "git_command_timeout_minutes";
  private static final String VARIABLE_ENABLE_GIT_READ_ONLY_REPOSITORIES = "enable_read_only_git_repositories";

  private static final String VARIABLE_MAX_LONG_RUNNING_HTTP_REQUESTS = "max_allowed_long_running_http_requests";

  /*
   * ------------------ QUOTAS ------------------
   */
  private static final String QUOTAS_PREFIX = "quotas";
  private static final String QUOTAS_FEATUREGROUPS_PREFIX = String.format("%s_featuregroups", QUOTAS_PREFIX);
  private static final String VARIABLE_QUOTAS_ONLINE_ENABLED_FEATUREGROUPS = String.format("%s_online_enabled",
          QUOTAS_FEATUREGROUPS_PREFIX);
  private static final String VARIABLE_QUOTAS_ONLINE_DISABLED_FEATUREGROUPS = String.format("%s_online_disabled",
          QUOTAS_FEATUREGROUPS_PREFIX);
  private static final String VARIABLE_QUOTAS_TRAINING_DATASETS = String.format("%s_training_datasets", QUOTAS_PREFIX);
  private static final String QUOTAS_MODEL_DEPLOYMENTS_PREFIX = String.format("%s_model_deployments", QUOTAS_PREFIX);
  private static final String VARIABLE_QUOTAS_RUNNING_MODEL_DEPLOYMENTS = String.format("%s_running",
          QUOTAS_MODEL_DEPLOYMENTS_PREFIX);
  private static final String VARIABLE_QUOTAS_TOTAL_MODEL_DEPLOYMENTS = String.format("%s_total",
          QUOTAS_MODEL_DEPLOYMENTS_PREFIX);
  private static final String VARIABLE_QUOTAS_MAX_PARALLEL_EXECUTIONS = String.format("%s_max_parallel_executions",
          QUOTAS_PREFIX);

  //Docker cgroups
  private static final String VARIABLE_DOCKER_CGROUP_ENABLED = "docker_cgroup_enabled";
  private static final String VARIABLE_DOCKER_CGROUP_HARD_LIMIT_MEMORY = "docker_cgroup_memory_limit_gb";
  private static final String VARIABLE_DOCKER_CGROUP_SOFT_LIMIT_MEMORY = "docker_cgroup_soft_limit_memory_gb";
  private static final String VARIABLE_DOCKER_CGROUP_CPU_QUOTA = "docker_cgroup_cpu_quota_percentage";
  private static final String VARIABLE_DOCKER_CGROUP_CPU_PERIOD = "docker_cgroup_cpu_period";
  private static final String VARIABLE_DOCKER_CGROUP_MONITOR_INTERVAL = "docker_cgroup_monitor_interval";

  private static final String VARIABLE_PROMETHEUS_PORT = "prometheus_port";

  private static final String VARIABLE_SKIP_NAMESPACE_CREATION =
      "kube_skip_namespace_creation";
  public enum KubeType{
    Local("local"),
    EKS("eks"),
    AKS("aks");
    private String name;
    KubeType(String name){
      this.name = name;
    }
    
    static KubeType fromString(String str){
      if(str.equals(Local.name)){
        return Local;
      }else if(str.equals(EKS.name)){
        return EKS;
      }else if(str.equals(AKS.name)) {
        return AKS;
      }
      return Local;
    }
  }
  // This property is duplicated in CAConf.java
  private static final String VARIABLE_KUBE_TYPE = "kube_type";
  private static final String VARIABLE_DOCKER_NAMESPACE = "docker_namespace";
  private static final String VARIABLE_MANAGED_DOCKER_REGISTRY =
      "managed_docker_registry";
  
  private String setVar(String varName, String defaultValue) {
    return setStrVar(varName, defaultValue);
  }

  private String setStrVar(String varName, String defaultValue) {
    Optional<Variables> variable = findById(varName);
    if (variable.isPresent()) {
      String value = variable.get().getValue();
      if (!Strings.isNullOrEmpty(value)) {
        return value;
      }
    }
    return defaultValue;
  }

  private String setDirVar(String varName, String defaultValue) {
    Optional<Variables> dirName = findById(varName);
    if (dirName.isPresent()) {
      String value = dirName.get().getValue();
      if (!Strings.isNullOrEmpty(value) && new File(value).isDirectory()) {
        return value;
      }
    }
    return defaultValue;
  }

  private String setIpVar(String varName, String defaultValue) {
    Optional<Variables> variable = findById(varName);
    if (variable.isPresent()) {
      String value = variable.get().getValue();
      if (!Strings.isNullOrEmpty(value) && Ip.validIp(value)) {
        return value;
      }
    }

    return defaultValue;
  }

  private Boolean setBoolVar(String varName, Boolean defaultValue) {
    Optional<Variables> variable = findById(varName);
    if (variable.isPresent()) {
      String value = variable.get().getValue();
      if (!Strings.isNullOrEmpty(value)) {
        return Boolean.parseBoolean(value);
      }
    }

    return defaultValue;
  }

  private Integer setIntVar(String varName, Integer defaultValue) {
    Optional<Variables> variable = findById(varName);
    if (variable.isPresent()) {
      String value = variable.get().getValue();
      try {
        if (!Strings.isNullOrEmpty(value)) {
          return Integer.parseInt(value);
        }
      } catch(NumberFormatException ex){
        LOGGER.log(Level.WARNING,
            "Error - not an integer! " + varName + " should be an integer. Value was " + value);
      }
    }
    return defaultValue;
  }

  private Double setDoubleVar(String varName, Double defaultValue) {
    Optional<Variables> variable = findById(varName);
    if (variable.isPresent()) {
      String value = variable.get().getValue();
      try {
        if (!Strings.isNullOrEmpty(value)) {
          return Double.parseDouble(value);
        }
      } catch(NumberFormatException ex){
        LOGGER.log(Level.WARNING, "Error - not a double! " + varName + " should be a double. Value was " + value);
      }
    }

    return defaultValue;
  }

  private long setLongVar(String varName, Long defaultValue) {
    Optional<Variables> variable = findById(varName);
    if (variable.isPresent()) {
      String value = variable.get().getValue();
      try {
        if (!Strings.isNullOrEmpty(value)) {
          return Long.parseLong(value);
        }
      } catch (NumberFormatException ex) {
        LOGGER.log(Level.WARNING, "Error - not a long! " + varName + " should be an integer. Value was " + value);
      }
    }

    return defaultValue;
  }

  private RESTLogLevel setLogLevelVar(String varName, RESTLogLevel defaultValue) {
    Optional<Variables> variable = findById(varName);
    if (variable.isPresent()) {
      String value = variable.get().getValue();
      if (!Strings.isNullOrEmpty(value)) {
        return RESTLogLevel.valueOf(value);
      }
    }
    return defaultValue;
  }

  private long setMillisecondVar(String varName, Long defaultValue) {
    Optional<Variables> variable = findById(varName);
    if (variable.isPresent()) {
      String value = variable.get().getValue();
      if (!Strings.isNullOrEmpty(value)) {
        long timeValue = getConfTimeValue(value);
        TimeUnit timeUnit = getConfTimeTimeUnit(value);
        return timeUnit.toMillis(timeValue);
      }
    }

    return defaultValue;
  }

  private PaymentType setPaymentType(String varName, PaymentType defaultValue) {
    Optional<Variables> variable = findById(varName);
    if (variable.isPresent()) {
      String value = variable.get().getValue();
      if (!Strings.isNullOrEmpty(value)) {
        return PaymentType.valueOf(value);
      }
    }
    return defaultValue;
  }

  private Set<String> setStringHashSetLowerCase(String varName, String defaultValue, String separator) {
    RESERVED_PROJECT_NAMES_STR = setStrVar(varName, defaultValue);
    return setStringHashSetLowerCase(RESERVED_PROJECT_NAMES_STR, separator, true);
  }
  
  private Set<String> setStringHashSetLowerCase(String values, String separator, boolean toLowerCase) {
    StringTokenizer tokenizer = new StringTokenizer(values, separator);
    HashSet<String> tokens = new HashSet<>(tokenizer.countTokens());
    while (tokenizer.hasMoreTokens()) {
      tokens.add(toLowerCase? tokenizer.nextToken().trim().toLowerCase() : tokenizer.nextToken().trim());
    }
    return tokens;
  }

  private boolean cached = false;
  private UUID myUUID;
  private ITopic<String> settingUpdatedTopic;
  
  @Inject
  private HazelcastInstance hazelcastInstance;
  
  @PostConstruct
  public void init() {
    // hazelcastInstance == null if Hazelcast is Disabled
    if (hazelcastInstance != null) {
      settingUpdatedTopic = hazelcastInstance.getTopic("setting_updated");
      myUUID = settingUpdatedTopic.addMessageListener(new MessageListenerImpl());
    }
  }
  
  @PreDestroy
  public void destroy() {
    if (settingUpdatedTopic != null) {
      //needed for redeploy to remove the listener
      settingUpdatedTopic.removeMessageListener(myUUID);
    }
  }
  
  public class MessageListenerImpl implements MessageListener<String> {
    
    @Override
    public void onMessage(Message<String> message) {
      if (!message.getPublishingMember().localMember()) {
        cached = false;
      }
    }
  }

  private void populateCache() {
    if (!cached) {
      ADMIN_EMAIL = setVar(VARIABLE_ADMIN_EMAIL, ADMIN_EMAIL);
      LOCALHOST = setBoolVar(VARIABLE_LOCALHOST, LOCALHOST);
      CLOUD = setStrVar(VARIABLE_CLOUD, CLOUD);
      REQUESTS_VERIFY = setBoolVar(VARIABLE_REQUESTS_VERIFY, REQUESTS_VERIFY);
      TWOFACTOR_AUTH = setVar(VARIABLE_TWOFACTOR_AUTH, TWOFACTOR_AUTH);
      TWOFACTOR_EXCLUDE = setVar(VARIABLE_TWOFACTOR_EXCLUD, TWOFACTOR_EXCLUDE);
      HOPSWORKS_USER = setVar(VARIABLE_HOPSWORKS_USER, HOPSWORKS_USER);
      JUPYTER_GROUP = setVar(VARIABLE_JUPYTER_GROUP, JUPYTER_GROUP);
      JUPYTER_ORIGIN_SCHEME = setVar(VARIABLE_JUPYTER_ORIGIN_SCHEME, JUPYTER_ORIGIN_SCHEME);
      HDFS_SUPERUSER = setVar(VARIABLE_HDFS_SUPERUSER, HDFS_SUPERUSER);
      SPARK_USER = setVar(VARIABLE_SPARK_USER, SPARK_USER);
      SPARK_DIR = setDirVar(VARIABLE_SPARK_DIR, SPARK_DIR);
      FLINK_DIR = setDirVar(VARIABLE_FLINK_DIR, FLINK_DIR);
      STAGING_DIR = setDirVar(VARIABLE_STAGING_DIR, STAGING_DIR);
      HOPS_EXAMPLES_VERSION = setVar(VARIABLE_HOPSEXAMPLES_VERSION, HOPS_EXAMPLES_VERSION);
      HIVE_SUPERUSER = setStrVar(VARIABLE_HIVE_SUPERUSER, HIVE_SUPERUSER);
      HIVE_WAREHOUSE = setStrVar(VARIABLE_HIVE_WAREHOUSE, HIVE_WAREHOUSE);
      HIVE_SCRATCHDIR = setStrVar(VARIABLE_HIVE_SCRATCHDIR, HIVE_SCRATCHDIR);
      HIVE_SCRATCHDIR_DELAY = setStrVar(VARIABLE_HIVE_SCRATCHDIR_DELAY, HIVE_SCRATCHDIR_DELAY);
      HIVE_SCRATCHDIR_CLEANER_INTERVAL = setStrVar(VARIABLE_HIVE_SCRATCHDIR_CLEANER_INTERVAL,
          HIVE_SCRATCHDIR_CLEANER_INTERVAL);
      HIVE_DB_DEFAULT_QUOTA = setLongVar(VARIABLE_HIVE_DEFAULT_QUOTA, HIVE_DB_DEFAULT_QUOTA);
      HADOOP_VERSION = setVar(VARIABLE_HADOOP_VERSION, HADOOP_VERSION);
      JUPYTER_DIR = setDirVar(VARIABLE_JUPYTER_DIR, JUPYTER_DIR);
      JUPYTER_WS_PING_INTERVAL_MS = setMillisecondVar(VARIABLE_JUPYTER_WS_PING_INTERVAL, JUPYTER_WS_PING_INTERVAL_MS);
      HADOOP_DIR = setDirVar(VARIABLE_HADOOP_DIR, HADOOP_DIR);
      HOPSWORKS_INSTALL_DIR = setDirVar(VARIABLE_HOPSWORKS_DIR, HOPSWORKS_INSTALL_DIR);
      CERTS_DIR = setDirVar(VARIABLE_CERTS_DIRS, CERTS_DIR);
      SUDOERS_DIR = setDirVar(VARIABLE_SUDOERS_DIR, SUDOERS_DIR);
      SERVICE_DISCOVERY_DOMAIN = setStrVar(VARIABLE_SERVICE_DISCOVERY_DOMAIN, SERVICE_DISCOVERY_DOMAIN);
      AIRFLOW_DIR = setDirVar(VARIABLE_AIRFLOW_DIR, AIRFLOW_DIR);
      String openSearchIps = setStrVar(VARIABLE_OPENSEARCH_IP,
          OpenSearchSettings.OPENSEARCH_IP_DEFAULT);
      int openSearchPort = setIntVar(VARIABLE_OPENSEARCH_PORT, OpenSearchSettings.OPENSEARCH_PORT_DEFAULT);
      int openSearchRestPort = setIntVar(VARIABLE_OPENSEARCH_REST_PORT,
          OpenSearchSettings.OPENSEARCH_REST_PORT_DEFAULT);
      boolean openSearchSecurityEnabled =
          setBoolVar(VARIABLE_OPENSEARCH_SECURITY_ENABLED,
              OpenSearchSettings.OPENSEARCH_SECURTIY_ENABLED_DEFAULT);
      boolean openSearchHttpsEnabled = setBoolVar(VARIABLE_OPENSEARCH_HTTPS_ENABLED
          , OpenSearchSettings.OPENSEARCH_HTTPS_ENABLED_DEFAULT);
      String openSearchAdminUser = setStrVar(VARIABLE_OPENSEARCH_ADMIN_USER,
          OpenSearchSettings.OPENSEARCH_ADMIN_USER_DEFAULT);
      String openSearchServiceLogUser = setStrVar(VARIABLE_OPENSEARCH_SERVICE_LOG_USER,
          OpenSearchSettings.OPENSEARCH_SERVICE_LOG_ROLE);
      String openSearchAdminPassword = setStrVar(VARIABLE_OPENSEARCH_ADMIN_PASSWORD,
          OpenSearchSettings.OPENSEARCH_ADMIN_PASSWORD_DEFAULT);
      boolean openSearchJWTEnabled =  setBoolVar(VARIABLE_OPENSEARCH_JWT_ENABLED
          , OpenSearchSettings.OPENSEARCH_JWT_ENABLED_DEFAULT);
      String openSearchJWTUrlParameter = setStrVar(VARIABLE_OPENSEARCH_JWT_URL_PARAMETER,
          OpenSearchSettings.OPENSEARCH_JWT_URL_PARAMETER_DEFAULT);
      long openSearchJWTEXPMS = setLongVar(VARIABLE_OPENSEARCH_JWT_EXP_MS,
          OpenSearchSettings.OPENSEARCH_JWT_EXP_MS_DEFAULT);
      OPENSEARCH_SETTINGS = new OpenSearchSettings(openSearchIps, openSearchPort,
          openSearchRestPort, openSearchSecurityEnabled, openSearchHttpsEnabled,
          openSearchAdminUser, openSearchAdminPassword, openSearchJWTEnabled,
          openSearchJWTUrlParameter, openSearchJWTEXPMS, openSearchServiceLogUser);
      OpenSearch_LOGS_INDEX_EXPIRATION = setLongVar(VARIABLE_OPENSEARCH_LOGS_INDEX_EXPIRATION,
        OpenSearch_LOGS_INDEX_EXPIRATION);
      KIBANA_IP = setIpVar(VARIABLE_KIBANA_IP, KIBANA_IP);
      KAFKA_MAX_NUM_TOPICS = setIntVar(VARIABLE_KAFKA_MAX_NUM_TOPICS, KAFKA_MAX_NUM_TOPICS);
      HOPSWORKS_DEFAULT_SSL_MASTER_PASSWORD = setVar(VARIABLE_HOPSWORKS_SSL_MASTER_PASSWORD,
          HOPSWORKS_DEFAULT_SSL_MASTER_PASSWORD);
      KAFKA_USER = setVar(VARIABLE_KAFKA_USER, KAFKA_USER);
      KAFKA_DIR = setDirVar(VARIABLE_KAFKA_DIR, KAFKA_DIR);
      KAFKA_DEFAULT_NUM_PARTITIONS = setIntVar(VARIABLE_KAFKA_NUM_PARTITIONS, KAFKA_DEFAULT_NUM_PARTITIONS);
      KAFKA_DEFAULT_NUM_REPLICAS = setIntVar(VARIABLE_KAFKA_NUM_REPLICAS, KAFKA_DEFAULT_NUM_REPLICAS);
      YARN_DEFAULT_QUOTA = setIntVar(VARIABLE_YARN_DEFAULT_QUOTA, YARN_DEFAULT_QUOTA);
      DEFAULT_PAYMENT_TYPE = setPaymentType(VARIABLE_PROJECT_PAYMENT_TYPE, DEFAULT_PAYMENT_TYPE);
      HDFS_DEFAULT_QUOTA_MBs = setLongVar(VARIABLE_HDFS_DEFAULT_QUOTA, HDFS_DEFAULT_QUOTA_MBs);
      HDFS_BASE_STORAGE_POLICY = setHdfsStoragePolicy(VARIABLE_HDFS_BASE_STORAGE_POLICY, HDFS_BASE_STORAGE_POLICY);
      HDFS_LOG_STORAGE_POLICY = setHdfsStoragePolicy(VARIABLE_HDFS_LOG_STORAGE_POLICY, HDFS_LOG_STORAGE_POLICY);
      MAX_NUM_PROJ_PER_USER = setIntVar(VARIABLE_MAX_NUM_PROJ_PER_USER, MAX_NUM_PROJ_PER_USER);
      FILE_PREVIEW_IMAGE_SIZE = setIntVar(VARIABLE_FILE_PREVIEW_IMAGE_SIZE, 10000000);
      FILE_PREVIEW_TXT_SIZE = setIntVar(VARIABLE_FILE_PREVIEW_TXT_SIZE, 100);
      ANACONDA_DIR = setDirVar(VARIABLE_ANACONDA_DIR, ANACONDA_DIR);
      ANACONDA_DEFAULT_REPO = setStrVar(VARIABLE_ANACONDA_DEFAULT_REPO, ANACONDA_DEFAULT_REPO);
      ANACONDA_ENABLED = Boolean.parseBoolean(setStrVar(
          VARIABLE_ANACONDA_ENABLED, ANACONDA_ENABLED.toString()));
      KAGENT_USER = setStrVar(VARIABLE_KAGENT_USER, KAGENT_USER);
      KAGENT_LIVENESS_MONITOR_ENABLED = setBoolVar(VARIABLE_KAGENT_LIVENESS_MONITOR_ENABLED,
          KAGENT_LIVENESS_MONITOR_ENABLED);
      KAGENT_LIVENESS_THRESHOLD = setStrVar(VARIABLE_KAGENT_LIVENESS_THRESHOLD, KAGENT_LIVENESS_THRESHOLD);
      DOWNLOAD_ALLOWED = Boolean.parseBoolean(setStrVar(VARIABLE_DOWNLOAD_ALLOWED, DOWNLOAD_ALLOWED.toString()));
      RESOURCE_DIRS = setStrVar(VARIABLE_RESOURCE_DIRS, RESOURCE_DIRS);
      MAX_STATUS_POLL_RETRY = setIntVar(VARIABLE_MAX_STATUS_POLL_RETRY, MAX_STATUS_POLL_RETRY);
      HOPS_RPC_TLS = setStrVar(VARIABLE_HOPS_RPC_TLS, HOPS_RPC_TLS);
      CERTIFICATE_MATERIALIZER_DELAY = setStrVar(VARIABLE_CERT_MATER_DELAY,
          CERTIFICATE_MATERIALIZER_DELAY);
      WHITELIST_USERS_LOGIN = setStrVar(VARIABLE_WHITELIST_USERS_LOGIN,
          WHITELIST_USERS_LOGIN);
      FIRST_TIME_LOGIN = setStrVar(VARIABLE_FIRST_TIME_LOGIN, FIRST_TIME_LOGIN);
      serviceKeyRotationEnabled = setBoolVar(SERVICE_KEY_ROTATION_ENABLED_KEY, serviceKeyRotationEnabled);
      serviceKeyRotationInterval = setStrVar(SERVICE_KEY_ROTATION_INTERVAL_KEY, serviceKeyRotationInterval);
      tensorBoardMaxLastAccessed = setIntVar(TENSORBOARD_MAX_LAST_ACCESSED, tensorBoardMaxLastAccessed);
      sparkUILogsOffset = setIntVar(SPARK_UI_LOGS_OFFSET, sparkUILogsOffset);
      jupyterShutdownTimerInterval = setStrVar(JUPYTER_SHUTDOWN_TIMER_INTERVAL, jupyterShutdownTimerInterval);
      checkNodemanagersStatus = setBoolVar(VARIABLE_CHECK_NODEMANAGERS_STATUS, checkNodemanagersStatus);

      populateLDAPCache();

      ZOOKEEPER_VERSION = setStrVar(VARIABLE_ZOOKEEPER_VERSION, ZOOKEEPER_VERSION);
      GRAFANA_VERSION = setStrVar(VARIABLE_GRAFANA_VERSION, GRAFANA_VERSION);
      LOGSTASH_VERSION = setStrVar(VARIABLE_LOGSTASH_VERSION, LOGSTASH_VERSION);
      KIBANA_VERSION = setStrVar(VARIABLE_KIBANA_VERSION, KIBANA_VERSION);
      FILEBEAT_VERSION = setStrVar(VARIABLE_FILEBEAT_VERSION, FILEBEAT_VERSION);
      NDB_VERSION = setStrVar(VARIABLE_NDB_VERSION, NDB_VERSION);
      LIVY_VERSION = setStrVar(VARIABLE_LIVY_VERSION, LIVY_VERSION);
      HIVE2_VERSION = setStrVar(VARIABLE_HIVE2_VERSION, HIVE2_VERSION);
      TEZ_VERSION = setStrVar(VARIABLE_TEZ_VERSION, TEZ_VERSION);
      SPARK_VERSION = setStrVar(VARIABLE_SPARK_VERSION, SPARK_VERSION);
      FLINK_VERSION = setStrVar(VARIABLE_FLINK_VERSION, FLINK_VERSION);
      EPIPE_VERSION = setStrVar(VARIABLE_EPIPE_VERSION, EPIPE_VERSION);
      KAFKA_VERSION = setStrVar(VARIABLE_KAFKA_VERSION, KAFKA_VERSION);
      OPENSEARCH_VERSION = setStrVar(VARIABLE_OPENSEARCH_VERSION, OPENSEARCH_VERSION);
      TENSORFLOW_VERSION = setStrVar(VARIABLE_TENSORFLOW_VERSION, TENSORFLOW_VERSION);
      KUBE_KSERVE_TENSORFLOW_VERSION = setStrVar(VARIABLE_KUBE_KSERVE_TENSORFLOW_VERSION,
          KUBE_KSERVE_TENSORFLOW_VERSION);
      HOPSWORKS_VERSION = setStrVar(VARIABLE_HOPSWORKS_VERSION, HOPSWORKS_VERSION);
      HOPSWORKS_REST_LOG_LEVEL = setLogLevelVar(VARIABLE_HOPSWORKS_REST_LOG_LEVEL, HOPSWORKS_REST_LOG_LEVEL);
      HOPSWORKS_PUBLIC_HOST = setStrVar(VARIABLE_HOPSWORKS_PUBLIC_HOST, HOPSWORKS_PUBLIC_HOST);

      PYPI_REST_ENDPOINT = setStrVar(VARIABLE_PYPI_REST_ENDPOINT, PYPI_REST_ENDPOINT);
      PYPI_SIMPLE_ENDPOINT = setStrVar(VARIABLE_PYPI_SIMPLE_ENDPOINT, PYPI_SIMPLE_ENDPOINT);
      PYPI_INDEXER_TIMER_INTERVAL = setStrVar(VARIABLE_PYPI_INDEXER_TIMER_INTERVAL, PYPI_INDEXER_TIMER_INTERVAL);
      PYTHON_LIBRARY_UPDATES_MONITOR_INTERVAL = setStrVar(VARIABLE_PYTHON_LIBRARY_UPDATES_MONITOR_INTERVAL,
        PYTHON_LIBRARY_UPDATES_MONITOR_INTERVAL);
      PYPI_INDEXER_TIMER_ENABLED = setBoolVar(VARIABLE_PYPI_INDEXER_TIMER_ENABLED, PYPI_INDEXER_TIMER_ENABLED);

      IMMUTABLE_PYTHON_LIBRARY_NAMES = toSetFromCsv(
          setStrVar(VARIABLE_IMMUTABLE_PYTHON_LIBRARY_NAMES, DEFAULT_IMMUTABLE_PYTHON_LIBRARY_NAMES),
          ",");

      ENABLE_DATA_SCIENCE_PROFILE = setBoolVar(VARIABLE_ENABLE_DATA_SCIENCE_PROFILE, ENABLE_DATA_SCIENCE_PROFILE);

      SERVING_MONITOR_INT = setStrVar(VARIABLE_SERVING_MONITOR_INT, SERVING_MONITOR_INT);
      SERVING_CONNECTION_POOL_SIZE = setIntVar(VARIABLE_SERVING_CONNECTION_POOL_SIZE,
        SERVING_CONNECTION_POOL_SIZE);
      SERVING_MAX_ROUTE_CONNECTIONS = setIntVar(VARIABLE_SERVING_MAX_ROUTE_CONNECTIONS,
        SERVING_MAX_ROUTE_CONNECTIONS);

      TENSORBOARD_MAX_RELOAD_THREADS = setIntVar(VARIABLE_TENSORBOARD_MAX_RELOAD_THREADS,
          TENSORBOARD_MAX_RELOAD_THREADS);

      KUBE_USER = setStrVar(VARIABLE_KUBE_USER, KUBE_USER);
      KUBE_HOPSWORKS_USER = setStrVar(VARIABLE_KUBE_HOPSWORKS_USER, KUBE_HOPSWORKS_USER);
      KUBEMASTER_URL = setStrVar(VARIABLE_KUBEMASTER_URL, KUBEMASTER_URL);
      KUBE_CA_CERTFILE = setStrVar(VARIABLE_KUBE_CA_CERTFILE, KUBE_CA_CERTFILE);
      KUBE_CLIENT_KEYFILE = setStrVar(VARIABLE_KUBE_CLIENT_KEYFILE, KUBE_CLIENT_KEYFILE);
      KUBE_CLIENT_CERTFILE = setStrVar(VARIABLE_KUBE_CLIENT_CERTFILE, KUBE_CLIENT_CERTFILE);
      KUBE_CLIENT_KEYPASS = setStrVar(VARIABLE_KUBE_CLIENT_KEYPASS, KUBE_CLIENT_KEYPASS);
      KUBE_TRUSTSTORE_PATH = setStrVar(VARIABLE_KUBE_TRUSTSTORE_PATH, KUBE_TRUSTSTORE_PATH);
      KUBE_TRUSTSTORE_KEY = setStrVar(VARIABLE_KUBE_TRUSTSTORE_KEY, KUBE_TRUSTSTORE_KEY);
      KUBE_KEYSTORE_PATH = setStrVar(VARIABLE_KUBE_KEYSTORE_PATH, KUBE_KEYSTORE_PATH);
      KUBE_KEYSTORE_KEY = setStrVar(VARIABLE_KUBE_KEYSTORE_KEY, KUBE_KEYSTORE_KEY);
      KUBE_PULL_POLICY = setStrVar(VARIABLE_KUBE_PULL_POLICY, KUBE_PULL_POLICY);
      KUBE_API_MAX_ATTEMPTS = setIntVar(VARIABLE_KUBE_API_MAX_ATTEMPTS, KUBE_API_MAX_ATTEMPTS);
      KUBE_DOCKER_MAX_MEMORY_ALLOCATION = setIntVar(VARIABLE_KUBE_DOCKER_MAX_MEMORY_ALLOCATION,
          KUBE_DOCKER_MAX_MEMORY_ALLOCATION);
      KUBE_DOCKER_MAX_CORES_ALLOCATION = setDoubleVar(VARIABLE_KUBE_DOCKER_MAX_CORES_ALLOCATION,
          KUBE_DOCKER_MAX_CORES_ALLOCATION);
      KUBE_DOCKER_MAX_GPUS_ALLOCATION = setIntVar(VARIABLE_KUBE_DOCKER_MAX_GPUS_ALLOCATION,
          KUBE_DOCKER_MAX_GPUS_ALLOCATION);
      KUBE_INSTALLED = setBoolVar(VARIABLE_KUBE_INSTALLED, KUBE_INSTALLED);
      KUBE_KSERVE_INSTALLED = setBoolVar(VARIABLE_KUBE_KSERVE_INSTALLED, KUBE_KSERVE_INSTALLED);
      KUBE_SERVING_NODE_LABELS = setStrVar(VARIABLE_KUBE_SERVING_NODE_LABELS, KUBE_SERVING_NODE_LABELS);
      KUBE_SERVING_NODE_TOLERATIONS = setStrVar(VARIABLE_KUBE_SERVING_NODE_TOLERATIONS, KUBE_SERVING_NODE_TOLERATIONS);
      KUBE_SERVING_MAX_MEMORY_ALLOCATION = setIntVar(VARIABLE_KUBE_SERVING_MAX_MEMORY_ALLOCATION,
        KUBE_SERVING_MAX_MEMORY_ALLOCATION);
      KUBE_SERVING_MAX_CORES_ALLOCATION = setDoubleVar(VARIABLE_KUBE_SERVING_MAX_CORES_ALLOCATION,
        KUBE_SERVING_MAX_CORES_ALLOCATION);
      KUBE_SERVING_MAX_GPUS_ALLOCATION = setIntVar(VARIABLE_KUBE_SERVING_MAX_GPUS_ALLOCATION,
        KUBE_SERVING_MAX_GPUS_ALLOCATION);
      KUBE_SERVING_MAX_NUM_INSTANCES = setIntVar(VARIABLE_KUBE_SERVING_MAX_NUM_INSTANCES,
        KUBE_SERVING_MAX_NUM_INSTANCES);
      KUBE_SERVING_MIN_NUM_INSTANCES = setIntVar(VARIABLE_KUBE_SERVING_MIN_NUM_INSTANCES,
        KUBE_SERVING_MIN_NUM_INSTANCES);
      KUBE_KNATIVE_DOMAIN_NAME = setStrVar(VARIABLE_KUBE_KNATIVE_DOMAIN_NAME, KUBE_KNATIVE_DOMAIN_NAME);
      KUBE_TAINTED_NODES = setStrVar(VARIABLE_KUBE_TAINTED_NODES, KUBE_TAINTED_NODES);
      KUBE_TAINTED_NODES_MONITOR_INTERVAL = setStrVar(VARIABLE_KUBE_TAINTED_NODES_MONITOR_INTERVAL,
          KUBE_TAINTED_NODES_MONITOR_INTERVAL);
  
      HOPSWORKS_ENTERPRISE = setBoolVar(VARIABLE_HOPSWORKS_ENTERPRISE, HOPSWORKS_ENTERPRISE);

      JUPYTER_HOST = setStrVar(VARIABLE_JUPYTER_HOST, JUPYTER_HOST);

      JWT_SIGNATURE_ALGORITHM = setStrVar(VARIABLE_JWT_SIGNATURE_ALGORITHM, JWT_SIGNATURE_ALGORITHM);
      JWT_LIFETIME_MS = setLongVar(VARIABLE_JWT_LIFETIME_MS, JWT_LIFETIME_MS);
      JWT_EXP_LEEWAY_SEC = setIntVar(VARIABLE_JWT_EXP_LEEWAY_SEC, JWT_EXP_LEEWAY_SEC);
      JWT_SIGNING_KEY_NAME = setStrVar(VARIABLE_JWT_SIGNING_KEY_NAME, JWT_SIGNING_KEY_NAME);
      JWT_ISSUER = setStrVar(VARIABLE_JWT_ISSUER_KEY, JWT_ISSUER);

      SERVICE_JWT_LIFETIME_MS = setLongVar(VARIABLE_SERVICE_JWT_LIFETIME_MS, SERVICE_JWT_LIFETIME_MS);
      SERVICE_JWT_EXP_LEEWAY_SEC = setIntVar(VARIABLE_SERVICE_JWT_EXP_LEEWAY_SEC, SERVICE_JWT_EXP_LEEWAY_SEC);

      populateServiceJWTCache();

      CONNECTION_KEEPALIVE_TIMEOUT = setIntVar(VARIABLE_CONNECTION_KEEPALIVE_TIMEOUT, CONNECTION_KEEPALIVE_TIMEOUT);

      FEATURESTORE_DB_DEFAULT_QUOTA = setLongVar(VARIABLE_FEATURESTORE_DEFAULT_QUOTA, FEATURESTORE_DB_DEFAULT_QUOTA);
      FEATURESTORE_DB_DEFAULT_STORAGE_FORMAT =
          setStrVar(VARIABLE_FEATURESTORE_DEFAULT_STORAGE_FORMAT, FEATURESTORE_DB_DEFAULT_STORAGE_FORMAT);
      FEATURESTORE_JDBC_URL = setStrVar(VARIABLE_FEATURESTORE_JDBC_URL, FEATURESTORE_JDBC_URL);
      ONLINE_FEATURESTORE = setBoolVar(VARIABLE_ONLINE_FEATURESTORE, ONLINE_FEATURESTORE);
      ONLINE_FEATURESTORE_TS = setStrVar(VARIABLE_ONLINE_FEATURESTORE_TS, ONLINE_FEATURESTORE_TS);
      ONLINEFS_THREAD_NUMBER = setIntVar(VARIABLE_ONLINEFS_THREAD_NUMBER, ONLINEFS_THREAD_NUMBER);

      KIBANA_HTTPS_ENABELED = setBoolVar(VARIABLE_KIBANA_HTTPS_ENABLED,
          KIBANA_HTTPS_ENABELED);
  
      KIBANA_MULTI_TENANCY_ENABELED = setBoolVar(VARIABLE_KIBANA_MULTI_TENANCY_ENABLED,
          KIBANA_MULTI_TENANCY_ENABELED);
  
      RESERVED_PROJECT_NAMES =
        setStringHashSetLowerCase(VARIABLE_RESERVED_PROJECT_NAMES, DEFAULT_RESERVED_PROJECT_NAMES, ",");
  
      CLOUD_EVENTS_ENDPOINT = setStrVar(VARIABLE_CLOUD_EVENTS_ENDPOINT,
          CLOUD_EVENTS_ENDPOINT);
  
      CLOUD_EVENTS_ENDPOINT_API_KEY =
          setStrVar(VARIABLE_CLOUD_EVENTS_ENDPOINT_API_KEY, CLOUD_EVENTS_ENDPOINT_API_KEY);

      FG_PREVIEW_LIMIT = setIntVar(VARIABLE_FG_PREVIEW_LIMIT, FG_PREVIEW_LIMIT);
      HIVE_CONF_PATH = setStrVar(VARIABLE_HIVE_CONF_PATH, HIVE_CONF_PATH);
      FS_PY_JOB_UTIL_PATH = setStrVar(VARIABLE_FS_PY_JOB_UTIL_PATH, FS_PY_JOB_UTIL_PATH);
      FS_JAVA_JOB_UTIL_PATH  = setStrVar(VARIABLE_FS_JAVA_JOB_UTIL_PATH, FS_JAVA_JOB_UTIL_PATH);
      HDFS_FILE_OP_JOB_UTIL  = setStrVar(VARIABLE_HDFS_FILE_OP_JOB_UTIL, HDFS_FILE_OP_JOB_UTIL);
      HDFS_FILE_OP_JOB_DRIVER_MEM  = setIntVar(VARIABLE_HDFS_FILE_OP_JOB_DRIVER_MEM, HDFS_FILE_OP_JOB_DRIVER_MEM);

      ENABLE_REDSHIFT_STORAGE_CONNECTORS = setBoolVar(VARIABLE_ENABLE_REDSHIFT_STORAGE_CONNECTORS,
              ENABLE_REDSHIFT_STORAGE_CONNECTORS);
      ENABLE_ADLS_STORAGE_CONNECTORS = setBoolVar(VARIABLE_ENABLE_ADLS_STORAGE_CONNECTORS,
              ENABLE_ADLS_STORAGE_CONNECTORS);
      ENABLE_SNOWFLAKE_STORAGE_CONNECTORS = setBoolVar(VARIABLE_ENABLE_SNOWFLAKE_STORAGE_CONNECTORS,
              ENABLE_SNOWFLAKE_STORAGE_CONNECTORS);
      ENABLE_KAFKA_STORAGE_CONNECTORS = setBoolVar(VARIABLE_ENABLE_KAFKA_STORAGE_CONNECTORS,
              ENABLE_KAFKA_STORAGE_CONNECTORS);
      ENABLE_GCS_STORAGE_CONNECTORS = setBoolVar(VARIABLE_ENABLE_GCS_STORAGE_CONNECTORS,
              ENABLE_GCS_STORAGE_CONNECTORS);
      ENABLE_BIGQUERY_STORAGE_CONNECTORS = setBoolVar(VARIABLE_ENABLE_BIGQUERY_STORAGE_CONNECTORS,
              ENABLE_BIGQUERY_STORAGE_CONNECTORS);
  
      YARN_RUNTIME = setStrVar(VARIABLE_YARN_RUNTIME, YARN_RUNTIME);
      DOCKER_MOUNTS = setStrVar(VARIABLE_DOCKER_MOUNTS, DOCKER_MOUNTS);
      DOCKER_JOB_MOUNTS_LIST = setStrVar(VARIABLE_DOCKER_JOB_MOUNTS_LIST, DOCKER_JOB_MOUNTS_LIST);
      DOCKER_JOB_MOUNT_ALLOWED = setBoolVar(VARIABLE_DOCKER_JOB_MOUNT_ALLOWED, DOCKER_JOB_MOUNT_ALLOWED);
      DOCKER_JOB_UID_STRICT = setBoolVar(VARIABLE_DOCKER_JOB_UID_STRICT, DOCKER_JOB_UID_STRICT);
      DOCKER_BASE_IMAGE_PYTHON_NAME = setStrVar(VARIABLE_DOCKER_BASE_IMAGE_PYTHON_NAME, DOCKER_BASE_IMAGE_PYTHON_NAME);
      DOCKER_BASE_IMAGE_PYTHON_VERSION = setStrVar(VARIABLE_DOCKER_BASE_IMAGE_PYTHON_VERSION,
          DOCKER_BASE_IMAGE_PYTHON_VERSION);

      // Job executions cleaner variables
      EXECUTIONS_PER_JOB_LIMIT =  setIntVar(VARIABLE_EXECUTIONS_PER_JOB_LIMIT, EXECUTIONS_PER_JOB_LIMIT);
      EXECUTIONS_CLEANER_BATCH_SIZE =  setIntVar(VARIABLE_EXECUTIONS_CLEANER_BATCH_SIZE, EXECUTIONS_CLEANER_BATCH_SIZE);
      EXECUTIONS_CLEANER_INTERVAL_MS = setIntVar(VARIABLE_EXECUTIONS_CLEANER_INTERVAL_MS,
                                                 EXECUTIONS_CLEANER_INTERVAL_MS);

      YARN_APP_UID = setLongVar(VARIABLE_YARN_APP_UID, YARN_APP_UID);
      populateProvenanceCache();
      
      CLIENT_PATH = setStrVar(VARIABLE_CLIENT_PATH, CLIENT_PATH);
      KUBE_TYPE = KubeType.fromString(setStrVar(VARIABLE_KUBE_TYPE, KUBE_TYPE.name));
      DOCKER_NAMESPACE = setStrVar(VARIABLE_DOCKER_NAMESPACE, DOCKER_NAMESPACE);
      MANAGED_DOCKER_REGISTRY = setBoolVar(VARIABLE_MANAGED_DOCKER_REGISTRY,
          MANAGED_DOCKER_REGISTRY);

      MAX_ENV_YML_BYTE_SIZE = setIntVar(VARIABLE_MAX_ENV_YML_BYTE_SIZE, MAX_ENV_YML_BYTE_SIZE);
      SPARK_EXECUTOR_MIN_MEMORY = setIntVar(VARIABLE_SPARK_EXECUTOR_MIN_MEMORY, SPARK_EXECUTOR_MIN_MEMORY);
      
      LIVY_STARTUP_TIMEOUT = setIntVar(VARIABLE_LIVY_STARTUP_TIMEOUT, LIVY_STARTUP_TIMEOUT);
  
      USER_SEARCH_ENABLED = setBoolVar(VARIABLE_USER_SEARCH, USER_SEARCH_ENABLED);
      REJECT_REMOTE_USER_NO_GROUP = setBoolVar(VARIABLE_REJECT_REMOTE_USER_NO_GROUP, REJECT_REMOTE_USER_NO_GROUP);

      //Git
      GIT_IMAGE_VERSION = setStrVar(VARIABLE_GIT_IMAGE_VERSION, GIT_IMAGE_VERSION);
      GIT_MAX_COMMAND_TIMEOUT_MINUTES = setIntVar(VARIABLE_GIT_COMMAND_TIMEOUT_MINUTES_DEFAULT,
          GIT_MAX_COMMAND_TIMEOUT_MINUTES);
      ENABLE_GIT_READ_ONLY_REPOSITORIES = setBoolVar(VARIABLE_ENABLE_GIT_READ_ONLY_REPOSITORIES,
              ENABLE_GIT_READ_ONLY_REPOSITORIES);

      DOCKER_CGROUP_ENABLED = setBoolVar(VARIABLE_DOCKER_CGROUP_ENABLED, DOCKER_CGROUP_ENABLED);
      DOCKER_CGROUP_MEMORY_LIMIT = setStrVar(VARIABLE_DOCKER_CGROUP_HARD_LIMIT_MEMORY,
          DOCKER_CGROUP_MEMORY_LIMIT);
      DOCKER_CGROUP_MEMORY_SOFT_LIMIT = setStrVar(VARIABLE_DOCKER_CGROUP_SOFT_LIMIT_MEMORY,
          DOCKER_CGROUP_MEMORY_SOFT_LIMIT);
      DOCKER_CGROUP_CPU_QUOTA = setDoubleVar(VARIABLE_DOCKER_CGROUP_CPU_QUOTA, DOCKER_CGROUP_CPU_QUOTA);
      DOCKER_CGROUP_CPU_PERIOD = setIntVar(VARIABLE_DOCKER_CGROUP_CPU_PERIOD, DOCKER_CGROUP_CPU_PERIOD);
      DOCKER_CGROUP_MONITOR_INTERVAL = setStrVar(VARIABLE_DOCKER_CGROUP_MONITOR_INTERVAL,
          DOCKER_CGROUP_MONITOR_INTERVAL);

      PROMETHEUS_PORT = setIntVar(VARIABLE_PROMETHEUS_PORT, PROMETHEUS_PORT);
  
      SKIP_NAMESPACE_CREATION = setBoolVar(VARIABLE_SKIP_NAMESPACE_CREATION,
          SKIP_NAMESPACE_CREATION);

      QUOTAS_ONLINE_ENABLED_FEATUREGROUPS = setLongVar(VARIABLE_QUOTAS_ONLINE_ENABLED_FEATUREGROUPS,
          QUOTAS_ONLINE_ENABLED_FEATUREGROUPS);
      QUOTAS_ONLINE_DISABLED_FEATUREGROUPS = setLongVar(VARIABLE_QUOTAS_ONLINE_DISABLED_FEATUREGROUPS,
          QUOTAS_ONLINE_DISABLED_FEATUREGROUPS);
      QUOTAS_TRAINING_DATASETS = setLongVar(VARIABLE_QUOTAS_TRAINING_DATASETS, QUOTAS_TRAINING_DATASETS);
      QUOTAS_RUNNING_MODEL_DEPLOYMENTS = setLongVar(VARIABLE_QUOTAS_RUNNING_MODEL_DEPLOYMENTS,
          QUOTAS_RUNNING_MODEL_DEPLOYMENTS);
      QUOTAS_TOTAL_MODEL_DEPLOYMENTS = setLongVar(VARIABLE_QUOTAS_TOTAL_MODEL_DEPLOYMENTS,
          QUOTAS_TOTAL_MODEL_DEPLOYMENTS);
      QUOTAS_MAX_PARALLEL_EXECUTIONS = setLongVar(VARIABLE_QUOTAS_MAX_PARALLEL_EXECUTIONS,
          QUOTAS_MAX_PARALLEL_EXECUTIONS);
      QUOTAS_MAX_PARALLEL_EXECUTIONS = setLongVar(VARIABLE_QUOTAS_MAX_PARALLEL_EXECUTIONS,
          QUOTAS_MAX_PARALLEL_EXECUTIONS);
      
      SQL_MAX_SELECT_IN = setIntVar(VARIABLE_SQL_MAX_SELECT_IN, SQL_MAX_SELECT_IN);

      ENABLE_JUPYTER_PYTHON_KERNEL_NON_KUBERNETES = setBoolVar(VARIABLE_ENABLE_JUPYTER_PYTHON_KERNEL_NON_KUBERNETES,
        ENABLE_JUPYTER_PYTHON_KERNEL_NON_KUBERNETES);

      MAX_LONG_RUNNING_HTTP_REQUESTS =
        setIntVar(VARIABLE_MAX_LONG_RUNNING_HTTP_REQUESTS, MAX_LONG_RUNNING_HTTP_REQUESTS);

      cached = true;
    }
  }

  private void checkCache() {
    if (!cached) {
      populateCache();
    }
  }

  public synchronized void refreshCache() {
    cached = false;
    populateCache();
    //Notify other nodes if settingUpdatedTopic is created == Hazelcast is enabled
    if (settingUpdatedTopic != null) {
      settingUpdatedTopic.publish("Settings cache invalidated.");
    }
  }

  public synchronized void updateVariable(String variableName, String variableValue, VariablesVisibility visibility) {
    updateVariableInternal(variableName, variableValue, visibility);
    refreshCache();
  }

  public synchronized void updateVariables(List<Variables> variablesToUpdate) {
    variablesToUpdate.forEach(v -> updateVariableInternal(v.getId(), v.getValue(), v.getVisibility()));
    refreshCache();
  }

  /**
   * ******************************************************************
   */
  private static final String GLASSFISH_DIR = "/srv/hops/glassfish";

  public synchronized String getGlassfishDir() {
    return GLASSFISH_DIR;
  }

  private String TWOFACTOR_AUTH = "false";
  private String TWOFACTOR_EXCLUDE = "AGENT;CLUSTER_AGENT";

  public synchronized String getTwoFactorAuth() {
    checkCache();
    return TWOFACTOR_AUTH;
  }

  public synchronized String getTwoFactorExclude() {
    checkCache();
    return TWOFACTOR_EXCLUDE;
  }

  public enum TwoFactorMode {
    MANDATORY("mandatory", "User can not disable two factor auth."),
    OPTIONAL("true", "Users can choose to disable two factor auth.");

    private final String name;
    private final String description;

    TwoFactorMode(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }

  private String HOPS_RPC_TLS = "false";

  public synchronized boolean getHopsRpcTls() {
    checkCache();
    return HOPS_RPC_TLS.toLowerCase().equals("true");
  }

  //Spark executor minimum memory
  public synchronized int getSparkExecutorMinMemory() {
    checkCache();
    return SPARK_EXECUTOR_MIN_MEMORY;
  }

  public static final String VERIFICATION_PATH = "/validate";

  /**
   * Default Directory locations
   */
  public static final String PRIVATE_DIRS = "/private_dirs/";

  public static final String TENSORBOARD_DIRS = "/tensorboard/";

  private String SPARK_DIR = "/srv/hops/spark";
  public static final String SPARK_EXAMPLES_DIR = "/examples/jars";
  
  public static final String CONVERSION_DIR = "/ipython_conversions/";

  public static final String SPARK_NUMBER_EXECUTORS_ENV
      = "spark.executor.instances";
  public static final String SPARK_DYNAMIC_ALLOC_ENV
      = "spark.dynamicAllocation.enabled";
  public static final String SPARK_DYNAMIC_ALLOC_MIN_EXECS_ENV
      = "spark.dynamicAllocation.minExecutors";
  public static final String SPARK_DYNAMIC_ALLOC_MAX_EXECS_ENV
      = "spark.dynamicAllocation.maxExecutors";
  public static final String SPARK_DYNAMIC_ALLOC_INIT_EXECS_ENV
      = "spark.dynamicAllocation.initialExecutors";
  public static final String SPARK_SHUFFLE_SERVICE
      = "spark.shuffle.service.enabled";
  public static final String SPARK_SUBMIT_DEPLOYMODE = "spark.submit.deployMode";
  public static final String SPARK_DRIVER_MEMORY_ENV = "spark.driver.memory";
  public static final String SPARK_DRIVER_CORES_ENV = "spark.driver.cores";
  public static final String SPARK_DRIVER_EXTRACLASSPATH = "spark.driver.extraClassPath";
  public static final String SPARK_EXECUTOR_MEMORY_ENV = "spark.executor.memory";
  public static final String SPARK_EXECUTOR_CORES_ENV = "spark.executor.cores";
  public static final String SPARK_EXECUTOR_GPU_AMOUNT = "spark.executor.resource.gpu.amount";
  public static final String SPARK_TASK_RESOURCE_GPU_AMOUNT = "spark.task.resource.gpu.amount";
  public static final String SPARK_EXECUTOR_RESOURCE_GPU_DISCOVERY_SCRIPT =
    "spark.executor.resource.gpu.discoveryScript";
  public static final String SPARK_EXECUTOR_EXTRACLASSPATH = "spark.executor.extraClassPath";
  public static final String SPARK_DRIVER_STAGINGDIR_ENV = "spark.yarn.stagingDir";
  public static final String SPARK_JAVA_LIBRARY_PROP = "java.library.path";
  public static final String SPARK_EXECUTOR_EXTRA_JAVA_OPTS = "spark.executor.extraJavaOptions";
  public static final String SPARK_DRIVER_EXTRA_JAVA_OPTIONS="spark.driver.extraJavaOptions";

  public static final String SPARK_YARN_DIST_PYFILES = "spark.yarn.dist.pyFiles";
  public static final String SPARK_YARN_DIST_FILES = "spark.yarn.dist.files";
  public static final String SPARK_YARN_DIST_ARCHIVES = "spark.yarn.dist.archives";
  public static final String SPARK_YARN_JARS = "spark.yarn.jars";


  //Blacklisting properties
  public static final String SPARK_BLACKLIST_ENABLED = "spark.blacklist.enabled";
  public static final String SPARK_BLACKLIST_MAX_TASK_ATTEMPTS_PER_EXECUTOR =
    "spark.blacklist.task.maxTaskAttemptsPerExecutor";
  public static final String SPARK_BLACKLIST_MAX_TASK_ATTEMPTS_PER_NODE =
    "spark.blacklist.task.maxTaskAttemptsPerNode";
  public static final String SPARK_BLACKLIST_STAGE_MAX_FAILED_TASKS_PER_EXECUTOR =
    "spark.blacklist.stage.maxFailedTasksPerExecutor";
  public static final String SPARK_BLACKLIST_STAGE_MAX_FAILED_TASKS_PER_NODE =
    "spark.blacklist.stage.maxFailedExecutorsPerNode";
  public static final String SPARK_BLACKLIST_APPLICATION_MAX_FAILED_TASKS_PER_EXECUTOR =
    "spark.blacklist.application.maxFailedTasksPerExecutor";
  public static final String SPARK_BLACKLIST_APPLICATION_MAX_FAILED_TASKS_PER_NODE =
    "spark.blacklist.application.maxFailedExecutorsPerNode";
  public static final String SPARK_BLACKLIST_KILL_BLACKLISTED_EXECUTORS =
    "spark.blacklist.killBlacklistedExecutors";
  public static final String SPARK_TASK_MAX_FAILURES = "spark.task.maxFailures";

  //PySpark properties
  public static final String SPARK_APP_NAME_ENV = "spark.app.name";

  public static final String SPARK_YARN_IS_PYTHON_ENV = "spark.yarn.isPython";

  public static final String SPARK_PYSPARK_PYTHON = "PYSPARK_PYTHON";

  public static final String SPARK_PYSPARK_PYTHON_OPTION = "spark.pyspark.python";

  //Spark log4j and metrics properties
  public static final String JOB_LOG4J_CONFIG = "log4j.configurationFile";
  public static final String JOB_LOG4J_PROPERTIES = "log4j2.properties";
  //If the value of this property changes, it must be changed in spark-chef log4j.properties as well
  public static final String LOGSTASH_JOB_INFO = "hopsworks.logstash.job.info";

  public static final String SPARK_CACHE_FILENAMES
      = "spark.yarn.cache.filenames";
  public static final String SPARK_CACHE_SIZES = "spark.yarn.cache.sizes";
  public static final String SPARK_CACHE_TIMESTAMPS
      = "spark.yarn.cache.timestamps";
  public static final String SPARK_CACHE_VISIBILITIES
      = "spark.yarn.cache.visibilities";
  public static final String SPARK_CACHE_TYPES = "spark.yarn.cache.types";
  //PYSPARK constants
  public static final String SPARK_PY_MAINCLASS
      = "org.apache.spark.deploy.PythonRunner";
  
  public static final long PYTHON_JOB_KUBE_WAITING_TIMEOUT_MS = 60000;

  public static final String SPARK_YARN_APPMASTER_ENV = "spark.yarn.appMasterEnv.";
  public static final String SPARK_EXECUTOR_ENV = "spark.executorEnv.";

  public static final String SPARK_YARN_APPMASTER_SPARK_USER = SPARK_YARN_APPMASTER_ENV + "SPARK_USER";
  public static final String SPARK_YARN_APPMASTER_YARN_MODE = SPARK_YARN_APPMASTER_ENV + "SPARK_YARN_MODE";
  public static final String SPARK_YARN_APPMASTER_YARN_STAGING_DIR = SPARK_YARN_APPMASTER_ENV 
      + "SPARK_YARN_STAGING_DIR";
  public static final String SPARK_YARN_APPMASTER_CUDA_DEVICES = SPARK_YARN_APPMASTER_ENV + "CUDA_VISIBLE_DEVICES";
  public static final String SPARK_YARN_APPMASTER_HIP_DEVICES = SPARK_YARN_APPMASTER_ENV + "HIP_VISIBLE_DEVICES";
  public static final String SPARK_YARN_APPMASTER_ENV_EXECUTOR_GPUS = SPARK_YARN_APPMASTER_ENV + "EXECUTOR_GPUS";
  public static final String SPARK_YARN_APPMASTER_LIBHDFS_OPTS = SPARK_YARN_APPMASTER_ENV + "LIBHDFS_OPTS";
  
  public static final String SPARK_YARN_APPMASTER_IS_DRIVER = SPARK_YARN_APPMASTER_ENV + "IS_HOPS_DRIVER";
  public static final String SPARK_EXECUTOR_SPARK_USER = SPARK_EXECUTOR_ENV + "SPARK_USER";
  public static final String SPARK_EXECUTOR_ENV_EXECUTOR_GPUS = SPARK_EXECUTOR_ENV + "EXECUTOR_GPUS";
  public static final String SPARK_EXECUTOR_LIBHDFS_OPTS = SPARK_EXECUTOR_ENV + "LIBHDFS_OPTS";

  //docker
  public static final String SPARK_YARN_APPMASTER_CONTAINER_RUNTIME = SPARK_YARN_APPMASTER_ENV
      + "YARN_CONTAINER_RUNTIME_TYPE";
  public static final String SPARK_YARN_APPMASTER_DOCKER_IMAGE = SPARK_YARN_APPMASTER_ENV
      + "YARN_CONTAINER_RUNTIME_DOCKER_IMAGE";
  public static final String SPARK_YARN_APPMASTER_DOCKER_MOUNTS = SPARK_YARN_APPMASTER_ENV
      + "YARN_CONTAINER_RUNTIME_DOCKER_MOUNTS";
  public static final String SPARK_EXECUTOR_CONTAINER_RUNTIME = SPARK_EXECUTOR_ENV + "YARN_CONTAINER_RUNTIME_TYPE";
  public static final String SPARK_EXECUTOR_DOCKER_IMAGE = SPARK_EXECUTOR_ENV + "YARN_CONTAINER_RUNTIME_DOCKER_IMAGE";
  public static final String SPARK_EXECUTOR_DOCKER_MOUNTS = SPARK_EXECUTOR_ENV + "YARN_CONTAINER_RUNTIME_DOCKER_MOUNTS";
  public static final String SPARK_HADOOP_FS_PERMISSIONS_UMASK = "spark.hadoop.fs.permissions.umask-mode";
  
  public static final String YARN_PROPERTIES_DYNAMIC_PROPERTIES_STRING = "dynamicPropertiesString";
  public static final String YARN_DYNAMIC_PROPERTIES_SEPARATOR = "@@"; // this has to be a regex for String.split()
  
  //nccl
  public static final String NCCL_SOCKET_NTHREADS = "NCCL_SOCKET_NTHREADS";
  public static final String NCCL_NSOCKS_PERTHREAD = "NCCL_NSOCKS_PERTHREAD";

  public synchronized String getSparkDir() {
    checkCache();
    return SPARK_DIR;
  }

  public synchronized String getSparkConfDir() {
    return getSparkDir() + "/conf";
  }

  public synchronized String getSparkLog4j2FilePath() {
    return getSparkConfDir() + "/log4j2.properties";
  }

  // "/tmp" by default
  private String STAGING_DIR = "/srv/hops/domains/domain1/staging";

  public synchronized String getStagingDir() {
    checkCache();
    return STAGING_DIR;
  }

  private final String FLINK_CONF_DIR = "conf";
  // Remember to change this in docker-images as well
  private String FLINK_DIR = "/srv/hops/flink";

  public synchronized String getFlinkDir() {
    checkCache();
    return FLINK_DIR;
  }

  public String getFlinkConfDir() {
    return getFlinkDir() + File.separator + FLINK_CONF_DIR + File.separator;
  }
  
  private final String FLINK_LIB_DIR = "lib";
  
  public String getFlinkLibDir() {
    return getFlinkDir() + File.separator + FLINK_LIB_DIR + File.separator;
  }

  private String AIRFLOW_DIR = "/srv/hops/airflow";
  public synchronized String getAirflowDir() {
    checkCache();
    return AIRFLOW_DIR;
  }

  private String HADOOP_DIR = "/srv/hops/hadoop";

  // This returns the unversioned base installation directory for hops-hadoop
  // For example, "/srv/hops/hadoop" - it does not return "/srv/hops/hadoop-2.8.2"
  public synchronized String getHadoopSymbolicLinkDir() {
    checkCache();
    return HADOOP_DIR;
  }

  private String HIVE_SUPERUSER = "hive";

  public synchronized String getHiveSuperUser() {
    checkCache();
    return HIVE_SUPERUSER;
  }

  private String ANACONDA_DEFAULT_REPO = "defaults";

  public synchronized String getCondaDefaultRepo() {
    checkCache();
    return ANACONDA_DEFAULT_REPO;
  }

  private String HIVE_WAREHOUSE = "/apps/hive/warehouse";

  public synchronized String getHiveWarehouse() {
    checkCache();
    return HIVE_WAREHOUSE;
  }

  private String HIVE_SCRATCHDIR = "/tmp/hive";

  public synchronized String getHiveScratchdir() {
    checkCache();
    return HIVE_SCRATCHDIR;
  }

  private String HIVE_SCRATCHDIR_DELAY = "7d";

  public synchronized String getHiveScratchdirDelay() {
    checkCache();
    return HIVE_SCRATCHDIR_DELAY;
  }

  private String HIVE_SCRATCHDIR_CLEANER_INTERVAL = "24h";

  public synchronized String getHiveScratchdirCleanerInterval() {
    checkCache();
    return HIVE_SCRATCHDIR_CLEANER_INTERVAL;
  }

  private long HIVE_DB_DEFAULT_QUOTA = HdfsConstants.QUOTA_DONT_SET;
  public synchronized long getHiveDbDefaultQuota() {
    checkCache();
    return HIVE_DB_DEFAULT_QUOTA;
  }

  private String CERTS_DIR = "/srv/hops/certs-dir";

  public synchronized String getCertsDir() {
    checkCache();
    return CERTS_DIR;
  }

  public synchronized String getHopsworksMasterEncPasswordFile() {
    checkCache();
    return getCertsDir() + File.separator + "encryption_master_password";
  }

  private String HOPSWORKS_INSTALL_DIR = "/srv/hops/domains/domain1";

  public synchronized String getHopsworksDomainDir() {
    checkCache();
    return HOPSWORKS_INSTALL_DIR;
  }

  private String SUDOERS_DIR = "/srv/hops/sbin";
  public synchronized String getSudoersDir() {
    checkCache();
    return SUDOERS_DIR;
  }

  private String HOPSWORKS_USER = "glassfish";

  public synchronized String getHopsworksUser() {
    checkCache();
    return HOPSWORKS_USER;
  }
  private String HDFS_SUPERUSER = "hdfs";

  public synchronized String getHdfsSuperUser() {
    checkCache();
    return HDFS_SUPERUSER;
  }
  private String SPARK_USER = "spark";

  public synchronized String getSparkUser() {
    checkCache();
    return SPARK_USER;
  }

  public String getSparkLog4JPath() {
    return "hdfs:///user/" + getSparkUser() + "/log4j2.properties";
  }

  private Integer YARN_DEFAULT_QUOTA = 60000;
  public synchronized Integer getYarnDefaultQuota() {
    checkCache();
    return YARN_DEFAULT_QUOTA;
  }

  private PaymentType DEFAULT_PAYMENT_TYPE = PaymentType.NOLIMIT;
  public synchronized PaymentType getDefaultPaymentType() {
    checkCache();
    return DEFAULT_PAYMENT_TYPE;
  }

  private long HDFS_DEFAULT_QUOTA_MBs = HdfsConstants.QUOTA_DONT_SET;
  public synchronized long getHdfsDefaultQuotaInMBs() {
    checkCache();
    return HDFS_DEFAULT_QUOTA_MBs;
  }

  // Set the DIR_ROOT (/Projects) to have DB storage policy, i.e. - small files stored on db
  private DistributedFileSystemOps.StoragePolicy HDFS_BASE_STORAGE_POLICY
    = DistributedFileSystemOps.StoragePolicy.SMALL_FILES;
  // To not fill the SSDs with Logs files that nobody access frequently
  // We set the StoragePolicy for the LOGS dir to be DEFAULT
  private DistributedFileSystemOps.StoragePolicy HDFS_LOG_STORAGE_POLICY
      = DistributedFileSystemOps.StoragePolicy.DEFAULT;

  private DistributedFileSystemOps.StoragePolicy setHdfsStoragePolicy(String policyName,
    DistributedFileSystemOps.StoragePolicy defaultPolicy) {

    Optional<Variables> policyOptional = findById(policyName);
    if (!policyOptional.isPresent()) {
      return defaultPolicy;
    }

    String existingPolicy = policyOptional.get().getValue();
    if (!Strings.isNullOrEmpty(existingPolicy)) {
      try {
        return DistributedFileSystemOps.StoragePolicy.fromPolicy(existingPolicy);
      } catch (IllegalArgumentException ex) {
        LOGGER.warning("Error - not a valid storage policy! Value was:" + existingPolicy);
        return defaultPolicy;
      }
    } else {
      return defaultPolicy;
    }
  }

  public synchronized DistributedFileSystemOps.StoragePolicy getHdfsBaseStoragePolicy() {
    checkCache();
    return HDFS_BASE_STORAGE_POLICY;
  }

  public synchronized DistributedFileSystemOps.StoragePolicy getHdfsLogStoragePolicy() {
    checkCache();
    return HDFS_LOG_STORAGE_POLICY;
  }

  private Integer MAX_NUM_PROJ_PER_USER = 5;
  public synchronized Integer getMaxNumProjPerUser() {
    checkCache();
    return MAX_NUM_PROJ_PER_USER;
  }

  private String HADOOP_VERSION = "2.8.2";

  public synchronized String getHadoopVersion() {
    checkCache();
    return HADOOP_VERSION;
  }

  //Hadoop locations
  public synchronized String getHadoopConfDir() {
    return hadoopConfDir(getHadoopSymbolicLinkDir());
  }

  private String hadoopConfDir(String hadoopDir) {
    return hadoopDir + "/" + HADOOP_CONF_RELATIVE_DIR;
  }

  public String getHadoopConfDir(String hadoopDir) {
    return hadoopConfDir(hadoopDir);
  }

  public String getYarnConfDir(String hadoopDir) {
    return hadoopConfDir(hadoopDir);
  }

  //Default configuration file names
  public static final String DEFAULT_YARN_CONFFILE_NAME = "yarn-site.xml";
  public static final String DEFAULT_HADOOP_CONFFILE_NAME = "core-site.xml";
  private static final String DEFAULT_HDFS_CONFFILE_NAME = "hdfs-site.xml";

  //Environment variable keys
  //TODO: Check if ENV_KEY_YARN_CONF_DIR should be replaced with ENV_KEY_YARN_CONF
  private static final String ENV_KEY_YARN_CONF_DIR = "hdfs";
  public static final String ENV_KEY_HADOOP_CONF_DIR = "HADOOP_CONF_DIR";
  public static final String ENV_KEY_YARN_CONF = "YARN_CONF_DIR";
  public static final String ENV_KEY_SPARK_CONF_DIR = "SPARK_CONF_DIR";
  //YARN constants
  public static final int YARN_DEFAULT_APP_MASTER_MEMORY = 2048;
  public static final String HADOOP_COMMON_HOME_KEY = "HADOOP_COMMON_HOME";
  public static final String HADOOP_HOME_KEY = "HADOOP_HOME";
  public static final String HADOOP_HDFS_HOME_KEY = "HADOOP_HDFS_HOME";
  public static final String HADOOP_YARN_HOME_KEY = "HADOOP_YARN_HOME";
  public static final String HADOOP_CONF_DIR_KEY = "HADOOP_CONF_DIR";

  private static final String HADOOP_CONF_RELATIVE_DIR = "etc/hadoop";

  //Spark constants
  // Subdirectory where Spark libraries will be placed.
  public static final String SPARK_LOCRSC_APP_JAR = "__app__.jar";

  public static final String HOPS_EXPERIMENTS_DATASET = "Experiments";
  public static final String HOPS_MODELS_DATASET = "Models";
  public static final String HOPS_MODELS_SCHEMA = "model_schema.json";
  public static final String HOPS_MODELS_INPUT_EXAMPLE = "input_example.json";

  public static final String HOPS_TOUR_DATASET = "TestJob";
  public static final String HOPS_DL_TOUR_DATASET = "TourData";
  public static final String HOPS_TOUR_DATASET_JUPYTER = "Jupyter";
  // Distribution-defined classpath to add to processes
  public static final String SPARK_AM_MAIN
      = "org.apache.spark.deploy.yarn.ApplicationMaster";
  public static final String SPARK_CONFIG_FILE = "conf/spark-defaults.conf";
  public static final String SPARK_BLACKLISTED_PROPS
      = "conf/spark-blacklisted-properties.txt";
  public static final String SPARK_HADOOP_FS_PERMISSIONS_UMASK_DEFAULT = "0007";
  // Spark executor min memory
  private int SPARK_EXECUTOR_MIN_MEMORY = 1024;

  //Flink constants
  public static final String HOPS_DEEP_LEARNING_TOUR_DATA = "tensorflow_demo/data";
  public static final String HOPS_DEEP_LEARNING_TOUR_NOTEBOOKS = "tensorflow_demo/notebooks";
  public static final String FLINK_AM_MAIN = "org.apache.flink.yarn.ApplicationMaster";
  public static final String FLINK_ENV_JAVA_OPTS = "env.java.opts";
  public static final String FLINK_STATE_CHECKPOINTS_DIR = "state.checkpoints.dir";

  //Featurestore constants
  public static final String HSFS_UTIL_MAIN_CLASS = "com.logicalclocks.utils.MainClass";

  //Serving constants
  public static final String INFERENCE_SCHEMANAME = "inferenceschema";
  public static final int INFERENCE_SCHEMAVERSION = 3;
  
  //Kafka constants
  public static final String PROJECT_COMPATIBILITY_SUBJECT = "projectcompatibility";
  
  public static final Set<String> KAFKA_SUBJECT_BLACKLIST =
    new HashSet<>(Arrays.asList(INFERENCE_SCHEMANAME, PROJECT_COMPATIBILITY_SUBJECT));

  public synchronized String getLocalFlinkJarPath() {
    return getFlinkDir() + "/flink.jar";
  }

  private static final String HADOOP_GLASSPATH_GLOB_ENV_VAR_KEY = "HADOOP_GLOB";
  private volatile String HADOOP_CLASSPATH_GLOB = null;

  public String getHadoopClasspathGlob() throws IOException {
    if (HADOOP_CLASSPATH_GLOB == null) {
      synchronized (Settings.class) {
        if (HADOOP_CLASSPATH_GLOB == null) {
          String classpathGlob = System.getenv(HADOOP_GLASSPATH_GLOB_ENV_VAR_KEY);
          if (classpathGlob == null) {
            LOGGER.log(Level.WARNING, HADOOP_GLASSPATH_GLOB_ENV_VAR_KEY + " environment variable is not set. "
                + "Launching a subprocess to discover it");
            String bin = Paths.get(getHadoopSymbolicLinkDir(), "bin", "hadoop").toString();
            ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
                .addCommand(bin)
                .addCommand("classpath")
                .addCommand("--glob")
                .build();
            ProcessResult result = osProcessExecutor.execute(processDescriptor);
            if (result.getExitCode() != 0) {
              throw new IOException("Could not get Hadoop classpath, exit code " + result.getExitCode()
                  + " Error: " + result.getStderr());
            }
            HADOOP_CLASSPATH_GLOB = result.getStdout();
          } else {
            HADOOP_CLASSPATH_GLOB = classpathGlob;
          }
        }
      }
    }
    return HADOOP_CLASSPATH_GLOB;
  }

  /**
   * Static final fields are allowed in session beans:
   * http://stackoverflow.com/questions/9141673/static-variables-restriction-in-session-beans
   */
  //Directory names in HDFS
  public static final String DIR_ROOT = "Projects";
  public static final String DIR_META_TEMPLATES = Path.SEPARATOR + "user" + Path.SEPARATOR + "metadata"
    + Path.SEPARATOR + "uploads" + Path.SEPARATOR;
  public static final String PROJECT_STAGING_DIR = "Resources";
  // Any word added to reserved words in DEFAULT_RESERVED_PROJECT_NAMES and DEFAULT_RESERVED_HIVE_NAMES should
  // also be added in the documentation in:
  // https://docs.hopsworks.ai/latest/user_guides/projects/project/create_project/#reserved-project-names
  private final static String DEFAULT_RESERVED_PROJECT_NAMES = "hops-system,hopsworks,information_schema,airflow," +
    "glassfish_timers,grafana,hops,metastore,mysql,ndbinfo,performance_schema,sqoop,sys,base,python37,python38," +
    "python39,python310,filebeat,airflow,git,onlinefs,sklearnserver";
  //Hive reserved words can be found at:
  //https://cwiki.apache.org/confluence/display/hive/LanguageManual+DDL#LanguageManualDDL-Keywords,Non-
  //reservedKeywordsandReservedKeywords
  private final static String DEFAULT_RESERVED_HIVE_NAMES = "ALL, ALTER, AND, ARRAY, AS, AUTHORIZATION, BETWEEN, " +
    "BIGINT, BINARY, BOOLEAN, BOTH, BY, CASE, CAST, CHAR, COLUMN, CONF, CREATE, CROSS, CUBE, CURRENT, CURRENT_DATE, " +
    "CURRENT_TIMESTAMP, CURSOR, DATABASE, DATE, DECIMAL, DELETE, DESCRIBE, DISTINCT, DOUBLE, DROP, ELSE, END, " +
    "EXCHANGE, EXISTS, EXTENDED, EXTERNAL, FALSE, FETCH, FLOAT, FOLLOWING, FOR, FROM, FULL, FUNCTION, GRANT, GROUP, " +
    "GROUPING, HAVING, IF, IMPORT, IN, INNER, INSERT, INT, INTERSECT, INTERVAL, INTO, IS, JOIN, LATERAL, LEFT, LESS, " +
    "LIKE, LOCAL, MACRO, MAP, MORE, NONE, NOT, NULL, OF, ON, OR, ORDER, OUT, OUTER, OVER, PARTIALSCAN, PARTITION, " +
    "PERCENT, PRECEDING, PRESERVE, PROCEDURE, RANGE, READS, REDUCE, REVOKE, RIGHT, ROLLUP, ROW, ROWS, SELECT, SET, " +
    "SMALLINT, TABLE, TABLESAMPLE, THEN, TIMESTAMP, TO, TRANSFORM, TRIGGER, TRUE, TRUNCATE, UNBOUNDED, UNION, " +
    "UNIQUEJOIN, UPDATE, USER, USING, UTC_TMESTAMP, VALUES, VARCHAR, WHEN, WHERE, WINDOW, WITH, COMMIT, ONLY, " +
    "REGEXP, RLIKE, ROLLBACK, START, CACHE, CONSTRAINT, FOREIGN, PRIMARY, REFERENCES, DAYOFWEEK, EXTRACT, FLOOR, " +
    "INTEGER, PRECISION, VIEWS, TIME, NUMERIC, SYNC";
  
  private Set<String> RESERVED_PROJECT_NAMES;
  private String RESERVED_PROJECT_NAMES_STR;
  
  public synchronized Set<String> getReservedProjectNames() {
    checkCache();
    RESERVED_PROJECT_NAMES = RESERVED_PROJECT_NAMES != null ? RESERVED_PROJECT_NAMES : new HashSet<>();
    RESERVED_PROJECT_NAMES.addAll(getReservedHiveNames());
    return RESERVED_PROJECT_NAMES;
  }
  
  public synchronized Set<String> getReservedHiveNames() {
    return setStringHashSetLowerCase(DEFAULT_RESERVED_HIVE_NAMES, ",", true);
  }
  
  public synchronized String getProjectNameReservedWords() {
    checkCache();
    return (RESERVED_PROJECT_NAMES_STR + ", " + DEFAULT_RESERVED_HIVE_NAMES).toLowerCase();
  }
  
  //Only for unit test
  public synchronized String getProjectNameReservedWordsTest() {
    return (DEFAULT_RESERVED_PROJECT_NAMES + ", " + DEFAULT_RESERVED_HIVE_NAMES).toLowerCase();
  }
  
  // OpenSearch
  OpenSearchSettings OPENSEARCH_SETTINGS;
  
  public synchronized List<String> getOpenSearchIps(){
    checkCache();
    return OPENSEARCH_SETTINGS.getOpenSearchIps();
  }
  
  public synchronized int getOpenSearchPort() {
    checkCache();
    return OPENSEARCH_SETTINGS.getOpenSearchPort();
  }
  
  public synchronized int getOpenSearchRESTPort() {
    checkCache();
    return OPENSEARCH_SETTINGS.getOpenSearchRESTPort();
  }
  
  public synchronized String getOpenSearchEndpoint() {
    checkCache();
    return OPENSEARCH_SETTINGS.getOpenSearchEndpoint();
  }

  public synchronized String getOpenSearchRESTEndpoint() {
    checkCache();
    return OPENSEARCH_SETTINGS.getOpenSearchRESTEndpoint();
  }
  
  public synchronized boolean isOpenSearchSecurityEnabled() {
    checkCache();
    return OPENSEARCH_SETTINGS.isOpenSearchSecurityEnabled();
  }
  
  public synchronized boolean isOpenSearchHTTPSEnabled() {
    checkCache();
    return OPENSEARCH_SETTINGS.isHttpsEnabled();
  }
  
  public synchronized String getOpenSearchAdminUser() {
    checkCache();
    return OPENSEARCH_SETTINGS.getAdminUser();
  }

  public synchronized String getOpenSearchServiceLogUser() {
    checkCache();
    return OPENSEARCH_SETTINGS.getServiceLogUser();
  }
  
  public synchronized String getOpenSearchAdminPassword() {
    checkCache();
    return OPENSEARCH_SETTINGS.getAdminPassword();
  }
  
  public synchronized boolean isOpenSearchJWTEnabled() {
    checkCache();
    return OPENSEARCH_SETTINGS.isOpenSearchJWTEnabled();
  }
  
  public synchronized String getOpenSearchJwtUrlParameter() {
    checkCache();
    return OPENSEARCH_SETTINGS.getOpenSearchJWTURLParameter();
  }
  
  public synchronized long getOpenSearchJwtExpMs() {
    checkCache();
    return OPENSEARCH_SETTINGS.getOpenSearchJWTExpMs();
  }
  
  public synchronized Integer getOpenSearchDefaultScrollPageSize() {
    checkCache();
    return OPENSEARCH_SETTINGS.getDefaultScrollPageSize();
  }
  
  public synchronized Integer getOpenSearchMaxScrollPageSize() {
    checkCache();
    return OPENSEARCH_SETTINGS.getMaxScrollPageSize();
  }

  private long OpenSearch_LOGS_INDEX_EXPIRATION = 7 * 24 * 60 * 60 * 1000;

  public synchronized long getOpenSearchLogsIndexExpiration() {
    checkCache();
    return OpenSearch_LOGS_INDEX_EXPIRATION;
  }

  public static final long JOB_LOGS_DISPLAY_SIZE = 1000000;

  // CertificateMaterializer service. Delay for deleting crypto material from
  // the local filesystem. The lower the value the more frequent we reach DB
  // for materialization
  // Suffix, defaults to minutes if omitted:
  // ms: milliseconds
  // s: seconds
  // m: minutes (default)
  // h: hours
  // d: days
  private String CERTIFICATE_MATERIALIZER_DELAY = "1m";

  public synchronized String getCertificateMaterializerDelay() {
    checkCache();
    return CERTIFICATE_MATERIALIZER_DELAY;
  }

  private String SERVICE_DISCOVERY_DOMAIN = "consul";
  public synchronized String getServiceDiscoveryDomain() {
    checkCache();
    return SERVICE_DISCOVERY_DOMAIN;
  }

  // Kibana
  public static final String KIBANA_INDEX_PREFIX = ".kibana";
  
  private String KIBANA_IP = "10.0.2.15";
  private static final int KIBANA_PORT = 5601;

  public synchronized String getKibanaUri() {
    checkCache();
    return (KIBANA_HTTPS_ENABELED ? "https" : "http") + "://" + KIBANA_IP +
        ":" + KIBANA_PORT;
  }
  
  public String getKibanaAppUri() {
    return "/hopsworks-api/kibana/app/discover?";
  }
  
  public String getKibanaAppUri(String jwtToken) {
    return  getKibanaAppUri() + OPENSEARCH_SETTINGS.getOpenSearchJWTURLParameter() + "=" + jwtToken + "&";
  }

  /*
   * Comma-separated list of user emails that should not be persisted in the
   * userlogins table for auditing.
   * kagent -> agent@hops.io
   */
  private String WHITELIST_USERS_LOGIN = "agent@hops.io";

  public synchronized String getWhitelistUsersLogin() {
    checkCache();
    return WHITELIST_USERS_LOGIN;
  }

  // Jupyter
  private String JUPYTER_DIR = "/srv/hops/jupyter";

  public synchronized String getJupyterDir() {
    checkCache();
    return JUPYTER_DIR;
  }

  private String JUPYTER_GROUP = "jupyter";

  public synchronized String getJupyterGroup() {
    checkCache();
    return JUPYTER_GROUP;
  }

  private String JUPYTER_ORIGIN_SCHEME = "https";

  public synchronized String getJupyterOriginScheme() {
    checkCache();
    return JUPYTER_ORIGIN_SCHEME;
  }

  private long JUPYTER_WS_PING_INTERVAL_MS = 10000L;

  public synchronized long getJupyterWSPingInterval() {
    checkCache();
    return JUPYTER_WS_PING_INTERVAL_MS;

  }

  private Integer PROMETHEUS_PORT = 9089;
  public synchronized Integer getPrometheusPort() {
    checkCache();
    return PROMETHEUS_PORT;
  }

  //Git
  private String GIT_DIR = "/srv/hops/git";

  public synchronized String getGitDir() {
    checkCache();
    return GIT_DIR;
  }

  private Integer GIT_MAX_COMMAND_TIMEOUT_MINUTES = 60;
  public synchronized long getGitJwtExpMs() {
    checkCache();
    return GIT_MAX_COMMAND_TIMEOUT_MINUTES * 60 * 1000;
  }

  private Boolean ENABLE_GIT_READ_ONLY_REPOSITORIES = false;
  public synchronized Boolean getEnableGitReadOnlyRepositories() {
    checkCache();
    return ENABLE_GIT_READ_ONLY_REPOSITORIES;
  }

  private String GIT_IMAGE_VERSION = "0.3.0";
  public synchronized String getGitImageName() {
    checkCache();
    return "git:" + GIT_IMAGE_VERSION;
  }

  private boolean DOCKER_CGROUP_ENABLED = false;
  public synchronized boolean isDockerCgroupEnabled() {
    checkCache();
    return DOCKER_CGROUP_ENABLED;
  }

  private String DOCKER_CGROUP_MEMORY_LIMIT = "6GB";
  public synchronized String getDockerCgroupMemoryLimit() {
    checkCache();
    return DOCKER_CGROUP_MEMORY_LIMIT;
  }

  private String DOCKER_CGROUP_MEMORY_SOFT_LIMIT = "2GB";
  public synchronized String getDockerCgroupSoftLimit() {
    checkCache();
    return DOCKER_CGROUP_MEMORY_SOFT_LIMIT;
  }

  private Double DOCKER_CGROUP_CPU_QUOTA = 100.0;
  public synchronized Double getDockerCgroupCpuQuota() {
    checkCache();
    return DOCKER_CGROUP_CPU_QUOTA;
  }

  private Integer DOCKER_CGROUP_CPU_PERIOD = 100000;
  public synchronized Integer getDockerCgroupCpuPeriod() {
    checkCache();
    return DOCKER_CGROUP_CPU_PERIOD;
  }

  private String DOCKER_CGROUP_MONITOR_INTERVAL = "10m";
  public synchronized String getDockerCgroupIntervalMonitor() {
    checkCache();
    return DOCKER_CGROUP_MONITOR_INTERVAL;
  }


  // Service key rotation interval
  private static final String JUPYTER_SHUTDOWN_TIMER_INTERVAL = "jupyter_shutdown_timer_interval";
  private String jupyterShutdownTimerInterval = "30m";

  public synchronized String getJupyterShutdownTimerInterval() {
    checkCache();
    return jupyterShutdownTimerInterval;
  }

  private String KAFKA_USER = "kafka";

  public synchronized String getKafkaUser() {
    checkCache();
    return KAFKA_USER;
  }

  private String KAFKA_DIR = "/srv/kafka";

  public synchronized String getKafkaDir() {
    checkCache();
    return KAFKA_DIR;
  }

  private String ANACONDA_DIR = "/srv/hops/anaconda";

  public synchronized String getAnacondaDir() {
    checkCache();
    return ANACONDA_DIR;
  }

  private String condaEnvName = "theenv";
  /**
   * Constructs the path to the project environment in Anaconda
   *
   * @return conda dir
   */
  public String getAnacondaProjectDir() {
    return getAnacondaDir() + File.separator + "envs" + File.separator + condaEnvName;
  }
  
  //TODO(Theofilos): Used by Flink. Will be removed as part of refactoring *YarnRunnerBuilders.
  public String getCurrentCondaEnvironment() {
    return condaEnvName;
  }
  
  private Boolean ANACONDA_ENABLED = true;

  public synchronized Boolean isAnacondaEnabled() {
    checkCache();
    return ANACONDA_ENABLED;
  }

  private Boolean DOWNLOAD_ALLOWED = true;
  public synchronized Boolean isDownloadAllowed() {
    checkCache();
    return DOWNLOAD_ALLOWED;
  }

  /**
   * kagent liveness monitor settings
   */
  private String KAGENT_USER = "kagent";
  public synchronized String getKagentUser() {
    checkCache();
    return KAGENT_USER;
  }

  private boolean KAGENT_LIVENESS_MONITOR_ENABLED = false;
  public synchronized boolean isKagentLivenessMonitorEnabled() {
    checkCache();
    return KAGENT_LIVENESS_MONITOR_ENABLED;
  }

  private String KAGENT_LIVENESS_THRESHOLD = "10s";
  public synchronized String getKagentLivenessThreshold() {
    checkCache();
    return KAGENT_LIVENESS_THRESHOLD;
  }

  private RESTLogLevel HOPSWORKS_REST_LOG_LEVEL = RESTLogLevel.PROD;

  public synchronized RESTLogLevel getHopsworksRESTLogLevel() {
    checkCache();
    return HOPSWORKS_REST_LOG_LEVEL;
  }

  private String FIRST_TIME_LOGIN = "0";

  public synchronized String getFirstTimeLogin() {
    checkCache();
    return FIRST_TIME_LOGIN;
  }


  private String ADMIN_EMAIL = "admin@hopsworks.ai";

  public synchronized String getAdminEmail() {
    checkCache();
    return ADMIN_EMAIL;
  }
  
  public synchronized boolean isDefaultAdminPasswordChanged() {
    Users user = userFacade.findByEmail(ADMIN_EMAIL);
    if (user != null) {
      String DEFAULT_ADMIN_PWD = "12fa520ec8f65d3a6feacfa97a705e622e1fea95b80b521ec016e43874dfed5a";
      return !DEFAULT_ADMIN_PWD.equals(user.getPassword());
    }
    return false;
  }

  private String HOPSWORKS_DEFAULT_SSL_MASTER_PASSWORD = "adminpw";

  public synchronized String getHopsworksMasterPasswordSsl() {
    checkCache();
    return HOPSWORKS_DEFAULT_SSL_MASTER_PASSWORD;
  }

  private Integer KAFKA_DEFAULT_NUM_PARTITIONS = 2;
  private Integer KAFKA_DEFAULT_NUM_REPLICAS = 1;

  public synchronized Integer getKafkaDefaultNumPartitions() {
    checkCache();
    return KAFKA_DEFAULT_NUM_PARTITIONS;
  }

  public synchronized Integer getKafkaDefaultNumReplicas() {
    checkCache();
    return KAFKA_DEFAULT_NUM_REPLICAS;
  }

  // HOPSWORKS-3158
  private String HOPSWORKS_PUBLIC_HOST = "";
  
  public String getHopsworksPublicHost() {
    checkCache();
    return HOPSWORKS_PUBLIC_HOST;
  }  

  // Hopsworks
  public static final String HOPS_USERNAME_SEPARATOR = "__";

  public static final int USERNAME_LEN = 8;
  public static final String META_NAME_FIELD = "name";
  public static final String META_USAGE_TIME = "usage_time";
  public static final String META_DESCRIPTION_FIELD = "description";
  public static final String META_INDEX = "projects";
  public static final String META_PROJECT_ID_FIELD = "project_id";
  public static final String META_DATASET_ID_FIELD = "dataset_id";
  public static final String META_DOC_TYPE_FIELD = "doc_type";
  public static final String DOC_TYPE_PROJECT = "proj";
  public static final String DOC_TYPE_DATASET = "ds";
  public static final String DOC_TYPE_INODE = "inode";
  public static final String META_ID = "_id";
  public static final String META_DATA_NESTED_FIELD = "xattr";
  public static final String META_NOTEBOOK_JUPYTER_CONFIG_XATTR_NAME = "jupyter_configuration";
  public static final String META_DATA_FIELDS = META_DATA_NESTED_FIELD + ".*";
  
  //Filename conventions
  public static final String FILENAME_DISALLOWED_CHARS = " /\\?*:|'\"<>%()&;#öäåÖÅÄàáéèâîïüÜ@${}[]+~^$`";
  public static final String SUBDIR_DISALLOWED_CHARS = "/\\?*:|'\"<>%()&;#öäåÖÅÄàáéèâîïüÜ@${}[]+~^$`";
  public static final String SHARED_FILE_SEPARATOR = "::";
  public static final String DOUBLE_UNDERSCORE = "__";

  // Authentication Constants
  // POSIX compliant usernake length
  public static final int USERNAME_LENGTH = 8;

  public static final String DEFAULT_ROLE = "HOPS_USER";

  // POSIX compliant usernake length
  public static final int ACCOUNT_VALIDATION_TRIES = 5;

  // Issuer of the QrCode
  public static final String ISSUER = "hops.io";

  // Used to indicate that a python version is unknown
  public static final String UNKNOWN_LIBRARY_VERSION = "UNKNOWN";

  public static final String PROJECT_PYTHON_DIR = PROJECT_STAGING_DIR + "/.python";
  public static final String ENVIRONMENT_FILE = "environment.yml";
  public static final String PROJECT_PYTHON_ENVIRONMENT_FILE = PROJECT_PYTHON_DIR + "/" + ENVIRONMENT_FILE;

  // when user is loged in 1 otherwise 0
  public static final int IS_ONLINE = 1;
  public static final int IS_OFFLINE = 0;

  public static final int ALLOWED_FALSE_LOGINS = 5;
  public static final int ALLOWED_AGENT_FALSE_LOGINS = 20;

  public static final String KEYSTORE_SUFFIX = "__kstore.jks";
  public static final String TRUSTSTORE_SUFFIX = "__tstore.jks";
  public static final String CERT_PASS_SUFFIX = "__cert.key";
  public static final String K_CERTIFICATE = "k_certificate";
  public static final String T_CERTIFICATE = "t_certificate";
  public static final String DOMAIN_CA_TRUSTSTORE = "t_certificate";
  //Glassfish truststore, used by hopsutil to initialize https connection to Hopsworks
  public static final String CRYPTO_MATERIAL_PASSWORD = "material_passwd";

  //Used by HopsUtil
  public static final String HOPSWORKS_PROJECTID_PROPERTY = "hopsworks.projectid";
  public static final String HOPSWORKS_PROJECTNAME_PROPERTY = "hopsworks.projectname";
  public static final String HOPSWORKS_PROJECTUSER_PROPERTY = "hopsworks.projectuser";
  public static final String HOPSWORKS_JOBNAME_PROPERTY = "hopsworks.job.name";
  public static final String HOPSWORKS_JOBTYPE_PROPERTY = "hopsworks.job.type";
  public static final String HOPSWORKS_APPID_PROPERTY = "hopsworks.job.appid";
  public static final String KAFKA_BROKERADDR_PROPERTY = "hopsworks.kafka.brokeraddress";
  public static final String SERVER_TRUSTSTORE_PROPERTY = "server.truststore";
  public static final String HOPSWORKS_REST_ENDPOINT_PROPERTY = "hopsworks.restendpoint";
  public static final String HOPSUTIL_INSECURE_PROPERTY = "hopsutil.insecure";
  public static final String HOPSWORKS_OPENSEARCH_ENDPOINT_PROPERTY = "hopsworks.opensearch.endpoint";
  public static final String HOPSWORKS_DOMAIN_CA_TRUSTSTORE_PROPERTY = "hopsworks.domain.truststore";

  private int FILE_PREVIEW_IMAGE_SIZE = 10000000;
  private int FILE_PREVIEW_TXT_SIZE = 100;
  public static final int FILE_PREVIEW_TXT_SIZE_BYTES = 1024 * 384;
  public static final String README_TEMPLATE = "*This is an auto-generated README.md"
      + " file for your Dataset!*\n"
      + "To replace it, go into your DataSet and edit the README.md file.\n"
      + "\n" + "*%s* DataSet\n" + "===\n" + "\n"
      + "## %s";

  public static final String FILE_PREVIEW_TEXT_TYPE = "text";
  public static final String FILE_PREVIEW_HTML_TYPE = "html";
  public static final String FILE_PREVIEW_IMAGE_TYPE = "image";

  //OpenSearch
  // log index pattern
  public static final String OPENSEARCH_LOGS_INDEX = "logs";
  public static final String OPENSEARCH_PYPI_LIBRARIES_INDEX_PATTERN_PREFIX = "pypi_libraries_";
  public static final String OPENSEARCH_LOGS_INDEX_PATTERN = "_" + Settings.OPENSEARCH_LOGS_INDEX + "-*";
  public static final String OPENSEARCH_SERVING_INDEX = "serving";
  public static final String OPENSEARCH_SERVICES_INDEX = ".services";

  public static final String OPENSEARCH_LOG_INDEX_REGEX = ".*_" + OPENSEARCH_LOGS_INDEX + "-\\d{4}.\\d{2}.\\d{2}";
  public static final String OPENSEARCH_SERVING_INDEX_REGEX =
    ".*_" + OPENSEARCH_SERVING_INDEX + "-\\d{4}.\\d{2}.\\d{2}";
  public static final String OPENSEARCH_SERVICES_INDEX_REGEX = OPENSEARCH_SERVICES_INDEX + "-\\d{4}.\\d{2}.\\d{2}";
  public static final String OPENSEARCH_PYPI_LIBRARIES_INDEX_REGEX =
    OPENSEARCH_PYPI_LIBRARIES_INDEX_PATTERN_PREFIX + "*";

  //Other OpenSearch indexes
  public static final String OPENSEARCH_INDEX_APP_PROVENANCE = "app_provenance";

  //OpenSearch aliases
  public static final String OPENSEARCH_PYPI_LIBRARIES_ALIAS = "pypi_libraries";
  
  public String getHopsworksTmpCertDir() {
    return Paths.get(getCertsDir(), "transient").toString();
  }

  public String getHdfsTmpCertDir() {
    return "/user/" + getHdfsSuperUser() + "/" + "kafkacerts";
  }

  //Dataset request subject
  public static final String MESSAGE_DS_REQ_SUBJECT = "Dataset access request.";

  // QUOTA
  public static final float DEFAULT_YARN_MULTIPLICATOR = 1.0f;

  /**
   * Returns the maximum image size in bytes that can be previewed in the browser.
   *
   * @return file size
   */
  public synchronized int getFilePreviewImageSize() {
    checkCache();
    return FILE_PREVIEW_IMAGE_SIZE;
  }

  /**
   * Returns the maximum number of lines of the file that can be previewed in the browser.
   *
   * @return file size
   */
  public synchronized int getFilePreviewTxtSize() {
    checkCache();
    return FILE_PREVIEW_TXT_SIZE;
  }

  //Project creation: default datasets
  public static enum BaseDataset {

    LOGS("Logs",
        "Contains the logs for jobs that have been run through the Hopsworks platform."),
    RESOURCES("Resources",
        "Contains resources used by jobs, for example, jar files.");
    private final String name;
    private final String description;

    private BaseDataset(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }

  public static enum ServiceDataset {
    JUPYTER("Jupyter", "Contains Jupyter notebooks."),
    SERVING("Models", "Contains models to be used for serving."),
    EXPERIMENTS("Experiments", "Contains experiments from using the hops python api"),
    TRAININGDATASETS("Training_Datasets", "Contains curated training datasets created from the feature store"),
    STATISTICS("Statistics", "Contains the statistics for feature groups and training datasets"),
    DATAVALIDATION("DataValidation", "Contains rules and results for Features validation"),
    INGESTION("Ingestion", "Temporary dataset to store feature data ready for ingestion");

    private final String name;
    private final String description;

    private ServiceDataset(String name, String description) {
      this.name = name;
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }

  public static final String JUPYTER_PIDS = "/tmp/jupyterNotebookServer.pids";
  private String RESOURCE_DIRS = ".sparkStaging;spark-warehouse";

  public synchronized String getResourceDirs() {
    checkCache();
    return RESOURCE_DIRS;
  }

  public Settings() {
  }
  
  /**
   * Get the variable value with the given name.
   *
   * @param id
   * @return The user with given email, or null if no such user exists.
   */
  public Optional<Variables> findById(String id) {
    try {
      return Optional.of(em.createNamedQuery("Variables.findById", Variables.class)
          .setParameter("id", id)
          .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }

  /**
   * Update a variable in the database.
   *
   * @param variableName name
   * @param variableValue value
   */
  private void updateVariableInternal(String variableName, String variableValue, VariablesVisibility visibility) {
    Variables var = findById(variableName)
        .orElseThrow(() -> new NoResultException("Variable <" + variableName + "> does not exist in the database"));

    if (!var.getValue().equals(variableValue) || !var.getVisibility().equals(visibility)) {
      var.setValue(variableValue);
      var.setVisibility(visibility);
      em.persist(var);
    }
  }

  public void detach(Variables variable) {
    em.detach(variable);
  }

  Configuration conf;

  public Configuration getConfiguration() throws IllegalStateException {
    if (conf == null) {
      String hadoopDir = getHadoopSymbolicLinkDir();
      //Get the path to the Yarn configuration file from environment variables
      String yarnConfDir = System.getenv(Settings.ENV_KEY_YARN_CONF_DIR);
      //If not found in environment variables: warn and use default,
      if (yarnConfDir == null) {
        yarnConfDir = getYarnConfDir(hadoopDir);
      }

      Path confPath = new Path(yarnConfDir);
      File confFile = new File(confPath + File.separator
          + Settings.DEFAULT_YARN_CONFFILE_NAME);
      if (!confFile.exists()) {
        throw new IllegalStateException("No Yarn conf file");
      }

      //Also add the hadoop config
      String hadoopConfDir = System.getenv(Settings.ENV_KEY_HADOOP_CONF_DIR);
      //If not found in environment variables: warn and use default
      if (hadoopConfDir == null) {
        hadoopConfDir = hadoopDir + "/" + Settings.HADOOP_CONF_RELATIVE_DIR;
      }
      confPath = new Path(hadoopConfDir);
      File hadoopConf = new File(confPath + "/"
          + Settings.DEFAULT_HADOOP_CONFFILE_NAME);
      if (!hadoopConf.exists()) {
        throw new IllegalStateException("No Hadoop conf file");
      }

      File hdfsConf = new File(confPath + "/" + Settings.DEFAULT_HDFS_CONFFILE_NAME);
      if (!hdfsConf.exists()) {
        throw new IllegalStateException("No HDFS conf file");
      }

      //Set the Configuration object for the returned YarnClient
      conf = new Configuration();
      conf.addResource(new Path(confFile.getAbsolutePath()));
      conf.addResource(new Path(hadoopConf.getAbsolutePath()));
      conf.addResource(new Path(hdfsConf.getAbsolutePath()));
      
      addPathToConfig(conf, confFile);
      addPathToConfig(conf, hadoopConf);
      setDefaultConfValues(conf);
    }
    return conf;
  }

  private void addPathToConfig(Configuration conf, File path) {
    // chain-in a new classloader
    URL fileUrl = null;
    try {
      fileUrl = path.toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException("Erroneous config file path", e);
    }
    URL[] urls = {fileUrl};
    ClassLoader cl = new URLClassLoader(urls, conf.getClassLoader());
    conf.setClassLoader(cl);
  }

  private void setDefaultConfValues(Configuration conf) {
    if (conf.get("fs.hdfs.impl", null) == null) {
      conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
    }
    if (conf.get("fs.file.impl", null) == null) {
      conf.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
    }
  }

  private int KAFKA_MAX_NUM_TOPICS = 10;

  public synchronized int getKafkaMaxNumTopics() {
    checkCache();
    return KAFKA_MAX_NUM_TOPICS;
  }

  private int MAX_STATUS_POLL_RETRY = 5;

  public synchronized int getMaxStatusPollRetry() {
    checkCache();
    return MAX_STATUS_POLL_RETRY;
  }

  /**
   * Returns aggregated log dir path for an application with the the given appId.
   *
   * @param hdfsUser user
   * @param appId appId
   * @return path
   */
  public String getAggregatedLogPath(String hdfsUser, String appId) {
    boolean logPathsAreAggregated = conf.getBoolean(
        YarnConfiguration.LOG_AGGREGATION_ENABLED,
        YarnConfiguration.DEFAULT_LOG_AGGREGATION_ENABLED);
    String aggregatedLogPath = null;
    if (logPathsAreAggregated) {
      String[] nmRemoteLogDirs = conf.getStrings(
          YarnConfiguration.NM_REMOTE_APP_LOG_DIR,
          YarnConfiguration.DEFAULT_NM_REMOTE_APP_LOG_DIR);

      String[] nmRemoteLogDirSuffix = conf.getStrings(
          YarnConfiguration.NM_REMOTE_APP_LOG_DIR_SUFFIX,
          YarnConfiguration.DEFAULT_NM_REMOTE_APP_LOG_DIR_SUFFIX);
      aggregatedLogPath = nmRemoteLogDirs[0] + File.separator + hdfsUser
          + File.separator + nmRemoteLogDirSuffix[0] + File.separator
          + appId;
    }
    return aggregatedLogPath;
  }

  private String PYPI_REST_ENDPOINT = "https://pypi.org/pypi/{package}/json";

  public synchronized String getPyPiRESTEndpoint() {
    checkCache();
    return PYPI_REST_ENDPOINT;
  }

  private String PYPI_INDEXER_TIMER_INTERVAL = "1d";

  public synchronized String getPyPiIndexerTimerInterval() {
    checkCache();
    return PYPI_INDEXER_TIMER_INTERVAL;
  }

  private String PYPI_SIMPLE_ENDPOINT = "https://pypi.org/simple/";

  public synchronized String getPyPiSimpleEndpoint() {
    checkCache();
    return PYPI_SIMPLE_ENDPOINT;
  }

  private boolean PYPI_INDEXER_TIMER_ENABLED = true;

  public synchronized boolean isPyPiIndexerTimerEnabled() {
    checkCache();
    return PYPI_INDEXER_TIMER_ENABLED;
  }

  private String PYTHON_LIBRARY_UPDATES_MONITOR_INTERVAL = "1d";

  public synchronized String getPythonLibraryUpdatesMonitorInterval() {
    checkCache();
    return PYTHON_LIBRARY_UPDATES_MONITOR_INTERVAL;
  }

  private String HOPS_EXAMPLES_VERSION = "0.3.0";

  public synchronized String getHopsExamplesSparkFilename() {
    checkCache();
    return "hops-examples-spark-" + HOPS_EXAMPLES_VERSION + ".jar";
  }


  //Dela START
  private static final String VARIABLE_HOPSWORKS_PUBLIC_HOST = "hopsworks_public_host";

  private Boolean DELA_ENABLED = false; // set to false if not found in variables table

  public static final String README_FILE = "README.md";
  
  public synchronized Boolean isDelaEnabled() {
    checkCache();
    return DELA_ENABLED;
  }
  
  private void populateServiceJWTCache() {
    SERVICE_MASTER_JWT = setStrVar(VARIABLE_SERVICE_MASTER_JWT, SERVICE_MASTER_JWT);
    RENEW_TOKENS = new String[NUM_OF_SERVICE_RENEW_TOKENS];
    for (int i = 0; i < NUM_OF_SERVICE_RENEW_TOKENS; i++) {
      String variableKey = String.format(SERVICE_RENEW_TOKEN_VARIABLE_TEMPLATE, i);
      String token = setStrVar(variableKey, "");
      RENEW_TOKENS[i] = token;
    }
  }
  
  //************************************************ZOOKEEPER********************************************************
  public static final int ZOOKEEPER_SESSION_TIMEOUT_MS = 30 * 1000;//30 seconds
  //Zookeeper END

  //************************************************KAFKA********************************************************

  public static final String KAFKA_ACL_WILDCARD = "*";
  public static final String KAFKA_ACL_OPERATION_TYPE_READ = "read";
  public static final String KAFKA_ACL_OPERATION_TYPE_DETAILS = "details";

  //Kafka END

  //-------------------------Remote auth [OAuth2, KRB, LDAP]----------------------------
  private static final String VARIABLE_KRB_AUTH = "kerberos_auth";
  private static final String VARIABLE_LDAP_AUTH = "ldap_auth";
  private static final String VARIABLE_LDAP_GROUP_MAPPING = "ldap_group_mapping";
  private static final String VARIABLE_LDAP_USER_ID = "ldap_user_id";
  private static final String VARIABLE_LDAP_USER_GIVEN_NAME = "ldap_user_givenName";
  private static final String VARIABLE_LDAP_USER_SURNAME = "ldap_user_surname";
  private static final String VARIABLE_LDAP_USER_EMAIL = "ldap_user_email";
  private static final String VARIABLE_LDAP_USER_SEARCH_FILTER = "ldap_user_search_filter";
  private static final String VARIABLE_LDAP_GROUP_SEARCH_FILTER = "ldap_group_search_filter";
  private static final String VARIABLE_LDAP_KRB_USER_SEARCH_FILTER = "ldap_krb_search_filter";
  private static final String VARIABLE_LDAP_ATTR_BINARY = "ldap_attr_binary";
  private static final String VARIABLE_LDAP_GROUP_TARGET = "ldap_group_target";
  private static final String VARIABLE_LDAP_DYNAMIC_GROUP_TARGET = "ldap_dyn_group_target";
  private static final String VARIABLE_LDAP_USERDN = "ldap_user_dn";
  private static final String VARIABLE_LDAP_GROUPDN = "ldap_group_dn";
  private static final String VARIABLE_LDAP_ACCOUNT_STATUS = "ldap_account_status";
  private static final String VARIABLE_LDAP_GROUPS_SEARCH_FILTER = "ldap_groups_search_filter";
  private static final String VARIABLE_LDAP_GROUP_MEMBERS_SEARCH_FILTER = "ldap_group_members_filter";
  private static final String VARIABLE_LDAP_GROUPS_TARGET = "ldap_groups_target";
  private static final String VARIABLE_OAUTH_ENABLED = "oauth_enabled";
  private static final String VARIABLE_OAUTH_REDIRECT_URI = "oauth_redirect_uri";
  private static final String VARIABLE_OAUTH_LOGOUT_REDIRECT_URI = "oauth_logout_redirect_uri";
  private static final String VARIABLE_OAUTH_ACCOUNT_STATUS = "oauth_account_status";
  private static final String VARIABLE_OAUTH_GROUP_MAPPING = "oauth_group_mapping";
  
  private static final String VARIABLE_REMOTE_AUTH_NEED_CONSENT = "remote_auth_need_consent";
  
  private static final String VARIABLE_DISABLE_PASSWORD_LOGIN = "disable_password_login";
  private static final String VARIABLE_DISABLE_REGISTRATION = "disable_registration";
  private static final String VARIABLE_DISABLE_REGISTRATION_UI = "disable_registration_ui";
  private static final String VARIABLE_LDAP_GROUP_MAPPING_SYNC_INTERVAL = "ldap_group_mapping_sync_interval";
  
  private static final String VARIABLE_VALIDATE_REMOTE_USER_EMAIL_VERIFIED = "validate_email_verified";

  private static final String VARIABLE_MANAGED_CLOUD_REDIRECT_URI = "managed_cloud_redirect_uri";
  private static final String VARIABLE_MANAGED_CLOUD_PROVIDER_NAME = "managed_cloud_provider_name";
  
  private String KRB_AUTH = "false";
  private String LDAP_AUTH = "false";
  private boolean IS_KRB_ENABLED = false;
  private boolean IS_LDAP_ENABLED = false;
  private String LDAP_GROUP_MAPPING = "";
  private String LDAP_USER_ID = "uid"; //login name
  private String LDAP_USER_GIVEN_NAME = "givenName";
  private String LDAP_USER_SURNAME = "sn";
  private String LDAP_USER_EMAIL = "mail";
  private String LDAP_USER_SEARCH_FILTER = "uid=%s";
  private String LDAP_GROUP_SEARCH_FILTER = "member=%d";
  private String LDAP_KRB_USER_SEARCH_FILTER = "krbPrincipalName=%s";
  private String LDAP_ATTR_BINARY = "java.naming.ldap.attributes.binary";
  private String LDAP_GROUP_TARGET = "cn";
  private String LDAP_DYNAMIC_GROUP_TARGET = "memberOf";
  private String LDAP_USER_DN_DEFAULT = "";
  private String LDAP_GROUP_DN_DEFAULT = "";
  private String LDAP_USER_DN = LDAP_USER_DN_DEFAULT;
  private String LDAP_GROUP_DN = LDAP_GROUP_DN_DEFAULT;
  private String LDAP_GROUPS_TARGET = "distinguishedName";
  private String LDAP_GROUPS_SEARCH_FILTER = "(&(objectCategory=group)(cn=%c))";
  private String LDAP_GROUP_MEMBERS_SEARCH_FILTER = "(&(objectCategory=user)(memberOf=%d))";
  private int LDAP_ACCOUNT_STATUS = 1;
  private String OAUTH_ENABLED = "false";
  private boolean IS_OAUTH_ENABLED = false;
  private String OAUTH_GROUP_MAPPING = "";
  private String OAUTH_REDIRECT_URI_PATH = "hopsworks/callback";
  private String OAUTH_LOGOUT_REDIRECT_URI_PATH = "hopsworks/";
  private String OAUTH_REDIRECT_URI = OAUTH_REDIRECT_URI_PATH;
  private String OAUTH_LOGOUT_REDIRECT_URI = OAUTH_LOGOUT_REDIRECT_URI_PATH;
  private int OAUTH_ACCOUNT_STATUS = 1;
  private long LDAP_GROUP_MAPPING_SYNC_INTERVAL = 0;
  
  private boolean REMOTE_AUTH_NEED_CONSENT = true;
  
  private boolean DISABLE_PASSWORD_LOGIN = false;
  private boolean DISABLE_REGISTRATION = false;
  private boolean VALIDATE_REMOTE_USER_EMAIL_VERIFIED = false;

  private String MANAGED_CLOUD_REDIRECT_URI = "";
  private String MANAGED_CLOUD_PROVIDER_NAME = "hopsworks.ai";
  
  private void populateLDAPCache() {
    KRB_AUTH = setVar(VARIABLE_KRB_AUTH, KRB_AUTH);
    LDAP_AUTH = setVar(VARIABLE_LDAP_AUTH, LDAP_AUTH);
    LDAP_GROUP_MAPPING = setVar(VARIABLE_LDAP_GROUP_MAPPING, LDAP_GROUP_MAPPING);
    LDAP_USER_ID = setVar(VARIABLE_LDAP_USER_ID, LDAP_USER_ID);
    LDAP_USER_GIVEN_NAME = setVar(VARIABLE_LDAP_USER_GIVEN_NAME, LDAP_USER_GIVEN_NAME);
    LDAP_USER_SURNAME = setVar(VARIABLE_LDAP_USER_SURNAME, LDAP_USER_SURNAME);
    LDAP_USER_EMAIL = setVar(VARIABLE_LDAP_USER_EMAIL, LDAP_USER_EMAIL);
    LDAP_ACCOUNT_STATUS = setIntVar(VARIABLE_LDAP_ACCOUNT_STATUS, LDAP_ACCOUNT_STATUS);
    LDAP_USER_SEARCH_FILTER = setVar(VARIABLE_LDAP_USER_SEARCH_FILTER, LDAP_USER_SEARCH_FILTER);
    LDAP_GROUP_SEARCH_FILTER = setVar(VARIABLE_LDAP_GROUP_SEARCH_FILTER, LDAP_GROUP_SEARCH_FILTER);
    LDAP_KRB_USER_SEARCH_FILTER = setVar(VARIABLE_LDAP_KRB_USER_SEARCH_FILTER, LDAP_KRB_USER_SEARCH_FILTER);
    LDAP_ATTR_BINARY = setVar(VARIABLE_LDAP_ATTR_BINARY, LDAP_ATTR_BINARY);
    LDAP_GROUP_TARGET = setVar(VARIABLE_LDAP_GROUP_TARGET, LDAP_GROUP_TARGET);
    LDAP_DYNAMIC_GROUP_TARGET = setVar(VARIABLE_LDAP_DYNAMIC_GROUP_TARGET, LDAP_DYNAMIC_GROUP_TARGET);
    LDAP_USER_DN = setStrVar(VARIABLE_LDAP_USERDN, LDAP_USER_DN_DEFAULT);
    LDAP_GROUP_DN = setStrVar(VARIABLE_LDAP_GROUPDN, LDAP_GROUP_DN_DEFAULT);
    LDAP_GROUPS_TARGET = setVar(VARIABLE_LDAP_GROUPS_TARGET, LDAP_GROUPS_TARGET);
    LDAP_GROUPS_SEARCH_FILTER = setStrVar(VARIABLE_LDAP_GROUPS_SEARCH_FILTER, LDAP_GROUPS_SEARCH_FILTER);
    LDAP_GROUP_MEMBERS_SEARCH_FILTER =
      setStrVar(VARIABLE_LDAP_GROUP_MEMBERS_SEARCH_FILTER, LDAP_GROUP_MEMBERS_SEARCH_FILTER);
    IS_KRB_ENABLED = setBoolVar(VARIABLE_KRB_AUTH, IS_KRB_ENABLED);
    IS_LDAP_ENABLED = setBoolVar(VARIABLE_LDAP_AUTH, IS_LDAP_ENABLED);
    OAUTH_ENABLED = setStrVar(VARIABLE_OAUTH_ENABLED, OAUTH_ENABLED);
    IS_OAUTH_ENABLED = setBoolVar(VARIABLE_OAUTH_ENABLED, IS_OAUTH_ENABLED);
    OAUTH_REDIRECT_URI = setStrVar(VARIABLE_OAUTH_REDIRECT_URI, OAUTH_REDIRECT_URI);
    OAUTH_LOGOUT_REDIRECT_URI = setStrVar(VARIABLE_OAUTH_LOGOUT_REDIRECT_URI, OAUTH_LOGOUT_REDIRECT_URI);
    OAUTH_ACCOUNT_STATUS = setIntVar(VARIABLE_OAUTH_ACCOUNT_STATUS, OAUTH_ACCOUNT_STATUS);
    OAUTH_GROUP_MAPPING = setStrVar(VARIABLE_OAUTH_GROUP_MAPPING, OAUTH_GROUP_MAPPING);
  
    REMOTE_AUTH_NEED_CONSENT = setBoolVar(VARIABLE_REMOTE_AUTH_NEED_CONSENT, REMOTE_AUTH_NEED_CONSENT);
    
    DISABLE_PASSWORD_LOGIN = setBoolVar(VARIABLE_DISABLE_PASSWORD_LOGIN, DISABLE_PASSWORD_LOGIN);
    DISABLE_REGISTRATION = setBoolVar(VARIABLE_DISABLE_REGISTRATION, DISABLE_REGISTRATION);
    DISABLE_REGISTRATION_UI = setBoolVar(VARIABLE_DISABLE_REGISTRATION_UI, DISABLE_REGISTRATION_UI);
  
    LDAP_GROUP_MAPPING_SYNC_INTERVAL = setLongVar(VARIABLE_LDAP_GROUP_MAPPING_SYNC_INTERVAL,
      LDAP_GROUP_MAPPING_SYNC_INTERVAL);
  
    VALIDATE_REMOTE_USER_EMAIL_VERIFIED =
      setBoolVar(VARIABLE_VALIDATE_REMOTE_USER_EMAIL_VERIFIED, VALIDATE_REMOTE_USER_EMAIL_VERIFIED);
    
    MANAGED_CLOUD_REDIRECT_URI = setStrVar(VARIABLE_MANAGED_CLOUD_REDIRECT_URI, MANAGED_CLOUD_REDIRECT_URI);
    MANAGED_CLOUD_PROVIDER_NAME = setStrVar(VARIABLE_MANAGED_CLOUD_PROVIDER_NAME, MANAGED_CLOUD_PROVIDER_NAME);
  }

  public synchronized String getKRBAuthStatus() {
    checkCache();
    return KRB_AUTH;
  }

  public synchronized String getLDAPAuthStatus() {
    checkCache();
    return LDAP_AUTH;
  }

  public synchronized  boolean isKrbEnabled() {
    checkCache();
    return IS_KRB_ENABLED;
  }

  public synchronized  boolean isLdapEnabled() {
    checkCache();
    return IS_LDAP_ENABLED;
  }

  public synchronized String getLdapGroupMapping() {
    checkCache();
    return LDAP_GROUP_MAPPING;
  }

  public synchronized String getLdapUserId() {
    checkCache();
    return LDAP_USER_ID;
  }

  public synchronized String getLdapUserGivenName() {
    checkCache();
    return LDAP_USER_GIVEN_NAME;
  }

  public synchronized String getLdapUserSurname() {
    checkCache();
    return LDAP_USER_SURNAME;
  }

  public synchronized String getLdapUserMail() {
    checkCache();
    return LDAP_USER_EMAIL;
  }

  public synchronized String getLdapUserSearchFilter() {
    checkCache();
    return LDAP_USER_SEARCH_FILTER;
  }

  public synchronized String getLdapGroupSearchFilter() {
    checkCache();
    return LDAP_GROUP_SEARCH_FILTER;
  }

  public synchronized String getKrbUserSearchFilter() {
    checkCache();
    return LDAP_KRB_USER_SEARCH_FILTER;
  }

  public synchronized String getLdapAttrBinary() {
    checkCache();
    return LDAP_ATTR_BINARY;
  }

  public synchronized String getLdapGroupTarget() {
    checkCache();
    return LDAP_GROUP_TARGET;
  }

  public synchronized String getLdapDynGroupTarget() {
    checkCache();
    return LDAP_DYNAMIC_GROUP_TARGET;
  }

  public synchronized String getLdapUserDN() {
    checkCache();
    return LDAP_USER_DN;
  }

  public synchronized String getLdapGroupDN() {
    checkCache();
    return LDAP_GROUP_DN;
  }

  public synchronized int getLdapAccountStatus() {
    checkCache();
    return LDAP_ACCOUNT_STATUS;
  }
  
  public synchronized String getLdapGroupsTarget() {
    checkCache();
    return LDAP_GROUPS_TARGET;
  }
  
  public synchronized String getLdapGroupsSearchFilter() {
    checkCache();
    return LDAP_GROUPS_SEARCH_FILTER;
  }
  
  public synchronized String getLdapGroupMembersFilter() {
    checkCache();
    return LDAP_GROUP_MEMBERS_SEARCH_FILTER;
  }

  public synchronized  boolean isOAuthEnabled() {
    checkCache();
    return IS_OAUTH_ENABLED;
  }

  public synchronized String getOAuthGroupMapping() {
    checkCache();
    return OAUTH_GROUP_MAPPING;
  }
  
  public void updateOAuthGroupMapping(String mapping) {
    updateVariableInternal(VARIABLE_OAUTH_GROUP_MAPPING, mapping, VariablesVisibility.ADMIN);
  }
  
  public synchronized String getOauthRedirectUri(String providerName) {
    return getOauthRedirectUri(providerName, false);
  }
  
  /*
   * when using oauth for hopsworks.ai we need to first redirect to hopsworks.ai
   * which then redirect to hopsworks.
   */
  public synchronized String getOauthRedirectUri(String providerName, boolean skipManagedCloud) {
    checkCache();
    if (MANAGED_CLOUD_REDIRECT_URI.isEmpty() || skipManagedCloud || !Objects.equals(MANAGED_CLOUD_PROVIDER_NAME,
      providerName)) {
      return OAUTH_REDIRECT_URI;
    }
    return MANAGED_CLOUD_REDIRECT_URI;
  }
  
  public synchronized String getManagedCloudRedirectUri() {
    checkCache();
    return MANAGED_CLOUD_REDIRECT_URI;
  }
  
  public synchronized String getManagedCloudProviderName() {
    checkCache();
    return MANAGED_CLOUD_PROVIDER_NAME;
  }
  
  public void updateOauthRedirectUri(String uri) {
    updateVariableInternal(VARIABLE_OAUTH_REDIRECT_URI, uri + OAUTH_REDIRECT_URI_PATH,
            VariablesVisibility.ADMIN);
  }
  
  public synchronized String getOauthLogoutRedirectUri() {
    checkCache();
    return OAUTH_LOGOUT_REDIRECT_URI;
  }
  
  public void addPathAndupdateOauthLogoutRedirectUri(String uri) {
    updateOauthLogoutRedirectUri(uri + OAUTH_LOGOUT_REDIRECT_URI_PATH);
  }
  
  public void updateOauthLogoutRedirectUri(String uri) {
    updateVariableInternal(VARIABLE_OAUTH_LOGOUT_REDIRECT_URI, uri,
        VariablesVisibility.ADMIN);
  }

  public void updateManagedCloudRedirectUri(String uri) {
    updateVariableInternal(VARIABLE_MANAGED_CLOUD_REDIRECT_URI, uri , VariablesVisibility.ADMIN);
  }
  
  public synchronized int getOAuthAccountStatus() {
    checkCache();
    return OAUTH_ACCOUNT_STATUS;
  }
  
  public void updateOAuthAccountStatus(Integer val) {
    updateVariableInternal(VARIABLE_OAUTH_ACCOUNT_STATUS, val.toString(), VariablesVisibility.ADMIN);
  }
  
  public synchronized  boolean shouldValidateEmailVerified() {
    checkCache();
    return VALIDATE_REMOTE_USER_EMAIL_VERIFIED;
  }
  
  public synchronized  boolean remoteAuthNeedConsent() {
    checkCache();
    return REMOTE_AUTH_NEED_CONSENT;
  }
  
  public void updateRemoteAuthNeedConsent(boolean needConsent) {
    updateVariableInternal(VARIABLE_REMOTE_AUTH_NEED_CONSENT, Boolean.toString(needConsent), VariablesVisibility.ADMIN);
  }

  public synchronized String getVarLdapAccountStatus() {
    return VARIABLE_LDAP_ACCOUNT_STATUS;
  }

  public synchronized String getVarLdapGroupMapping() {
    return VARIABLE_LDAP_GROUP_MAPPING;
  }

  public synchronized String getVarLdapUserId() {
    return VARIABLE_LDAP_USER_ID;
  }

  public synchronized String getVarLdapUserGivenName() {
    return VARIABLE_LDAP_USER_GIVEN_NAME;
  }

  public synchronized String getVarLdapUserSurname() {
    return VARIABLE_LDAP_USER_SURNAME;
  }

  public synchronized String getVarLdapUserMail() {
    return VARIABLE_LDAP_USER_EMAIL;
  }

  public synchronized String getVarLdapUserSearchFilter() {
    return VARIABLE_LDAP_USER_SEARCH_FILTER;
  }

  public synchronized String getVarLdapGroupSearchFilter() {
    return VARIABLE_LDAP_GROUP_SEARCH_FILTER;
  }

  public synchronized String getVarKrbUserSearchFilter() {
    return VARIABLE_LDAP_KRB_USER_SEARCH_FILTER;
  }

  public synchronized String getVarLdapAttrBinary() {
    return VARIABLE_LDAP_ATTR_BINARY;
  }

  public synchronized String getVarLdapGroupTarget() {
    return VARIABLE_LDAP_GROUP_TARGET;
  }

  public synchronized String getVarLdapDynGroupTarget() {
    return VARIABLE_LDAP_DYNAMIC_GROUP_TARGET;
  }

  public synchronized String getVarLdapUserDN() {
    return VARIABLE_LDAP_USERDN;
  }

  public synchronized String getVarLdapGroupDN() {
    return VARIABLE_LDAP_GROUPDN;
  }
  
  public synchronized  boolean isPasswordLoginDisabled() {
    checkCache();
    return DISABLE_PASSWORD_LOGIN;
  }
  
  public synchronized  boolean isRegistrationDisabled() {
    checkCache();
    return DISABLE_REGISTRATION;
  }
  
  public void updateRegistrationDisabled(boolean disable) {
    updateVariableInternal(VARIABLE_DISABLE_REGISTRATION, Boolean.toString(disable), VariablesVisibility.ADMIN);
  }

  // Special flag to disable only registration UI but not the backend
  // It is used in managed cloud when user management is MANAGED by hopsworks.ai
  // Variable value is set during instance initialization by ec2-init
  private boolean DISABLE_REGISTRATION_UI = false;
  public synchronized boolean isRegistrationUIDisabled() {
    checkCache();
    return isRegistrationDisabled() || DISABLE_REGISTRATION_UI;
  }

  public synchronized long ldapGroupMappingSyncInterval() {
    checkCache();
    return LDAP_GROUP_MAPPING_SYNC_INTERVAL;
  }
  
  
  //----------------------------END remote user------------------------------------

  // Service key rotation enabled
  private static final String SERVICE_KEY_ROTATION_ENABLED_KEY = "service_key_rotation_enabled";
  private boolean serviceKeyRotationEnabled = false;

  public synchronized boolean isServiceKeyRotationEnabled() {
    checkCache();
    return serviceKeyRotationEnabled;
  }

  // Service key rotation interval
  private static final String SERVICE_KEY_ROTATION_INTERVAL_KEY = "service_key_rotation_interval";
  private String serviceKeyRotationInterval = "3d";

  public synchronized String getServiceKeyRotationInterval() {
    checkCache();
    return serviceKeyRotationInterval;
  }

 // TensorBoard kill rotation interval in milliseconds (should be lower than the TensorBoardKillTimer)
  private static final String TENSORBOARD_MAX_LAST_ACCESSED = "tensorboard_max_last_accessed";
  private int tensorBoardMaxLastAccessed = 1140000;

  public synchronized int getTensorBoardMaxLastAccessed() {
    checkCache();
    return tensorBoardMaxLastAccessed;
  }

  // TensorBoard kill rotation interval in milliseconds
  private static final String SPARK_UI_LOGS_OFFSET = "spark_ui_logs_offset";
  private int sparkUILogsOffset = 512000;

  public synchronized int getSparkUILogsOffset() {
    checkCache();
    return sparkUILogsOffset;
  }

  public Long getConfTimeValue(String configurationTime) {
    Matcher matcher = TIME_CONF_PATTERN.matcher(configurationTime.toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid time in configuration: " + configurationTime);
    }
    return Long.parseLong(matcher.group(1));
  }

  public TimeUnit getConfTimeTimeUnit(String configurationTime) {
    Matcher matcher = TIME_CONF_PATTERN.matcher(configurationTime.toLowerCase());
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid time in configuration: " + configurationTime);
    }
    String timeUnitStr = matcher.group(2);
    if (null != timeUnitStr && !TIME_SUFFIXES.containsKey(timeUnitStr.toLowerCase())) {
      throw new IllegalArgumentException("Invalid time suffix in configuration: " + configurationTime);
    }
    return timeUnitStr == null ? TimeUnit.MINUTES : TIME_SUFFIXES.get(timeUnitStr.toLowerCase());
  }

  private Set<String> toSetFromCsv(String csv, String separator) {
    return new HashSet<>(Splitter.on(separator).trimResults().splitToList(csv));
  }

  // Libraries that should not be uninstallable
  private Set<String> IMMUTABLE_PYTHON_LIBRARY_NAMES;
  private static final String VARIABLE_IMMUTABLE_PYTHON_LIBRARY_NAMES = "preinstalled_python_lib_names";
  private static final String DEFAULT_IMMUTABLE_PYTHON_LIBRARY_NAMES = "pydoop, pyspark, jupyterlab, sparkmagic, " +
      "hdfscontents, pyjks, hops-apache-beam, pyopenssl";

  public synchronized Set<String> getImmutablePythonLibraryNames() {
    checkCache();
    return IMMUTABLE_PYTHON_LIBRARY_NAMES;
  }

  private String HOPSWORKS_VERSION;

  public synchronized String getHopsworksVersion() {
    checkCache();
    return HOPSWORKS_VERSION;
  }

  private String KUBE_KSERVE_TENSORFLOW_VERSION;

  public synchronized String getKServeTensorflowVersion() {
    checkCache();
    return KUBE_KSERVE_TENSORFLOW_VERSION;
  }

  private String TENSORFLOW_VERSION;

  public synchronized String getTensorflowVersion() {
    checkCache();
    return TENSORFLOW_VERSION;
  }

  private String OPENSEARCH_VERSION;

  public synchronized String getOpenSearchVersion() {
    checkCache();
    return OPENSEARCH_VERSION;
  }

  private String KAFKA_VERSION;

  public synchronized String getKafkaVersion() {
    checkCache();
    return KAFKA_VERSION;
  }

  private String EPIPE_VERSION;

  public synchronized String getEpipeVersion() {
    checkCache();
    return EPIPE_VERSION;
  }

  private String FLINK_VERSION;

  public synchronized String getFlinkVersion() {
    checkCache();
    return FLINK_VERSION;
  }

  private String SPARK_VERSION;

  public synchronized String getSparkVersion() {
    checkCache();
    return SPARK_VERSION;
  }

  private String TEZ_VERSION;

  public synchronized String getTezVersion() {
    checkCache();
    return TEZ_VERSION;
  }

  private String HIVE2_VERSION;

  public synchronized String getHive2Version() {
    checkCache();
    return HIVE2_VERSION;
  }

  private String LIVY_VERSION;

  public synchronized String getLivyVersion() {
    checkCache();
    return LIVY_VERSION;
  }

  private String NDB_VERSION;

  public synchronized String getNdbVersion() {
    checkCache();
    return NDB_VERSION;
  }

  private String FILEBEAT_VERSION;

  public synchronized String getFilebeatVersion() {
    checkCache();
    return FILEBEAT_VERSION;
  }

  private String KIBANA_VERSION;

  public synchronized String getKibanaVersion() {
    checkCache();
    return KIBANA_VERSION;
  }

  private String LOGSTASH_VERSION;

  public synchronized String getLogstashVersion() {
    checkCache();
    return LOGSTASH_VERSION;
  }

  private String GRAFANA_VERSION;

  public synchronized String getGrafanaVersion() {
    checkCache();
    return GRAFANA_VERSION;
  }

  private String ZOOKEEPER_VERSION;

  public synchronized String getZookeeperVersion() {
    checkCache();
    return ZOOKEEPER_VERSION;
  }

  // -------------------------------- Kubernetes ----------------------------------------------//
  private String KUBE_USER = "kubernetes";

  public synchronized String getKubeUser() {
    checkCache();
    return KUBE_USER;
  }
  
  private String KUBE_HOPSWORKS_USER = "hopsworks";
  
  public synchronized String getKubeHopsworksUser() {
    checkCache();
    return KUBE_HOPSWORKS_USER;
  }

  private String KUBEMASTER_URL = "https://192.168.68.102:6443";

  public synchronized String getKubeMasterUrl() {
    checkCache();
    return KUBEMASTER_URL;
  }

  private String KUBE_CA_CERTFILE = "/srv/hops/certs-dir/certs/ca.cert.pem";

  public synchronized String getKubeCaCertfile() {
    checkCache();
    return KUBE_CA_CERTFILE;
  }

  private String KUBE_CLIENT_KEYFILE = "/srv/hops/certs-dir/kube/hopsworks/hopsworks.key.pem";

  public synchronized String getKubeClientKeyfile() {
    checkCache();
    return KUBE_CLIENT_KEYFILE;
  }

  private String KUBE_CLIENT_CERTFILE = "/srv/hops/certs-dir/kube/hopsworks/hopsworks.cert.pem";

  public synchronized String getKubeClientCertfile() {
    checkCache();
    return KUBE_CLIENT_CERTFILE;
  }

  private String KUBE_CLIENT_KEYPASS = "adminpw";

  public synchronized String getKubeClientKeypass() {
    checkCache();
    return KUBE_CLIENT_KEYPASS;
  }

  private String KUBE_TRUSTSTORE_PATH = "/srv/hops/certs-dir/kube/hopsworks/hopsworks__tstore.jks";

  public synchronized String getKubeTruststorePath() {
    checkCache();
    return KUBE_TRUSTSTORE_PATH;
  }

  private String KUBE_TRUSTSTORE_KEY = "adminpw";

  public synchronized String getKubeTruststoreKey() {
    checkCache();
    return KUBE_TRUSTSTORE_KEY;
  }

  private String KUBE_KEYSTORE_PATH = "/srv/hops/certs-dir/kube/hopsworks/hopsworks__kstore.jks";

  public synchronized String getKubeKeystorePath() {
    checkCache();
    return KUBE_KEYSTORE_PATH;
  }

  private String KUBE_KEYSTORE_KEY = "adminpw";

  public synchronized String getKubeKeystoreKey() {
    checkCache();
    return KUBE_KEYSTORE_KEY;
  }

  private String KUBE_PULL_POLICY = "Always";
  public synchronized String getKubeImagePullPolicy() {
    checkCache();
    return KUBE_PULL_POLICY;
  }

  private Integer KUBE_API_MAX_ATTEMPTS = 12;
  public synchronized Integer getKubeAPIMaxAttempts() {
    checkCache();
    return KUBE_API_MAX_ATTEMPTS;
  }
  
  private Boolean ONLINE_FEATURESTORE = false;
  
  public synchronized Boolean isOnlineFeaturestore() {
    checkCache();
    return ONLINE_FEATURESTORE;
  }

  private String ONLINE_FEATURESTORE_TS = "";
  public synchronized String getOnlineFeatureStoreTableSpace() {
    checkCache();
    return ONLINE_FEATURESTORE_TS;
  }

  private Integer ONLINEFS_THREAD_NUMBER = 10;
  public synchronized  Integer getOnlineFsThreadNumber() {
    checkCache();
    return ONLINEFS_THREAD_NUMBER;
  }

  private Integer KUBE_DOCKER_MAX_MEMORY_ALLOCATION = 8192;
  public synchronized Integer getKubeDockerMaxMemoryAllocation() {
    checkCache();
    return KUBE_DOCKER_MAX_MEMORY_ALLOCATION;
  }

  private Double KUBE_DOCKER_MAX_CORES_ALLOCATION = 4.0;
  public synchronized Double getKubeDockerMaxCoresAllocation() {
    checkCache();
    return KUBE_DOCKER_MAX_CORES_ALLOCATION;
  }

  private Integer KUBE_DOCKER_MAX_GPUS_ALLOCATION = 1;
  public synchronized Integer getKubeDockerMaxGpusAllocation() {
    checkCache();
    return KUBE_DOCKER_MAX_GPUS_ALLOCATION;
  }
  
  private Boolean KUBE_INSTALLED = false;
  public synchronized Boolean getKubeInstalled() {
    checkCache();
    return KUBE_INSTALLED;
  }
  
  private Boolean KUBE_KSERVE_INSTALLED = false;
  public synchronized Boolean getKubeKServeInstalled() {
    checkCache();
    return KUBE_KSERVE_INSTALLED;
  }
  
  private String KUBE_SERVING_NODE_LABELS = "";
  public synchronized String getKubeServingNodeLabels() {
    checkCache();
    return KUBE_SERVING_NODE_LABELS;
  }
  
  private String KUBE_SERVING_NODE_TOLERATIONS = "";
  public synchronized String getKubeServingNodeTolerations() {
    checkCache();
    return KUBE_SERVING_NODE_TOLERATIONS;
  }
  
  private Integer KUBE_SERVING_MAX_MEMORY_ALLOCATION = -1; // no upper limit
  public synchronized Integer getKubeServingMaxMemoryAllocation() {
    checkCache();
    return KUBE_SERVING_MAX_MEMORY_ALLOCATION;
  }
  
  private Double KUBE_SERVING_MAX_CORES_ALLOCATION = -1.0;  // no upper limit
  public synchronized Double getKubeServingMaxCoresAllocation() {
    checkCache();
    return KUBE_SERVING_MAX_CORES_ALLOCATION;
  }
  
  private Integer KUBE_SERVING_MAX_GPUS_ALLOCATION = -1; // no upper limit
  public synchronized Integer getKubeServingMaxGpusAllocation() {
    checkCache();
    return KUBE_SERVING_MAX_GPUS_ALLOCATION;
  }
  
  // Maximum number of instances. Possible values >=-1 where -1 means no limit.
  private Integer KUBE_SERVING_MAX_NUM_INSTANCES = -1;
  public synchronized Integer getKubeServingMaxNumInstances() {
    checkCache();
    return KUBE_SERVING_MAX_NUM_INSTANCES;
  }
  
  // Minimum number of instances. Possible values: >=-1 where -1 means no limit and 0 enforces scale-to-zero
  // capabilities when available
  private Integer KUBE_SERVING_MIN_NUM_INSTANCES = -1;
  public synchronized Integer getKubeServingMinNumInstances() {
    checkCache();
    return KUBE_SERVING_MIN_NUM_INSTANCES;
  }
  
  private String KUBE_KNATIVE_DOMAIN_NAME = "";
  public synchronized String getKubeKnativeDomainName() {
    checkCache();
    return KUBE_KNATIVE_DOMAIN_NAME;
  }

  //comma seperated list of tainted nodes
  private String KUBE_TAINTED_NODES = "";
  public synchronized String getKubeTaintedNodes() {
    checkCache();
    return KUBE_TAINTED_NODES;
  }

  private String KUBE_TAINTED_NODES_MONITOR_INTERVAL = "30m";
  public synchronized String getKubeTaintedMonitorInterval() {
    checkCache();
    return KUBE_TAINTED_NODES_MONITOR_INTERVAL;
  }

  private Boolean HOPSWORKS_ENTERPRISE = false;
  public synchronized Boolean getHopsworksEnterprise() {
    checkCache();
    return HOPSWORKS_ENTERPRISE;
  }

  private boolean ENABLE_DATA_SCIENCE_PROFILE = false;
  public synchronized boolean getEnableDataScienceProfile() {
    checkCache();
    return ENABLE_DATA_SCIENCE_PROFILE;
  }

  private String SERVING_MONITOR_INT = "30s";

  public synchronized String getServingMonitorInt() {
    checkCache();
    return SERVING_MONITOR_INT;
  }

  private int SERVING_CONNECTION_POOL_SIZE = 40;
  public synchronized int getServingConnectionPoolSize() {
    checkCache();
    return SERVING_CONNECTION_POOL_SIZE;
  }

  private int SERVING_MAX_ROUTE_CONNECTIONS = 10;
  public synchronized int getServingMaxRouteConnections() {
    checkCache();
    return SERVING_MAX_ROUTE_CONNECTIONS;
  }

  private int TENSORBOARD_MAX_RELOAD_THREADS = 1;
  public synchronized int getTensorBoardMaxReloadThreads() {
    checkCache();
    return TENSORBOARD_MAX_RELOAD_THREADS;
  }

  private String JUPYTER_HOST = "localhost";

  public synchronized String getJupyterHost() {
    checkCache();
    return JUPYTER_HOST;
  }

  private boolean ENABLE_JUPYTER_PYTHON_KERNEL_NON_KUBERNETES = false;

  public synchronized boolean isPythonKernelEnabled() {
    checkCache();
    if(getKubeInstalled()) {
      return true;
    }
    return ENABLE_JUPYTER_PYTHON_KERNEL_NON_KUBERNETES;
  }

  //These dependencies were collected by installing jupyterlab in a new environment
  public  static final List<String> JUPYTER_DEPENDENCIES = Arrays.asList("urllib3", "chardet", "idna", "requests",
      "attrs", "zipp", "importlib-metadata", "pyrsistent", "six", "jsonschema", "prometheus-client", "pycparser",
      "cffi", "argon2-cffi", "pyzmq", "ipython-genutils", "decorator", "traitlets", "jupyter-core", "Send2Trash",
      "tornado", "pygments", "pickleshare", "wcwidth", "prompt-toolkit", "backcall", "ptyprocess", "pexpect",
      "parso", "jedi", "ipython", "python-dateutil", "jupyter-client", "ipykernel", "terminado", "MarkupSafe",
      "jinja2", "mistune", "defusedxml", "jupyterlab-pygments", "pandocfilters", "entrypoints", "pyparsing",
      "packaging", "webencodings", "bleach", "testpath", "nbformat", "nest-asyncio", "async-generator",
      "nbclient", "nbconvert", "notebook", "json5", "jupyterlab-server", "jupyterlab", "sparkmagic");

  private String JWT_SIGNATURE_ALGORITHM = "HS512";
  private String JWT_SIGNING_KEY_NAME = "apiKey";
  private String JWT_ISSUER = "hopsworks@logicalclocks.com";

  private long JWT_LIFETIME_MS = 1800000l;
  private int JWT_EXP_LEEWAY_SEC = 900;

  private long SERVICE_JWT_LIFETIME_MS = 86400000l;
  private int SERVICE_JWT_EXP_LEEWAY_SEC = 43200;

  public synchronized String getJWTSignatureAlg() {
    checkCache();
    return JWT_SIGNATURE_ALGORITHM;
  }

  public synchronized long getJWTLifetimeMs() {
    checkCache();
    return JWT_LIFETIME_MS;
  }

  public synchronized int getJWTExpLeewaySec() {
    checkCache();
    return JWT_EXP_LEEWAY_SEC;
  }

  public synchronized long getJWTLifetimeMsPlusLeeway() {
    checkCache();
    return JWT_LIFETIME_MS + (JWT_EXP_LEEWAY_SEC * 1000L);
  }

  public synchronized long getServiceJWTLifetimeMS() {
    checkCache();
    return SERVICE_JWT_LIFETIME_MS;
  }

  public synchronized int getServiceJWTExpLeewaySec() {
    checkCache();
    return SERVICE_JWT_EXP_LEEWAY_SEC;
  }

  public synchronized String getJWTSigningKeyName() {
    checkCache();
    return JWT_SIGNING_KEY_NAME;
  }

  public synchronized String getJWTIssuer() {
    checkCache();
    return JWT_ISSUER;
  }

  private String SERVICE_MASTER_JWT = "";
  public synchronized String getServiceMasterJWT() {
    checkCache();
    return SERVICE_MASTER_JWT;
  }

  public synchronized void setServiceMasterJWT(String JWT) {
    updateVariableInternal(VARIABLE_SERVICE_MASTER_JWT, JWT, VariablesVisibility.ADMIN);
    em.flush();
    SERVICE_MASTER_JWT = JWT;
  }

  private final int NUM_OF_SERVICE_RENEW_TOKENS = 5;
  private final static String SERVICE_RENEW_TOKEN_VARIABLE_TEMPLATE = "service_renew_token_%d";
  private String[] RENEW_TOKENS = new String[0];
  public synchronized String[] getServiceRenewJWTs() {
    checkCache();
    return RENEW_TOKENS;
  }

  public synchronized void setServiceRenewJWTs(String[] renewTokens) {
    for (int i = 0; i < renewTokens.length; i++) {
      String variableKey = String.format(SERVICE_RENEW_TOKEN_VARIABLE_TEMPLATE, i);
      updateVariableInternal(variableKey, renewTokens[i], VariablesVisibility.ADMIN);
    }
    RENEW_TOKENS = renewTokens;
  }

  private int CONNECTION_KEEPALIVE_TIMEOUT = 30;
  public synchronized int getConnectionKeepAliveTimeout() {
    checkCache();
    return CONNECTION_KEEPALIVE_TIMEOUT;
  }

  private int MAGGY_CLEANUP_INTERVAL = 24 * 60 * 1000;
  public synchronized int getMaggyCleanupInterval() {
    checkCache();
    return MAGGY_CLEANUP_INTERVAL;
  }

  private String HIVE_CONF_PATH = "/srv/hops/apache-hive/conf/hive-site.xml";
  public synchronized String getHiveConfPath() {
    checkCache();
    return HIVE_CONF_PATH;
  }

  private String FS_PY_JOB_UTIL_PATH = "hdfs:///user/spark/hsfs_util-2.1.0-SNAPSHOT.py";
  public synchronized String getFSPyJobUtilPath() {
    checkCache();
    return FS_PY_JOB_UTIL_PATH;
  }

  private String FS_JAVA_JOB_UTIL_PATH = "hdfs:///user/spark/hsfs-utils-2.1.0-SNAPSHOT.jar";
  public synchronized String getFSJavaJobUtilPath() {
    checkCache();
    return FS_JAVA_JOB_UTIL_PATH;
  }
  
  private String HDFS_FILE_OP_JOB_UTIL = "hdfs:///user/spark/hdfs_file_operations-0.1.0.py";
  public synchronized String getHdfsFileOpJobUtil() {
    checkCache();
    return HDFS_FILE_OP_JOB_UTIL;
  }

  private int HDFS_FILE_OP_JOB_DRIVER_MEM = 2048;
  public synchronized int getHdfsFileOpJobDriverMemory() {
    checkCache();
    return HDFS_FILE_OP_JOB_DRIVER_MEM;
  }
  private long FEATURESTORE_DB_DEFAULT_QUOTA = HdfsConstants.QUOTA_DONT_SET;
  public synchronized long getFeaturestoreDbDefaultQuota() {
    checkCache();
    return FEATURESTORE_DB_DEFAULT_QUOTA;
  }

  private String FEATURESTORE_DB_DEFAULT_STORAGE_FORMAT = "ORC";

  public synchronized String getFeaturestoreDbDefaultStorageFormat() {
    checkCache();
    return FEATURESTORE_DB_DEFAULT_STORAGE_FORMAT;
  }

  // Storage connectors

  private boolean ENABLE_REDSHIFT_STORAGE_CONNECTORS = true;
  public synchronized boolean isRedshiftStorageConnectorsEnabled() {
    checkCache();
    return ENABLE_REDSHIFT_STORAGE_CONNECTORS;
  }

  private boolean ENABLE_ADLS_STORAGE_CONNECTORS = false;
  public synchronized boolean isAdlsStorageConnectorsEnabled() {
    checkCache();
    return ENABLE_ADLS_STORAGE_CONNECTORS;
  }

  private boolean ENABLE_SNOWFLAKE_STORAGE_CONNECTORS = true;
  public synchronized boolean isSnowflakeStorageConnectorsEnabled() {
    checkCache();
    return ENABLE_SNOWFLAKE_STORAGE_CONNECTORS;
  }

  private boolean ENABLE_KAFKA_STORAGE_CONNECTORS = false;
  public synchronized boolean isKafkaStorageConnectorsEnabled() {
    checkCache();
    return ENABLE_KAFKA_STORAGE_CONNECTORS;
  }

  private boolean ENABLE_GCS_STORAGE_CONNECTORS = false;
  public synchronized boolean isGcsStorageConnectorsEnabled() {
    checkCache();
    return ENABLE_GCS_STORAGE_CONNECTORS;
  }

  private boolean ENABLE_BIGQUERY_STORAGE_CONNECTORS = false;
  public synchronized boolean isBigqueryStorageConnectorsEnabled() {
    checkCache();
    return ENABLE_BIGQUERY_STORAGE_CONNECTORS;
  }

  // End - Storage connectors

  private Boolean LOCALHOST = false;

  public synchronized Boolean isLocalHost() {
    checkCache();
    return LOCALHOST;
  }

  private String CLOUD = "";

  public synchronized String getCloudProvider() {
    checkCache();
    return CLOUD;
  }

  public Boolean isCloud() {
    return !getCloudProvider().isEmpty();
  }

  public synchronized CLOUD_TYPES getCloudType() {
    checkCache();
    if (CLOUD.isEmpty()) {
      return CLOUD_TYPES.NONE;
    }
    return CLOUD_TYPES.fromString(CLOUD);
  }
  
  public static enum CLOUD_TYPES {
    NONE,
    AWS,
    GCP,
    AZURE;
    
    public static CLOUD_TYPES fromString(String type) {
      return CLOUD_TYPES.valueOf(type.toUpperCase());
    }
  }

  public Boolean isHopsUtilInsecure() {
    return isCloud() || isLocalHost();
  }
  
  private String FEATURESTORE_JDBC_URL = "jdbc:mysql://onlinefs.mysql.service.consul:3306/";
  
  public synchronized String getFeaturestoreJdbcUrl() {
    checkCache();
    return FEATURESTORE_JDBC_URL;
  }
  
  private Boolean REQUESTS_VERIFY = false;
  
  /**
   * Whether to verify HTTP requests in hops-util-py. Accepted values are "true", "false"
   *
   */
  public synchronized Boolean getRequestsVerify() {
    checkCache();
    return REQUESTS_VERIFY;
  }
  
  private  Boolean KIBANA_HTTPS_ENABELED = false;
  public synchronized Boolean isKibanaHTTPSEnabled() {
    checkCache();
    return KIBANA_HTTPS_ENABELED;
  }
  
  private  Boolean KIBANA_MULTI_TENANCY_ENABELED = false;
  public synchronized Boolean isKibanaMultiTenancyEnabled() {
    checkCache();
    return KIBANA_MULTI_TENANCY_ENABELED;
  }
  
  public static final int OPENSEARCH_KIBANA_NO_CONNECTIONS = 5;

  //-------------------------------- PROVENANCE ----------------------------------------------//
  private static final String VARIABLE_PROVENANCE_TYPE = "provenance_type"; //disabled/meta/min/full
  private static final String VARIABLE_PROVENANCE_GRAPH_MAX_SIZE = "provenance_graph_max_size";
  private static final String VARIABLE_PROVENANCE_CLEANUP_SIZE = "provenance_cleanup_size";
  private static final String VARIABLE_PROVENANCE_CLEANER_PERIOD = "provenance_cleaner_period";
  
  public static final String PROV_FILE_INDEX_SUFFIX = "__file_prov";
  private Provenance.Type PROVENANCE_TYPE = Provenance.Type.MIN;
  private String PROVENANCE_TYPE_S = PROVENANCE_TYPE.name();
  private Integer PROVENANCE_CLEANUP_SIZE = 5;
  private Integer PROVENANCE_GRAPH_MAX_SIZE = 50;
  private Long PROVENANCE_CLEANER_PERIOD = 3600L; //1h in s
  public static final Integer PROVENANCE_OPENSEARCH_PAGE_DEFAULT_SIZE = 1000;
  
  public String getProvFileIndex(Long projectIId) {
    return projectIId.toString() + Settings.PROV_FILE_INDEX_SUFFIX;
  }
  
  private void populateProvenanceCache() {
    PROVENANCE_TYPE_S = setStrVar(VARIABLE_PROVENANCE_TYPE, PROVENANCE_TYPE_S);
    try {
      PROVENANCE_TYPE = ProvTypeDTO.provTypeFromString(PROVENANCE_TYPE_S);
    } catch(ProvenanceException e) {
      LOGGER.log(Level.WARNING, "unknown prov type:" + PROVENANCE_TYPE_S + ", using default");
      PROVENANCE_TYPE = Provenance.Type.MIN;
      PROVENANCE_TYPE_S = PROVENANCE_TYPE.name();
    }
    PROVENANCE_GRAPH_MAX_SIZE = setIntVar(VARIABLE_PROVENANCE_GRAPH_MAX_SIZE, PROVENANCE_GRAPH_MAX_SIZE);
    PROVENANCE_CLEANUP_SIZE = setIntVar(VARIABLE_PROVENANCE_CLEANUP_SIZE, PROVENANCE_CLEANUP_SIZE);
    PROVENANCE_CLEANER_PERIOD = setLongVar(VARIABLE_PROVENANCE_CLEANER_PERIOD, PROVENANCE_CLEANER_PERIOD);
  }
  
  public synchronized Provenance.Type getProvType() {
    checkCache();
    return PROVENANCE_TYPE;
  }

  public synchronized Integer getProvenanceGraphMaxSize() {
    checkCache();
    return PROVENANCE_GRAPH_MAX_SIZE;
  }

  public synchronized Integer getProvCleanupSize() {
    checkCache();
    return PROVENANCE_CLEANUP_SIZE;
  }

  public synchronized Long getProvCleanerPeriod() {
    checkCache();
    return PROVENANCE_CLEANER_PERIOD;
  }

  //------------------------------ END PROVENANCE --------------------------------------------//
  
  private String CLIENT_PATH = "/srv/hops/client.tar.gz";
  public synchronized String getClientPath() {
    checkCache();
    return CLIENT_PATH;
  }
  
  // CLOUD
  private String CLOUD_EVENTS_ENDPOINT = "";
  
  public synchronized String getCloudEventsEndPoint() {
    checkCache();
    return CLOUD_EVENTS_ENDPOINT;
  }
  
  private String CLOUD_EVENTS_ENDPOINT_API_KEY = "";
  
  public synchronized String getCloudEventsEndPointAPIKey() {
    checkCache();
    return CLOUD_EVENTS_ENDPOINT_API_KEY;
  }

  private int FG_PREVIEW_LIMIT = 100;
  public synchronized int getFGPreviewLimit() {
    checkCache();
    return FG_PREVIEW_LIMIT;
  }

  public static final String FEATURESTORE_INDEX = "featurestore";
  public static final String FEATURESTORE_PROJECT_ID_FIELD = "project_id";

  //-----------------------------YARN DOCKER-------------------------------------------------//
  private static String YARN_RUNTIME = "docker";
  
  public synchronized String getYarnRuntime(){
    checkCache();
    return YARN_RUNTIME;
  }

  //----------------------------YARN NODEMANAGER--------------------------------------------//
  private boolean checkNodemanagersStatus = false;
  public synchronized boolean isCheckingForNodemanagerStatusEnabled() {
    checkCache();
    return checkNodemanagersStatus;
  }

  private static String DOCKER_MOUNTS = 
      "/srv/hops/hadoop/etc/hadoop,/srv/hops/spark,/srv/hops/flink";
  
  public synchronized String getDockerMounts() {
    checkCache();
    String result = "";
    for(String mountPoint: DOCKER_MOUNTS.split(",")){
      result += mountPoint + ":" + mountPoint + ":ro,";
    }
    return result.substring(0, result.length() - 1);
  }

  private String DOCKER_BASE_IMAGE_PYTHON_NAME = "python38";

  public synchronized String getBaseDockerImagePythonName() {
    checkCache();
    if(isManagedDockerRegistry()){
      return DOCKER_BASE_NON_PYTHON_IMAGE + ":" + DOCKER_BASE_IMAGE_PYTHON_NAME +
          "_" + HOPSWORKS_VERSION;
    }else{
      return DOCKER_BASE_IMAGE_PYTHON_NAME + ":" + HOPSWORKS_VERSION;
    }
  }

  private String DOCKER_BASE_IMAGE_PYTHON_VERSION = "3.8";

  public synchronized String getDockerBaseImagePythonVersion() {
    checkCache();
    return DOCKER_BASE_IMAGE_PYTHON_VERSION;
  }

  private final static String DOCKER_BASE_NON_PYTHON_IMAGE = "base";
  public synchronized String getBaseNonPythonDockerImage() {
    return DOCKER_BASE_NON_PYTHON_IMAGE + ":" + HOPSWORKS_VERSION;
  }

  private long YARN_APP_UID = 1235L;
  public long getYarnAppUID() {
    checkCache();
    return YARN_APP_UID;
  }
  //-----------------------------END YARN DOCKER-------------------------------------------------//
  
  private KubeType KUBE_TYPE = KubeType.Local;
  public synchronized KubeType getKubeType() {
    checkCache();
    return KUBE_TYPE;
  }
  
  private String DOCKER_NAMESPACE = "";
  public synchronized String getDockerNamespace(){
    checkCache();
    return DOCKER_NAMESPACE;
  }
  
  private Boolean MANAGED_DOCKER_REGISTRY = false;
  public synchronized Boolean isManagedDockerRegistry(){
    checkCache();
    return MANAGED_DOCKER_REGISTRY && isCloud();
  }

  public synchronized String getBaseNonPythonDockerImageWithNoTag(){
    checkCache();
    return DOCKER_BASE_NON_PYTHON_IMAGE;
  }

  private String DOCKER_JOB_MOUNTS_LIST;
  public synchronized List<String> getDockerMountsList(){
    checkCache();
    return Arrays.asList(DOCKER_JOB_MOUNTS_LIST.split(","));
  }

  private Boolean DOCKER_JOB_MOUNT_ALLOWED = true;
  public synchronized Boolean isDockerJobMountAllowed(){
    checkCache();
    return DOCKER_JOB_MOUNT_ALLOWED;
  }

  private Boolean DOCKER_JOB_UID_STRICT = true;
  public synchronized Boolean isDockerJobUidStrict(){
    checkCache();
    return DOCKER_JOB_UID_STRICT;
  }

  private int EXECUTIONS_PER_JOB_LIMIT = 10000;
  public synchronized int getExecutionsPerJobLimit(){
    checkCache();
    return EXECUTIONS_PER_JOB_LIMIT;
  }

  private int EXECUTIONS_CLEANER_BATCH_SIZE = 1000;
  public synchronized int getExecutionsCleanerBatchSize(){
    checkCache();
    return EXECUTIONS_CLEANER_BATCH_SIZE;
  }

  private int EXECUTIONS_CLEANER_INTERVAL_MS = 600000;
  public synchronized int getExecutionsCleanerInterval(){
    checkCache();
    return EXECUTIONS_CLEANER_INTERVAL_MS;
  }

  private int MAX_ENV_YML_BYTE_SIZE = 20000;
  public synchronized int getMaxEnvYmlByteSize() {
    checkCache();
    return MAX_ENV_YML_BYTE_SIZE;
  }
  
  private int LIVY_STARTUP_TIMEOUT = 240;
  public synchronized int getLivyStartupTimeout() {
    checkCache();
    return LIVY_STARTUP_TIMEOUT;
  }
  
  private boolean USER_SEARCH_ENABLED = true;
  public synchronized boolean isUserSearchEnabled() {
    checkCache();
    return USER_SEARCH_ENABLED;
  }
  
  /*
   * When a user try to connect for the first time with OAuth or LDAP
   * do not create the user if it does not bellong to any group.
   * This is to avoid having users that belong to no group poluting the users table
   */
  private boolean REJECT_REMOTE_USER_NO_GROUP = false;
  public synchronized boolean getRejectRemoteNoGroup() {
    checkCache();
    return REJECT_REMOTE_USER_NO_GROUP;
  }
  
  public void updateRejectRemoteNoGroup(boolean reject) {
    updateVariableInternal(VARIABLE_REJECT_REMOTE_USER_NO_GROUP, Boolean.toString(reject), VariablesVisibility.ADMIN);
  }
  
  private boolean SKIP_NAMESPACE_CREATION = false;
  public synchronized boolean shouldSkipNamespaceCreation() {
    checkCache();
    return SKIP_NAMESPACE_CREATION;
  }

  private long QUOTAS_ONLINE_ENABLED_FEATUREGROUPS = -1L;
  public synchronized long getQuotasOnlineEnabledFeaturegroups() {
    checkCache();
    return QUOTAS_ONLINE_ENABLED_FEATUREGROUPS;
  }

  private long QUOTAS_ONLINE_DISABLED_FEATUREGROUPS = -1L;
  public synchronized long getQuotasOnlineDisabledFeaturegroups() {
    checkCache();
    return QUOTAS_ONLINE_DISABLED_FEATUREGROUPS;
  }

  private long QUOTAS_TRAINING_DATASETS = -1L;
  public synchronized long getQuotasTrainingDatasets() {
    checkCache();
    return QUOTAS_TRAINING_DATASETS;
  }

  private long QUOTAS_RUNNING_MODEL_DEPLOYMENTS = -1L;
  public synchronized long getQuotasRunningModelDeployments() {
    checkCache();
    return QUOTAS_RUNNING_MODEL_DEPLOYMENTS;
  }

  private long QUOTAS_TOTAL_MODEL_DEPLOYMENTS = -1L;
  public synchronized long getQuotasTotalModelDeployments() {
    checkCache();
    return QUOTAS_TOTAL_MODEL_DEPLOYMENTS;
  }

  private long QUOTAS_MAX_PARALLEL_EXECUTIONS = -1L;
  public synchronized long getQuotasMaxParallelExecutions() {
    checkCache();
    return QUOTAS_MAX_PARALLEL_EXECUTIONS;
  }

  private static final String VARIABLE_SQL_MAX_SELECT_IN = "sql_max_select_in";
  private Integer SQL_MAX_SELECT_IN = 100;
  /**
   * For performance reasons SELECT ... WHERE col_name IN (.. , ..) queries should not have an unbounded in array.
   */
  public synchronized Integer getSQLMaxSelectIn() {
    checkCache();
    return SQL_MAX_SELECT_IN;
  }

  // The maximum number of http threads in the thread pool is set to 200 by default
  private int MAX_LONG_RUNNING_HTTP_REQUESTS = 50;

  public synchronized int getMaxLongRunningHttpRequests() {
    checkCache();
    return MAX_LONG_RUNNING_HTTP_REQUESTS;
  }
}
