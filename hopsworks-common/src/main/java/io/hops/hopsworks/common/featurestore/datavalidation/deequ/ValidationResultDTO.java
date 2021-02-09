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

package io.hops.hopsworks.common.featurestore.datavalidation.deequ;

import io.hops.hopsworks.common.api.RestDTO;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class ValidationResultDTO extends RestDTO<ValidationResultDTO> {
  @XmlEnum
  public enum Status {
    Success,
    Warning,
    Failure,
    Empty
  }
  
  private Status status;
  private List<ConstraintResultDTO> constraintsResult;
  
  public ValidationResultDTO() {}
  
  public ValidationResultDTO(Status status, List<ConstraintResultDTO> constraintsResult) {
    this.status = status;
    this.constraintsResult = constraintsResult;
  }
  
  public Status getStatus() {
    return status;
  }
  
  public void setStatus(Status status) {
    this.status = status;
  }
  
  public List<ConstraintResultDTO> getConstraintsResult() {
    return constraintsResult;
  }
  
  public void setConstraintsResult(
      List<ConstraintResultDTO> constraintsResult) {
    this.constraintsResult = constraintsResult;
  }
}
