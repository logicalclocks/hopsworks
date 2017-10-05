package io.hops.hopsworks.dela.old_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class KafkaDetails {

  private String file;
  private KafkaResource resource;

  public KafkaDetails(String file, KafkaResource resource) {
    this.file = file;
    this.resource = resource;
  }

  public KafkaDetails() {
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public KafkaResource getResource() {
    return resource;
  }

  public void setResource(KafkaResource resource) {
    this.resource = resource;
  }
}
