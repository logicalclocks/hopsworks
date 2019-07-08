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
import io.hops.hopsworks.common.dao.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.common.dao.featurestore.settings.FeaturestoreClientSettingsDTO;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;
import io.hops.hopsworks.common.dao.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnectorFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;

/**
 * Class controlling the interaction with the on_demand_feature_group table and required business logic
 */
@Stateless
public class OnDemandFeaturegroupController {
  @EJB
  private OnDemandFeaturegroupFacade onDemandFeaturegroupFacade;
  @EJB
  private FeaturestoreJdbcConnectorFacade featurestoreJdbcConnectorFacade;
  
  /**
   * Persists an on demand feature group
   *
   * @param featuregroup the parent feature group entity (one-to-one relationship)
   * @param onDemandFeaturegroupDTO the user input data to use when creating the feature group
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public OnDemandFeaturegroupDTO createOnDemandFeaturegroup(
      Featuregroup featuregroup, OnDemandFeaturegroupDTO onDemandFeaturegroupDTO)
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
    onDemandFeaturegroup.setFeaturegroup(featuregroup);
    onDemandFeaturegroup.setDescription(onDemandFeaturegroupDTO.getDescription());
    onDemandFeaturegroup.setName(onDemandFeaturegroupDTO.getName());
    onDemandFeaturegroup.setFeaturestoreJdbcConnector(featurestoreJdbcConnector);
    onDemandFeaturegroup.setQuery(onDemandFeaturegroupDTO.getQuery());
    onDemandFeaturegroupFacade.persist(onDemandFeaturegroup);
    return new OnDemandFeaturegroupDTO(onDemandFeaturegroup);
  }

  /**
   * Verify user input specific for creation of on-demand feature group
   *
   * @param onDemandFeaturegroupDTO the input data to use when creating the feature group
   */
  public void verifyOnDemandFeaturegroupUserInput(OnDemandFeaturegroupDTO onDemandFeaturegroupDTO) {

    if(onDemandFeaturegroupDTO.getName().length() >
      FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_NAME_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_NAME.getMessage()
          + ", the name of a on-demand feature group should be less than "
          + FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_NAME_MAX_LENGTH + " characters");
    }

    if(!Strings.isNullOrEmpty(onDemandFeaturegroupDTO.getDescription()) &&
        onDemandFeaturegroupDTO.getDescription().length()
            > FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_DESCRIPTION_MAX_LENGTH){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATUREGROUP_DESCRIPTION.getMessage()
          + ", the descritpion of an on-demand feature group should be less than "
          + FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_DESCRIPTION_MAX_LENGTH + " " +
          "characters");
    }

    onDemandFeaturegroupDTO.getFeatures().stream().forEach(f -> {
      if(Strings.isNullOrEmpty(f.getName()) || f.getName().length() >
          FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH){
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_NAME.getMessage()
            + ", the feature name in an on-demand feature group should be less than "
            + FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_FEATURE_NAME_MAX_LENGTH + " characters");
      }
      if(!Strings.isNullOrEmpty(f.getDescription()) &&
          f.getDescription().length() >
            FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH) {
        throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_DESCRIPTION.getMessage()
            + ", the feature description in an on-demand feature group should be less than "
            + FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_FEATURE_DESCRIPTION_MAX_LENGTH + " characters");
      }
    });

    if(onDemandFeaturegroupDTO.getJdbcConnectorId() == null){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.JDBC_CONNECTOR_ID_NOT_PROVIDED.getMessage());
    }
    FeaturestoreJdbcConnector featurestoreJdbcConnector =
        featurestoreJdbcConnectorFacade.find(onDemandFeaturegroupDTO.getJdbcConnectorId());
    if(featurestoreJdbcConnector == null) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.JDBC_CONNECTOR_NOT_FOUND.getMessage()
          + "JDBC connector with id: " + onDemandFeaturegroupDTO.getJdbcConnectorId() + " was not found");
    }

    if(Strings.isNullOrEmpty(onDemandFeaturegroupDTO.getQuery())){
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.INVALID_SQL_QUERY.getMessage()
          + ", SQL Query cannot be empty");
    }

    if(onDemandFeaturegroupDTO.getQuery().length() >
      FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH) {
      throw new IllegalArgumentException(RESTCodes.FeaturestoreErrorCode.INVALID_SQL_QUERY.getMessage()
          + ", SQL Query cannot exceed " +
        FeaturestoreClientSettingsDTO.ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH + " " +
        "characters.");
    }
  }

}
