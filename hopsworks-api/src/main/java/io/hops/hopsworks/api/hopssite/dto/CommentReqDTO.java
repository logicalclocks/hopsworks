package io.hops.hopsworks.api.hopssite.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CommentReqDTO {

  private String content;
  private DatasetReqDTO dataset;

  public CommentReqDTO() {
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public DatasetReqDTO getDataset() {
    return dataset;
  }

  public void setDataset(DatasetReqDTO dataset) {
    this.dataset = dataset;
  }

}