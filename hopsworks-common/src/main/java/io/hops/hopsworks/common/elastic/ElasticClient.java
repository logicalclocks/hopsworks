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
package io.hops.hopsworks.common.elastic;

import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import io.hops.hopsworks.common.util.Ip;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryShardException;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.RemoteTransportException;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.javatuples.Pair;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An Elastic transport client shared by elastic controllers in hopsworks.
 * An Elastic transport client opens quite a few tcp connection that are kept alive for possibly minutes, and in test
 * cases for 50-100 parallel ongoing hopsworks elastic based requests it end up with upwards of 2000 tcp connections
 * used by elastic clients - this can also saturate the amount of tcp connection configured by the elastic node
 * ending up with a NoNodeAvailableException
 */
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ElasticClient {
  private static final Logger LOG = Logger.getLogger(ElasticClient.class.getName());
  
  @EJB
  private Settings settings;
  private Client elasticClient = null;
  
  @PostConstruct
  private void initClient() {
    try {
      getClient();
    } catch (ServiceException ex) {
      LOG.log(Level.SEVERE, "", ex);
    }
  }
  @PreDestroy
  private void closeClient(){
    shutdownClient();
  }
  
  /**
   * Shuts down the client
   * <p/>
   */
  private void shutdownClient() {
    if (elasticClient != null) {
      elasticClient.close();
      elasticClient = null;
    }
  }
  
  private synchronized Client getClient() throws ServiceException {
    if (elasticClient == null) {
      LOG.log(Level.INFO, "creating new elastic client");
      final org.elasticsearch.common.settings.Settings settings
        = org.elasticsearch.common.settings.Settings.builder()
        .put("client.transport.sniff", true) //being able to retrieve other nodes
        .put("cluster.name", "hops").build();
      
      List<String> elasticAddrs = getElasticIpsAsString();
      TransportClient _client = new PreBuiltTransportClient(settings);
      for(String addr : elasticAddrs){
        _client.addTransportAddress(new TransportAddress(new InetSocketAddress(addr, this.settings.getElasticPort())));
      }
      elasticClient = _client;
      
      Iterator<ThreadPool.Info> tpInfoIt = elasticClient.threadPool().info().iterator();
      StringBuilder tp = new StringBuilder();
      while(tpInfoIt.hasNext()) {
        ThreadPool.Info tpInfo = tpInfoIt.next();
        switch(tpInfo.getName()) {
          case ThreadPool.Names.BULK:
          case ThreadPool.Names.INDEX:
          case ThreadPool.Names.SEARCH:
            tp.append("name:").append(tpInfo.getName())
              .append("(")
              .append(tpInfo.getThreadPoolType())
              .append(",")
              .append(tpInfo.getMin())
              .append(",")
              .append(tpInfo.getMax())
              .append(",")
              .append(tpInfo.getQueueSize().singles())
              .append(")");
        }
      }
      LOG.log(Level.INFO, "threadpools {0}", tp);
    }
    return elasticClient;
  }
  
  private List<String> getElasticIpsAsString() throws ServiceException {
    List<String> addrs = settings.getElasticIps();
    
    for(String addr : addrs) {
      // Validate the ip address pulled from the variables
      if (!Ip.validIp(addr)) {
        try {
          InetAddress.getByName(addr);
        } catch (UnknownHostException ex) {
          throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_SERVER_NOT_AVAILABLE,
            Level.SEVERE, null, ex.getMessage(), ex);
        }
      }
    }
    return addrs;
  }
  
  private void processException(ServiceException ex) {
    LOG.log(Level.WARNING, "elastic client exception", ex);
    //decide if exception requires restart of client
    //shutdownClient();
  }
  //**************************************************************************************************************
  public GetIndexResponse mngIndexGet(GetIndexRequest request) throws ServiceException {
    GetIndexResponse response;
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = getClient().admin().indices().getIndex(request).get();
    } catch (InterruptedException | ExecutionException e) {
      String msg = "elastic index:" + request.indices() + "error during index get";
      ServiceException se = processException(e, msg);
      throw se;
    }
    return response;
  }
  
  public Map<String, Map<String, String>> mngIndexGetMappings(String indexRegex) throws ServiceException {
    GetIndexRequest request = new GetIndexRequest().indices(indexRegex);
    GetIndexResponse response = mngIndexGet(request);
  
    Map<String, Map<String, String>> result = new HashMap<>();
    Iterator<ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>>> it1
      = response.mappings().iterator();
    while(it1.hasNext()) {
      ObjectObjectCursor<String, ImmutableOpenMap<String, MappingMetaData>> current1 = it1.next();
      String index = current1.key;
      Iterator<ObjectObjectCursor<String, MappingMetaData>> it2 = current1.value.iterator();
      while(it2.hasNext()) {
        Map<String, String> mapping = parseMapping((Map)it2.next().value.getSourceAsMap().get("properties"));
        result.put(index, mapping);
      }
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
  
  public CreateIndexResponse mngIndexCreate(CreateIndexRequest request) throws ServiceException {
    if(request.index().length() > 255) {
      String msg = "elastic index name is too long:" + request.index();
      LOG.log(Level.INFO, msg);
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    if(!request.index().equals(request.index().toLowerCase())) {
      String msg = "elastic index names can only contain lower case:" + request.index();
      LOG.log(Level.INFO, msg);
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    CreateIndexResponse response;
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = getClient().admin().indices().create(request).get();
    } catch (InterruptedException | ExecutionException e) {
      String msg = "elastic index:" + request.index() + "error during index create";
      ServiceException se = processException(e, msg);
      throw se;
    }
    if(response.isAcknowledged()) {
      return response;
    } else {
      String msg = "elastic index:" + request.index() + "creation could not be acknowledged";
      LOG.log(Level.INFO, msg);
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }
  
  public DeleteIndexResponse mngIndexDelete(DeleteIndexRequest request) throws ServiceException {
    DeleteIndexResponse response;
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = getClient().admin().indices().delete(request).get();
    } catch (InterruptedException | ExecutionException e) {
      String msg = "elastic index:" + request.indices()[0] + "error during index delete";
      ServiceException se = processException(e, msg);
      throw se;
    }
    if(response.isAcknowledged()) {
      return response;
    } else {
      String msg = "elastic index:" + request.indices()[0] + "deletion could not be acknowledged";
      LOG.log(Level.INFO, msg);
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }
  
  public <S, E extends Exception> S getDoc(GetRequest request, ElasticHitParser<S, E> resultParser)
    throws ServiceException, E {
    GetResponse response;
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = getClient().get(request).get();
    } catch (InterruptedException | ExecutionException e) {
      String msg =  "error during get doc:" + request.id();
      ServiceException se = processException(e, msg);
      throw se;
    }
    if(response.isExists()) {
      return resultParser.apply(BasicElasticHit.instance(response));
    } else {
      return null;
    }
  }
  
  public void indexDoc(IndexRequest request) throws ServiceException {
    IndexResponse response;
    
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = getClient().index(request).get();
    } catch (InterruptedException | ExecutionException e) {
      String msg = "error during index doc:" + request.id();
      ServiceException se = processException(e, msg);
      throw se;
    }
    if (response.status().getStatus() != 201) {
      String msg = "doc index - bad status response:" + response.status().getStatus();
      LOG.log(Level.INFO, msg);
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }
  
  public void updateDoc(UpdateRequest request) throws ServiceException {
    UpdateResponse response;
    
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = getClient().update(request).get();
    } catch (InterruptedException | ExecutionException e) {
      String msg = "error during update doc:" + request.id();
      ServiceException se = processException(e, msg);
      throw se;
    }
    if (response.status().getStatus() != 200) {
      String msg = "doc update - bad status response:" + response.status().getStatus();
      LOG.log(Level.INFO, msg);
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }
  
  public <S, A, E extends Exception> Pair<Long, S> search(SearchRequest request,
    ElasticHitsHandler<?, S, A, E> resultParser) throws ServiceException, E {
    SearchResponse response;
    LOG.log(Level.FINE, "request:{0}", request.toString());
    response = searchBasicInt(request);
    resultParser.apply(response.getHits().getHits());
    return Pair.with(response.getHits().getTotalHits(), resultParser.get());
  }
  
  public <S, A, E extends Exception> Pair<Long, S> searchScrolling(SearchRequest request,
    ElasticHitsHandler<?, S, A, E> resultParser) throws ServiceException, E {
    SearchResponse response;
    long leftover;
    LOG.log(Level.FINE, "request:{0}", request.toString());
    response = searchBasicInt(request);
    
    if(response.getHits().getTotalHits() > settings.getElasticMaxScrollPageSize()) {
      String msg = "Elasticsearch query items size is too big: " + response.getHits().getTotalHits();
      LOG.log(Level.INFO, msg);
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    long totalHits = response.getHits().totalHits;
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
  
  public long searchCount(SearchRequest request) throws ServiceException {
    SearchResponse response;
    LOG.log(Level.FINE, "request:{0}", request.toString());
    response = searchBasicInt(request);
    LOG.log(Level.FINE, "response:{0}", response.toString());
    return response.getHits().getTotalHits();
  }
  
  public <A extends ElasticAggregation, E extends Exception> Pair<Long, Map<A, List>> searchCount(
    SearchRequest request, Map<A, ElasticAggregationParser<?, E>> aggregations)
    throws ServiceException, E {
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
    return Pair.with(response.getHits().getTotalHits(), aggResults);
  }
  
  private SearchResponse searchScrollingInt(SearchScrollRequest request)
    throws ServiceException {
    SearchResponse response;
    try {
      response = getClient().searchScroll(request).get();
    } catch (InterruptedException | ExecutionException e) {
      String msg = "error querying elastic";
      ServiceException se = processException(e, msg);
      throw se;
    }
    if (response.status().getStatus() != 200) {
      String msg = "searchBasic query - bad status response:" + response.status().getStatus();
      LOG.log(Level.INFO, msg);
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    return response;
  }
  
  private SearchScrollRequest nextScrollPage(String scrollId) {
    SearchScrollRequest ssr = new SearchScrollRequest(scrollId);
    ssr.scroll(TimeValue.timeValueMinutes(1));
    return ssr;
  }
  
  public void bulkDelete(BulkRequest request) throws ServiceException {
    BulkResponse response;
    try {
      LOG.log(Level.FINE, "request:{0}", request.toString());
      response = getClient().bulk(request).get();
    } catch (InterruptedException | ExecutionException e) {
      String msg = "error during bulk delete";
      ServiceException se = processException(e, msg);
      throw se;
    }
    if(response.hasFailures()) {
      String msg = "failures during bulk delete";
      LOG.log(Level.INFO, msg);
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }
  
  private SearchResponse searchBasicInt(SearchRequest request)
    throws ServiceException {
    SearchResponse response;
    try {
      response = getClient().search(request).get();
    } catch (ExecutionException | InterruptedException e) {
      String msg = "error querying elastic index";
      ServiceException se = processException(e, msg);
      throw se;
    }
    if (response.status().getStatus() != 200) {
      String msg = "searchBasic query - bad status response:" + response.status().getStatus();
      LOG.log(Level.INFO, msg);
      throw new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    return response;
  }
  
  private ServiceException processException(Exception e, String msg) {
    if(e.getCause() instanceof RemoteTransportException) {
      RemoteTransportException e1 = (RemoteTransportException)e.getCause();
      if(e1.getCause() instanceof SearchPhaseExecutionException) {
        SearchPhaseExecutionException e2 = (SearchPhaseExecutionException)e1.getCause();
        if(e2.getCause() instanceof QueryShardException) {
          QueryShardException e3 = (QueryShardException) e2.getCause();
          if (e3.getMessage().startsWith("No mapping found for ")) {
            int idx1 = e3.getMessage().indexOf("[");
            int idx2 = e3.getMessage().indexOf("]");
            if (idx1 != -1 && idx2 != -1 && idx1 < idx2) {
              String field = e3.getMessage().substring(idx1 + 1, idx2);
              String devMsg = "index[" + e1.getIndex().getName() + "] - " +
                "error querying - index missing mapping for field[" + field + "]";
              ServiceException ex = new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_QUERY_NO_MAPPING,
                Level.INFO, msg, devMsg, e);
              LOG.log(Level.INFO, devMsg);
              return ex;
            }
          }
        }
      }
    }
    LOG.log(Level.WARNING, msg, e);
    ServiceException ex = new ServiceException(RESTCodes.ServiceErrorCode.ELASTIC_SERVICE_ERROR, Level.WARNING,
      msg, e.getMessage(), e);
    processException(ex);
    return ex;
  }
}
