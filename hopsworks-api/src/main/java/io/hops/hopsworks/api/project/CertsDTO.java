package io.hops.hopsworks.api.project;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CertsDTO {
  private String fileExtension;
  private String kStore;
  private String tStore;

  public CertsDTO() {
  }

  public CertsDTO(String fileExtension, String kStore, String tStore) {
    this.fileExtension = fileExtension;
    this.kStore = kStore;
    this.tStore = tStore;
  }

  public String getFileExtension() {
    return fileExtension;
  }

  public void setFileExtension(String fileExtension) {
    this.fileExtension = fileExtension;
  }

  public String getkStore() {
    return kStore;
  }

  public void setkStore(String kStore) {
    this.kStore = kStore;
  }

  public String gettStore() {
    return tStore;
  }

  public void settStore(String tStore) {
    this.tStore = tStore;
  }
  
  
}
