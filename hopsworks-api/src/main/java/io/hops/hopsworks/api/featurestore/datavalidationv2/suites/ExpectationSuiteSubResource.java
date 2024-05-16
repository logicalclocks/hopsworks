/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.featurestore.datavalidationv2.suites;

import io.hops.hopsworks.api.featurestore.featuregroup.FeatureGroupSubResource;
import io.hops.hopsworks.common.featurestore.datavalidationv2.suites.ExpectationSuiteController;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidationv2.ExpectationSuite;

public abstract class ExpectationSuiteSubResource extends FeatureGroupSubResource {
  private Integer expectationSuiteId;

  public Integer getExpectationSuiteId() {
    return expectationSuiteId;
  }

  public void setExpectationSuiteId(Integer expectationSuiteId) {
    this.expectationSuiteId = expectationSuiteId;
  }

  public ExpectationSuite getExpectationSuite() {
    return getExpectationSuiteController().getExpectationSuiteById(expectationSuiteId);
  }

  protected abstract ExpectationSuiteController getExpectationSuiteController();
}
