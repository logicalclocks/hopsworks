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

package io.hops.hopsworks.common.featurestore.featuregroup.cached;


import org.junit.Assert;
import org.junit.Test;

public class CachedFeaturegroupControllerTest {

  private CachedFeaturegroupController cachedFeaturegroupController = new CachedFeaturegroupController();

  @Test
  public void testPreviewWhereSingle() {
    String queryPart = "part_param=3";
    String output = cachedFeaturegroupController.getWhereCondition(queryPart);
    Assert.assertEquals("WHERE part_param='3'", output);
  }

  @Test
  public void testPreviewWhereDouble() {
    String queryPart = "part_param=3/part_param2=hello";
    String output = cachedFeaturegroupController.getWhereCondition(queryPart);
    Assert.assertEquals("WHERE part_param='3' AND part_param2='hello'", output);
  }

  @Test
  public void testPreviewWhereDoubleSpace() {
    String queryPart = "part_param=3 4/part_param2=hello";
    String output = cachedFeaturegroupController.getWhereCondition(queryPart);
    Assert.assertEquals("WHERE part_param='3 4' AND part_param2='hello'", output);
  }

  @Test
  public void testPreviewWhereDoubleEquals() {
    String queryPart = "part_param=3=4/part_param2=hello";
    String output = cachedFeaturegroupController.getWhereCondition(queryPart);
    Assert.assertEquals("WHERE part_param='3=4' AND part_param2='hello'", output);
  }
}