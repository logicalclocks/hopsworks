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

package io.hops.hopsworks.common.featurestore.featuregroup;

import io.hops.hopsworks.common.featurestore.datavalidation.Expectation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupValidation;

import java.util.List;

public class ExpectationResult {
  
  private FeatureGroupValidation.Status status;
  private Expectation expectation;
  private List<ValidationResult> results;

  public ExpectationResult() {
  }
  
  public FeatureGroupValidation.Status getStatus() {
    return status;
  }
  
  public void setStatus(FeatureGroupValidation.Status status) {
    this.status = status;
  }

  public Expectation getExpectation() {
    return expectation;
  }

  public void setExpectation(Expectation expectation) {
    this.expectation = expectation;
  }

  public List<ValidationResult> getResults() {
    return results;
  }

  public void setResults(List<ValidationResult> results) {
    this.results = results;
  }

  @Override
  public String toString() {
    return "ExpectationResult{"
      + "status=" + status
      + ", results=" + results
      + ", expectation=" + expectation
      + '}';
  }
}
