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

import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.elastic.ProvElasticController;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHelper;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHitsHandler;
import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateElastic;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.provenance.util.functional.CheckedFunction;
import io.hops.hopsworks.common.provenance.util.functional.CheckedSupplier;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ProvAppController {
  
  @EJB
  private Settings settings;
  @EJB
  private ProvElasticController client;
  
  public Map<String, Map<Provenance.AppState, ProvAppStateElastic>> provAppState(
    Map<String, ProvParser.FilterVal> appStateFilters)
    throws ProvenanceException {
    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      ElasticHelper.scrollingSearchRequest(
        Settings.ELASTIC_INDEX_APP_PROVENANCE,
        settings.getElasticDefaultScrollPageSize())
        .andThen(provAppStateQB(appStateFilters));
    SearchRequest request = srF.get();
    Pair<Long, Map<String, Map<Provenance.AppState, ProvAppStateElastic>>> searchResult;
    try {
      searchResult = client.searchScrolling(request, appStateParser());
    } catch (ElasticException e) {
      String msg = "provenance - elastic query problem";
      throw ProvHelper.fromElastic(e, msg, msg + " - app state");
    }
    return searchResult.getValue1();
  }
  
  private ElasticHitsHandler<ProvAppStateElastic, Map<String, Map<Provenance.AppState, ProvAppStateElastic>>, ?,
    ProvenanceException> appStateParser() {
    return ElasticHitsHandler.instanceBasic(new HashMap<>(),
      hit -> ProvAppStateElastic.instance(hit),
      (ProvAppStateElastic item, Map<String, Map<Provenance.AppState, ProvAppStateElastic>> state) -> {
        Map<Provenance.AppState, ProvAppStateElastic> appStates =
          state.computeIfAbsent(item.getAppId(), k -> new TreeMap<>());
        appStates.put(item.getAppState(), item);
      });
  }
  
  private CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> provAppStateQB(
    Map<String, ProvParser.FilterVal> appStateFilters) {
    return (SearchRequest sr) -> {
      BoolQueryBuilder query = boolQuery();
      query = ElasticHelper.filterByBasicFields(query, appStateFilters);
      sr.source().query(query);
      return sr;
    };
  }
}
