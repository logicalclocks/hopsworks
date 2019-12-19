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
import io.hops.hopsworks.common.provenance.core.Provenance;
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

public class ProvFileStateParamBuilder {
  private Map<String, ProvParser.FilterVal> fileStateFilter = new HashMap<>();
  private List<Pair<ProvParser.Field, SortOrder>> fileStateSortBy = new ArrayList<>();
  private Map<String, String> exactXAttrFilter = new HashMap<>();
  private Map<String, String> likeXAttrFilter = new HashMap<>();
  private Set<String> hasXAttrFilter = new HashSet<>();
  private List<SortE> xAttrSortBy = new ArrayList<>();
  private Set<ProvParser.Expansions> expansions = new HashSet<>();
  private Map<String, ProvParser.FilterVal> appStateFilter = new HashMap<>();
  private Pair<Integer, Integer> pagination = null;
  
  public ProvFileStateParamBuilder withQueryParamFileStateFilterBy(Set<String> params) throws ProvenanceException {
    for(String param : params) {
      ProvParser.addToFilters(fileStateFilter, ProvSParser.extractFilter(param));
    }
    return this;
  }
  
  public ProvFileStateParamBuilder withQueryParamExactXAttr(Set<String> params) throws ProvenanceException {
    for(String param : params) {
      Pair<String, String> p = ProvParser.extractXAttrParam(param);
      exactXAttrFilter.put(p.getValue0(), p.getValue1());
    }
    return this;
  }
  
  public ProvFileStateParamBuilder withQueryParamLikeXAttr(Set<String> params) throws ProvenanceException {
    for(String param : params) {
      Pair<String, String> p = ProvParser.extractXAttrParam(param);
      likeXAttrFilter.put(p.getValue0(), p.getValue1());
    }
    return this;
  }
  
  public ProvFileStateParamBuilder withQueryParamExpansions(Set<String> params) throws ProvenanceException {
    ProvParser.withExpansions(expansions, params);
    return this;
  }
  
  public ProvFileStateParamBuilder withQueryParamAppExpansionFilter(Set<String> params) throws ProvenanceException {
    for(String param : params) {
      ProvParser.addToFilters(appStateFilter, ProvAParser.extractFilter(param));
    }
    return this;
  }
  
  public ProvFileStateParamBuilder withPagination(Integer offset, Integer limit) throws ProvenanceException {
    pagination = Pair.with(offset, limit);
    return this;
  }
  
  public Map<String, ProvParser.FilterVal> getFileStateFilter() {
    return fileStateFilter;
  }
  
  public List<Pair<ProvParser.Field, SortOrder>> getFileStateSortBy() {
    return fileStateSortBy;
  }
  
  public Map<String, String> getExactXAttrFilter() {
    return exactXAttrFilter;
  }
  
  public Map<String, String> getLikeXAttrFilter() {
    return likeXAttrFilter;
  }
  
  public Set<String> getHasXAttrFilter() {
    return hasXAttrFilter;
  }
  
  public List<SortE> getXAttrSortBy() {
    return xAttrSortBy;
  }
  
  public Set<ProvParser.Expansions> getExpansions() {
    return expansions;
  }
  
  public Map<String, ProvParser.FilterVal> getAppStateFilter() {
    return appStateFilter;
  }
  
  public Pair<Integer, Integer> getPagination() {
    return pagination;
  }
  
  public ProvFileStateParamBuilder withProjectInodeId(Long projectInodeId) throws ProvenanceException {
    ProvParser.addToFilters(fileStateFilter, Pair.with(ProvSParser.State.PROJECT_I_ID, projectInodeId));
    return this;
  }
  
  public ProvFileStateParamBuilder withFileInodeId(Long inodeId) throws ProvenanceException {
    ProvParser.addToFilters(fileStateFilter, Pair.with(ProvSParser.State.FILE_I_ID, inodeId));
    return this;
  }
  
  public ProvFileStateParamBuilder withFileName(String fileName) throws ProvenanceException {
    ProvParser.addToFilters(fileStateFilter, Pair.with(ProvSParser.State.FILE_NAME, fileName));
    return this;
  }
  
  public ProvFileStateParamBuilder withFileNameLike(String fileName) throws ProvenanceException {
    ProvParser.addToFilters(fileStateFilter, Pair.with(ProvSParser.StateAux.FILE_NAME_LIKE, fileName));
    return this;
  }
  
  public ProvFileStateParamBuilder withUserId(String userId) throws ProvenanceException {
    ProvParser.addToFilters(fileStateFilter, Pair.with(ProvSParser.State.USER_ID, userId));
    return this;
  }
  
  public ProvFileStateParamBuilder createdBefore(Long timestamp) throws ProvenanceException {
    ProvParser.addToFilters(fileStateFilter,
      Pair.with(ProvSParser.StateAux.CREATE_TIMESTAMP_LT, timestamp));
    return this;
  }
  
  public ProvFileStateParamBuilder createdAfter(Long timestamp) throws ProvenanceException {
    ProvParser.addToFilters(fileStateFilter,
      Pair.with(ProvSParser.StateAux.CREATE_TIMESTAMP_GT, timestamp));
    return this;
  }
  
  public ProvFileStateParamBuilder createdOn(Long timestamp) throws ProvenanceException {
    ProvParser.addToFilters(fileStateFilter, Pair.with(ProvSParser.State.CREATE_TIMESTAMP, timestamp));
    return this;
  }
  
  public ProvFileStateParamBuilder withAppId(String appId) throws ProvenanceException {
    ProvParser.addToFilters(fileStateFilter, Pair.with(ProvSParser.State.APP_ID, appId));
    return this;
  }
  
  public ProvFileStateParamBuilder withMlId(String mlId) throws ProvenanceException {
    ProvParser.addToFilters(fileStateFilter, Pair.with(ProvSParser.State.ML_ID, mlId));
    return this;
  }
  
  public ProvFileStateParamBuilder withMlType(String mlType) throws ProvenanceException {
    ProvParser.addToFilters(fileStateFilter, Pair.with(ProvSParser.State.ML_TYPE, mlType));
    return this;
  }
  
  public ProvFileStateParamBuilder withXAttrs(Map<String, String> xAttrs) {
    for(Map.Entry<String, String> xAttr : xAttrs.entrySet()) {
      withXAttr(xAttr.getKey(), xAttr.getValue());
    }
    return this;
  }
  
  public ProvFileStateParamBuilder withXAttr(String key, String val) {
    String xattrKey = ProvParser.processXAttrKey(key);
    exactXAttrFilter.put(xattrKey, val);
    return this;
  }
  
  public ProvFileStateParamBuilder withXAttrsLike(Map<String, String> xAttrs) {
    for(Map.Entry<String, String> xAttr : xAttrs.entrySet()) {
      withXAttrLike(xAttr.getKey(), xAttr.getValue());
    }
    return this;
  }
  
  public ProvFileStateParamBuilder withXAttrLike(String key, String val) {
    String xattrKey = ProvParser.processXAttrKey(key);
    likeXAttrFilter.put(xattrKey, val);
    return this;
  }
  
  public ProvFileStateParamBuilder withAppExpansion() {
    expansions.add(ProvParser.Expansions.APP);
    return this;
  }
  
  public ProvFileStateParamBuilder withAppExpansionCurrentState(Provenance.AppState currentAppState)
    throws ProvenanceException {
    withAppExpansion();
    ProvParser.addToFilters(appStateFilter,
      Pair.with(ProvAParser.Field.APP_STATE, currentAppState.name()));
    return this;
  }
  
  public ProvFileStateParamBuilder withAppExpansion(String appId) throws ProvenanceException {
    withAppExpansion();
    ProvParser.addToFilters(appStateFilter,
      Pair.with(ProvAParser.Field.APP_ID, appId));
    return this;
  }

  public ProvFileStateParamBuilder withQueryParamFileStateSortBy(List<String> params) throws ProvenanceException {
    for(String param : params) {
      fileStateSortBy.add(ProvSParser.extractSort(param));
    }
    return this;
  }

  public ProvFileStateParamBuilder withQueryParamXAttrSortBy(List<String> params) throws ProvenanceException {
    for(String param : params) {
      Pair<String, String> xattr = ProvParser.extractXAttrParam(param);
      SortOrder order = ProvParser.extractSortOrder(xattr.getValue1());
      xAttrSortBy.add(new SortE(xattr.getValue0(), order));
    }
    return this;
  }
  
  public ProvFileStateParamBuilder sortBy(String field, SortOrder order) {
    try {
      ProvParser.Field sortField = ProvSParser.extractField(field);
      fileStateSortBy.add(Pair.with(sortField, order));
    } catch(ProvenanceException ex) {
      String xattrKey = ProvParser.processXAttrKey(field);
      xAttrSortBy.add(new SortE(xattrKey, order));
    }
    return this;
  }
  
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
  
  public ProvFileStateParamBuilder filterByStateField(ProvParser.Field field, String val)
    throws ProvenanceException {
    if(!(field instanceof ProvSParser.State
      || field instanceof ProvSParser.StateAux)) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
        "allowed fields - FileState and fileStateAux");
    }
    Object v = field.filterValParser().apply(val);
    ProvParser.addToFilters(fileStateFilter, Pair.with(field, v));
    return this;
  }
  
  public ProvFileStateParamBuilder filterByHasXAttr(String key) {
    String xattrKey = ProvParser.processXAttrKey(key);
    hasXAttrFilter.add(xattrKey);
    return this;
  }
  
  public ProvFileStateParamBuilder filterByHasXAttr(Set<String> keys) {
    for(String key : keys) {
      String xattrKey = ProvParser.processXAttrKey(key);
      hasXAttrFilter.add(xattrKey);
    }
    return this;
  }
  
  public boolean hasAppExpansion() {
    return expansions.contains(ProvParser.Expansions.APP);
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
