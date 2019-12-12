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
package io.hops.hopsworks.api.jwt;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ElasticJWTResponseDTO {
  private String token;
  private String kibanaUrl;
  private String projectName;
  
  public ElasticJWTResponseDTO(){
  }
  
  public ElasticJWTResponseDTO(String token, String kibanaUrl, String projectName){
    this.token = token;
    this.kibanaUrl = kibanaUrl;
    this.projectName = projectName;
  }
  
  public String getToken() {
    return token;
  }
  
  public void setToken(String token) {
    this.token = token;
  }
  
  public String getKibanaUrl() {
    return kibanaUrl;
  }
  
  public void setKibanaUrl(String kibanaUrl) {
    this.kibanaUrl = kibanaUrl;
  }
  
  public String getProjectName() {
    return projectName;
  }
  
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  @Override
  public String toString() {
    return "ElasticJWTResponseDTO{" +
        "token='" + token + '\'' +
        ", kibanaUrl='" + kibanaUrl + '\'' +
        ", projectName='" + projectName + '\'' +
        '}';
  }
}
