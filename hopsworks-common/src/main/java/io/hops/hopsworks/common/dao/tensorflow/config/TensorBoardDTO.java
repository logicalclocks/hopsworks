/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.tensorflow.config;

import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.user.UsersDTO;
import io.hops.hopsworks.persistence.entity.tensorflow.TensorBoard;
import io.hops.hopsworks.common.project.ProjectDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TensorBoardDTO extends RestDTO<TensorBoardDTO> {

  @JsonIgnore
  private String cid;

  private String endpoint;

  private String mlId;

  private Date lastAccessed;

  private String hdfsLogdir;

  private ProjectDTO project;

  private UsersDTO user;

  public TensorBoardDTO(){

  }

  public TensorBoardDTO(TensorBoard tensorBoard) {
    this.cid = tensorBoard.getCid();
    this.endpoint = tensorBoard.getEndpoint();
    this.mlId = tensorBoard.getMlId();
    this.lastAccessed = tensorBoard.getLastAccessed();
    this.hdfsLogdir = tensorBoard.getHdfsLogdir();
  }

  public String getCid() {
    return cid;
  }

  public void setCid(String cid) {
    this.cid = cid;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getMlid() {
    return mlId;
  }

  public void setMlId(String mlId) {
    this.mlId = mlId;
  }

  public Date getLastAccessed() {
    return lastAccessed;
  }

  public void setLastAccessed(Date lastAccessed) {
    this.lastAccessed = lastAccessed;
  }

  public String getHdfsLogdir() {
    return hdfsLogdir;
  }

  public void setHdfsLogdir(String hdfsLogdir) {
    this.hdfsLogdir = hdfsLogdir;
  }

  public ProjectDTO getProject() {
    return project;
  }

  public void setProject(ProjectDTO project) {
    this.project = project;
  }

  public UsersDTO getUser() {
    return user;
  }

  public void setUser(UsersDTO user) {
    this.user = user;
  }
}
