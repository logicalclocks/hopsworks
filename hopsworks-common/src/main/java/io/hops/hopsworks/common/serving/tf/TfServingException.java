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

package io.hops.hopsworks.common.serving.tf;

import javax.ws.rs.core.Response;

public class TfServingException extends Exception {

  public enum TfServingExceptionErrors {
    INSTANCENOTFOUND(70, "TFServing instance not found", Response.Status.NOT_FOUND),
    DELETIONERROR(71, "TFServing instance could not be deleted", Response.Status.INTERNAL_SERVER_ERROR),
    UPDATEERROR(72, "TFServing instance could not be updated", Response.Status.INTERNAL_SERVER_ERROR),
    LIFECYCLEERROR(73, "TFServing instance could not be started/stopped", Response.Status.BAD_REQUEST),
    LIFECYCLEERRORINT(74, "TFServing instance could not be started/stopped", Response.Status.INTERNAL_SERVER_ERROR),
    STATUSERROR(75, "Error getting TFServing instance status", Response.Status.INTERNAL_SERVER_ERROR),
    PATHNOTFOUND(76, "Model Path not found", Response.Status.BAD_REQUEST),
    COMMANDNOTRECOGNIZED(77, "Command not recognized", Response.Status.BAD_REQUEST),
    COMMANDNOTPROVIDED(78, "Command not provided", Response.Status.BAD_REQUEST),
    SPECNOTPROVIDED(78, "TFServing spec not provided", Response.Status.BAD_REQUEST);


    private final int errorCode;
    private final String message;
    private final Response.Status httpStatusCode;

    TfServingExceptionErrors(int errorCode, String message, Response.Status httpStatusCode) {
      this.errorCode = errorCode;
      this.message = message;
      this.httpStatusCode = httpStatusCode;
    }

    public int getErrorCode() {
      return errorCode;
    }

    public String getMessage() {
      return message;
    }

    public Response.Status getHttpStatusCode() {
      return httpStatusCode;
    }
  }

  private TfServingExceptionErrors error;
  private String userMsg;
  private String devMsg;

  public TfServingException(TfServingExceptionErrors error) {
    this.error = error;
  }

  public TfServingException(TfServingExceptionErrors error, String userMsg) {
    this.error = error;
    this.userMsg = userMsg;
  }

  public TfServingException(TfServingExceptionErrors error, String userMsg, String devMsg) {
    this.error = error;
    this.userMsg = userMsg;
    this.devMsg = devMsg;
  }

  public TfServingExceptionErrors getError() {
    return error;
  }

  public void setError(TfServingExceptionErrors error) {
    this.error = error;
  }

  public String getUserMsg() {
    return userMsg;
  }

  public void setUserMsg(String userMsg) {
    this.userMsg = userMsg;
  }

  public String getDevMsg() {
    return devMsg;
  }

  public void setDevMsg(String devMsg) {
    this.devMsg = devMsg;
  }
}
