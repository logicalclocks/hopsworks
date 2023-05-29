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
  public final static int RETRY_SECONDS = 20;
  public final static int NUM_RETRIES = 6;
  public final static int NUM_SERVER_ERRORS = 5;

  public final static String USER_PLACE_HOLDER = "%%user%%";
  public final static String PROJECT_PLACE_HOLDER = "%%project%%";
  public final static String FG_PLACE_HOLDER = "%%fg%%";
  public final static String JOB_PLACE_HOLDER = "%%job%%";
  public final static String FG_ID_PLACE_HOLDER = "%%fgId%%";
  public final static String EXECUTION_ID_PLACE_HOLDER = "%%executionId%%";
  public final static String SILENCE_CREATED_BY_FORMAT = USER_PLACE_HOLDER + "@" + PROJECT_PLACE_HOLDER;
  public final static String RECEIVER_NAME_SEPARATOR = "__";
  public final static String GLOBAL_RECEIVER_NAME_PREFIX = "global-receiver" + RECEIVER_NAME_SEPARATOR;
  public final static String RECEIVER_NAME_PLACE_HOLDER = "%%receiverName%%";
  public final static String RECEIVER_NAME_PREFIX = PROJECT_PLACE_HOLDER + RECEIVER_NAME_SEPARATOR;
  public final static String RECEIVER_NAME_FORMAT = RECEIVER_NAME_PREFIX + RECEIVER_NAME_PLACE_HOLDER;

  public final static String ALERT_NAME_LABEL = "alertname";
  public final static String ALERT_NAME_JOB = "JobExecution";
  public final static String ALERT_NAME_FEATURE_VALIDATION = "FeatureValidation";

  public final static String ALERT_TYPE_LABEL = "type";
  public final static String LABEL_PROJECT = "project";
  public final static String LABEL_SEVERITY = "severity";
  public final static String LABEL_STATUS = "status";

  public final static String LABEL_JOB = "job";
  public final static String LABEL_EXECUTION_ID = "executionId";
  public final static String LABEL_FEATURE_STORE = "featureStore";
  public final static String LABEL_FEATURE_GROUP = "featureGroup";
  public final static String LABEL_FEATURE_GROUP_ID = "featureGroupId";
  public final static String LABEL_FEATURE_GROUP_VERSION = "featureGroupVersion";

  public final static String LABEL_TITLE = "title";
  public final static String LABEL_SUMMARY = "summary";
  public final static String LABEL_DESCRIPTION = "description";

  public final static String FILTER_BY_PROJECT_LABEL = LABEL_PROJECT + "=\"";
  public final static String FILTER_BY_PROJECT_FORMAT = FILTER_BY_PROJECT_LABEL + PROJECT_PLACE_HOLDER + "\"";

  public final static String FILTER_BY_JOB_LABEL = LABEL_JOB + "=\"";
  public final static String FILTER_BY_JOB_FORMAT = FILTER_BY_JOB_LABEL + JOB_PLACE_HOLDER + "\"";

  public final static String FILTER_BY_EXECUTION_LABEL = LABEL_EXECUTION_ID + "=\"";
  public final static String FILTER_BY_EXECUTION_FORMAT = FILTER_BY_EXECUTION_LABEL + EXECUTION_ID_PLACE_HOLDER + "\"";

  public final static String FILTER_BY_FG_LABEL = LABEL_FEATURE_GROUP + "=\"";
  public final static String FILTER_BY_FG_FORMAT = FILTER_BY_FG_LABEL + FG_PLACE_HOLDER + "\"";

  public final static String FILTER_BY_FG_ID_LABEL = LABEL_FEATURE_GROUP_ID + "=\"";
  public final static String FILTER_BY_FG_ID_FORMAT = FILTER_BY_FG_ID_LABEL + FG_ID_PLACE_HOLDER + "\"";

  public final static String TEST_ALERT_JOB_NAME = "AlertTestJob";
  public final static String TEST_ALERT_FS_NAME = "AlertTestFeatureStore";
  public final static String TEST_ALERT_FG_NAME = "AlertTestFeatureGroup";
  public final static String TEST_ALERT_FG_DESCRIPTION = "Alert test description.";
  public final static String TEST_ALERT_FG_SUMMARY = "Alert test summary.";
  
  public final static Integer TEST_ALERT_EXECUTION_ID = 1;
  public final static Integer TEST_ALERT_FG_ID = 1;
  public final static Integer TEST_ALERT_FG_VERSION = 1;

  public final static String DEFAULT_EMAIL_HTML = "{{ template \"hopsworks.email.default.html\" . }}";
  public final static String DEFAULT_SLACK_ICON_URL = "https://uploads-ssl.webflow.com/5f6353590bb01cacbcecfbac/" +
    "633ed0f0ed7662f8621ce701_Hopsworks%20Logo%20Social%20.png";
  public final static String DEFAULT_SLACK_TEXT = "{{ template \"hopsworks.slack.default.text\" . }}";
  public final static String DEFAULT_SLACK_TITLE = "{{ template \"hopsworks.slack.default.title\" . }}";

  public final static String AM_CONFIG_UPDATED_TOPIC_NAME = "alertmanager_config_updated";

  public enum TimerType {
    CLIENT,
    CONFIG
  }
}
