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

import org.json.JSONObject;

public abstract class RESTException extends Exception {
  
  //Optional messages
  private final String usrMsg;
  private final String devMsg;
  private final RESTCodes.RESTErrorCode errorCode;
  
  RESTException() {
    this(null, null, null);
  }
  
  RESTException(RESTCodes.RESTErrorCode errorCode) {
    this(errorCode, null, null);
  }
  
  RESTException(RESTCodes.RESTErrorCode errorCode, String usrMsg) {
    this(errorCode, usrMsg, null);
  }
  
  RESTException(RESTCodes.RESTErrorCode errorCode, String usrMsg, String devMsg) {
    this.errorCode = errorCode;
    this.usrMsg = usrMsg;
    this.devMsg = devMsg;
  }
  
  RESTException(RESTCodes.RESTErrorCode errorCode, String usrMsg, String devMsg, Throwable throwable) {
    super(throwable);
    this.errorCode = errorCode;
    this.usrMsg = usrMsg;
    this.devMsg = devMsg;
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
  
  public JSONObject getJson() {
    return new JSONObject()
      .put("code", errorCode.getCode())
      .put("message", errorCode.getMessage())
      .put("usrMsg", usrMsg)
      .put("devMsg", devMsg);
  }
}
