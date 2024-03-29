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

package io.hops.hopsworks.common.featurestore.storageconnectors.adls;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.OptionDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.StorageConnectorUtil;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.adls.FeaturestoreADLSConnector;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.Secret;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreADLSConnectorController {

  @EJB
  private StorageConnectorUtil storageConnectorUtil;

  public FeaturestoreADLSConnector createADLConnector(Users user, Project project, Featurestore featurestore,
                                                      FeaturestoreADLSConnectorDTO adlConnectorDTO)
      throws FeaturestoreException, ProjectException, UserException {
    verifyConnectorDTO(adlConnectorDTO);

    String secretName = storageConnectorUtil.createSecretName(featurestore.getId(), adlConnectorDTO.getName(),
      adlConnectorDTO.getStorageConnectorType());
    Secret secret = storageConnectorUtil.createProjectSecret(user, secretName, featurestore,
        adlConnectorDTO.getServiceCredential());

    FeaturestoreADLSConnector adlConnector = new FeaturestoreADLSConnector();
    adlConnector.setGeneration(adlConnectorDTO.getGeneration());
    adlConnector.setDirectoryId(adlConnectorDTO.getDirectoryId());
    adlConnector.setApplicationId(adlConnectorDTO.getApplicationId());
    adlConnector.setServiceCredentialSecret(secret);
    adlConnector.setAccountName(adlConnectorDTO.getAccountName());
    adlConnector.setContainerName(adlConnectorDTO.getContainerName());

    return adlConnector;
  }



  public FeaturestoreADLSConnectorDTO getADLConnectorDTO(FeaturestoreConnector featurestoreConnector)
      throws FeaturestoreException {
    FeaturestoreADLSConnectorDTO adlsConnectorDTO = new FeaturestoreADLSConnectorDTO(featurestoreConnector);
    String serviceCredential = storageConnectorUtil.getSecret(
      featurestoreConnector.getAdlsConnector().getServiceCredentialSecret(), String.class);

    adlsConnectorDTO.setServiceCredential(serviceCredential);

    // Spark/Hadoop-Azure requries these options for setting up the reader/writer
    FeaturestoreADLSConnector adlsConnector = featurestoreConnector.getAdlsConnector();

    if (adlsConnector.getGeneration() == 1) {
      adlsConnectorDTO.setSparkOptions(getSparkOptionsGen1(adlsConnector, serviceCredential));
    } else if (adlsConnector.getGeneration() == 2) {
      adlsConnectorDTO.setSparkOptions(getSparkOptionsGen2(adlsConnector, serviceCredential));
    }

    return adlsConnectorDTO;
  }

  private List<OptionDTO> getSparkOptionsGen2(FeaturestoreADLSConnector adlsConnector, String serviceCredential) {
    List<OptionDTO> sparkOptions = new ArrayList<>();
    sparkOptions.add(new OptionDTO(
        "fs.azure.account.auth.type." + adlsConnector.getAccountName() + ".dfs.core.windows.net",
        "OAuth"));
    sparkOptions.add(new OptionDTO(
        "fs.azure.account.oauth.provider.type." + adlsConnector.getAccountName() + ".dfs.core.windows.net",
        "org.apache.hadoop.fs.azurebfs.oauth2.ClientCredsTokenProvider"));
    sparkOptions.add(new OptionDTO(
        "fs.azure.account.oauth2.client.id." + adlsConnector.getAccountName() + ".dfs.core.windows.net",
        adlsConnector.getApplicationId()));
    sparkOptions.add(new OptionDTO(
        "fs.azure.account.oauth2.client.secret." + adlsConnector.getAccountName() + ".dfs.core.windows.net",
        serviceCredential));
    sparkOptions.add(new OptionDTO(
        "fs.azure.account.oauth2.client.endpoint." + adlsConnector.getAccountName() + ".dfs.core.windows.net",
        "https://login.microsoftonline.com/" + adlsConnector.getDirectoryId() + "/oauth2/token"));

    return sparkOptions;
  }

  private List<OptionDTO> getSparkOptionsGen1(FeaturestoreADLSConnector adlsConnector, String serviceCredential) {
    List<OptionDTO> sparkOptions = new ArrayList<>();
    sparkOptions.add(new OptionDTO(
        "fs.adl.oauth2.access.token.provider.type", "ClientCredential"));
    sparkOptions.add(new OptionDTO(
        "fs.adl.account." + adlsConnector.getAccountName() + ".oauth2.client.id",
        adlsConnector.getApplicationId()));
    sparkOptions.add(new OptionDTO(
        "fs.adl.account." + adlsConnector.getAccountName() + ".oauth2.credential",
        serviceCredential));
    sparkOptions.add(new OptionDTO(
        "fs.adl.account." + adlsConnector.getAccountName() + ".oauth2.refresh.url",
        "https://login.microsoftonline.com/" + adlsConnector.getDirectoryId() + "/oauth2/token"));

    return sparkOptions;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  @Transactional(rollbackOn = FeaturestoreException.class)
  public FeaturestoreADLSConnector updateAdlConnector(Users user, Featurestore featureStore,
                                                      FeaturestoreADLSConnectorDTO adlConnectorDTO,
                                                      FeaturestoreADLSConnector adlConnector)
      throws FeaturestoreException, ProjectException, UserException {
    verifyConnectorDTO(adlConnectorDTO);

    adlConnector.setGeneration(adlConnectorDTO.getGeneration());
    adlConnector.setDirectoryId(adlConnectorDTO.getDirectoryId());
    adlConnector.setApplicationId(adlConnectorDTO.getApplicationId());
    adlConnector.setAccountName(adlConnectorDTO.getAccountName());
    adlConnector.setContainerName(adlConnectorDTO.getContainerName());
    // Update the content of the secret
    storageConnectorUtil.updateProjectSecret(user, adlConnector.getServiceCredentialSecret(),
      adlConnector.getServiceCredentialSecret().getId().getName(), featureStore,
      adlConnectorDTO.getServiceCredential());

    return adlConnector;
  }

  private void verifyConnectorDTO(FeaturestoreADLSConnectorDTO adlConnectorDTO) throws FeaturestoreException {
    if (adlConnectorDTO.getGeneration() == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Generation is empty");
    }
    int generation = adlConnectorDTO.getGeneration() != null ? adlConnectorDTO.getGeneration() : 2;
    if (!(generation == 1 || generation == 2)) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "Invalid ADLS generation - Only generation 1 and 2 supported");
    }
    if (Strings.isNullOrEmpty(adlConnectorDTO.getDirectoryId())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "DirectoryId is empty");
    }
    if (Strings.isNullOrEmpty(adlConnectorDTO.getApplicationId())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "ApplicationId is empty");
    }
    if (Strings.isNullOrEmpty(adlConnectorDTO.getServiceCredential())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "ServiceCredentials is empty");
    }
    if (Strings.isNullOrEmpty(adlConnectorDTO.getAccountName())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "AccountName is empty");
    }
    if (generation == 2 && Strings.isNullOrEmpty(adlConnectorDTO.getContainerName())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_STORAGE_CONNECTOR_ARG, Level.FINE,
          "ContainerName is empty");
    }
  }
}
