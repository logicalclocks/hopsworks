package se.kth.bbc.jobs.model.configuration;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author tkak
 */
@XmlRootElement
public class ConsumerGroupDTO {

  private String id;
  private String name;

  public ConsumerGroupDTO() {
  }

  public ConsumerGroupDTO(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return "ConsumerGroupDTO{" + "name=" + name + '}';
  }

}
