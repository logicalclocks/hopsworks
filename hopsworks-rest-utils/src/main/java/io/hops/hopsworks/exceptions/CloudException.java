/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.exceptions;

import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTException;

import java.util.logging.Level;

public class CloudException extends RESTException {
  
  public CloudException(RESTCodes.CloudErrorCode errorCode, Level level) {
    super(errorCode, level);
  }
  
  public CloudException(RESTCodes.CloudErrorCode errorCode, Level level, String usrMsg) {
    super(errorCode, level, usrMsg);
  }
  
  protected CloudException(RESTCodes.CloudErrorCode errorCode, Level level, String usrMsg, String devMsg) {
    super(errorCode, level, usrMsg, devMsg);
  }
  
  protected CloudException(RESTCodes.CloudErrorCode errorCode, Level level, String usrMsg, String devMsg,
    Throwable throwable) {
    super(errorCode, level, usrMsg, devMsg, throwable);
  }
}
