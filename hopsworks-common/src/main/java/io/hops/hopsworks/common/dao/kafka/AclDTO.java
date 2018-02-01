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
