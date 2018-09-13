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

package io.hops.hopsworks.common.jobs.configuration;

import io.hops.hopsworks.common.dao.jobs.JsonReduceable;
import io.hops.hopsworks.common.jobs.MutableJsonObject;

/**
 * CTO containing containing kafka related Hopsworks information.
 * <p>
 */
public class KafkaDTO implements JsonReduceable {

  protected final static String KEY_KAFKA_ADVANCED = "ADVANCED";
  protected final static String KEY_KAFKA_TOPICS = "TOPICS";
  protected final static String KEY_KAFKA_CONSUMER_GROUPS
          = "CONSUMER_GROUPS";
  protected final static String KEY_TOPIC_NAME = "NAME";
  protected final static String KEY_TOPIC_TICKED = "TICKED";
  protected final static String KEY_CONSUMER_GROUP_ID = "ID";
  protected final static String KEY_CONSUMER_GROUP_NAME = "NAME";

  //Kafka properties
  private boolean advanced;
  private KafkaTopicDTO[] topics;
  private ConsumerGroupDTO[] consumergroups;

  public boolean isAdvanced() {
    return advanced;
  }

  public void setAdvanced(boolean advanced) {
    this.advanced = advanced;
  }

  public KafkaTopicDTO[] getTopics() {
    return topics;
  }

  public void setTopics(KafkaTopicDTO[] topics) {
    this.topics = topics;
  }

  public ConsumerGroupDTO[] getConsumergroups() {
    return consumergroups;
  }

  public void setConsumergroups(ConsumerGroupDTO[] consumergroups) {
    this.consumergroups = consumergroups;
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = new MutableJsonObject();
    obj.set(KEY_KAFKA_ADVANCED, "" + advanced);
    if (topics != null && topics.length > 0) {
      MutableJsonObject topicsJson = new MutableJsonObject();
      for (KafkaTopicDTO topicDTO : topics) {
        MutableJsonObject topicJson = new MutableJsonObject();
        topicJson.set(KEY_TOPIC_NAME, topicDTO.getName());
        topicJson.set(KEY_TOPIC_TICKED, topicDTO.getTicked());
        topicsJson.set(topicDTO.getName(), topicJson);
      }
      obj.set(KEY_KAFKA_TOPICS, topicsJson);
    }
    if (consumergroups != null && consumergroups.length > 0) {
      MutableJsonObject consumerGroupsJson = new MutableJsonObject();
      for (ConsumerGroupDTO group : consumergroups) {
        MutableJsonObject groupJson = new MutableJsonObject();
        groupJson.set(KEY_CONSUMER_GROUP_ID, group.getId());
        groupJson.set(KEY_CONSUMER_GROUP_NAME, group.getName());
        consumerGroupsJson.set(group.getName(), groupJson);
      }
      obj.set(KEY_KAFKA_CONSUMER_GROUPS, consumerGroupsJson);
    }

    return obj;
  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException {

    if (json.containsKey(KEY_KAFKA_ADVANCED)) {
      advanced = Boolean.parseBoolean(json.getString(KEY_KAFKA_ADVANCED));
    }

    if (json.containsKey(KEY_KAFKA_TOPICS)) {
      MutableJsonObject topicsObj = json.getJsonObject(KEY_KAFKA_TOPICS);
      KafkaTopicDTO[] jsonTopics = new KafkaTopicDTO[topicsObj.size()];
      int i = 0;
      for (String key : topicsObj.keySet()) {
        MutableJsonObject topic = topicsObj.getJsonObject(key);
        jsonTopics[i] = new KafkaTopicDTO(topic.getString(KEY_TOPIC_NAME),
                topic.getString(KEY_TOPIC_TICKED));
        i++;
      }
      topics = jsonTopics;
    }
    if (json.containsKey(KEY_KAFKA_CONSUMER_GROUPS)) {
      MutableJsonObject consumerGroupsObj = json.getJsonObject(
              KEY_KAFKA_CONSUMER_GROUPS);
      ConsumerGroupDTO[] jsonConsumerGroups
              = new ConsumerGroupDTO[consumerGroupsObj.size()];
      int i = 0;
      for (String key : consumerGroupsObj.keySet()) {
        MutableJsonObject consumerGroup = consumerGroupsObj.getJsonObject(key);
        jsonConsumerGroups[i] = new ConsumerGroupDTO(consumerGroup.getString(
                KEY_CONSUMER_GROUP_ID), consumerGroup.getString(
                        KEY_CONSUMER_GROUP_NAME));
        i++;
      }
      consumergroups = jsonConsumerGroups;
    }

  }

}
