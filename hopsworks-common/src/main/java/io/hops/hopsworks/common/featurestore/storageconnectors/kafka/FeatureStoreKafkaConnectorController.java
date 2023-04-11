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

package io.hops.hopsworks.common.featurestore.storageconnectors.kafka;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.kafka.FeatureStoreKafkaConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.kafka.SSLEndpointIdentificationAlgorithm;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.kafka.SecurityProtocol;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureStoreKafkaConnectorController {

  private static final Logger LOGGER = Logger.getLogger(FeatureStoreKafkaConnectorController.class.getName());
  
  @EJB
  private StorageConnectorUtil storageConnectorUtil;
  @EJB
  private DistributedFsService dfs;

  public FeatureStoreKafkaConnectorDTO getConnector(FeaturestoreConnector featurestoreConnector)
    throws FeaturestoreException {
    FeatureStoreKafkaConnectorDTO kafkaConnectorDTO = new FeatureStoreKafkaConnectorDTO(featurestoreConnector);
    if (featurestoreConnector.getKafkaConnector().getSslSecret() != null) {
      FeatureStoreKafkaConnectorSecrets kafkaConnectorSecrets = storageConnectorUtil.getSecret(
        featurestoreConnector.getKafkaConnector().getSslSecret(), FeatureStoreKafkaConnectorSecrets.class);
      kafkaConnectorDTO.setSslTruststorePassword(kafkaConnectorSecrets.getSslTrustStorePassword());
      kafkaConnectorDTO.setSslKeystorePassword(kafkaConnectorSecrets.getSslKeyStorePassword());
      kafkaConnectorDTO.setSslKeyPassword(kafkaConnectorSecrets.getSslKeyPassword());
    }
    kafkaConnectorDTO.setSslTruststoreLocation(featurestoreConnector.getKafkaConnector().getTrustStorePath());
    kafkaConnectorDTO.setSslKeystoreLocation(featurestoreConnector.getKafkaConnector().getKeyStorePath());
    kafkaConnectorDTO.setOptions(
      storageConnectorUtil.toOptions(featurestoreConnector.getKafkaConnector().getOptions()));
    return kafkaConnectorDTO;
  }
  
  public FeatureStoreKafkaConnector createConnector(Project project, Users user, Featurestore featureStore,
                                                    FeatureStoreKafkaConnectorDTO kafkaConnectorDTO)
      throws ProjectException, UserException, FeaturestoreException {
    verifyUserInput(kafkaConnectorDTO);
    if (kafkaConnectorDTO.getSecurityProtocol() == SecurityProtocol.SSL) {
      verifySSLSecurityProtocolProperties(project, user, kafkaConnectorDTO);
    }
    
    FeatureStoreKafkaConnector kafkaConnector = new FeatureStoreKafkaConnector();
  
    setGeneralAttributes(kafkaConnectorDTO, kafkaConnector);
  
    // create secret for truststore password etc.
    String secretName = storageConnectorUtil.createSecretName(featureStore.getId(), kafkaConnectorDTO.getName(),
      kafkaConnectorDTO.getStorageConnectorType());
    FeatureStoreKafkaConnectorSecrets secretsClass =
      new FeatureStoreKafkaConnectorSecrets(kafkaConnectorDTO.getSslTruststorePassword(),
        kafkaConnectorDTO.getSslKeystorePassword(), kafkaConnectorDTO.getSslKeyPassword());
    kafkaConnector.setSslSecret(storageConnectorUtil.createProjectSecret(user, secretName, featureStore, secretsClass));
    return kafkaConnector;
  }
  
  private void setGeneralAttributes(FeatureStoreKafkaConnectorDTO kafkaConnectorDTO,
                                    FeatureStoreKafkaConnector kafkaConnector) {
    kafkaConnector.setBootstrapServers(kafkaConnectorDTO.getBootstrapServers());
    kafkaConnector.setSecurityProtocol(kafkaConnectorDTO.getSecurityProtocol());
    kafkaConnector.setOptions(storageConnectorUtil.fromOptions(kafkaConnectorDTO.getOptions()));
    kafkaConnector.setSslEndpointIdentificationAlgorithm(
      SSLEndpointIdentificationAlgorithm.fromString(kafkaConnectorDTO.getSslEndpointIdentificationAlgorithm()));
    kafkaConnector.setKeyStorePath(kafkaConnectorDTO.getSslKeystoreLocation());
    kafkaConnector.setTrustStorePath(kafkaConnectorDTO.getSslTruststoreLocation());
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = FeaturestoreException.class)
  public FeatureStoreKafkaConnector updateConnector(Project project, Users user, Featurestore featureStore,
                                                    FeatureStoreKafkaConnectorDTO kafkaConnectorDTO,
                                                    FeatureStoreKafkaConnector kafkaConnector)
      throws FeaturestoreException, ProjectException, UserException {
    verifyUserInput(kafkaConnectorDTO);
    if (kafkaConnectorDTO.getSecurityProtocol() == SecurityProtocol.SSL) {
      verifySSLSecurityProtocolProperties(project, user, kafkaConnectorDTO);
    }
  
    setGeneralAttributes(kafkaConnectorDTO, kafkaConnector);

    FeatureStoreKafkaConnectorSecrets secretsClass =
      new FeatureStoreKafkaConnectorSecrets(kafkaConnectorDTO.getSslTruststorePassword(),
        kafkaConnectorDTO.getSslKeystorePassword(), kafkaConnectorDTO.getSslKeyPassword());
    kafkaConnector.setSslSecret(storageConnectorUtil.updateProjectSecret(user, kafkaConnector.getSslSecret(),
      kafkaConnector.getSslSecret().getId().getName(), featureStore, secretsClass));
    
    return kafkaConnector;
  }
  
  private void verifyUserInput(FeatureStoreKafkaConnectorDTO kafkaConnectorDTO) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(kafkaConnectorDTO.getBootstrapServers())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Bootstrap server string cannot be null or empty.");
    }
    if (kafkaConnectorDTO.getSecurityProtocol() == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Kafka security protocol cannot be null or empty.");
    }
    if (kafkaConnectorDTO.getBootstrapServers().length() >
      FeaturestoreConstants.KAFKA_STORAGE_CONNECTOR_BOOTSTRAP_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Bootstrap server string is too long, can be maximum " +
          FeaturestoreConstants.KAFKA_STORAGE_CONNECTOR_BOOTSTRAP_MAX_LENGTH + " characters.");
    }
    String optionString = storageConnectorUtil.fromOptions(kafkaConnectorDTO.getOptions());
    if (!Strings.isNullOrEmpty(optionString) && optionString.length() >
      FeaturestoreConstants.STORAGE_CONNECTOR_ARGUMENTS_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Additional Kafka Option list is too long, please raise an Issue with the Hopsworks team.");
    }
  }
  
  private void verifySSLSecurityProtocolProperties(Project project, Users user,
                                                   FeatureStoreKafkaConnectorDTO kafkaConnectorDTO)
      throws FeaturestoreException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps(project, user);
      storageConnectorUtil.validatePath(dfso, kafkaConnectorDTO.getSslTruststoreLocation(), "Truststore location");
      storageConnectorUtil.validatePath(dfso, kafkaConnectorDTO.getSslKeystoreLocation(), "Keystore location");
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.INFO,
          "Error validating keystore/truststore path", e.getMessage(), e);
    } finally {
      dfs.closeDfsClient(dfso);
    }

    if (Strings.isNullOrEmpty(kafkaConnectorDTO.getSslKeyPassword())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Key password cannot be null or empty for Kafka SSL Security Policy.");
    }
  }
}
