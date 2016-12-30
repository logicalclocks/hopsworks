package io.hops.hopsworks.common.dataset;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Provides information on the previewed file in Datasets.
 * <p>
 */
@XmlRootElement
public class FilePreviewDTO {

  private String type;
  private String content;
  private String extension;

  public FilePreviewDTO() {
  }

  public FilePreviewDTO(String type, String extension, String content) {
    this.type = type;
    this.extension = extension;
    this.content = content;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  @Override
  /**
   * Formats a JSON to be displayed by the browser.
   */
  public String toString() {
    return "{\"filePreviewDTO\":[{\"type\":\"" + type
            + "\", \"extension\":\"" + extension
            + "\", \"content\":\"" + content.replace("\\", "\\\\'").
            replace("\"", "\\\"").replace("\n", "\\n").
            replace("\r", "\\r").replace("\t", "\\t")
            + "\"}]}";
  }

}
