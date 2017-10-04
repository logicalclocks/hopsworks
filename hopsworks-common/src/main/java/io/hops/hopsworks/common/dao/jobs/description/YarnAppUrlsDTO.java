package io.hops.hopsworks.common.dao.jobs.description;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class YarnAppUrlsDTO implements Serializable {

  String name;
  String url;

  public YarnAppUrlsDTO() {
  }

  public YarnAppUrlsDTO(String name, String url) {
    this.name = name;
    this.url = url;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

}
