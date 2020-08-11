/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.ops;

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
  Set<ProvParser.Expansions> expansions = new HashSet<>();
  Map<ProvParser.Field, ProvParser.FilterVal> appStateFilter = new HashMap<>();
  Pair<Integer, Integer> pagination = null;
  Set<ProvOpsElastic.Aggregations> aggregations = new HashSet<>();
  
  public ProvOpsParamBuilder filterByFields(Set<String> params)
    throws ProvenanceException {
    for(String param : params) {
      Pair<ProvOpsParser.Field, Object> field = ProvOpsParser.extractFilter(param);
      ProvParser.addToFilters(fileOpsFilterBy, field.getValue0(), field.getValue1());
    }
    return this;
  }
  
  public ProvOpsParamBuilder filterByField(ProvOpsParser.Field field, Object val) throws ProvenanceException {
    Object parsedVal = field.filterValParser().apply(val);
    ProvParser.addToFilters(fileOpsFilterBy, field, parsedVal);
    return this;
  }
  
  public ProvOpsParamBuilder sortByFields(List<String> params) throws ProvenanceException {
    for(String param : params) {
      fileOpsSortBy.add(ProvOpsParser.extractSort(param));
    }
    return this;
  }
  
  public ProvOpsParamBuilder sortByField(ProvOpsParser.Field field, SortOrder order) {
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
  
  public ProvOpsParamBuilder expansions(Set<String> params) throws ProvenanceException {
    ProvParser.withExpansions(expansions, params);
    return this;
  }
  
  public ProvOpsParamBuilder withAppExpansionFilters(Set<String> params) throws ProvenanceException {
    for(String param : params) {
      ProvParser.addToFilters(appStateFilter, ProvAParser.extractFilter(param));
    }
    return this;
  }
  
  public ProvOpsParamBuilder withAppExpansion() {
    expansions.add(ProvParser.Expansions.APP);
    return this;
  }
  
  public ProvOpsParamBuilder withAppExpansion(String appId) throws ProvenanceException {
    withAppExpansion();
    ProvParser.addToFilters(appStateFilter, Pair.with(ProvAParser.Field.APP_ID, appId));
    return this;
  }
  
  public boolean hasAppExpansion() {
    return expansions.contains(ProvParser.Expansions.APP);
  }
  
  public ProvOpsParamBuilder withAggregation(ProvOpsElastic.Aggregations aggregation) {
    this.aggregations.add(aggregation);
    return this;
  }
  
  public ProvOpsParamBuilder aggregations(Set<String> aggregations) throws ProvenanceException {
    for(String agg : aggregations) {
      try {
        ProvOpsElastic.Aggregations aggregation = ProvOpsElastic.Aggregations.valueOf(agg);
        withAggregation(aggregation);
      } catch(NullPointerException | IllegalArgumentException e) {
        String msg = "aggregation" + agg
          + " not supported - supported:" + EnumSet.allOf(ProvOpsElastic.Aggregations.class);
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
          msg, "exception extracting aggregations");
      }
    }
    return this;
  }
}