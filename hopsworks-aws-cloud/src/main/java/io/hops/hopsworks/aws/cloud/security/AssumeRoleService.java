/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.aws.cloud.security;

import io.hops.hopsworks.common.cloud.AWSSecurityTokenService;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.restutils.RESTCodes;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AssumeRoleService {
  private static final Logger LOGGER = Logger.getLogger(AWSSecurityTokenServiceImpl.class.getName());
  private StsClient stsClient;
  @EJB
  private Settings settings;
  
  @PostConstruct
  public void init() {
    // InstanceProfile Credentials should be on all payara nodes in HA.
    if (isAWSCloud()) {//Will need a redeploy if variable is changed
      stsClient = StsClient.builder()
        .credentialsProvider(InstanceProfileCredentialsProvider.create())
        .build();
    } else {
      LOGGER.log(Level.INFO, "Not an AWS cloud skipping initialization of STS client.");
    }
  }
  
  @PreDestroy
  public void destroy() {
    if (stsClient != null) {
      stsClient.close();
    }
  }
  
  @Lock(LockType.READ)
  public boolean isAWSCloud() {
    return settings.getCloudType().equals(Settings.CLOUD_TYPES.AWS);
  }
  
  @Lock(LockType.READ)
  public AwsSessionCredentials assumeRole(String roleARN, String roleSessionName, int durationSeconds)
    throws CloudException {
    AwsSessionCredentials awsSessionCredentials;
    if (stsClient != null) {
      try {
        AssumeRoleRequest assumeRoleRequest = buildAssumeRoleRequest(roleARN, roleSessionName, durationSeconds);
        StsAssumeRoleCredentialsProvider stsAssumeRoleCredentialsProvider = StsAssumeRoleCredentialsProvider.builder()
          .refreshRequest(assumeRoleRequest)
          .stsClient(stsClient)
          .build();
        awsSessionCredentials = (AwsSessionCredentials) stsAssumeRoleCredentialsProvider.resolveCredentials();
      } catch (Exception e) {
        throw new CloudException(RESTCodes.CloudErrorCode.FAILED_TO_ASSUME_ROLE, Level.WARNING, e.getMessage());
      }
    } else {
      throw new CloudException(RESTCodes.CloudErrorCode.CLOUD_FEATURE, Level.FINE, "This method is not allowed in a " +
        "non AWS installation. Failed to assume role: " + AWSSecurityTokenService.hideId(roleARN));
    }
    return awsSessionCredentials;
  }
  
  
  private AssumeRoleRequest buildAssumeRoleRequest(String roleARN, String roleSessionName, int durationSeconds) {
    AssumeRoleRequest.Builder assumeRoleRequest = AssumeRoleRequest.builder().roleArn(roleARN);
    if (roleSessionName == null || roleSessionName.isEmpty()) {
      String uniqueID = UUID.randomUUID().toString();
      roleSessionName = "session_" + uniqueID;
    }
    assumeRoleRequest.roleSessionName(roleSessionName);
    if (durationSeconds > 0) {
      assumeRoleRequest.durationSeconds(durationSeconds);
    }
    return assumeRoleRequest.build();
  }
}
