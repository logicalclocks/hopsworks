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

import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HiveSds;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.HiveTbls;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestFeatureGroupCommitController {

  private Featurestore fs;
  private Featuregroup fg1;
  private FeatureGroupCommit featureGroupCommit;
  private FeatureGroupCommitController featureGroupCommitController = new FeatureGroupCommitController();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    Inode inode = new Inode();
    HiveSds hiveSds = new HiveSds();
    hiveSds.setSdId(1l);
    hiveSds.setLocation("hopsfs://namenode.service.consul:8020/apps/hive/warehouse/test_proj_featurestore.db/fg1_1");
    hiveSds.setInode(inode);

    HiveTbls hiveTbls = new HiveTbls();
    hiveTbls.setSdId(hiveSds);
    hiveTbls.setTblName("fg1_1");

    CachedFeaturegroup cachedFeaturegroup = new CachedFeaturegroup();
    cachedFeaturegroup.setHiveTbls(hiveTbls);

    fs = new Featurestore();
    fs.setHiveDbId(1l);
    fs.setProject(new Project("test_proj"));

    fg1 = new Featuregroup(1);
    fg1.setName("fg1_1");
    fg1.setVersion(1);
    fg1.setFeaturestore(fs);
    fg1.setCachedFeaturegroup(cachedFeaturegroup);
  }

  @Test
  public void testComputeHudiCommitPath() {
    String expected =
        "hopsfs://namenode.service.consul:8020/apps/hive/warehouse/test_proj_featurestore.db/fg1_1/.hoodie/20201021000000.commit";
    String hudiCommitPath = featureGroupCommitController.computeHudiCommitPath(fg1, "20201021000000");

    Assert.assertEquals(expected, hudiCommitPath);
  }
}