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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation;

public enum Name {
  HAS_MEAN,
  HAS_MIN,
  HAS_MAX,
  HAS_SUM,
  HAS_SIZE,
  HAS_COMPLETENESS,
  HAS_UNIQUENESS,
  HAS_DISTINCTNESS,
  HAS_UNIQUE_VALUE_RATIO,
  HAS_NUMBER_OF_DISTINCT_VALUES,
  HAS_ENTROPY,
  HAS_MUTUAL_INFORMATION,
  HAS_APPROX_QUANTILE,
  HAS_STANDARD_DEVIATION,
  HAS_APPROX_COUNT_DISTINCT,
  HAS_CORRELATION,
  HAS_PATTERN,
  HAS_DATATYPE,
  IS_NON_NEGATIVE,
  IS_POSITIVE,
  IS_LESS_THAN,
  IS_LESS_THAN_OR_EQUAL_TO,
  IS_GREATER_THAN,
  IS_GREATER_THAN_OR_EQUAL_TO,
  IS_CONTAINED_IN
}
