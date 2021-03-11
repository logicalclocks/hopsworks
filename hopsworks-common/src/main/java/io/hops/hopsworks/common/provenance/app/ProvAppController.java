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
package io.hops.hopsworks.common.provenance.app;

import com.lambdista.util.Try;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.elastic.BasicElasticHit;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHits;
import io.hops.hopsworks.common.elastic.ElasticClientController;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHelper;
import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateElastic;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.provenance.util.functional.CheckedFunction;
import io.hops.hopsworks.common.provenance.util.functional.CheckedSupplier;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ProvAppController {
  
  @EJB
  private Settings settings;
  @EJB
  private ElasticClientController client;
  
  public Map<String, Map<Provenance.AppState, ProvAppStateElastic>> provAppState(
    Map<ProvParser.Field, ProvParser.FilterVal> filterBy) throws ProvenanceException {
    return provAppState(filterBy, new LinkedList<>(), 0, Settings.PROVENANCE_ELASTIC_PAGE_DEFAULT_SIZE);
  }
  
  public Map<String, Map<Provenance.AppState, ProvAppStateElastic>> provAppState(
    Map<ProvParser.Field, ProvParser.FilterVal> filterBy, List<Pair<ProvParser.Field, SortOrder>> sortBy,
    Integer offset, Integer limit)
    throws ProvenanceException {
    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      ElasticHelper.scrollingSearchRequest(
        Settings.ELASTIC_INDEX_APP_PROVENANCE,
        settings.getElasticDefaultScrollPageSize())
        .andThen(provAppStateQB(filterBy))
        .andThen(ElasticHelper.sortBy(sortBy))
        .andThen(ElasticHelper.withPagination(offset, limit, settings.getElasticMaxScrollPageSize()));
    SearchRequest request = srF.get();
    Pair<Long, Try<ElasticAppStatesObj>> searchResult;
    try {
      searchResult = client.searchScrolling(request, ElasticAppStatesObj.getHandler());
    } catch (ElasticException e) {
      String msg = "provenance - elastic query problem";
      throw ProvHelper.fromElastic(e, msg, msg + " - app state");
    }
    return ElasticAppStatesObj.checkedResult(searchResult);
  }
  
  /**
   * Key1 - String - appId
   * Key2 - Provenance.AppState
   * Value - ProvAppStateElastic
   */
  private static class ElasticAppStatesObj extends HashMap<String, Map<Provenance.AppState, ProvAppStateElastic>> {
    private static ElasticHits.Merger<ProvAppStateElastic, ElasticAppStatesObj> getMerger() {
      return (ProvAppStateElastic item, ElasticAppStatesObj states) -> {
        Map<Provenance.AppState, ProvAppStateElastic> appIdStates
          = states.computeIfAbsent(item.getAppId(), key -> new TreeMap<>());
        appIdStates.put(item.getAppState(), item);
        return Try.apply(() -> states);
      };
    }
  
    static ElasticHits.Handler<ProvAppStateElastic, ElasticAppStatesObj> getHandler() {
      ElasticHits.Parser<ProvAppStateElastic> parser
        = hit -> ProvAppStateElastic.tryInstance(BasicElasticHit.instance(hit));
      return ElasticHits.handlerBasic(parser, new ElasticAppStatesObj(), getMerger());
    }
  
    /**
     * Key1 - String - appId
     * Key2 - Provenance.AppState
     * Value - ProvAppStateElastic
     */
    static Map<String, Map<Provenance.AppState, ProvAppStateElastic>>
      checkedResult(Pair<Long, Try<ElasticAppStatesObj>> result) throws ProvenanceException {
      try {
        return result.getValue1().checkedGet();
      } catch (Throwable t) {
        if (t instanceof ProvenanceException) {
          throw (ProvenanceException) t;
        } else {
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.INFO, "unhandled error",
            "unhandled error", t);
        }
      }
    }
  }
  
  private CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> provAppStateQB(
    Map<ProvParser.Field, ProvParser.FilterVal> filterBy) {
    return (SearchRequest sr) -> {
      BoolQueryBuilder query = boolQuery();
      query = ElasticHelper.filterByBasicFields(query, filterBy);
      sr.source().query(query);
      return sr;
    };
  }
}
