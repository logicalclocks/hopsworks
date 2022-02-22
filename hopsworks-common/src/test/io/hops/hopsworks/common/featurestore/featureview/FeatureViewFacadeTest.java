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

import com.google.common.collect.Lists;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class FeatureViewFacadeTest {

  private FeatureViewFacade target = new FeatureViewFacade();

  @Test
  public void testRetainLatestVersion() {
    FeatureView featureView1_1 = createFeatureView("test1", 1);
    FeatureView featureView1_2 = createFeatureView("test1", 2);
    FeatureView featureView2_1 = createFeatureView("test2", 1);
    FeatureView featureView2_2 = createFeatureView("test2", 2);
    FeatureView featureView3_1 = createFeatureView("test3", 1);
    List<FeatureView> featureViews = Lists.newArrayList(
      featureView1_1, featureView1_2,
      featureView2_1, featureView2_2,
      featureView3_1
    );
    List<FeatureView> actual = target.retainLatestVersion(featureViews);
    List<FeatureView> expected = Lists.newArrayList(
      featureView1_2,
      featureView2_2,
      featureView3_1
    );
    assertEquals(actual.size(), expected.size());
    assertTrue(actual.containsAll(expected));
    assertTrue(expected.containsAll(actual));
  }

  public FeatureView createFeatureView(String name, Integer version) {
    FeatureView featureView = new FeatureView();
    featureView.setName(name);
    featureView.setVersion(version);
    return featureView;
  }

}