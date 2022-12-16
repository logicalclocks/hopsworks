/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.aws.cloud.security;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.cloud.AWSClusterCredentials;
import io.hops.hopsworks.common.cloud.AWSClusterCredentialsService;
import io.hops.hopsworks.common.cloud.AWSSecurityTokenService;
import io.hops.hopsworks.common.integrations.CloudStereotype;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.restutils.RESTCodes;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.services.redshift.RedshiftClient;
import software.amazon.awssdk.services.redshift.model.GetClusterCredentialsRequest;
import software.amazon.awssdk.services.redshift.model.GetClusterCredentialsResponse;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;
import java.util.logging.Logger;

@CloudStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AWSClusterCredentialsServiceImpl implements AWSClusterCredentialsService {
  private static final Logger LOGGER = Logger.getLogger(AWSClusterCredentialsServiceImpl.class.getName());
  
  @EJB
  private Settings settings;
  @EJB
  private AssumeRoleService assumeRoleService;
  
  @Override
  public boolean isAWSCloud() {
    return settings.getCloudType().equals(Settings.CLOUD_TYPES.AWS);
  }
  
  @Override
  public AWSClusterCredentials getClusterCredential(String roleARN, String roleSessionName, int durationSeconds,
    String clusterIdentifier, Boolean autoCreate, String dbName, String dbUser, String[] dbGroups,
    int dbDurationSeconds) throws CloudException {
    GetClusterCredentialsResponse getClusterCredentialsResponse;
    if (isAWSCloud()) {
      RedshiftClient client = null;
      try {
        if (!Strings.isNullOrEmpty(roleARN)) {
          AwsSessionCredentials awsSessionCredentials = assumeRoleService.assumeRole(roleARN,
            roleSessionName, durationSeconds);
          AwsCredentialsProvider awsCredentialsProvider = () -> awsSessionCredentials;
          client = RedshiftClient.builder().credentialsProvider(awsCredentialsProvider).build();
        } else {
          // use instance role
          client = RedshiftClient.create();
        }
        GetClusterCredentialsRequest getClusterCredentialsRequest = buildClusterCredentialsRequest(clusterIdentifier,
          autoCreate, dbName, dbUser, dbGroups, dbDurationSeconds);
        getClusterCredentialsResponse = client.getClusterCredentials(getClusterCredentialsRequest);
      } catch (Exception e) {
        throw new CloudException(RESTCodes.CloudErrorCode.FAILED_TO_GET_CLUSTER_CRED, Level.WARNING, e.getMessage());
      } finally {
        if (client != null) {
          client.close();
        }
      }
    } else {
      throw new CloudException(RESTCodes.CloudErrorCode.CLOUD_FEATURE, Level.FINE, "This method is not allowed in a " +
        "non AWS installation. Failed to get Cluster Credential: " + AWSSecurityTokenService.hideId(roleARN));
    }
    return new AWSClusterCredentials(getClusterCredentialsResponse.dbUser(), getClusterCredentialsResponse.dbPassword(),
      getClusterCredentialsResponse.expiration());
  }
  
  private GetClusterCredentialsRequest buildClusterCredentialsRequest(String clusterIdentifier, Boolean autoCreate,
    String dbName, String dbUser, String[] dbGroups, int dbDurationSeconds) {
    GetClusterCredentialsRequest.Builder builder = GetClusterCredentialsRequest.builder()
      .clusterIdentifier(clusterIdentifier)
      .autoCreate(autoCreate != null ? autoCreate : false);
    if (!Strings.isNullOrEmpty(dbName)) {
      builder.dbName(dbName);
    }
    if (!Strings.isNullOrEmpty(dbUser)) {
      builder.dbUser(dbUser);
    }
    if (dbGroups != null && dbGroups.length > 0) {
      builder.dbGroups(dbGroups);
    }
    if (dbDurationSeconds > 0) {
      builder.durationSeconds(dbDurationSeconds);
    }
    return builder.build();
  }
}
