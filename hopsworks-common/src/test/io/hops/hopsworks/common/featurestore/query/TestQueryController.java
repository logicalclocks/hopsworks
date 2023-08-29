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

package io.hops.hopsworks.common.featurestore.query;

import com.google.common.collect.Lists;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupController;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.query.filter.FilterController;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.common.featurestore.query.join.JoinController;
import io.hops.hopsworks.common.featurestore.query.join.JoinDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.CachedFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.apache.calcite.sql.JoinType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TestQueryController {

  private Featurestore fs;

  private Featuregroup fg1;
  private Featuregroup fg2;
  private Featuregroup fg3;
  private Featuregroup fg4;
  private Featuregroup fgHudi;

  private CachedFeaturegroup cachedFeaturegroup;
  private CachedFeaturegroup hudiFeatureGroup;

  private List<Feature> fg1Features = new ArrayList<>();
  private List<Feature> fg2Features = new ArrayList<>();
  private List<Feature> fg3Features = new ArrayList<>();
  private List<Feature> fg4Features = new ArrayList<>();

  private List<FeatureGroupFeatureDTO> fg1FeaturesDTO = new ArrayList<>();
  private List<FeatureGroupFeatureDTO> fg2FeaturesDTO = new ArrayList<>();

  private List<SqlCondition> singleEqualsJoinOperator;

  private FeaturegroupController featuregroupController;
  private FeaturestoreController featurestoreController;
  private FeaturegroupFacade featuregroupFacade;
  private OnlineFeaturestoreController onlineFeaturestoreController;

  private QueryController target;
  private FilterController filterController;
  
  private Project project;
  private Users user;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    fs = new Featurestore();
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
    fg3 = new Featuregroup(3);
    fg3.setName("fg3");
    fg3.setVersion(1);
    fg3.setCachedFeaturegroup(cachedFeaturegroup);
    fg3.setFeaturestore(fs);
    fg4 = new Featuregroup(4);
    fg4.setName("fg4");
    fg4.setVersion(1);
    fg4.setCachedFeaturegroup(cachedFeaturegroup);
    fg4.setFeaturestore(fs);
    fgHudi = new Featuregroup(5);
    fgHudi.setName("fgHudi");
    fgHudi.setVersion(1);
    hudiFeatureGroup = new CachedFeaturegroup();
    hudiFeatureGroup.setTimeTravelFormat(TimeTravelFormat.HUDI);
    fgHudi.setCachedFeaturegroup(hudiFeatureGroup);
    fgHudi.setFeaturestore(fs);

    fg1Features = new ArrayList<>();
    fg1Features.add(new Feature("pr", "", true));
    fg1Features.add(new Feature("fg1_ft2", "", false));

    fg1FeaturesDTO = new ArrayList<>();
    fg1FeaturesDTO.add(new FeatureGroupFeatureDTO("pr", "Integer", "", true, false, "", null));
    fg1FeaturesDTO.add(new FeatureGroupFeatureDTO("fg1_ft2", "String", "", false, false, "", null));

    fg2Features = new ArrayList<>();
    fg2Features.add(new Feature("pr", "", true));
    fg2Features.add(new Feature("fg2_ft2", "", false));

    fg2FeaturesDTO = new ArrayList<>();
    fg2FeaturesDTO.add(new FeatureGroupFeatureDTO("pr", "Integer", "", true, false, "", null));
    fg2FeaturesDTO.add(new FeatureGroupFeatureDTO("fg2_ft2", "String", "", false, false, "", null));

    fg3Features = new ArrayList<>();
    fg3Features.add(new Feature("fg3_ft1", "", true));
    fg3Features.add(new Feature("fg3_ft2", "", false));

    fg4Features = new ArrayList<>();
    fg4Features.add(new Feature("pr", "fg4", true));
    fg4Features.add(new Feature("fg4_ft4_1", "fg4", "Float", null, "prefix4_"));
    fg4Features.add(new Feature("fg4_ft4_2", "fg4", "Float", null, "prefix4_"));
    fg4Features.add(new Feature("_hoodie_record_key", "fg4", "String", null, null));
    fg4Features.add(new Feature("_hoodie_partition_path", "fg4", "String", null, null));
    fg4Features.add(new Feature("_hoodie_commit_time", "fg4", "String", null, null));
    fg4Features.add(new Feature("_hoodie_file_name", "fg4", "String", null, null));
    fg4Features.add(new Feature("_hoodie_commit_seqno", "fg4", "String", null, null));

    singleEqualsJoinOperator = Arrays.asList(SqlCondition.EQUALS);

    featuregroupController = Mockito.mock(FeaturegroupController.class);
    featuregroupFacade = Mockito.mock(FeaturegroupFacade.class);
    featurestoreController = Mockito.mock(FeaturestoreController.class);
    onlineFeaturestoreController = Mockito.mock(OnlineFeaturestoreController.class);
    project = Mockito.mock(Project.class);
    user = Mockito.mock(Users.class);
    filterController = new FilterController(new ConstructorController());

    target = new QueryController(featuregroupController, featuregroupFacade, filterController, featurestoreController,
        onlineFeaturestoreController, null);
      new JoinController(new ConstructorController());
  }

  @Test
  public void testValidateFeatures() throws Exception {
    List<FeatureGroupFeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureGroupFeatureDTO("fg1_ft2"));

    List<Feature> extractedFeatures =
        target.validateFeatures(fg1, requestedFeatures, fg1Features);
    Assert.assertEquals(1, extractedFeatures.size());
    Assert.assertEquals("fg1_ft2", extractedFeatures.get(0).getName());
    // Make sure the object returned is the one for the DB with more infomation in (e.g. Type, Primary key)
    Assert.assertFalse(extractedFeatures.get(0).isPrimary());
  }

  @Test
  public void testMissingFeature() throws Exception {
    List<FeatureGroupFeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureGroupFeatureDTO("fg1_ft3"));

    thrown.expect(FeaturestoreException.class);
    target.validateFeatures(fg1, requestedFeatures, fg1Features);
  }

  @Test
  public void testExtractAllFeatures() throws Exception {
    List<FeatureGroupFeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureGroupFeatureDTO("*"));

    List<Feature> extractedFeatures =
        target.validateFeatures(fg1, requestedFeatures, fg1Features);
    // Make sure both features have been returned.
    Assert.assertEquals(2, extractedFeatures.size());
  }

  @Test
  public void testRemoveDuplicateColumns() throws Exception {
    List<Feature> joinLeft = new ArrayList<>();
    joinLeft.add(new Feature("ft1","fg0", "Float", null, null));

    List<Feature> availableLeft = new ArrayList<>(joinLeft);
    availableLeft.add(new Feature("ft2", "fg0", "int", null, null));

    List<Feature> joinRight = new ArrayList<>();
    joinRight.add(new Feature("ft1","fg1", "Float", null, null));

    List<Feature> availableRight = new ArrayList<>(joinRight);
    availableRight.add(new Feature("ft2", "fg1", "int", null, null));
    availableRight.add(new Feature("ft3", "fg1", "int", null, null));

    Query rightQuery = new Query("fs1", "project_fs1", fg2, "fg0", availableRight, availableRight);
    Query leftQuery = new Query("fs1", "project_fs1", fg1, "fg1", availableLeft, availableLeft);
    Join join = new Join(leftQuery, rightQuery, joinLeft, joinRight, JoinType.INNER, null, singleEqualsJoinOperator);
    leftQuery.setJoins(Arrays.asList(join));

    target.removeDuplicateColumns(leftQuery, false);
    Assert.assertEquals(2, rightQuery.getFeatures().size());
    Assert.assertEquals("ft2", rightQuery.getFeatures().get(0).getName());
    Assert.assertEquals("ft3", rightQuery.getFeatures().get(1).getName());
  }

  @Test
  public void testRemoveDuplicateColumnsPrefix() throws Exception {
    List<Feature> joinLeft = new ArrayList<>();
    joinLeft.add(new Feature("ft1","fg0", "Float", null, null));

    List<Feature> availableLeft = new ArrayList<>(joinLeft);
    availableLeft.add(new Feature("ft2", "fg0", "int", null, null));

    List<Feature> joinRight = new ArrayList<>();
    joinRight.add(new Feature("ft1","fg1", "Float", null, "right_"));

    List<Feature> availableRight = new ArrayList<>(joinRight);
    availableRight.add(new Feature("ft2", "fg1", "int", null, "right_"));
    availableRight.add(new Feature("ft3", "fg1", "int", null, "right_"));

    Query rightQuery = new Query("fs1", "project_fs1", fg2, "fg0", availableRight, availableRight);
    Query leftQuery = new Query("fs1", "project_fs1", fg1, "fg1", availableLeft, availableLeft);
    Join join = new Join(leftQuery, rightQuery, joinLeft, joinRight, JoinType.INNER, "right_",
      singleEqualsJoinOperator);
    leftQuery.setJoins(Arrays.asList(join));

    target.removeDuplicateColumns(leftQuery, false);
    Assert.assertEquals(3, rightQuery.getFeatures().size());
  }

  @Test
  public void testRemoveDuplicateColumnsPitEnabled() throws Exception {
    List<Feature> joinLeft = new ArrayList<>();
    joinLeft.add(new Feature("ft1","fg0", "Float", null, null));

    List<Feature> availableLeft = new ArrayList<>(joinLeft);
    availableLeft.add(new Feature("ft2", "fg0", "int", null, null));

    List<Feature> joinRight = new ArrayList<>();
    joinRight.add(new Feature("ft1","fg1", "Float", null, "right_"));

    List<Feature> availableRight = new ArrayList<>(joinRight);
    availableRight.add(new Feature("ft2", "fg1", "int", null, "right_"));
    availableRight.add(new Feature("ft3", "fg1", "int", null, "right_"));

    fg2.setEventTime("ft3");

    Query rightQuery = new Query("fs1", "project_fs1", fg2, "fg0", availableRight, availableRight);
    Query leftQuery = new Query("fs1", "project_fs1", fg1, "fg1", availableLeft, availableLeft);
    Join join = new Join(leftQuery, rightQuery, joinLeft, joinRight, JoinType.INNER, "right_",
      singleEqualsJoinOperator);
    leftQuery.setJoins(Arrays.asList(join));

    target.removeDuplicateColumns(leftQuery, true);
    Assert.assertEquals(3, rightQuery.getFeatures().size());
    Assert.assertEquals("ft1", rightQuery.getFeatures().get(0).getName());
    Assert.assertEquals("ft2", rightQuery.getFeatures().get(1).getName());
    Assert.assertEquals("ft3", rightQuery.getFeatures().get(2).getName());
  }

  @Test
  public void testCheckNestedJoin_nestedJoinNotExist() throws Exception {
    FeaturegroupDTO fg1 = new FeaturegroupDTO();
    fg1.setId(1);
    FeaturegroupDTO fg2 = new FeaturegroupDTO();
    fg2.setId(2);

    List<FeatureGroupFeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureGroupFeatureDTO("*"));

    QueryDTO rightQueryDTO = new QueryDTO(fg2, requestedFeatures);
    JoinDTO joinDTO = new JoinDTO(rightQueryDTO, null, null);

    QueryDTO queryDTO = new QueryDTO(fg1, requestedFeatures, Arrays.asList(joinDTO));
    target.checkNestedJoin(queryDTO);
  }

  @Test(expected = FeaturestoreException.class)
  public void testCheckNestedJoin_nestedJoinExist() throws Exception {
    // fg1 join (fg2 join fg3)
    FeaturegroupDTO fg1 = new FeaturegroupDTO();
    fg1.setId(1);
    FeaturegroupDTO fg2 = new FeaturegroupDTO();
    fg2.setId(2);
    FeaturegroupDTO fg3 = new FeaturegroupDTO();
    fg2.setId(3);

    List<FeatureGroupFeatureDTO> requestedFeatures = new ArrayList<>();
    requestedFeatures.add(new FeatureGroupFeatureDTO("*"));

    QueryDTO queryDTO2 = new QueryDTO(fg2, requestedFeatures);
    JoinDTO joinDTO2 = new JoinDTO(queryDTO2, null, null);

    QueryDTO queryDTO3 = new QueryDTO(fg3, requestedFeatures);
    queryDTO3.setJoins(Lists.newArrayList(joinDTO2));
    JoinDTO joinDTO3 = new JoinDTO(queryDTO3, null, null);


    QueryDTO queryDTO = new QueryDTO(fg1, requestedFeatures, Arrays.asList(joinDTO3));
    target.checkNestedJoin(queryDTO);
  }
}
