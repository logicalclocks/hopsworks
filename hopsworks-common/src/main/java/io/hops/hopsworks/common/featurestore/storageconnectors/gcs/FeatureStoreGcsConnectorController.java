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

package io.hops.hopsworks.common.featurestore.storageconnectors.gcs;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.gcs.FeatureStoreGcsConnector;
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
public class FeatureStoreGcsConnectorController {
  
  private static final Logger LOGGER = Logger.getLogger(FeatureStoreGcsConnectorController.class.getName());
  
  @EJB
  private StorageConnectorUtil storageConnectorUtil;
  @EJB
  private InodeController inodeController;
  
  public FeatureStoreGcsConnectorDTO getConnector(FeaturestoreConnector featurestoreConnector)
    throws FeaturestoreException {
    FeatureStoreGcsConnectorDTO gcsConnectorDTO = new FeatureStoreGcsConnectorDTO(featurestoreConnector);
    gcsConnectorDTO.setKeyPath(inodeController.getPath(featurestoreConnector.getGcsConnector().getKeyInode()));
    gcsConnectorDTO.setBucket(featurestoreConnector.getGcsConnector().getBucket());
    if (featurestoreConnector.getGcsConnector().getEncryptionSecret() != null) {
      EncryptionSecrets encryptionSecrets = storageConnectorUtil.getSecret(
        featurestoreConnector.getGcsConnector().getEncryptionSecret(), EncryptionSecrets.class);
      gcsConnectorDTO.setEncryptionKey(encryptionSecrets.getEncryptionKey());
      gcsConnectorDTO.setEncryptionKeyHash(encryptionSecrets.getEncryptionKeyHash());
      
    }
    return gcsConnectorDTO;
  }
  
  public FeatureStoreGcsConnector createConnector(Users user, Featurestore featureStore,
    FeatureStoreGcsConnectorDTO gcsConnectorDTO)
    throws FeaturestoreException, ProjectException, UserException {
    validateInput(gcsConnectorDTO);
    
    FeatureStoreGcsConnector gcsConnector = new FeatureStoreGcsConnector();
    setKeyInode(gcsConnectorDTO, gcsConnector);
    gcsConnector.setAlgorithm(gcsConnectorDTO.getAlgorithm());
    gcsConnector.setBucket(gcsConnectorDTO.getBucket());

    if (gcsConnectorDTO.getAlgorithm() != null) {
      String secretName = storageConnectorUtil.createSecretName(featureStore.getId(), gcsConnectorDTO.getName(),
        gcsConnectorDTO.getStorageConnectorType());
      EncryptionSecrets secretsClass = new EncryptionSecrets(gcsConnectorDTO.getEncryptionKey(),
        gcsConnectorDTO.getEncryptionKeyHash());
      gcsConnector.setEncryptionSecret(storageConnectorUtil.createProjectSecret(user, secretName, featureStore,
        secretsClass));
    }
    return gcsConnector;
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = FeaturestoreException.class)
  public FeatureStoreGcsConnector updateConnector(Users user, Featurestore featureStore,
    FeatureStoreGcsConnectorDTO gcsConnectorDTO,
    FeatureStoreGcsConnector gcsConnector)
    throws FeaturestoreException, ProjectException, UserException {

    validateInput(gcsConnectorDTO);
    setKeyInode(gcsConnectorDTO, gcsConnector);
    gcsConnector.setAlgorithm(gcsConnectorDTO.getAlgorithm());
    gcsConnector.setBucket(gcsConnectorDTO.getBucket());

    if (gcsConnectorDTO.getAlgorithm() != null) {
      String secretName = storageConnectorUtil.createSecretName(featureStore.getId(), gcsConnectorDTO.getName(),
        gcsConnectorDTO.getStorageConnectorType());
      EncryptionSecrets secretsClass = new EncryptionSecrets(gcsConnectorDTO.getEncryptionKey(),
        gcsConnectorDTO.getEncryptionKeyHash());
      gcsConnector.setEncryptionSecret(
        storageConnectorUtil.updateProjectSecret(user, gcsConnector.getEncryptionSecret(),
          secretName, featureStore, secretsClass));
    }
    return gcsConnector;
  }
  
  private void setKeyInode(FeatureStoreGcsConnectorDTO gcsConnectorDTO, FeatureStoreGcsConnector gcsConnector)
    throws FeaturestoreException {
    Inode keyInode = inodeController.getInodeAtPath(gcsConnectorDTO.getKeyPath());
    
    if (keyInode == null && gcsConnectorDTO.getKeyPath() != null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG,
        Level.FINE, "Could not find key file in provided location: " + gcsConnectorDTO.getKeyPath());
    }
  
    gcsConnector.setKeyInode(keyInode);
  }
  
  public void validateInput(FeatureStoreGcsConnectorDTO gcsConnectorDTO) throws FeaturestoreException {
    if (Strings.isNullOrEmpty(gcsConnectorDTO.getKeyPath())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.GCS_FIELD_MISSING, Level.FINE,
        "Key File Path cannot be empty");
    }
    if (Strings.isNullOrEmpty(gcsConnectorDTO.getBucket())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.GCS_FIELD_MISSING, Level.FINE,
                                      "Bucket cannot be empty");
    }
    
    // either none of the three should be set or all
    if (!Strings.isNullOrEmpty(gcsConnectorDTO.getEncryptionKey())
      || !Strings.isNullOrEmpty(gcsConnectorDTO.getEncryptionKeyHash())
      || gcsConnectorDTO.getAlgorithm() != null) {
      if (Strings.isNullOrEmpty(gcsConnectorDTO.getEncryptionKey())
        || Strings.isNullOrEmpty(gcsConnectorDTO.getEncryptionKeyHash())
        || gcsConnectorDTO.getAlgorithm() == null) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.GCS_FIELD_MISSING, Level.FINE,
          "Encryption algorithm, key and key hash have all to be set or all to be null, you provided: algorithm="
            + gcsConnectorDTO.getAlgorithm() + ", key="
            + gcsConnectorDTO.getEncryptionKey() + ", hashKey="
            + gcsConnectorDTO.getEncryptionKeyHash());
      }
    }
  }
}
