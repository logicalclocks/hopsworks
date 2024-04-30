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

package io.hops.hopsworks.vectordb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.http.client.config.RequestConfig;
import org.opensearch.OpenSearchException;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.CreateIndexResponse;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.client.indices.GetIndexResponse;
import org.opensearch.client.indices.PutMappingRequest;
import org.opensearch.common.Strings;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.QueryStringQueryBuilder;
import org.opensearch.index.reindex.BulkByScrollResponse;
import org.opensearch.index.reindex.DeleteByQueryRequest;
import org.opensearch.rest.RestStatus;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class OpensearchVectorDatabase implements VectorDatabase {

  private RestHighLevelClient client = null;
  private ObjectMapper objectMapper = new ObjectMapper();
  private static final Logger LOGGER = Logger.getLogger(
      OpensearchVectorDatabase.class.getName());
  private Integer requestTimeout = 60000;
  private Integer socketTimeout = 61000;
  private final Integer maxRetry = 10;

  private static final Map<String, String> dataTypeMap = ImmutableMap.<String, String>builder()
      .put("BOOLEAN", "byte")
      .put("TINYINT", "byte")
      .put("INT", "integer")
      .put("SMALLINT", "short")
      .put("BIGINT", "long")
      .put("FLOAT", "float")
      .put("DOUBLE", "double")
      .put("TIMESTAMP", "date")
      .put("DATE", "date")
      .put("STRING", "text")
      .put("ARRAY", "binary")
      .put("STRUCT", "binary")
      .put("BINARY", "binary")
      .put("MAP", "binary")
      .build();

  public static String getDataType(String offlineType) {
    if (!Strings.isNullOrEmpty(offlineType)) {
      offlineType = offlineType.split("<")[0];
    } else {
      return null;
    }
    return dataTypeMap.get(offlineType.toUpperCase());
  }

  public OpensearchVectorDatabase(RestHighLevelClient client) {
    this.client = client;
  }

  public OpensearchVectorDatabase(RestHighLevelClient client, Integer requestTimeout) {
    this.client = client;
    this.requestTimeout = requestTimeout;
    this.socketTimeout = requestTimeout + 1000;
  }

  @Override
  public void createIndex(Index index, String mapping, Boolean skipIfExist) throws VectorDatabaseException {
    long delayMillis = 1000;  // Initial delay between retries (1 second)
    try {
      if (skipIfExist) {
        Request request = new Request("HEAD", "/" + index.getName());
        Response response = client.getLowLevelClient().performRequest(request);
        if (response.getStatusLine().getStatusCode() == 200) {
          return;
        }
      }

      for (int i = 0; i < maxRetry; i++) {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(index.getName());
        createIndexRequest.setTimeout(new TimeValue(requestTimeout));
        createIndexRequest.setMasterTimeout(new TimeValue(requestTimeout));
        RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(socketTimeout)
            .build();
        RequestOptions options = RequestOptions.DEFAULT.toBuilder()
            .setRequestConfig(requestConfig)
            .build();
        createIndexRequest.source(mapping, XContentType.JSON);
        CreateIndexResponse response = client.indices().create(createIndexRequest, options);
        if (response.isAcknowledged()) {
          return;
        }
        // Acknowledgement failed, log and retry
        LOGGER.log(Level.INFO, String.format("Create index [%s] not acknowledged, retrying...", index.getName()));
        Thread.sleep(getDelayMillis(delayMillis));
        delayMillis *= 2;
      }
    } catch (IOException e) {
      throw new VectorDatabaseException(String.format("Failed to create opensearch index: %s. Error: %s",
          index.getName(), e.getMessage()));
    } catch (InterruptedException e) {
      LOGGER.log(Level.INFO, String.format("Retry create index [%s] interrupted.", index.getName()));
    } catch (OpenSearchStatusException e) {
      if (e.getDetailedMessage().contains("process_cluster_event_timeout_exception")) {
        LOGGER.log(Level.INFO, String.format("Create index [%s] timeout, retrying...", index.getName()));
        try {
          Thread.sleep(getDelayMillis(delayMillis));
          delayMillis *= 2;
        } catch (InterruptedException e1) {
          LOGGER.log(Level.INFO, String.format("Retry create index [%s] interrupted.", index.getName()));
        }
      }
    }

    throw new VectorDatabaseException("Failed to create opensearch index: " + index.getName());
  }

  public Optional<Index> getIndex(String name) throws VectorDatabaseException {
    try {
      GetIndexRequest getIndexRequest = new GetIndexRequest(name);
      GetIndexResponse getIndexResponse = client.indices().get(getIndexRequest, RequestOptions.DEFAULT);
      return getIndexResponse.getMappings().keySet().stream().map(Index::new).findFirst();
    } catch (IOException e) {
      throw new VectorDatabaseException("Failed to fetch opensearch indices. Err: " + e);
    } catch (OpenSearchStatusException e) {
      if (e.status() == RestStatus.NOT_FOUND) {
        return Optional.empty();
      } else {
        throw new VectorDatabaseException("Failed to fetch opensearch indices. Err: " + e.getDetailedMessage());
      }
    }
  }

  /**
   * Get all indices from OpenSearch.
   *
   * @return A set of index names.
   * @throws VectorDatabaseException If there is an error while fetching indices.
   */
  public Set<Index> getAllIndices() throws VectorDatabaseException {
    try {
      GetIndexRequest getIndexRequest = new GetIndexRequest("*"); // "*" retrieves all indices
      GetIndexResponse getIndexResponse = client.indices().get(getIndexRequest, RequestOptions.DEFAULT);
      return getIndexResponse.getMappings().keySet().stream().map(Index::new).collect(Collectors.toSet());
    } catch (IOException e) {
      throw new VectorDatabaseException("Failed to fetch opensearch indices. Err: " + e);
    }
  }

  @Override
  public void deleteIndex(Index index) throws VectorDatabaseException {
    long delayMillis = 1000;  // Initial delay between retries (1 second)

    for (int i = 0; i < maxRetry; i++) {
      try {
        DeleteIndexRequest deleteIndexRequest =
            new DeleteIndexRequest(index.getName())
                .timeout(new TimeValue(requestTimeout))
                .masterNodeTimeout(new TimeValue(requestTimeout));
        RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(socketTimeout)
            .build();
        RequestOptions options = RequestOptions.DEFAULT.toBuilder()
            .setRequestConfig(requestConfig)
            .build();
        AcknowledgedResponse response = client.indices().delete(deleteIndexRequest, options);
        if (response.isAcknowledged()) {
          return;
        }
        // Acknowledgement failed, log and retry
        LOGGER.log(Level.INFO, String.format("Delete index [%s] not acknowledged, retrying...", index.getName()));
        Thread.sleep(getDelayMillis(delayMillis));
        delayMillis *= 2;
      } catch (IOException e) {
        throw new VectorDatabaseException("Failed to delete opensearch index: " + index.getName() + "Err: " + e);
      } catch (InterruptedException e) {
        LOGGER.log(Level.INFO, String.format("Retry delete index [%s] interrupted.", index.getName()));
      } catch (OpenSearchStatusException e) {
        if (e.status() == RestStatus.NOT_FOUND) {
          // If index has been deleted, exit.
          return;
        } else if (e.getDetailedMessage().contains("process_cluster_event_timeout_exception")) {
          LOGGER.log(Level.INFO,
              String.format("Delete index [%s] timeout, retrying... \nError message: %s", index.getName(),
                  e.getDetailedMessage()));
          try {
            Thread.sleep(getDelayMillis(delayMillis));
            delayMillis *= 2;
          } catch (InterruptedException e1) {
            LOGGER.log(Level.INFO, String.format("Retry delete index [%s] interrupted.", index.getName()));
          }
        } else {
          throw new VectorDatabaseException("Failed to delete opensearch indices. Err: " + e.getDetailedMessage());
        }
      }
    }
    // All retries fail.
    throw new VectorDatabaseException("Failed to delete opensearch index: " + index.getName());
  }

  @Override
  public void addFields(Index index, String mapping) throws VectorDatabaseException {
    PutMappingRequest request = new PutMappingRequest(index.getName());
    request.source(mapping, XContentType.JSON);
    try {
      AcknowledgedResponse response = client.indices().putMapping(request, RequestOptions.DEFAULT);
      if (!response.isAcknowledged()) {
        throw new VectorDatabaseException("Failed to add fields to opensearch index: " + index.getName());
      }
    } catch (IOException e) {
      throw new VectorDatabaseException("Failed to add fields to opensearch index: " + index.getName() + "Err: " + e);
    }
  }

  @Override
  public List<Field> getSchema(Index index) throws VectorDatabaseException {
    // Create a GetIndexRequest
    GetIndexRequest request = new GetIndexRequest(index.getName());
    // Get the index mapping
    try {
      GetIndexResponse response = client.indices().get(request, RequestOptions.DEFAULT);
      Object mapping = response.getMappings().get(index.getName()).getSourceAsMap().getOrDefault("properties", null);
      if (mapping != null) {
        return ((Map<String, Object>) mapping).entrySet().stream()
            .map(entry -> new Field(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
      } else {
        return Lists.newArrayList();
      }
    } catch (IOException e) {
      throw new VectorDatabaseException("Failed to get schema from opensearch index: " + index.getName() + "Err: " + e);
    }
  }

  @Override
  public void write(Index index, String data, String docId) throws VectorDatabaseException {
    try {
      IndexRequest indexRequest = makeIndexRequest(index.getName(), data, docId);
      IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);
      if (!(response.status().equals(RestStatus.CREATED) || response.status().equals(RestStatus.OK))) {
        throw new VectorDatabaseException("Cannot index data. Status: " + response.status());
      }
    } catch (IOException | OpenSearchException e) {
      throw new VectorDatabaseException("Cannot index data. Err: " + e);
    }
  }

  private IndexRequest makeIndexRequest(String indexName, String data, String docId) {
    IndexRequest indexRequest = new IndexRequest(indexName)
        .source(data, XContentType.JSON);
    if (docId != null) {
      indexRequest.id(docId);
    }
    return indexRequest;
  }

  @Override
  public void write(Index index, String data) throws VectorDatabaseException {
    write(index, data, null);
  }

  @Override
  public void batchWrite(Index index, List<String> data) throws VectorDatabaseException {
    BulkRequest bulkRequest = new BulkRequest();
    for (String doc : data) {
      bulkRequest.add(
          makeIndexRequest(index.getName(), doc, null)
      );
    }
    bulkRequest(bulkRequest);
  }

  @Override
  public void batchWrite(Index index, Map<String, String> data) throws VectorDatabaseException {
    BulkRequest bulkRequest = new BulkRequest();
    for (Map.Entry<String, String> entry : data.entrySet()) {
      bulkRequest.add(
          makeIndexRequest(index.getName(), entry.getValue(), entry.getKey())
      );
    }
    bulkRequest(bulkRequest);
  }

  @Override
  public List<Map<String, Object>> preview(Index index, Set<Field> fields, int n) throws VectorDatabaseException {
    List<Map<String, Object>> results = Lists.newArrayList();

    // If fields is empty, return no result, otherwise the search query matches any documents which may not
    // belong to requested feature group.
    if (fields.size() == 0) {
      return results;
    }

    // Create a bool query
    BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

    // Add exists queries for each field
    for (Field field : fields) {
      boolQueryBuilder.must(QueryBuilders.existsQuery(field.getName()));
    }

    // Create a SearchSourceBuilder to define the search query
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.query(boolQueryBuilder);
    sourceBuilder.size(n);

    // Create a SearchRequest with the specified index and source builder
    SearchRequest searchRequest = new SearchRequest(index.getName());
    searchRequest.source(sourceBuilder);

    try {
      // Execute the search request
      SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
      // Process the search hits
      for (SearchHit hit : searchResponse.getHits().getHits()) {
        results.add(hit.getSourceAsMap());
      }

    } catch (IOException e) {
      throw new VectorDatabaseException("Error occurred while querying OpenSearch index");
    }

    return results;
  }

  private void bulkRequest(BulkRequest bulkRequest) throws VectorDatabaseException {
    try {
      BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
      if (response.hasFailures()) {
        // Do not include message from `response.buildFailureMessage()` as this method failed to execute.
        String msg = String.format("Index data failed partially. Response status %d", response.status().getStatus());
        throw new VectorDatabaseException(msg);
      }
    } catch (IOException e) {
      throw new VectorDatabaseException("Cannot index data. Err: " + e);
    }
  }

  @Override
  public void writeMap(Index index, Map<String, Object> data) throws VectorDatabaseException {
    writeMap(index, data, null);
  }

  @Override
  public void writeMap(Index index, Map<String, Object> data, String docId) throws VectorDatabaseException {
    try {
      write(index, objectMapper.writeValueAsString(data), docId);
    } catch (IOException e) {
      throw new VectorDatabaseException("Failed to index data because data cannot be written to String.");
    }
  }

  @Override
  public void batchWriteMap(Index index, List<Map<String, Object>> data) throws VectorDatabaseException {
    List<String> batchData = Lists.newArrayList();
    try {
      for (Map<String, Object> map : data) {
        batchData.add(objectMapper.writeValueAsString(map));
      }
    } catch (IOException e) {
      throw new VectorDatabaseException("Failed to index data because data cannot be written to String.");
    }
    batchWrite(index, batchData);
  }

  @Override
  public void batchWriteMap(Index index, Map<String, Map<String, Object>> data)
      throws VectorDatabaseException {
    Map<String, String> batchData = Maps.newHashMap();
    try {
      for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
        batchData.put(entry.getKey(), objectMapper.writeValueAsString(entry.getValue()));
      }
    } catch (IOException e) {
      throw new VectorDatabaseException("Failed to index data because data cannot be written to String.");
    }
    batchWrite(index, batchData);
  }

  @Override
  public void deleteByQuery(Index index, String query) throws VectorDatabaseException {
    long delayMillis = 1000;  // Initial delay between retries (1 second)
    for (int i = 0; i < maxRetry; i++) {
      DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(index.getName());
      deleteByQueryRequest.setQuery(new QueryStringQueryBuilder(query));
      deleteByQueryRequest.setTimeout(new TimeValue(requestTimeout));

      try {
        RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(socketTimeout)
            .build();
        RequestOptions options = RequestOptions.DEFAULT.toBuilder()
            .setRequestConfig(requestConfig)
            .build();
        BulkByScrollResponse response = client.deleteByQuery(deleteByQueryRequest, options);
        if (response.getBulkFailures().size() == 0) {
          return;
        }
        // Got failures, log and retry
        LOGGER.log(Level.INFO, String.format("Drop index [%s] failed partially, retrying...", index.getName()));
        Thread.sleep(getDelayMillis(delayMillis));
        delayMillis *= 2;
      } catch (IOException e) {
        throw new VectorDatabaseException("Failed to delete opensearch data. Error: " + e.getMessage());
      } catch (InterruptedException e) {
        LOGGER.log(Level.INFO, String.format("Retry drop index [%s] interrupted.", index.getName()));
      } catch (OpenSearchStatusException e) {
        if (e.getDetailedMessage().contains("process_cluster_event_timeout_exception")) {
          LOGGER.log(Level.INFO, String.format("Drop index [%s] timeout, retrying...", index.getName()));
          try {
            Thread.sleep(getDelayMillis(delayMillis));
            delayMillis *= 2;
          } catch (InterruptedException e1) {
            LOGGER.log(Level.INFO, String.format("Retry create index [%s] interrupted.", index.getName()));
          }
        }
      }
    }
    throw new VectorDatabaseException("Drop index failed: " + index.getName());
  }

  private long getDelayMillis(long delayMillis) {
    return Math.min(delayMillis, 5000);
  }

  @Override
  public void close() {
    if (client != null) {
      try {
        client.close();
        client = null;
      } catch (IOException e) {
        throw new OpenSearchException("Error while shuting down client");
      }
    }
  }
}
