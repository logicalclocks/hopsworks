/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.query;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.FeatureGroupCommitController;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.query.filter.Filter;
import io.hops.hopsworks.common.featurestore.query.filter.FilterController;
import io.hops.hopsworks.common.featurestore.query.filter.FilterLogic;
import io.hops.hopsworks.common.featurestore.query.join.Join;
import io.hops.hopsworks.common.featurestore.query.join.JoinDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.FeatureGroupCommit;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.cached.TimeTravelFormat;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlFilterLogic;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFilter;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoin;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.calcite.sql.JoinType;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlFilterLogic.AND;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class QueryController {

  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeaturegroupFacade featuregroupFacade;
  @EJB
  private FeatureGroupCommitController featureGroupCommitController;
  @EJB
  private FilterController filterController;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @EJB
  private TrainingDatasetController trainingDatasetController;
  private final static String ALL_FEATURES = "*";

  public QueryController() {
  }

  public QueryController(FeaturegroupController featuregroupController, FeaturegroupFacade featuregroupFacade,
                         FilterController filterController, FeaturestoreFacade featurestoreFacade,
                         OnlineFeaturestoreController onlineFeaturestoreController,
                         FeatureGroupCommitController featureGroupCommitController) {
    this.featuregroupController = featuregroupController;
    this.featuregroupFacade = featuregroupFacade;
    this.filterController = filterController;
    this.featurestoreFacade = featurestoreFacade;
    this.onlineFeaturestoreController = onlineFeaturestoreController;
    this.featureGroupCommitController = featureGroupCommitController;
  }

  public Query convertQueryDTO(Project project, Users user, QueryDTO queryDTO, boolean pitEnabled)
      throws FeaturestoreException {
    // construct lookup tables once for all involved feature groups
    // all maps have the feature group id as key
    Map<Integer, String> fgAliasLookup = new HashMap<>();
    Map<Integer, Featuregroup> fgLookup = new HashMap<>();
    Map<Integer, List<Feature>> availableFeatureLookup = new HashMap<>();

    populateFgLookupTables(queryDTO, 0, fgAliasLookup, fgLookup, availableFeatureLookup, project, user, null);

    return convertQueryDTO(queryDTO, fgAliasLookup, fgLookup, availableFeatureLookup, pitEnabled);
  }

  public Set<Featuregroup> getFeatureGroups(Query query) {
    // Note that nested join is not allowed.
    Set<Featuregroup> allFgs = query.getJoins().stream().map(join -> join.getRightQuery().getFeaturegroup()).collect(
        Collectors.toSet());
    allFgs.add(query.getFeaturegroup());
    return allFgs;
  }

  /**
   * Recursively convert the QueryDTO into the internal query representation
   *
   * @param queryDTO
   * @return
   */
  public Query convertQueryDTO(QueryDTO queryDTO, Map<Integer, String> fgAliasLookup,
      Map<Integer, Featuregroup> fgLookup, Map<Integer, List<Feature>> availableFeatureLookup,
      boolean pitEnabled)
      throws FeaturestoreException {
    checkNestedJoin(queryDTO);
    Integer fgId = queryDTO.getLeftFeatureGroup().getId();
    Featuregroup fg = fgLookup.get(fgId);

    String featureStore = featurestoreFacade.getHiveDbName(fg.getFeaturestore().getHiveDbId());
    // used to build the online query - needs to respect the online db format name
    String projectName = onlineFeaturestoreController.getOnlineFeaturestoreDbName(fg.getFeaturestore().getProject());

    List<Feature> requestedFeatures = validateFeatures(fg, queryDTO.getLeftFeatures(),
        availableFeatureLookup.get(fgId));

    Query query = new Query(featureStore, projectName, fg, fgAliasLookup.get(fgId), requestedFeatures,
        availableFeatureLookup.get(fgId), queryDTO.getHiveEngine());

    if (fg.getStreamFeatureGroup() != null ||
        (fg.getCachedFeaturegroup() != null &&
        fg.getCachedFeaturegroup().getTimeTravelFormat() == TimeTravelFormat.HUDI)) {

      // if a user specified a point in time in the request use it, otherwise set null and let the HSFS
      // APIs use SNAPSHOT query. the end time field will trigger an INCREMENTAL query. Incremental queries
      // only take into consideration the active commits, i.e. not the commits that have been archived by Hudi.
      // By default HUDI keeps the last 20 commits on the active timeline.
      if (queryDTO.getLeftFeatureGroupEndTime() != null) {
        FeatureGroupCommit endCommit =
            featureGroupCommitController.findCommitByDate(fg, queryDTO.getLeftFeatureGroupEndTime())
                .orElseThrow(() -> new FeaturestoreException(
                    RESTCodes.FeaturestoreErrorCode.NO_DATA_AVAILABLE_FEATUREGROUP_COMMITDATE, Level.FINE,
                    "featureGroup: " + fg.getName() + " version " + fg.getVersion()));

        query.setLeftFeatureGroupEndTimestamp(endCommit.getCommittedOn());
        query.setLeftFeatureGroupEndCommitId(endCommit.getFeatureGroupCommitPK().getCommitId());
      }

      if (queryDTO.getLeftFeatureGroupStartTime() != null) {
        Integer commitCount = featureGroupCommitController.countCommitsInRange(fg,
                                queryDTO.getLeftFeatureGroupStartTime(),
                                queryDTO.getLeftFeatureGroupEndTime());

        if (commitCount == 0) {
          throw new FeaturestoreException(
                  RESTCodes.FeaturestoreErrorCode.NO_DATA_AVAILABLE_FEATUREGROUP_COMMITDATE, Level.FINE,
                  "featureGroup: " + fg.getName() + " version " + fg.getVersion());
        }

        query.setLeftFeatureGroupStartTimestamp(queryDTO.getLeftFeatureGroupStartTime());
      }
    }

    // If there are any joins, recursively convert the Join's QueryDTO into the internal Query representation
    if (queryDTO.getJoins() != null && !queryDTO.getJoins().isEmpty()) {
      query.setJoins(
          convertJoins(query, queryDTO.getJoins(), fgAliasLookup, fgLookup, availableFeatureLookup, pitEnabled));
      // remove duplicated join columns
      removeDuplicateColumns(query, pitEnabled);
    }

    // If there are any filters, recursively convert the
    if (queryDTO.getFilter() != null) {
      query.setFilter(filterController.convertFilterLogic(queryDTO.getFilter(), availableFeatureLookup));
    }

    return query;
  }

  void checkNestedJoin(QueryDTO queryDTO) throws FeaturestoreException {
    if (queryDTO.getJoins() != null) {
      for (JoinDTO join : queryDTO.getJoins()) {
        if (join.getQuery().getJoins() != null && join.getQuery().getJoins().size() > 0) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.NESTED_JOIN_NOT_ALLOWED,
              Level.SEVERE,
              "Nested join is not supported.");
        }
      }
    }
  }

  public Query appendFilter(Query query, SqlFilterLogic sqlLogic, FilterLogic filterLogic) {
    FilterLogic currentFilter = query.getFilter();
    if (currentFilter != null) {
      FilterLogic filter = new FilterLogic(sqlLogic, currentFilter, filterLogic);
      query.setFilter(filter);
    } else {
      query.setFilter(filterLogic);
    }
    return query;
  }

  public Query appendEventTimeFilter(Query query, Date startTime, Date endTime) throws FeaturestoreException {
    query = appendEventTimeFilter(query, startTime, SqlCondition.GREATER_THAN_OR_EQUAL);
    return appendEventTimeFilter(query, endTime, SqlCondition.LESS_THAN);
  }

  private Query appendEventTimeFilter(Query query, Date eventTime, SqlCondition sqlCondition)
      throws FeaturestoreException {
    if (eventTime != null) {
      Filter eventTimeFilter = createEventTimeFilter(getEventTimeFeature(query), sqlCondition, eventTime);
      return appendFilter(query, AND, new FilterLogic(eventTimeFilter));
    }
    return query;
  }

  Feature getEventTimeFeature(Query query) throws FeaturestoreException {
    String eventTimeFieldName = query.getFeaturegroup().getEventTime();
    if (eventTimeFieldName == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.EVENT_TIME_FEATURE_NOT_FOUND, Level.FINE,
          "Cannot find event feature in feature group " + query.getFeaturegroup().getName());
    }
    return query.getAvailableFeatures()
        .stream()
        .filter(feature -> feature.getName().equals(eventTimeFieldName))
        .findFirst()
        .orElseThrow(IllegalStateException::new);
  }

  Filter createEventTimeFilter(Feature feature, SqlCondition condition, Date time) throws FeaturestoreException {
    String value = filterController.convertToEventTimeFeatureValue(feature, time);
    return new Filter(feature, condition, value);
  }

  public int populateFgLookupTables(QueryDTO queryDTO, int fgId, Map<Integer, String> fgAliasLookup,
      Map<Integer, Featuregroup> fgLookup,
      Map<Integer, List<Feature>> availableFeatureLookup, Project project,
      Users user, String prefix)
      throws FeaturestoreException {
    // go into depth first
    if (queryDTO.getJoins() != null && !queryDTO.getJoins().isEmpty()) {
      for (JoinDTO join : queryDTO.getJoins()) {
        fgId = populateFgLookupTables(join.getQuery(), fgId, fgAliasLookup, fgLookup, availableFeatureLookup, project,
            user, join.getPrefix());
        fgId++;
      }
    }

    Featuregroup fg = validateFeaturegroupDTO(queryDTO.getLeftFeatureGroup());
    fgLookup.put(fg.getId(), fg);
    fgAliasLookup.put(fg.getId(), generateAs(fgId));

    List<Feature> availableFeatures = featuregroupController.getFeatures(fg, project, user).stream()
        // Set the type as well, as the same code is used when parsing the query to generate a training dataset
        // in that case we would like to show the type of the feature in the UI.
        // it's easier and faster to return the training dataset schema if we store the type in the
        // training dataset features table.
        .map(f -> new Feature(
            f.getName(), fgAliasLookup.get(fg.getId()), f.getType(), f.getDefaultValue(), f.getPrimary(), fg, prefix))
        .collect(Collectors.toList());
    availableFeatureLookup.put(fg.getId(), availableFeatures);

    return fgId;
  }

  private String generateAs(int id) {
    return "fg" + id;
  }

  /**
   * Validate FeatureGroupDTO to make sure it exists. Authorization is done at the storage layer by HopsFS when
   * actually executing the query.
   *
   * @param featuregroupDTO
   * @return
   */
  private Featuregroup validateFeaturegroupDTO(FeaturegroupDTO featuregroupDTO) throws FeaturestoreException {
    if (featuregroupDTO == null) {
      throw new IllegalArgumentException("Feature group not specified");
    } else {
      return featuregroupFacade.findById(featuregroupDTO.getId())
          .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND,
              Level.FINE, "Could not find feature group with ID " + featuregroupDTO.getId()));
    }
  }

  /**
   * Given the list of features available in the feature group check that all the features the user
   * requests actually exists.
   *
   * Users are allowed to pass a list with a single feature named * to select all the features.
   *
   * @param fg
   * @param requestedFeatures
   * @param availableFeatures
   * @return The list of feature objects that it's going to be used to generate the SQL string.
   */
  protected List<Feature> validateFeatures(Featuregroup fg, List<FeatureGroupFeatureDTO> requestedFeatures,
      List<Feature> availableFeatures)
      throws FeaturestoreException {
    List<Feature> featureList = new ArrayList<>();

    if (requestedFeatures == null || requestedFeatures.isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_DOES_NOT_EXIST,
          Level.FINE, String.format("No feature is selected from feature group %s.", fg.getName()));
    } else if (requestedFeatures.size() == 1 && requestedFeatures.get(0).getName().equals(ALL_FEATURES)) {
      // Users can specify * to request all the features in a specific feature group
      featureList.addAll(availableFeatures);
    } else {
      // Check that all the requested features are available in the list, based on the provided name
      for (FeatureGroupFeatureDTO requestedFeature : requestedFeatures) {
        featureList.add(availableFeatures.stream().filter(af -> af.getName().equals(requestedFeature.getName()))
            .findFirst()
            .orElseThrow(() -> new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_DOES_NOT_EXIST,
                Level.FINE,
                "Feature: " + requestedFeature.getName() + " not found in feature group: " + fg.getName())));
      }
    }
    return featureList;
  }

  /**
   * Convert the JoinDTOs into the internal representation of the Join object.
   * The returned list will already contain the correct set of joining keys
   *
   * @param leftQuery
   * @param joinDTOS
   * @return
   */
  private List<Join> convertJoins(Query leftQuery, List<JoinDTO> joinDTOS, Map<Integer, String> fgAliasLookup,
      Map<Integer, Featuregroup> fgLookup,
      Map<Integer, List<Feature>> availableFeatureLookup,
      boolean pitEnabled)
      throws FeaturestoreException {
    List<Join> joins = new ArrayList<>();
    for (JoinDTO joinDTO : joinDTOS) {
      if (joinDTO.getQuery() == null) {
        throw new IllegalArgumentException("Subquery not specified");
      }
      // Recursively convert the QueryDTO. Currently we don't support Joins of Joins
      Query rightQuery = convertQueryDTO(
          joinDTO.getQuery(), fgAliasLookup, fgLookup, availableFeatureLookup, pitEnabled);

      if (joinDTO.getOn() != null && !joinDTO.getOn().isEmpty()) {
        List<Feature> leftOn = joinDTO.getOn().stream().map(f -> new Feature(f.getName())).collect(Collectors.toList());
        List<Feature> rightOn =
            joinDTO.getOn().stream().map(f -> new Feature(f.getName())).collect(Collectors.toList());

        joins.add(extractLeftRightOn(leftQuery, rightQuery, leftOn, rightOn, joinDTO.getType(), joinDTO.getPrefix()));
      } else if (joinDTO.getLeftOn() != null && !joinDTO.getLeftOn().isEmpty()) {
        List<Feature> leftOn = joinDTO.getLeftOn().stream()
            .map(f -> new Feature(f.getName())).collect(Collectors.toList());
        List<Feature> rightOn = joinDTO.getRightOn().stream()
            .map(f -> new Feature(f.getName())).collect(Collectors.toList());

        joins.add(extractLeftRightOn(leftQuery, rightQuery, leftOn, rightOn, joinDTO.getType(), joinDTO.getPrefix()));
      } else {
        // Only if right feature group is present, extract the primary keys for the join
        joins.add(extractPrimaryKeysJoin(leftQuery, rightQuery, joinDTO.getType(), joinDTO.getPrefix()));
      }
    }

    return joins;
  }

  /**
   * In case the user has not specified any joining key, the largest subset of matching primary key will be used
   * for the join. The name should match for the feature to be added in the subset
   *
   * @param leftQuery
   * @param rightQuery
   * @param joinType
   * @return
   */
  protected Join extractPrimaryKeysJoin(Query leftQuery, Query rightQuery, JoinType joinType, String prefix)
      throws FeaturestoreException {
    // Find subset of matching primary keys (same name) to be used as join condition
    List<Feature> joinFeatures = new ArrayList<>();
    leftQuery.getAvailableFeatures().stream().filter(Feature::isPrimary).forEach(lf -> {
      joinFeatures.addAll(rightQuery.getAvailableFeatures().stream()
          .filter(rf -> rf.getName().equals(lf.getName()) && rf.isPrimary())
          .collect(Collectors.toList()));
    });

    if (joinFeatures.isEmpty()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.NO_PK_JOINING_KEYS, Level.FINE,
          leftQuery.getFeaturegroup().getName() + " and: " + rightQuery.getFeaturegroup().getName());
    }

    // primary key join is always an EQUALS join since user does not specify anything
    List<SqlCondition> joinOperator = joinFeatures.stream().map(f -> SqlCondition.EQUALS).collect(Collectors.toList());

    return new Join(leftQuery, rightQuery, joinFeatures, joinFeatures, joinType, prefix, joinOperator);
  }

  /**
   * If the user has specified the `leftOn` and `rightOn` make sure that both list have the same length, that the leftOn
   * features are present in the left feature group, the rightOn features in the right feature group.
   *
   * @param leftQuery
   * @param rightQuery
   * @param leftOn
   * @param rightOn
   * @param joinType
   * @return
   */
  public Join extractLeftRightOn(Query leftQuery, Query rightQuery, List<Feature> leftOn, List<Feature> rightOn,
      JoinType joinType, String prefix)
      throws FeaturestoreException {
    // Make sure that they 2 list have the same length
    if (leftOn.size() != rightOn.size()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.LEFT_RIGHT_ON_DIFF_SIZES, Level.FINE);
    }

    // note currently we don't support letting the user specify different operators than EQUALS for joins
    // this is here for completeness and to have the possibility later to allow for it
    List<SqlCondition> joinOperator = leftOn.stream().map(f -> SqlCondition.EQUALS).collect(Collectors.toList());

    // make sure that join operator list has same length as on list
    if (joinOperator.size() != leftOn.size()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.JOIN_OPERATOR_MISMATCH, Level.FINE);
    }

    // Check that all the left features exist in the left query
    for (Feature feature : leftOn) {
      checkFeatureExistsAndSetAttributes(leftQuery, feature);
    }

    // Check that all the right features exist in the right query
    for (Feature feature : rightOn) {
      checkFeatureExistsAndSetAttributes(rightQuery, feature);
    }

    return new Join(leftQuery, rightQuery, leftOn, rightOn, joinType, prefix, joinOperator);
  }

  private void checkFeatureExistsAndSetAttributes(Query query, Feature feature)
      throws FeaturestoreException {
    Optional<Feature> availableFeature =
        query.getAvailableFeatures().stream().filter(f -> (f.getName().equals(feature.getName()))).findAny();
    if (!availableFeature.isPresent()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURE_DOES_NOT_EXIST, Level.FINE,
          "Could not find Join feature " + feature.getName() + " in feature group: "
              + query.getFeaturegroup().getName());
    } else {
      feature.setDefaultValue(availableFeature.get().getDefaultValue());
      feature.setType(availableFeature.get().getType());
      feature.setFgAlias(availableFeature.get().getFgAlias());
    }
  }

  /**
   * For Join on primary keys or On condition we should remove duplicated (same name) columns.
   * Spark refuses to write dataframes with duplicated column names.
   *
   * @param query
   */
  void removeDuplicateColumns(Query query, boolean pitEnabled) {
    for (Join join : query.getJoins()) {

      // Extract left join feature names and drop all features on right side with same name
      List<String> leftJoinFeatureNames = join.getLeftOn().stream().map(Feature::getName).collect(Collectors.toList());

      // Remove all features which are on the join condition and are not already present in the left side of the join
      List<Feature> filteredRightFeatures = new ArrayList<>();
      for (Feature rightFeature : join.getRightQuery().getFeatures()) {
        if (leftJoinFeatureNames.contains(rightFeature.getName()) &&
            join.getLeftQuery().getFeatures().stream().anyMatch(lf -> lf.getName().equals(
                Strings.isNullOrEmpty(rightFeature.getPrefix()) ? rightFeature.getName() :
                    rightFeature.getPrefix() + rightFeature.getName())
            )) {
          // The feature is part of the joining condition and it's also part of the features list in the left query
          // no need to pass it here.
          continue;
        }

        filteredRightFeatures.add(rightFeature);
      }

      // replace the features for the right query
      join.getRightQuery().setFeatures(filteredRightFeatures);
    }
  }

  public Query makeQuery(FeatureView featureView, Project project, Users user, boolean withLabel, Boolean isHiveEngine)
      throws FeaturestoreException {
    return makeQuery(featureView, project, user, withLabel, isHiveEngine, Lists.newArrayList());
  }

  public Query makeQuery(FeatureView featureView, Project project, Users user, boolean withLabel, Boolean isHiveEngine,
      Collection<TrainingDatasetFilter> extraFilters)
      throws FeaturestoreException {
    List<TrainingDatasetJoin> joins = featureView.getJoins().stream()
        .sorted(Comparator.comparing(TrainingDatasetJoin::getIndex))
        .collect(Collectors.toList());

    List<TrainingDatasetFeature> tdFeatures = featureView.getFeatures().stream()
        .sorted((t1, t2) -> {
          if (t1.getIndex() != null) {
            // compare based on index
            return t1.getIndex().compareTo(t2.getIndex());
          } else {
            // Old training dataset with no index. compare based on name
            return t1.getName().compareTo(t2.getName());
          }
        })
        // drop label features if desired
        .filter(f -> !f.isLabel() || withLabel)
        .collect(Collectors.toList());

    // remove join if it contains only with label feature and withLabel is false.
    // Otherwise `validateFeatures` will be failed when constructing query string because no feature is contained in
    // that join.
    Set<Featuregroup> labelOnlyFgs = featureView.getFeatures().stream().map(TrainingDatasetFeature::getFeatureGroup)
        .collect(Collectors.toSet());
    labelOnlyFgs.removeAll(tdFeatures.stream().map(TrainingDatasetFeature::getFeatureGroup)
        .collect(Collectors.toSet()));
    joins.removeIf(join -> labelOnlyFgs.contains(join.getFeatureGroup()));
    return trainingDatasetController.getQuery(
        joins, tdFeatures, featureView.getFilters(), project, user, isHiveEngine, extraFilters);
  }

  public Query constructBatchQuery(FeatureView featureView, Project project, Users user, Long startTimestamp,
      Long endTimestamp, Boolean withLabel, Boolean isHiveEngine, Integer trainingDataVersion)
      throws FeaturestoreException {
    Date startTime = startTimestamp == null ? null : new Date(startTimestamp);
    Date endTime = endTimestamp == null ? null : new Date(endTimestamp);
    return constructBatchQuery(featureView, project, user, startTime, endTime, withLabel, isHiveEngine,
        trainingDataVersion);
  }

  public Query constructBatchQuery(FeatureView featureView, Project project, Users user, Date startTime,
      Date endTime, Boolean withLabel, Boolean isHiveEngine, Integer trainingDataVersion)
      throws FeaturestoreException {
    Query baseQuery;
    if (trainingDataVersion != null) {
      baseQuery = makeQuery(featureView, project, user, withLabel, isHiveEngine,
          trainingDatasetController.getTrainingDatasetByFeatureViewAndVersion(featureView, trainingDataVersion)
              .getFilters());
    } else {
      baseQuery = makeQuery(featureView, project, user, withLabel, isHiveEngine);
    }
    return appendEventTimeFilter(baseQuery, startTime, endTime);
  }
}
