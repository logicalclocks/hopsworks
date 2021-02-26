/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore.commit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommitDTO extends RestDTO<CommitDTO> {

  private Long commitID;
  private String commitDateString;
  private Long commitTime;
  private Long rowsInserted;
  private Long rowsUpdated;
  private Long rowsDeleted;
  private Integer validationId;

  public CommitDTO() {
  }

  public CommitDTO(Long commitID, String commitDateString, Long commitTime, Long rowsInserted,
                   Long rowsUpdated, Long rowsDeleted) {
    this.commitID = commitID;
    this.commitDateString = commitDateString;
    this.commitTime = commitTime;
    this.rowsInserted = rowsInserted;
    this.rowsUpdated = rowsUpdated;
    this.rowsDeleted = rowsDeleted;
  }

  public CommitDTO(Long commitID, String commitDateString, Long commitTime, Long rowsInserted,
                   Long rowsUpdated, Long rowsDeleted, Integer validationId) {
    this(commitID, commitDateString, commitTime, rowsInserted, rowsUpdated, rowsDeleted);
    this.validationId = validationId;
  }

  public Long getCommitID() {
    return commitID;
  }

  public void setCommitID(Long commitID) {
    this.commitID = commitID;
  }

  public String getCommitDateString() {
    return commitDateString;
  }

  public void setCommitDateString(String commitDateString) {
    this.commitDateString = commitDateString;
  }

  public Long getCommitTime() {
    return commitTime;
  }

  public void setCommitTime(Long commitTime) {
    this.commitTime = commitTime;
  }

  public Long getRowsInserted() {
    return rowsInserted;
  }

  public void setRowsInserted(Long rowsInserted) {
    this.rowsInserted = rowsInserted;
  }

  public Long getRowsUpdated() {
    return rowsUpdated;
  }

  public void setRowsUpdated(Long rowsUpdated) {
    this.rowsUpdated = rowsUpdated;
  }

  public Long getRowsDeleted() {
    return rowsDeleted;
  }

  public void setRowsDeleted(Long rowsDeleted) {
    this.rowsDeleted = rowsDeleted;
  }

  public Integer getValidationId() {
    return validationId;
  }

  public void setValidationId(Integer validationId) {
    this.validationId = validationId;
  }

  @Override
  public String toString() {
    return "CommitDTO{" + "commitID=" + commitID + ", commitDateString=" + commitDateString + ", rowsInserted="
        + rowsInserted + ", rowsUpdated=" + rowsUpdated + ", rowsDeleted=" + rowsDeleted
        + ", validationId=" + validationId + "}";
  }
}
