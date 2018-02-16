/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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