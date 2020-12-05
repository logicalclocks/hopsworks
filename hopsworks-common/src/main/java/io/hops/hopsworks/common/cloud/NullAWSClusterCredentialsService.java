/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.common.cloud;

import io.hops.hopsworks.common.integrations.OnPremiseStereotype;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.logging.Level;

@OnPremiseStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class NullAWSClusterCredentialsService implements AWSClusterCredentialsService{
  @Override
  public boolean isAWSCloud() {
    return false;
  }
  
  @Override
  public AWSClusterCredentials getClusterCredential(String roleARN, String roleSessionName, int durationSeconds,
    String clusterIdentifier, Boolean autoCreate, String dbName, String dbUser, String[] dbGroups,
    int dbDurationSeconds) throws CloudException {
    throw new CloudException(RESTCodes.CloudErrorCode.CLOUD_FEATURE, Level.FINE, "This method is not allowed in a " +
      "non AWS installation. Failed to assume role: " + AWSSecurityTokenService.hideId(roleARN));
  }
}
