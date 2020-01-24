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
import io.hops.hopsworks.common.dao.kafka.KafkaConst;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserAccountsEmailMessages;
import io.hops.hopsworks.common.dao.util.Variables;
import io.hops.hopsworks.common.dela.AddressJSON;
import io.hops.hopsworks.common.dela.DelaClientType;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.dto.ProvTypeDTO;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTLogLevel;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class Settings implements Serializable {

  private static final Logger LOGGER = Logger.getLogger(Settings.class.
      getName());

  @EJB
  private UserFacade userFacade;
  @EJB
  private ProjectUtils projectUtils;
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

  @PostConstruct
  private void init() {
    try {
      setKafkaBrokers(getBrokerEndpoints());
    } catch (IOException | KeeperException | InterruptedException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  public static final String AGENT_EMAIL = "kagent@hops.io";
  /**
   * Global Variables taken from the DB
   */
  private static final String VARIABLE_ADMIN_EMAIL = "admin_email";
  private static final String VARIABLE_PYPI_REST_ENDPOINT = "pypi_rest_endpoint";
  private static final String VARIABLE_PYTHON_KERNEL = "python_kernel";
  private static final String VARIABLE_HADOOP_VERSION = "hadoop_version";
  private static final String VARIABLE_JAVA_HOME = "JAVA_HOME";
  private static final String VARIABLE_HOPSWORKS_IP = "hopsworks_ip";
  private static final String VARIABLE_KIBANA_IP = "kibana_ip";
  private static final String VARIABLE_LIVY_IP = "livy_ip";
  private static final String VARIABLE_JHS_IP = "jhs_ip";
  private static final String VARIABLE_RM_IP = "rm_ip";
  private static final String VARIABLE_RM_PORT = "rm_port";
  private static final String VARIABLE_LOCALHOST = "localhost";
  private static final String VARIABLE_REQUESTS_VERIFY = "requests_verify";
  private static final String VARIABLE_CLOUD= "cloud";
  private static final String VARIABLE_AWS_INSTANCE_ROLE = "aws_instance_role";
  private static final String VARIABLE_LOGSTASH_IP = "logstash_ip";
  private static final String VARIABLE_LOGSTASH_PORT = "logstash_port";
  private static final String VARIABLE_LOGSTASH_PORT_TF_SERVING = "logstash_port_tf_serving";
  private static final String VARIABLE_LOGSTASH_PORT_SKLEARN_SERVING = "logstash_port_sklearn_serving";
  private static final String VARIABLE_OOZIE_IP = "oozie_ip";
  private static final String VARIABLE_SPARK_HISTORY_SERVER_IP
      = "spark_history_server_ip";
  private static final String VARIABLE_ELASTIC_IP = "elastic_ip";
  private static final String VARIABLE_ELASTIC_PORT = "elastic_port";
  private static final String VARIABLE_ELASTIC_REST_PORT = "elastic_rest_port";
  private static final String VARIABLE_ELASTIC_LOGS_INDEX_EXPIRATION = "elastic_logs_index_expiration";
  private static final String VARIABLE_SPARK_USER = "spark_user";
  private static final String VARIABLE_YARN_SUPERUSER = "yarn_user";
  private static final String VARIABLE_HDFS_SUPERUSER = "hdfs_user";
  private static final String VARIABLE_HOPSWORKS_USER = "hopsworks_user";
  private static final String VARIABLE_JUPYTER_GROUP = "jupyter_group";
  private static final String VARIABLE_JUPYTER_USER = "jupyter_user";
  private static final String VARIABLE_AIRFLOW_USER = "airflow_user";
  private static final String VARIABLE_STAGING_DIR = "staging_dir";
  private static final String VARIABLE_AIRFLOW_DIR = "airflow_dir";
  private static final String VARIABLE_JUPYTER_DIR = "jupyter_dir";
  private static final String VARIABLE_JUPYTER_WS_PING_INTERVAL = "jupyter_ws_ping_interval";
  private static final String VARIABLE_SPARK_DIR = "spark_dir";
  private static final String VARIABLE_FLINK_DIR = "flink_dir";
  private static final String VARIABLE_FLINK_USER = "flink_user";
  private static final String VARIABLE_NDB_DIR = "ndb_dir";
  private static final String VARIABLE_MYSQL_DIR = "mysql_dir";
  private static final String VARIABLE_MYSQL_USER = "mysql_user";
  private static final String VARIABLE_HADOOP_DIR = "hadoop_dir";
  private static final String VARIABLE_HOPSWORKS_DIR = "hopsworks_dir";
  private static final String VARIABLE_SUDOERS_DIR = "sudoers_dir";
  private static final String VARIABLE_YARN_DEFAULT_QUOTA = "yarn_default_quota";
  private static final String VARIABLE_HDFS_DEFAULT_QUOTA = "hdfs_default_quota";
  private static final String VARIABLE_MAX_NUM_PROJ_PER_USER
      = "max_num_proj_per_user";
  private static final String VARIABLE_RESERVED_PROJECT_NAMES = "reserved_project_names";
  
  // HIVE configuration variables
  private static final String VARIABLE_HIVE_SERVER_HOSTNAME = "hiveserver_ssl_hostname";
  private static final String VARIABLE_HIVE_SERVER_HOSTNAME_EXT
      = "hiveserver_ext_hostname";
  private static final String VARIABLE_HIVE_SUPERUSER = "hive_superuser";
  private static final String VARIABLE_HIVE_WAREHOUSE = "hive_warehouse";
  private static final String VARIABLE_HIVE_SCRATCHDIR = "hive_scratchdir";
  private static final String VARIABLE_HIVE_SCRATCHDIR_DELAY = "hive_scratchdir_delay";
  private static final String VARIABLE_HIVE_SCRATCHDIR_CLEANER_INTERVAL = "hive_scratchdir_cleaner_interval";
  private static final String VARIABLE_HIVE_DEFAULT_QUOTA = "hive_default_quota";
  private static final String VARIABLE_HIVE_LLAP_SLIDER_DIR = "hive_llap_slider_dir";
  private static final String VARIABLE_HIVE_LLAP_LOCAL_DIR = "hive_llap_local_dir";
  public static final String VARIABLE_LLAP_APP_ID = "hive_llap_app_id";
  public static final String VARIABLE_LLAP_START_PROC = "hive_llap_start_proc";

  private static final String VARIABLE_TWOFACTOR_AUTH = "twofactor_auth";
  private static final String VARIABLE_TWOFACTOR_EXCLUD = "twofactor-excluded-groups";
  private static final String VARIABLE_KAFKA_DIR = "kafka_dir";
  private static final String VARIABLE_KAFKA_USER = "kafka_user";
  private static final String VARIABLE_KAFKA_MAX_NUM_TOPICS = "kafka_max_num_topics";
  private static final String VARIABLE_ZK_DIR = "zk_dir";
  private static final String VARIABLE_ZK_USER = "zk_user";
  private static final String VARIABLE_ZK_IP = "zk_ip";
  private static final String VARIABLE_DRELEPHANT_IP = "drelephant_ip";
  private static final String VARIABLE_DRELEPHANT_DB = "drelephant_db";
  private static final String VARIABLE_DRELEPHANT_PORT = "drelephant_port";
  private static final String VARIABLE_YARN_WEB_UI_IP = "yarn_ui_ip";
  private static final String VARIABLE_YARN_WEB_UI_PORT = "yarn_ui_port";
  private static final String VARIABLE_FILE_PREVIEW_IMAGE_SIZE
      = "file_preview_image_size";
  private static final String VARIABLE_FILE_PREVIEW_TXT_SIZE
      = "file_preview_txt_size";
  private static final String VARIABLE_HOPSWORKS_REST_ENDPOINT
      = "hopsworks_endpoint";
  private static final String VARIABLE_HOPS_RPC_TLS = "hops_rpc_tls";
  public static final String ERASURE_CODING_CONFIG = "erasure-coding-site.xml";

  private static final String VARIABLE_KAFKA_NUM_PARTITIONS
      = "kafka_num_partitions";
  private static final String VARIABLE_KAFKA_NUM_REPLICAS = "kafka_num_replicas";
  private static final String VARIABLE_HOPSWORKS_SSL_MASTER_PASSWORD = "hopsworks_master_password";
  private static final String VARIABLE_CUDA_DIR = "conda_dir";
  private static final String VARIABLE_ANACONDA_USER = "anaconda_user";
  private static final String VARIABLE_ANACONDA_DIR = "anaconda_dir";
  private static final String VARIABLE_ANACONDA_ENABLED = "anaconda_enabled";
  private static final String VARIABLE_ANACONDA_DEFAULT_REPO = "conda_default_repo";

  private static final String VARIABLE_DOWNLOAD_ALLOWED = "download_allowed";
  private static final String VARIABLE_SUPPORT_EMAIL_ADDR = "support_email_addr";
  private static final String VARIABLE_HOPSEXAMPLES_VERSION = "hopsexamples_version";

  private static final String VARIABLE_INFLUXDB_IP = "influxdb_ip";
  private static final String VARIABLE_INFLUXDB_PORT = "influxdb_port";
  private static final String VARIABLE_INFLUXDB_USER = "influxdb_user";
  private static final String VARIABLE_INFLUXDB_PW = "influxdb_pw";
  private static final String VARIABLE_KAGENT_USER = "kagent_user";
  private static final String VARIABLE_KAGENT_LIVENESS_MONITOR_ENABLED = "kagent_liveness_monitor_enabled";
  private static final String VARIABLE_KAGENT_LIVENESS_THRESHOLD = "kagent_liveness_threshold";
  private static final String VARIABLE_RESOURCE_DIRS = "resources";
  private static final String VARIABLE_CERTS_DIRS = "certs_dir";
  private static final String VARIABLE_MAX_STATUS_POLL_RETRY = "max_status_poll_retry";
  private static final String VARIABLE_CERT_MATER_DELAY = "cert_mater_delay";
  private static final String VARIABLE_WHITELIST_USERS_LOGIN = "whitelist_users";
  private static final String VARIABLE_VERIFICATION_PATH = "verification_endpoint";
  private static final String VARIABLE_ALERT_EMAIL_ADDRS = "alert_email_addrs";
  private static final String VARIABLE_FIRST_TIME_LOGIN = "first_time_login";
  private static final String VARIABLE_CERTIFICATE_USER_VALID_DAYS = "certificate_user_valid_days";

  private static final String VARIABLE_ZOOKEEPER_VERSION = "zookeeper_version";
  private static final String VARIABLE_INFLUXDB_VERSION = "influxdb_version";
  private static final String VARIABLE_GRAFANA_VERSION = "grafana_version";
  private static final String VARIABLE_TELEGRAF_VERSION = "telegraf_version";
  private static final String VARIABLE_KAPACITOR_VERSION = "kapacitor_version";
  private static final String VARIABLE_LOGSTASH_VERSION = "logstash_version";
  private static final String VARIABLE_KIBANA_VERSION = "kibana_version";
  private static final String VARIABLE_FILEBEAT_VERSION = "filebeat_version";
  private static final String VARIABLE_NDB_VERSION = "ndb_version";
  private static final String VARIABLE_LIVY_VERSION = "livy_version";
  private static final String VARIABLE_HIVE2_VERSION = "hive2_version";
  private static final String VARIABLE_TEZ_VERSION = "tez_version";
  private static final String VARIABLE_SLIDER_VERSION = "slider_version";
  private static final String VARIABLE_SPARK_VERSION = "spark_version";
  private static final String VARIABLE_FLINK_VERSION = "flink_version";
  private static final String VARIABLE_EPIPE_VERSION = "epipe_version";
  private static final String VARIABLE_DELA_VERSION = "dela_version";
  private static final String VARIABLE_KAFKA_VERSION = "kafka_version";
  private static final String VARIABLE_ELASTIC_VERSION = "elastic_version";
  private static final String VARIABLE_DRELEPHANT_VERSION = "drelephant_version";
  private static final String VARIABLE_TENSORFLOW_VERSION = "tensorflow_version";
  private static final String VARIABLE_CUDA_VERSION = "cuda_version";
  private static final String VARIABLE_HOPSWORKS_VERSION = "hopsworks_version";
  private static final String VARIABLE_HOPS_VERIFICATION_VERSION = "hops_verification_version";

  //Used by RESTException to include devMsg or not in response
  private static final String VARIABLE_HOPSWORKS_REST_LOG_LEVEL = "hopsworks_rest_log_level";

  /*
   * -------------------- Serving ---------------
   */
  private static final String VARIABLE_SERVING_MONITOR_INT = "serving_monitor_int";

  /*
   * -------------------- Serving ---------------
   */
  private static final String VARIABLE_SERVING_CONNECTION_POOL_SIZE = "serving_connection_pool_size";
  private static final String VARIABLE_SERVING_MAX_ROUTE_CONNECTIONS = "serving_max_route_connections";

  /*
   * -------------------- Kubernetes ---------------
   */
  private static final String VARIABLE_KUBEMASTER_URL = "kube_master_url";
  private static final String VARIABLE_KUBE_USER = "kube_user";
  private static final String VARIABLE_KUBE_CA_CERTFILE = "kube_ca_certfile";
  private static final String VARIABLE_KUBE_CLIENT_KEYFILE = "kube_client_keyfile";
  private static final String VARIABLE_KUBE_CLIENT_CERTFILE = "kube_client_certfile";
  private static final String VARIABLE_KUBE_CLIENT_KEYPASS = "kube_client_keypass";
  private static final String VARIABLE_KUBE_TRUSTSTORE_PATH = "kube_truststore_path";
  private static final String VARIABLE_KUBE_TRUSTSTORE_KEY = "kube_truststore_key";
  private static final String VARIABLE_KUBE_KEYSTORE_PATH = "kube_keystore_path";
  private static final String VARIABLE_KUBE_KEYSTORE_KEY = "kube_keystore_key";
  private static final String VARIABLE_KUBE_REGISTRY = "kube_registry";
  private static final String VARIABLE_KUBE_MAX_SERVING = "kube_max_serving_instances";
  private static final String VARIABLE_KUBE_API_MAX_ATTEMPTS = "kube_api_max_attempts";
  private static final String VARIABLE_KUBE_DOCKER_MAX_MEMORY_ALLOCATION = "kube_docker_max_memory_allocation";
  private static final String VARIABLE_KUBE_DOCKER_CORES_FRACTION = "kube_docker_cores_fraction";
  private static final String VARIABLE_KUBE_DOCKER_MAX_CORES_ALLOCATION = "kube_docker_max_cores_allocation";

  // Container image versions
  private static final String VARIABLE_KUBE_TF_IMG_VERSION = "kube_tf_img_version";
  private static final String VARIABLE_KUBE_SKLEARN_IMG_VERSION = "kube_sklearn_img_version";
  private static final String VARIABLE_KUBE_FILEBEAT_IMG_VERSION = "kube_filebeat_img_version";
  private static final String VARIABLE_KUBE_JUPYTER_IMG_VERSION = "kube_jupyter_img_version";

  /*
   * -------------------- Jupyter ---------------
   */
  private static final String VARIABLE_JUPYTER_HOST = "jupyter_host";
  private static final String VARIABLE_JUPYTER_ORIGIN_SCHEME = "jupyter_origin_scheme";

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

  //Elastic OpenDistro
  private static final String VARIABLE_ELASTIC_OPENDISTRO_SECURITY_ENABLED = "elastic_opendistro_security_enabled";
  private static final String VARIABLE_ELASTIC_HTTPS_ENABLED = "elastic_https_enabled";
  private static final String VARIABLE_ELASTIC_ADMIN_USER = "elastic_admin_user";
  private static final String VARIABLE_ELASTIC_ADMIN_PASSWORD = "elastic_admin_password";
  private static final String VARIABLE_KIBANA_HTTPS_ENABLED = "kibana_https_enabled";
  private static final String VARIABLE_ELASTIC_JWT_ENABLED = "elastic_jwt_enabled";
  private static final String VARIABLE_ELASTIC_JWT_URL_PARAMETER = "elastic_jwt_url_parameter";
  private static final String VARIABLE_ELASTIC_JWT_EXP_MS = "elastic_jwt_exp_ms";
  private static final String VARIABLE_KIBANA_MULTI_TENANCY_ENABLED = "kibana_multi_tenancy_enabled";
  
  private String setVar(String varName, String defaultValue) {
    Variables userName = findById(varName);
    if (userName != null && userName.getValue() != null && (!userName.getValue().isEmpty())) {
      String user = userName.getValue();
      if (user != null && !user.isEmpty()) {
        return user;
      }
    }
    return defaultValue;
  }

  private String setStrVar(String varName, String defaultValue) {
    Variables var = findById(varName);
    if (var != null && var.getValue() != null) {
      String val = var.getValue();
      if (val != null && !val.isEmpty()) {
        return val;
      }
    }
    return defaultValue;
  }

  private String setDirVar(String varName, String defaultValue) {
    Variables dirName = findById(varName);
    if (dirName != null && dirName.getValue() != null && (new File(dirName.
        getValue()).isDirectory())) {
      String val = dirName.getValue();
      if (val != null && !val.isEmpty()) {
        return val;
      }
    }
    return defaultValue;
  }

  private String setIpVar(String varName, String defaultValue) {
    Variables var = findById(varName);
    if (var != null && var.getValue() != null && Ip.validIp(var.getValue())) {
      String val = var.getValue();
      if (val != null && !val.isEmpty()) {
        return val;
      }
    }
    return defaultValue;
  }

  private String setDbVar(String varName, String defaultValue) {
    Variables var = findById(varName);
    if (var != null && var.getValue() != null) {
      // TODO - check this is a valid DB name
      String val = var.getValue();
      if (val != null && !val.isEmpty()) {
        return val;
      }
    }
    return defaultValue;
  }

  private Boolean setBoolVar(String varName, Boolean defaultValue) {
    Variables var = findById(varName);
    if (var != null && var.getValue() != null) {
      String val = var.getValue();
      if (val != null && !val.isEmpty()) {
        return Boolean.parseBoolean(val);
      }
    }
    return defaultValue;
  }

  private Integer setIntVar(String varName, Integer defaultValue) {
    Variables var = findById(varName);
    try {
      if (var != null && var.getValue() != null) {
        String val = var.getValue();
        if (val != null && !val.isEmpty()) {
          return Integer.parseInt(val);
        }
      }
    } catch (NumberFormatException ex) {
      LOGGER.info("Error - not an integer! " + varName
          + " should be an integer. Value was " + defaultValue);
    }
    return defaultValue;
  }

  private long setLongVar(String varName, Long defaultValue) {
    Variables var = findById(varName);
    try {
      if (var != null && var.getValue() != null) {
        String val = var.getValue();
        if (val != null && !val.isEmpty()) {
          return Long.parseLong(val);
        }
      }
    } catch (NumberFormatException ex) {
      LOGGER.info("Error - not a long! " + varName
          + " should be an integer. Value was " + defaultValue);
    }

    return defaultValue;
  }

  private Double setDoubleVar(String varName, Double defaultValue) {
    Variables var = findById(varName);
    try {
      if (var != null && var.getValue() != null) {
        String val = var.getValue();
        if (val != null && !val.isEmpty()) {
          return Double.parseDouble(val);
        }
      }
    } catch (NumberFormatException ex) {
      LOGGER.info("Error - not a double! " + varName
        + " should be a double. Value was " + defaultValue);
    }
    return defaultValue;
  }

  private RESTLogLevel setLogLevelVar(String varName, RESTLogLevel defaultValue) {
    Variables var = findById(varName);
    if (var != null && var.getValue() != null) {
      String val = var.getValue();
      if (val != null && !val.isEmpty()) {
        return RESTLogLevel.valueOf(val);
      }
    }
    return defaultValue;
  }

  private long setMillisecondVar(String varName, Long defaultValue) {
    Variables var = findById(varName);
    if (var != null && var.getValue() != null && !var.getValue().isEmpty()) {
      String val = var.getValue();
      long timeValue = getConfTimeValue(val);
      TimeUnit timeUnit = getConfTimeTimeUnit(val);
      return timeUnit.toMillis(timeValue);
    }

    return defaultValue;
  }

  private boolean cached = false;

  private void populateCache() {
    if (!cached) {
      ADMIN_EMAIL = setVar(VARIABLE_ADMIN_EMAIL, ADMIN_EMAIL);
      LOCALHOST = setBoolVar(VARIABLE_LOCALHOST, LOCALHOST);
      CLOUD = setStrVar(VARIABLE_CLOUD, CLOUD);
      IAM_ROLE_CONFIGURED = setBoolVar(VARIABLE_AWS_INSTANCE_ROLE, IAM_ROLE_CONFIGURED);
      REQUESTS_VERIFY = setBoolVar(VARIABLE_REQUESTS_VERIFY, REQUESTS_VERIFY);
      PYTHON_KERNEL = setBoolVar(VARIABLE_PYTHON_KERNEL, PYTHON_KERNEL);
      JAVA_HOME = setVar(VARIABLE_JAVA_HOME, JAVA_HOME);
      TWOFACTOR_AUTH = setVar(VARIABLE_TWOFACTOR_AUTH, TWOFACTOR_AUTH);
      TWOFACTOR_EXCLUDE = setVar(VARIABLE_TWOFACTOR_EXCLUD, TWOFACTOR_EXCLUDE);
      HOPSWORKS_USER = setVar(VARIABLE_HOPSWORKS_USER, HOPSWORKS_USER);
      JUPYTER_USER = setVar(VARIABLE_JUPYTER_USER, JUPYTER_USER);
      JUPYTER_GROUP = setVar(VARIABLE_JUPYTER_GROUP, JUPYTER_GROUP);
      JUPYTER_ORIGIN_SCHEME = setVar(VARIABLE_JUPYTER_ORIGIN_SCHEME, JUPYTER_ORIGIN_SCHEME);
      AIRFLOW_USER = setVar(VARIABLE_AIRFLOW_USER, AIRFLOW_USER);
      HDFS_SUPERUSER = setVar(VARIABLE_HDFS_SUPERUSER, HDFS_SUPERUSER);
      YARN_SUPERUSER = setVar(VARIABLE_YARN_SUPERUSER, YARN_SUPERUSER);
      SPARK_USER = setVar(VARIABLE_SPARK_USER, SPARK_USER);
      SPARK_DIR = setDirVar(VARIABLE_SPARK_DIR, SPARK_DIR);
      FLINK_USER = setVar(VARIABLE_FLINK_USER, FLINK_USER);
      FLINK_DIR = setDirVar(VARIABLE_FLINK_DIR, FLINK_DIR);
      STAGING_DIR = setDirVar(VARIABLE_STAGING_DIR, STAGING_DIR);
      HOPS_EXAMPLES_VERSION = setVar(VARIABLE_HOPSEXAMPLES_VERSION, HOPS_EXAMPLES_VERSION);
      HIVE_SERVER_HOSTNAME = setStrVar(VARIABLE_HIVE_SERVER_HOSTNAME,
          HIVE_SERVER_HOSTNAME);
      HIVE_SERVER_HOSTNAME_EXT = setStrVar(VARIABLE_HIVE_SERVER_HOSTNAME_EXT,
          HIVE_SERVER_HOSTNAME_EXT);
      HIVE_SUPERUSER = setStrVar(VARIABLE_HIVE_SUPERUSER, HIVE_SUPERUSER);
      HIVE_WAREHOUSE = setStrVar(VARIABLE_HIVE_WAREHOUSE, HIVE_WAREHOUSE);
      HIVE_LLAP_SLIDER_DIR = setStrVar(VARIABLE_HIVE_LLAP_SLIDER_DIR, HIVE_LLAP_SLIDER_DIR);
      HIVE_LLAP_LOCAL_FS_DIR = setStrVar(VARIABLE_HIVE_LLAP_LOCAL_DIR, HIVE_LLAP_LOCAL_FS_DIR);
      HIVE_SCRATCHDIR = setStrVar(VARIABLE_HIVE_SCRATCHDIR, HIVE_SCRATCHDIR);
      HIVE_SCRATCHDIR_DELAY = setStrVar(VARIABLE_HIVE_SCRATCHDIR_DELAY, HIVE_SCRATCHDIR_DELAY);
      HIVE_SCRATCHDIR_CLEANER_INTERVAL = setStrVar(VARIABLE_HIVE_SCRATCHDIR_CLEANER_INTERVAL,
          HIVE_SCRATCHDIR_CLEANER_INTERVAL);
      HIVE_DB_DEFAULT_QUOTA = setStrVar(VARIABLE_HIVE_DEFAULT_QUOTA, HIVE_DB_DEFAULT_QUOTA);
      ALERT_EMAIL_ADDRS = setStrVar(VARIABLE_ALERT_EMAIL_ADDRS, "");
      HADOOP_VERSION = setVar(VARIABLE_HADOOP_VERSION, HADOOP_VERSION);
      JUPYTER_DIR = setDirVar(VARIABLE_JUPYTER_DIR, JUPYTER_DIR);
      JUPYTER_WS_PING_INTERVAL_MS = setMillisecondVar(VARIABLE_JUPYTER_WS_PING_INTERVAL, JUPYTER_WS_PING_INTERVAL_MS);
      MYSQL_DIR = setDirVar(VARIABLE_MYSQL_DIR, MYSQL_DIR);
      MYSQL_USER = setStrVar(VARIABLE_MYSQL_USER, MYSQL_USER);
      HADOOP_DIR = setDirVar(VARIABLE_HADOOP_DIR, HADOOP_DIR);
      HOPSWORKS_INSTALL_DIR = setDirVar(VARIABLE_HOPSWORKS_DIR, HOPSWORKS_INSTALL_DIR);
      CERTS_DIR = setDirVar(VARIABLE_CERTS_DIRS, CERTS_DIR);
      SUDOERS_DIR = setDirVar(VARIABLE_SUDOERS_DIR, SUDOERS_DIR);
      CERTIFICATE_USER_VALID_DAYS = setStrVar(VARIABLE_CERTIFICATE_USER_VALID_DAYS, CERTIFICATE_USER_VALID_DAYS);
      NDB_DIR = setDirVar(VARIABLE_NDB_DIR, NDB_DIR);
      AIRFLOW_DIR = setDirVar(VARIABLE_AIRFLOW_DIR, AIRFLOW_DIR);
      String elasticIps = setStrVar(VARIABLE_ELASTIC_IP,
          ElasticSettings.ELASTIC_IP_DEFAULT);
      int elasticPort = setIntVar(VARIABLE_ELASTIC_PORT, ElasticSettings.ELASTIC_PORT_DEFAULT);
      int elasticRestPort = setIntVar(VARIABLE_ELASTIC_REST_PORT,
          ElasticSettings.ELASTIC_REST_PORT_DEFAULT);
      boolean elasticOpenDistroEnabled =
          setBoolVar(VARIABLE_ELASTIC_OPENDISTRO_SECURITY_ENABLED,
              ElasticSettings.ELASTIC_OPENDISTRO_SECURTIY_ENABLED_DEFAULT);
      boolean elasticHttpsEnabled = setBoolVar(VARIABLE_ELASTIC_HTTPS_ENABLED
          , ElasticSettings.ELASTIC_HTTPS_ENABLED_DEFAULT);
      String elasticAdminUser = setStrVar(VARIABLE_ELASTIC_ADMIN_USER,
          ElasticSettings.ELASTIC_ADMIN_USER_DEFAULT);
      String elasticAdminPassword = setStrVar(VARIABLE_ELASTIC_ADMIN_PASSWORD,
          ElasticSettings.ELASTIC_ADMIN_PASSWORD_DEFAULT);
      boolean elasticJWTEnabled =  setBoolVar(VARIABLE_ELASTIC_JWT_ENABLED
          , ElasticSettings.ELASTIC_JWT_ENABLED_DEFAULT);
      String elasticJWTUrlParameter = setStrVar(VARIABLE_ELASTIC_JWT_URL_PARAMETER,
          ElasticSettings.ELASTIC_JWT_URL_PARAMETER_DEFAULT);
      long elasticJWTEXPMS = setLongVar(VARIABLE_ELASTIC_JWT_EXP_MS,
          ElasticSettings.ELASTIC_JWT_EXP_MS_DEFAULT);
      ELASTIC_SETTINGS = new ElasticSettings(elasticIps, elasticPort,
          elasticRestPort, elasticOpenDistroEnabled, elasticHttpsEnabled,
          elasticAdminUser, elasticAdminPassword, elasticJWTEnabled,
          elasticJWTUrlParameter, elasticJWTEXPMS);
      ELASTIC_LOGS_INDEX_EXPIRATION = setLongVar(VARIABLE_ELASTIC_LOGS_INDEX_EXPIRATION, ELASTIC_LOGS_INDEX_EXPIRATION);
      HOPSWORKS_IP = setIpVar(VARIABLE_HOPSWORKS_IP, HOPSWORKS_IP);
      RM_IP = setIpVar(VARIABLE_RM_IP, RM_IP);
      RM_PORT = setIntVar(VARIABLE_RM_PORT, RM_PORT);
      LOGSTASH_IP = setIpVar(VARIABLE_LOGSTASH_IP, LOGSTASH_IP);
      LOGSTASH_PORT = setIntVar(VARIABLE_LOGSTASH_PORT, LOGSTASH_PORT);
      LOGSTASH_PORT_TF_SERVING = setIntVar(VARIABLE_LOGSTASH_PORT_TF_SERVING, LOGSTASH_PORT_TF_SERVING);
      LOGSTASH_PORT_SKLEARN_SERVING = setIntVar(VARIABLE_LOGSTASH_PORT_SKLEARN_SERVING, LOGSTASH_PORT_SKLEARN_SERVING);
      JHS_IP = setIpVar(VARIABLE_JHS_IP, JHS_IP);
      LIVY_IP = setIpVar(VARIABLE_LIVY_IP, LIVY_IP);
      OOZIE_IP = setIpVar(VARIABLE_OOZIE_IP, OOZIE_IP);
      SPARK_HISTORY_SERVER_IP = setIpVar(VARIABLE_SPARK_HISTORY_SERVER_IP,
          SPARK_HISTORY_SERVER_IP);
      ZK_IP = setIpVar(VARIABLE_ZK_IP, ZK_IP);
      ZK_USER = setVar(VARIABLE_ZK_USER, ZK_USER);
      ZK_DIR = setDirVar(VARIABLE_ZK_DIR, ZK_DIR);
      DRELEPHANT_IP = setIpVar(VARIABLE_DRELEPHANT_IP, DRELEPHANT_IP);
      DRELEPHANT_PORT = setIntVar(VARIABLE_DRELEPHANT_PORT, DRELEPHANT_PORT);
      DRELEPHANT_DB = setDbVar(VARIABLE_DRELEPHANT_DB, DRELEPHANT_DB);
      KIBANA_IP = setIpVar(VARIABLE_KIBANA_IP, KIBANA_IP);
      KAFKA_MAX_NUM_TOPICS = setIntVar(VARIABLE_KAFKA_MAX_NUM_TOPICS, KAFKA_MAX_NUM_TOPICS);
      HOPSWORKS_DEFAULT_SSL_MASTER_PASSWORD = setVar(VARIABLE_HOPSWORKS_SSL_MASTER_PASSWORD,
          HOPSWORKS_DEFAULT_SSL_MASTER_PASSWORD);
      KAFKA_USER = setVar(VARIABLE_KAFKA_USER, KAFKA_USER);
      KAFKA_DIR = setDirVar(VARIABLE_KAFKA_DIR, KAFKA_DIR);
      KAFKA_DEFAULT_NUM_PARTITIONS = setIntVar(VARIABLE_KAFKA_NUM_PARTITIONS,
          KAFKA_DEFAULT_NUM_PARTITIONS);
      KAFKA_DEFAULT_NUM_REPLICAS = setIntVar(VARIABLE_KAFKA_NUM_REPLICAS,
          KAFKA_DEFAULT_NUM_REPLICAS);
      YARN_DEFAULT_QUOTA = setIntVar(VARIABLE_YARN_DEFAULT_QUOTA,
          YARN_DEFAULT_QUOTA);
      YARN_WEB_UI_IP = setIpVar(VARIABLE_YARN_WEB_UI_IP, YARN_WEB_UI_IP);
      YARN_WEB_UI_PORT = setIntVar(VARIABLE_YARN_WEB_UI_PORT, YARN_WEB_UI_PORT);
      HDFS_DEFAULT_QUOTA_MBs = setDirVar(VARIABLE_HDFS_DEFAULT_QUOTA,
          HDFS_DEFAULT_QUOTA_MBs);
      MAX_NUM_PROJ_PER_USER = setDirVar(VARIABLE_MAX_NUM_PROJ_PER_USER,
          MAX_NUM_PROJ_PER_USER);
      CLUSTER_CERT = setVar(VARIABLE_CLUSTER_CERT, CLUSTER_CERT);
      FILE_PREVIEW_IMAGE_SIZE = setIntVar(VARIABLE_FILE_PREVIEW_IMAGE_SIZE, 10000000);
      FILE_PREVIEW_TXT_SIZE = setIntVar(VARIABLE_FILE_PREVIEW_TXT_SIZE, 100);
      HOPSWORKS_REST_ENDPOINT = setStrVar(VARIABLE_HOPSWORKS_REST_ENDPOINT,
          HOPSWORKS_REST_ENDPOINT);
      CUDA_DIR = setDirVar(VARIABLE_CUDA_DIR, CUDA_DIR);
      ANACONDA_USER = setStrVar(VARIABLE_ANACONDA_USER, ANACONDA_USER);
      ANACONDA_DIR = setDirVar(VARIABLE_ANACONDA_DIR, ANACONDA_DIR);
      ANACONDA_DEFAULT_REPO = setStrVar(VARIABLE_ANACONDA_DEFAULT_REPO, ANACONDA_DEFAULT_REPO);
      ANACONDA_ENABLED = Boolean.parseBoolean(setStrVar(
          VARIABLE_ANACONDA_ENABLED, ANACONDA_ENABLED.toString()));
      KAGENT_USER = setStrVar(VARIABLE_KAGENT_USER, KAGENT_USER);
      KAGENT_LIVENESS_MONITOR_ENABLED = setBoolVar(VARIABLE_KAGENT_LIVENESS_MONITOR_ENABLED,
          KAGENT_LIVENESS_MONITOR_ENABLED);
      KAGENT_LIVENESS_THRESHOLD = setStrVar(VARIABLE_KAGENT_LIVENESS_THRESHOLD, KAGENT_LIVENESS_THRESHOLD);
      DOWNLOAD_ALLOWED = Boolean.parseBoolean(setStrVar(VARIABLE_DOWNLOAD_ALLOWED, DOWNLOAD_ALLOWED.toString()));
      INFLUXDB_IP = setStrVar(VARIABLE_INFLUXDB_IP, INFLUXDB_IP);
      INFLUXDB_PORT = setStrVar(VARIABLE_INFLUXDB_PORT, INFLUXDB_PORT);
      INFLUXDB_USER = setStrVar(VARIABLE_INFLUXDB_USER, INFLUXDB_USER);
      INFLUXDB_PW = setStrVar(VARIABLE_INFLUXDB_PW, INFLUXDB_PW);
      SUPPORT_EMAIL_ADDR = setStrVar(VARIABLE_SUPPORT_EMAIL_ADDR, SUPPORT_EMAIL_ADDR);
      UserAccountsEmailMessages.HOPSWORKS_SUPPORT_EMAIL = SUPPORT_EMAIL_ADDR;
      RESOURCE_DIRS = setStrVar(VARIABLE_RESOURCE_DIRS, RESOURCE_DIRS);
      MAX_STATUS_POLL_RETRY = setIntVar(VARIABLE_MAX_STATUS_POLL_RETRY, MAX_STATUS_POLL_RETRY);
      HOPS_RPC_TLS = setStrVar(VARIABLE_HOPS_RPC_TLS, HOPS_RPC_TLS);
      CERTIFICATE_MATERIALIZER_DELAY = setStrVar(VARIABLE_CERT_MATER_DELAY,
          CERTIFICATE_MATERIALIZER_DELAY);
      WHITELIST_USERS_LOGIN = setStrVar(VARIABLE_WHITELIST_USERS_LOGIN,
          WHITELIST_USERS_LOGIN);
      FIRST_TIME_LOGIN = setStrVar(VARIABLE_FIRST_TIME_LOGIN, FIRST_TIME_LOGIN);
      VERIFICATION_PATH = setStrVar(VARIABLE_VERIFICATION_PATH, VERIFICATION_PATH);
      serviceKeyRotationEnabled = setBoolVar(SERVICE_KEY_ROTATION_ENABLED_KEY, serviceKeyRotationEnabled);
      serviceKeyRotationInterval = setStrVar(SERVICE_KEY_ROTATION_INTERVAL_KEY, serviceKeyRotationInterval);
      tensorBoardMaxLastAccessed = setIntVar(TENSORBOARD_MAX_LAST_ACCESSED, tensorBoardMaxLastAccessed);
      sparkUILogsOffset = setIntVar(SPARK_UI_LOGS_OFFSET, sparkUILogsOffset);
      jupyterShutdownTimerInterval = setStrVar(JUPYTER_SHUTDOWN_TIMER_INTERVAL, jupyterShutdownTimerInterval);

      populateDelaCache();
      populateLDAPCache();

      ZOOKEEPER_VERSION = setStrVar(VARIABLE_ZOOKEEPER_VERSION, ZOOKEEPER_VERSION);
      INFLUXDB_VERSION = setStrVar(VARIABLE_INFLUXDB_VERSION, INFLUXDB_VERSION);
      GRAFANA_VERSION = setStrVar(VARIABLE_GRAFANA_VERSION, GRAFANA_VERSION);
      TELEGRAF_VERSION = setStrVar(VARIABLE_TELEGRAF_VERSION, TELEGRAF_VERSION);
      KAPACITOR_VERSION = setStrVar(VARIABLE_KAPACITOR_VERSION, KAPACITOR_VERSION);
      LOGSTASH_VERSION = setStrVar(VARIABLE_LOGSTASH_VERSION, LOGSTASH_VERSION);
      KIBANA_VERSION = setStrVar(VARIABLE_KIBANA_VERSION, KIBANA_VERSION);
      FILEBEAT_VERSION = setStrVar(VARIABLE_FILEBEAT_VERSION, FILEBEAT_VERSION);
      NDB_VERSION = setStrVar(VARIABLE_NDB_VERSION, NDB_VERSION);
      LIVY_VERSION = setStrVar(VARIABLE_LIVY_VERSION, LIVY_VERSION);
      HIVE2_VERSION = setStrVar(VARIABLE_HIVE2_VERSION, HIVE2_VERSION);
      TEZ_VERSION = setStrVar(VARIABLE_TEZ_VERSION, TEZ_VERSION);
      SLIDER_VERSION = setStrVar(VARIABLE_SLIDER_VERSION, SLIDER_VERSION);
      SPARK_VERSION = setStrVar(VARIABLE_SPARK_VERSION, SPARK_VERSION);
      FLINK_VERSION = setStrVar(VARIABLE_FLINK_VERSION, FLINK_VERSION);
      EPIPE_VERSION = setStrVar(VARIABLE_EPIPE_VERSION, EPIPE_VERSION);
      DELA_VERSION = setStrVar(VARIABLE_DELA_VERSION, DELA_VERSION);
      KAFKA_VERSION = setStrVar(VARIABLE_KAFKA_VERSION, KAFKA_VERSION);
      ELASTIC_VERSION = setStrVar(VARIABLE_ELASTIC_VERSION, ELASTIC_VERSION);
      DRELEPHANT_VERSION = setStrVar(VARIABLE_DRELEPHANT_VERSION, DRELEPHANT_VERSION);
      TENSORFLOW_VERSION = setStrVar(VARIABLE_TENSORFLOW_VERSION, TENSORFLOW_VERSION);
      CUDA_VERSION = setStrVar(VARIABLE_CUDA_VERSION, CUDA_VERSION);
      HOPSWORKS_VERSION = setStrVar(VARIABLE_HOPSWORKS_VERSION, HOPSWORKS_VERSION);
      HOPSWORKS_REST_LOG_LEVEL = setLogLevelVar(VARIABLE_HOPSWORKS_REST_LOG_LEVEL, HOPSWORKS_REST_LOG_LEVEL);
      HOPS_VERIFICATION_VERSION = setStrVar(VARIABLE_HOPS_VERIFICATION_VERSION, HOPS_VERIFICATION_VERSION);

      PYPI_REST_ENDPOINT = setStrVar(VARIABLE_PYPI_REST_ENDPOINT, PYPI_REST_ENDPOINT);
      PROVIDED_PYTHON_LIBRARY_NAMES = toSetFromCsv(
          setStrVar(VARIABLE_PROVIDED_PYTHON_LIBRARY_NAMES, DEFAULT_PROVIDED_PYTHON_LIBRARY_NAMES), ",");
      PREINSTALLED_PYTHON_LIBRARY_NAMES = toSetFromCsv(
          setStrVar(VARIABLE_PREINSTALLED_PYTHON_LIBRARY_NAMES, DEFAULT_PREINSTALLED_PYTHON_LIBRARY_NAMES),
          ",");

      SERVING_MONITOR_INT = setStrVar(VARIABLE_SERVING_MONITOR_INT, SERVING_MONITOR_INT);

      SERVING_CONNECTION_POOL_SIZE = setIntVar(VARIABLE_SERVING_CONNECTION_POOL_SIZE,
        SERVING_CONNECTION_POOL_SIZE);
      SERVING_MAX_ROUTE_CONNECTIONS = setIntVar(VARIABLE_SERVING_MAX_ROUTE_CONNECTIONS,
        SERVING_MAX_ROUTE_CONNECTIONS);

      KUBE_USER = setStrVar(VARIABLE_KUBE_USER, KUBE_USER);
      KUBEMASTER_URL = setStrVar(VARIABLE_KUBEMASTER_URL, KUBEMASTER_URL);
      KUBE_CA_CERTFILE = setStrVar(VARIABLE_KUBE_CA_CERTFILE, KUBE_CA_CERTFILE);
      KUBE_CLIENT_KEYFILE = setStrVar(VARIABLE_KUBE_CLIENT_KEYFILE, KUBE_CLIENT_KEYFILE);
      KUBE_CLIENT_CERTFILE = setStrVar(VARIABLE_KUBE_CLIENT_CERTFILE, KUBE_CLIENT_CERTFILE);
      KUBE_CLIENT_KEYPASS = setStrVar(VARIABLE_KUBE_CLIENT_KEYPASS, KUBE_CLIENT_KEYPASS);
      KUBE_TRUSTSTORE_PATH = setStrVar(VARIABLE_KUBE_TRUSTSTORE_PATH, KUBE_TRUSTSTORE_PATH);
      KUBE_TRUSTSTORE_KEY = setStrVar(VARIABLE_KUBE_TRUSTSTORE_KEY, KUBE_TRUSTSTORE_KEY);
      KUBE_KEYSTORE_PATH = setStrVar(VARIABLE_KUBE_KEYSTORE_PATH, KUBE_KEYSTORE_PATH);
      KUBE_KEYSTORE_KEY = setStrVar(VARIABLE_KUBE_KEYSTORE_KEY, KUBE_KEYSTORE_KEY);
      KUBE_REGISTRY = setStrVar(VARIABLE_KUBE_REGISTRY, KUBE_REGISTRY);
      KUBE_MAX_SERVING_INSTANCES = setIntVar(VARIABLE_KUBE_MAX_SERVING, KUBE_MAX_SERVING_INSTANCES);
      KUBE_TF_IMG_VERSION = setVar(VARIABLE_KUBE_TF_IMG_VERSION, KUBE_TF_IMG_VERSION);
      KUBE_SKLEARN_IMG_VERSION = setVar(VARIABLE_KUBE_SKLEARN_IMG_VERSION, KUBE_SKLEARN_IMG_VERSION);
      KUBE_FILEBEAT_IMG_VERSION = setVar(VARIABLE_KUBE_FILEBEAT_IMG_VERSION, KUBE_FILEBEAT_IMG_VERSION);
      KUBE_JUPYTER_IMG_VERSION = setVar(VARIABLE_KUBE_JUPYTER_IMG_VERSION, KUBE_JUPYTER_IMG_VERSION);
      KUBE_API_MAX_ATTEMPTS = setIntVar(VARIABLE_KUBE_API_MAX_ATTEMPTS, KUBE_API_MAX_ATTEMPTS);
      KUBE_DOCKER_MAX_MEMORY_ALLOCATION = setIntVar(VARIABLE_KUBE_DOCKER_MAX_MEMORY_ALLOCATION,
          KUBE_DOCKER_MAX_MEMORY_ALLOCATION);
      KUBE_DOCKER_MAX_CORES_ALLOCATION = setIntVar(VARIABLE_KUBE_DOCKER_MAX_CORES_ALLOCATION,
        KUBE_DOCKER_MAX_CORES_ALLOCATION);
      KUBE_DOCKER_CORES_FRACTION = setDoubleVar(VARIABLE_KUBE_DOCKER_CORES_FRACTION,
        KUBE_DOCKER_CORES_FRACTION);

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

      FEATURESTORE_DB_DEFAULT_QUOTA = setStrVar(VARIABLE_FEATURESTORE_DEFAULT_QUOTA, FEATURESTORE_DB_DEFAULT_QUOTA);
      FEATURESTORE_DB_DEFAULT_STORAGE_FORMAT =
          setStrVar(VARIABLE_FEATURESTORE_DEFAULT_STORAGE_FORMAT, FEATURESTORE_DB_DEFAULT_STORAGE_FORMAT);
      FEATURESTORE_JDBC_URL = setStrVar(VARIABLE_FEATURESTORE_JDBC_URL, FEATURESTORE_JDBC_URL);
      ONLINE_FEATURESTORE = setBoolVar(VARIABLE_ONLINE_FEATURESTORE, ONLINE_FEATURESTORE);
  
      KIBANA_HTTPS_ENABELED = setBoolVar(VARIABLE_KIBANA_HTTPS_ENABLED,
          KIBANA_HTTPS_ENABELED);
  
      KIBANA_MULTI_TENANCY_ENABELED = setBoolVar(VARIABLE_KIBANA_MULTI_TENANCY_ENABLED,
          KIBANA_MULTI_TENANCY_ENABELED);
      
      String reservedProjectNamesRaw = setStrVar(VARIABLE_RESERVED_PROJECT_NAMES, DEFAULT_RESERVED_PROJECT_NAMES);
      StringTokenizer tokenizer = new StringTokenizer(reservedProjectNamesRaw, ",");
      RESERVED_PROJECT_NAMES = new HashSet<>(tokenizer.countTokens());
      while (tokenizer.hasMoreTokens()) {
        RESERVED_PROJECT_NAMES.add(tokenizer.nextToken());
      }


      populateProvenanceCache();
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
  }

  public synchronized void updateVariable(String variableName, String variableValue) {
    updateVariableInternal(variableName, variableValue);
    refreshCache();
  }

  public synchronized void updateVariable(String variableName, Long variableValue) {
    updateVariableInternal(variableName, variableValue.toString());
    refreshCache();
  }

  public synchronized void updateVariables(Map<String, String> variablesToUpdate) {
    for (Map.Entry<String, String> entry : variablesToUpdate.entrySet()) {
      updateVariableInternal(entry.getKey(), entry.getValue());
    }
    refreshCache();
  }

  /**
   * This method will invalidate the cache of variables. The next call to read a variable after invalidateCache() will
   * trigger a read of all variables from the database.
   */
  public synchronized void invalidateCache() {
    cached = false;
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

  /**
   * Default Directory locations
   */
  public static final String PRIVATE_DIRS = "/private_dirs/";

  public static final String SERVING_DIRS = "/serving/";

  public static final String TENSORBOARD_DIRS = "/tensorboard/";

  private String SPARK_DIR = "/srv/hops/spark";
  public static final String SPARK_EXAMPLES_DIR = "/examples/jars";

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
  public static final String SPARK_YARN_APPMASTER_ENV = "spark.yarn.appMasterEnv.";
  public static final String SPARK_EXECUTOR_ENV = "spark.executorEnv.";

  //PySpark properties
  public static final String SPARK_APP_NAME_ENV = "spark.app.name";

  public static final String SPARK_YARN_IS_PYTHON_ENV = "spark.yarn.isPython";

  public static final String SPARK_PYSPARK_PYTHON = "PYSPARK_PYTHON";

  public static final String SPARK_PYSPARK_PYTHON_OPTION = "spark.pyspark.python";

  //TFSPARK properties
  public static final String SPARK_TF_GPUS_ENV = "spark.executor.gpus";
  public static final String SPARK_TENSORFLOW_APPLICATION = "spark.tensorflow.application";
  public static final String SPARK_TENSORFLOW_NUM_PS = "spark.tensorflow.num.ps";

  //Spark log4j and metrics properties
  public static final String JOB_LOG4J_CONFIG = "log4j.configuration";
  public static final String JOB_LOG4J_PROPERTIES = "log4j.properties";
  //If the value of this property changes, it must be changed in spark-chef log4j.properties as well
  public static final String LOGSTASH_JOB_INFO = "hopsworks.logstash.job.info";
  public static final String SPARK_METRICS_PROPERTIES = "metrics.properties";

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

  //Hive config
  public static final String HIVE_SITE = "hive-site.xml";

  public synchronized String getSparkDir() {
    checkCache();
    return SPARK_DIR;
  }

  public synchronized String getSparkConfDir() {
    return getSparkDir() + "/conf";
  }

  public synchronized String getSparkExampleDir() {
    checkCache();
    return SPARK_EXAMPLES_DIR;
  }

  private final String SPARK_CONF_FILE = "/spark-defaults.conf";

  public synchronized String getSparkConfFile() {
    return getSparkConfDir() + SPARK_CONF_FILE;
  }

  // "/tmp" by default
  private String STAGING_DIR = "/srv/hops/domains/domain1/staging";

  public synchronized String getStagingDir() {
    checkCache();
    return STAGING_DIR;
  }

  private final String FLINK_CONF_DIR = "conf";
  private String FLINK_DIR = "/srv/hops/flink";

  public synchronized String getFlinkDir() {
    checkCache();
    return FLINK_DIR;
  }

  public String getFlinkConfDir() {
    String flinkDir = getFlinkDir();
    return flinkDir + File.separator + FLINK_CONF_DIR + File.separator;
  }

  private final String FLINK_CONF_FILE = "flink-conf.yaml";

  public String getFlinkConfFile() {
    return getFlinkConfDir() + File.separator + FLINK_CONF_FILE;
  }

  private String MYSQL_DIR = "/usr/local/mysql";
  public synchronized String getMySqlDir() {
    checkCache();
    return MYSQL_DIR;
  }

  private String MYSQL_USER = "mysql";
  public synchronized String getMysqlUser() {
    checkCache();
    return MYSQL_USER;
  }

  private String NDB_DIR = "/var/lib/mysql-cluster";
  public synchronized String getNdbDir() {
    checkCache();
    return NDB_DIR;
  }

  private String AIRFLOW_DIR = "/srv/hops/airflow";
  public synchronized String getAirflowDir() {
    checkCache();
    return AIRFLOW_DIR;
  }

  private String AIRFLOW_USER = "airflow";
  public synchronized String getAirflowUser() {
    checkCache();
    return AIRFLOW_USER;
  }

  private String HADOOP_DIR = "/srv/hops/hadoop";

  // This returns the unversioned base installation directory for hops-hadoop
  // For example, "/srv/hops/hadoop" - it does not return "/srv/hops/hadoop-2.8.2"
  public synchronized String getHadoopSymbolicLinkDir() {
    checkCache();
    return HADOOP_DIR;
  }

  public synchronized String getHadoopVersionedDir() {
    checkCache();
    return HADOOP_DIR + "-" + getHadoopVersion();
  }

  private String HIVE_SERVER_HOSTNAME = "127.0.0.1:9085";
  private String HIVE_SERVER_HOSTNAME_EXT = "127.0.0.1:9084";

  public synchronized String getHiveServerHostName(boolean ext) {
    checkCache();
    if (ext) {
      return HIVE_SERVER_HOSTNAME_EXT;
    }
    return HIVE_SERVER_HOSTNAME;
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

  private String HIVE_LLAP_SLIDER_DIR = "/home/hive/.slider";

  public synchronized String getHiveLlapSliderDir() {
    checkCache();
    return HIVE_LLAP_SLIDER_DIR;
  }

  private String HIVE_LLAP_LOCAL_FS_DIR = "/srv/hops/apache-hive/bin/llap";

  public synchronized String getHiveLlapLocalDir() {
    checkCache();
    return HIVE_LLAP_LOCAL_FS_DIR;
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

  private String HIVE_DB_DEFAULT_QUOTA = "50000";

  public synchronized Long getHiveDbDefaultQuota() {
    checkCache();
    return Long.parseLong(HIVE_DB_DEFAULT_QUOTA);
  }

  private String HOPSWORKS_IP = "127.0.0.1";

  public synchronized String getHopsworksIp() {
    checkCache();
    return HOPSWORKS_IP;
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

  //User under which yarn is run
  private String YARN_SUPERUSER = "rmyarn";

  public synchronized String getYarnSuperUser() {
    checkCache();
    return YARN_SUPERUSER;
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

  public String JAVA_HOME = "/usr/lib/jvm/default-java";

  public synchronized String getJavaHome() {
    checkCache();
    return JAVA_HOME;
  }

  private String FLINK_USER = "flink";

  public synchronized String getFlinkUser() {
    checkCache();
    return FLINK_USER;
  }

  private Integer YARN_DEFAULT_QUOTA = 60000;

  public synchronized Integer getYarnDefaultQuota() {
    checkCache();
    return YARN_DEFAULT_QUOTA;
  }

  private String YARN_WEB_UI_IP = "127.0.0.1";
  private int YARN_WEB_UI_PORT = 8088;

  public synchronized String getYarnWebUIAddress() {
    checkCache();
    return YARN_WEB_UI_IP + ":" + YARN_WEB_UI_PORT;
  }

  private String HDFS_DEFAULT_QUOTA_MBs = "200000";

  public synchronized long getHdfsDefaultQuotaInMBs() {
    checkCache();
    return Long.parseLong(HDFS_DEFAULT_QUOTA_MBs);
  }

  private String AIRFLOW_WEB_UI_IP = "127.0.0.1";
  private int AIRFLOW_WEB_UI_PORT = 12358;

  public synchronized String getAirflowWebUIAddress() {
    checkCache();
    return AIRFLOW_WEB_UI_IP + ":" + AIRFLOW_WEB_UI_PORT + "/hopsworks-api/airflow";
  }

  private String MAX_NUM_PROJ_PER_USER = "5";

  public synchronized Integer getMaxNumProjPerUser() {
    checkCache();
    int num = 5;
    try {
      num = Integer.parseInt(MAX_NUM_PROJ_PER_USER);
    } catch (NumberFormatException ex) {
      // should print to log here
    }
    return num;
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

  public synchronized String getYarnConfDir() {
    return getHadoopConfDir();
  }

  public String getYarnConfDir(String hadoopDir) {
    return hadoopConfDir(hadoopDir);
  }

  public String getHopsLeaderElectionJarPath() {
    return getHadoopSymbolicLinkDir() + "/share/hadoop/hdfs/lib/hops-leader-election-" + getHadoopVersion() + ".jar";
  }

  //Default configuration file names
  public static final String DEFAULT_YARN_CONFFILE_NAME = "yarn-site.xml";
  public static final String DEFAULT_HADOOP_CONFFILE_NAME = "core-site.xml";
  private static final String DEFAULT_HDFS_CONFFILE_NAME = "hdfs-site.xml";
  public static final String DEFAULT_SPARK_CONFFILE_NAME = "spark-defaults.conf";

  //Environment variable keys
  //TODO: Check if ENV_KEY_YARN_CONF_DIR should be replaced with ENV_KEY_YARN_CONF
  private static final String ENV_KEY_YARN_CONF_DIR = "hdfs";
  public static final String ENV_KEY_HADOOP_CONF_DIR = "HADOOP_CONF_DIR";
  public static final String ENV_KEY_YARN_CONF = "YARN_CONF_DIR";
  public static final String ENV_KEY_SPARK_CONF_DIR = "SPARK_CONF_DIR";
  //YARN constants
  public static final int YARN_DEFAULT_APP_MASTER_MEMORY = 2048;
  public static final String YARN_DEFAULT_OUTPUT_PATH = "Logs/Yarn/";
  public static final String HADOOP_COMMON_HOME_KEY = "HADOOP_COMMON_HOME";
  public static final String HADOOP_HOME_KEY = "HADOOP_HOME";
  public static final String HADOOP_HDFS_HOME_KEY = "HADOOP_HDFS_HOME";
  public static final String HADOOP_YARN_HOME_KEY = "HADOOP_YARN_HOME";
  public static final String HADOOP_CONF_DIR_KEY = "HADOOP_CONF_DIR";

  private static final String HADOOP_CONF_RELATIVE_DIR = "etc/hadoop";
  public static final String SPARK_CONF_RELATIVE_DIR = "conf";
  public static final String YARN_CONF_RELATIVE_DIR = HADOOP_CONF_RELATIVE_DIR;

  //Spark constants
  // Subdirectory where Spark libraries will be placed.
  public static final String SPARK_LOCALIZED_LIB_DIR = "__spark_libs__";
  public static final String SPARK_LOCALIZED_CONF_DIR = "__spark_conf__";
  public static final String SPARK_LOCALIZED_PYTHON_DIR = "__pyfiles__";
  public static final String SPARK_LOCRSC_APP_JAR = "__app__.jar";

  public static final String HOPS_EXPERIMENTS_DATASET = "Experiments";
  public static final String HOPS_MODELS_DATASET = "Models";

  public static final String HOPS_TOUR_DATASET = "TestJob";
  public static final String HOPS_DL_TOUR_DATASET = "TourData";
  public static final String HOPS_TOUR_DATASET_JUPYTER = "Jupyter";
  // Distribution-defined classpath to add to processes
  public static final String SPARK_AM_MAIN
      = "org.apache.spark.deploy.yarn.ApplicationMaster";
  public static final String SPARK_DEFAULT_OUTPUT_PATH = "Logs/Spark/";
  public static final String SPARK_CONFIG_FILE = "conf/spark-defaults.conf";
  public static final String SPARK_BLACKLISTED_PROPS
      = "conf/spark-blacklisted-properties.txt";
  public static final int SPARK_MIN_EXECS = 1;
  public static final int SPARK_MAX_EXECS = 2;

  //Flink constants
  public static final String FLINK_DEFAULT_OUTPUT_PATH = "Logs/Flink/";
  public static final String FLINK_LOCRSC_FLINK_JAR = "flink.jar";
  public static final int FLINK_APP_MASTER_MEMORY = 768;

  public static final String HOPS_DEEP_LEARNING_TOUR_DATA = "tensorflow_demo/data";
  public static final String HOPS_DEEP_LEARNING_TOUR_NOTEBOOKS = "tensorflow_demo/notebooks";
  public static final String FLINK_AM_MAIN = "org.apache.flink.yarn.ApplicationMaster";
  public static final String FLINK_ENV_JAVA_OPTS = "env.java.opts";
  public static final String FLINK_ENV_JAVA_OPTS_JOBMANAGER = "env.java.opts.jobmanager";
  public static final String FLINK_ENV_JAVA_OPTS_TASKMANAGER = "env.java.opts.taskmanager";
  public static final String FLINK_STATE_CHECKPOINTS_DIR = "state.checkpoints.dir";
  public static final String FLINK_WEB_UPLOAD_DIR = "web.upload.dir";
  
  

  //Featurestore constants
  public static final String HOPS_FEATURESTORE_TOUR_DATA = "featurestore_demo";
  public static final String HOPS_FEATURESTORE_TOUR_JOB_CLASS = "io.hops.examples.featurestore_tour.Main";
  public static final String HOPS_FEATURESTORE_TOUR_JOB_NAME = "featurestore_tour_job";
  public static final String HOPS_FEATURESTORE_TOUR_JOB_INPUT_PARAM = "--input ";


  //Serving constants
  public static final String INFERENCE_SCHEMANAME = "inferenceschema";
  public static final int INFERENCE_SCHEMAVERSION = 2;

  //Kafka constants
  public static final String PROJECT_COMPATIBILITY_SUBJECT = "projectcompatibility";
  
  public static final Set<String> KAFKA_SUBJECT_BLACKLIST =
    new HashSet<>(Arrays.asList(INFERENCE_SCHEMANAME, PROJECT_COMPATIBILITY_SUBJECT));

  public synchronized String getLocalFlinkJarPath() {
    return getFlinkDir() + "/flink.jar";
  }

  public synchronized String getFlinkJarPath() {
    return hdfsFlinkJarPath(getFlinkUser());
  }

  private String hdfsFlinkJarPath(String flinkUser) {
    return "hdfs:///user/" + flinkUser + "/flink.jar";
  }

  public synchronized String getFlinkDefaultClasspath() {
    return flinkDefaultClasspath(getFlinkDir());
  }

  private String flinkDefaultClasspath(String flinkDir) {
    return flinkDir + "/lib/*";
  }

  public String getFlinkDefaultClasspath(String flinkDir) {
    return flinkDefaultClasspath(flinkDir);
  }

  public String getSparkLog4JPath() {
    return "hdfs:///user/" + getSparkUser() + "/log4j.properties";
  }

  public String getSparkMetricsPath() {
    return "hdfs:///user/" + getSparkUser() + "/metrics.properties";
  }

  public synchronized String getSparkDefaultClasspath() {
    return sparkDefaultClasspath(getSparkDir());
  }

  private String sparkDefaultClasspath(String sparkDir) {
    return sparkDir + "/lib/*";
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

  private static String DEFAULT_RESERVED_PROJECT_NAMES = "python27,python36,python37,python38,python39,hops-system," +
    "hopsworks,information_schema,airflow,glassfish_timers,grafana,hops,metastore,mysql,ndbinfo,performance_schema," +
    "sqoop,sys";
  private Set<String> RESERVED_PROJECT_NAMES;
  
  public synchronized Set<String> getReservedProjectNames() {
    checkCache();
    return RESERVED_PROJECT_NAMES != null ? RESERVED_PROJECT_NAMES : new HashSet<>();
  }
  
  // Elasticsearch
  ElasticSettings ELASTIC_SETTINGS;
  
  public synchronized List<String> getElasticIps(){
    checkCache();
    return ELASTIC_SETTINGS.getElasticIps();
  }
  
  public synchronized int getElasticPort() {
    checkCache();
    return ELASTIC_SETTINGS.getElasticPort();
  }
  
  public synchronized int getElasticRESTPort() {
    checkCache();
    return ELASTIC_SETTINGS.getElasticRESTPort();
  }
  
  public synchronized String getElasticEndpoint() {
    checkCache();
    return ELASTIC_SETTINGS.getElasticEndpoint();
  }

  public synchronized String getElasticRESTEndpoint() {
    checkCache();
    return ELASTIC_SETTINGS.getElasticRESTEndpoint();
  }
  
  public synchronized boolean isElasticOpenDistroSecurityEnabled() {
    checkCache();
    return ELASTIC_SETTINGS.isOpenDistroSecurityEnabled();
  }
  
  public synchronized boolean isElasticHTTPSEnabled() {
    checkCache();
    return ELASTIC_SETTINGS.isHttpsEnabled();
  }
  
  public synchronized String getElasticAdminUser() {
    checkCache();
    return ELASTIC_SETTINGS.getAdminUser();
  }
  
  public synchronized String getElasticAdminPassword() {
    checkCache();
    return ELASTIC_SETTINGS.getAdminPassword();
  }
  
  public synchronized boolean isElasticJWTEnabled() {
    checkCache();
    return ELASTIC_SETTINGS.isElasticJWTEnabled();
  }
  
  public synchronized String getElasticJwtUrlParameter() {
    checkCache();
    return ELASTIC_SETTINGS.getElasticJWTURLParameter();
  }
  
  public synchronized long getElasicJwtExpMs() {
    checkCache();
    return ELASTIC_SETTINGS.getElasticJWTExpMs();
  }
  
  public synchronized Integer getElasticDefaultScrollPageSize() {
    checkCache();
    return ELASTIC_SETTINGS.getDefaultScrollPageSize();
  }
  
  public synchronized Integer getElasticMaxScrollPageSize() {
    checkCache();
    return ELASTIC_SETTINGS.getMaxScrollPageSize();
  }

  private long ELASTIC_LOGS_INDEX_EXPIRATION = 7 * 24 * 60 * 60 * 1000;

  public synchronized long getElasticLogsIndexExpiration() {
    checkCache();
    return ELASTIC_LOGS_INDEX_EXPIRATION;
  }

  private static final int JOB_LOGS_EXPIRATION = 604800;

  /**
   * TTL for job logs in elasticsearch, in seconds.
   *
   * @return
   */
  public int getJobLogsExpiration() {
    return JOB_LOGS_EXPIRATION;
  }

  private static final long JOB_LOGS_DISPLAY_SIZE = 1000000;

  public long getJobLogsDisplaySize() {
    return JOB_LOGS_DISPLAY_SIZE;
  }

  private static final String JOB_LOGS_ID_FIELD = "jobid";

  public String getJobLogsIdField() {
    return JOB_LOGS_ID_FIELD;
  }

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

  private String CERTIFICATE_USER_VALID_DAYS = "12";

  public synchronized String getCertificateUserValidDays() {
    checkCache();
    return CERTIFICATE_USER_VALID_DAYS;
  }

  // Spark
  private String SPARK_HISTORY_SERVER_IP = "127.0.0.1";

  public synchronized String getSparkHistoryServerIp() {
    checkCache();
    return SPARK_HISTORY_SERVER_IP + ":18080";
  }

  // Oozie
  private String OOZIE_IP = "127.0.0.1";

  public synchronized String getOozieIp() {
    checkCache();
    return OOZIE_IP;
  }

  // MapReduce Job History Server
  private String JHS_IP = "127.0.0.1";

  public synchronized String getJhsIp() {
    checkCache();
    return JHS_IP;
  }

  // Resource Manager for YARN
  private String RM_IP = "127.0.0.1";

  public synchronized String getRmIp() {
    checkCache();
    return RM_IP;
  }

  // Resource Manager Port
  private int RM_PORT = 8088;

  public synchronized Integer getRmPort() {
    checkCache();
    return RM_PORT;
  }

  private String LOGSTASH_IP = "127.0.0.1";

  public synchronized String getLogstashIp() {
    checkCache();
    return LOGSTASH_IP;
  }

  // Resource Manager Port
  private int LOGSTASH_PORT = 8088;

  public synchronized Integer getLogstashPort() {
    checkCache();
    return LOGSTASH_PORT;
  }

  private int LOGSTASH_PORT_TF_SERVING = 5045;

  public synchronized Integer getLogstashPortTfServing() {
    checkCache();
    return LOGSTASH_PORT_TF_SERVING;
  }

  private int LOGSTASH_PORT_SKLEARN_SERVING = 5047;

  public synchronized Integer getLogstashPortSkLearnServing() {
    checkCache();
    return LOGSTASH_PORT_SKLEARN_SERVING;
  }

  // Livy Server`
  private String LIVY_IP = "127.0.0.1";
  private final String LIVY_YARN_MODE = "yarn";

  public synchronized String getLivyIp() {
    checkCache();
    return LIVY_IP;
  }

  public synchronized String getLivyUrl() {
    return "http://" + getLivyIp() + ":8998";
  }

  public synchronized String getLivyYarnMode() {
    checkCache();
    return LIVY_YARN_MODE;
  }

  private static final int ZK_PORT = 2181;

  // Kibana
  public static final String KIBANA_INDEX_PREFIX = ".kibana";
  
  private String KIBANA_IP = "10.0.2.15";
  private static final int KIBANA_PORT = 5601;

  public synchronized String getKibanaUri() {
    checkCache();
    return (KIBANA_HTTPS_ENABELED ? "https" : "http") + "://" + KIBANA_IP +
        ":" + KIBANA_PORT;
  }
  
  public synchronized String getKibanaAppUri() {
    checkCache();
    return "/hopsworks-api/kibana/app/kibana?";
  }
  
  public synchronized String getKibanaAppUri(String jwtToken) {
    checkCache();
    return  getKibanaAppUri() + ELASTIC_SETTINGS.getElasticJWTURLParameter()
        + "=" + jwtToken + "&";
  }
  
  // Zookeeper
  private String ZK_IP = "10.0.2.15";

  public synchronized String getZkConnectStr() {
    checkCache();
    return ZK_IP + ":" + ZK_PORT;
  }

  private String ZK_USER = "zk";

  public synchronized String getZkUser() {
    checkCache();
    return ZK_USER;
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

  private String JUPYTER_USER = "jupyter";

  public synchronized String getJupyterUser() {
    checkCache();
    return JUPYTER_USER;
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

  private String ANACONDA_USER = "anaconda";

  public synchronized String getAnacondaUser() {
    checkCache();
    return ANACONDA_USER;
  }

  private String ANACONDA_DIR = "/srv/hops/anaconda/anaconda";

  public synchronized String getAnacondaDir() {
    checkCache();
    return ANACONDA_DIR;
  }

  private String CUDA_DIR = "/usr/local/cuda";

  public synchronized String getCudaDir() {
    checkCache();
    return CUDA_DIR;
  }

  /**
   * Constructs the path to the project environment in Anaconda
   *
   * @param project project
   * @return conda dir
   */
  public String getAnacondaProjectDir(Project project) {
    String condaEnv = projectUtils.getCurrentCondaEnvironment(project);
    return getAnacondaDir() + File.separator + "envs" + File.separator + condaEnv;
  }
  
  //TODO(Theofilos): Used by Flink. Will be removed as part of refactoring *YarnRunnerBuilders.
  public String getCurrentCondaEnvironment(Project project) {
    return projectUtils.getCurrentCondaEnvironment(project);
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

//  private String CONDA_CHANNEL_URL = "https://repo.continuum.io/pkgs/free/linux-64/";
  private String CONDA_CHANNEL_URL = "default";

  public synchronized String getCondaChannelUrl() {
    checkCache();
    return CONDA_CHANNEL_URL;
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

  private String HOPSWORKS_REST_ENDPOINT = "hopsworks0:8181";

  public synchronized String getRestEndpoint() {
    checkCache();

    if (isLocalHost()) {
      return "https://localhost:" +
        HOPSWORKS_REST_ENDPOINT.substring(HOPSWORKS_REST_ENDPOINT.lastIndexOf(":") + 1);
    }

    return "https://" + HOPSWORKS_REST_ENDPOINT;
  }

  private RESTLogLevel HOPSWORKS_REST_LOG_LEVEL = RESTLogLevel.PROD;

  public synchronized RESTLogLevel getHopsworksRESTLogLevel() {
    checkCache();
    return HOPSWORKS_REST_LOG_LEVEL;
  }

  private String SUPPORT_EMAIL_ADDR = "support@hops.io";

  public synchronized String getSupportEmailAddr() {
    checkCache();
    return SUPPORT_EMAIL_ADDR;
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

  private String ZK_DIR = "/srv/zookeeper";

  public synchronized String getZkDir() {
    checkCache();
    return ZK_DIR;
  }

  // Dr Elephant
  private String DRELEPHANT_IP = "127.0.0.1";
  private String DRELEPHANT_DB = "hopsworks";
  private int DRELEPHANT_PORT = 11000;

  public synchronized String getDrElephantUrl() {
    checkCache();
    return "http://" + DRELEPHANT_IP + ":" + DRELEPHANT_PORT;
  }

  public synchronized String getDrElephantDb() {
    checkCache();
    return DRELEPHANT_DB;
  }

  private String CLUSTER_CERT = "asdasxasx8as6dx8a7sx7asdta8dtasxa8";

  public synchronized String getCLUSTER_CERT() {
    checkCache();
    return CLUSTER_CERT;
  }

  // Hopsworks
  public static final Charset ENCODING = StandardCharsets.UTF_8;
  public static final String HOPS_USERS_HOMEDIR = "/home/";
  public static final String HOPS_USERNAME_SEPARATOR = "__";

  public static final String UNZIP_FILES_SCRIPTNAME = "unzip-hdfs-files.sh";
  public static final int USERNAME_LEN = 8;
  public static final int MAX_USERNAME_SUFFIX = 99;
  public static final int MAX_RETRIES = 500;
  public static final String META_NAME_FIELD = "name";
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
  public static final String META_DATA_FIELDS = META_DATA_NESTED_FIELD + ".*";

  //Filename conventions
  public static final String FILENAME_DISALLOWED_CHARS = " /\\?*:|'\"<>%()&;#öäåÖÅÄàáéèâîïüÜ@${}[]+~^$`";
  public static final String SUBDIR_DISALLOWED_CHARS = "/\\?*:|'\"<>%()&;#öäåÖÅÄàáéèâîïüÜ@${}[]+~^$`";
  public static final String SHARED_FILE_SEPARATOR = "::";
  public static final String DOUBLE_UNDERSCORE = "__";

  // Authentication Constants
  // POSIX compliant usernake length
  public static final int USERNAME_LENGTH = 8;

  // Strating user id from 1000 to create a POSIX compliant username: meb1000
  public static final int STARTING_USER = 1000;
  public static final int PASSWORD_MIN_LENGTH = 6;

  // POSIX compliant usernake length
  public static final int ACCOUNT_VALIDATION_TRIES = 5;

  // Issuer of the QrCode
  public static final String ISSUER = "hops.io";

  // For padding when password field is empty: 6 chars
  public static final String MOBILE_OTP_PADDING = "@@@@@@";

  // when user is loged in 1 otherwise 0
  public static final int IS_ONLINE = 1;
  public static final int IS_OFFLINE = 0;

  public static final int ALLOWED_FALSE_LOGINS = 5;
  public static final int ALLOWED_AGENT_FALSE_LOGINS = 20;

  //hopsworks user prefix username prefix
  public static final String USERNAME_PREFIX = "meb";

  public static final String KEYSTORE_SUFFIX = "__kstore.jks";
  public static final String TRUSTSTORE_SUFFIX = "__tstore.jks";
  public static final String CERT_PASS_SUFFIX = "__cert.key";
  public static final String K_CERTIFICATE = "k_certificate";
  public static final String T_CERTIFICATE = "t_certificate";
  private static final String CA_TRUSTSTORE_NAME = "cacerts.jks";
  public static final String DOMAIN_CA_TRUSTSTORE_PEM = "cacerts.pem";
  public static final String DOMAIN_CA_TRUSTSTORE = "cacerts.jks";
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
  public static final String KAFKA_JOB_TOPICS_PROPERTY = "hopsworks.kafka.job.topics";
  public static final String SERVER_TRUSTSTORE_PROPERTY = "server.truststore";
  public static final String KAFKA_CONSUMER_GROUPS = "hopsworks.kafka.consumergroups";
  public static final String HOPSWORKS_REST_ENDPOINT_PROPERTY = "hopsworks.restendpoint";
  public static final String HOPSUTIL_INSECURE_PROPERTY = "hopsutil.insecure";
  public static final String HOPSWORKS_ELASTIC_ENDPOINT_PROPERTY = "hopsworks.elastic.endpoint";
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
  public static final String FILE_PREVIEW_MODE_TAIL = "tail";

  //Elastic log index pattern
  public static final String ELASTIC_LOGS_INDEX = "logs";
  public static final String ELASTIC_BEAMJOBSERVER = "beamjobserver";
  public static final String ELASTIC_BEAMSDKWORKER = "beamsdkworker";
  public static final String ELASTIC_LOGS_INDEX_PATTERN = "_" + Settings.ELASTIC_LOGS_INDEX + "-*";
  public static final String ELASTIC_SERVING_INDEX = "serving";
  public static final String ELASTIC_KAGENT_INDEX = "kagent";
  public static final String ELASTIC_SERVING_INDEX_PATTERN = "_" + ELASTIC_SERVING_INDEX + "-*";
  public static final String ELASTIC_KAGENT_INDEX_PATTERN = "_" + ELASTIC_KAGENT_INDEX + "-*";

  public static final String ELASTIC_BEAMJOBSERVER_INDEX_PATTERN =
    "_" + Settings.ELASTIC_BEAMJOBSERVER + "-*";
  public static final String ELASTIC_BEAMSDKWORKER_INDEX_PATTERN =
    "_" + Settings.ELASTIC_BEAMSDKWORKER + "-*";
  public static final String ELASTIC_LOG_INDEX_REGEX = ".*_" + ELASTIC_LOGS_INDEX + "-\\d{4}.\\d{2}.\\d{2}";
  public static final String ELASTIC_SERVING_INDEX_REGEX = ".*_" + ELASTIC_SERVING_INDEX+ "-\\d{4}.\\d{2}.\\d{2}";
  public static final String ELASTIC_KAGENT_INDEX_REGEX = ".*_" + ELASTIC_KAGENT_INDEX + "-\\d{4}.\\d{2}.\\d{2}";
  public static final String ELASTIC_BEAMJOBSERVER_INDEX_REGEX =
    ".*_" + ELASTIC_BEAMJOBSERVER + "-\\d{4}.\\d{2}.\\d{2}";
  public static final String ELASTIC_BEAMSDKWORKER_INDEX_REGEX =
    ".*_" + ELASTIC_BEAMSDKWORKER + "-\\d{4}.\\d{2}.\\d{2}";

  //Other Elastic indexes
  public static final String ELASTIC_INDEX_APP_PROVENANCE = "app_prov";
  
  public String getHopsworksTmpCertDir() {
    return Paths.get(getCertsDir(), "transient").toString();
  }

  public String getHdfsTmpCertDir() {
    return "/user/" + getHdfsSuperUser() + "/" + "kafkacerts";
  }

  public String getHopsworksTrueTempCertDir() {
    return "/tmp/usercerts/";
  }

  public String getGlassfishTrustStoreHdfs() {
    return "hdfs:///user/" + getSparkUser() + "/" + CA_TRUSTSTORE_NAME;
  }
  
  public String getGlassfishTrustStore() {
    return getHopsworksDomainDir() + File.separator + "config" + File.separator + CA_TRUSTSTORE_NAME;
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

  private String INFLUXDB_IP = "localhost";
  private String INFLUXDB_PORT = "8086";
  private String INFLUXDB_USER = "hopsworks";
  private String INFLUXDB_PW = "hopsworks";

  public synchronized String getInfluxDBAddress() {
    checkCache();
    return "http://" + INFLUXDB_IP + ":" + INFLUXDB_PORT;
  }

  public synchronized String getInfluxDBUser() {
    checkCache();
    return INFLUXDB_USER;
  }

  public synchronized String getInfluxDBPW() {
    checkCache();
    return INFLUXDB_PW;
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
    DATAVALIDATION("DataValidation",
        "Contains rules and results for Features validation");

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

  private static final String FEATURESTORE_IMPORT_PARENT_DIR = "featurestore_import";
  public static final String FEATURESTORE_IMPORT_CONF = "configurations";
  public static final String FEATURESTORE_IMPORT_JOB_NAME = "ft_import.py";

  public String getBaseFeaturestoreJobImportDir(Project project) {
    return Utils.getProjectPath(project.getName()) + Path.SEPARATOR +
        Settings.BaseDataset.RESOURCES.getName() + Path.SEPARATOR +
        Settings.FEATURESTORE_IMPORT_PARENT_DIR + Path.SEPARATOR;
  }

  public static final String REDSHIFT_JDBC_NAME = "RedshiftJDBC42-no-awssdk.jar";

  public synchronized String getFeaturestoreImportJobPath() {
    checkCache();
    return "hdfs:///user" + Path.SEPARATOR + getSparkUser() + Path.SEPARATOR + FEATURESTORE_IMPORT_JOB_NAME;
  }
  
  private static final String FEATURESTORE_TRAININGDATASET_JOB_PARENT_DIR = "featurestore-trainingdataset-job";
  public static final String FEATURESTORE_TRAININGDATASET_JOB_CONF = "configurations";
  public static final String FEATURESTORE_TRAININGDATASET_JOB_NAME = "ft_trainingdataset_job.py";
  public static final String FEATURESTORE_TRAININGDATASET_SQL_JOB_NAME = "ft_trainingdataset_sql_job.py";
  
  public String getBaseFeaturestoreTrainingDatasetJobDir(Project project) {
    return Utils.getProjectPath(project.getName()) + Settings.BaseDataset.RESOURCES.getName() + Path.SEPARATOR +
      Settings.FEATURESTORE_TRAININGDATASET_JOB_PARENT_DIR + Path.SEPARATOR;
  }
  
  public synchronized String getFeaturestoreTrainingDatasetJobPath(String sql_query) {
    checkCache();
    if (sql_query != null) {
      return "hdfs:///user" + Path.SEPARATOR + getSparkUser() + Path.SEPARATOR +
        FEATURESTORE_TRAININGDATASET_SQL_JOB_NAME;
    } else {
      return "hdfs:///user" + Path.SEPARATOR + getSparkUser() + Path.SEPARATOR + FEATURESTORE_TRAININGDATASET_JOB_NAME;
    }
  }
  
  public Settings() {
  }

  private String ALERT_EMAIL_ADDRS = "";

  public synchronized String getAlertEmailAddrs() {
    checkCache();
    return ALERT_EMAIL_ADDRS;
  }

  /**
   * Get the variable value with the given name.
   *
   * @param id
   * @return The user with given email, or null if no such user exists.
   */
  public Variables findById(String id) {
    try {
      return em.createNamedQuery("Variables.findById", Variables.class
      ).setParameter("id", id).getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   * Get all variables from the database.
   *
   * @return List with all the variables
   */
  public List<Variables> getAllVariables() {
    TypedQuery<Variables> query = em.createNamedQuery("Variables.findAll", Variables.class);

    try {
      return query.getResultList();
    } catch (EntityNotFoundException ex) {
      LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
    } catch (NoResultException ex) {
    }
    return new ArrayList<>();
  }

  /**
   * Update a variable in the database.
   *
   * @param variableName name
   * @param variableValue value
   */
  private void updateVariableInternal(String variableName, String variableValue) {
    Variables var = findById(variableName);
    if (var == null) {
      throw new NoResultException("Variable <" + variableName + "> does not exist in the database");
    }
    if (!var.getValue().equals(variableValue)) {
      var.setValue(variableValue);
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

      File hdfsConf = new File(confPath + "/"
          + Settings.DEFAULT_HDFS_CONFFILE_NAME);
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

  // For performance reasons, we have an in-memory cache of files being unzipped
  // Lazily remove them from the cache, when we check the FS and they aren't there.
  private Set<String> zippingFiles = new HashSet<>();

  public synchronized void addZippingState(String hdfsPath) {
    zippingFiles.add(hdfsPath);
  }
  private Set<String> unzippingFiles = new HashSet<>();

  public synchronized void addUnzippingState(String hdfsPath) {
    unzippingFiles.add(hdfsPath);
  }

  public synchronized String getZipState(String hdfsPath) {

    boolean zipOperation = false;
    boolean unzipOperation = false;

    if (zippingFiles.contains(hdfsPath)) {
      zipOperation = true;
    } else if (unzippingFiles.contains(hdfsPath)) {
      unzipOperation = true;
    } else {
      return "NONE";
    }

    String hashedPath = DigestUtils.sha256Hex(hdfsPath);
    String fsmPath = getStagingDir() + File.separator + hashedPath + "/fsm.txt";
    String state = "NOT_FOUND";
    try {
      state = new String(java.nio.file.Files.readAllBytes(Paths.get(fsmPath)));
      state = state.trim();
    } catch (IOException ex) {

      String stagingDir = getStagingDir() + File.separator + hashedPath;
      if (!java.nio.file.Files.exists(Paths.get(stagingDir))) {
        state = "NONE";
        // lazily remove the file, probably because it has finished zipping/unzipping
        if (zipOperation) {
          zippingFiles.remove(hdfsPath);
        } else if (unzipOperation) {
          unzippingFiles.remove(hdfsPath);
        }
      }
    }
    // If a terminal state has been reached, removed the entry and the file.
    if (state.isEmpty() || state.compareTo("FAILED") == 0 || state.compareTo("SUCCESS") == 0) {
      try {
        if (zipOperation) {
          zippingFiles.remove(hdfsPath);
        } else if (unzipOperation) {
          unzippingFiles.remove(hdfsPath);
        }
        java.nio.file.Files.deleteIfExists(Paths.get(fsmPath));
      } catch (IOException ex) {
        Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    if (state.isEmpty()) {
      state = "NOT_FOUND";
    }

    return state;
  }

  private boolean PYTHON_KERNEL = true;

  public synchronized boolean isPythonKernelEnabled() {
    checkCache();
    return PYTHON_KERNEL;
  }

  private String PYPI_REST_ENDPOINT = "https://pypi.org/pypi/{package}/json";

  public synchronized String getPyPiRESTEndpoint() {
    checkCache();
    return PYPI_REST_ENDPOINT;
  }

  private String HOPS_EXAMPLES_VERSION = "0.3.0";

  public synchronized String getHopsExamplesSparkFilename() {
    checkCache();
    return "hops-examples-spark-" + HOPS_EXAMPLES_VERSION + ".jar";
  }

  public synchronized String getHopsExamplesFeaturestoreTourFilename() {
    checkCache();
    return "hops-examples-featurestore-tour-" + HOPS_EXAMPLES_VERSION + ".jar";
  }

  public synchronized String getHopsExamplesFeaturestoreUtil4JFilename() {
    checkCache();
    return "hops-examples-featurestore-util4j-" + HOPS_EXAMPLES_VERSION + ".jar";
  }

  public synchronized String getHopsExamplesFeaturestoreUtilPythonFilename() {
    checkCache();
    return "featurestore_util.py";
  }

  private String VERIFICATION_PATH = "/hopsworks-admin/security/validate_account.xhtml";

  public synchronized String getEmailVerificationEndpoint() {
    checkCache();
    return VERIFICATION_PATH;
  }

  public synchronized String getVerificationEndpoint() {
    checkCache();
    return getRestEndpoint() + "/" + VERIFICATION_PATH;
  }

  //Dela START
  private static final String VARIABLE_HOPSSITE_BASE_URI = "hops_site_endpoint";
  private static final String VARIABLE_HOPSSITE_BASE_URI_HOST = "hops_site_host";
  private static final String VARIABLE_CLUSTER_CERT = "hopsworks_certificate";
  private static final String VARIABLE_DELA_ENABLED = "dela_enabled";
  private static final String VARIABLE_DELA_CLIENT_TYPE = "dela_client_type";
  private static final String VARIABLE_HOPSSITE_HEARTBEAT_INTERVAL = "hopssite_heartbeat_interval";

  private static final String VARIABLE_DELA_CLUSTER_ID = "cluster_id";
  private static final String VARIABLE_DELA_CLUSTER_IP = "dela_cluster_ip";
  private static final String VARIABLE_DELA_CLUSTER_HTTP_PORT = "dela_cluster_http_port";
  private static final String VARIABLE_DELA_PUBLIC_HOPSWORKS_PORT = "dela_hopsworks_public_port";
  private static final String VARIABLE_PUBLIC_HTTPS_PORT = "public_https_port";
  private static final String VARIABLE_DELA_SEARCH_ENDPOINT = "dela_search_endpoint";
  private static final String VARIABLE_DELA_TRANSFER_ENDPOINT = "dela_transfer_endpoint";

  public static final Level DELA_DEBUG = Level.INFO;
  private String HOPSSITE_HOST = "hops.site";
  private String HOPSSITE = "http://hops.site:5081/hops-site/api";
  private Boolean DELA_ENABLED = false; // set to false if not found in variables table
  private DelaClientType DELA_CLIENT_TYPE = DelaClientType.FULL_CLIENT;

  private long HOPSSITE_HEARTBEAT_RETRY = 10 * 1000l; //10s
  private long HOPSSITE_HEARTBEAT_INTERVAL = 10 * 60 * 1000l;//10min

  private String DELA_TRANSFER_IP = "localhost";
  private String DELA_TRANSFER_HTTP_PORT = "42000";
  private String DELA_PUBLIC_HOPSWORK_PORT = "8080";
  private String PUBLIC_HTTPS_PORT = "8181";

  //set on registration after Dela is contacted to detect public port
  private String DELA_SEARCH_ENDPOINT = "";
  private String DELA_TRANSFER_ENDPOINT = "";
  //set on cluster registration
  private String DELA_CLUSTER_ID = null;
  //
  private AddressJSON DELA_PUBLIC_ENDPOINT = null;
  //
  public static final String MANIFEST_FILE = "manifest.json";
  public static final String README_FILE = "README.md";

  private void populateDelaCache() {
    DELA_ENABLED = setBoolVar(VARIABLE_DELA_ENABLED, DELA_ENABLED);
    DELA_CLIENT_TYPE = DelaClientType.from(setVar(VARIABLE_DELA_CLIENT_TYPE, DELA_CLIENT_TYPE.type));
    HOPSSITE_CLUSTER_NAME = setVar(VARIABLE_HOPSSITE_CLUSTER_NAME, HOPSSITE_CLUSTER_NAME);
    HOPSSITE_CLUSTER_PSWD = setVar(VARIABLE_HOPSSITE_CLUSTER_PSWD, HOPSSITE_CLUSTER_PSWD);
    HOPSSITE_CLUSTER_PSWD_AUX = setVar(VARIABLE_HOPSSITE_CLUSTER_PSWD_AUX, HOPSSITE_CLUSTER_PSWD_AUX);
    HOPSSITE_HOST = setVar(VARIABLE_HOPSSITE_BASE_URI_HOST, HOPSSITE_HOST);
    HOPSSITE = setVar(VARIABLE_HOPSSITE_BASE_URI, HOPSSITE);
    HOPSSITE_HEARTBEAT_INTERVAL = setLongVar(VARIABLE_HOPSSITE_HEARTBEAT_INTERVAL, HOPSSITE_HEARTBEAT_INTERVAL);

    DELA_TRANSFER_IP = setStrVar(VARIABLE_DELA_CLUSTER_IP, DELA_TRANSFER_IP);
    DELA_TRANSFER_HTTP_PORT = setStrVar(VARIABLE_DELA_CLUSTER_HTTP_PORT, DELA_TRANSFER_HTTP_PORT);
    DELA_SEARCH_ENDPOINT = setStrVar(VARIABLE_DELA_SEARCH_ENDPOINT, DELA_SEARCH_ENDPOINT);
    DELA_TRANSFER_ENDPOINT = setStrVar(VARIABLE_DELA_TRANSFER_ENDPOINT, DELA_TRANSFER_ENDPOINT);
    DELA_PUBLIC_HOPSWORK_PORT = setStrVar(VARIABLE_DELA_PUBLIC_HOPSWORKS_PORT, DELA_PUBLIC_HOPSWORK_PORT);
    PUBLIC_HTTPS_PORT = setStrVar(VARIABLE_PUBLIC_HTTPS_PORT, PUBLIC_HTTPS_PORT);
    DELA_CLUSTER_ID = setStrVar(VARIABLE_DELA_CLUSTER_ID, DELA_CLUSTER_ID);
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

  public synchronized Boolean isDelaEnabled() {
    checkCache();
    return DELA_ENABLED;
  }

  public synchronized DelaClientType getDelaClientType() {
    return DELA_CLIENT_TYPE;
  }

  public synchronized String getHOPSSITE_HOST() {
    checkCache();
    return HOPSSITE_HOST;
  }

  public synchronized String getHOPSSITE() {
    checkCache();
    return HOPSSITE;
  }

  public synchronized long getHOPSSITE_HEARTBEAT_RETRY() {
    checkCache();
    return HOPSSITE_HEARTBEAT_RETRY;
  }

  public synchronized long getHOPSSITE_HEARTBEAT_INTERVAL() {
    checkCache();
    return HOPSSITE_HEARTBEAT_INTERVAL;
  }

  public synchronized String getDELA_TRANSFER_IP() {
    checkCache();
    return DELA_TRANSFER_IP;
  }

  public synchronized String getDELA_TRANSFER_HTTP_PORT() {
    checkCache();
    return DELA_TRANSFER_HTTP_PORT;
  }

  public synchronized String getDELA_TRANSFER_HTTP_ENDPOINT() {
    checkCache();
    return "http://" + DELA_TRANSFER_IP + ":" + DELA_TRANSFER_HTTP_PORT + "/";
  }

  public synchronized String getDELA_HOPSWORKS_PORT() {
    checkCache();
    return DELA_PUBLIC_HOPSWORK_PORT;
  }

  public synchronized String getPUBLIC_HTTPS_PORT() {
    checkCache();
    return PUBLIC_HTTPS_PORT;
  }

  public synchronized AddressJSON getDELA_PUBLIC_ENDPOINT() {
    return DELA_PUBLIC_ENDPOINT;
  }

  public synchronized String getDELA_SEARCH_ENDPOINT() {
    checkCache();
    if (DELA_SEARCH_ENDPOINT != null) {
      return DELA_SEARCH_ENDPOINT;
    }
    Variables v = findById(DELA_SEARCH_ENDPOINT);
    if (v != null) {
      return v.getValue();
    }
    return null;
  }

  public synchronized String getDELA_TRANSFER_ENDPOINT() {
    checkCache();
    if (DELA_TRANSFER_ENDPOINT != null) {
      return DELA_TRANSFER_ENDPOINT;
    }
    Variables v = findById(DELA_TRANSFER_ENDPOINT);
    if (v != null) {
      return v.getValue();
    }
    return null;
  }

  public synchronized void setDELA_PUBLIC_ENDPOINT(AddressJSON endpoint) {
    DELA_PUBLIC_ENDPOINT = endpoint;

    String delaSearchEndpoint = "https://" + endpoint.getIp() + ":"
        + getPUBLIC_HTTPS_PORT() + "/hopsworks-api/api";
    String delaTransferEndpoint = endpoint.getIp() + ":" + endpoint.getPort() + "/" + endpoint.getId();

    if (getDELA_SEARCH_ENDPOINT() == null) {
      em.persist(new Variables(VARIABLE_DELA_SEARCH_ENDPOINT, delaSearchEndpoint));
    } else {
      em.merge(new Variables(VARIABLE_DELA_SEARCH_ENDPOINT, delaSearchEndpoint));
    }
    DELA_SEARCH_ENDPOINT = delaSearchEndpoint;

    if (getDELA_TRANSFER_ENDPOINT() == null) {
      em.persist(new Variables(VARIABLE_DELA_TRANSFER_ENDPOINT, delaTransferEndpoint));
    } else {
      em.merge(new Variables(VARIABLE_DELA_TRANSFER_ENDPOINT, delaTransferEndpoint));
    }
    DELA_TRANSFER_ENDPOINT = delaTransferEndpoint;
  }

  public synchronized void setDELA_CLUSTER_ID(String id) {
    if (getDELA_CLUSTER_ID() == null) {
      em.persist(new Variables(VARIABLE_DELA_CLUSTER_ID, id));
    } else {
      em.merge(new Variables(VARIABLE_DELA_CLUSTER_ID, id));
    }
    DELA_CLUSTER_ID = id;
  }

  public synchronized String getDELA_CLUSTER_ID() {
    checkCache();
    if (DELA_CLUSTER_ID != null) {
      return DELA_CLUSTER_ID;
    } else {
      Variables v = findById(VARIABLE_DELA_CLUSTER_ID);
      if (v != null) {
        return v.getValue();
      }
      return null;
    }
  }

  public synchronized String getDELA_DOMAIN() {
    if (DELA_PUBLIC_ENDPOINT != null) {
      return DELA_PUBLIC_ENDPOINT.getIp();
    }
    return null;
  }

  //************************************************CERTIFICATES********************************************************
  private static final String HOPS_SITE_CA_DIR = "hops-site-certs";
  private final static String HOPS_SITE_CERTFILE = "/pub.pem";
  private final static String HOPS_SITE_CA_CERTFILE = "/ca_pub.pem";
  private final static String HOPS_SITE_INTERMEDIATE_CERTFILE = "/intermediate_ca_pub.pem";
  private final static String HOPS_SITE_KEY_STORE = "/keystores/keystore.jks";
  private final static String HOPS_SITE_TRUST_STORE = "/keystores/truststore.jks";

  private static final String VARIABLE_HOPSSITE_CLUSTER_NAME = "hops_site_cluster_name";
  private static final String VARIABLE_HOPSSITE_CLUSTER_PSWD = "hops_site_cluster_pswd";
  private static final String VARIABLE_HOPSSITE_CLUSTER_PSWD_AUX = "hops_site_cluster_pswd_aux";

  private String HOPSSITE_CLUSTER_NAME = null;
  private String HOPSSITE_CLUSTER_PSWD = null;
  private String HOPSSITE_CLUSTER_PSWD_AUX = "1234";

  public synchronized Optional<String> getHopsSiteClusterName() {
    checkCache();
    return Optional.ofNullable(HOPSSITE_CLUSTER_NAME);
  }

  public synchronized void setHopsSiteClusterName(String clusterName) {
    if (getHopsSiteClusterName().isPresent()) {
      em.merge(new Variables(VARIABLE_HOPSSITE_CLUSTER_NAME, clusterName));
    } else {
      em.persist(new Variables(VARIABLE_HOPSSITE_CLUSTER_NAME, clusterName));
    }
    HOPSSITE_CLUSTER_NAME = clusterName;
  }

  public synchronized void deleteHopsSiteClusterName() {
    if (getHopsSiteClusterName().isPresent()) {
      Variables v = findById(VARIABLE_HOPSSITE_CLUSTER_NAME);
      em.remove(v);
      HOPSSITE_CLUSTER_NAME = null;
    }
  }

  public synchronized String getHopsSiteClusterPswdAux() {
    checkCache();
    return HOPSSITE_CLUSTER_PSWD_AUX;
  }

  public synchronized Optional<String> getHopsSiteClusterPswd() {
    checkCache();
    return Optional.ofNullable(HOPSSITE_CLUSTER_PSWD);
  }

  public synchronized void setHopsSiteClusterPswd(String pswd) {
    if (getHopsSiteClusterPswd().isPresent()) {
      em.merge(new Variables(VARIABLE_HOPSSITE_CLUSTER_PSWD, pswd));
    } else {
      em.persist(new Variables(VARIABLE_HOPSSITE_CLUSTER_PSWD, pswd));
    }
    HOPSSITE_CLUSTER_PSWD = pswd;
  }

  public synchronized String getHopsSiteCaDir() {
    return getCertsDir() + File.separator + HOPS_SITE_CA_DIR;
  }

  public synchronized String getHopsSiteCaScript() {
    return getSudoersDir() + File.separator + "ca-keystore.sh";
  }

  public synchronized String getHopsSiteCert() {
    return getHopsSiteCaDir() + HOPS_SITE_CERTFILE;
  }

  public synchronized String getHopsSiteCaCert() {
    return getHopsSiteCaDir() + HOPS_SITE_CA_CERTFILE;
  }

  public synchronized String getHopsSiteIntermediateCert() {
    return getHopsSiteCaDir() + HOPS_SITE_INTERMEDIATE_CERTFILE;
  }

  public synchronized String getHopsSiteKeyStorePath() {
    return getHopsSiteCaDir() + HOPS_SITE_KEY_STORE;
  }

  public synchronized String getHopsSiteTrustStorePath() {
    return getHopsSiteCaDir() + HOPS_SITE_TRUST_STORE;
  }
  //Dela END

  //************************************************ZOOKEEPER********************************************************
  public static final int ZOOKEEPER_SESSION_TIMEOUT_MS = 30 * 1000;//30 seconds
  public static final int ZOOKEEPER_CONNECTION_TIMEOUT_MS = 30 * 1000;// 30 seconds
  //Zookeeper END

  //************************************************KAFKA********************************************************
  public static final String KAFKA_ACL_WILDCARD = "*";
  public static final String KAFKA_DEFAULT_CONSUMER_GROUP = "default";
  private static final String KAFKA_BROKER_PROTOCOL = "INTERNAL";
  //These brokers are updated periodically by ZookeeperTimerThread
  private Set<String> kafkaBrokers = new HashSet<>();

  public synchronized Set<String> getKafkaBrokers() {
    return kafkaBrokers;
  }

  /**
   * Used when the application does not want all the brokers but mearly one to connect.
   *
   * @return broker
   */
  public synchronized String getRandomKafkaBroker() {
    Iterator<String> it = this.kafkaBrokers.iterator();
    if (it.hasNext()) {
      return it.next();
    }
    return null;
  }

  /**
   *
   * @return brokers
   */
  public synchronized String getKafkaBrokersStr() {
    if (!kafkaBrokers.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (String addr : kafkaBrokers) {
        sb.append(addr).append(",");
      }
      return sb.substring(0, sb.length() - 1);
    }
    return null;
  }

  public synchronized void setKafkaBrokers(Set<String> kafkaBrokers) {
    this.kafkaBrokers.clear();
    this.kafkaBrokers.addAll(kafkaBrokers);
  }

  public Set<String> getBrokerEndpoints() throws IOException, KeeperException, InterruptedException {
    Set<String> brokerList = new HashSet<>();
    ZooKeeper zk;
    zk = new ZooKeeper(getZkConnectStr(),
        Settings.ZOOKEEPER_SESSION_TIMEOUT_MS, new ZookeeperWatcher());
    try {
      List<String> ids = zk.getChildren("/brokers/ids", false);
      for (String id : ids) {
        String brokerInfo = new String(zk.getData("/brokers/ids/" + id,
            false, null));
        String[] tokens = brokerInfo.split(KafkaConst.DLIMITER);
        for (String str : tokens) {
          if (str.contains(KafkaConst.SLASH_SEPARATOR) && str.startsWith(KAFKA_BROKER_PROTOCOL)) {
            brokerList.add(str);
          }
        }
      }
    } finally {
      zk.close();
    }
    return brokerList;
  }

  public class ZookeeperWatcher implements Watcher {

    @Override
    public void process(WatchedEvent we) {
    }
  }
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
  private static final String VARIABLE_OAUTH_ENABLED = "oauth_enabled";
  private static final String VARIABLE_OAUTH_REDIRECT_URI = "oauth_redirect_uri";
  private static final String VARIABLE_OAUTH_ACCOUNT_STATUS = "oauth_account_status";
  private static final String VARIABLE_OAUTH_GROUP_MAPPING = "oauth_group_mapping";

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
  private int LDAP_ACCOUNT_STATUS = 1;
  private String OAUTH_ENABLED = "false";
  private boolean IS_OAUTH_ENABLED = false;
  private String OAUTH_GROUP_MAPPING = "";
  private String OAUTH_REDIRECT_URI = "hopsworks/callback";
  private int OAUTH_ACCOUNT_STATUS = 1;

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
    IS_KRB_ENABLED = setBoolVar(VARIABLE_KRB_AUTH, IS_KRB_ENABLED);
    IS_LDAP_ENABLED = setBoolVar(VARIABLE_LDAP_AUTH, IS_LDAP_ENABLED);
    OAUTH_ENABLED = setStrVar(VARIABLE_OAUTH_ENABLED, OAUTH_ENABLED);
    IS_OAUTH_ENABLED = setBoolVar(VARIABLE_OAUTH_ENABLED, IS_OAUTH_ENABLED);
    OAUTH_REDIRECT_URI = setStrVar(VARIABLE_OAUTH_REDIRECT_URI, OAUTH_REDIRECT_URI);
    OAUTH_ACCOUNT_STATUS = setIntVar(VARIABLE_OAUTH_ACCOUNT_STATUS, OAUTH_ACCOUNT_STATUS);
    OAUTH_GROUP_MAPPING = setStrVar(VARIABLE_OAUTH_GROUP_MAPPING, OAUTH_GROUP_MAPPING);
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

  public synchronized String getOAuthEnabled() {
    checkCache();
    return OAUTH_ENABLED;
  }

  public synchronized  boolean isOAuthEnabled() {
    checkCache();
    return IS_OAUTH_ENABLED;
  }

  public synchronized String getOAuthGroupMapping() {
    checkCache();
    return OAUTH_GROUP_MAPPING;
  }

  public synchronized String getOauthRedirectUri() {
    checkCache();
    return OAUTH_REDIRECT_URI;
  }

  public synchronized int getOAuthAccountStatus() {
    checkCache();
    return OAUTH_ACCOUNT_STATUS;
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

  // User upgradable libraries we installed for them
  private Set<String> PROVIDED_PYTHON_LIBRARY_NAMES;
  private static final String VARIABLE_PROVIDED_PYTHON_LIBRARY_NAMES = "provided_python_lib_names";
  private static final String DEFAULT_PROVIDED_PYTHON_LIBRARY_NAMES =
      "hops, pandas, numpy";

  public synchronized Set<String> getProvidedPythonLibraryNames() {
    checkCache();
    return PROVIDED_PYTHON_LIBRARY_NAMES;
  }

  // Libraries we preinstalled users should not mess with
  private Set<String> PREINSTALLED_PYTHON_LIBRARY_NAMES;
  private static final String VARIABLE_PREINSTALLED_PYTHON_LIBRARY_NAMES = "preinstalled_python_lib_names";
  private static final String DEFAULT_PREINSTALLED_PYTHON_LIBRARY_NAMES =
      "tensorflow-gpu, tensorflow, pydoop, pyspark, tensorboard";

  public synchronized Set<String> getPreinstalledPythonLibraryNames() {
    checkCache();
    return PREINSTALLED_PYTHON_LIBRARY_NAMES;
  }

  private String HOPSWORKS_VERSION;

  public synchronized String getHopsworksVersion() {
    checkCache();
    return HOPSWORKS_VERSION;
  }

  private String CUDA_VERSION;

  public synchronized String getCudaVersion() {
    checkCache();
    return CUDA_VERSION;
  }

  private String TENSORFLOW_VERSION;

  public synchronized String getTensorflowVersion() {
    checkCache();
    return TENSORFLOW_VERSION;
  }

  private String DRELEPHANT_VERSION;

  public synchronized String getDrelephantVersion() {
    checkCache();
    return DRELEPHANT_VERSION;
  }

  private String ELASTIC_VERSION;

  public synchronized String getElasticVersion() {
    checkCache();
    return ELASTIC_VERSION;
  }

  private String KAFKA_VERSION;

  public synchronized String getKafkaVersion() {
    checkCache();
    return KAFKA_VERSION;
  }

  private String DELA_VERSION;

  public synchronized String getDelaVersion() {
    checkCache();
    return DELA_VERSION;
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

  private String SLIDER_VERSION;

  public synchronized String getSliderVersion() {
    checkCache();
    return SLIDER_VERSION;
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

  private String KAPACITOR_VERSION;

  public synchronized String getKapacitorVersion() {
    checkCache();
    return KAPACITOR_VERSION;
  }

  private String TELEGRAF_VERSION;

  public synchronized String getTelegrafVersion() {
    checkCache();
    return TELEGRAF_VERSION;
  }

  private String GRAFANA_VERSION;

  public synchronized String getGrafanaVersion() {
    checkCache();
    return GRAFANA_VERSION;
  }

  private String INFLUXDB_VERSION;

  public synchronized String getInfluxdbVersion() {
    checkCache();
    return INFLUXDB_VERSION;
  }

  private String ZOOKEEPER_VERSION;

  public synchronized String getZookeeperVersion() {
    checkCache();
    return ZOOKEEPER_VERSION;
  }

  // -------------------------------- Kubernetes ----------------------------------------------//
  private String KUBE_USER = "hopsworks";

  public synchronized String getKubeUser() {
    checkCache();
    return KUBE_USER;
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

  private String KUBE_REGISTRY = "registry.docker-registry.svc.cluster.local";

  public synchronized String getKubeRegistry() {
    checkCache();
    return KUBE_REGISTRY;
  }

  private Integer KUBE_MAX_SERVING_INSTANCES = 10;

  public synchronized Integer getKubeMaxServingInstances() {
    checkCache();
    return KUBE_MAX_SERVING_INSTANCES;
  }

  private Integer KUBE_API_MAX_ATTEMPTS = 12;
  public synchronized Integer getKubeAPIMaxAttempts() {
    checkCache();
    return KUBE_API_MAX_ATTEMPTS;
  }

  private String KUBE_TF_IMG_VERSION = "0.10.0";
  public synchronized String getKubeTfImgVersion() {
    checkCache();
    return KUBE_TF_IMG_VERSION;
  }

  private String KUBE_SKLEARN_IMG_VERSION = "0.10.0";
  public synchronized String getKubeSKLearnImgVersion() {
    checkCache();
    return KUBE_SKLEARN_IMG_VERSION;
  }

  private String KUBE_FILEBEAT_IMG_VERSION = "0.10.0";
  public synchronized String getKubeFilebeatImgVersion() {
    checkCache();
    return KUBE_FILEBEAT_IMG_VERSION;
  }
  
  
  private Boolean ONLINE_FEATURESTORE = false;
  
  public synchronized Boolean isOnlineFeaturestore() {
    checkCache();
    return ONLINE_FEATURESTORE;
  }

  private String KUBE_JUPYTER_IMG_VERSION = "0.10.0";
  public synchronized String getJupyterImgVersion() {
    checkCache();
    return KUBE_JUPYTER_IMG_VERSION;
  }

  private Integer KUBE_DOCKER_MAX_MEMORY_ALLOCATION = 8192;
  public synchronized Integer getKubeDockerMaxMemoryAllocation() {
    checkCache();
    return KUBE_DOCKER_MAX_MEMORY_ALLOCATION;
  }

  private Integer KUBE_DOCKER_MAX_CORES_ALLOCATION = 4;
  public synchronized Integer getKubeDockerMaxCoresAllocation() {
    checkCache();
    return KUBE_DOCKER_MAX_CORES_ALLOCATION;
  }

  private Double KUBE_DOCKER_CORES_FRACTION = 1.0;
  public synchronized Double getKubeDockerCoresFraction() {
    checkCache();
    return KUBE_DOCKER_CORES_FRACTION;
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

  private String JUPYTER_HOST = "localhost";

  public synchronized String getJupyterHost() {
    checkCache();
    return JUPYTER_HOST;
  }
  
  private String HOPS_VERIFICATION_VERSION = "1.0.0-SNAPSHOT";
  
  public synchronized String getHopsVerificationVersion() {
    checkCache();
    return HOPS_VERIFICATION_VERSION;
  }
  
  private volatile String HOPS_VERIFICATION_MAIN_CLASS = null;
  public void setHopsVerificationMainClass(String mainClass) {
    synchronized (Settings.class) {
      HOPS_VERIFICATION_MAIN_CLASS = mainClass;
    }
  }
  
  public String getHopsVerificationMainClass() {
    if (HOPS_VERIFICATION_MAIN_CLASS == null) {
      synchronized (Settings.class) {
        return HOPS_VERIFICATION_MAIN_CLASS;
      }
    }
    return HOPS_VERIFICATION_MAIN_CLASS;
  }

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
    updateVariableInternal(VARIABLE_SERVICE_MASTER_JWT, JWT);
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
      updateVariableInternal(variableKey, renewTokens[i]);
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

  public String getHiveSiteSparkHdfsPath() {
    return "hdfs:///user/" + getSparkUser() + "/hive-site.xml";
  }

  private String FEATURESTORE_DB_DEFAULT_QUOTA = "50000";

  public synchronized Long getFeaturestoreDbDefaultQuota() {
    checkCache();
    return Long.parseLong(FEATURESTORE_DB_DEFAULT_QUOTA);
  }

  private String FEATURESTORE_DB_DEFAULT_STORAGE_FORMAT = "ORC";

  public synchronized String getFeaturestoreDbDefaultStorageFormat() {
    checkCache();
    return FEATURESTORE_DB_DEFAULT_STORAGE_FORMAT;
  }

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

  private boolean IAM_ROLE_CONFIGURED = false;
  public synchronized boolean isIAMRoleConfigured() {
    checkCache();
    return IAM_ROLE_CONFIGURED;
  }

  public Boolean isHopsUtilInsecure() {
    return isCloud() || isLocalHost();
  }
  
  private String FEATURESTORE_JDBC_URL = "jdbc:mysql://" + HOPSWORKS_IP + ":3306/";
  
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
  
  public static final int ELASTIC_KIBANA_NO_CONNECTIONS = 5;

  //-------------------------------- PROVENANCE ----------------------------------------------//
  private static final String VARIABLE_PROVENANCE_TYPE = "provenance_type"; //disabled/meta/min/full
  private static final String VARIABLE_PROVENANCE_ARCHIVE_SIZE = "provenance_archive_size";
  private static final String VARIABLE_PROVENANCE_ARCHIVE_DELAY = "provenance_archive_delay";
  private static final String VARIABLE_PROVENANCE_CLEANUP_SIZE = "provenance_cleanup_size";
  private static final String VARIABLE_PROVENANCE_CLEANER_PERIOD = "provenance_cleaner_period";
  
  public static final String PROV_FILE_INDEX_SUFFIX = "__file_prov";
  private Provenance.Type PROVENANCE_TYPE = Provenance.Type.MIN;
  private String PROVENANCE_TYPE_S = PROVENANCE_TYPE.name();
  private Integer PROVENANCE_CLEANUP_SIZE = 5;
  private Integer PROVENANCE_ARCHIVE_SIZE = 100;
  private Long PROVENANCE_CLEANER_PERIOD = 3600L; //1h in s
  private Long PROVENANCE_ARCHIVE_DELAY = 0l;
  private Integer PROVENANCE_ELASTIC_ARCHIVAL_PAGE_SIZE = 50;
  
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
    PROVENANCE_ARCHIVE_SIZE = setIntVar(VARIABLE_PROVENANCE_ARCHIVE_SIZE, PROVENANCE_ARCHIVE_SIZE);
    PROVENANCE_ARCHIVE_DELAY = setLongVar(VARIABLE_PROVENANCE_ARCHIVE_DELAY, PROVENANCE_ARCHIVE_DELAY);
    PROVENANCE_CLEANUP_SIZE = setIntVar(VARIABLE_PROVENANCE_CLEANUP_SIZE, PROVENANCE_CLEANUP_SIZE);
    PROVENANCE_CLEANER_PERIOD = setLongVar(VARIABLE_PROVENANCE_CLEANER_PERIOD, PROVENANCE_CLEANER_PERIOD);
  }
  
  public synchronized Provenance.Type getProvType() {
    checkCache();
    return PROVENANCE_TYPE;
  }
  
  public synchronized Integer getProvArchiveSize() {
    checkCache();
    return PROVENANCE_ARCHIVE_SIZE;
  }
  
  public synchronized void setProvArchiveSize(Integer size) {
    if(!PROVENANCE_ARCHIVE_SIZE.equals(size)) {
      em.merge(new Variables(VARIABLE_PROVENANCE_ARCHIVE_SIZE, size.toString()));
      PROVENANCE_ARCHIVE_SIZE = size;
    }
  }
  
  public synchronized Long getProvArchiveDelay() {
    checkCache();
    return PROVENANCE_ARCHIVE_DELAY;
  }
  
  public synchronized void setProvArchiveDelay(Long delay) {
    if(!PROVENANCE_ARCHIVE_DELAY.equals(delay)) {
      em.merge(new Variables(VARIABLE_PROVENANCE_ARCHIVE_DELAY, delay.toString()));
      PROVENANCE_ARCHIVE_DELAY = delay;
    }
  }
  
  public synchronized Integer getProvCleanupSize() {
    checkCache();
    return PROVENANCE_CLEANUP_SIZE;
  }
  
  public synchronized Integer getProvElasticArchivalPageSize() {
    checkCache();
    return PROVENANCE_ELASTIC_ARCHIVAL_PAGE_SIZE;
  }
  
  public synchronized Long getProvCleanerPeriod() {
    checkCache();
    return PROVENANCE_CLEANER_PERIOD;
  }
  
  public synchronized void setProvCleanerPeriod(Long period) {
    if(!PROVENANCE_CLEANER_PERIOD.equals(period)) {
      em.merge(new Variables(VARIABLE_PROVENANCE_CLEANER_PERIOD, period.toString()));
      PROVENANCE_CLEANER_PERIOD = period;
    }
  }
  //------------------------------ END PROVENANCE --------------------------------------------//
}
