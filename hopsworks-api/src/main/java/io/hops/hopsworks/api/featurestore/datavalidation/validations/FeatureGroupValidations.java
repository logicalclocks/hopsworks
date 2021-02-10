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

package io.hops.hopsworks.api.featurestore.datavalidation.validations;

import io.hops.hopsworks.common.featurestore.featuregroup.ExpectationResult;

import java.util.List;

public class FeatureGroupValidations {
  
  private Long validationTime;
  private List<ExpectationResult> expectationResults;
  
  public FeatureGroupValidations() {
  }
  
  public Long getValidationTime() {
    return validationTime;
  }
  
  public void setValidationTime(Long validationTime) {
    this.validationTime = validationTime;
  }
  
  public List<ExpectationResult> getExpectationResults() {
    return expectationResults;
  }
  
  public void setExpectationResults(List<ExpectationResult> expectationResults) {
    this.expectationResults = expectationResults;
  }
  
  @Override
  public String toString() {
    return "FeatureGroupValidationDTO{" +
      "validationTime='" + validationTime + '\'' +
      ", expectationResults=" + expectationResults +
      '}';
  }
}
