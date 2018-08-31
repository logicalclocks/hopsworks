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

import io.hops.hopsworks.common.dao.tensorflow.TensorBoard;
import io.hops.hopsworks.common.dao.user.UserDTO;
import io.hops.hopsworks.common.project.ProjectDTO;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.math.BigInteger;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class TensorBoardDTO {

  @JsonIgnore
  private BigInteger pid;

  private String endpoint;

  private String elasticId;

  private Date lastAccessed;

  private String hdfsLogdir;

  private ProjectDTO project;

  private UserDTO user;

  public TensorBoardDTO(){}

  public TensorBoardDTO(TensorBoard tensorBoard) {
    this.pid = tensorBoard.getPid();
    this.endpoint = tensorBoard.getEndpoint();
    this.elasticId = tensorBoard.getElasticId();
    this.lastAccessed = tensorBoard.getLastAccessed();
    this.hdfsLogdir = tensorBoard.getHdfsLogdir();
  }

  public TensorBoardDTO(BigInteger pid, int hdfsUserId, String endpoint, String elasticId, Date lastAccessed,
                        String hdfsLogdir) {
    this.pid = pid;
    this.endpoint = endpoint;
    this.elasticId = elasticId;
    this.lastAccessed = lastAccessed;
    this.hdfsLogdir = hdfsLogdir;
  }

  public BigInteger getPid() {
    return pid;
  }

  public void setPid(BigInteger pid) {
    this.pid = pid;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getElasticId() {
    return elasticId;
  }

  public void setElasticId(String elasticId) {
    this.elasticId = elasticId;
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

  public UserDTO getUser() {
    return user;
  }

  public void setUser(UserDTO user) {
    this.user = user;
  }
}
