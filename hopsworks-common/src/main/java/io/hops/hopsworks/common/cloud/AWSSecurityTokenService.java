/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.common.cloud;

import io.hops.hopsworks.exceptions.CloudException;

public interface AWSSecurityTokenService {
  boolean isAWSCloud();
  Credentials assumeRole(String roleARN, String roleSessionName, int durationSeconds) throws CloudException;
  
  static public String hideId(String roleARN) {
    if (roleARN.startsWith("arn:aws:iam::")) {
      int index = roleARN.lastIndexOf(":");
      return "arn:aws:iam::xxxxxxxxxxxx" + roleARN.substring(index);
    }
    return "";
  }
}
