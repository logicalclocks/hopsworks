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

package io.hops.hopsworks.common.featurestore.featureview;

import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FeatureViewFilterByTest {

  @Test
  public void testGetSql_givenParam() {
    String actual = new FeatureViewFilterBy("name", "test1", "name1").getSql();
    String expected = FeatureView.TABLE_NAME_ALIAS + ".name = :name1 ";
    assertEquals(expected, actual);
  }

  @Test
  public void testGetSql_defaultParam() {
    String actual = new FeatureViewFilterBy("name").getSql();
    String expected = FeatureView.TABLE_NAME_ALIAS + ".name = :name ";
    assertEquals(expected, actual);
  }

}