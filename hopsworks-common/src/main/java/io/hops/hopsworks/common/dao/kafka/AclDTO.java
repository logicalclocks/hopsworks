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

package io.hops.hopsworks.common.dao.kafka;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class AclDTO implements Serializable {

  private Integer id;
  private String projectName;
  private String userEmail;
  private String permissionType;
  private String operationType;
  private String host;
  private String role;

  public AclDTO() {
  }

  public AclDTO(Integer id, String projectName, String userEmail,
      String permissionType, String operationType, String host, String role) {
    this.id = id;
    this.projectName = projectName;
    this.userEmail = userEmail;
    this.permissionType = permissionType;
    this.operationType = operationType;
    this.host = host;
    this.role = role;
  }
  
  public AclDTO(String projectName, String userEmail,
      String permissionType, String operationType, String host, String role) {
    this.projectName = projectName;
    this.userEmail = userEmail;
    this.permissionType = permissionType;
    this.operationType = operationType;
    this.host = host;
    this.role = role;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getHost() {
    return host;
  }

  public String getOperationType() {
    return operationType;
  }

  public String getPermissionType() {
    return permissionType;
  }

  public String getRole() {
    return role;
  }

  public String getUserEmail() {
    return userEmail;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }

  public void setPermissionType(String permissionType) {
    this.permissionType = permissionType;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }
}
