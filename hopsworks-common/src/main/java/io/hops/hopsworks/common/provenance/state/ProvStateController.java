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

import com.lambdista.util.Try;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHits;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.common.provenance.app.ProvAppController;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.elastic.BasicElasticHit;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticCache;
import io.hops.hopsworks.common.elastic.ElasticClientController;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHelper;
import io.hops.hopsworks.common.provenance.app.ProvAppHelper;
import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateElastic;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateDTO;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.provenance.util.functional.CheckedFunction;
import io.hops.hopsworks.common.provenance.util.functional.CheckedSupplier;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ProvStateController {
  private final static Logger LOGGER = Logger.getLogger(ProvStateController.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private ElasticClientController client;
  @EJB
  private ProvAppController appCtrl;
  @EJB
  private ElasticCache cache;
  
  public ProvStateDTO provFileStateList(Project project, ProvStateParamBuilder params)
    throws ProvenanceException {
    if(params.base.pagination != null && !params.extensions.appStateFilter.isEmpty()) {
      String msg = "cannot use pagination with app state filtering";
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.UNSUPPORTED, Level.INFO, msg);
    }
    ProvStateDTO fileStates
      = provFileState(project, params.base, new HandlerFactory.BaseList(), Provenance.getProjectIndex(project));
    if (params.extensions.hasAppExpansion()) {
      //If withAppStates, update params based on appIds of items files and do a appState index query.
      //After this filter the fileStates based on the results of the appState query
      for (ProvStateDTO fileState : fileStates.getItems()) {
        Optional<String> appId = getAppId(fileState);
        if(appId.isPresent()) {
          params.withAppExpansion(appId.get());
        }
      }
      Map<String, Map<Provenance.AppState, ProvAppStateElastic>> appExps
        = appCtrl.provAppState(params.extensions.appStateFilter);
      Iterator<ProvStateDTO> fileStateIt = fileStates.getItems().iterator();
      while(fileStateIt.hasNext()) {
        ProvStateDTO fileState = fileStateIt.next();
        Optional<String> appId = getAppId(fileState);
        if(appId.isPresent() && appExps.containsKey(appId.get())) {
          Map<Provenance.AppState, ProvAppStateElastic> appExp = appExps.get(appId.get());
          fileState.setAppState(ProvAppHelper.buildAppState(appExp));
        } else {
          fileState.setAppState(ProvAppStateDTO.unknown());
        }
      }
    }
    return fileStates;
  }
  
  /**
   * @param <R> parsed elastic item
   * @param <S1> intermediate result wrapped in Try
   * @param <S2> final result
   * @return
   * @throws ProvenanceException
   */
  public <R, S1, S2> S2 provFileState(Project project, ProvStateParamBuilder.Base base,
    HandlerFactory<R, S1, S2> handlerFactory, String index) throws ProvenanceException {
    
    checkMapping(base, index);
    return provFileState(project.getInode().getId(),
      base.fileStateFilter, base.fileStateSortBy,
      base.exactXAttrFilter, base.likeXAttrFilter, base.hasXAttrFilter,
      base.xAttrSortBy, base.pagination.getValue0(), base.pagination.getValue1(), handlerFactory);
  }
  
  public ProvStateDTO provFileStateCount(Project project, ProvStateParamBuilder params)
    throws ProvenanceException {
    if(params.extensions.hasAppExpansion()) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.UNSUPPORTED, Level.INFO,
        "provenance file state count does not currently work with app state expansion");
    }
    return provFileStateCount(project.getInode().getId(), params.base.fileStateFilter,
      params.base.exactXAttrFilter, params.base.likeXAttrFilter, params.base.hasXAttrFilter);
  }
  
  private void checkMapping(ProvStateParamBuilder.Base base, String index)
    throws ProvenanceException {
    Map<String, String> mapping;
    try {
      mapping = cache.mngIndexGetMapping(index, false);
      try {
        base.fixSortBy(index, mapping);
      } catch(ProvenanceException e) {
        mapping = cache.mngIndexGetMapping(index, true);
        if(mapping.isEmpty()) {
          throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
            "provenance file state - no index");
        }
        base.fixSortBy(index, mapping);
      }
    } catch (ElasticException e) {
      String msg = "provenance - elastic query problem";
      throw ProvHelper.fromElastic(e, msg, msg + " - file state mapping");
    }
  }
  
  private Optional<String> getAppId(ProvStateDTO fileState) {
    if(fileState.getAppId().equals("none")) {
      if(fileState.getXattrs().containsKey("appId")) {
        return Optional.of(fileState.getXattrs().get("appId"));
      } else {
        return Optional.empty();
      }
    } else {
      return Optional.of(fileState.getAppId());
    }
  }
  
  /**
   * @param <R> parsed elastic item
   * @param <S1> intermediate result wrapped in Try
   * @param <S2> final result
   * @return
   * @throws ProvenanceException
   */
  private <R, S1, S2> S2 provFileState(Long projectIId,
    Map<ProvParser.Field, ProvParser.FilterVal> fileStateFilters,
    List<Pair<ProvParser.Field, SortOrder>> fileStateSortBy,
    Map<String, String> xAttrsFilters,
    Map<String, String> likeXAttrsFilters,
    Set<String> hasXAttrsFilters,
    List<ProvStateParamBuilder.SortE> xattrSortBy,
    Integer offset, Integer limit, HandlerFactory<R, S1, S2> handlerFactory)
    throws ProvenanceException {
    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      ElasticHelper.baseSearchRequest(
        settings.getProvFileIndex(projectIId),
        settings.getElasticDefaultScrollPageSize())
        .andThen(filterByStateParams(fileStateFilters, xAttrsFilters, likeXAttrsFilters, hasXAttrsFilters))
        .andThen(ElasticHelper.withFileStateOrder(fileStateSortBy, xattrSortBy))
        .andThen(ElasticHelper.withPagination(offset, limit, settings.getElasticMaxScrollPageSize()));
    SearchRequest request = srF.get();
    Pair<Long, Try<S1>> searchResult;
    try {
      searchResult = client.search(request, handlerFactory.getHandler());
    } catch (ElasticException e) {
      String msg = "provenance - elastic query problem";
      throw ProvHelper.fromElastic(e, msg, msg + " - file state");
    }
    return handlerFactory.checkedResult(searchResult);
  }
  
  private ProvStateDTO provFileStateCount(Long projectIId, Map<ProvParser.Field, ProvParser.FilterVal> fileStateFilters,
    Map<String, String> xAttrsFilters, Map<String, String> likeXAttrsFilters, Set<String> hasXAttrsFilters)
    throws ProvenanceException {
    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      ElasticHelper.countSearchRequest(
        settings.getProvFileIndex(projectIId))
        .andThen(filterByStateParams(fileStateFilters, xAttrsFilters, likeXAttrsFilters, hasXAttrsFilters));
    SearchRequest request = srF.get();
    Long searchResult;
    try {
      searchResult = client.searchCount(request);
    } catch (ElasticException e) {
      String msg = "provenance - elastic query problem";
      throw ProvHelper.fromElastic(e, msg, msg + " - file state count");
    }
    ProvStateDTO container = new ProvStateDTO();
    container.setCount(searchResult);
    return container;
  }
  
  private CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> filterByStateParams(
    Map<ProvParser.Field, ProvParser.FilterVal> fileStateFilters, Map<String, String> xAttrsFilters,
    Map<String, String> likeXAttrsFilters, Set<String> hasXAttrsFilters) {
    return (SearchRequest sr) -> {
      BoolQueryBuilder query = boolQuery()
        .must(termQuery(ProvParser.Fields.ENTRY_TYPE.toString().toLowerCase(),
          ProvParser.EntryType.STATE.toString().toLowerCase()));
      query = ElasticHelper.filterByBasicFields(query, fileStateFilters);
      for (Map.Entry<String, String> filter : xAttrsFilters.entrySet()) {
        query = query.must(getXAttrQB(filter.getKey(), filter.getValue()));
      }
      for (Map.Entry<String, String> filter : likeXAttrsFilters.entrySet()) {
        query = query.must(getLikeXAttrQB(filter.getKey(), filter.getValue()));
      }
      for(String xattrKey : hasXAttrsFilters) {
        query = query.must(hasXAttrQB(xattrKey));
      }
      sr.source().query(query);
      return sr;
    };
  }
  
  public QueryBuilder hasXAttrQB(String xattrAdjustedKey) {
    return existsQuery(xattrAdjustedKey);
  }
  
  public QueryBuilder getXAttrQB(String xattrAdjustedKey, String xattrVal) {
    return termQuery(xattrAdjustedKey, xattrVal.toLowerCase());
  }
  
  public QueryBuilder getLikeXAttrQB(String xattrAdjustedKey, String xattrVal) {
    return ElasticHelper.fullTextSearch(xattrAdjustedKey, xattrVal);
  }
  
  public interface HandlerFactory<R, S1, S2> {
    ElasticHits.Handler<R, S1> getHandler();
    S2 checkedResult(Pair<Long, Try<S1>> result) throws ProvenanceException;
    
    class BaseList implements HandlerFactory<ProvStateDTO, List<ProvStateDTO>, ProvStateDTO> {
      public ElasticHits.Handler<ProvStateDTO, List<ProvStateDTO>> getHandler() {
        ElasticHits.Parser<ProvStateDTO> parser =
          hit -> ProvStateParser.tryInstance(BasicElasticHit.instance(hit));
        return ElasticHits.handlerAddToList(parser);
      }
  
      public ProvStateDTO checkedResult(Pair<Long, Try<List<ProvStateDTO>>> result) throws ProvenanceException {
        try {
          ProvStateDTO container = new ProvStateDTO();
          container.setItems(result.getValue1().checkedGet());
          container.setCount(result.getValue0());
          return container;
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
    
    class MLIdSet implements HandlerFactory<String, Set<String>, Pair<Long, Set<String>>> {
      @Override
      public ElasticHits.Handler<String, Set<String>> getHandler() {
        ElasticHits.Parser<String> mlIdParser =
          hit -> ProvStateParser.tryInstance(BasicElasticHit.instance(hit))
            .flatMap(s -> Try.apply(s::getMlId));
        return ElasticHits.handlerAddToSet(mlIdParser);
      }
      
      @Override
      public Pair<Long, Set<String>> checkedResult(Pair<Long, Try<Set<String>>> result) throws ProvenanceException {
        try {
          return Pair.with(result.getValue0(), result.getValue1().checkedGet());
        } catch (Throwable t) {
          if (t instanceof ProvenanceException) {
            throw (ProvenanceException) t;
          } else {
            throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.INTERNAL_ERROR, Level.INFO,
              "unhandled error", "unhandled error", t);
          }
        }
      }
    }
  }
}
