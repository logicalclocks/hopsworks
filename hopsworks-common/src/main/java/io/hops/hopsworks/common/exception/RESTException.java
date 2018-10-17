/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.exception;

import io.hops.hopsworks.common.util.JsonResponse;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Serializable;
import java.util.logging.Level;

public abstract class RESTException extends Exception implements Serializable {
  //Optional messages
  private final String usrMsg;
  private final String devMsg;
  private final RESTCodes.RESTErrorCode errorCode;
  
  //Logging level for app server
  private final Level level;
  
  protected RESTException(RESTCodes.RESTErrorCode errorCode, Level level) {
    this(errorCode, level, null, null);
  }
  
  protected RESTException(RESTCodes.RESTErrorCode errorCode, Level level, String usrMsg) {
    this(errorCode, level, usrMsg, null);
  }
  
  protected RESTException(RESTCodes.RESTErrorCode errorCode, Level level, String usrMsg, String devMsg) {
    this(errorCode, level, usrMsg, devMsg, null);
  }
  
  protected RESTException(RESTCodes.RESTErrorCode errorCode, Level level, String usrMsg, String devMsg,
    Throwable throwable) {
    super(throwable);
    this.errorCode = errorCode;
    this.usrMsg = usrMsg;
    this.devMsg = devMsg;
    this.level = level;
  }
  
  public String getUsrMsg() {
    return usrMsg;
  }
  
  public String getDevMsg() {
    return devMsg;
  }
  
  public RESTCodes.RESTErrorCode getErrorCode() {
    return errorCode;
  }
  
  /**
   * @return Logging level for application server.
   */
  public Level getLevel() {
    return level;
  }
  
  public JsonResponse buildJsonResponse(JsonResponse jsonResponse, Settings.LOG_LEVEL logLevel) {
    
    if (jsonResponse == null) {
      throw new IllegalArgumentException("jsonResponse was not provided.");
    }
    jsonResponse.setErrorMsg(errorCode.getMessage());
    jsonResponse.setErrorCode(errorCode.getCode());
    
    if (logLevel.getLevel() <= Settings.LOG_LEVEL.PROD.getLevel()) {
      jsonResponse.setUsrMsg(usrMsg);
    }
    if (logLevel.getLevel() <= Settings.LOG_LEVEL.TEST.getLevel()) {
      jsonResponse.setDevMsg(devMsg);
    }
    if (logLevel.getLevel() <= Settings.LOG_LEVEL.DEV.getLevel()) {
      jsonResponse.setTrace(ExceptionUtils.getStackTrace(this));
    }
    return jsonResponse;
  }
  
}
