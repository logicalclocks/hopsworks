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

package io.hops.hopsworks.common.dao.featurestore.featuregroup.on_demand_featuregroup;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.feature.FeaturestoreFeatureController;
import io.hops.hopsworks.common.dao.featurestore.settings.FeaturestoreClientSettingsDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnectorFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the on_demand_feature_group table and required business logic
 */
@Stateless
public class OnDemandFeaturegroupController {
  @EJB
  private OnDemandFeaturegroupFacade onDemandFeaturegroupFacade;
  @EJB
  private FeaturestoreJdbcConnectorFacade featurestoreJdbcConnectorFacade;
  @EJB
  private FeaturestoreFeatureController featurestoreFeatureController;
  
  /**
   * Persists an on demand feature group
   *
   * @param onDemandFeaturegroupDTO the user input data to use when creating the feature group
   * @return the created entity
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public OnDemandFeaturegroup createOnDemandFeaturegroup(OnDemandFeaturegroupDTO onDemandFeaturegroupDTO)
      throws FeaturestoreException {
    //Verify User Input
    verifyOnDemandFeaturegroupUserInput(onDemandFeaturegroupDTO);
    //Get JDBC Connector
    FeaturestoreJdbcConnector featurestoreJdbcConnector = featurestoreJdbcConnectorFacade.find(
        onDemandFeaturegroupDTO.getJdbcConnectorId());
    if(featurestoreJdbcConnector == null){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ON_DEMAND_FEATUREGROUP_JDBC_CONNECTOR_NOT_FOUND,
          Level.FINE, "jdbConnectorId: " + onDemandFeaturegroupDTO.getJdbcConnectorId());
    }
    //Persist on-demand featuregroup
    OnDemandFeaturegroup onDemandFeaturegroup = new OnDemandFeaturegroup();
    onDemandFeaturegroup.setDescription(onDemandFeaturegroupDTO.getDescription());
    onDemandFeaturegroup.setName(onDemandFeaturegroupDTO.getName());
    onDemandFeaturegroup.setFeaturestoreJdbcConnector(featurestoreJdbcConnector);
    onDemandFeaturegroup.setQuery(onDemandFeaturegroupDTO.getQuery());
    onDemandFeaturegroupFacade.persist(onDemandFeaturegroup);
    
    //Persist feature data
    featurestoreFeatureController.updateOnDemandFeaturegroupFeatures(onDemandFeaturegroup,
      onDemandFeaturegroupDTO.getFeatures());
    
    return onDemandFeaturegroup;
  }
  
  /**
   * Updates metadata of an on demand feature group in the feature store
   *
   * @param onDemandFeaturegroup the on-demand feature group to update
   * @param onDemandFeaturegroupDTO the metadata DTO
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void updateOnDemandFeaturegroupMetadata(OnDemandFeaturegroup onDemandFeaturegroup,
    OnDemandFeaturegroupDTO onDemandFeaturegroupDTO) throws FeaturestoreException {
    
    if(!Strings.isNullOrEmpty(onDemandFeaturegroupDTO.getName())){
      verifyOnDemandFeaturegroupName(onDemandFeaturegroupDTO.getName());
      onDemandFeaturegroup.setName(onDemandFeaturegroupDTO.getName());
    }
    if(!Strings.isNullOrEmpty(onDemandFeaturegroupDTO.getDescription())){
      verifyOnDemandFeaturegroupDescription(onDemandFeaturegroupDTO.getDescription());
      onDemandFeaturegroup.setDescription(onDemandFeaturegroupDTO.getDescription());
    }
    if(onDemandFeaturegroupDTO.getJdbcConnectorId() != null) {
      FeaturestoreJdbcConnector featurestoreJdbcConnector =
        verifyOnDemandFeaturegroupJdbcConnector(onDemandFeaturegroupDTO.getJdbcConnectorId());
      onDemandFeaturegroup.setFeaturestoreJdbcConnector(featurestoreJdbcConnector);
    }
    if(!Strings.isNullOrEmpty(onDemandFeaturegroupDTO.getQuery())){
      verifyOnDemandFeaturegroupSqlQuery(onDemandFeaturegroupDTO.getQuery());
    }
    //Update metadata in on_demand_feature_group table
    onDemandFeaturegroupFacade.updateMetadata(onDemandFeaturegroup);
    
    //Update feature metadata in feature_store_feature table
    if(onDemandFeaturegroupDTO.getFeatures() == null && !onDemandFeaturegroup.getFeatures().isEmpty()) {
      verifyOnDemandFeaturegroupFeatures(onDemandFeaturegroupDTO.getFeatures());
      featurestoreFeatureController.updateOnDemandFeaturegroupFeatures(onDemandFeaturegroup,
        onDemandFeaturegroupDTO.getFeatures());
    }
  }
  
  /**
   * Verifies the name of an on-demand feature group
   * @param name
   * @throws FeaturestoreException
   */
  public void verifyOnDemandFeaturegroupName(String name) throws FeaturestoreException {
    if(Strings.isNullOrEmpty(name)){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_NAME, Level.FINE,
        ", the name of an on-demand feature group should not be empty ");
    }
    if(name.length() >
      FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_NAME_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_NAME, Level.FINE,
        ", the name of an on-demand feature group should be less than "
        + FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_NAME_MAX_LENGTH + " characters");
    }
  }
  
  /**
   * Verifies the description of an on-demand feature group
   *
   * @param description the description to verify
   * @throws FeaturestoreException
   */
  private void verifyOnDemandFeaturegroupDescription(String description) throws FeaturestoreException {
    if(!Strings.isNullOrEmpty(description) &&
      description.length()
        > FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_DESCRIPTION_MAX_LENGTH){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_DESCRIPTION, Level.FINE,
        ", the descritpion of an on-demand feature group should be less than "
        + FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_DESCRIPTION_MAX_LENGTH + " " +
        "characters");
    }
  }
  
  /**
   * Verifies the user input feature list for an on-demand feature group
   *
   * @param featureDTOs the feature list to verify
   */
  private void verifyOnDemandFeaturegroupFeatures(List<FeatureDTO> featureDTOs) throws FeaturestoreException {
    if(!featureDTOs.stream().filter(f -> {
      return (Strings.isNullOrEmpty(f.getName()) || f.getName().length() >
        FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH);
    }).collect(Collectors.toList()).isEmpty()){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_NAME, Level.FINE,
        ", the feature name in an on-demand feature group should be less than "
          + FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH + " characters");
    }
    if(!featureDTOs.stream().filter(f -> {
      return (!Strings.isNullOrEmpty(f.getDescription()) &&
        f.getDescription().length() >
          FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH);
    }).collect(Collectors.toList()).isEmpty()){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_DESCRIPTION, Level.FINE,
        ", the feature description in an on-demand feature group should be less than "
          + FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH + " characters");
    }
  }
  
  /**
   * Verifies the user input JDBC Connector Id for an on-demand feature group
   *
   * @param jdbcConnectorId the JDBC connector id to verify
   * @returns the jdbc connector with the given id if it passed the validation
   * @throws FeaturestoreException
   */
  private FeaturestoreJdbcConnector verifyOnDemandFeaturegroupJdbcConnector(Integer jdbcConnectorId)
    throws FeaturestoreException {
    if(jdbcConnectorId == null){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.JDBC_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreJdbcConnector featurestoreJdbcConnector =
      featurestoreJdbcConnectorFacade.find(jdbcConnectorId);
    if(featurestoreJdbcConnector == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.JDBC_CONNECTOR_NOT_FOUND, Level.FINE,
        "JDBC connector with id: " + jdbcConnectorId + " was not found");
    }
    return featurestoreJdbcConnector;
  }
  
  /**
   * Verifies the user input SQL query for an on-demand feature group
   *
   * @param query the query to verify
   * @throws FeaturestoreException
   */
  private void verifyOnDemandFeaturegroupSqlQuery(String query) throws FeaturestoreException {
    if(Strings.isNullOrEmpty(query)){
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_SQL_QUERY, Level.FINE,
        ", SQL Query cannot be empty");
    }
  
    if(query.length() >
      FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_SQL_QUERY, Level.FINE,
        ", SQL Query cannot exceed " +
        FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH + "characters.");
    }
  }
  
  /**
   * Verify user input specific for creation of on-demand feature group
   *
   * @param onDemandFeaturegroupDTO the input data to use when creating the feature group
   * @throws FeaturestoreException
   */
  private void verifyOnDemandFeaturegroupUserInput(OnDemandFeaturegroupDTO onDemandFeaturegroupDTO)
    throws FeaturestoreException {
    verifyOnDemandFeaturegroupName(onDemandFeaturegroupDTO.getName());
    verifyOnDemandFeaturegroupDescription(onDemandFeaturegroupDTO.getDescription());
    verifyOnDemandFeaturegroupFeatures(onDemandFeaturegroupDTO.getFeatures());
    verifyOnDemandFeaturegroupJdbcConnector(onDemandFeaturegroupDTO.getJdbcConnectorId());
    verifyOnDemandFeaturegroupSqlQuery(onDemandFeaturegroupDTO.getQuery());
  }
  
  /**
   * Removes a on demand feature group
   *
   * @param onDemandFeaturegroup the on-demand feature group
   * @return the deleted entity
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public OnDemandFeaturegroup removeOnDemandFeaturegroup(OnDemandFeaturegroup onDemandFeaturegroup){
    onDemandFeaturegroupFacade.remove(onDemandFeaturegroup);
    return onDemandFeaturegroup;
  }

}
