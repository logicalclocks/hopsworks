/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with this program.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.featurestore.trainingdataset;

import io.hops.hopsworks.api.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.Feature;
import io.hops.hopsworks.common.featurestore.query.PreparedStatementParameterDTO;
import io.hops.hopsworks.common.featurestore.query.Query;
import io.hops.hopsworks.common.featurestore.query.ServingPreparedStatementDTO;
import io.hops.hopsworks.common.featurestore.query.filter.Filter;
import io.hops.hopsworks.common.featurestore.query.filter.FilterController;
import io.hops.hopsworks.common.featurestore.query.filter.FilterLogic;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.SqlCondition;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetJoin;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class PreparedStatementBuilder {

  // this is used to overwrite feature type in prepared statement
  private final static String PREPARED_STATEMENT_TYPE =  "parameter";
  private final static String ALIAS = "fg0";

  @EJB
  private TrainingDatasetController trainingDatasetController;
  @EJB
  private FeatureViewController featureViewController;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @EJB
  private ConstructorController constructorController;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FilterController filterController;

  private URI uri(UriInfo uriInfo, Project project, Featurestore featurestore, TrainingDataset trainingDataset) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featurestore.getId()))
        .path(ResourceRequest.Name.TRAININGDATASETS.toString().toLowerCase())
        .path(Integer.toString(trainingDataset.getId()))
        .path(ResourceRequest.Name.PREPAREDSTATEMENTS.toString().toLowerCase()).build();
  }

  private URI uri(UriInfo uriInfo, Project project, Featurestore featurestore, FeatureView featureView) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featurestore.getId()))
        .path(ResourceRequest.Name.FEATUREVIEW.toString().toLowerCase())
        .path(featureView.getName())
        .path(ResourceRequest.Name.VERSION.toString().toLowerCase())
        .path(Integer.toString(featureView.getVersion()))
        .path(ResourceRequest.Name.PREPAREDSTATEMENTS.toString().toLowerCase()).build();
  }

  private boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.PREPAREDSTATEMENTS);
  }

  public ServingPreparedStatementDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                                           Users user, Featurestore featurestore, Integer trainingDatasetId,
                                           boolean batch) throws FeaturestoreException {
    TrainingDataset trainingDataset = trainingDatasetController.getTrainingDatasetById(featurestore, trainingDatasetId);

    List<ServingPreparedStatementDTO> servingPreparedStatementDTOs =
        getServingStatements(trainingDataset, project, user, batch);

    ServingPreparedStatementDTO servingPreparedStatementDTO = new ServingPreparedStatementDTO();
    servingPreparedStatementDTO.setHref(uri(uriInfo, project, featurestore, trainingDataset));
    servingPreparedStatementDTO.setExpand(expand(resourceRequest));
    if (servingPreparedStatementDTO.isExpand()) {
      servingPreparedStatementDTO.setItems(servingPreparedStatementDTOs);
      servingPreparedStatementDTO.setCount((long) servingPreparedStatementDTOs.size());
    }
    return servingPreparedStatementDTO;
  }

  private List<ServingPreparedStatementDTO> getServingStatements(TrainingDataset trainingDataset, Project project,
                                                                 Users user, boolean batch)
      throws FeaturestoreException {
    if (!trainingDataset.isQuery()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.TRAINING_DATASET_NO_QUERY,
          Level.FINE, "Inference vector is only available for datasets generated by queries");
    }

    List<TrainingDatasetJoin> joins = trainingDatasetController.getJoinsSorted(trainingDataset);
    // Check that all the feature groups still exists, if not throw a reasonable error
    if (trainingDataset.getFeatures().stream().anyMatch(j -> j.getFeatureGroup() == null)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.QUERY_FAILED_FG_DELETED, Level.FINE);
    }

    List<ServingPreparedStatementDTO> servingPreparedStatementDTOS =
        createServingPreparedStatementDTOS(joins, project, user, batch);

    return servingPreparedStatementDTOS;
  }

  public ServingPreparedStatementDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                                           Users user, Featurestore featurestore, FeatureView featureView,
                                           boolean batch) throws FeaturestoreException {
    List<ServingPreparedStatementDTO> servingPreparedStatementDTOs =
        getServingStatements(featureView, project, user, batch);

    ServingPreparedStatementDTO servingPreparedStatementDTO = new ServingPreparedStatementDTO();
    servingPreparedStatementDTO.setHref(uri(uriInfo, project, featurestore, featureView));
    servingPreparedStatementDTO.setExpand(expand(resourceRequest));
    if (servingPreparedStatementDTO.isExpand()) {
      servingPreparedStatementDTO.setItems(servingPreparedStatementDTOs);
      servingPreparedStatementDTO.setCount((long) servingPreparedStatementDTOs.size());
    }
    return servingPreparedStatementDTO;
  }

  private List<ServingPreparedStatementDTO> getServingStatements(FeatureView featureView, Project project,
                                                                 Users user, boolean batch)
      throws FeaturestoreException {
    List<TrainingDatasetJoin> joins = trainingDatasetController.getJoinsSorted(featureView.getJoins());
    // Check that all the feature groups still exists, if not throw a reasonable error
    if (featureView.getFeatures().stream().anyMatch(j -> j.getFeatureGroup() == null)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.QUERY_FAILED_FG_DELETED, Level.FINE);
    }

    List<ServingPreparedStatementDTO> servingPreparedStatementDTOS =
        createServingPreparedStatementDTOS(joins, project, user, batch);

    return servingPreparedStatementDTOS;
  }

  private List<ServingPreparedStatementDTO> createServingPreparedStatementDTOS(
      Collection<TrainingDatasetJoin> joins, Project project, Users user, boolean batch)
      throws FeaturestoreException {
    List<ServingPreparedStatementDTO> servingPreparedStatementDTOS = new ArrayList<>();

    // each join is a feature group, iterate over them.
    for (TrainingDatasetJoin join : joins) {
      Featuregroup featuregroup = join.getFeatureGroup();

      if ((featuregroup.getStreamFeatureGroup() != null && !featuregroup.getStreamFeatureGroup().isOnlineEnabled())
        || (featuregroup.getCachedFeaturegroup() != null && !featuregroup.getCachedFeaturegroup().isOnlineEnabled())){
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.FEATURESTORE_ONLINE_NOT_ENABLED,
          Level.FINE, "Inference vector is only available for training datasets generated by online enabled " +
          "feature groups. Feature group `" + featuregroup.getName() + "` is not online enabled.");
      }

      Map<String, Feature> featureGroupFeatures =
        featuregroupController.getFeatures(featuregroup, project, user).stream()
          .collect(Collectors.toMap(FeatureGroupFeatureDTO::getName,
            f -> new Feature(f.getName(), ALIAS, f.getType(), f.getPrimary(), f.getDefaultValue(), join.getPrefix(),
                join.getFeatureGroup())
          ));
      
      // Identify and create primary key features for this feature group. Primary key features may not be the part of
      //  query that generated the training dataset.
      List<Feature> primaryKeys = featureGroupFeatures.values().stream()
          .filter(Feature::isPrimary)
          .collect(Collectors.toList());
      if (primaryKeys.size() == 0 ) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.PRIMARY_KEY_REQUIRED,
            Level.FINE, "Inference vector is only available for training datasets generated by feature groups with " +
            "at least 1 primary key");
      }

      // create td features
      List<Feature> selectFeatures = join.getFeatures().stream()
          .filter(tdf -> !tdf.isLabel())
          .sorted(Comparator.comparing(TrainingDatasetFeature::getIndex))
          .map(tdf -> featureGroupFeatures.get(tdf.getName()))
          .collect(Collectors.toList());

      if (batch) {
        // to be able to sort the batch correctly and align feature vectors from different feature groups
        // the hsfs library needs to know which row refers to which entity. We do that by adding the
        // primary keys in the select statement
        selectFeatures.addAll(primaryKeys.stream()
            .filter(primaryKey -> !selectFeatures.contains(primaryKey))
            .collect(Collectors.toList()));
      }

      // In some cases only label(s) are used from a feature group. In this case they will not be
      // part of the prepared statement thus don't add to this query.
      if (selectFeatures.size() > 0){
        // construct query for this feature group
        Query query = new Query(
            featurestoreController.getOfflineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()),
            onlineFeaturestoreController.getOnlineFeaturestoreDbName(featuregroup.getFeaturestore().getProject()),
            featuregroup,
            ALIAS,
            selectFeatures
        );
        // construct ServingPreparedStatementDTO and add to the list
        servingPreparedStatementDTOS.add(
            buildDTO(query, primaryKeys, featuregroup.getId(), join.getIndex(), batch, join.getPrefix()));
      }
    }

    return servingPreparedStatementDTOS;
  }

  private ServingPreparedStatementDTO buildDTO(Query query, List<Feature> primaryKeys, Integer featureGroupId,
                                               Integer statementIndex, boolean batch, String prefix)
      throws FeaturestoreException {
    // create primary key prepared statement filters for the query
    List<PreparedStatementParameterDTO> stmtParameters = new ArrayList<>();

    // Change the type of PK to PREPARED_STATEMENT_TYPE. This will avoid having the query constructor
    // adding additional quotes around the ? sign
    primaryKeys.forEach(f -> f.setType(PREPARED_STATEMENT_TYPE));

    // record pk position in the prepared statement - start from 1 as that's how
    // prepared statements work.
    int primaryKeyIndex = 1;

    // First condition doesn't have any "AND"
    // we are guaranteed there is at least one primary key, as no primary key situations are filtered above
    Feature pkFeature = primaryKeys.get(0);

    stmtParameters.add(new PreparedStatementParameterDTO(pkFeature.getName(), primaryKeyIndex++));

    FilterLogic filterLogic;
    if (batch){
      filterLogic = new FilterLogic(new Filter(primaryKeys, SqlCondition.IN, "?"));
    } else {
      filterLogic = new FilterLogic(new Filter(Arrays.asList(pkFeature), SqlCondition.EQUALS, "?"));
    }

    // Concatenate conditions
    for (int i = 1; i < primaryKeys.size(); i++) {
      pkFeature = primaryKeys.get(i);
      if (!batch) {
        filterLogic = filterLogic.and(new Filter(Arrays.asList(pkFeature), SqlCondition.EQUALS, "?"));
      }
      stmtParameters.add(
          new PreparedStatementParameterDTO(pkFeature.getName(), primaryKeyIndex++));
    }

    query.setFilter(filterLogic);

    // set prepared statement parameters
    return new ServingPreparedStatementDTO(featureGroupId, statementIndex, stmtParameters,
        constructorController.generateSQL(query, true)
            .toSqlString(new MysqlSqlDialect(SqlDialect.EMPTY_CONTEXT)).getSql(), prefix);
  }
}