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


import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConstraintResult {
  private String check;
  private ConstraintGroupLevel checkLevel;
  private ConstraintGroupLevel checkStatus;
  private String constraint;
  private ValidationResult.Status constraintStatus;
  private String constraintMessage;
  
  public ConstraintResult() {}
  
  public String getCheck() {
    return check;
  }
  
  public void setCheck(String check) {
    this.check = check;
  }
  
  public ConstraintGroupLevel getCheckLevel() {
    return checkLevel;
  }
  
  public void setCheckLevel(ConstraintGroupLevel checkLevel) {
    this.checkLevel = checkLevel;
  }
  
  public ConstraintGroupLevel getCheckStatus() {
    return checkStatus;
  }
  
  public void setCheckStatus(ConstraintGroupLevel checkStatus) {
    this.checkStatus = checkStatus;
  }
  
  public String getConstraint() {
    return constraint;
  }
  
  public void setConstraint(String constraint) {
    this.constraint = constraint;
  }
  
  public ValidationResult.Status getConstraintStatus() {
    return constraintStatus;
  }
  
  public void setConstraintStatus(ValidationResult.Status constraintStatus) {
    this.constraintStatus = constraintStatus;
  }
  
  public String getConstraintMessage() {
    return constraintMessage;
  }
  
  public void setConstraintMessage(String constraintMessage) {
    this.constraintMessage = constraintMessage;
  }
}
