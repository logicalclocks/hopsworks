/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.ops;

import com.lambdista.util.Try;
import io.hops.hopsworks.common.provenance.app.ProvAppController;
import io.hops.hopsworks.common.provenance.app.ProvAppHelper;
import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateDTO;
import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateElastic;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.elastic.BasicElasticHit;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticAggregationParser;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHelper;
import io.hops.hopsworks.common.provenance.core.elastic.ElasticHits;
import io.hops.hopsworks.common.provenance.core.elastic.ProvElasticController;
import io.hops.hopsworks.common.provenance.ops.dto.ProvLinksDTO;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.common.provenance.state.ProvStateParamBuilder;
import io.hops.hopsworks.common.provenance.state.ProvStateParser;
import io.hops.hopsworks.common.provenance.state.ProvStateController;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.provenance.util.functional.CheckedFunction;
import io.hops.hopsworks.common.provenance.util.functional.CheckedSupplier;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ProvOpsController {
  private final static Logger LOGGER = Logger.getLogger(ProvOpsController.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private ProvElasticController client;
  @EJB
  private ProvAppController appCtrl;
  @EJB
  private ProvStateController stateCtrl;
  
  public ProvOpsDTO provFileOpsList(Project project, ProvOpsParamBuilder params)
    throws ProvenanceException {
    if(!params.aggregations.isEmpty()) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.UNSUPPORTED, Level.INFO,
        "aggregations currently only allowed with count");
    }
    if(params.pagination == null || params.pagination.getValue0() == null || params.pagination.getValue1() == null) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.UNSUPPORTED, Level.INFO,
        "all searches should be paginated");
    }
  
    ProvOpsDTO fileOps = provFileOpsBase(project.getInode().getId(),
        params.fileOpsFilterBy, params.fileOpsSortBy,
        params.pagination.getValue0(), params.pagination.getValue1());
    
    if (params.hasAppExpansion()) {
      //If withAppStates, update params based on appIds of items files and do a appState index query.
      //After this filter the fileStates based on the results of the appState query
      for (ProvOpsDTO fileOp : fileOps.getItems()) {
        Optional<String> appId = getAppId(fileOp);
        if(appId.isPresent()) {
          params.withAppExpansion(appId.get());
        }
      }
      Map<String, Map<Provenance.AppState, ProvAppStateElastic>> appExps
        = appCtrl.provAppState(params.appStateFilter);
      Iterator<ProvOpsDTO> fileOpIt = fileOps.getItems().iterator();
      while (fileOpIt.hasNext()) {
        ProvOpsDTO fileOp = fileOpIt.next();
        Optional<String> appId = getAppId(fileOp);
        if(appId.isPresent() && appExps.containsKey(appId.get())) {
          Map<Provenance.AppState, ProvAppStateElastic> appExp = appExps.get(appId.get());
          fileOp.setAppState(ProvAppHelper.buildAppState(appExp));
        } else {
          fileOp.setAppState(ProvAppStateDTO.unknown());
        }
      }
    }
    return fileOps;
  }
  
  public ProvOpsDTO provFileOpsCount(Project project, ProvOpsParamBuilder params)
    throws ProvenanceException {
    return provFileOpsCount(project.getInode().getId(),
      params.fileOpsFilterBy, params.aggregations);
  }
  
  public ProvOpsDTO provFileOpsCount(Long projectIId,
    Map<ProvParser.Field, ProvParser.FilterVal> fileOpsFilters,
    Set<ProvOpsElastic.Aggregations> aggregations)
    throws ProvenanceException {
    
    Map<ProvOpsElastic.Aggregations, ElasticAggregationParser<?, ProvenanceException>> aggParsers = new HashMap<>();
    List<AggregationBuilder> aggBuilders = new ArrayList<>();
    for(ProvOpsElastic.Aggregations aggregation : aggregations) {
      aggParsers.put(aggregation, ProvOpsElastic.getAggregationParser(aggregation));
      aggBuilders.add(ProvOpsElastic.getAggregationBuilder(aggregation));
    }
    
    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      ElasticHelper.countSearchRequest(
        settings.getProvFileIndex(projectIId))
        .andThen(filterByOpsParams(fileOpsFilters))
        .andThen(ElasticHelper.withAggregations(aggBuilders));
    SearchRequest request = srF.get();
    
    Pair<Long, Map<ProvOpsElastic.Aggregations, List>> result;
    try {
      result = client.searchCount(request, aggParsers);
    } catch (ElasticException e) {
      String msg = "provenance - elastic query problem";
      throw ProvHelper.fromElastic(e, msg, msg + " - file ops");
    }
    ProvOpsDTO container = new ProvOpsDTO();
    container.setCount(result.getValue0());
    return container;
  }
  
  private Optional<String> getAppId(ProvOpsDTO fileOp) {
    if(fileOp.getAppId().equals("none")) {
      return Optional.empty();
    } else {
      return Optional.of(fileOp.getAppId());
    }
  }
  
  public ProvOpsDTO provFileOpsScrolling(Long projectIId, Map<ProvParser.Field, ProvParser.FilterVal> fileOpsFilters,
    List<Pair<ProvParser.Field, SortOrder>> fileOpsSortBy)
    throws ProvenanceException {
    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      ElasticHelper.scrollingSearchRequest(
        settings.getProvFileIndex(projectIId),
        settings.getElasticDefaultScrollPageSize())
        .andThen(filterByOpsParams(fileOpsFilters))
        .andThen(ElasticHelper.withFileOpsOrder(fileOpsSortBy));
    SearchRequest request = srF.get();
    Pair<Long, Try<List<ProvOpsDTO>>> searchResult;
    try {
      searchResult = client.searchScrolling(request, ProvOpsHandlerFactory.getHandler());
    } catch (ElasticException e) {
      String msg = "provenance - elastic query problem";
      throw ProvHelper.fromElastic(e, msg, msg + " - file ops");
    }
    return ProvOpsHandlerFactory.checkedResult(searchResult);
  }
  
  public ProvOpsDTO provFileOpsBase(Long projectIId, Map<ProvParser.Field, ProvParser.FilterVal> fileOpsFilters,
    List<Pair<ProvParser.Field, SortOrder>> fileOpsSortBy, Integer offset, Integer limit)
    throws ProvenanceException {
    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      ElasticHelper.baseSearchRequest(settings.getProvFileIndex(projectIId), settings.getElasticDefaultScrollPageSize())
        .andThen(filterByOpsParams(fileOpsFilters))
        .andThen(ElasticHelper.withFileOpsOrder(fileOpsSortBy))
        .andThen(ElasticHelper.withPagination(offset, limit, settings.getElasticMaxScrollPageSize()));
    SearchRequest request = srF.get();
    Pair<Long, Try<List<ProvOpsDTO>>> searchResult;
    try {
      searchResult = client.search(request, ProvOpsHandlerFactory.getHandler());
    } catch (ElasticException e) {
      String msg = "provenance - elastic query problem";
      throw ProvHelper.fromElastic(e, msg, msg + " - file ops");
    }
    return ProvOpsHandlerFactory.checkedResult(searchResult);
  }
  
  /**
   *
   * @param project
   * @param params - no clone defined yet, so the method might modify the params
   * @return
   * @throws ProvenanceException
   */
  public ProvLinksDTO provLinks(Project project, ProvLinksParamBuilder params, boolean filterAlive)
    throws ProvenanceException {
    HandlerFactory.AppIdMlIdMap linkHandlerFactory = new HandlerFactory.AppIdMlIdMap();
    ProvLinksDTO.Builder inLinks;
    ProvLinksDTO.Builder outLinks;
    if(params.inArtifactDefined) {
      inLinks = provInLinks(provLinks(project.getInode().getId(), params.inFilterBy, linkHandlerFactory));
      if(inLinks.getAppLinks().isEmpty()) {
        //if the artifacts (in) were not used in any application, there will not be any out artifacts of interest
        return new ProvLinksDTO();
      }
      /**
       * if an app was not defined in the query, we refine step 2 with appIds where the artifacts defined by in query
       * were used as source
       */
      if(!params.appIdDefined) {
        for (Map.Entry<String, ProvLinksDTO> app : inLinks.getAppLinks().entrySet()) {
          if(!app.getValue().getIn().isEmpty()) {
            params.outFilterByField(ProvLinksParser.FieldsPF.APP_ID, app.getKey());
          }
        }
      }
      outLinks = provOutLinks(provLinks(project.getInode().getId(), params.outFilterBy, linkHandlerFactory));
      if(outLinks.getAppLinks().isEmpty()) {
        return new ProvLinksDTO();
      }
      Set<String> outAlive = null;
      if(filterAlive) {
        outAlive = getLinksAlive(project, splitOutLinks(outLinks));
      }
      return mergeLinks(inLinks, outLinks, null, outAlive, params.fullLink);
    } else if(params.outArtifactDefined) {
      outLinks = provOutLinks(provLinks(project.getInode().getId(), params.outFilterBy, linkHandlerFactory));
      if(outLinks.getAppLinks().isEmpty()) {
        //if the artifacts (out) were not generated in any application, there will not be any in artifacts of interest
        return new ProvLinksDTO();
      }
      /**
       * if an app was not defined in the query, we refine step 2 with appIds where the artifacts defined by out query
       * were generated
       */
      if(!params.appIdDefined) {
        for (Map.Entry<String, ProvLinksDTO> app : outLinks.getAppLinks().entrySet()) {
          if(!app.getValue().getIn().isEmpty()) {
            params.inFilterByField(ProvLinksParser.FieldsPF.APP_ID, app.getKey());
          }
        }
      }
      inLinks = provInLinks(provLinks(project.getInode().getId(), params.inFilterBy, linkHandlerFactory));
      if(inLinks.getAppLinks().isEmpty()) {
        return new ProvLinksDTO();
      }
      Set<String> inAlive = null;
      if(filterAlive) {
        inAlive = getLinksAlive(project, splitInLinks(inLinks));
      }
      return mergeLinks(inLinks, outLinks, inAlive, null, params.fullLink);
    } else if(params.appIdDefined){
      inLinks = provInLinks(provLinks(project.getInode().getId(), params.inFilterBy, linkHandlerFactory));
      if(inLinks.getAppLinks().isEmpty()) {
        return new ProvLinksDTO();
      }
      outLinks = provOutLinks(provLinks(project.getInode().getId(), params.outFilterBy, linkHandlerFactory));
      if(outLinks.getAppLinks().isEmpty()) {
        return new ProvLinksDTO();
      }
      Set<String> inAlive = null;
      Set<String> outAlive = null;
      if(filterAlive) {
        inAlive = getLinksAlive(project, splitInLinks(inLinks));
        outAlive = getLinksAlive(project, splitOutLinks(outLinks));
      }
      return mergeLinks(inLinks, outLinks, inAlive, outAlive, params.fullLink);
    } else {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
        "query too vague - please define at least one of APP_ID, IN_ARTIFACT, OUT_ARTIFACT",
        "full provenance link query error");
    }
  }
  
  private ProvLinksDTO mergeLinks(ProvLinksDTO.Builder inLinks, ProvLinksDTO.Builder outLinks,
    Set<String> inAlive, Set<String> outAlive, boolean fullLink) {
    ProvLinksDTO.Builder result = new ProvLinksDTO.Builder();
    for(Map.Entry<String, ProvLinksDTO> e: inLinks.getAppLinks().entrySet()) {
      if(inAlive != null) {
        e.getValue().getIn().entrySet().removeIf(ee -> !inAlive.contains(ee.getKey()));
      }
      result.addInArtifacts(e.getKey(), e.getValue().getIn());
    }
    for(Map.Entry<String, ProvLinksDTO> e: outLinks.getAppLinks().entrySet()) {
      if(outAlive != null) {
        e.getValue().getOut().entrySet().removeIf(ee -> !outAlive.contains(ee.getKey()));
      }
      result.addOutArtifacts(e.getKey(), e.getValue().getOut());
    }
    Iterator<Map.Entry<String, ProvLinksDTO>> it = result.getAppLinks().entrySet().iterator();
    while(it.hasNext()) {
      ProvLinksDTO appLinks = it.next().getValue();
      if(appLinks.getIn().isEmpty() && appLinks.getOut().isEmpty()) {
        it.remove();
      } else if(fullLink && (appLinks.getIn().isEmpty() || appLinks.getOut().isEmpty())) {
        it.remove();
      }
    }
    return result.build();
  }
  
  private ProvLinksDTO.Builder provInLinks(ProvLinksDTO.Builder raw) {
    raw.getAppLinks().entrySet().removeIf(e -> e.getValue().getIn().isEmpty());
    return raw;
  }
  
  private ProvLinksDTO.Builder provOutLinks(ProvLinksDTO.Builder raw) {
    raw.getAppLinks().entrySet().removeIf(e -> e.getValue().getOut().isEmpty());
    return raw;
  }
  
  enum LinkTypes {
    FEATURESTORE,
    ML,
    OTHER;
  }
  private Map<LinkTypes, List<ProvOpsDTO>> splitInLinks(ProvLinksDTO.Builder links) {
    return links.getAppLinks().values().stream().flatMap(appLinks -> appLinks.getIn().values().stream())
      .collect(Collectors.groupingBy(this::getLinkType));
  }
  
  private Map<LinkTypes, List<ProvOpsDTO>> splitOutLinks(ProvLinksDTO.Builder links) {
    return links.getAppLinks().values().stream().flatMap(appLinks -> appLinks.getOut().values().stream())
      .collect(Collectors.groupingBy(this::getLinkType));
  }
  
  private LinkTypes getLinkType(ProvOpsDTO link) {
    switch(link.getDocSubType()) {
      case FEATURE:
      case FEATURE_PART:
      case TRAINING_DATASET:
      case TRAINING_DATASET_PART:
        return LinkTypes.FEATURESTORE;
      case EXPERIMENT:
      case EXPERIMENT_PART:
      case MODEL:
      case MODEL_PART:
        return LinkTypes.ML;
      default:
        return LinkTypes.OTHER;
    }
  }
  
  private Set<String> getLinksAlive(Project project, Map<LinkTypes, List<ProvOpsDTO>> links)
    throws ProvenanceException {
    ProvStateParamBuilder params1 = new ProvStateParamBuilder();
    ProvStateParamBuilder params2 = new ProvStateParamBuilder();
    int mlSize1 = links.getOrDefault(LinkTypes.FEATURESTORE, new LinkedList<>()).size();
    int mlSize2 = links.getOrDefault(LinkTypes.ML, new LinkedList<>()).size();
    ProvStateController.HandlerFactory<String, Set<String>, Pair<Long, Set<String>>> stateHandlerFactory
      = new ProvStateController.HandlerFactory.MLIdSet();
    Set<String> outAlive = new HashSet<>();
    if(mlSize1 > 0) {
      for(ProvOpsDTO link : links.get(LinkTypes.FEATURESTORE)) {
        params1.filterByField(ProvStateParser.FieldsP.ML_ID, link.getMlId());
      }
      params1.paginate(0, mlSize1);
      String index = Settings.FEATURESTORE_INDEX;
      outAlive.addAll(stateCtrl.provFileState(project, params1.base, stateHandlerFactory, index).getValue1());
    }
    if(mlSize2 > 0) {
      for(ProvOpsDTO link : links.get(LinkTypes.ML)) {
        params2.filterByField(ProvStateParser.FieldsP.ML_ID, link.getMlId());
      }
      params2.paginate(0, mlSize2);
      String index = Provenance.getProjectIndex(project);
      outAlive.addAll(stateCtrl.provFileState(project, params2.base, stateHandlerFactory, index).getValue1());
    }
    return outAlive;
  }
  
  private <O1, O2> O2 provLinks(Long projectIId, Map<ProvParser.Field, ProvParser.FilterVal> filterByFields,
    HandlerFactory<O1, O2> handlerFactory)
    throws ProvenanceException {
    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      ElasticHelper
        .scrollingSearchRequest(settings.getProvFileIndex(projectIId), settings.getElasticDefaultScrollPageSize())
        .andThen(filterByOpsParams2(filterByFields))
        .andThen(ElasticHelper.withPagination(null, null, settings.getElasticMaxScrollPageSize()));
    SearchRequest request = srF.get();
    Pair<Long, Try<O1>> searchResult;
    try {
      searchResult = client.searchScrolling(request, handlerFactory.getHandler());
    } catch (ElasticException e) {
      String msg = "provenance - elastic query problem";
      throw ProvHelper.fromElastic(e, msg, msg + " - file ops");
    }
    return handlerFactory.checkedResult(searchResult.getValue1());
  }
  
  private CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> filterByOpsParams(
    Map<ProvParser.Field, ProvParser.FilterVal> fileOpsFilters) {
    return (SearchRequest sr) -> {
      BoolQueryBuilder query = QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery(ProvParser.Fields.ENTRY_TYPE.toString().toLowerCase(),
          ProvParser.EntryType.OPERATION.toString().toLowerCase()));
      query = ElasticHelper.filterByBasicFields(query, fileOpsFilters);
      sr.source().query(query);
      return sr;
    };
  }
  
  private CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> filterByOpsParams2(
    Map<ProvParser.Field, ProvParser.FilterVal> fileOpsFilters) {
    return (SearchRequest sr) -> {
      BoolQueryBuilder query = QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery(ProvParser.Fields.ENTRY_TYPE.toString().toLowerCase(),
          ProvParser.EntryType.OPERATION.toString().toLowerCase()));
      query = ElasticHelper.filterByBasicFields(query, fileOpsFilters);
      sr.source().query(query);
      return sr;
    };
  }
  
  static class ProvOpsHandlerFactory {
    static ElasticHits.Handler<ProvOpsDTO, List<ProvOpsDTO>> getHandler() {
      ElasticHits.Parser<ProvOpsDTO> parser
        = hit -> ProvOpsParser.tryInstance(BasicElasticHit.instance(hit));
      return ElasticHits.handlerAddToList(parser);
    }
    static ProvOpsDTO checkedResult(Pair<Long, Try<List<ProvOpsDTO>>> result)
      throws ProvenanceException {
      try {
        ProvOpsDTO container = new ProvOpsDTO();
        container.setCount(result.getValue0());
        container.setItems(result.getValue1().checkedGet());
        return container;
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
  
  static class AppState {
    //<ml_id, dto>
    Map<String, ProvOpsDTO> in = new HashMap<>();
    Map<String, ProvOpsDTO> out = new HashMap<>();
  }
  
  /**
   * We assume immutable artifacts, otherwise query is too complex for elastic.
   * This means that if we detect one CREATE operation, this means the whole artifact is being created
   * and if we only detect READ operations, the artifact is being used.
   */
  static final ElasticHits.Merger<ProvOpsDTO, Map<String, AppState>> provLinksMerger
    = (ProvOpsDTO hit, Map<String, AppState> state) -> {
      if (hit.getAppId().equals("none")) {
        return Try.apply(() -> state);
      }
      switch (hit.getInodeOperation()) {
        case CREATE: {
          AppState appArtifacts = state.get(hit.getAppId());
          if (appArtifacts == null) {
            appArtifacts = new AppState();
            state.put(hit.getAppId(), appArtifacts);
          }
          // assume immutable artifacts
          appArtifacts.in.remove(hit.getMlId());
          
          ProvOpsDTO s = appArtifacts.out.get(hit.getMlId());
          if (s == null) {
            s = hit.upgradePart();
            appArtifacts.out.put(hit.getMlId(), s);
          }
          // timestamp marks when you are done creating it - we want the highest (last)
          if (s.getTimestamp() < hit.getTimestamp()) {
            s.setTimestamp(hit.getTimestamp());
            s.setLogicalTime(hit.getLogicalTime());
            s.setReadableTimestamp(hit.getReadableTimestamp());
          }
        }
        break;
        case ACCESS_DATA: {
          AppState appArtifacts = state.get(hit.getAppId());
          if (appArtifacts == null) {
            appArtifacts = new AppState();
            state.put(hit.getAppId(), appArtifacts);
          }
          if (appArtifacts.out.containsKey(hit.getMlId())) {
            break;
          }
          ProvOpsDTO s = appArtifacts.in.get(hit.getMlId());
          if (s == null) {
            s = hit.upgradePart();
            appArtifacts.in.put(hit.getMlId(), s);
          }
          // timestamp marks when you start reading it - we want the lowest (first)
          if (s.getTimestamp() > hit.getTimestamp()) {
            s.setTimestamp(hit.getTimestamp());
            s.setLogicalTime(hit.getLogicalTime());
            s.setReadableTimestamp(hit.getReadableTimestamp());
          }
        }
        break;
      }
      return Try.apply(() -> state);
    };
    
  interface HandlerFactory<O1, O2> {
    ElasticHits.Handler<ProvOpsDTO, O1> getHandler();
    O2 checkedResult(Try<O1> result) throws ProvenanceException;
    
    class AppIdMlIdMap implements HandlerFactory<Map<String, AppState>, ProvLinksDTO.Builder> {
      public ElasticHits.Handler<ProvOpsDTO, Map<String, AppState>> getHandler() {
        ElasticHits.Parser<ProvOpsDTO> parser
          = hit -> ProvOpsParser.tryMLInstance(BasicElasticHit.instance(hit));
        Map<String, AppState> initState = new HashMap<>();
        return ElasticHits.handlerBasic(parser, initState, provLinksMerger);
      }
  
      public ProvLinksDTO.Builder checkedResult(Try<Map<String, AppState>> result) throws ProvenanceException {
        try {
          ProvLinksDTO.Builder r = new ProvLinksDTO.Builder();
          for (Map.Entry<String, AppState> app : result.checkedGet().entrySet()) {
            r.addArtifacts(app.getKey(), app.getValue().in, app.getValue().out);
          }
          return r;
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
