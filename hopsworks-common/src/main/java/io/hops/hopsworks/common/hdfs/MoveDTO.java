package io.hops.hopsworks.common.hdfs;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MoveDTO {

  private int inodeId;
  private String destPath;

  public MoveDTO() {
  }

  
  public MoveDTO(int inodeId, String destPath) {
    this.inodeId = inodeId;
    this.destPath = destPath;
  }

  public String getDestPath() {
    return destPath;
  }

  public int getInodeId() {
    return inodeId;
  }

  public void setDestPath(String destPath) {
    this.destPath = destPath;
  }

  public void setInodeId(int inodeId) {
    this.inodeId = inodeId;
  }
  
  
}
