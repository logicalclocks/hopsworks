/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.ca.controllers;

import io.hops.hadoop.shaded.com.google.gson.Gson;
import io.hops.hopsworks.ca.configuration.CAsConfiguration;
import io.hops.hopsworks.ca.configuration.IntermediateCAConfiguration;
import io.hops.hopsworks.ca.configuration.SubjectAlternativeName;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TestCAConfiguration {

  @Test
  public void testIntermediateCAWithExtraSAN() {
    Map<String, SubjectAlternativeName> extraSans = new HashMap<>();
    extraSans.put("hdfs",
        new SubjectAlternativeName(Arrays.asList("hdfs1.another.domain", "hdfs2.another.domain"), null));
    extraSans.put("yarn",
        new SubjectAlternativeName(Arrays.asList("yarn1.another.domain", "yarn2.another.domain"), null));
    IntermediateCAConfiguration caConf =
        new IntermediateCAConfiguration("CN=intermediate", "10d", extraSans);
    CAsConfiguration casConf = new CAsConfiguration(null, caConf, null);
    Gson gson = new Gson();
    String confJson = gson.toJson(casConf);
    /*
     * {
     *   "intermediateCA": {
     *     "extraUsernameSAN": {
     *       "hdfs": {
     *         "dns": [
     *           "hdfs1.another.domain",
     *           "hdfs2.another.domain"
     *         ]
     *       },
     *       "yarn": {
     *         "dns": [
     *           "yarn1.another.domain",
     *           "yarn2.another.domain"
     *         ]
     *       }
     *     },
     *     "x509Name": "CN=intermediate",
     *     "validityDuration": "10d"
     *   }
     * }
     */

    casConf = gson.fromJson(confJson, CAsConfiguration.class);
    Assert.assertTrue(casConf.getIntermediateCA().isPresent());
    caConf = casConf.getIntermediateCA().get();
    Assert.assertEquals(2, caConf.getExtraUsernameSAN().size());

    SubjectAlternativeName extraUsernameSan = caConf.getExtraUsernameSAN().get("hdfs");
    Assert.assertEquals(2, extraUsernameSan.getDns().get().size());
    Assert.assertTrue(extraUsernameSan.getDns().get().contains("hdfs1.another.domain"));
    Assert.assertTrue(extraUsernameSan.getDns().get().contains("hdfs2.another.domain"));

    extraUsernameSan = caConf.getExtraUsernameSAN().get("yarn");
    Assert.assertEquals(2, extraUsernameSan.getDns().get().size());
    Assert.assertTrue(extraUsernameSan.getDns().get().contains("yarn1.another.domain"));
    Assert.assertTrue(extraUsernameSan.getDns().get().contains("yarn2.another.domain"));

    extraUsernameSan = caConf.getExtraUsernameSAN().get("who");
    Assert.assertNull(extraUsernameSan);
  }

  @Test
  public void testIntermediateCAWithoutExtraSAN() {
    IntermediateCAConfiguration caConf =
        new IntermediateCAConfiguration("CN=intermediate", "10d");
    CAsConfiguration casConf = new CAsConfiguration(null, caConf, null);
    Gson gson = new Gson();
    String confJson = gson.toJson(casConf);

    casConf = gson.fromJson(confJson, CAsConfiguration.class);
    Assert.assertTrue(casConf.getIntermediateCA().get().getExtraUsernameSAN().isEmpty());
  }
}
