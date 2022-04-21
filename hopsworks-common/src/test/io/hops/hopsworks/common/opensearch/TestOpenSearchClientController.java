/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.opensearch;

import com.lambdista.util.Try;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchHits;
import io.hops.hopsworks.exceptions.OpenSearchException;
import org.apache.lucene.search.TotalHits;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchResponseSections;
import org.opensearch.action.search.ShardSearchFailure;
import org.opensearch.common.bytes.BytesArray;
import org.opensearch.common.bytes.BytesReference;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestOpenSearchClientController {

  private OpenSearchHits.Handler handler;
  private OpenSearchClientController client;
  private SearchHit createHit, readHit;

  @Before
  public void setup() {
    handler = Mockito.mock(OpenSearchHits.Handler.class);
    Mockito.when(handler.apply(Mockito.any())).thenReturn(new Try.Success(null));
    client = Mockito.spy(new OpenSearchClientController());

    BytesReference create = new BytesArray("{\n" +
            "    \"inode_id\" : 1,\n" +
            "    \"inode_operation\" : \"CREATE\",\n" +
            "    \"logical_time\" : 1,\n" +
            "    \"timestamp\" : 1,\n" +
            "    \"app_id\" : \"application_0001\",\n" +
            "    \"user_id\" : 1,\n" +
            "    \"project_i_id\" : 10018,\n" +
            "    \"dataset_i_id\" : 10041,\n" +
            "    \"parent_i_id\" : 10041,\n" +
            "    \"inode_name\" : \"raw_fg1_1\",\n" +
            "    \"project_name\" : \"test\",\n" +
            "    \"ml_id\" : \"raw_fg1_1\",\n" +
            "    \"ml_type\" : \"FEATURE\",\n" +
            "    \"entry_type\" : \"operation\",\n" +
            "    \"partition_id\" : 10041,\n" +
            "    \"r_timestamp\" : \"2021.Dec.29 8:10:29\"\n" +
            "}");
    createHit = new SearchHit(1);
    createHit.sourceRef(create);

    BytesReference read = new BytesArray("{\n" +
            "    \"inode_id\" : 1,\n" +
            "    \"inode_operation\" : \"ACCESS_DATA\",\n" +
            "    \"logical_time\" : 2,\n" +
            "    \"timestamp\" : 2,\n" +
            "    \"app_id\" : \"application_0001\",\n" +
            "    \"user_id\" : 1,\n" +
            "    \"project_i_id\" : 10018,\n" +
            "    \"dataset_i_id\" : 10041,\n" +
            "    \"parent_i_id\" : 10041,\n" +
            "    \"inode_name\" : \"raw_fg1_1\",\n" +
            "    \"project_name\" : \"test\",\n" +
            "    \"ml_id\" : \"raw_fg1_1\",\n" +
            "    \"ml_type\" : \"FEATURE\",\n" +
            "    \"entry_type\" : \"operation\",\n" +
            "    \"partition_id\" : 10041,\n" +
            "    \"r_timestamp\" : \"2021.Dec.29 8:10:30\"\n" +
            "}");
    readHit = new SearchHit(1);
    readHit.sourceRef(read);
  }

  private SearchResponse createResponse(SearchHit[] hitArray, int totalHits, String scrollId) {
    SearchHits hits = new SearchHits(hitArray, new TotalHits(totalHits, TotalHits.Relation.GREATER_THAN_OR_EQUAL_TO), 10);
    SearchResponseSections searchResponseSections = new SearchResponseSections(hits, null, null, false, null, null, 1);
    return new SearchResponse(searchResponseSections, scrollId, hitArray.length, hitArray.length, 0, 8l, new ShardSearchFailure[] {}, new SearchResponse.Clusters(0,0,0));
  }

  @Test
  public void testScrollingWithScrollId() throws OpenSearchException {
    //arrange
    int totalHits = 1;

    SearchRequest request = new SearchRequest();
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.size(totalHits);
    request.source(sourceBuilder);

    SearchResponse response = createResponse(new SearchHit[] { createHit }, totalHits, "");

    //region Mock
    Mockito.doReturn(null)
            .when(client).baseSearch(Mockito.any());

    Mockito.doReturn(null)
            .when(client).searchScrollingInt(Mockito.any());

    Mockito.doReturn(null)
            .when(client).clearScrollingContext(Mockito.any());
    //endregion

    //act
    Pair<Long, Try<Object>> result = client.scrolling(response, handler, request);

    //assert
    Assert.assertEquals(1, result.getValue0().intValue());
    Mockito.verify(client, Mockito.times(0)).baseSearch(Mockito.any());
    Mockito.verify(client, Mockito.times(0)).searchScrollingInt(Mockito.any());
    Mockito.verify(client, Mockito.times(1)).clearScrollingContext(Mockito.any());
  }

  @Test
  public void testScrollingWithoutScrollId() throws OpenSearchException {
    //arrange
    int totalHits = 1;

    SearchRequest request = new SearchRequest();
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.size(totalHits);
    request.source(sourceBuilder);

    SearchResponse response = createResponse(new SearchHit[] { createHit }, totalHits, null);

    //region Mock
    Mockito.doReturn(null)
            .when(client).baseSearch(Mockito.any());

    Mockito.doReturn(null)
            .when(client).searchScrollingInt(Mockito.any());

    Mockito.doReturn(null)
            .when(client).clearScrollingContext(Mockito.any());
    //endregion

    //act
    Pair<Long, Try<Object>> result = client.scrolling(response, handler, request);

    //assert
    Assert.assertEquals(1, result.getValue0().intValue());
    Mockito.verify(client, Mockito.times(0)).baseSearch(Mockito.any());
    Mockito.verify(client, Mockito.times(0)).searchScrollingInt(Mockito.any());
    Mockito.verify(client, Mockito.times(0)).clearScrollingContext(Mockito.any());
  }

  @Test
  public void testScrollingHasLeftoverWithScrollId() throws OpenSearchException {
    //arrange
    int totalHits = 2;

    SearchRequest request = new SearchRequest();
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.size(totalHits);
    request.source(sourceBuilder);

    SearchResponse response = createResponse(new SearchHit[] { createHit }, totalHits, "");

    //region Mock
    Mockito.doReturn(null)
            .when(client).baseSearch(Mockito.any());

    Mockito.doReturn(createResponse(new SearchHit[] { readHit }, totalHits, ""))
            .when(client).searchScrollingInt(Mockito.any());

    Mockito.doReturn(null)
            .when(client).clearScrollingContext(Mockito.any());
    //endregion

    //act
    Pair<Long, Try<Object>> result = client.scrolling(response, handler, request);

    //assert
    Assert.assertEquals(2, result.getValue0().intValue());
    Mockito.verify(client, Mockito.times(0)).baseSearch(Mockito.any());
    Mockito.verify(client, Mockito.times(1)).searchScrollingInt(Mockito.any());
    Mockito.verify(client, Mockito.times(1)).clearScrollingContext(Mockito.any());
  }

  @Test
  public void testScrollingHasLeftoverWithoutScrollId() throws OpenSearchException {
    //arrange
    int totalHits = 2;

    SearchRequest request = new SearchRequest();
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.size(totalHits);
    request.source(sourceBuilder);

    SearchResponse response = createResponse(new SearchHit[] { createHit }, totalHits, null);

    //region Mock
    Mockito.doReturn(createResponse(new SearchHit[] { createHit }, totalHits, ""))
            .when(client).baseSearch(Mockito.any());

    Mockito.doReturn(createResponse(new SearchHit[] { readHit }, totalHits, ""))
            .when(client).searchScrollingInt(Mockito.any());

    Mockito.doReturn(null)
            .when(client).clearScrollingContext(Mockito.any());
    //endregion

    //act
    Pair<Long, Try<Object>> result = client.scrolling(response, handler, request);

    //assert
    Assert.assertEquals(2, result.getValue0().intValue());
    Mockito.verify(client, Mockito.times(1)).baseSearch(Mockito.any());
    Mockito.verify(client, Mockito.times(1)).searchScrollingInt(Mockito.any());
    Mockito.verify(client, Mockito.times(1)).clearScrollingContext(Mockito.any());
  }
}