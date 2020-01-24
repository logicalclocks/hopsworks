/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.provenance.core.elastic;

import io.hops.hopsworks.common.elastic.ElasticClient;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.unit.TimeValue;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ProvElasticController {
  private static final Logger LOG = Logger.getLogger(ProvElasticController.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private ElasticClient client;

  public GetIndexResponse mngIndexGet(GetIndexRequest request) throws ElasticException {
    GetIndexResponse response;
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = client.getClient().indices().get(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String msg = "elastic index:" + request.indices() + "error during index get";
      LOG.log(Level.WARNING, msg, e);
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        msg, e.getMessage(), e);
    }
    return response;
  }
  
  public Map<String, Map<String, String>> mngIndexGetMappings(String indexRegex) throws ElasticException {
    GetMappingsRequest request = new GetMappingsRequest().indices(indexRegex);
    GetMappingsResponse response;
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = client.getClient().indices().getMapping(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String msg = "elastic index:" + request.indices() + "error during index mapping get";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        msg, e.getMessage(), e);
    }
  
    Map<String, Map<String, String>> result = new HashMap<>();
    for(Map.Entry<String, MappingMetaData> e1 : response.mappings().entrySet()) {
      String index = e1.getKey();
      Map<String, String> mapping = parseMapping((Map)e1.getValue().sourceAsMap().get("properties"));
      result.put(index, mapping);
    }
    return result;
  }
  
  private Map<String, String> parseMapping(Map mapping) {
    Map<String, String> result = new HashMap<>();
    for(Map.Entry<String, Object> e1 : ((Map<String, Object>)mapping).entrySet()) {
      String key1 = e1.getKey();
      Map<String, Object> value = (Map)e1.getValue();
      if(value.containsKey("type")) {
        result.put(key1, (String)value.get("type"));
      } else if(value.containsKey("properties")) {
        Map<String, String> embeddedMapping = parseMapping((Map)value.get("properties"));
        for(Map.Entry<String, String> e2 : embeddedMapping.entrySet()) {
          String key2 = key1 + "." + e2.getKey();
          result.put(key2, e2.getValue());
        }
      }
    }
    return result;
  }
  
  public CreateIndexResponse mngIndexCreate(CreateIndexRequest request) throws ElasticException {
    if(request.index().length() > 255) {
      String msg = "elastic index name is too long:" + request.index();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    if(!request.index().equals(request.index().toLowerCase())) {
      String msg = "elastic index names can only contain lower case:" + request.index();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    CreateIndexResponse response;
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = client.getClient().indices().create(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String msg = "elastic index:" + request.index() + "error during index create";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        msg, e.getMessage(), e);
    }
    if(response.isAcknowledged()) {
      return response;
    } else {
      String msg = "elastic index:" + request.index() + "creation could not be acknowledged";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }
  
  public AcknowledgedResponse mngIndexDelete(DeleteIndexRequest request) throws ElasticException {
    AcknowledgedResponse response;
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = client.getClient().indices().delete(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String msg = "elastic index:" + request.indices()[0] + "error during index delete";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        msg, e.getMessage(), e);
    }
    if(response.isAcknowledged()) {
      return response;
    } else {
      String msg = "elastic index:" + request.indices()[0] + "deletion could not be acknowledged";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }
  
  public <S, E extends Exception> S getDoc(GetRequest request, ElasticHitParser<S, E> resultParser)
    throws E, ElasticException {
    GetResponse response;
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = client.getClient().get(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String msg =  "error during get doc:" + request.id();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        msg, e.getMessage(), e);
    }
    if(response.isExists()) {
      return resultParser.apply(BasicElasticHit.instance(response));
    } else {
      return null;
    }
  }
  
  public void indexDoc(IndexRequest request) throws ElasticException {
    IndexResponse response;
    
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = client.getClient().index(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String msg = "error during index doc:" + request.id();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        msg, e.getMessage(), e);
    }
    if (response.status().getStatus() != 201) {
      String msg = "doc index - bad status response:" + response.status().getStatus();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }
  
  public void updateDoc(UpdateRequest request) throws ElasticException {
    UpdateResponse response;
    
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = client.getClient().update(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String msg = "error during update doc:" + request.id();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        msg, e.getMessage(), e);
    }
    if (response.status().getStatus() != 200) {
      String msg = "doc update - bad status response:" + response.status().getStatus();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }
  
  public <S, A, E extends Exception> Pair<Long, S> search(SearchRequest request,
    ElasticHitsHandler<?, S, A, E> resultParser) throws ElasticException, E {
    SearchResponse response;
    LOG.log(Level.FINE, "request:{0}", request.toString());
    response = searchBasicInt(request);
    resultParser.apply(response.getHits().getHits());
    return Pair.with(response.getHits().getTotalHits().value, resultParser.get());
  }
  
  public <S, A, E extends Exception> Pair<Long, S> searchScrolling(SearchRequest request,
    ElasticHitsHandler<?, S, A, E> resultParser) throws ElasticException, E {
    SearchResponse response;
    long leftover;
    LOG.log(Level.FINE, "request:{0}", request.toString());
    response = searchBasicInt(request);
    
    if(response.getHits().getTotalHits().value > settings.getElasticMaxScrollPageSize()) {
      String msg = "Elasticsearch query items size is too big: " + response.getHits().getTotalHits();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    long totalHits = response.getHits().getTotalHits().value;
    leftover = totalHits - response.getHits().getHits().length;
    resultParser.apply(response.getHits().getHits());
    
    while (leftover > 0) {
      SearchScrollRequest next = nextScrollPage(response.getScrollId());
      response = searchScrollingInt(next);
      leftover = leftover - response.getHits().getHits().length;
      resultParser.apply(response.getHits().getHits());
    }
    return Pair.with(totalHits, resultParser.get());
  }
  
  public long searchCount(SearchRequest request) throws ElasticException {
    SearchResponse response;
    LOG.log(Level.FINE, "request:{0}", request.toString());
    response = searchBasicInt(request);
    LOG.log(Level.FINE, "response:{0}", response.toString());
    return response.getHits().getTotalHits().value;
  }
  
  public <A extends ElasticAggregation, E extends Exception> Pair<Long, Map<A, List>> searchCount(
    SearchRequest request, Map<A, ElasticAggregationParser<?, E>> aggregations)
    throws ElasticException, E {
    SearchResponse response;
    LOG.log(Level.FINE, "request:{0}", request.toString());
    response = searchBasicInt(request);
    LOG.log(Level.FINE, "response:{0}", response.toString());
    Map<A, List> aggResults = new HashMap<>();
    if(!aggregations.isEmpty()) {
      for (Map.Entry<A, ElasticAggregationParser<?, E>> aggregation : aggregations.entrySet()) {
        aggResults.put(aggregation.getKey(), aggregation.getValue().apply(response.getAggregations()));
      }
    }
    return Pair.with(response.getHits().getTotalHits().value, aggResults);
  }
  
  private SearchResponse searchScrollingInt(SearchScrollRequest request) throws ElasticException {
    SearchResponse response;
    try {
      response = client.getClient().scroll(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String msg = "error querying elastic";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        msg, e.getMessage(), e);
    }
    if (response.status().getStatus() != 200) {
      String msg = "searchBasic query - bad status response:" + response.status().getStatus();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    return response;
  }
  
  private SearchScrollRequest nextScrollPage(String scrollId) {
    SearchScrollRequest ssr = new SearchScrollRequest(scrollId);
    ssr.scroll(TimeValue.timeValueMinutes(1));
    return ssr;
  }
  
  public void bulkDelete(BulkRequest request) throws ElasticException {
    BulkResponse response;
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = client.getClient().bulk(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String msg = "error during bulk delete";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        msg, e.getMessage(), e);
    }
    if(response.hasFailures()) {
      String msg = "failures during bulk delete";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }
  
  private SearchResponse searchBasicInt(SearchRequest request) throws ElasticException {
    SearchResponse response;
    try {
      response = client.getClient().search(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String msg = "error querying elastic index";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        msg, e.getMessage(), e);
    }
    if (response.status().getStatus() != 200) {
      String msg = "searchBasic query - bad status response:" + response.status().getStatus();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    return response;
  }
}
