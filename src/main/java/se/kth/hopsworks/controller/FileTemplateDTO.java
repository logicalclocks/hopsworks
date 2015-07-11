package se.kth.hopsworks.controller;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author vangelis
 */
@XmlRootElement
public class FileTemplateDTO {

  private String inodePath;
  private int templateId;

  public FileTemplateDTO() {
  }

  public FileTemplateDTO(String inodePath, int templateId) {
    this.inodePath = inodePath;
    this.templateId = templateId;
  }

  public void setInodePath(String inodePath) {
    this.inodePath = inodePath;
  }

  public void setTemplateId(int templateId) {
    this.templateId = templateId;
  }

  public String getInodePath() {
    return this.inodePath;
  }

  public int getTemplateId() {
    return this.templateId;
  }

  @Override
  public String toString() {
    return "FileTemplateDTO{" + "inodePath=" + this.inodePath + ", templateId="
            + this.templateId + "}";
  }
}
