/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.common.cloud;

import io.hops.hopsworks.exceptions.CloudException;

public interface AWSClusterCredentialsService {
  boolean isAWSCloud();
  
  AWSClusterCredentials getClusterCredential(String roleARN, String roleSessionName, int durationSeconds,
    String clusterIdentifier, Boolean autoCreate, String dbName, String dbUser, String[] dbGroups,
    int dbDurationSeconds) throws CloudException;
}
