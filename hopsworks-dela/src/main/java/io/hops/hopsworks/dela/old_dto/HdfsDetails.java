package io.hops.hopsworks.dela.old_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class HdfsDetails {
  private String file;
  private HDFSResource resource;

  public HdfsDetails(String file, HDFSResource resource) {
    this.file = file;
    this.resource = resource;
  }

  public HdfsDetails() {
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public HDFSResource getResource() {
    return resource;
  }

  public void setResource(HDFSResource resource) {
    this.resource = resource;
  }
}
