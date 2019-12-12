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
package io.hops.hopsworks.common.provenance.core;

import io.hops.hopsworks.common.provenance.core.elastic.ElasticHelper;
import io.hops.hopsworks.common.provenance.util.functional.CheckedFunction;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.javatuples.Pair;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

public class ProvParser {
  public interface ElasticField {
  }
  
  public enum BaseField implements ElasticField {
    PROJECT_I_ID,
    DATASET_I_ID,
    PARENT_I_ID,
    INODE_ID,
    INODE_NAME,
    USER_ID,
    APP_ID,
    ML_TYPE,
    ML_ID,
    ENTRY_TYPE;
    
    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }
  
  public enum AuxField implements ElasticField {
    PARTITION_ID,
    PROJECT_NAME;
    
    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }
  
  public enum XAttrField implements ElasticField {
    XATTR_PROV;
    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }
  
  public enum DocSubType {
    FEATURE,
    TRAINING_DATASET,
    EXPERIMENT,
    MODEL,
    HIVE,
    DATASET,
    NONE,
    
    FEATURE_PART,
    TRAINING_DATASET_PART,
    EXPERIMENT_PART,
    MODEL_PART,
    HIVE_PART,
    DATASET_PART;
    
    @Override
    public String toString() {
      return name().toLowerCase();
    }
    
    public boolean isMLPart() {
      switch (this) {
        case FEATURE_PART:
        case TRAINING_DATASET_PART:
        case EXPERIMENT_PART:
        case MODEL_PART:
          return true;
        default: return false;
      }
    }
    
    public boolean isMLParent() {
      switch(this) {
        case FEATURE:
        case TRAINING_DATASET:
        case EXPERIMENT:
        case MODEL:
          return true;
        default: return false;
      }
    }
  }
  
  public enum EntryType {
    STATE,
    OPERATION,
    ARCHIVE;
    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }
  
  public interface ValParser<V> extends CheckedFunction<Object, V, ProvenanceException> {
  }
  
  public interface Field {
    String elasticFieldName();
    String queryFieldName();
    FilterType filterType();
    ValParser<?> filterValParser();
  }
  
  public enum FilterType {
    EXACT,
    LIKE,
    RANGE_LT,
    RANGE_LTE,
    RANGE_GT,
    RANGE_GTE;
  }
  
  public interface FilterVal {
    void add(Pair<Field, Object> filter) throws ProvenanceException;
    QueryBuilder query() throws ProvenanceException;
  }
  
  public static FilterVal filterValInstance(FilterType filterType) {
    FilterVal filterVal;
    if(filterType == FilterType.RANGE_GT
      || filterType == FilterType.RANGE_GTE
      || filterType == FilterType.RANGE_LT
      || filterType == FilterType.RANGE_LTE ) {
      filterVal = new FilterValRange();
    } else {
      filterVal = new FilterValInList();
    }
    return filterVal;
  }
  
  public static class FilterValInList implements FilterVal {
    List<Pair<Field, Object>> inList = new LinkedList<>();
    
    @Override
    public void add(Pair<Field, Object> filter) throws ProvenanceException {
      if(filter.getValue0().filterType() == FilterType.RANGE_GT
        || filter.getValue0().filterType() == FilterType.RANGE_GTE
        || filter.getValue0().filterType() == FilterType.RANGE_LT
        || filter.getValue0().filterType() == FilterType.RANGE_LTE) {
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.UNSUPPORTED, Level.INFO,
          "cannot combine range and not range filters on the same field");
      }
      inList.add(filter);
    }
    
    @Override
    public QueryBuilder query() throws ProvenanceException {
      BoolQueryBuilder fieldQuery = boolQuery();
      for (Pair<Field, Object> fieldFilter : inList) {
        switch(fieldFilter.getValue0().filterType()) {
          case EXACT: {
            String sVal = fieldFilter.getValue1().toString();
            fieldQuery.should(termQuery(fieldFilter.getValue0().elasticFieldName(), sVal));
          } break;
          case LIKE: {
            if (fieldFilter.getValue1() instanceof String) {
              String sVal = fieldFilter.getValue1().toString();
              fieldQuery.should(ElasticHelper.fullTextSearch(fieldFilter.getValue0().elasticFieldName(), sVal));
            } else {
              throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
                "like queries only work on string values");
            }
          } break;
          default:
            throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
              "in list filters do not allow: " + fieldFilter.getValue0().filterType());
        }
      }
      return fieldQuery;
    }
  }
  
  public static class FilterValRange implements FilterVal {
    String elasticFieldName;
    Pair<Field, Object> lower = null;
    Pair<Field, Object> upper = null;
    
    @Override
    public void add(Pair<Field, Object> filter) throws ProvenanceException {
      elasticFieldName = filter.getValue0().elasticFieldName();
      if(lower == null
        && (filter.getValue0().filterType() == FilterType.RANGE_GT
        || filter.getValue0().filterType() == FilterType.RANGE_GTE )) {
        lower = filter;
      } else if(upper == null
        && (filter.getValue0().filterType() == FilterType.RANGE_LT
        || filter.getValue0().filterType() == FilterType.RANGE_LTE)) {
        upper = filter;
      } else {
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
          "cannot combine range and not range filters on the same field or more than one lower or upper");
      }
    }
    
    @Override
    public QueryBuilder query() {
      RangeQueryBuilder fieldQuery = rangeQuery(elasticFieldName);
      if(upper != null) {
        switch(upper.getValue0().filterType()) {
          case RANGE_LT:
            fieldQuery.lt(upper.getValue1());
            break;
          case RANGE_LTE:
            fieldQuery.lte(upper.getValue1());
            break;
          default:
            //cannot get here due to checks in add
        }
      }
      if(lower != null) {
        switch(lower.getValue0().filterType()) {
          case RANGE_GT:
            fieldQuery.gt(lower.getValue1());
            break;
          case RANGE_GTE:
            fieldQuery.gte(lower.getValue1());
            break;
          default:
            //cannot get here due to checks in add
        }
      }
      return fieldQuery;
    }
  }
  
  public static class IntValParser implements ValParser<Integer> {
    
    @Override
    public Integer apply(Object o) throws ProvenanceException {
      try {
        if(o instanceof String) {
          return Integer.valueOf((String) o);
        } else if(o instanceof Number) {
          return ((Number)o).intValue();
        } else {
          String msg = "expected int - found " + o.getClass();
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg);
        }
      } catch (NumberFormatException e) {
        String msg = "expected int - found " + o.getClass();
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
          msg, msg, e);
      }
    }
  }
  
  public static class StringValParser implements ValParser<String> {
    
    @Override
    public String apply(Object o) throws ProvenanceException {
      if(o instanceof String) {
        return (String)o;
      } else {
        String msg = "expected string - found " + o.getClass();
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg);
      }
    }
    
  }
  
  public static class LongValParser implements ValParser<Long> {
    
    @Override
    public Long apply(Object o) throws ProvenanceException {
      try {
        if(o instanceof String) {
          return Long.valueOf((String)o);
        } else if (o instanceof Number) {
          return ((Number)o).longValue();
        } else {
          String msg = "expected long - found " + o.getClass();
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg);
        }
      } catch (NumberFormatException e) {
        String msg = "expected long - found " + o.getClass();
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg, msg, e);
      }
    }
  }
  
  public static class MLTypeValParser implements ValParser<Provenance.MLType> {
    @Override
    public Provenance.MLType apply(Object o) throws ProvenanceException {
      try {
        if(o instanceof String) {
          return Provenance.MLType.valueOf((String)o);
        } else {
          String msg = "expected string-ified version of MLType found " + o.getClass();
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg);
        }
      } catch (NullPointerException | IllegalArgumentException e) {
        String msg = "expected string-ified version of MLType found " + o.getClass();
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO, msg, msg, e);
      }
    }
  }
  
  public static SortOrder extractSortOrder(String val) throws ProvenanceException {
    try{
      return SortOrder.valueOf(val.toUpperCase());
    } catch(NullPointerException | IllegalArgumentException e) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
        "sort order " + val + " not supported - supported order:" + EnumSet.allOf(SortOrder.class),
        "exception extracting FilterBy param", e);
    }
  }
  
  public static Pair<String, String> extractXAttrParam(String param) throws ProvenanceException {
    String[] xattrParts = param.split(":");
    if(xattrParts.length != 2 || xattrParts[0].isEmpty()) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
        "malformed xattr:" + param);
    }
    return Pair.with(processXAttrKey(xattrParts[0]), xattrParts[1]);
  }
  
  public static String processXAttrKey(String key) {
    String[] keyParts = key.split("\\.");
    StringJoiner keyj = new StringJoiner(".");
    keyj.add("xattr_prov").add(keyParts[0]).add("value");
    for(int i = 1; i < keyParts.length; i++) keyj.add(keyParts[i]);
    //    }
    return keyj.toString();
  }
  
  public enum Expansions {
    APP("APP");
    
    public final String queryParamName;
    
    Expansions(String queryParamName) {
      this.queryParamName = queryParamName;
    }
    
    @Override
    public String toString() {
      return queryParamName;
    }
  }
  
  public static void addToFilters(Map<String, FilterVal> filters, Pair<Field, Object> filter)
    throws ProvenanceException {
    FilterVal fieldFilters = filters.get(filter.getValue0().queryFieldName());
    if(fieldFilters == null) {
      fieldFilters = ProvParser.filterValInstance(filter.getValue0().filterType());
      filters.put(filter.getValue0().queryFieldName(), fieldFilters);
    }
    fieldFilters.add(filter);
  }
  
  public static void withExpansions(Set<Expansions> expansions, Set<String> params)
    throws ProvenanceException {
    for(String param : params) {
      try {
        expansions.add(ProvParser.Expansions.valueOf(param));
      } catch (NullPointerException | IllegalArgumentException e) {
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
          "param " + param + " not supported - supported params:"
            + EnumSet.allOf(ProvParser.Expansions.class),
          "exception extracting FilterBy param", e);
      }
    }
  }
  
  public static Provenance.MLType mlTypeParser(ProvParser.DocSubType type) {
    switch(type) {
      case DATASET:
        return Provenance.MLType.DATASET;
      case HIVE:
        return Provenance.MLType.HIVE;
      case FEATURE:
      case FEATURE_PART:
        return Provenance.MLType.FEATURE;
      case TRAINING_DATASET:
      case TRAINING_DATASET_PART:
        return Provenance.MLType.TRAINING_DATASET;
      case EXPERIMENT:
      case EXPERIMENT_PART:
        return Provenance.MLType.EXPERIMENT;
      case MODEL:
      case MODEL_PART:
        return Provenance.MLType.MODEL;
      case NONE:
      default:
        return Provenance.MLType.NONE;
    }
  }
}
