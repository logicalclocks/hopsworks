/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.provenance.ops;

import com.google.gson.Gson;
import com.lambdista.util.Try;
import io.hops.hopsworks.common.opensearch.OpenSearchClientController;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturegroupXAttr;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchCache;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchHits;
import io.hops.hopsworks.common.provenance.ops.ProvLinksParamBuilder;
import io.hops.hopsworks.common.provenance.ops.ProvOpsControllerIface;
import io.hops.hopsworks.common.provenance.ops.dto.ProvLinksDTO;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.common.provenance.state.ProvStateController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.hdfs.inode.InodePK;
import io.hops.hopsworks.persistence.entity.project.Project;
import org.opensearch.action.search.MultiSearchRequest;
import org.opensearch.action.search.SearchRequest;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TestProvOpsControllerEEImpl {

  private ProvOpsControllerIface provOpsController;
  private OpenSearchClientController client;
  private Project project;
  private Inode projectInode;
  private Map map, map1, map2, map3, map4, map5, map6, map7, map8, map9;
  private Builder builder;

  public String constructSearchScrollingQuery(String jsonObject) {
    return "{\"size\":1,\"query\":{\"bool\":{\"must\":[{\"term\":{\"entry_type\":" +
            "{\"value\":\"operation\",\"boost\":1.0}}}" + jsonObject + "]," +
            "\"adjust_pure_negative\":true,\"boost\":1.0}}}";
  }

  public String listToTerms(String term, String... values) {
    List<String> objectString = new ArrayList<>();
    for (String value : values) {
      objectString.add("{\"term\":{\"" + term + "\":{\"value\":\"" + value + "\",\"boost\":1.0}}}");
    }
    return String.join(",", objectString);
  }
  
  public String constructJsonObject(String type, String term, String... values) {
    String termList = listToTerms(term, values);
    return ",{\"bool\":{\"" + type + "\":[" + termList + "],\"adjust_pure_negative\":true,\"boost\":1.0}}";
  }
  
  public String should(String term, String... values) {
    return constructJsonObject("should", term, values);
  }
  
  public String mustNot(String term, String... values) {
    return constructJsonObject("must_not", term, values);
  }

  public String constructSearchQuery(String... values) {
    String termList = listToTerms("ml_id", values);
    return "{\"from\":0,\"size\":" + values.length +
      ",\"query\":{\"bool\":{\"must\":[{\"term\":{\"entry_type\":{\"value\":\"state\"," +
      "\"boost\":1.0}}},{\"bool\":{\"should\":[" + termList + "]," +
      "\"adjust_pure_negative\":true,\"boost\":1.0}}],\"adjust_pure_negative\":true,\"boost\":1.0}}}";
  }

  private static class Builder {
    Map<String, ProvOpsDTO> ops = new HashMap<>();
    Map<String, ProvOpsControllerEEImpl.AppState> appStates = new HashMap<>();
    List<ProvStateDTO> aliveList = new ArrayList<>();
    
  }
  
  private void addAppState(Builder builder, String elasticId, String artifactId, ProvParser.DocSubType docType,
                           String... upstreamArtifactIds) {
    ProvOpsDTO provOpsDTO = new ProvOpsDTO();
    provOpsDTO.setDocSubType(docType);
    provOpsDTO.setMlId(artifactId);
    builder.ops.put(artifactId, provOpsDTO);
    
    ProvOpsControllerEEImpl.AppState appState = new ProvOpsControllerEEImpl.AppState();
    appState.out.put(provOpsDTO.getMlId(), provOpsDTO);
    for(String upstreamArtifactId : upstreamArtifactIds) {
      ProvOpsDTO aux = builder.ops.get(upstreamArtifactId);
      appState.in.put(upstreamArtifactId, aux);
    }
    builder.appStates.put(elasticId, appState);
  }
  
  private void addAliveState(Builder builder, String artifactId, Provenance.MLType artifactType, String xattr) {
    ProvStateDTO provStateDTO = new ProvStateDTO();
    provStateDTO.setMlId(artifactId);
    provStateDTO.setMlType(artifactType);
    provStateDTO.setXattrs(new HashMap<String, String>() {{ put("featurestore", xattr); }});
    builder.aliveList.add(provStateDTO);
  }
  
  @Before
  public void setup() throws OpenSearchException {
    client = Mockito.mock(OpenSearchClientController.class);
    
    builder = new Builder();
    
    //region Create provenance graph
    addAppState(builder, "1", "raw_fg1_1", ProvParser.DocSubType.FEATURE);
    addAppState(builder, "2", "raw_fg2_1", ProvParser.DocSubType.FEATURE);
    addAppState(builder, "3", "raw_fg3_1", ProvParser.DocSubType.FEATURE,
      "raw_fg1_1");
    addAppState(builder, "4", "derived_td1_1", ProvParser.DocSubType.TRAINING_DATASET,
      "raw_fg2_1", "raw_fg3_1");
    addAppState(builder, "5", "derived_td2_1", ProvParser.DocSubType.TRAINING_DATASET,
      "derived_td1_1");
    addAppState(builder, "6", "derived_td3_1", ProvParser.DocSubType.TRAINING_DATASET,
      "derived_td1_1");
    addAppState(builder, "7", "derived_td4_1", ProvParser.DocSubType.TRAINING_DATASET,
      "derived_td3_1");
    //endregion

    //region Alive ProvStateDTO
    FeaturegroupXAttr.FullDTO fullDTO = new FeaturegroupXAttr.FullDTO();
    fullDTO.setFgType(FeaturegroupXAttr.FGType.CACHED);
    String xattr = new Gson().toJson(fullDTO);
    addAliveState(builder, "raw_fg1_1", Provenance.MLType.FEATURE, xattr);
    addAliveState(builder, "raw_fg2_1", Provenance.MLType.FEATURE, xattr);
    addAliveState(builder, "raw_fg3_1", Provenance.MLType.FEATURE, xattr);
    
    fullDTO = new FeaturegroupXAttr.FullDTO();
    xattr = new Gson().toJson(fullDTO);
    addAliveState(builder, "derived_td1_1", Provenance.MLType.TRAINING_DATASET, xattr);
    addAliveState(builder, "derived_td2_1", Provenance.MLType.TRAINING_DATASET, xattr);
    addAliveState(builder, "derived_td3_1", Provenance.MLType.TRAINING_DATASET, xattr);
    addAliveState(builder, "derived_td4_1", Provenance.MLType.TRAINING_DATASET, xattr);
    //endregion
  
    ProvOpsControllerEEImpl.AppState appState = new ProvOpsControllerEEImpl.AppState();
    //region multiSearchScrolling responses
    //get by mlId 0
    map = new HashMap();
    appState.out = builder.appStates.get("4").out;
    map.put("4", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.in = builder.appStates.get("5").in;
    map.put("5", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.in = builder.appStates.get("6").in;
    map.put("6", appState);
    //get by appId 0
    map1 = new HashMap();
    map1.put("4", builder.appStates.get("4"));
    //get by mlId 1
    map2 = new HashMap();
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.out = builder.appStates.get("2").out;
    map2.put("2", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.out = builder.appStates.get("3").out;
    map2.put("3", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.in = builder.appStates.get("4").in;
    map2.put("4", appState);
    //get by appId 1
    map3 = new HashMap();
    map3.put("2", builder.appStates.get("2"));
    map3.put("3", builder.appStates.get("3"));
    //get by mlId 2
    map4 = new HashMap();
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.out = builder.appStates.get("1").out;
    map4.put("1", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.in = builder.appStates.get("2").in;
    map4.put("2", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.in = builder.appStates.get("3").in;
    map4.put("3", appState);
    //get by appId 2
    map5 = new HashMap();
    map5.put("1", builder.appStates.get("1"));

    //get by appId 0
    map6 = new HashMap();
    map6.put("5", builder.appStates.get("5"));
    map6.put("6", builder.appStates.get("6"));
    //get by mlId 1
    map7 = new HashMap();
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.out = builder.appStates.get("5").out;
    map7.put("5", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.out = builder.appStates.get("6").out;
    map7.put("6", appState);
    appState = new ProvOpsControllerEEImpl.AppState();
    appState.in = builder.appStates.get("7").in;
    map7.put("7", appState);
    //get by appId 1
    map8 = new HashMap();
    map8.put("7", builder.appStates.get("7"));
    //get by mlId 2
    map9 = new HashMap();
    //endregion

    Settings settings = Mockito.mock(Settings.class);
    Mockito.when(settings.getProvFileIndex(Mockito.anyLong())).thenReturn("1__file_prov");
    Mockito.when(settings.getOpenSearchDefaultScrollPageSize()).thenReturn(1);
    Mockito.when(settings.getOpenSearchMaxScrollPageSize()).thenReturn(5);
    Mockito.when(settings.getProvenanceGraphMaxSize()).thenReturn(100);
    
    OpenSearchCache cache = Mockito.mock(OpenSearchCache.class);
    Mockito.when(cache.mngIndexGetMapping(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(new HashMap<>());

    ProvStateController provStateController = new ProvStateController(settings, client, cache);

    provOpsController = new ProvOpsControllerEEImpl(settings, client, provStateController);

    projectInode = new Inode(new InodePK());
    projectInode.setId(1l);
    project = new Project("test_project");
  }

  private <O> List<Pair<Long, Try<O>>> searchResult(O result) {
    return Arrays.asList(Pair.with(1l, new Try.Success<>(result)));
  }
  
  private void compareSearchRequests(List<List<String>> expectedRequests,
                                     ArgumentCaptor<MultiSearchRequest> capturedRequests) {
    Assert.assertEquals(expectedRequests.size(), capturedRequests.getAllValues().size());
    for (int i = 0; i < capturedRequests.getAllValues().size(); i++) {
      List<SearchRequest> outgoingReq = capturedRequests.getAllValues().get(i).requests();
      List<String> expectedReq = expectedRequests.get(i);
      Assert.assertEquals(expectedReq.size(), outgoingReq.size());
      for (int j = 0; j < outgoingReq.size(); j++) {
        JSONAssert.assertEquals(expectedReq.get(j), outgoingReq.get(j).source().toString(), false);
      }
    }
  }
  
  @Test
  public void testNoResultArtifactLeftOne() throws Exception {
    ProvLinksParamBuilder params = noResultArtifactQueryParams().expand(-1, 0);
    testNoResult(params, false);
  }
  
  @Test
  public void testNoResultArtifactRightOne() throws Exception {
    ProvLinksParamBuilder params = noResultArtifactQueryParams().expand(0, -1);
    testNoResult(params, true);
  }
  
  @Test
  public void testNoResultArtifactBothOne() throws Exception {
    ProvLinksParamBuilder params = noResultArtifactQueryParams().expand(-1, -1);
    testNoResult(params, true);
  }
  
  private ProvLinksParamBuilder noResultArtifactQueryParams() throws ProvenanceException {
    Set<String> filterBy = new HashSet<>();
    filterBy.add("ARTIFACT:fg1_1");
    filterBy.add("ARTIFACT_TYPE:FEATURE");
    return new ProvLinksParamBuilder()
      .onlyApps(true)
      .linkType(true)
      .filterByFields(filterBy);
  }
  
  @Test
  public void testNoResultInArtifact() throws Exception {
    Set<String> filterBy = new HashSet<>();
    filterBy.add("IN_ARTIFACT:fg1_1");
    filterBy.add("IN_TYPE:FEATURE");
    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
      .onlyApps(true)
      .linkType(true)
      .filterByFields(filterBy);
    testNoResult(paramBuilder, true);
  }
  
  @Test
  public void testNoResultOutArtifact() throws Exception {
    Set<String> filterBy = new HashSet<>();
    filterBy.add("OUT_ARTIFACT:fg1_1");
    filterBy.add("OUT_TYPE:FEATURE");
    ProvLinksParamBuilder paramBuilder = new ProvLinksParamBuilder()
      .onlyApps(true)
      .linkType(true)
      .filterByFields(filterBy);
    testNoResult(paramBuilder, false);
  }
  
  /**
   * slight missed optimization, depending on query there might be a double query on root node
   */
  private void testNoResult(ProvLinksParamBuilder paramBuilder, boolean doubleRootQuery) throws Exception {
    List<List<String>> expectedQueries = new ArrayList<>();
    ArgumentCaptor<MultiSearchRequest> capturedRequests = ArgumentCaptor.forClass(MultiSearchRequest.class);
  
    String rootQuery = constructSearchScrollingQuery(
      should("ml_id", "fg1_1") +
        should("ml_type", "FEATURE", "FEATURE_PART") +
        mustNot( "app_id", "none"));
    expectedQueries.add(Arrays.asList(rootQuery));
    if(doubleRootQuery) {
      expectedQueries.add(Arrays.asList(rootQuery));
    }
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
      //upstream
      .thenReturn(searchResult(new HashMap<>()));
  
    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, false);
  
    //assert
    Mockito.verify(client, Mockito.times(expectedQueries.size()))
      .multiSearchScrolling(capturedRequests.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedQueries, capturedRequests);
  
    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(0, provLinksDTO.getItems().size());
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
                    should("ml_id", "derived_td1_1") +
                      mustNot( "app_id", "none"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should( "app_id", "4"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should( "ml_id", "raw_fg3_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART") +
                    mustNot("app_id", "none")),
            constructSearchScrollingQuery(
                    should( "ml_id", "raw_fg2_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART") +
                    mustNot("app_id", "none"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should( "app_id", "2")),
            constructSearchScrollingQuery(
                    should("app_id", "3"))));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map1))
            .thenReturn(searchResult(map2))
            .thenReturn(searchResult(map3));
            //downstream

    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, false);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

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
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "4"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg3_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART")),
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg2_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "2")),
            constructSearchScrollingQuery(
                    should("app_id", "3"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery("derived_td1_1"));
    expectedAlive.add(constructSearchQuery("raw_fg2_1", "raw_fg3_1"));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);
    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map1))
            .thenReturn(searchResult(map2))
            .thenReturn(searchResult(map3));
    //downstream

    builder.aliveList.removeAll(builder.aliveList.stream().filter(s -> s.getMlId().equals("raw_fg2_1")).collect(Collectors.toList()));
    builder.aliveList.removeAll(builder.aliveList.stream().filter(s -> s.getMlId().equals("raw_fg3_1")).collect(Collectors.toList()));
    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(OpenSearchHits.Handler.class));
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
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "4"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg3_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART")),
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg2_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "2")),
            constructSearchScrollingQuery(
                    should("app_id", "3"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery("derived_td1_1"));
    expectedAlive.add(constructSearchQuery("raw_fg2_1", "raw_fg3_1"));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);
    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map1))
            .thenReturn(searchResult(map2))
            .thenReturn(searchResult(map3));
    //downstream
  
    builder.aliveList.removeAll(builder.aliveList.stream().filter(s -> s.getMlId().equals("raw_fg2_1")).collect(Collectors.toList()));
    builder.aliveList.removeAll(builder.aliveList.stream().filter(s -> s.getMlId().equals("raw_fg3_1")).collect(Collectors.toList()));
    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(OpenSearchHits.Handler.class));
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
                    should("ml_id", "derived_td1_1") +
                    mustNot("app_id", "none"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "4"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should( "ml_id", "derived_td1_1") +
                    mustNot("app_id", "none"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "5")),
            constructSearchScrollingQuery(
                    should("app_id", "6"))));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map1))
            //downstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map6))
            .thenReturn(searchResult(map7))
            .thenReturn(searchResult(map8));
    //downstream

    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, false);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

    Assert.assertNotNull(provLinksDTO);
    Assert.assertEquals(1, provLinksDTO.getItems().size());
    Assert.assertEquals(1, provLinksDTO.getItems().get(0).getIn().size());
    Assert.assertEquals(2, provLinksDTO.getItems().get(0).getOut().size());
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
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "4"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "5")),
            constructSearchScrollingQuery(
                    should("app_id", "6"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery("derived_td1_1"));
    expectedAlive.add(constructSearchQuery("derived_td2_1", "derived_td3_1"));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);
    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map1))
            //downstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map6))
            .thenReturn(searchResult(map7))
            .thenReturn(searchResult(map8));
    //downstream
  
    builder.aliveList.removeAll(builder.aliveList.stream().filter(s -> s.getMlId().equals("derived_td2_1")).collect(Collectors.toList()));
    builder.aliveList.removeAll(builder.aliveList.stream().filter(s -> s.getMlId().equals("derived_td3_1")).collect(Collectors.toList()));
    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(OpenSearchHits.Handler.class));
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
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "4"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "5")),
            constructSearchScrollingQuery(
                    should("app_id", "6"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery("derived_td1_1"));
    expectedAlive.add(constructSearchQuery("derived_td2_1", "derived_td3_1"));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);
    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map1))
            //downstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map6))
            .thenReturn(searchResult(map7))
            .thenReturn(searchResult(map8));
    //downstream
  
    builder.aliveList.removeAll(builder.aliveList.stream().filter(s -> s.getMlId().equals("derived_td2_1")).collect(Collectors.toList()));
    builder.aliveList.removeAll(builder.aliveList.stream().filter(s -> s.getMlId().equals("derived_td3_1")).collect(Collectors.toList()));
    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(OpenSearchHits.Handler.class));
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
                    should("app_id", "4") +
                      mustNot("app_id", "none"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg3_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART") +
                    mustNot("app_id", "none")),
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg2_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART") +
                    mustNot("app_id", "none"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "2")),
            constructSearchScrollingQuery(
                    should("app_id", "3"))));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map1))
            .thenReturn(searchResult(map2))
            .thenReturn(searchResult(map3));
    //downstream

    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, false);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

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
                    should("app_id", "4"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg3_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART")),
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg2_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "2")),
            constructSearchScrollingQuery(
                    should("app_id", "3"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery("derived_td1_1"));
    expectedAlive.add(constructSearchQuery("raw_fg2_1", "raw_fg3_1"));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);
    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map1))
            .thenReturn(searchResult(map2))
            .thenReturn(searchResult(map3));
    //downstream
  
    builder.aliveList.removeAll(builder.aliveList.stream().filter(s -> s.getMlId().equals("raw_fg2_1")).collect(Collectors.toList()));
    builder.aliveList.removeAll(builder.aliveList.stream().filter(s -> s.getMlId().equals("raw_fg3_1")).collect(Collectors.toList()));
    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(OpenSearchHits.Handler.class));
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
                    should("app_id", "4"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg3_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART")),
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg2_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "2")),
            constructSearchScrollingQuery(
                    should("app_id", "3"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery("derived_td1_1"));
    expectedAlive.add(constructSearchQuery("raw_fg2_1", "raw_fg3_1"));
    //endregion

    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);
    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map1))
            .thenReturn(searchResult(map2))
            .thenReturn(searchResult(map3));
    //downstream
  
    builder.aliveList.removeAll(builder.aliveList.stream().filter(s -> s.getMlId().equals("raw_fg2_1")).collect(Collectors.toList()));
    builder.aliveList.removeAll(builder.aliveList.stream().filter(s -> s.getMlId().equals("raw_fg3_1")).collect(Collectors.toList()));
    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));
    //endregion

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(OpenSearchHits.Handler.class));
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
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "4"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg3_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART")),
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg2_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "2")),
            constructSearchScrollingQuery(
                    should("app_id", "3"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg1_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "5")),
            constructSearchScrollingQuery(
                    should("app_id", "6"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "derived_td2_1") +
                    should("ml_type", "TRAINING_DATASET", "TRAINING_DATASET_PART")),
            constructSearchScrollingQuery(
                    should("ml_id", "derived_td3_1") +
                    should("ml_type", "TRAINING_DATASET", "TRAINING_DATASET_PART"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "7"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "derived_td4_1") +
                    should("ml_type", "TRAINING_DATASET", "TRAINING_DATASET_PART"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery("derived_td1_1"));
    expectedAlive.add(constructSearchQuery("raw_fg2_1", "raw_fg3_1"));
    expectedAlive.add(constructSearchQuery("raw_fg1_1"));
    expectedAlive.add(constructSearchQuery("derived_td2_1", "derived_td3_1"));
    expectedAlive.add(constructSearchQuery("derived_td4_1"));
    //endregion

    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map1))
            .thenReturn(searchResult(map2))
            .thenReturn(searchResult(map3))
            .thenReturn(searchResult(map4))
            .thenReturn(searchResult(map5))
            //downstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map6))
            .thenReturn(searchResult(map7))
            .thenReturn(searchResult(map8))
            .thenReturn(searchResult(map9));
    //endregion

    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(OpenSearchHits.Handler.class));
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
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "4"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg3_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART")),
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg2_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "2")),
            constructSearchScrollingQuery(
                    should("app_id", "3"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg1_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "1"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery("derived_td1_1"));
    expectedAlive.add(constructSearchQuery("raw_fg2_1", "raw_fg3_1"));
    expectedAlive.add(constructSearchQuery("raw_fg1_1"));
    //endregion

    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map1))
            .thenReturn(searchResult(map2))
            .thenReturn(searchResult(map3))
            .thenReturn(searchResult(map4))
            .thenReturn(searchResult(map5))
            //downstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map6));
    //endregion

    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(OpenSearchHits.Handler.class));
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
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "4"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "5")),
            constructSearchScrollingQuery(
                    should("app_id", "6"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "derived_td2_1") +
                    should("ml_type", "TRAINING_DATASET", "TRAINING_DATASET_PART")),
            constructSearchScrollingQuery(
                    should("ml_id", "derived_td3_1") +
                    should("ml_type", "TRAINING_DATASET", "TRAINING_DATASET_PART"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "7"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "derived_td4_1") +
                    should("ml_type", "TRAINING_DATASET", "TRAINING_DATASET_PART"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery("derived_td1_1"));
    expectedAlive.add(constructSearchQuery("derived_td2_1", "derived_td3_1"));
    expectedAlive.add(constructSearchQuery("derived_td4_1"));
    //endregion

    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map1))
            //downstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map6))
            .thenReturn(searchResult(map7))
            .thenReturn(searchResult(map8))
            .thenReturn(searchResult(map9));
    //endregion

    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(OpenSearchHits.Handler.class));
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
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "4"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg3_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART")),
            constructSearchScrollingQuery(
                    should("ml_id", "raw_fg2_1") +
                    should("ml_type", "FEATURE", "FEATURE_PART"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "2")),
            constructSearchScrollingQuery(
                    should("app_id", "3"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "5")),
            constructSearchScrollingQuery(
                    should("app_id", "6"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery("derived_td1_1"));
    expectedAlive.add(constructSearchQuery("raw_fg2_1", "raw_fg3_1"));
    expectedAlive.add(constructSearchQuery("derived_td2_1", "derived_td3_1"));
    //endregion

    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map1))
            .thenReturn(searchResult(map2))
            .thenReturn(searchResult(map3))
            //downstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map6))
            .thenReturn(searchResult(map7))
            .thenReturn(searchResult(map8));
    //endregion

    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(OpenSearchHits.Handler.class));
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
                    should("ml_id", "derived_td1_1"))));
    expectedMultiScrollingQuery.add(Arrays.asList(
            constructSearchScrollingQuery(
                    should("app_id", "4"))));

    List<String> expectedAlive = new ArrayList<>();
    expectedAlive.add(constructSearchQuery("derived_td1_1"));
    //endregion

    ArgumentCaptor<SearchRequest> request = ArgumentCaptor.forClass(SearchRequest.class);
    ArgumentCaptor<MultiSearchRequest> multiRequest = ArgumentCaptor.forClass(MultiSearchRequest.class);

    //region Mock graph
    Mockito.when(client.multiSearchScrolling(Mockito.any(), Mockito.any()))
            //upstream
            .thenReturn(searchResult(map))
            .thenReturn(searchResult(map1));
            //downstream
    //endregion

    Try provStateDTOs = new Try.Success<>(builder.aliveList);
    Mockito.when(client.search(Mockito.any(), Mockito.any())).thenReturn(Pair.with(1l, provStateDTOs));

    //act
    ProvLinksDTO provLinksDTO = provOpsController.provLinks(project, projectInode, paramBuilder, true);

    //assert
    Mockito.verify(client, Mockito.times(expectedMultiScrollingQuery.size()))
            .multiSearchScrolling(multiRequest.capture(), Mockito.any(ProvOpsControllerEEImpl.HandlerFactory.class));
    compareSearchRequests(expectedMultiScrollingQuery, multiRequest);

    Mockito.verify(client, Mockito.times(expectedAlive.size()))
            .search(request.capture(), Mockito.any(OpenSearchHits.Handler.class));
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
