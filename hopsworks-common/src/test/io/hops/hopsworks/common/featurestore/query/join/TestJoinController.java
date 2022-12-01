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

package io.hops.hopsworks.common.featurestore.query.join;

import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.apache.calcite.sql.JoinType;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.dialect.SparkSqlDialect;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestJoinController {
  
  private Featurestore fs;
  private Featuregroup fg1;
  private Featuregroup fg2;
  private CachedFeaturegroup cachedFeaturegroup;
  private JoinController joinController;
  
  @Before
  public void setup() {
    fs = new Featurestore();
    fs.setHiveDbId(1l);
    fs.setProject(new Project("test_proj"));
    cachedFeaturegroup = new CachedFeaturegroup();
    cachedFeaturegroup.setTimeTravelFormat(TimeTravelFormat.NONE);
    fg1 = new Featuregroup(1);
    fg1.setName("fg1");
    fg1.setVersion(1);
    fg1.setCachedFeaturegroup(cachedFeaturegroup);
    fg1.setFeaturestore(fs);
    fg2 = new Featuregroup(2);
    fg2.setName("fg2");
    fg2.setVersion(1);
    fg2.setCachedFeaturegroup(cachedFeaturegroup);
    fg2.setFeaturestore(fs);

    joinController = new JoinController(new ConstructorController());
  }
  
  
  @Test
  public void testThreeConditionsOn() {
    List<Feature> availableLeft = new ArrayList<>();
    availableLeft.add(new Feature("ft1", true));
    availableLeft.add(new Feature("ft2", true));
    availableLeft.add(new Feature("ft3", true));
    
    List<Feature> availableRight = new ArrayList<>();
    availableRight.add(new Feature("ft1", true));
    availableRight.add(new Feature("ft2", true));
    availableRight.add(new Feature("ft3", true));
    
    Query leftQuery = new Query("fs1", "project_fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", "project_fs1", fg2, "fg2", availableRight, availableRight);
    
    Join join = new Join(leftQuery, rightQuery, availableLeft, availableLeft, JoinType.INNER, null,
      Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS, SqlCondition.EQUALS));
    leftQuery.setJoins(Arrays.asList(join));
    
    SqlNode sqlNode = joinController.getLeftRightCondition(join, false);
    String sqlConditionStr = sqlNode.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).toString();
    Assert.assertEquals("`fg1`.`ft1` = `fg2`.`ft1` AND `fg1`.`ft2` = `fg2`.`ft2` AND `fg1`.`ft3` = `fg2`.`ft3`",
      sqlConditionStr);
  }

  @Test
  public void testThreeConditionsOnOnline() {
    List<Feature> availableLeft = new ArrayList<>();
    availableLeft.add(new Feature("ft1", true));
    availableLeft.add(new Feature("ft2", true));
    availableLeft.add(new Feature("ft3", true));

    List<Feature> availableRight = new ArrayList<>();
    availableRight.add(new Feature("ft1", true));
    availableRight.add(new Feature("ft2", true));
    availableRight.add(new Feature("ft3", true));

    Query leftQuery = new Query("fs1", "project_fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", "project_fs1", fg2, "fg2", availableRight, availableRight);

    Join join = new Join(leftQuery, rightQuery, availableLeft, availableLeft, JoinType.INNER, null,
        Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS, SqlCondition.EQUALS));
    leftQuery.setJoins(Arrays.asList(join));

    SqlNode sqlNode = joinController.getLeftRightCondition(join, true);
    String sqlConditionStr = sqlNode.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).toString();
    Assert.assertEquals("`fg1`.`ft1` = `fg2`.`ft1` AND `fg1`.`ft2` = `fg2`.`ft2` AND `fg1`.`ft3` = `fg2`.`ft3`",
        sqlConditionStr);
  }
  
  @Test
  public void testFourConditionsOn() {
    List<Feature> availableLeft = new ArrayList<>();
    availableLeft.add(new Feature("ft1", true));
    availableLeft.add(new Feature("ft2", true));
    availableLeft.add(new Feature("ft3", true));
    availableLeft.add(new Feature("ft4", true));
    
    List<Feature> availableRight = new ArrayList<>();
    availableRight.add(new Feature("ft1", true));
    availableRight.add(new Feature("ft2", true));
    availableRight.add(new Feature("ft3", true));
    availableRight.add(new Feature("ft4", true));
    
    Query leftQuery = new Query("fs1", "project_fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", "project_fs1", fg2, "fg2", availableRight, availableRight);
    
    Join join = new Join(leftQuery, rightQuery, availableLeft, availableLeft, JoinType.INNER, null,
      Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS, SqlCondition.EQUALS, SqlCondition.EQUALS));
    leftQuery.setJoins(Arrays.asList(join));
    
    SqlNode sqlNode = joinController.getLeftRightCondition(join, false);
    String sqlConditionStr = sqlNode.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).toString();
    Assert.assertEquals("`fg1`.`ft1` = `fg2`.`ft1` AND `fg1`.`ft2` = `fg2`.`ft2` " +
        "AND `fg1`.`ft3` = `fg2`.`ft3` AND `fg1`.`ft4` = `fg2`.`ft4`",
      sqlConditionStr);
  }

  @Test
  public void testFourConditionsOnOnline() {
    List<Feature> availableLeft = new ArrayList<>();
    availableLeft.add(new Feature("ft1", true));
    availableLeft.add(new Feature("ft2", true));
    availableLeft.add(new Feature("ft3", true));
    availableLeft.add(new Feature("ft4", true));

    List<Feature> availableRight = new ArrayList<>();
    availableRight.add(new Feature("ft1", true));
    availableRight.add(new Feature("ft2", true));
    availableRight.add(new Feature("ft3", true));
    availableRight.add(new Feature("ft4", true));

    Query leftQuery = new Query("fs1", "project_fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", "project_fs1", fg2, "fg2", availableRight, availableRight);

    Join join = new Join(leftQuery, rightQuery, availableLeft, availableLeft, JoinType.INNER, null,
        Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS, SqlCondition.EQUALS, SqlCondition.EQUALS));
    leftQuery.setJoins(Arrays.asList(join));

    SqlNode sqlNode = joinController.getLeftRightCondition(join, true);
    String sqlConditionStr = sqlNode.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).toString();
    Assert.assertEquals("`fg1`.`ft1` = `fg2`.`ft1` AND `fg1`.`ft2` = `fg2`.`ft2` " +
            "AND `fg1`.`ft3` = `fg2`.`ft3` AND `fg1`.`ft4` = `fg2`.`ft4`",
        sqlConditionStr);
  }

  @Test
  public void testWithDefaultValue() {
    List<Feature> availableLeft = new ArrayList<>();
    availableLeft.add(new Feature("ft1", "x", "varchar", true, "test", null));

    List<Feature> availableRight = new ArrayList<>();
    availableRight.add(new Feature("ft1", "y", "varchar", true, "test", null));

    Query leftQuery = new Query("fs1", "project_fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", "project_fs1", fg2, "fg2", availableRight, availableRight);

    Join join = new Join(leftQuery, rightQuery, availableLeft, availableRight, JoinType.INNER, null,
        Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS, SqlCondition.EQUALS, SqlCondition.EQUALS));
    leftQuery.setJoins(Arrays.asList(join));

    SqlNode sqlNode = joinController.getLeftRightCondition(join, false);
    String sqlConditionStr = sqlNode.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).toString();
    Assert.assertEquals("CASE WHEN `x`.`ft1` IS NULL THEN test ELSE `x`.`ft1` END = " +
            "CASE WHEN `y`.`ft1` IS NULL THEN test ELSE `y`.`ft1` END",
        sqlConditionStr);
  }

  @Test
  public void testOnlineWithDefaultValue() {
    List<Feature> availableLeft = new ArrayList<>();
    availableLeft.add(new Feature("ft1", "x", "varchar", true, "test", null));

    List<Feature> availableRight = new ArrayList<>();
    availableRight.add(new Feature("ft1", "y", "varchar", true, "test", null));

    Query leftQuery = new Query("fs1", "project_fs1", fg1, "fg1", availableLeft, availableLeft);
    Query rightQuery = new Query("fs1", "project_fs1", fg2, "fg2", availableRight, availableRight);

    Join join = new Join(leftQuery, rightQuery, availableLeft, availableRight, JoinType.INNER, null,
        Arrays.asList(SqlCondition.EQUALS, SqlCondition.EQUALS, SqlCondition.EQUALS, SqlCondition.EQUALS));
    leftQuery.setJoins(Arrays.asList(join));

    SqlNode sqlNode = joinController.getLeftRightCondition(join, true);
    String sqlConditionStr = sqlNode.toSqlString(new SparkSqlDialect(SqlDialect.EMPTY_CONTEXT)).toString();
    Assert.assertEquals("CASE WHEN `x`.`ft1` IS NULL THEN test ELSE `x`.`ft1` END = " +
            "CASE WHEN `y`.`ft1` IS NULL THEN test ELSE `y`.`ft1` END",
        sqlConditionStr);
  }
}
