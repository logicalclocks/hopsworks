package io.hops.hopsworks.common.dao.user;

import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;

@XmlRootElement
public class BbcGroupDTO extends RestDTO<BbcGroupDTO> {
  
  private String groupName;
  private String groupDesc;
  private Integer gid;
  
  public BbcGroupDTO() {
  }
  
  public BbcGroupDTO(BbcGroup group, URI href) {
    this.groupName = group.getGroupName();
    this.groupDesc = group.getGroupDesc();
    this.gid = group.getGid();
    this.setHref(href);
  }
  
  public String getGroupName() {
    return groupName;
  }
  
  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }
  
  public String getGroupDesc() {
    return groupDesc;
  }
  
  public void setGroupDesc(String groupDesc) {
    this.groupDesc = groupDesc;
  }
  
  public Integer getGid() {
    return gid;
  }
  
  public void setGid(Integer gid) {
    this.gid = gid;
  }
}
