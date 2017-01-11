package se.kth.bbc.jobs.model.configuration;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author tkak
 */
@XmlRootElement
public class KafkaTopicDTO {

  private String name;
  private String ticked;

  public KafkaTopicDTO() {
  }

  public KafkaTopicDTO(String name, String ticked) {
    this.name = name;
    this.ticked = ticked;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getTicked() {
    return ticked;
  }

  public void setTicked(String ticked) {
    this.ticked = ticked;
  }

  @Override
  public String toString() {
    return "KafkaTopicDTO{" + "name=" + name + ", ticked=" + ticked + '}';
  }

}
