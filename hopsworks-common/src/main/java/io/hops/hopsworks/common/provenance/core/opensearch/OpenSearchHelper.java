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
package io.hops.hopsworks.common.provenance.core.opensearch;

import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.state.ProvStateParamBuilder;
import io.hops.hopsworks.common.provenance.util.functional.CheckedFunction;
import io.hops.hopsworks.common.provenance.util.functional.CheckedSupplier;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.IndexNotFoundException;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.rest.RestStatus;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;
import org.javatuples.Pair;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.fuzzyQuery;
import static org.opensearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.opensearch.index.query.QueryBuilders.prefixQuery;
import static org.opensearch.index.query.QueryBuilders.wildcardQuery;

public class OpenSearchHelper {
  public static QueryBuilder fullTextSearch(String key, String term) {
    return boolQuery()
      .should(matchPhraseQuery(key, term.toLowerCase()))
      .should(prefixQuery(key, term.toLowerCase()))
      .should(fuzzyQuery(key, term.toLowerCase()))
      .should(wildcardQuery(key, String.format("*%s*", term.toLowerCase())));
  }
  
  public static void checkPagination(Integer offset, Integer limit, long defaultPageSize)
    throws OpenSearchException {
    if(offset == null) {
      offset = 0;
    }
    if(offset < 0) {
      throw new OpenSearchException(RESTCodes.OpenSearchErrorCode.OPENSEARCH_QUERY_ERROR, Level.INFO,
        "malformed - offset cannot be negative");
    }
    if(limit != null) {
      if(0 > limit || limit > defaultPageSize) {
        throw new OpenSearchException(RESTCodes.OpenSearchErrorCode.OPENSEARCH_QUERY_ERROR, Level.INFO,
          "malformed - limit not between 0 and ELASTIC_DEFAULT_SCROLL_PAGE_SIZE:" + defaultPageSize);
      }
    }
  }
  
  public static CheckedSupplier<SearchRequest, ProvenanceException> scrollingSearchRequest(String index, int pageSize) {
    return () -> {
      SearchRequest sr = new SearchRequest(index)
        .scroll(TimeValue.timeValueMinutes(1));
      sr.source().size(pageSize);
      return sr;
    };
  }
  
  public static CheckedSupplier<SearchRequest, ProvenanceException> baseSearchRequest(String index, int pageSize) {
    return () -> {
      SearchRequest sr = new SearchRequest(index);
      sr.source().size(pageSize);
      return sr;
    };
  }
  
  public static CheckedSupplier<SearchRequest, ProvenanceException> countSearchRequest(String index) {
    return () -> {
      SearchRequest sr = new SearchRequest(index);
      sr.source().size(0);
      return sr;
    };
  }
  
  public static CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> withPagination(
    Integer offset, Integer limit, long defaultPageSize) {
    return (SearchRequest sr) -> {
      try {
        OpenSearchHelper.checkPagination(offset, limit, defaultPageSize);
      } catch(OpenSearchException e) {
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
          "query with malformed pagination", "query with malformed pagination", e);
      }
      if(offset != null) {
        sr.source().from(offset);
      }
      if(limit != null) {
        sr.source().size(limit);
      }
      return sr;
    };
  }
  
  public static  CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> withFileStateOrder(
    List<Pair<ProvParser.Field, SortOrder>> fileStateSortBy, List<ProvStateParamBuilder.SortE> xattrSortBy) {
    return (SearchRequest sr) -> {
      for (Pair<ProvParser.Field, SortOrder> sb : fileStateSortBy) {
        sr.source().sort(SortBuilders.fieldSort(sb.getValue0().openSearchFieldName()).order(sb.getValue1()));
      }
      for (ProvStateParamBuilder.SortE sb : xattrSortBy) {
        sr.source().sort(SortBuilders.fieldSort(sb.key).order(sb.order));
      }
      return sr;
    };
  }
  
  public static CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> withFileOpsOrder(
    List<Pair<ProvParser.Field, SortOrder>> fileOpsSortBy) {
    return (SearchRequest sr) -> {
      for (Pair<ProvParser.Field, SortOrder> sb : fileOpsSortBy) {
        sr.source().sort(SortBuilders.fieldSort(sb.getValue0().openSearchFieldName()).order(sb.getValue1()));
      }
      return sr;
    };
  }
  
  public static CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> withAggregations(
    List<AggregationBuilder> aggregationBuilder) {
    return (SearchRequest sr) -> {
      if(!aggregationBuilder.isEmpty()) {
        for (AggregationBuilder builder : aggregationBuilder) {
          sr.source().aggregation(builder);
        }
      }
      return sr;
    };
  }
  
  public static BoolQueryBuilder filterByBasicFields(BoolQueryBuilder query,
    Map<ProvParser.Field, ProvParser.FilterVal> filters) throws ProvenanceException {
    for (ProvParser.FilterVal fieldFilters : filters.values()) {
      query.must(fieldFilters.query());
    }
    return query;
  }
  
  public static CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> sortBy(
    List<Pair<ProvParser.Field, SortOrder>> sortBy) {
    return (SearchRequest sr) -> {
      for (Pair<ProvParser.Field, SortOrder> sb : sortBy) {
        sr.source().sort(SortBuilders.fieldSort(sb.getValue0().openSearchFieldName()).order(sb.getValue1()));
      }
      return sr;
    };
  }
  
  
  public static boolean indexNotFound(Throwable t) {
    if(t instanceof IndexNotFoundException) {
      return true;
    }
    if(t instanceof OpenSearchStatusException) {
      OpenSearchStatusException e = (OpenSearchStatusException) t;
      return e.status().equals(RestStatus.NOT_FOUND);
    }
    return false;
  }
}
