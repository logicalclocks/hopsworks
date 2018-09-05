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

package io.hops.hopsworks.common.serving.inference;

import javax.ws.rs.core.Response;

public class InferenceException extends Exception {

  public enum InferenceExceptionErrors {
    SERVINGNOTFOUND(80, "Serving instance not found", Response.Status.NOT_FOUND),
    SERVINGNOTRUNNING(81, "Serving instance not running", Response.Status.BAD_REQUEST),
    REQUESTERROR(82, "Error contacting the serving server", Response.Status.INTERNAL_SERVER_ERROR),
    EMPTYRESPONSE(83, "Empty response from the serving server", Response.Status.INTERNAL_SERVER_ERROR),
    BADREQUEST(84, "Request malformed", Response.Status.BAD_REQUEST);

    private final int errorCode;
    private final String message;
    private final Response.Status httpStatusCode;

    InferenceExceptionErrors(int errorCode, String message, Response.Status httpStatusCode) {
      this.errorCode = errorCode;
      this.message = message;
      this.httpStatusCode = httpStatusCode;
    }

    public int getErrorCode() {
      return errorCode;
    }

    public Response.Status getHttpStatusCode() {
      return httpStatusCode;
    }

    public String getMessage() {
      return message;
    }
  }

  private InferenceExceptionErrors error;
  private String userMsg;
  private String devMsg;

  public InferenceException(InferenceExceptionErrors error) {
    this.error = error;
  }

  public InferenceException(InferenceExceptionErrors error, String userMsg) {
    this.error = error;
    this.userMsg = userMsg;
  }

  public InferenceException(InferenceExceptionErrors error, String userMsg, String devMsg) {
    this.error = error;
    this.userMsg = userMsg;
    this.devMsg = devMsg;
  }

  public InferenceExceptionErrors getError() {
    return error;
  }

  public void setError(InferenceExceptionErrors error) {
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
