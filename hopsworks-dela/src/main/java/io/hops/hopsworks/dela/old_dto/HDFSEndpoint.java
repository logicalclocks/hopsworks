package io.hops.hopsworks.dela.old_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HDFSEndpoint {

  private String xmlPath;
  private String user;

  public HDFSEndpoint() {
  }

  public HDFSEndpoint(String xmlPath, String user) {
    this.xmlPath = xmlPath;
    this.user = user;
  }

  public String getXmlPath() {
    return xmlPath;
  }

  public void setXmlPath(String xmlPath) {
    this.xmlPath = xmlPath;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  @Override
  public String toString() {
    return "HDFSEndpoint{" + "xmlPath=" + xmlPath + ", user=" + user + '}';
  }

}
