/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.provenance.ops;

import io.hops.hopsworks.common.provenance.app.ProvAParser;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.search.sort.SortOrder;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ProvOpsParamBuilder {
  Map<ProvParser.Field, ProvParser.FilterVal> fileOpsFilterBy = new HashMap<>();
  List<Pair<ProvParser.Field, SortOrder>> fileOpsSortBy = new ArrayList<>();
  Set<ProvParser.ElasticExpansions> expansions = new HashSet<>();
  Map<ProvParser.Field, ProvParser.FilterVal> appStateFilter = new HashMap<>();
  Pair<Integer, Integer> pagination = null;
  Set<ProvOpsAggregations> aggregations = new HashSet<>();
  
  public ProvOpsParamBuilder filterByFields(Set<String> params)
    throws ProvenanceException {
    if(params != null) {
      for (String param : params) {
        Pair<ProvOps.Field, Object> field = ProvOps.extractFilter(param);
        ProvParser.addToFilters(fileOpsFilterBy, field.getValue0(), field.getValue1());
      }
    }
    return this;
  }
  
  public ProvOpsParamBuilder filterByField(ProvOps.Field field, Object val) throws ProvenanceException {
    Object parsedVal = field.filterValParser().apply(val);
    ProvParser.addToFilters(fileOpsFilterBy, field, parsedVal);
    return this;
  }
  
  public ProvOpsParamBuilder sortByFields(List<String> params) throws ProvenanceException {
    if(params != null) {
      for (String param : params) {
        fileOpsSortBy.add(ProvOps.extractSort(param));
      }
    }
    return this;
  }
  
  public ProvOpsParamBuilder sortByField(ProvOps.Field field, SortOrder order) {
    fileOpsSortBy.add(Pair.with(field, order));
    return this;
  }
  
  public ProvOpsParamBuilder paginate(Integer offset, Integer limit) {
    if(offset == null) {
      offset = 0;
    }
    if(limit == null) {
      limit = Settings.PROVENANCE_ELASTIC_PAGE_DEFAULT_SIZE;
    }
    pagination = Pair.with(offset, limit);
    return this;
  }
  
  public ProvOpsParamBuilder provExpansions(Set<String> params) throws ProvenanceException {
    if(params != null) {
      ProvParser.withExpansions(expansions, params);
    }
    return this;
  }
  
  public ProvOpsParamBuilder withAppExpansionFilters(Set<String> params) throws ProvenanceException {
    if(params != null) {
      for (String param : params) {
        ProvParser.addToFilters(appStateFilter, ProvAParser.extractFilter(param));
      }
    }
    return this;
  }
  
  public ProvOpsParamBuilder withAppExpansion() {
    expansions.add(ProvParser.ElasticExpansions.APP);
    return this;
  }
  
  public ProvOpsParamBuilder withAppExpansion(String appId) throws ProvenanceException {
    withAppExpansion();
    ProvParser.addToFilters(appStateFilter, Pair.with(ProvAParser.Field.APP_ID, appId));
    return this;
  }
  
  public boolean hasAppExpansion() {
    return expansions.contains(ProvParser.ElasticExpansions.APP);
  }
  
  public ProvOpsParamBuilder withAggregation(ProvOpsAggregations aggregation) {
    this.aggregations.add(aggregation);
    return this;
  }
  
  public ProvOpsParamBuilder aggregations(Set<String> aggregations) throws ProvenanceException {
    if(aggregations == null) {
      return this;
    }
    for(String agg : aggregations) {
      try {
        ProvOpsAggregations aggregation = ProvOpsAggregations.valueOf(agg);
        withAggregation(aggregation);
      } catch(NullPointerException | IllegalArgumentException e) {
        String msg = "aggregation" + agg
          + " not supported - supported:" + EnumSet.allOf(ProvOpsAggregations.class);
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
          msg, "exception extracting aggregations");
      }
    }
    return this;
  }
  
  public Set<ProvOpsAggregations> getAggregations() {
    return aggregations;
  }
  
  public Map<ProvParser.Field, ProvParser.FilterVal> getFileOpsFilterBy() {
    return fileOpsFilterBy;
  }
  
  public List<Pair<ProvParser.Field, SortOrder>> getFileOpsSortBy() {
    return fileOpsSortBy;
  }
  
  public Set<ProvParser.ElasticExpansions> getExpansions() {
    return expansions;
  }
  
  public Map<ProvParser.Field, ProvParser.FilterVal> getAppStateFilter() {
    return appStateFilter;
  }
  
  public Pair<Integer, Integer> getPagination() {
    return pagination;
  }
}