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
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.kafka.FeatureStoreKafkaConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.kafka.SSLEndpointIdentificationAlgorithm;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.kafka.SecurityProtocol;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transactional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureStoreKafkaConnectorController {
  
  private static final Logger LOGGER = Logger.getLogger(FeatureStoreKafkaConnectorController.class.getName());
  
  @EJB
  private StorageConnectorUtil storageConnectorUtil;
  @EJB
  private InodeController inodeController;
  
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
    kafkaConnectorDTO.setSslTruststoreLocation(
      featurestoreConnector.getKafkaConnector().getTruststoreInode() == null ? null:
        inodeController.getPath(featurestoreConnector.getKafkaConnector().getTruststoreInode()));
    kafkaConnectorDTO.setSslKeystoreLocation(
      featurestoreConnector.getKafkaConnector().getKeystoreInode() == null ? null:
        inodeController.getPath(featurestoreConnector.getKafkaConnector().getKeystoreInode()));
    kafkaConnectorDTO.setOptions(
      storageConnectorUtil.toOptions(featurestoreConnector.getKafkaConnector().getOptions()));
    return kafkaConnectorDTO;
  }
  
  public FeatureStoreKafkaConnector createConnector(Users user, Featurestore featureStore,
                                                    FeatureStoreKafkaConnectorDTO kafkaConnectorDTO)
      throws ProjectException, UserException, FeaturestoreException {
    verifyUserInput(kafkaConnectorDTO);
    if (kafkaConnectorDTO.getSecurityProtocol() == SecurityProtocol.SSL) {
      verifySSLSecurityProtocolProperties(kafkaConnectorDTO);
    }
    
    FeatureStoreKafkaConnector kafkaConnector = new FeatureStoreKafkaConnector();
  
    setGeneralAttributes(kafkaConnectorDTO, kafkaConnector);
  
    // get and set inodes to validate that truststore/keystore exist at provided path
    setTrustStoreKeyStoreInodes(kafkaConnectorDTO, kafkaConnector);
  
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
  }
  
  private void setTrustStoreKeyStoreInodes(FeatureStoreKafkaConnectorDTO kafkaConnectorDTO,
                                           FeatureStoreKafkaConnector kafkaConnector)
      throws FeaturestoreException {
    Inode truststoreInode = kafkaConnectorDTO.getSslTruststoreLocation() == null ? null :
      inodeController.getInodeAtPath(kafkaConnectorDTO.getSslTruststoreLocation());
    Inode keystoreInode = kafkaConnectorDTO.getSslKeystoreLocation() == null ? null :
      inodeController.getInodeAtPath(kafkaConnectorDTO.getSslKeystoreLocation());
    
    if (truststoreInode == null && kafkaConnectorDTO.getSslTruststoreLocation() != null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KAFKA_STORAGE_CONNECTOR_STORE_NOT_EXISTING,
        Level.FINE, "Could not find trust store in provided location: " + kafkaConnectorDTO.getSslTruststoreLocation());
    }
    
    if (keystoreInode == null && kafkaConnectorDTO.getSslKeystoreLocation() != null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.KAFKA_STORAGE_CONNECTOR_STORE_NOT_EXISTING,
        Level.FINE, "Could not find key store in provided location: " + kafkaConnectorDTO.getSslKeystoreLocation());
    }
    
    kafkaConnector.setTruststoreInode(truststoreInode);
    kafkaConnector.setKeystoreInode(keystoreInode);
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = FeaturestoreException.class)
  public FeatureStoreKafkaConnector updateConnector(Users user, Featurestore featureStore,
                                                    FeatureStoreKafkaConnectorDTO kafkaConnectorDTO,
                                                    FeatureStoreKafkaConnector kafkaConnector)
      throws FeaturestoreException, ProjectException, UserException {
    verifyUserInput(kafkaConnectorDTO);
    if (kafkaConnectorDTO.getSecurityProtocol() == SecurityProtocol.SSL) {
      verifySSLSecurityProtocolProperties(kafkaConnectorDTO);
    }
  
    setGeneralAttributes(kafkaConnectorDTO, kafkaConnector);
    
    setTrustStoreKeyStoreInodes(kafkaConnectorDTO, kafkaConnector);
    
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
  
  private void verifySSLSecurityProtocolProperties(FeatureStoreKafkaConnectorDTO kafkaConnectorDTO)
      throws FeaturestoreException {
    if (Strings.isNullOrEmpty(kafkaConnectorDTO.getSslTruststoreLocation())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Truststore location cannot be null or empty for Kafka SSL Security Policy.");
    }
    if (Strings.isNullOrEmpty(kafkaConnectorDTO.getSslKeystoreLocation())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Keystore location cannot be null or empty for Kafka SSL Security Policy.");
    }
    if (Strings.isNullOrEmpty(kafkaConnectorDTO.getSslTruststorePassword())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Truststore password cannot be null or empty for Kafka SSL Security Policy.");
    }
    if (Strings.isNullOrEmpty(kafkaConnectorDTO.getSslKeystorePassword())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Keystore password cannot be null or empty for Kafka SSL Security Policy.");
    }
    if (Strings.isNullOrEmpty(kafkaConnectorDTO.getSslKeyPassword())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
        "Key password cannot be null or empty for Kafka SSL Security Policy.");
    }
  }
}
