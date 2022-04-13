/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.ops;

import com.google.gson.Gson;
import com.lambdista.util.Try;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturegroupXAttr;
import io.hops.hopsworks.common.provenance.ops.ProvLinksParamBuilder;
import io.hops.hopsworks.common.integrations.EnterpriseStereotype;
import io.hops.hopsworks.common.provenance.app.ProvAppController;
import io.hops.hopsworks.common.provenance.app.ProvAppHelper;
import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateDTO;
import io.hops.hopsworks.common.provenance.app.dto.ProvAppStateOpenSearch;
import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.opensearch.BasicOpenSearchHit;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchAggregationParser;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchHelper;
import io.hops.hopsworks.common.provenance.core.opensearch.OpenSearchHits;
import io.hops.hopsworks.common.opensearch.OpenSearchClientController;
import io.hops.hopsworks.common.provenance.ops.ProvLinks;
import io.hops.hopsworks.common.provenance.ops.ProvOpsAggregations;
import io.hops.hopsworks.common.provenance.ops.ProvOpsControllerIface;
import io.hops.hopsworks.common.provenance.ops.ProvOpsParamBuilder;
import io.hops.hopsworks.common.provenance.ops.dto.ProvLinksDTO;
import io.hops.hopsworks.common.provenance.ops.dto.ProvOpsDTO;
import io.hops.hopsworks.common.provenance.state.ProvStateParamBuilder;
import io.hops.hopsworks.common.provenance.state.ProvStateParser;
import io.hops.hopsworks.common.provenance.state.ProvStateController;
import io.hops.hopsworks.common.provenance.state.dto.ProvStateDTO;
import io.hops.hopsworks.common.provenance.util.ProvHelper;
import io.hops.hopsworks.common.provenance.util.functional.CheckedFunction;
import io.hops.hopsworks.common.provenance.util.functional.CheckedSupplier;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.collections.CollectionUtils;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.MultiSearchRequest;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.sort.SortOrder;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import java.util.Collections;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@EnterpriseStereotype
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProvOpsControllerEEImpl implements ProvOpsControllerIface {
  private final static Logger LOGGER = Logger.getLogger(ProvOpsControllerEEImpl.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private OpenSearchClientController client;
  @EJB
  private ProvAppController appCtrl;
  @EJB
  private ProvStateController stateCtrl;
  @EJB
  private ProvOpsOpenSearchAggregations provOpsOpenSearchAggregations;

  // For testing
  protected ProvOpsControllerEEImpl(Settings settings, OpenSearchClientController client, 
                                    ProvStateController stateCtrl) {
    this.settings = settings;
    this.client = client;
    this.stateCtrl = stateCtrl;
  }

  public ProvOpsControllerEEImpl() {}

  public ProvOpsDTO provFileOpsList(Project project, ProvOpsParamBuilder params)
    throws ProvenanceException {
    if (!params.getAggregations().isEmpty()) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.UNSUPPORTED, Level.INFO,
        "aggregations currently only allowed with count");
    }
    if (params.getPagination() == null
      || params.getPagination().getValue0() == null || params.getPagination().getValue1() == null) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.UNSUPPORTED, Level.INFO,
        "all searches should be paginated");
    }

    ProvOpsDTO fileOps = provFileOpsBase(project.getInode().getId(),
      params.getFileOpsFilterBy(), params.getFileOpsSortBy(),
      params.getPagination().getValue0(), params.getPagination().getValue1());

    if (params.hasAppExpansion()) {
      //If withAppStates, update params based on appIds of items files and do a appState index query.
      //After this filter the fileStates based on the results of the appState query
      for (ProvOpsDTO fileOp : fileOps.getItems()) {
        Optional<String> appId = getAppId(fileOp);
        if (appId.isPresent()) {
          params.withAppExpansion(appId.get());
        }
      }
      Map<String, Map<Provenance.AppState, ProvAppStateOpenSearch>> appExps
        = appCtrl.provAppState(params.getAppStateFilter());
      Iterator<ProvOpsDTO> fileOpIt = fileOps.getItems().iterator();
      while (fileOpIt.hasNext()) {
        ProvOpsDTO fileOp = fileOpIt.next();
        Optional<String> appId = getAppId(fileOp);
        if (appId.isPresent() && appExps.containsKey(appId.get())) {
          Map<Provenance.AppState, ProvAppStateOpenSearch> appExp = appExps.get(appId.get());
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
    if (!params.getAggregations().isEmpty()) {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.UNSUPPORTED, Level.INFO,
        "misuse of COUNT together with AGGREGATIONS - use either COUNT or AGGREGATIONS");
    }
    return provFileOpsCount(project.getInode().getId(), params.getFileOpsFilterBy());
  }

  public ProvOpsDTO provFileOpsCount(Long projectIId, Map<ProvParser.Field, ProvParser.FilterVal> fileOpsFilters)
    throws ProvenanceException {

    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      OpenSearchHelper.countSearchRequest(
        settings.getProvFileIndex(projectIId))
        .andThen(filterByOpsParams(fileOpsFilters));
    SearchRequest request = srF.get();

    try {
      ProvOpsDTO container = new ProvOpsDTO();
      container.setCount(client.searchCount(request));
      return container;
    } catch (OpenSearchException e) {
      String msg = "provenance - opensearch query problem";
      throw ProvHelper.fromOpenSearch(e, msg, msg + " - file ops");
    }
  }

  public ProvOpsDTO provFileOpsAggs(Project project, ProvOpsParamBuilder params)
      throws ProvenanceException, GenericException {
    if(params.getAggregations().size() > 1) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_ARGUMENT, Level.FINE,
        "currently multiple aggregations in one request are not supported");
    }
    Map<ProvOpsAggregations, List> aggregations = provFileOpsAggs(project.getInode().getId(),
        params.getFileOpsFilterBy(), params.getAggregations());
    List<ProvOpsDTO> aggregationItems = new ArrayList<>();
    for(Map.Entry<ProvOpsAggregations, List> agg : aggregations.entrySet()) {
      ProvOpsDTO aggregation = new ProvOpsDTO();
      aggregation.setAggregation(agg.getKey().toString());
      aggregation.setItems(agg.getValue());
      aggregationItems.add(aggregation);
    }

    ProvOpsDTO allAggregations = new ProvOpsDTO();
    allAggregations.setItems(aggregationItems);
    return allAggregations;
  }

  public Map<ProvOpsAggregations, List> provFileOpsAggs(Long projectIId,
                                                        Map<ProvParser.Field, ProvParser.FilterVal> fileOpsFilters,
                                                        Set<ProvOpsAggregations> aggregations)
    throws ProvenanceException {
    
    Map<ProvOpsAggregations, OpenSearchAggregationParser<?, ProvenanceException>> aggParsers = new HashMap<>();
    List<AggregationBuilder> aggBuilders = new ArrayList<>();
    for (ProvOpsAggregations aggregation : aggregations) {
      aggParsers.put(aggregation, provOpsOpenSearchAggregations.getAggregationParser(aggregation));
      aggBuilders.add(provOpsOpenSearchAggregations.getAggregationBuilder(aggregation));
    }

    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      OpenSearchHelper.baseSearchRequest(
        settings.getProvFileIndex(projectIId), 0)
        .andThen(filterByOpsParams(fileOpsFilters))
        .andThen(OpenSearchHelper.withAggregations(aggBuilders));
    SearchRequest request = srF.get();

    Map<ProvOpsAggregations, List> aggregationResult;
    try {
      aggregationResult = client.searchAggregations(request, aggParsers);
      return aggregationResult;
    } catch (OpenSearchException e) {
      String msg = "provenance - opensearch query problem";
      throw ProvHelper.fromOpenSearch(e, msg, msg + " - file ops");
    }
  }

  private Optional<String> getAppId(ProvOpsDTO fileOp) {
    if (fileOp.getAppId().equals("none")) {
      return Optional.empty();
    } else {
      return Optional.of(fileOp.getAppId());
    }
  }

  public ProvOpsDTO provFileOpsBase(Long projectIId, Map<ProvParser.Field, ProvParser.FilterVal> fileOpsFilters,
                                    List<Pair<ProvParser.Field, SortOrder>> fileOpsSortBy, Integer offset,
                                    Integer limit)
    throws ProvenanceException {
    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      OpenSearchHelper.baseSearchRequest(settings.getProvFileIndex(projectIId),
        settings.getOpenSearchDefaultScrollPageSize())
        .andThen(filterByOpsParams(fileOpsFilters))
        .andThen(OpenSearchHelper.withFileOpsOrder(fileOpsSortBy))
        .andThen(OpenSearchHelper.withPagination(offset, limit, settings.getOpenSearchMaxScrollPageSize()));
    SearchRequest request = srF.get();
    Pair<Long, Try<List<ProvOpsDTO>>> searchResult;
    try {
      searchResult = client.search(request, ProvOpsHandlerFactory.getHandler());
    } catch (OpenSearchException e) {
      String msg = "provenance - opensearch query problem";
      throw ProvHelper.fromOpenSearch(e, msg, msg + " - file ops");
    }
    return ProvOpsHandlerFactory.checkedResult(searchResult);
  }

  /**
   * @param project
   * @param params
   *   - no clone defined yet, so the method might modify the params
   * @return
   * @throws ProvenanceException
   */
  public ProvLinksDTO provLinks(Project project, ProvLinksParamBuilder params, boolean filterAlive)
    throws ProvenanceException {
    HandlerFactory.AppIdMlIdMap linkHandlerFactory = new HandlerFactory.AppIdMlIdMap();
    if (params.isInArtifactDefined() || params.isOutArtifactDefined() || params.isAppIdDefined()) {
      ProvLinksDTO provLinksDTO;
      if (params.isAppIdDefined()) {
        provLinksDTO = deepProvLinks(project, params, filterAlive, linkHandlerFactory, false);
      } else {
        provLinksDTO = deepProvLinks(project, params, filterAlive, linkHandlerFactory, true);
      }
      return convertToInOut(provLinksDTO, params);
    } else if (params.isArtifactDefined()) {
      return deepProvLinks(project, params, filterAlive, linkHandlerFactory, true);
    } else {
      throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
              "query too vague - please define at least one of APP_ID, IN_ARTIFACT, OUT_ARTIFACT",
              "full provenance link query error");
    }
  }

  private ProvLinksDTO convertToInOut(ProvLinksDTO provLinksDTO, ProvLinksParamBuilder params) {
    ProvLinksDTO finalProvLinksDTO = new ProvLinksDTO();
    finalProvLinksDTO.setItems(new ArrayList<>());
    provLinksDTO.getItems().stream().forEach(link -> {
      if (params.isInArtifactDefined()) {
        link.setIn(Collections.singletonMap(link.getRoot().getMlId(), link.getRoot()));
        link.setOut(link.getDownstreamLinks().stream()
                .collect(Collectors.toMap(e -> e.getRoot().getMlId(), e -> e.getRoot())));
        finalProvLinksDTO.addItem(link);
      } else if (params.isOutArtifactDefined() || params.isAppIdDefined()) {
        link.setIn(link.getUpstreamLinks().stream()
                .collect(Collectors.toMap(e -> e.getRoot().getMlId(), e -> e.getRoot())));
        link.setOut(Collections.singletonMap(link.getRoot().getMlId(), link.getRoot()));
        finalProvLinksDTO.addItem(link);
      }
      link.setUpstreamLinks(null);
      link.setDownstreamLinks(null);
      link.setRoot(null);
    });
    Iterator<ProvLinksDTO> it = finalProvLinksDTO.getItems().iterator();
    while (it.hasNext()) {
      ProvLinksDTO appLinks = it.next();
      if (appLinks.getIn().isEmpty() && appLinks.getOut().isEmpty()) {
        it.remove();
      } else if (params.isFullLink() && (appLinks.getIn().isEmpty() || appLinks.getOut().isEmpty())) {
        it.remove();
      }
    }
    return finalProvLinksDTO;
  }

  private Map<String, ProvOpsDTO> updateLive(Map<String, ProvOpsDTO> map, Set<ProvStateDTO> alive){
    if (alive != null) {
      // remove not alive
      Map<String, ProvStateDTO> aliveMap = alive.stream().collect(Collectors.toMap(ProvStateDTO::getMlId, e -> e));
      map.entrySet().removeIf(ee -> !aliveMap.containsKey(ee.getKey()));
      // set feature group type
      map.entrySet().stream().forEach(entry -> {
        ProvStateDTO aliveState = aliveMap.get(entry.getKey());
        if(aliveState.getMlType().equals(Provenance.MLType.FEATURE)) {
          String featurestoreXAttr  = aliveState.getXattrs().getOrDefault("featurestore", "");
          if(featurestoreXAttr != null) {
            FeaturegroupXAttr.FGType fgType = new Gson().fromJson(featurestoreXAttr, FeaturegroupXAttr.FullDTO.class)
                    .getFgType();
            entry.getValue().setFgType(fgType);
          }
        }
      });
    }
    return map;
  }

  enum LinkTypes {
    FEATURESTORE,
    ML,
    OTHER
  }

  private Map<LinkTypes, List<ProvOpsDTO>> splitInLinks(ProvLinksDTO.Builder links) {
    return links.getAppLinks().values().stream().flatMap(appLinks -> appLinks.getIn().values().stream())
            .distinct().collect(Collectors.groupingBy(this::getLinkType));
  }

  private Map<LinkTypes, List<ProvOpsDTO>> splitOutLinks(ProvLinksDTO.Builder links) {
    return links.getAppLinks().values().stream().flatMap(appLinks -> appLinks.getOut().values().stream())
      .distinct().collect(Collectors.groupingBy(this::getLinkType));
  }

  private LinkTypes getLinkType(ProvOpsDTO link) {
    switch (link.getDocSubType()) {
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

  private Set<ProvStateDTO> getLinksAlive(Project project, Map<LinkTypes, List<ProvOpsDTO>> links)
    throws ProvenanceException {
    ProvStateParamBuilder params1 = new ProvStateParamBuilder();
    ProvStateParamBuilder params2 = new ProvStateParamBuilder();
    int mlSize1 = links.getOrDefault(LinkTypes.FEATURESTORE, new LinkedList<>()).size();
    int mlSize2 = links.getOrDefault(LinkTypes.ML, new LinkedList<>()).size();
    ProvStateController.HandlerFactory<ProvStateDTO, Set<ProvStateDTO>,
            Pair<Long, Set<ProvStateDTO>>> stateHandlerFactory
      = new ProvStateController.HandlerFactory.MLIdSet();
    Set<ProvStateDTO> outAlive = new HashSet<>();
    if (mlSize1 > 0) {
      for (ProvOpsDTO link : links.get(LinkTypes.FEATURESTORE)) {
        params1.filterByField(ProvStateParser.FieldsP.ML_ID, link.getMlId());
      }
      params1.paginate(0, mlSize1);
      String index = Settings.FEATURESTORE_INDEX;
      outAlive.addAll(stateCtrl.provFileState(project, params1.base, stateHandlerFactory, index).getValue1());
    }
    if (mlSize2 > 0) {
      for (ProvOpsDTO link : links.get(LinkTypes.ML)) {
        params2.filterByField(ProvStateParser.FieldsP.ML_ID, link.getMlId());
      }
      params2.paginate(0, mlSize2);
      String index = Provenance.getProjectIndex(project);
      outAlive.addAll(stateCtrl.provFileState(project, params2.base, stateHandlerFactory, index).getValue1());
    }
    return outAlive;
  }

  private Map getFilter(ProvLinks.FieldsPF field, String id, boolean onlyApps) throws ProvenanceException {
    Map filters = new HashMap<>();
    ProvParser.addToFilters(filters, field, id);
    if(onlyApps){
      ProvParser.addToFilters(filters, ProvLinks.FieldsPF.ONLY_APPS, "none");
    }
    return filters;
  }

  private Map getFilterWithType(ProvLinks.FieldsPF field, Pair<String, List<String>> ml, boolean onlyApps)
          throws ProvenanceException {
    Map filters = getFilter(field, ml.getValue0(), onlyApps);
    for (String type: ml.getValue1()) {
      ProvParser.addToFilters(filters, ProvLinks.FieldsPF.ARTIFACT_TYPE, type);
    }
    return filters;
  }

  private SearchRequest getProvLinksRequest(Long projectIId, Map<ProvParser.Field, ProvParser.FilterVal> filterByFields)
          throws ProvenanceException {
    CheckedSupplier<SearchRequest, ProvenanceException> srF =
      OpenSearchHelper
        .scrollingSearchRequest(settings.getProvFileIndex(projectIId), settings.getOpenSearchDefaultScrollPageSize())
        .andThen(filterByOpsParams(filterByFields))
        .andThen(OpenSearchHelper.withPagination(null, null, settings.getOpenSearchMaxScrollPageSize()));
    return srF.get();
  }

  private List<Map<ProvParser.Field, ProvParser.FilterVal>> generateFilter(Set<Pair<String, List<String>>> mlIdList,
      boolean onlyApps, ProvLinks.FieldsPF field)
          throws ProvenanceException {
    List<Map<ProvParser.Field, ProvParser.FilterVal>> filterList = new ArrayList();
    for (Pair<String, List<String>> ml: mlIdList) {
      Map filters = getFilterWithType(field, ml, onlyApps);
      filterList.add(filters);
    }
    return filterList;
  }

  enum StreamDirection {
    Upstream,
    Downstream
  }

  private List<Map<ProvParser.Field, ProvParser.FilterVal>> generateAppIdFilterFromMlIdFilter(Project project,
      HandlerFactory<Map<String, AppState>, ProvLinksDTO.Builder> handlerFactory,
      List<Map<ProvParser.Field, ProvParser.FilterVal>> mlIdFilterList, Map<String, ProvLinksDTO> map,
      boolean filterAlive, StreamDirection direction)
          throws ProvenanceException {
    List<Map<ProvParser.Field, ProvParser.FilterVal>> filterList = new ArrayList();
    for (ProvLinksDTO.Builder builder: multipleProvLinks(project.getInode().getId(), mlIdFilterList, handlerFactory)) {

      boolean found = false;
      for (Map.Entry<String, ProvLinksDTO> app: builder.getAppLinks().entrySet()) {
        if ((!app.getValue().getIn().isEmpty() && direction == StreamDirection.Downstream) ||
            (!app.getValue().getOut().isEmpty() && direction == StreamDirection.Upstream)) {
          Map filters = getFilter(ProvLinks.FieldsPF.APP_ID, app.getKey(), false);
          filterList.add(filters);
          found = true;
        }
      }

      //todo remove region if on-demand fg is created with appId present
      //region on-demand fg
      if (!found && !builder.getAppLinks().isEmpty() && direction == StreamDirection.Upstream) {
        // when upstream and all outputs are empty
        Map.Entry<String, ProvLinksDTO> entry = builder.getAppLinks().entrySet().iterator().next();
        ProvLinksDTO provLinks = entry.getValue();

        Set<ProvStateDTO> alive = null;
        if (filterAlive) {
          alive = getLinksAlive(project, splitInLinks(builder));
        }
        Map<String, ProvOpsDTO> in = updateLive(provLinks.getIn(), alive);// remove dead

        ProvLinksDTO provLinksDTO = new ProvLinksDTO();
        provLinksDTO.setAppId("none");
        provLinksDTO.setOut(in);

        map.put(in.entrySet().stream().findFirst().get().getValue().getMlId(), provLinksDTO);
      }
      //endregion
    }
    return filterList;
  }

  private Set<Pair<String, List<String>>> generateMlIdFromAppIdFilter(Project project,
       HandlerFactory<Map<String, AppState>, ProvLinksDTO.Builder> handlerFactory,
       List<Map<ProvParser.Field, ProvParser.FilterVal>> appIdFilterList, Map<String, ProvLinksDTO> map,
       boolean filterAlive, StreamDirection direction)
          throws ProvenanceException {
    Set<Pair<String, List<String>>> mlIdList = new HashSet<>();
    if (appIdFilterList.size() > 0) {
      for (ProvLinksDTO.Builder builder:
              multipleProvLinks(project.getInode().getId(), appIdFilterList, handlerFactory)) {
        Set<ProvStateDTO> alive = null;
        if (filterAlive) {
          alive = getLinksAlive(project, splitOutLinks(builder));
        }
        for (Map.Entry<String, ProvLinksDTO> app: builder.getAppLinks().entrySet()) {
          if (!map.containsKey(app.getKey())) { // do not query what has already been seen
            if (direction == StreamDirection.Upstream) {
              for (Map.Entry<String, ProvOpsDTO> ops : app.getValue().getIn().entrySet()) {
                mlIdList.add(new Pair(ops.getKey(),
                  ProvLinks.FieldsPF.ARTIFACT_TYPE.filterValParser().apply(ops.getValue().getDocSubType().toString())));
              }
            }
            Map<String, ProvOpsDTO> out = updateLive(app.getValue().getOut(), alive);// remove dead
            for (Map.Entry<String, ProvOpsDTO> ops : out.entrySet()) {
              //done to ensure ProvLinksDTO are not ref to same object
              ProvLinksDTO provLinksDTO = new ProvLinksDTO();
              provLinksDTO.setAppId(app.getValue().getAppId());
              provLinksDTO.setIn(app.getValue().getIn());
              provLinksDTO.setOut(app.getValue().getOut());
              map.put(ops.getKey(), provLinksDTO);
              if (direction == StreamDirection.Downstream) {
                mlIdList.add(new Pair(ops.getKey(),
                  ProvLinks.FieldsPF.ARTIFACT_TYPE.filterValParser().apply(ops.getValue().getDocSubType().toString())));
              }
            }
          }
        }
      }
    }
    return mlIdList;
  }

  private Pair<Integer, Set<Pair<String, List<String>>>> deepSearch(Project project, Set<Pair<String, List<String>>>
      idList, HandlerFactory<Map<String, AppState>, ProvLinksDTO.Builder> handlerFactory,
      Map<String, ProvLinksDTO> map, boolean startWithMlId, boolean onlyApps, boolean filterAlive,
      StreamDirection direction, int allowedProvenanceGraphSize)
          throws ProvenanceException {
    int oldMapSize = map.size();
    List<Map<ProvParser.Field, ProvParser.FilterVal>> appIdFilterList;
    if (startWithMlId) {
      // create filters for mlId
      List<Map<ProvParser.Field, ProvParser.FilterVal>> mlIdFilterList =
              generateFilter(idList, onlyApps, ProvLinks.FieldsPF.ARTIFACT);

      // get all links where the provided mlId is the output
      appIdFilterList = generateAppIdFilterFromMlIdFilter(project, handlerFactory, mlIdFilterList, map, filterAlive,
              direction);

      //todo remove region if on-demand fg is created with appId present
      //region on-demand fg
      // reduce the allowed prov graph size by the difference in map size
      int difference = map.size() - oldMapSize;
      allowedProvenanceGraphSize -= difference;
      oldMapSize = map.size();
      //endregion
    }
    else {
      // create filters for mlId
      appIdFilterList = generateFilter(idList, onlyApps, ProvLinks.FieldsPF.APP_ID);
    }

    // trim appIdFilterList if more nodes received than allowed
    if (appIdFilterList.size() > allowedProvenanceGraphSize) {
      appIdFilterList = appIdFilterList.subList(0, allowedProvenanceGraphSize);
    }

    //generate the new mlIdList
    Set<Pair<String, List<String>>> mlIdList = generateMlIdFromAppIdFilter(project, handlerFactory, appIdFilterList,
            map, filterAlive, direction);

    // reduce the allowed prov graph size by the difference in map size
    int difference = map.size() - oldMapSize;
    allowedProvenanceGraphSize -= difference;

    return new Pair(allowedProvenanceGraphSize, mlIdList);
  }

  private void compileDeepLinks(Map<String, ProvLinksDTO> map, StreamDirection direction){
    while(map.entrySet().stream().anyMatch(app ->
            map.keySet().stream().anyMatch(app.getValue().getIn().keySet()::contains))) {

      Map<String, ProvLinksDTO> leafMap = map.entrySet().stream().filter(leaf ->
          (direction == StreamDirection.Upstream &&
            !map.keySet().stream().anyMatch(leaf.getValue().getIn().keySet()::contains)) ||
          (direction == StreamDirection.Downstream &&
            !map.entrySet().stream().anyMatch(entry ->
              CollectionUtils.intersection(entry.getValue().getIn().keySet(), leaf.getValue().getOut().keySet())
                .size() > 0)))
        .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));

      for (Map.Entry<String, ProvLinksDTO> leaf: leafMap.entrySet()) {
        ProvLinksDTO link = leaf.getValue();
        for (Map.Entry<String, ProvOpsDTO> outEntry: link.getOut().entrySet()) {
          ProvLinksDTO provLinksDTO = new ProvLinksDTO();
          provLinksDTO.setAppId(link.getAppId());
          provLinksDTO.setRoot(outEntry.getValue());
          provLinksDTO.setUpstreamLinks(link.getUpstreamLinks());
          provLinksDTO.setDownstreamLinks(link.getDownstreamLinks());
          map.entrySet().stream().filter(entry ->
              (direction == StreamDirection.Upstream && entry.getValue().getIn().containsKey(outEntry.getKey())) ||
              (direction == StreamDirection.Downstream && link.getIn().containsKey(entry.getKey())))
            .forEach(entry -> {
              if (direction == StreamDirection.Upstream) {
                entry.getValue().addUpstreamLink(provLinksDTO);
              } else if (direction == StreamDirection.Downstream) {
                entry.getValue().addDownstreamLink(provLinksDTO);
              }
            });
        }
        link.getIn().clear();
        link.getOut().clear();
      }
      map.keySet().removeAll(leafMap.keySet());
    }
  }

  private Pair<Integer, Map<String, ProvLinksDTO>> deepUpstreamLinks(Project project, Set<Pair<String,
      List<String>>> idList, boolean filterAlive, HandlerFactory<Map<String, AppState>,
      ProvLinksDTO.Builder> handlerFactory, ProvLinksParamBuilder params, boolean startWithMlId,
      int allowedProvenanceGraphSize)
          throws ProvenanceException {
    // final Map containing all results
    Map<String, ProvLinksDTO> map = new HashMap<>();

    int depth = params.getExpand().getValue0();
    StreamDirection direction = StreamDirection.Upstream;

    do {
      Pair<Integer, Set<Pair<String, List<String>>>> result = deepSearch(project, idList, handlerFactory, map,
              startWithMlId, params.isOnlyApps(), filterAlive, direction, allowedProvenanceGraphSize);
      allowedProvenanceGraphSize = result.getValue0();
      idList = result.getValue1();

      startWithMlId = true;

      // reduce depth
      depth -= 1;
    } while (depth + 1 != 0 && idList.size() > 0 && allowedProvenanceGraphSize > 0);

    //compile the final map
    compileDeepLinks(map, direction);

    return new Pair(allowedProvenanceGraphSize, map);
  }

  private Pair<Integer, List<ProvLinksDTO>> deepDownstreamLinks(Project project, Set<Pair<String, List<String>>> idList,
      boolean filterAlive, HandlerFactory<Map<String, AppState>, ProvLinksDTO.Builder> handlerFactory,
      ProvLinksParamBuilder params, Map<String, ProvLinksDTO> map, boolean startWithMlId,
      int allowedProvenanceGraphSize)
          throws ProvenanceException {
    int depth = params.getExpand().getValue1();
    StreamDirection direction = StreamDirection.Downstream;

    while (depth != 0 && idList.size() > 0 && allowedProvenanceGraphSize > 0) {
      Pair<Integer, Set<Pair<String, List<String>>>> result = deepSearch(project, idList, handlerFactory, map,
              startWithMlId, params.isOnlyApps(), filterAlive, direction, allowedProvenanceGraphSize);
      allowedProvenanceGraphSize = result.getValue0();
      idList = result.getValue1();

      startWithMlId = true;

      // reduce depth
      depth -= 1;
    }

    //compile the final map
    compileDeepLinks(map, direction);

    //create provLinksDTOList
    List<ProvLinksDTO> provLinksDTOList = new ArrayList<>();
    for (Map.Entry<String, ProvLinksDTO> entry: map.entrySet()) {
      for (Map.Entry<String, ProvOpsDTO> e: entry.getValue().getOut().entrySet()) {
        ProvLinksDTO provLinksDTO = new ProvLinksDTO();
        provLinksDTO.setAppId(entry.getValue().getAppId());
        provLinksDTO.setRoot(e.getValue());
        provLinksDTO.setUpstreamLinks(entry.getValue().getUpstreamLinks());
        provLinksDTO.setDownstreamLinks(entry.getValue().getDownstreamLinks());
        provLinksDTOList.add(provLinksDTO);
      }
      //clear in case any Ops referenced by other Links
      entry.getValue().getOut().clear();
    }

    return new Pair(allowedProvenanceGraphSize, provLinksDTOList);
  }

  private ProvLinksDTO deepProvLinks(Project project, ProvLinksParamBuilder params, boolean filterAlive,
      HandlerFactory<Map<String, AppState>, ProvLinksDTO.Builder> handlerFactory, boolean startWithMlId)
          throws ProvenanceException {
    ProvLinksDTO provLinksDTO = new ProvLinksDTO();

    Set<Pair<String, List<String>>> filterSetPair = new HashSet<>();
    for (String filter: params.getFilterBy()) {
      filterSetPair.add(new Pair(filter, params.getFilterType()));
    }

    Pair<Integer, Map<String, ProvLinksDTO>> upstreamResult = deepUpstreamLinks(project, filterSetPair, filterAlive,
            handlerFactory, params, startWithMlId, settings.getProvenanceGraphMaxSize());
    Pair<Integer, List<ProvLinksDTO>> downstreamResult = deepDownstreamLinks(project, filterSetPair, filterAlive,
            handlerFactory, params, upstreamResult.getValue1(), startWithMlId, upstreamResult.getValue0());

    provLinksDTO.addItems(downstreamResult.getValue1());
    if (downstreamResult.getValue0() == 0) {
      provLinksDTO.setMaxProvenanceGraphSizeReached(true);
    }
    return provLinksDTO;
  }

  private <O1, O2> List<O2> multipleProvLinks(Long projectIId,
                                              List<Map<ProvParser.Field, ProvParser.FilterVal>> filterByFieldsList,
                                              HandlerFactory<O1, O2> handlerFactory)
          throws ProvenanceException {
    MultiSearchRequest multiSearchRequest = new MultiSearchRequest();
    for (Map<ProvParser.Field, ProvParser.FilterVal> filterByFields: filterByFieldsList) {
      SearchRequest request = getProvLinksRequest(projectIId, filterByFields);
      multiSearchRequest.add(request);
    }
    List<Pair<Long, Try<O1>>> searchResult;
    try {
      searchResult = client.multiSearchScrolling(multiSearchRequest, handlerFactory);
    } catch (OpenSearchException e) {
      String msg = "provenance - opensearch multi query problem";
      throw ProvHelper.fromOpenSearch(e, msg, msg + " - file ops");
    }
    List<O2> result = new ArrayList<>();
    for (Pair<Long, Try<O1>> pair: searchResult) {
      result.add(handlerFactory.checkedResult(pair.getValue1()));
    }
    return result;
  }

  private CheckedFunction<SearchRequest, SearchRequest, ProvenanceException> filterByOpsParams(
      Map<ProvParser.Field, ProvParser.FilterVal> fileOpsFilters) {
    return (SearchRequest sr) -> {
      BoolQueryBuilder query = QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery(ProvParser.Fields.ENTRY_TYPE.toString().toLowerCase(),
          ProvParser.EntryType.OPERATION.toString().toLowerCase()));
      OpenSearchHelper.filterByBasicFields(query, fileOpsFilters);
      sr.source().query(query);
      return sr;
    };
  }

  static class ProvOpsHandlerFactory {
    static OpenSearchHits.Handler<ProvOpsDTO, List<ProvOpsDTO>> getHandler() {
      OpenSearchHits.Parser<ProvOpsDTO> parser
        = hit -> ProvOpsParser.tryInstance(BasicOpenSearchHit.instance(hit));
      return OpenSearchHits.handlerAddToList(parser);
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
   * We assume immutable artifacts, otherwise query is too complex for opensearch.
   * This means that if we detect one CREATE operation, this means the whole artifact is being created
   * and if we only detect READ operations, the artifact is being used.
   */
  static final OpenSearchHits.Merger<ProvOpsDTO, Map<String, AppState>> provLinksMerger
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

  interface HandlerFactory<O1, O2> extends OpenSearchClientController.GenericHandlerFactory<O1, O2, ProvOpsDTO> {
    OpenSearchHits.Handler<ProvOpsDTO, O1> getHandler();

    @Override
    O2 checkedResult(Try<O1> result) throws ProvenanceException;

    class AppIdMlIdMap implements HandlerFactory<Map<String, AppState>, ProvLinksDTO.Builder> {
      public OpenSearchHits.Handler<ProvOpsDTO, Map<String, AppState>> getHandler() {
        OpenSearchHits.Parser<ProvOpsDTO> parser
          = hit -> ProvOpsParser.tryMLInstance(BasicOpenSearchHit.instance(hit));
        Map<String, AppState> initState = new HashMap<>();
        return OpenSearchHits.handlerBasic(parser, initState, provLinksMerger);
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
