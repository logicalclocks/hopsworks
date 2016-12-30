package io.hops.hopsworks.common.jobs.configuration;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobs.JsonReduceable;
import io.hops.hopsworks.common.jobs.MutableJsonObject;

/**
 * CTO containing containing kafka related HopsWorks information.
 * <p>
 */
public class KafkaDTO implements JsonReduceable {

  protected final static String KEY_KAFKA_SELECTED = "KAFKA_SELECTED";
  protected final static String KEY_KAFKA_TOPICS = "KAFKA_TOPICS";
  protected final static String KEY_KAFKA_CONSUMER_GROUP
          = "KAFKA_CONSUMER_GROUP";
  //Kafka properties
  private boolean selected;
  private String topics;
  private String consumergroups;

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public String getTopics() {
    return topics;
  }

  public void setTopics(String topics) {
    this.topics = topics;
  }

  public String getConsumergroups() {
    return consumergroups;
  }

  public void setConsumergroups(String consumergroups) {
    this.consumergroups = consumergroups;
  }

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = new MutableJsonObject();
    obj.set(KEY_KAFKA_SELECTED, "" + selected);
    if (selected) {
      obj.set(KEY_KAFKA_SELECTED, "" + selected);
      if (!Strings.isNullOrEmpty(topics)) {
        obj.set(KEY_KAFKA_TOPICS, topics);
      }
      if (!Strings.isNullOrEmpty(consumergroups)) {
        obj.set(KEY_KAFKA_CONSUMER_GROUP, consumergroups);
      }
    }
    return obj;

  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException {
    String jsonSelected, jsonTopics, jsonConsumerGroups;
    jsonTopics = jsonConsumerGroups = "";

    if (json.containsKey(KEY_KAFKA_SELECTED)) {
      jsonSelected = json.getString(KEY_KAFKA_SELECTED);
      selected = Boolean.parseBoolean(jsonSelected);
      if (json.containsKey(KEY_KAFKA_TOPICS)) {
        jsonTopics = json.getString(KEY_KAFKA_TOPICS);
      }
      if (json.containsKey(KEY_KAFKA_CONSUMER_GROUP)) {
        jsonConsumerGroups = json.getString(KEY_KAFKA_CONSUMER_GROUP);
      }

    }

    if (selected) {
      if (!Strings.isNullOrEmpty(jsonTopics)) {
        topics = jsonTopics;
      }
      if (!Strings.isNullOrEmpty(jsonConsumerGroups)) {
        consumergroups = jsonConsumerGroups;
      }
    }

  }

}
