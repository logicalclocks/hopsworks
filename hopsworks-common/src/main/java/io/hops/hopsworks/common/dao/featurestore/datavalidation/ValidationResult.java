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

package io.hops.hopsworks.common.dao.featurestore.datavalidation;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class ValidationResult {
  @XmlEnum
  enum Status {
    Success,
    Warning,
    Failure,
    Empty
  }
  
  private Status status;
  private List<ConstraintResult> constraintsResult;
  
  public ValidationResult() {}
  
  public ValidationResult(Status status, List<ConstraintResult> constraintsResult) {
    this.status = status;
    this.constraintsResult = constraintsResult;
  }
  
  public Status getStatus() {
    return status;
  }
  
  public void setStatus(Status status) {
    this.status = status;
  }
  
  public List<ConstraintResult> getConstraintsResult() {
    return constraintsResult;
  }
  
  public void setConstraintsResult(
      List<ConstraintResult> constraintsResult) {
    this.constraintsResult = constraintsResult;
  }
}
