/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.aws.cloud.security;

import io.hops.hopsworks.common.cloud.AWSSecurityTokenService;
import io.hops.hopsworks.common.cloud.Credentials;
import io.hops.hopsworks.common.integrations.CloudStereotype;
import io.hops.hopsworks.exceptions.CloudException;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Logger;

@CloudStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AWSSecurityTokenServiceImpl implements AWSSecurityTokenService {
  private static final Logger LOGGER = Logger.getLogger(AWSSecurityTokenServiceImpl.class.getName());

  @EJB
  private AssumeRoleService assumeRoleService;
  
  @Override
  public boolean isAWSCloud() {
    return assumeRoleService.isAWSCloud();
  }
  
  @Override
  public Credentials assumeRole(String roleARN, String roleSessionName, int durationSeconds) throws CloudException {
    AwsSessionCredentials awsSessionCredentials = assumeRoleService.assumeRole(roleARN, roleSessionName,
      durationSeconds);
    return new Credentials(awsSessionCredentials.accessKeyId(), awsSessionCredentials.secretAccessKey(),
      awsSessionCredentials.sessionToken());
  }
}
