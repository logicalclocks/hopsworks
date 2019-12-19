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

import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.state.ProvFileStateParamBuilder;
import io.hops.hopsworks.common.provenance.util.functional.CheckedFunction;
import io.hops.hopsworks.common.provenance.util.functional.CheckedSupplier;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.javatuples.Pair;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.fuzzyQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchPhraseQuery;
import static org.elasticsearch.index.query.QueryBuilders.prefixQuery;
import static org.elasticsearch.index.query.QueryBuilders.wildcardQuery;

public class ElasticHelper {
  public static QueryBuilder fullTextSearch(String key, String term) {
    return boolQuery()
      .should(matchPhraseQuery(key, term.toLowerCase()))
      .should(prefixQuery(key, term.toLowerCase()))
      .should(fuzzyQuery(key, term.toLowerCase()))
      .should(wildcardQuery(key, String.format("*%s*", term.toLowerCase())));
  }
  
  public static void checkPagination(Integer offset, Integer limit, long defaultPageSize)
    throws ElasticException {
    if(offset == null) {
      offset = 0;
    }
    if(offset < 0) {
      throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO,
        "malformed - offset cannot be negative");
    }
    if(limit != null) {
      if(0 > limit || limit > defaultPageSize) {
        throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_QUERY_ERROR, Level.INFO,
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
  
  public static CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> sortBy(
    String field, SortOrder order) {
    return (SearchRequest sr) -> {
      sr.source().sort(field, order);
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
        ElasticHelper.checkPagination(offset, limit, defaultPageSize);
      } catch(ElasticException e) {
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
    List<Pair<ProvParser.Field, SortOrder>> fileStateSortBy, List<ProvFileStateParamBuilder.SortE> xattrSortBy) {
    return (SearchRequest sr) -> {
      for (Pair<ProvParser.Field, SortOrder> sb : fileStateSortBy) {
        sr.source().sort(SortBuilders.fieldSort(sb.getValue0().elasticFieldName()).order(sb.getValue1()));
      }
      for (ProvFileStateParamBuilder.SortE sb : xattrSortBy) {
        sr.source().sort(SortBuilders.fieldSort(sb.key).order(sb.order));
      }
      return sr;
    };
  }
  
  public static CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> withFileOpsOrder(
    List<Pair<ProvParser.Field, SortOrder>> fileOpsSortBy) {
    return (SearchRequest sr) -> {
      for (Pair<ProvParser.Field, SortOrder> sb : fileOpsSortBy) {
        sr.source().sort(SortBuilders.fieldSort(sb.getValue0().elasticFieldName()).order(sb.getValue1()));
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
    Map<String, ProvParser.FilterVal> filters) throws ProvenanceException {
    for (Map.Entry<String, ProvParser.FilterVal> fieldFilters : filters.entrySet()) {
      query.must(fieldFilters.getValue().query());
    }
    return query;
  }
}
