/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.jobs.yarn;

import io.hops.hopsworks.common.jobs.configuration.ConsumerGroupDTO;
import io.hops.hopsworks.common.jobs.configuration.KafkaTopicDTO;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;

/**
 * POJO that provides Kafka related information to HopsUtil.
 */
public class KafkaProperties extends ServiceProperties {

  private String brokerAddresses;

  private String restEndpoint;
  //Comma-separated list of consumer groups
  private String consumerGroups;
  //Comma-separated list of topics;
  private String topics;
  private String sessionId;

  public KafkaProperties() {
  }

  public String getBrokerAddresses() {
    return brokerAddresses;
  }

  public void setBrokerAddresses(String brokerAddresses) {
    this.brokerAddresses = brokerAddresses;
  }

  public String getConsumerGroups() {
    return consumerGroups;
  }

  /**
   * Append project name to every consumer group. Sets the default consumer
   * group.
   *
   * @param projectName
   * @param consumerGroups
   */
  public void setProjectConsumerGroups(String projectName,
      ConsumerGroupDTO[] consumerGroups) {
    this.consumerGroups = "";
    StringBuilder sb = new StringBuilder();
    sb.append(projectName).append("__").append(
        Settings.KAFKA_DEFAULT_CONSUMER_GROUP).append(File.pathSeparator);
    if (consumerGroups != null) {
      for (ConsumerGroupDTO consumerGroup : consumerGroups) {
        if (!consumerGroup.getName().equals(Settings.KAFKA_DEFAULT_CONSUMER_GROUP)) {
          sb.append(projectName).append("__").append(consumerGroup.getName()).append(File.pathSeparator);
        }
      }
    }
    this.consumerGroups += sb.substring(0, sb.lastIndexOf(File.pathSeparator));
  }

  public String getTopics() {
    return topics;
  }

  public void setTopics(KafkaTopicDTO[] topics) {
    this.topics = "";
    StringBuilder sb = new StringBuilder();
    for (KafkaTopicDTO topic : topics) {
      sb.append(topic.getName()).append(File.pathSeparator);
    }
    this.topics = sb.substring(0, sb.lastIndexOf(File.pathSeparator));
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @Override
  public String toString() {
    return "KafkaProperties{" + "brokerAddresses=" + brokerAddresses
        + ", restEndpoint=" + restEndpoint
        + ", consumerGroups=" + consumerGroups + ", topics=" + topics + '}';
  }

}
