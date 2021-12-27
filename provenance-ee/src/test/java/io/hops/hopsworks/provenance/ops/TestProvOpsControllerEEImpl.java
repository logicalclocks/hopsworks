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

package io.hops.hopsworks.provenance.ops;

import com.google.gson.Gson;
import com.lambdista.util.Try;
import io.hops.hopsworks.common.elastic.ElasticClientController;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturegroupXAttr;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticCache;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHits;
import io.hops.hopsworks.common.provenance.ops.ProvLinksParamBuilder;
import io.hops.hopsworks.common.provenance.ops.ProvOpsControllerIface;
import io.hops.hopsworks.common.provenance.ops.dto.ProvLinksDTO;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.common.provenance.state.ProvStateController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.hdfs.inode.InodePK;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.Matchers.*;

public class TestProvOpsControllerEEImpl {

  private ProvOpsControllerIface provOpsController;
  private ElasticClientController client;
  private Project project;
  private Map<String, ProvOpsControllerEEImpl.AppState> result;
  private List<ProvStateDTO> aliveList;
  private Map map, map1, map2, map3, map4, map5, map6, map7, map8, map9;

  public String constructSearchScrollingQuery(String jsonObject) {
    return "{\"size\":1,\"query\":{\"bool\":{\"must\":[{\"term\":{\"entry_type\":" +
            "{\"value\":\"operation\",\"boost\":1.0}}}" + jsonObject + "]," +
            "\"adjust_pure_negative\":true,\"boost\":1.0}}}";
  }

  public String listToTerms(List<String> values, String term) {
    List<String> objectString = new ArrayList<>();
    for (String value : values) {
      objectString.add("{\"term\":{\"" + term + "\":{\"value\":\"" + value + "\",\"boost\":1.0}}}");
    }
    return String.join(",", objectString);
  }

  public String constructJsonObject(String type, List<String> values, String term) {
    String termList = listToTerms(values, term);
    return ",{\"bool\":{\"" + type + "\":[" + termList + "],\"adjust_pure_negative\":true,\"boost\":1.0}}";
  }

  public String constructSearchQuery(List<String> values) {
    String termList = listToTerms(values, "ml_id");
    return "{\"from\":0,\"size\":" + values.size() + ",\"query\":{\"bool\":{\"must\":[{\"term\":{\"entry_type\":{\"value\":\"state\"," +
            "\"boost\":1.0}}},{\"bool\":{\"should\":[" + termList + "]," +
            "\"adjust_pure_negative\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}}}";
  }

  @Before
  public void setup() throws ElasticException {
    client = Mockito.mock(ElasticClientController.class);
    result = new HashMap<>();

    //region Create provenance graph
    ProvOpsControllerEEImpl.AppState appState_1 = new ProvOpsControllerEEImpl.AppState();
    ProvOpsDTO provOpsDTO_1_1 = new ProvOpsDTO();
    provOpsDTO_1_1.setDocSubType(ProvParser.DocSubType.FEATURE);
    provOpsDTO_1_1.setMlId("raw_fg1_1");
    appState_1.out.put(provOpsDTO_1_1.getMlId(), provOpsDTO_1_1);
    result.put("1", appState_1);

    ProvOpsControllerEEImpl.AppState appState_2 = new ProvOpsControllerEEImpl.AppState();
    ProvOpsDTO provOpsDTO_2_1 = new ProvOpsDTO();
    provOpsDTO_2_1.setDocSubType(ProvParser.DocSubType.FEATURE);
    provOpsDTO_2_1.setMlId("raw_fg2_1");
    appState_2.out.put(provOpsDTO_2_1.getMlId(), provOpsDTO_2_1);
    result.put("2", appState_2);

    ProvOpsControllerEEImpl.AppState appState_3 = new ProvOpsControllerEEImpl.AppState();
    ProvOpsDTO provOpsDTO_3_1 = new ProvOpsDTO();
    provOpsDTO_3_1.setDocSubType(ProvParser.DocSubType.FEATURE);
    provOpsDTO_3_1.setMlId("raw_fg3_1");
    appState_3.out.put(provOpsDTO_3_1.getMlId(), provOpsDTO_3_1);
    appState_3.in.put(provOpsDTO_1_1.getMlId(), provOpsDTO_1_1);
    result.put("3", appState_3);

    ProvOpsControllerEEImpl.AppState appState_4 = new ProvOpsControllerEEImpl.AppState();
    ProvOpsDTO provOpsDTO_4_1 = new ProvOpsDTO();
    provOpsDTO_4_1.setDocSubType(ProvParser.DocSubType.TRAINING_DATASET);
    provOpsDTO_4_1.setMlId("derived_td1_1");
    appState_4.out.put(provOpsDTO_4_1.getMlId(), provOpsDTO_4_1);
    appState_4.in.put(provOpsDTO_2_1.getMlId(), provOpsDTO_2_1);
    appState_4.in.put(provOpsDTO_3_1.getMlId(), provOpsDTO_3_1);
    result.put("4", appState_4);

    ProvOpsControllerEEImpl.AppState appState_5 = new ProvOpsControllerEEImpl.AppState();
    ProvOpsDTO provOpsDTO_5_1 = new ProvOpsDTO();
    provOpsDTO_5_1.setDocSubType(ProvParser.DocSubType.TRAINING_DATASET);
    provOpsDTO_5_1.setMlId("derived_td2_1");
    appState_5.out.put(provOpsDTO_5_1.getMlId(), provOpsDTO_5_1);
    appState_5.in.put(provOpsDTO_4_1.getMlId(), provOpsDTO_4_1);
    result.put("5", appState_5);

    ProvOpsControllerEEImpl.AppState appState_6 = new ProvOpsControllerEEImpl.AppState();
    ProvOpsDTO provOpsDTO_6_1 = new ProvOpsDTO();
    provOpsDTO_6_1.setDocSubType(ProvParser.DocSubType.TRAINING_DATASET);
    provOpsDTO_6_1.setMlId("derived_td3_1");
    appState_6.out.put(provOpsDTO_6_1.getMlId(), provOpsDTO_6_1);
    appState_6.in.put(provOpsDTO_4_1.getMlId(), provOpsDTO_4_1);
    result.put("6", appState_6);

    ProvOpsControllerEEImpl.AppState appState_7 = new ProvOpsControllerEEImpl.AppState();
    ProvOpsDTO provOpsDTO_7_1 = new ProvOpsDTO();
    provOpsDTO_7_1.setDocSubType(ProvParser.DocSubType.TRAINING_DATASET);
    provOpsDTO_7_1.setMlId("derived_td4_1");
    appState_7.out.put(provOpsDTO_7_1.getMlId(), provOpsDTO_7_1);
    appState_7.in.put(provOpsDTO_6_1.getMlId(), provOpsDTO_6_1);
    result.put("7", appState_7);
    //endregion

    //region Alive ProvStateDTO
    aliveList = new ArrayList<>();
    FeaturegroupXAttr.FullDTO fullDTO = new FeaturegroupXAttr.FullDTO();
    fullDTO.setFgType(FeaturegroupXAttr.FGType.CACHED);
    String xattr = new Gson().toJson(fullDTO);

    ProvStateDTO provStateDTO = new ProvStateDTO();
    provStateDTO.setMlId("raw_fg1_1");
    provStateDTO.setMlType(Provenance.MLType.FEATURE);
    provStateDTO.setXattrs(new HashMap<String, String>() {{
      put("featurestore", xattr);
    }});
    aliveList.add(provStateDTO);

    provStateDTO = new ProvStateDTO();
    provStateDTO.setMlId("raw_fg2_1");
    provStateDTO.setMlType(Provenance.MLType.FEATURE);
    provStateDTO.setXattrs(new HashMap<String, String>() {{
      put("featurestore", xattr);
    }});
    aliveList.add(provStateDTO);

    provStateDTO = new ProvStateDTO();
    provStateDTO.setMlId("raw_fg3_1");
    provStateDTO.setMlType(Provenance.MLType.FEATURE);
    provStateDTO.setXattrs(new HashMap<String, String>() {{
      put("featurestore", xattr);
    }});
    aliveList.add(provStateDTO);

    provStateDTO = new ProvStateDTO();
    provStateDTO.setMlId("derived_td1_1");
    provStateDTO.setMlType(Provenance.MLType.TRAINING_DATASET);
    provStateDTO.setXattrs(new HashMap<String, String>() {{
      put("featurestore", xattr);
    }});
    aliveList.add(provStateDTO);

    provStateDTO = new ProvStateDTO();
    provStateDTO.setMlId("derived_td2_1");
    provStateDTO.setMlType(Provenance.MLType.TRAINING_DATASET);
    provStateDTO.setXattrs(new HashMap<String, String>() {{
      put("featurestore", xattr);
    }});
    aliveList.add(provStateDTO);

    provStateDTO = new ProvStateDTO();
    provStateDTO.setMlId("derived_td3_1");
    provStateDTO.setMlType(Provenance.MLType.TRAINING_DATASET);
    provStateDTO.setXattrs(new HashMap<String, String>() {{
      put("featurestore", xattr);
    }});
    aliveList.add(provStateDTO);

    provStateDTO = new ProvStateDTO();
    provStateDTO.setMlId("derived_td4_1");
    provStateDTO.setMlType(Provenance.MLType.TRAINING_DATASET);
    provStateDTO.setXattrs(new HashMap<String, String>() {{
      put("featurestore", xattr);
    }});
    aliveList.add(provStateDTO);
    //endregion

    //region multiSearchScrolling responses
    //get by mlId 0
    map = new HashMap();
    ProvOpsControllerEEImpl.AppState appState = new ProvOpsControllerEEImpl.AppState();
    appState.out = result.get("4").out;
    map.put("4", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.in = result.get("5").in;
    map.put("5", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.in = result.get("6").in;
    map.put("6", appState);
    //get by appId 0
    map1 = new HashMap();
    map1.put("4", result.get("4"));
    //get by mlId 1
    map2 = new HashMap();
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.out = result.get("2").out;
    map2.put("2", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.out = result.get("3").out;
    map2.put("3", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.in = result.get("4").in;
    map2.put("4", appState);
    //get by appId 1
    map3 = new HashMap();
    map3.put("2", result.get("2"));
    map3.put("3", result.get("3"));
    //get by mlId 2
    map4 = new HashMap();
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.out = result.get("1").out;
    map4.put("1", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.in = result.get("2").in;
    map4.put("2", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.in = result.get("3").in;
    map4.put("3", appState);
    //get by appId 2
    map5 = new HashMap();
    map5.put("1", result.get("1"));

    //get by appId 0
    map6 = new HashMap();
    map6.put("5", result.get("5"));
    map6.put("6", result.get("6"));
    //get by mlId 1
    map7 = new HashMap();
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.out = result.get("5").out;
    map7.put("5", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.out = result.get("6").out;
    map7.put("6", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.in = result.get("7").in;
    map7.put("7", appState);
    //get by appId 1
    map8 = new HashMap();
    map8.put("7", result.get("7"));
    //get by mlId 2
    map9 = new HashMap();
    //endregion

    Settings settings = Mockito.mock(Settings.class);
    Mockito.stub(settings.getProvFileIndex(anyLong())).toReturn("1__file_prov");
    Mockito.stub(settings.getElasticDefaultScrollPageSize()).toReturn(1);
    Mockito.stub(settings.getElasticMaxScrollPageSize()).toReturn(5);
    Mockito.stub(settings.getProvenanceGraphMaxSize()).toReturn(100);

    ElasticCache cache = Mockito.mock(ElasticCache.class);
    Mockito.stub(cache.mngIndexGetMapping(anyString(), anyBoolean())).toReturn(new HashMap<>());

    ProvStateController provStateController = new ProvStateController(settings, client, cache);

    provOpsController = new ProvOpsControllerEEImpl(settings, client, provStateController);

    Inode inode = new Inode(new InodePK());
    inode.setId(1l);
    project = new Project("test_project", inode);
  }

  @Test
  public void testProvLinksOutArtifactOnlyApps() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("OUT_ARTIFACT:derived_td1_1");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(true)
            .linkType(false);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id") + constructJsonObject("must_not", Arrays.asList("none"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id") +  constructJsonObject("must_not", Arrays.asList("none"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg2_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type") + constructJsonObject("must_not", Arrays.asList("none"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg3_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type") + constructJsonObject("must_not", Arrays.asList("none"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("2"), "app_id") + constructJsonObject("must_not", Arrays.asList("none"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("3"), "app_id") + constructJsonObject("must_not", Arrays.asList("none"), "app_id"))));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map2))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map3))));
            //downstream

    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, false);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(1, provLinksDTO.getItems().size());
    Assert.assertEquals(2, provLinksDTO.getItems().get(0).getIn().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(0).getOut().size());
  }

  @Test
  public void testProvLinksOutArtifactLinkType() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("OUT_ARTIFACT:derived_td1_1");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(false)
            .linkType(true);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg2_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg3_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("2"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("3"), "app_id"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td1_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("raw_fg2_1", "raw_fg3_1")));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);
    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map2))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map3))));
    //downstream

    aliveList.removeAll(aliveList.stream().filter(s -> s.getMlId().equals("raw_fg2_1")).collect(Collectors.toList()));
    aliveList.removeAll(aliveList.stream().filter(s -> s.getMlId().equals("raw_fg3_1")).collect(Collectors.toList()));
    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < request.getAllValues().size(); i++) {
      JSONAssert.assertEquals(expectedAlive.get(i), request.getAllValues().get(i).source().toString(), false);
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(0, provLinksDTO.getItems().size());
  }

  @Test
  public void testProvLinksOutArtifact() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("OUT_ARTIFACT:derived_td1_1");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(false)
            .linkType(false);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg2_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg3_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("2"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("3"), "app_id"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td1_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("raw_fg2_1", "raw_fg3_1")));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);
    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map2))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map3))));
    //downstream

    aliveList.removeAll(aliveList.stream().filter(s -> s.getMlId().equals("raw_fg2_1")).collect(Collectors.toList()));
    aliveList.removeAll(aliveList.stream().filter(s -> s.getMlId().equals("raw_fg3_1")).collect(Collectors.toList()));
    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < request.getAllValues().size(); i++) {
      JSONAssert.assertEquals(expectedAlive.get(i), request.getAllValues().get(i).source().toString(), false);
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(1, provLinksDTO.getItems().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getIn().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(0).getOut().size());
  }

  @Test
  public void testProvLinksInArtifactOnlyApps() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("IN_ARTIFACT:derived_td1_1");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(true)
            .linkType(false);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id") + constructJsonObject("must_not", Arrays.asList("none"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id") + constructJsonObject("must_not", Arrays.asList("none"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id") + constructJsonObject("must_not", Arrays.asList("none"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("5"), "app_id") + constructJsonObject("must_not", Arrays.asList("none"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("6"), "app_id") + constructJsonObject("must_not", Arrays.asList("none"), "app_id"))));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            //downstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map6))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map7))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map8))));
    //downstream

    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, false);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(2, provLinksDTO.getItems().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(0).getIn().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(0).getOut().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(1).getIn().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(1).getOut().size());
  }

  @Test
  public void testProvLinksInArtifactLinkType() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("IN_ARTIFACT:derived_td1_1");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(false)
            .linkType(true);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("5"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("6"), "app_id"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td1_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td2_1", "derived_td3_1")));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);
    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            //downstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map6))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map7))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map8))));
    //downstream

    aliveList.removeAll(aliveList.stream().filter(s -> s.getMlId().equals("derived_td2_1")).collect(Collectors.toList()));
    aliveList.removeAll(aliveList.stream().filter(s -> s.getMlId().equals("derived_td3_1")).collect(Collectors.toList()));
    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < request.getAllValues().size(); i++) {
      JSONAssert.assertEquals(expectedAlive.get(i), request.getAllValues().get(i).source().toString(), false);
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(0, provLinksDTO.getItems().size());
  }

  @Test
  public void testProvLinksInArtifact() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("IN_ARTIFACT:derived_td1_1");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(false)
            .linkType(false);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("5"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("6"), "app_id"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td1_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td2_1", "derived_td3_1")));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);
    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            //downstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map6))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map7))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map8))));
    //downstream

    aliveList.removeAll(aliveList.stream().filter(s -> s.getMlId().equals("derived_td2_1")).collect(Collectors.toList()));
    aliveList.removeAll(aliveList.stream().filter(s -> s.getMlId().equals("derived_td3_1")).collect(Collectors.toList()));
    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < request.getAllValues().size(); i++) {
      JSONAssert.assertEquals(expectedAlive.get(i), request.getAllValues().get(i).source().toString(), false);
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(1, provLinksDTO.getItems().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getOut().size());
  }

  @Test
  public void testProvLinksAppIdOnlyApps() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("APP_ID:4");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(true)
            .linkType(false);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id") + constructJsonObject("must_not", Arrays.asList("none"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg2_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type") + constructJsonObject("must_not", Arrays.asList("none"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg3_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type") + constructJsonObject("must_not", Arrays.asList("none"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("2"), "app_id") + constructJsonObject("must_not", Arrays.asList("none"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("3"), "app_id") + constructJsonObject("must_not", Arrays.asList("none"), "app_id"))));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map2))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map3))));
    //downstream

    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, false);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(1, provLinksDTO.getItems().size());
    Assert.assertEquals(2, provLinksDTO.getItems().get(0).getIn().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(0).getOut().size());
  }

  @Test
  public void testProvLinksAppIdLinkType() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("APP_ID:4");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(false)
            .linkType(true);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg2_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg3_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("2"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("3"), "app_id"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td1_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("raw_fg2_1", "raw_fg3_1")));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);
    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map2))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map3))));
    //downstream

    aliveList.removeAll(aliveList.stream().filter(s -> s.getMlId().equals("raw_fg2_1")).collect(Collectors.toList()));
    aliveList.removeAll(aliveList.stream().filter(s -> s.getMlId().equals("raw_fg3_1")).collect(Collectors.toList()));
    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < request.getAllValues().size(); i++) {
      JSONAssert.assertEquals(expectedAlive.get(i), request.getAllValues().get(i).source().toString(), false);
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(0, provLinksDTO.getItems().size());
  }

  @Test
  public void testProvLinksAppId() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("APP_ID:4");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(false)
            .linkType(false);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg2_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg3_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("2"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("3"), "app_id"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td1_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("raw_fg2_1", "raw_fg3_1")));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);
    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map2))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map3))));
    //downstream

    aliveList.removeAll(aliveList.stream().filter(s -> s.getMlId().equals("raw_fg2_1")).collect(Collectors.toList()));
    aliveList.removeAll(aliveList.stream().filter(s -> s.getMlId().equals("raw_fg3_1")).collect(Collectors.toList()));
    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < request.getAllValues().size(); i++) {
      JSONAssert.assertEquals(expectedAlive.get(i), request.getAllValues().get(i).source().toString(), false);
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(1, provLinksDTO.getItems().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getIn().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(0).getOut().size());
  }

  @Test
  public void testProvLinksArtifactMaxUpstreamMaxDownstream() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("ARTIFACT:derived_td1_1");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(false)
            .linkType(false)
            .expand(-1, -1);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg2_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg3_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("2"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("3"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg1_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("1"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("5"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("6"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td2_1"), "ml_id") + constructJsonObject("should", Arrays.asList("TRAINING_DATASET_PART"), "ml_type")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td3_1"), "ml_id") + constructJsonObject("should", Arrays.asList("TRAINING_DATASET_PART"), "ml_type"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("7"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td4_1"), "ml_id") + constructJsonObject("should", Arrays.asList("TRAINING_DATASET_PART"), "ml_type"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td1_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("raw_fg2_1", "raw_fg3_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("raw_fg1_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td2_1", "derived_td3_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td4_1")));
    //endregion

    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map2))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map3))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map4))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map5))))
            //downstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map6))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map7))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map8))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map9))));
    //endregion

    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < request.getAllValues().size(); i++) {
      JSONAssert.assertEquals(expectedAlive.get(i), request.getAllValues().get(i).source().toString(), false);
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(1, provLinksDTO.getItems().size());
    //depth 0
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getIn().size());
    Assert.assertEquals(2, provLinksDTO.getItems().get(0).getDownstreamLinks().size());
    Assert.assertEquals(2, provLinksDTO.getItems().get(0).getUpstreamLinks().size());
    //Upstream
    //depth 1
    //1
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getUpstreamLinks().size());
    //2
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getDownstreamLinks().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().size());
    //depth 2
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().get(0).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().get(0).getUpstreamLinks().size());
    //Downstream
    //depth 1
    //1
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getUpstreamLinks().size());
    //2
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getIn().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getUpstreamLinks().size());
    //depth 2
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().get(0).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().get(0).getUpstreamLinks().size());
  }

  @Test
  public void testProvLinksArtifactMaxUpstream() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("ARTIFACT:derived_td1_1");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(false)
            .linkType(false)
            .expand(-1, 0);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg2_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg3_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("2"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("3"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg1_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("1"), "app_id"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td1_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("raw_fg2_1", "raw_fg3_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("raw_fg1_1")));
    //endregion

    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map2))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map3))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map4))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map5))))
            //downstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map6))));
    //endregion

    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < request.getAllValues().size(); i++) {
      JSONAssert.assertEquals(expectedAlive.get(i), request.getAllValues().get(i).source().toString(), false);
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(1, provLinksDTO.getItems().size());
    //depth 0
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().size());
    Assert.assertEquals(2, provLinksDTO.getItems().get(0).getUpstreamLinks().size());
    //Upstream
    //depth 1
    //1
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getUpstreamLinks().size());
    //2
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getDownstreamLinks().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().size());
    //depth 2
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().get(0).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().get(0).getUpstreamLinks().size());
  }

  @Test
  public void testProvLinksArtifactMaxDownstream() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("ARTIFACT:derived_td1_1");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(false)
            .linkType(false)
            .expand(0, -1);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("5"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("6"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td2_1"), "ml_id") + constructJsonObject("should", Arrays.asList("TRAINING_DATASET_PART"), "ml_type")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td3_1"), "ml_id") + constructJsonObject("should", Arrays.asList("TRAINING_DATASET_PART"), "ml_type"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("7"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td4_1"), "ml_id") + constructJsonObject("should", Arrays.asList("TRAINING_DATASET_PART"), "ml_type"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td1_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td2_1", "derived_td3_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td4_1")));
    //endregion

    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            //downstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map6))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map7))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map8))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map9))));
    //endregion

    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < request.getAllValues().size(); i++) {
      JSONAssert.assertEquals(expectedAlive.get(i), request.getAllValues().get(i).source().toString(), false);
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(1, provLinksDTO.getItems().size());
    //depth 0
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getIn().size());
    Assert.assertEquals(2, provLinksDTO.getItems().get(0).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().size());
    //Downstream
    //depth 1
    //1
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getUpstreamLinks().size());
    //2
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getIn().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getUpstreamLinks().size());
    //depth 2
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().get(0).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().get(0).getUpstreamLinks().size());
  }

  @Test
  public void testProvLinksArtifact1Upstream1Downstream() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("ARTIFACT:derived_td1_1");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(false)
            .linkType(false)
            .expand(1, 1);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg2_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("raw_fg3_1"), "ml_id") + constructJsonObject("should", Arrays.asList("FEATURE_PART"), "ml_type"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("2"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("3"), "app_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("5"), "app_id")),
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("6"), "app_id"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td1_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("raw_fg2_1", "raw_fg3_1")));
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td2_1", "derived_td3_1")));
    //endregion

    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map2))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map3))))
            //downstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map6))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map7))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map8))));
    //endregion

    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < request.getAllValues().size(); i++) {
      JSONAssert.assertEquals(expectedAlive.get(i), request.getAllValues().get(i).source().toString(), false);
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(1, provLinksDTO.getItems().size());
    //depth 0
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getIn().size());
    Assert.assertEquals(2, provLinksDTO.getItems().get(0).getDownstreamLinks().size());
    Assert.assertEquals(2, provLinksDTO.getItems().get(0).getUpstreamLinks().size());
    //Upstream
    //depth 1
    //1
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(0).getUpstreamLinks().size());
    //2
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().get(1).getUpstreamLinks().size());
    //Downstream
    //depth 1
    //1
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(0).getUpstreamLinks().size());
    //2
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().get(1).getUpstreamLinks().size());
  }

  @Test
  public void testProvLinksArtifact() throws Exception {
    //arrange
    Set<String> filterBy = new HashSet<>();
    filterBy.add("ARTIFACT:derived_td1_1");

    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
            .filterByFields(filterBy)
            .onlyApps(false)
            .linkType(false)
            .expand(0, 0);

    //region expected
    List<List<String>> expectedMultiScrollingQuery = new ArrayList<>();
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("derived_td1_1"), "ml_id"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    constructJsonObject("should", Arrays.asList("4"), "app_id"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery(Arrays.asList("derived_td1_1")));
    //endregion

    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(any(), any()))
            //upstream
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map))))
            .thenReturn(Arrays.asList(Pair.with(1l, new Try.Success<>(map1))));
            //downstream
    //endregion

    Try provStateDTOs = new Try.Success<>(aliveList);
    Mockito.stub(client.search(any(), any())).toReturn(Pair.with(1l, provStateDTOs));

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < multiRequest.getAllValues().size(); i++) {
      for (int j = 0; j < multiRequest.getAllValues().get(i).requests().size(); j++) {
        JSONAssert.assertEquals(expectedMultiScrollingQuery.get(i).get(j), multiRequest.getAllValues().get(i).requests().get(j).source().toString(), false);
      }
    }

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(ElasticHits.Handler.class));
    for (int i = 0; i < request.getAllValues().size(); i++) {
      JSONAssert.assertEquals(expectedAlive.get(i), request.getAllValues().get(i).source().toString(), false);
    }

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(1, provLinksDTO.getItems().size());
    //depth 0
    Assert.assertNotNull(provLinksDTO.getItems().get(0).getRoot());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getOut().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getIn().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getDownstreamLinks().size());
    Assert.assertEquals(0, provLinksDTO.getItems().get(0).getUpstreamLinks().size());
  }
}
