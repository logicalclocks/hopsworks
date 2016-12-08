package se.kth.bbc.jobs.model.configuration;

import com.google.common.base.Strings;
import se.kth.bbc.jobs.MutableJsonObject;
import se.kth.bbc.jobs.model.JsonReduceable;

/**
 *
 * @author tkak
 */
public class KafkaDTO implements JsonReduceable {

  protected final static String KEY_KAFKA_SELECTED = "KAFKA_SELECTED";
  protected final static String KEY_KAFKA_TOPICS = "KAFKA_TOPICS";
  //Kafka properties
  private boolean selected;
  private String topics;

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

  @Override
  public MutableJsonObject getReducedJsonObject() {
    MutableJsonObject obj = new MutableJsonObject();
    if (selected) {
      obj.set(KEY_KAFKA_SELECTED, "" + selected);
      if (!Strings.isNullOrEmpty(topics)) {
        obj.set(KEY_KAFKA_TOPICS, topics);
      }
    }
    return obj;

  }

  @Override
  public void updateFromJson(MutableJsonObject json) throws
          IllegalArgumentException {
    String jsonSelected, jsonTopics;
    jsonSelected = jsonTopics = "";

    if (json.containsKey(KEY_KAFKA_SELECTED)) {
      jsonSelected = json.getString(KEY_KAFKA_SELECTED);
      jsonTopics = json.getString(KEY_KAFKA_TOPICS);
    }

    selected = Boolean.parseBoolean(jsonSelected);
    if (selected) {
      topics = jsonTopics;  
    }
  }

}
