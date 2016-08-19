package se.kth.hopsworks.controller;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Provides information on the previewed file in Datasets.
 * <p>
 */
@XmlRootElement
public class FilePreviewDTO {

  private String fileType;
  private String content;

  public FilePreviewDTO() {
  }

  public FilePreviewDTO(String fileType, String content) {
    this.fileType = fileType;
    this.content = content;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public String toString() {
    return "FilePreviewDTO{" + "fileType=" + fileType + ", content=" + content
            + '}';
  }

}
