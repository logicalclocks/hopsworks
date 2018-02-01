/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.dela.old_hopssite_dto;

import io.hops.hopsworks.dela.dto.common.UserDTO;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DatasetIssueDTO {

  private String type;
  private String msg;
  private UserDTO.Complete user;
  private String publicDSId;

  public DatasetIssueDTO() {
  }

  public DatasetIssueDTO(String publicDSId, UserDTO.Complete user, String type, String msg) {
    this.type = type;
    this.msg = msg;
    this.user = user;
    this.publicDSId = publicDSId;
  }
  
  public UserDTO.Complete getUser() {
    return user;
  }

  public void setUser(UserDTO.Complete user) {
    this.user = user;
  }

  public String getPublicDSId() {
    return publicDSId;
  }

  public void setPublicDSId(String publicDSId) {
    this.publicDSId = publicDSId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

}