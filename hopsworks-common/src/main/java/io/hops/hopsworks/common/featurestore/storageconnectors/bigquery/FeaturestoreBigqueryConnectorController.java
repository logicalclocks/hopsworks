/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.storageconnectors.bigquery;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.bigquery.FeatureStoreBigqueryConnector;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transactional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class controlling the interaction with the feature_store_bigquery_connector table and required business logic
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreBigqueryConnectorController {
  
  private static final Logger LOGGER = Logger.getLogger(FeaturestoreBigqueryConnectorController.class.getName());
  @EJB
  private StorageConnectorUtil storageConnectorUtil;

  public FeaturestoreBigqueryConnectorController() {}

  @VisibleForTesting
  public FeaturestoreBigqueryConnectorController(StorageConnectorUtil storageConnectorUtil) {
    this.storageConnectorUtil = storageConnectorUtil;
  }

  public FeaturestoreBigqueryConnectorDTO getBigqueryConnectorDTO(FeaturestoreConnector featurestoreConnector)
    throws FeaturestoreException {
    
    FeaturestoreBigqueryConnectorDTO bigqueryConnectorDTO = new FeaturestoreBigqueryConnectorDTO(featurestoreConnector);
    bigqueryConnectorDTO.setKeyPath(featurestoreConnector.getBigqueryConnector().getKeyPath());
    bigqueryConnectorDTO.setParentProject(
      featurestoreConnector.getBigqueryConnector().getParentProject()
    );
    bigqueryConnectorDTO.setDataset(featurestoreConnector.getBigqueryConnector().getDataset());
    bigqueryConnectorDTO.setQueryProject(featurestoreConnector.getBigqueryConnector().getQueryProject());
    bigqueryConnectorDTO.setQueryTable(
      featurestoreConnector.getBigqueryConnector().getQueryTable()
    );
    bigqueryConnectorDTO.setMaterializationDataset(
      featurestoreConnector.getBigqueryConnector().getMaterializationDataset()
    );
    bigqueryConnectorDTO.setArguments(
      storageConnectorUtil.toOptions(featurestoreConnector.getBigqueryConnector().getArguments())
    );
    
    return bigqueryConnectorDTO;
  }
  
  public FeatureStoreBigqueryConnector createBigqueryConnector(Project project, Users users,
         FeaturestoreBigqueryConnectorDTO featurestoreBigqueryConnectorDTO)
      throws FeaturestoreException {
    
    validateInput(project, users, featurestoreBigqueryConnectorDTO);
    FeatureStoreBigqueryConnector featurestoreBigqueryConnector = new FeatureStoreBigqueryConnector();
    return setConnectorData(featurestoreBigqueryConnectorDTO, featurestoreBigqueryConnector);
  }
  
  
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = {FeaturestoreException.class})
  public FeatureStoreBigqueryConnector updateBigqueryConnector(Project project, Users user,
          FeaturestoreBigqueryConnectorDTO featurestoreBigqueryConnectorDTO,
          FeatureStoreBigqueryConnector featureStoreBigqueryConnector)
      throws FeaturestoreException {
    
    validateInput(project, user, featurestoreBigqueryConnectorDTO);
    return setConnectorData(featurestoreBigqueryConnectorDTO, featureStoreBigqueryConnector);
  }
  
  /**
   * Helper method to set the DTO values into controller
   *
   * @param featurestoreBigqueryConnectorDTO
   * @param featureStoreBigqueryConnector
   * @return
   * @throws FeaturestoreException
   */
  private FeatureStoreBigqueryConnector setConnectorData(
      FeaturestoreBigqueryConnectorDTO featurestoreBigqueryConnectorDTO,
      FeatureStoreBigqueryConnector featureStoreBigqueryConnector) {

    featureStoreBigqueryConnector.setKeyPath(featurestoreBigqueryConnectorDTO.getKeyPath());
    featureStoreBigqueryConnector.setParentProject(featurestoreBigqueryConnectorDTO.getParentProject());
    featureStoreBigqueryConnector.setDataset(featurestoreBigqueryConnectorDTO.getDataset());
    featureStoreBigqueryConnector.setQueryTable(featurestoreBigqueryConnectorDTO.getQueryTable());
    featureStoreBigqueryConnector.setQueryProject(featurestoreBigqueryConnectorDTO.getQueryProject());

    featureStoreBigqueryConnector.setMaterializationDataset(
      featurestoreBigqueryConnectorDTO.getMaterializationDataset()
    );

    featureStoreBigqueryConnector.setArguments(
      storageConnectorUtil.fromOptions(featurestoreBigqueryConnectorDTO.getArguments())
    );
    
    return featureStoreBigqueryConnector;
  }

  public void validateInput(Project project, Users user,
                            FeaturestoreBigqueryConnectorDTO featurestoreBigqueryConnectorDTO)
    throws FeaturestoreException {
    
    //validate keyPath
    storageConnectorUtil.validatePath(project, user, featurestoreBigqueryConnectorDTO.getKeyPath(), "Key file path");

    // validate parentProject
    if (Strings.isNullOrEmpty(featurestoreBigqueryConnectorDTO.getParentProject())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
                                      "Parent Project is mandatory");
    }
    // query project,dataset and table are null or not null together
    if (!Strings.isNullOrEmpty(featurestoreBigqueryConnectorDTO.getQueryProject()) ||
      !Strings.isNullOrEmpty(featurestoreBigqueryConnectorDTO.getDataset()) ||
      !Strings.isNullOrEmpty(featurestoreBigqueryConnectorDTO.getQueryTable())) {
      
      if (Strings.isNullOrEmpty(featurestoreBigqueryConnectorDTO.getQueryProject()) ||
        Strings.isNullOrEmpty(featurestoreBigqueryConnectorDTO.getDataset()) ||
        Strings.isNullOrEmpty(featurestoreBigqueryConnectorDTO.getQueryTable())) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
                                        "Query Project, Dataset, Table are set either all together" +
                                          " or none of them");
      }
    }
    // materialization dataset is required if query project,dataset and table are null
    if (Strings.isNullOrEmpty(featurestoreBigqueryConnectorDTO.getQueryProject()) &&
      Strings.isNullOrEmpty(featurestoreBigqueryConnectorDTO.getDataset()) &&
      Strings.isNullOrEmpty(featurestoreBigqueryConnectorDTO.getQueryTable())) {
      if (Strings.isNullOrEmpty(featurestoreBigqueryConnectorDTO.getMaterializationDataset())) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
                                        "Materialization Dataset is required if Query " +
                                          "Project, Dataset, " +
                                          "Table are null");
      }
    }
    // validate arguments length
    if (featurestoreBigqueryConnectorDTO.getArguments() != null) {
      String arguments = storageConnectorUtil.fromOptions(featurestoreBigqueryConnectorDTO.getArguments());
      if (!Strings.isNullOrEmpty(arguments)
        && arguments.length() > FeaturestoreConstants.STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_JDBC_CONNECTION_ARGUMENTS, Level.FINE,
          "Key-Value arguments should not exceed: " +
            FeaturestoreConstants.STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH + " characters");
      }
    }

  }
}
