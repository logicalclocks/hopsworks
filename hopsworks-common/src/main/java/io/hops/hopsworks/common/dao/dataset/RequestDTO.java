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

package io.hops.hopsworks.common.dao.dataset;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RequestDTO {

  private Integer inodeId;
  private Integer projectId;
  private String messageContent;

  public RequestDTO() {
  }

  public RequestDTO(Integer inodeId, Integer projectId, String message) {
    this.inodeId = inodeId;
    this.projectId = projectId;
    this.messageContent = message;
  }

  public Integer getInodeId() {
    return inodeId;
  }

  public void setInodeId(Integer inodeId) {
    this.inodeId = inodeId;
  }

  public Integer getProjectId() {
    return projectId;
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public String getMessageContent() {
    return messageContent;
  }

  public void setMessageContent(String message) {
    this.messageContent = message;
  }

}
