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
