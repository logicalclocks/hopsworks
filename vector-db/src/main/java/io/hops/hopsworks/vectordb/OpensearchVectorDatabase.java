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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.opensearch.OpenSearchException;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
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
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.index.query.QueryStringQueryBuilder;
import org.opensearch.index.reindex.BulkByScrollResponse;
import org.opensearch.index.reindex.DeleteByQueryRequest;
import org.opensearch.rest.RestStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class OpensearchVectorDatabase implements VectorDatabase {

  private RestHighLevelClient client = null;
  private ObjectMapper objectMapper = new ObjectMapper();
  private static final Logger LOGGER = Logger.getLogger(
      OpensearchVectorDatabase.class.getName());

  public OpensearchVectorDatabase(RestHighLevelClient client) {
    this.client = client;
  }

  @Override
  public void createIndex(Index index, String mapping, Boolean skipIfExist) throws VectorDatabaseException {
    try {
      if (skipIfExist) {
        Request request = new Request("HEAD", "/" + index.getName());
        Response response = client.getLowLevelClient().performRequest(request);
        if (response.getStatusLine().getStatusCode() == 200) {
          return;
        }
      }
      CreateIndexRequest createIndexRequest = new CreateIndexRequest(index.getName());
      createIndexRequest.source(mapping, XContentType.JSON);
      CreateIndexResponse response = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
      if (!response.isAcknowledged()) {
        throw new VectorDatabaseException("Failed to create opensearch index: " + index.getName());
      }
    } catch (IOException e) {
      throw new VectorDatabaseException("Failed to create opensearch index: " + index.getName() + "Err: " + e);
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
    try {
      DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(index.getName());
      AcknowledgedResponse response = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
      if (!response.isAcknowledged()) {
        throw new VectorDatabaseException("Failed to delete opensearch index: " + index.getName());
      }
    } catch (IOException e) {
      throw new VectorDatabaseException("Failed to delete opensearch index: " + index.getName() + "Err: " + e);
    }
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

  private void bulkRequest(BulkRequest bulkRequest) throws VectorDatabaseException {
    try {
      BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
      if (response.status().getStatus() != 200) {
        throw new VectorDatabaseException(
            String.format("Cannot index data. Response status %d; Message: %s",
                response.status().getStatus(), response.buildFailureMessage())
        );
      } else {
        if (response.hasFailures()) {
          throw new VectorDatabaseException(
              String.format("Index data failed partially. Response status %d; Message: %s",
                  response.status().getStatus(), response.buildFailureMessage())
          );
        }
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
    DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest(index.getName());
    deleteByQueryRequest.setQuery(new QueryStringQueryBuilder(query));
    try {
      BulkByScrollResponse response = client.deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
      if (response.getBulkFailures().size() != 0) {
        throw new VectorDatabaseException(
            "Drop index failed partially. Message: " +
                response.getBulkFailures()
                    .stream()
                    .map(f -> String.format("Index: %s , responseId: %s, status: %d, message: %s",
                        f.getIndex(), f.getId(), f.getStatus().getStatus(), f.getMessage()))
                    .collect(Collectors.joining("\t"))
        );
      }
    } catch (IOException e) {
      throw new VectorDatabaseException("Failed to delete opensearch data.");
    }
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
