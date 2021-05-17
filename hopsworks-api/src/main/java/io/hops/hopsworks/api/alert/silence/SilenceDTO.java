/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.alert.silence;

import io.hops.hopsworks.alerting.api.alert.dto.Matcher;
import io.hops.hopsworks.alerting.api.alert.dto.SilenceStatus;
import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.List;

@XmlRootElement
public class SilenceDTO extends RestDTO<SilenceDTO> {
  
  private String id;
  private SilenceStatus status;
  private Date updatedAt;
  private String comment;
  private String createdBy;
  private Date endsAt;
  private Date startsAt;
  private List<Matcher> matchers;
  
  public SilenceDTO() {
  }
  
  public String getId() {
    return id;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public SilenceStatus getStatus() {
    return status;
  }
  
  public void setStatus(SilenceStatus status) {
    this.status = status;
  }
  
  public String getComment() {
    return comment;
  }
  
  public void setComment(String comment) {
    this.comment = comment;
  }
  
  public String getCreatedBy() {
    return createdBy;
  }
  
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }
  
  public Date getUpdatedAt() {
    return updatedAt;
  }
  
  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }
  
  public Date getEndsAt() {
    return endsAt;
  }
  
  public void setEndsAt(Date endsAt) {
    this.endsAt = endsAt;
  }
  
  public Date getStartsAt() {
    return startsAt;
  }
  
  public void setStartsAt(Date startsAt) {
    this.startsAt = startsAt;
  }
  
  public List<Matcher> getMatchers() {
    return matchers;
  }
  
  public void setMatchers(List<Matcher> matchers) {
    this.matchers = matchers;
  }
  
  @Override
  public String toString() {
    return "SilenceDTO{" +
        "id='" + id + '\'' +
        ", status=" + status +
        ", updatedAt='" + updatedAt + '\'' +
        ", comment='" + comment + '\'' +
        ", createdBy='" + createdBy + '\'' +
        ", endsAt='" + endsAt + '\'' +
        ", startsAt='" + startsAt + '\'' +
        ", matchers=" + matchers +
        '}';
  }
}
