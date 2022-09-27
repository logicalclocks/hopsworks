/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.api.cloud;


import com.google.common.base.Strings;
import io.hops.hopsworks.common.cloud.AWSClusterCredentials;
import io.hops.hopsworks.common.cloud.AWSClusterCredentialsService;
import io.hops.hopsworks.common.cloud.AWSSecurityTokenService;
import io.hops.hopsworks.common.cloud.CloudRoleMappingController;
import io.hops.hopsworks.common.cloud.Credentials;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.redshift.FeaturestoreRedshiftConnectorDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.s3.FeaturestoreS3ConnectorDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.persistence.entity.cloud.CloudRoleMapping;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TemporaryCredentialsHelper {
  
  @Inject
  private AWSSecurityTokenService awsSecurityTokenService;
  @Inject
  private AWSClusterCredentialsService awsClusterCredentialsService;
  @EJB
  private CloudRoleMappingController cloudRoleMappingController;
  @EJB
  private Settings settings;
  
  public boolean checkService() {
    return awsSecurityTokenService.isAWSCloud();
  }
  
  public Credentials getTemporaryCredentials(Users user, Project project, int durationSeconds, String roleARN)
    throws CloudException {
    return getTemporaryCredentials(roleARN, null, durationSeconds, user, project);
  }
  
  public Credentials getTemporaryCredentials(String roleARN, String roleSessionName, int durationSeconds, Users user,
    Project project) throws CloudException {
    CloudRoleMapping cloudRoleMapping = getCloudRoleMapping(roleARN, user, project);
    return awsSecurityTokenService.assumeRole(cloudRoleMapping.getCloudRole(), roleSessionName, durationSeconds);
  }
  
  public AWSClusterCredentials getTemporaryClusterCredentials(Users user, Project project, int durationSeconds,
    FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO)
    throws CloudException {
    String roleARN = featurestoreRedshiftConnectorDTO.getIamRole();
    CloudRoleMapping cloudRoleMapping = getCloudRoleMapping(roleARN, user, project);
    return awsClusterCredentialsService.getClusterCredential(cloudRoleMapping.getCloudRole(), null, 0,
      featurestoreRedshiftConnectorDTO.getClusterIdentifier(), featurestoreRedshiftConnectorDTO.getAutoCreate(),
      featurestoreRedshiftConnectorDTO.getDatabaseName(), featurestoreRedshiftConnectorDTO.getDatabaseUserName(),
      featurestoreRedshiftConnectorDTO.getDatabaseGroups(), durationSeconds);
  }
  
  private CloudRoleMapping getCloudRoleMapping(String roleARN, Users user, Project project) throws CloudException {
    CloudRoleMapping cloudRoleMapping = cloudRoleMappingController.getRoleMappingOrDefault(roleARN, project, user);
    if (!cloudRoleMappingController.canAssumeRole(user, cloudRoleMapping)) {
      throw new CloudException(RESTCodes.CloudErrorCode.ACCESS_CONTROL_EXCEPTION, Level.FINE, "No role mapping found " +
        "for user.");
    }
    return cloudRoleMapping;
  }
  
  public void setTemporaryCredentials(boolean temporaryCredentials, Users user, Project project, int durationSeconds,
    FeaturestoreStorageConnectorDTO featurestoreStorageConnectorDTO) throws CloudException {
    if (durationSeconds < 0) {
      durationSeconds = settings.getFSStorageConnectorSessionDuration();
    }
    if (temporaryCredentials) {
      switch (featurestoreStorageConnectorDTO.getStorageConnectorType()) {
        case S3:
          FeaturestoreS3ConnectorDTO featurestoreS3ConnectorDTO =
            (FeaturestoreS3ConnectorDTO) featurestoreStorageConnectorDTO;
          //can not set default role
          if (!Strings.isNullOrEmpty(featurestoreS3ConnectorDTO.getIamRole())) {
            Credentials credentials =
              getTemporaryCredentials(user, project, durationSeconds, featurestoreS3ConnectorDTO.getIamRole());
            featurestoreS3ConnectorDTO.setSecretKey(credentials.getSecretAccessKey());
            featurestoreS3ConnectorDTO.setAccessKey(credentials.getAccessKeyId());
            featurestoreS3ConnectorDTO.setSessionToken(credentials.getSessionToken());
          }
          break;
        case REDSHIFT:
          FeaturestoreRedshiftConnectorDTO featurestoreRedshiftConnectorDTO =
            (FeaturestoreRedshiftConnectorDTO) featurestoreStorageConnectorDTO;
          if (!Strings.isNullOrEmpty(featurestoreRedshiftConnectorDTO.getIamRole())) {
            AWSClusterCredentials credentials =
              getTemporaryClusterCredentials(user, project, durationSeconds, featurestoreRedshiftConnectorDTO);
            featurestoreRedshiftConnectorDTO.setDatabasePassword(credentials.getDbPassword());
            featurestoreRedshiftConnectorDTO.setDatabaseUserName(credentials.getDbUser());
            featurestoreRedshiftConnectorDTO.setExpiration(credentials.getExpiration());
          }
      }
    }
  }
}
