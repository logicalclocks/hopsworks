/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.api.user;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.user.BbcGroup;

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
