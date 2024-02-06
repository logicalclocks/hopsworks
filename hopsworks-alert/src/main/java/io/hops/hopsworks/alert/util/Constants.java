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
package io.hops.hopsworks.alert.util;

public class Constants {
  public static final int RETRY_SECONDS = 20;
  public static final int NUM_RETRIES = 6;
  public static final int NUM_SERVER_ERRORS = 5;
  
  public static final String USER_PLACE_HOLDER = "%%user%%";
  public static final String PROJECT_PLACE_HOLDER = "%%project%%";
  public static final String FG_PLACE_HOLDER = "%%fg%%";
  public static final String JOB_PLACE_HOLDER = "%%job%%";
  public static final String FG_ID_PLACE_HOLDER = "%%fgId%%";
  public static final String EXECUTION_ID_PLACE_HOLDER = "%%executionId%%";
  public static final String SILENCE_CREATED_BY_FORMAT = USER_PLACE_HOLDER + "@" + PROJECT_PLACE_HOLDER;
  public static final String RECEIVER_NAME_SEPARATOR = "__";
  public static final String GLOBAL_RECEIVER_NAME_PREFIX = "global-receiver" + RECEIVER_NAME_SEPARATOR;
  public static final String RECEIVER_NAME_PLACE_HOLDER = "%%receiverName%%";
  public static final String RECEIVER_NAME_PREFIX = PROJECT_PLACE_HOLDER + RECEIVER_NAME_SEPARATOR;
  public static final String RECEIVER_NAME_FORMAT = RECEIVER_NAME_PREFIX + RECEIVER_NAME_PLACE_HOLDER;
  
  public static final String ALERT_NAME_LABEL = "alertname";
  public static final String ALERT_NAME_JOB = "JobExecution";
  public static final String ALERT_NAME_FEATURE_VALIDATION = "FeatureValidation";
  
  public static final String ALERT_TYPE_LABEL = "type";
  public static final String LABEL_PROJECT = "project";
  public static final String LABEL_SEVERITY = "severity";
  public static final String LABEL_STATUS = "status";
  
  public static final String LABEL_JOB = "job";
  public static final String LABEL_EXECUTION_ID = "executionId";
  public static final String LABEL_FEATURE_STORE = "featureStore";
  public static final String LABEL_FEATURE_GROUP = "featureGroup";
  public static final String LABEL_FEATURE_GROUP_ID = "featureGroupId";
  public static final String LABEL_FEATURE_GROUP_VERSION = "featureGroupVersion";
  
  public static final String LABEL_TITLE = "title";
  public static final String LABEL_SUMMARY = "summary";
  public static final String LABEL_DESCRIPTION = "description";
  
  public final static String ALERT_NAME_FEATURE_MONITOR = "FeatureMonitoring";
  public final static String LABEL_FM_CONFIG = "featureMonitorConfig";
  public final static String LABEL_FM_RESULT_ID = "featureMonitorResultId";
  public final static String LABEL_FEATURE_VIEW_NAME = "featureViewName";
  public final static String LABEL_FEATURE_VIEW_VERSION = "featureViewVersion";
  
  public static final String FILTER_BY_PROJECT_LABEL = LABEL_PROJECT + "=\"";
  public static final String FILTER_BY_PROJECT_FORMAT = FILTER_BY_PROJECT_LABEL + PROJECT_PLACE_HOLDER + "\"";
  
  public static final String FILTER_BY_JOB_LABEL = LABEL_JOB + "=\"";
  public static final String FILTER_BY_JOB_FORMAT = FILTER_BY_JOB_LABEL + JOB_PLACE_HOLDER + "\"";
  
  public static final String FILTER_BY_EXECUTION_LABEL = LABEL_EXECUTION_ID + "=\"";
  public static final String FILTER_BY_EXECUTION_FORMAT = FILTER_BY_EXECUTION_LABEL + EXECUTION_ID_PLACE_HOLDER + "\"";
  
  public static final String FILTER_BY_FG_LABEL = LABEL_FEATURE_GROUP + "=\"";
  public static final String FILTER_BY_FG_FORMAT = FILTER_BY_FG_LABEL + FG_PLACE_HOLDER + "\"";
  
  public static final String FILTER_BY_FG_ID_LABEL = LABEL_FEATURE_GROUP_ID + "=\"";
  public static final String FILTER_BY_FG_ID_FORMAT = FILTER_BY_FG_ID_LABEL + FG_ID_PLACE_HOLDER + "\"";
  
  public static final String TEST_ALERT_JOB_NAME = "AlertTestJob";
  public static final String TEST_ALERT_FS_NAME = "AlertTestFeatureStore";
  public static final String TEST_ALERT_FG_NAME = "AlertTestFeatureGroup";
  public static final String TEST_ALERT_FG_DESCRIPTION = "Alert test description.";
  public static final String TEST_ALERT_FG_SUMMARY = "Alert test summary.";
  public static final String TEST_ALERT_FM_NAME = "AlertTestFeatureMonitor";
  public static final Integer TEST_ALERT_EXECUTION_ID = 1;
  public static final Integer TEST_ALERT_FG_ID = 1;
  public static final Integer TEST_ALERT_FG_VERSION = 1;
  
  public static final  String FM_RESULT_ID_LABEL = "featureMonitorResultId";
  public static final String FM_ID_PLACE_HOLDER = "%%fmResultId%%";
  public static final String FILTER_BY_FM_RESULT_ID = FM_RESULT_ID_LABEL + "=\"";
  public static final  String FILTER_BY_FM_RESULT_FORMAT =  FILTER_BY_FM_RESULT_ID + FM_ID_PLACE_HOLDER + "\"";
  public static final  String FM_CONFIG_NAME_LABEL ="featureMonitorConfig";
  public static final  String FM_NAME_PLACE_HOLDER ="%%configName%%";
  public static final String FILTER_BY_FV_LABEL = FM_CONFIG_NAME_LABEL + "=\"";
  public static final String FILTER_BY_FM_NAME_FORMAT = FILTER_BY_FV_LABEL + FM_NAME_PLACE_HOLDER + "\"";
  
  public static final String DEFAULT_EMAIL_HTML = "{{ template \"hopsworks.email.default.html\" . }}";
  public static final String DEFAULT_SLACK_ICON_URL = "https://uploads-ssl.webflow.com/5f6353590bb01cacbcecfbac/" +
    "633ed0f0ed7662f8621ce701_Hopsworks%20Logo%20Social%20.png";
  public static final String DEFAULT_SLACK_TEXT = "{{ template \"hopsworks.slack.default.text\" . }}";
  public static final String DEFAULT_SLACK_TITLE = "{{ template \"hopsworks.slack.default.title\" . }}";
  public static final String NO_PAYLOAD = "No payload.";
  
  public static final String AM_CONFIG_UPDATED_TOPIC_NAME = "alertmanager_config_updated";
  
  public enum TimerType {
    CLIENT,
    CONFIG
  }
}
