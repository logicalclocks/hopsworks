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

package io.hops.hopsworks.common.featurestore.featuregroup.ondemand;

import com.google.common.base.Strings;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.ondemand.OnDemandFeaturegroup;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.jdbc.FeaturestoreJdbcConnector;
import io.hops.hopsworks.common.featurestore.feature.FeaturestoreFeatureController;
import io.hops.hopsworks.common.featurestore.storageconnectors.jdbc.FeaturestoreJdbcConnectorFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
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
    //Verify User Input specific for on demand feature groups
    FeaturestoreJdbcConnector featurestoreJdbcConnector =
      verifyOnDemandFeaturegroupJdbcConnector(onDemandFeaturegroupDTO.getJdbcConnectorId());
    verifyOnDemandFeaturegroupSqlQuery(onDemandFeaturegroupDTO.getQuery());
    //Persist on-demand featuregroup
    OnDemandFeaturegroup onDemandFeaturegroup = new OnDemandFeaturegroup();
    onDemandFeaturegroup.setDescription(onDemandFeaturegroupDTO.getDescription());
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

    // Verify User Input specific for on demand feature groups
    FeaturestoreJdbcConnector featurestoreJdbcConnector =
      verifyOnDemandFeaturegroupJdbcConnector(onDemandFeaturegroupDTO.getJdbcConnectorId());
    verifyOnDemandFeaturegroupSqlQuery(onDemandFeaturegroupDTO.getQuery());
    
    // Update metadata in entity
    onDemandFeaturegroup.setDescription(onDemandFeaturegroupDTO.getDescription());
    onDemandFeaturegroup.setFeaturestoreJdbcConnector(featurestoreJdbcConnector);
    onDemandFeaturegroup.setQuery(onDemandFeaturegroupDTO.getQuery());

    // finally persist in database
    onDemandFeaturegroupFacade.updateMetadata(onDemandFeaturegroup);
    featurestoreFeatureController.updateOnDemandFeaturegroupFeatures(onDemandFeaturegroup,
      onDemandFeaturegroupDTO.getFeatures());
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
      FeaturestoreConstants.ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.INVALID_SQL_QUERY, Level.FINE,
        ", SQL Query cannot exceed " +
          FeaturestoreConstants.ON_DEMAND_FEATUREGROUP_SQL_QUERY_MAX_LENGTH + "characters.");
    }
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
