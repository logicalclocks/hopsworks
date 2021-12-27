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

import com.lambdista.util.FailableSupplier;
import com.lambdista.util.Try;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticAggregation;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticAggregationParser;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHelper;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHits;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.client.indices.GetIndexTemplatesRequest;
import org.elasticsearch.client.indices.GetIndexTemplatesResponse;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.rest.RestStatus;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.net.ssl.SSLHandshakeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
/**
 * This controller is here to simplify general elastic search access
 * This is a wrapper around the ElasticClient Singleton in order to:
 * 1. translate elastic exceptions to our internal exceptions in a consistent manner
 * 2. reset client in case of certificate rotation
 * 3. deal with search scrolling and aggregations consistently
 */
public class ElasticClientController {
  private static final Logger LOG = Logger.getLogger(ElasticClientController.class.getName());
  
  @EJB
  private ElasticClient client;
  
  public GetIndexResponse mngIndexGet(GetIndexRequest request) throws ElasticException {
    FailableSupplier<GetIndexResponse> query =
      () -> client.getClient().indices().get(request, RequestOptions.DEFAULT);
    return executeElasticQuery(query, "elastic get index", request.toString());
  }
  
  /**
   * This is an optimized elastic query, but the regex expression only accepts wildcards *
   * @param regex
   * @return
   * @throws ElasticException
   */
  public String[] mngIndicesGetBySimplifiedRegex(String regex) throws ElasticException {
    try {
      return mngIndexGet(new GetIndexRequest(regex)).getIndices();
    } catch(ElasticException e) {
      if (ElasticHelper.indexNotFound(e.getCause())) {
        return new String[0];
      } else {
        throw e;
      }
    }
  }
  public String[] mngIndicesGetByRegex(String regex) throws ElasticException {
    GetIndexResponse response = mngIndexGet(new GetIndexRequest("*"));
    ArrayList<String> result = new ArrayList<>();
    Pattern pattern = Pattern.compile(regex);
    for(String index : response.getIndices()) {
      if (pattern.matcher(index).matches()) {
        result.add(index);
      }
    }
    return result.toArray(new String[0]);
  }
  
  public <T> Map<String, T> mngIndicesGetByRegex(String regex, Function<Settings, T> indexSettingsParser)
      throws ElasticException {
    GetIndexResponse response = mngIndexGet(new GetIndexRequest("*"));
    Map<String, T> result = new HashMap<>();
    Pattern pattern = Pattern.compile(regex);
    Map<String, Settings> settings = response.getSettings();
    for(String index : response.getIndices()) {
      if (pattern.matcher(index).matches()) {
        result.put(index, indexSettingsParser.apply(settings.get(index)));
      }
    }
    return result;
  }
  
  public Map<String, Map<String, String>> mngIndexGetMappings(String indexRegex) throws ElasticException {
    GetMappingsRequest request = new GetMappingsRequest().indices(indexRegex);
    FailableSupplier<GetMappingsResponse> query =
      () -> client.getClient().indices().getMapping(request, RequestOptions.DEFAULT);
    GetMappingsResponse response = executeElasticQuery(query, "elastic get index mapping", request.toString());

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
  
  public boolean mngIndexExists(String indexName) throws ElasticException {
    GetIndexRequest request = new GetIndexRequest(indexName);
    FailableSupplier<Boolean> query =
      () ->  client.getClient().indices().exists(request, RequestOptions.DEFAULT);
    return executeElasticQuery(query, "elastic index exists", request.toString());
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
    FailableSupplier<CreateIndexResponse> query =
      () -> client.getClient().indices().create(request, RequestOptions.DEFAULT);
    CreateIndexResponse response = executeElasticQuery(query, "elastic index create", request.toString());
    if(response.isAcknowledged()) {
      return response;
    } else {
      String msg = "elastic index:" + request.index() + "creation could not be acknowledged";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }
  
  public AcknowledgedResponse mngIndexDelete(String index) throws ElasticException {
    return mngIndexDelete(new DeleteIndexRequest((index)));
  }
  
  public AcknowledgedResponse mngIndexDelete(DeleteIndexRequest request) throws ElasticException {
    FailableSupplier<AcknowledgedResponse> query =
      () -> client.getClient().indices().delete(request, RequestOptions.DEFAULT);
    AcknowledgedResponse response = executeElasticQuery(query, "elastic index delete", request.toString());
    if(response.isAcknowledged()) {
      return response;
    } else {
      String msg = "elastic index:" + request.indices()[0] + "deletion could not be acknowledged";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }

  public void indexDoc(IndexRequest request) throws ElasticException {
    FailableSupplier<IndexResponse> query =
      () -> client.getClient().index(request, RequestOptions.DEFAULT);
    IndexResponse response = executeElasticQuery(query, "elastic index doc", request.toString());
    if (response.status().getStatus() != 201) {
      String msg = "doc index - bad status response:" + response.status().getStatus();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }

  public void updateDoc(UpdateRequest request) throws ElasticException {
    FailableSupplier<UpdateResponse> query =
      () -> client.getClient().update(request, RequestOptions.DEFAULT);
    UpdateResponse response = executeElasticQuery(query, "elastic update doc", request.toString());
    if (response.status().getStatus() != 200) {
      String msg = "doc update - bad status response:" + response.status().getStatus();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }
  
  public MultiSearchResponse multiSearch(MultiSearchRequest request) throws ElasticException {
    FailableSupplier<MultiSearchResponse> query =
      () -> client.getClient().msearch(request, RequestOptions.DEFAULT);
    MultiSearchResponse response = executeElasticQuery(query, "elastic multi search", request.toString());
    return response;
  }
  
  /**
   * When using this method keep in mind that a single page is returned and it is the user's job to get all pages
   * @param request
   * @return
   * @throws ElasticException
   */
  public SearchResponse baseSearch(SearchRequest request) throws ElasticException {
    FailableSupplier<SearchResponse> query =
      () -> client.getClient().search(request, RequestOptions.DEFAULT);
    SearchResponse response = executeElasticQuery(query, "elastic basic search", request.toString());
    if (response.status().getStatus() != 200) {
      String msg = "searchBasic query - bad status response:" + response.status().getStatus();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    return response;
  }
  
  /**
   * When using this method keep in mind that a single page is returned and it is the user's job to get all pages
   * @param request
   * @param handler
   * @param <R>
   * @param <S>
   * @return
   * @throws ElasticException
   */
  public <R, S> Pair<Long, Try<S>> search(SearchRequest request, ElasticHits.Handler<R, S> handler)
    throws ElasticException {
    SearchResponse response;
    response = baseSearch(request);
    Try<S> collectedResults = handler.apply(response.getHits().getHits());
    return Pair.with(response.getHits().getTotalHits().value, collectedResults);
  }

  /**
   * Performs scrolling search for any request exceeding requested size, finally returning pair a containing totalHits
   * number and the whole response.
   * @param response
   * @param handler
   * @param request
   * @param <R>
   * @param <S>
   * @return
   * @throws ElasticException
   */
  public <R, S> Pair<Long, Try<S>> scrolling(SearchResponse response, ElasticHits.Handler<R, S> handler,
                                             SearchRequest request)
          throws ElasticException {
    long leftover;

    long totalHits = response.getHits().getTotalHits().value;
    leftover = Math.min(request.source().size(), totalHits);
    leftover = leftover - response.getHits().getHits().length;
    Try<S> result = handler.apply(response.getHits().getHits());

    //make into a scrolling request if not already and there are more hits
    if(leftover > 0 && response.getScrollId() != null) {
      response = baseSearch(request);
    }

    while (leftover > 0 && result.isSuccess()) {
      SearchScrollRequest next = nextScrollPage(response.getScrollId());
      response = searchScrollingInt(next);
      leftover = leftover - response.getHits().getHits().length;
      result = handler.apply(response.getHits().getHits());
    }
    if(response.getScrollId() != null) {//if scrolling request clear context
      clearScrollingContext(response.getScrollId());
    }
    return Pair.with(totalHits, result);
  }

  /**
   * Returns all results matching the search - these results are all built in memory, so use with care.
   * @param request
   * @param handler
   * @param <R>
   * @param <S>
   * @return
   * @throws ElasticException
   */
  public <R, S> Pair<Long, Try<S>> searchScrolling(SearchRequest request, ElasticHits.Handler<R, S> handler)
          throws ElasticException {
    SearchResponse response = baseSearch(request);
    return scrolling(response, handler, request);
  }

  /**
   * Returns all MultiSearch results in a list matching the respective MultiSearch request -
   * these results are all built in memory, so use with care.
   * @param multiSearchRequest
   * @param handler
   * @param <R>
   * @param <S>
   * @return
   * @throws ElasticException
   */
  public <R, S> List<Pair<Long, Try<S>>> multiSearchScrolling(
          MultiSearchRequest multiSearchRequest, ElasticHits.Handler<R, S> handler)
          throws ElasticException {
    MultiSearchResponse multiSearchResponse = multiSearch(multiSearchRequest);
    List<Pair<Long, Try<S>>> searchResult = new ArrayList<>();
    int index = 0;
    for (MultiSearchResponse.Item item: multiSearchResponse) {
      SearchResponse response = item.getResponse();
      SearchRequest request = multiSearchRequest.requests().get(index++);
      searchResult.add(scrolling(response, handler, request));
    }
    return searchResult;
  }

  public long searchCount(SearchRequest request) throws ElasticException {
    SearchResponse response;
    LOG.log(Level.FINE, "request:{0}", request.toString());
    response = baseSearch(request);
    LOG.log(Level.FINE, "response:{0}", response.toString());
    return response.getHits().getTotalHits().value;
  }

  public <A extends ElasticAggregation, E extends Exception> Map<A, List> searchAggregations(
      SearchRequest request, Map<A, ElasticAggregationParser<?, E>> aggregations)
      throws ElasticException, E {
    SearchResponse response = baseSearch(request);
    LOG.log(Level.FINE, "response:{0}", response.toString());
    Map<A, List> aggResults = new HashMap<>();
    if(!aggregations.isEmpty()) {
      for (Map.Entry<A, ElasticAggregationParser<?, E>> aggregation : aggregations.entrySet()) {
        aggResults.put(aggregation.getKey(), aggregation.getValue().apply(response.getAggregations()));
      }
    }
    return aggResults;
  }
  
  private SearchResponse searchScrollingInt(SearchScrollRequest request) throws ElasticException {
    FailableSupplier<SearchResponse> query =
      () -> client.getClient().scroll(request, RequestOptions.DEFAULT);
    SearchResponse response = executeElasticQuery(query, "elastic scrolling search", request.toString());
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
  
  private ClearScrollResponse clearScrollingContext(String scrollId) throws ElasticException {
    ClearScrollRequest request = new ClearScrollRequest();
    request.addScrollId(scrollId);
    
    FailableSupplier<ClearScrollResponse> query =
      () -> client.getClient().clearScroll(request, RequestOptions.DEFAULT);
    ClearScrollResponse response = executeElasticQuery(query, "elastic scrolling search", request.toString());
    if (response.status().getStatus() != 200) {
      String msg = "scroll context clearing query - bad status response:" + response.status().getStatus();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    return response;
  }
  
  public void bulkDelete(BulkRequest request) throws ElasticException {
    FailableSupplier<BulkResponse> query =
      () -> client.getClient().bulk(request, RequestOptions.DEFAULT);
    BulkResponse response = executeElasticQuery(query, "elastic bulk delete", request.toString());
    if(response.hasFailures()) {
      String msg = "failures during bulk delete";
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
  }

  public BulkResponse bulkUpdateDoc(BulkRequest request) throws ElasticException {
    FailableSupplier<BulkResponse> query =
      () -> client.getClient().bulk(request, RequestOptions.DEFAULT);
    BulkResponse response = executeElasticQuery(query, "elastic bulk update doc", request.toString());
    if (response.status().getStatus() != 200) {
      String msg = "doc update - bad status response:" + response.status().getStatus();
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO, msg);
    }
    return response;
  }

  public AcknowledgedResponse aliasUpdate(IndicesAliasesRequest request) throws ElasticException {
    FailableSupplier<AcknowledgedResponse> query =
      () -> client.getClient().indices().updateAliases(request, RequestOptions.DEFAULT);
    AcknowledgedResponse response = executeElasticQuery(query, "elastic alias update", request.toString());
    if(response.isAcknowledged()) {
      return response;
    } else {
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.INFO,
        "error during elastic alias update");
    }
  }

  public AcknowledgedResponse aliasSwitchIndex(String alias, String fromIndex, String toIndex)
      throws ElasticException {
    IndicesAliasesRequest request = new IndicesAliasesRequest()
        .addAliasAction(new IndicesAliasesRequest.AliasActions(
          IndicesAliasesRequest.AliasActions.Type.REMOVE).index(fromIndex).alias(alias))
        .addAliasAction(new IndicesAliasesRequest.AliasActions(
          IndicesAliasesRequest.AliasActions.Type.ADD).index(toIndex).alias(alias));
    return aliasUpdate(request);
  }

  public GetAliasesResponse aliasGet(GetAliasesRequest request) throws ElasticException {
    FailableSupplier<GetAliasesResponse> query =
      () -> client.getClient().indices().getAlias(request, RequestOptions.DEFAULT);
    GetAliasesResponse response = executeElasticQuery(query, "elastic get alias", request.toString());
    if(response.status().equals(RestStatus.OK) || response.status().equals(RestStatus.NOT_FOUND)) {
      return response;
    } else {
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO,
        "error during elastic get alias");
    }
  }

  public GetAliasesResponse getAliases(String alias)
    throws ElasticException {
    GetAliasesRequest request = new GetAliasesRequest().aliases(alias);
    return aliasGet(request);
  }

  public AcknowledgedResponse createAlias(String alias, String index) throws ElasticException {
    IndicesAliasesRequest request = new IndicesAliasesRequest()
      .addAliasAction(new IndicesAliasesRequest.AliasActions(
        IndicesAliasesRequest.AliasActions.Type.ADD).index(index).alias(alias));
    return aliasUpdate(request);
  }

  public ClusterHealthResponse clusterHealthGet() throws ElasticException {
    ClusterHealthRequest request = new ClusterHealthRequest();
    FailableSupplier<ClusterHealthResponse> query =
      () -> client.getClient().cluster().health(request, RequestOptions.DEFAULT);
    ClusterHealthResponse response = executeElasticQuery(query, "elastic get cluster health", request.toString());
    if(response.status().equals(RestStatus.OK) || response.status().equals(RestStatus.NOT_FOUND)) {
      return response;
    } else {
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO,
        "error during elastic get cluster health");
    }
  }

  public GetIndexTemplatesResponse templateGet(String template) throws ElasticException {
    GetIndexTemplatesRequest request = new GetIndexTemplatesRequest(template);
    FailableSupplier<GetIndexTemplatesResponse> query =
      () -> client.getClient().indices().getIndexTemplate(request, RequestOptions.DEFAULT);
    return executeElasticQuery(query, "elastic get template", request.toString());
  }
  
  private <O> O executeElasticQuery(FailableSupplier<O> query, String usrMsg, String devMsg) throws ElasticException {
    try {
      try {
        LOG.log(Level.FINE, "{0}:{1}", new Object[]{usrMsg, devMsg});
        return query.get();
      } catch (SSLHandshakeException e) {
        //certificates might have changed, we reset client and retry
        client.resetClient();
        return query.get();
      }
    } catch (IndexNotFoundException e) {
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INDEX_NOT_FOUND, Level.INFO,
        "elastic index not found during " + usrMsg, devMsg, e);
    } catch (ElasticsearchStatusException e) {
      if(e.status().equals(RestStatus.NOT_FOUND)) {
        throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INDEX_NOT_FOUND, Level.INFO,
          "elastic index not found during " + usrMsg, devMsg, e);
      }
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        "error during " + usrMsg, devMsg, e);
    } catch(Throwable e) {
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR, Level.WARNING,
        "error during " + usrMsg, devMsg, e);
    }
  }
}
