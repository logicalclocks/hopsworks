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
package io.hops.hopsworks.common.provenance.state;

import io.hops.hopsworks.common.provenance.app.ProvAParser;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.search.sort.SortOrder;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ProvStateParamBuilder {
  public static class Base {
    Map<ProvParser.Field, ProvParser.FilterVal> fileStateFilter = new HashMap<>();
    List<Pair<ProvParser.Field, SortOrder>> fileStateSortBy = new ArrayList<>();
    Map<String, String> exactXAttrFilter = new HashMap<>();
    Map<String, String> likeXAttrFilter = new HashMap<>();
    Set<String> hasXAttrFilter = new HashSet<>();
    List<SortE> xAttrSortBy = new ArrayList<>();
    Pair<Integer, Integer> pagination = null;
  
    public void fixSortBy(String index, Map<String, String> mapping) throws ProvenanceException {
      for(SortE s : xAttrSortBy) {
        String type = mapping.get(s.key);
        if(type == null) {
          String devMsg = "missing mapping for field[" + s.key + "] - index[" + index + "] - ";
          String usrMsg = "bad field[" + s.key + "]";
          ProvenanceException ex = new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST,
            Level.INFO, usrMsg, devMsg);
          throw ex;
        }
        if(type.equals("text")) {
          s.key = s.key + ".keyword";
        }
      }
    }
  }
  
  public static class Extensions {
    Set<ProvParser.ElasticExpansions> expansions = new HashSet<>();
    Map<ProvParser.Field, ProvParser.FilterVal> appStateFilter = new HashMap<>();
  
    public boolean hasAppExpansion() {
      return expansions.contains(ProvParser.ElasticExpansions.APP);
    }
  }
  
  public Base base = new Base();
  public Extensions extensions = new Extensions();
  
  public ProvStateParamBuilder paginate(Integer offset, Integer limit) {
    base.pagination = Pair.with(offset, limit);
    return this;
  }
  
  public ProvStateParamBuilder filterByFields(Set<String> params) throws ProvenanceException {
    for(String param : params) {
      Pair<ProvStateParser.Field, Object> filter = ProvStateParser.extractFilter(param);
      ProvParser.addToFilters(base.fileStateFilter, filter.getValue0(), filter.getValue1());
    }
    return this;
  }
  
  public ProvStateParamBuilder filterByField(ProvStateParser.Field field, Object val)
    throws ProvenanceException {
    Object checkedVal = field.filterValParser().apply(val);
    ProvParser.addToFilters(base.fileStateFilter, field, checkedVal);
    return this;
  }
  
  public ProvStateParamBuilder filterByXAttrs(Set<String> params) throws ProvenanceException {
    for(String param : params) {
      Pair<String, String> p = ProvParser.extractXAttrParam(param);
      base.exactXAttrFilter.put(p.getValue0(), p.getValue1());
    }
    return this;
  }
  
  public ProvStateParamBuilder filterByXAttrs(Map<String, String> xAttrs) {
    for(Map.Entry<String, String> xAttr : xAttrs.entrySet()) {
      filterByXAttr(xAttr.getKey(), xAttr.getValue());
    }
    return this;
  }
  
  public ProvStateParamBuilder filterByXAttr(String key, String val) {
    String xattrKey = ProvParser.processXAttrKey(key);
    base.exactXAttrFilter.put(xattrKey, val);
    return this;
  }
  
  public ProvStateParamBuilder filterLikeXAttrs(Set<String> params) throws ProvenanceException {
    for(String param : params) {
      Pair<String, String> p = ProvParser.extractXAttrParam(param);
      base.likeXAttrFilter.put(p.getValue0(), p.getValue1());
    }
    return this;
  }
  
  public ProvStateParamBuilder filterLikeXAttrs(Map<String, String> xAttrs) {
    for(Map.Entry<String, String> xAttr : xAttrs.entrySet()) {
      filterLikeXAttr(xAttr.getKey(), xAttr.getValue());
    }
    return this;
  }
  
  public ProvStateParamBuilder filterLikeXAttr(String key, String val) {
    String xattrKey = ProvParser.processXAttrKey(key);
    base.likeXAttrFilter.put(xattrKey, val);
    return this;
  }
  
  public ProvStateParamBuilder sortByFields(List<String> params) throws ProvenanceException {
    for(String param : params) {
      Pair<ProvParser.Field, SortOrder> field = ProvStateParser.extractSort(param);
      base.fileStateSortBy.add(field);
    }
    return this;
  }
  
  public ProvStateParamBuilder sortByField(ProvStateParser.Field field, SortOrder order) {
    base.fileStateSortBy.add(Pair.with(field, order));
    return this;
  }
  
  public ProvStateParamBuilder sortByXAttrs(List<String> params) throws ProvenanceException {
    for(String param : params) {
      Pair<String, String> xattr = ProvParser.extractXAttrParam(param);
      SortOrder order = ProvParser.extractSortOrder(xattr.getValue1());
      base.xAttrSortBy.add(new SortE(xattr.getValue0(), order));
    }
    return this;
  }
  
  public ProvStateParamBuilder sortByXAttr(String field, SortOrder order) {
    String xattrKey = ProvParser.processXAttrKey(field);
    base.xAttrSortBy.add(new SortE(xattrKey, order));
    return this;
  }
  
  public ProvStateParamBuilder hasXAttr(String key) {
    String xattrKey = ProvParser.processXAttrKey(key);
    base.hasXAttrFilter.add(xattrKey);
    return this;
  }
  
  public ProvStateParamBuilder hasXAttrs(Set<String> keys) {
    for(String key : keys) {
      hasXAttr(key);
    }
    return this;
  }
  
  public ProvStateParamBuilder withAppExpansion() {
    extensions.expansions.add(ProvParser.ElasticExpansions.APP);
    return this;
  }
  
  public ProvStateParamBuilder withAppExpansion(String appId) throws ProvenanceException {
    withAppExpansion();
    ProvParser.addToFilters(extensions.appStateFilter, Pair.with(ProvAParser.Field.APP_ID, appId));
    return this;
  }
  
  public ProvStateParamBuilder withAppExpansionFilter(Set<String> params) throws ProvenanceException {
    for(String param : params) {
      ProvParser.addToFilters(extensions.appStateFilter, ProvAParser.extractFilter(param));
    }
    return this;
  }
  
  public ProvStateParamBuilder withExpansions(Set<String> params) throws ProvenanceException {
    ProvParser.withExpansions(extensions.expansions, params);
    return this;
  }
  
  public static class SortE {
    public String key;
    public SortOrder order;
    
    public SortE(String key, SortOrder order) {
      this.key = key;
      this.order = order;
    }
  }
}
