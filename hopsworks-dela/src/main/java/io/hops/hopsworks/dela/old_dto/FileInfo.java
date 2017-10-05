package io.hops.hopsworks.dela.old_dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FileInfo {

  private String fileName;

  private String schema;

  private long length;

  public FileInfo() {
  }

  public FileInfo(String fileName, String schema, long length) {
    this.fileName = fileName;
    this.schema = schema;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public long getLength() {
    return length;
  }

  public void setLength(long length) {
    this.length = length;
  }

}
