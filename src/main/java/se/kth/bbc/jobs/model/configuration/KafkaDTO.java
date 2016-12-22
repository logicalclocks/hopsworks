package se.kth.bbc.jobs.model.configuration;

import java.io.File;
import se.kth.bbc.jobs.MutableJsonObject;
import se.kth.bbc.jobs.model.JsonReduceable;

/**
 * CTO containing containing kafka related HopsWorks information.
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
