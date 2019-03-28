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

package io.hops.hopsworks.ca.controllers;

import io.hops.hopsworks.ca.api.exception.mapper.CAJsonResponse;
import io.hops.hopsworks.restutils.JsonResponse;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTException;
import io.hops.hopsworks.restutils.RESTLogLevel;

import java.util.logging.Level;

public class CAException extends RESTException {
  
  private final CertificateType certType;
  
  public CAException(RESTCodes.CAErrorCode code, Level level, CertificateType certType) {
    this(code, level, certType, null);
  }
  
  public CAException(RESTCodes.CAErrorCode code, Level level, CertificateType certType, String usrMsg) {
    this(code, level, certType, usrMsg, null);
  }
  
  public CAException(RESTCodes.CAErrorCode code, Level level, CertificateType certType, String usrMsg, String devMsg) {
    this(code, level, certType, usrMsg, devMsg, null);
  }
  
  public CAException(RESTCodes.CAErrorCode code, Level level, CertificateType certType, String usrMsg, String devMsg,
    Throwable throwable) {
    super(code, level, usrMsg, devMsg, throwable);
    this.certType = certType;
  }

  @Override
  public CAJsonResponse buildJsonResponse(JsonResponse jsonResponse, RESTLogLevel logLevel) {
    CAJsonResponse caJsonResponse = (CAJsonResponse)super.buildJsonResponse(jsonResponse, logLevel);
    caJsonResponse.setCertificateType(certType);
    return caJsonResponse;
  }
  
  @Override
  public String toString() {
    return "{" +
      "certType=" + certType +
      super.toString() +
      '}';
  }
}
