package io.hops.hopsworks.common.dao.app;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class EmailJsonDTO extends KeystoreDTO {

  private String dest;
  private String subject;
  private String message;

  public EmailJsonDTO() {
  }

  public String getDest() {
    return dest;
  }

  public void setDest(String dest) {
    this.dest = dest;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

}
