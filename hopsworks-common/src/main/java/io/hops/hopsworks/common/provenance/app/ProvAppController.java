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
import io.hops.hopsworks.common.provenance.core.opensearch.BasicOpenSearchHit;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchHits;
import io.hops.hopsworks.common.opensearch.OpenSearchClientController;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchHelper;
import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateOpenSearch;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.provenance.util.functional.CheckedFunction;
import io.hops.hopsworks.common.provenance.util.functional.CheckedSupplier;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.search.sort.SortOrder;
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

import static org.opensearch.index.query.QueryBuilders.boolQuery;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ProvAppController {
  
  @EJB
  private Settings settings;
  @EJB
  private OpenSearchClientController client;
  
  public Map<String, Map<Provenance.AppState, ProvAppStateOpenSearch>> provAppState(
    Map<ProvParser.Field, ProvParser.FilterVal> filterBy) throws ProvenanceException {
    return provAppState(filterBy, new LinkedList<>(), 0, Settings.PROVENANCE_OPENSEARCH_PAGE_DEFAULT_SIZE);
  }
  
  public Map<String, Map<Provenance.AppState, ProvAppStateOpenSearch>> provAppState(
    Map<ProvParser.Field, ProvParser.FilterVal> filterBy, List<Pair<ProvParser.Field, SortOrder>> sortBy,
    Integer offset, Integer limit)
    throws ProvenanceException {
    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      OpenSearchHelper.scrollingSearchRequest(
        Settings.OPENSEARCH_INDEX_APP_PROVENANCE,
        settings.getOpenSearchDefaultScrollPageSize())
        .andThen(provAppStateQB(filterBy))
        .andThen(OpenSearchHelper.sortBy(sortBy))
        .andThen(OpenSearchHelper.withPagination(offset, limit, settings.getOpenSearchMaxScrollPageSize()));
    SearchRequest request = srF.get();
    Pair<Long, Try<OpenSearchAppStatesObj>> searchResult;
    try {
      searchResult = client.searchScrolling(request,  OpenSearchAppStatesObj.getHandler());
    } catch (OpenSearchException e) {
      String msg = "provenance - opensearch query problem";
      throw ProvHelper.fromOpenSearch(e, msg, msg + " - app state");
    }
    return OpenSearchAppStatesObj.checkedResult(searchResult);
  }
  
  /**
   * Key1 - String - appId
   * Key2 - Provenance.AppState
   * Value - ProvAppStateOpenSearch
   */
  private static class OpenSearchAppStatesObj
    extends HashMap<String, Map<Provenance.AppState, ProvAppStateOpenSearch>> {
    private static OpenSearchHits.Merger<ProvAppStateOpenSearch, OpenSearchAppStatesObj> getMerger() {
      return (ProvAppStateOpenSearch item, OpenSearchAppStatesObj states) -> {
        Map<Provenance.AppState, ProvAppStateOpenSearch> appIdStates
          = states.computeIfAbsent(item.getAppId(), key -> new TreeMap<>());
        appIdStates.put(item.getAppState(), item);
        return Try.apply(() -> states);
      };
    }
  
    static OpenSearchHits.Handler<ProvAppStateOpenSearch, OpenSearchAppStatesObj> getHandler() {
      OpenSearchHits.Parser<ProvAppStateOpenSearch> parser
        = hit -> ProvAppStateOpenSearch.tryInstance(BasicOpenSearchHit.instance(hit));
      return OpenSearchHits.handlerBasic(parser, new OpenSearchAppStatesObj(), getMerger());
    }
  
    /**
     * Key1 - String - appId
     * Key2 - Provenance.AppState
     * Value - ProvAppStateOpenSearch
     */
    static Map<String, Map<Provenance.AppState, ProvAppStateOpenSearch>>
      checkedResult(Pair<Long, Try<OpenSearchAppStatesObj>> result) throws ProvenanceException {
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
      query = OpenSearchHelper.filterByBasicFields(query, filterBy);
      sr.source().query(query);
      return sr;
    };
  }
}
