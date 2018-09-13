/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.security;

import javax.ws.rs.core.Response;

public class DelaCSRCheckException extends Exception {
  public enum DelaCSRCheckErrors {
    BADREQUEST(60, "User or CS not set", Response.Status.BAD_REQUEST),
    EMAIL(61, "CSR email not set or does not match user", Response.Status.UNAUTHORIZED),
    CN(62, "CSR common name not set", Response.Status.BAD_REQUEST),
    O(63, "CSR organization name not set", Response.Status.BAD_REQUEST),
    OU(64, "CSR organization unit name not set", Response.Status.BAD_REQUEST),
    NOTFOUND(65, "No cluster registered with the given organization name and organizational unit",
        Response.Status.BAD_REQUEST),
    SERIALNUMBER(66, "Cluster has already a signed certificate", Response.Status.BAD_REQUEST),
    CNNOTFOUND(67, "No cluster registered with the CSR common name", Response.Status.BAD_REQUEST),
    AGENTIDNOTFOUND(68, "No cluster registered for the user", Response.Status.UNAUTHORIZED);

    private final int errorCode;
    private final String message;
    private final Response.Status httpStatusCode;

    DelaCSRCheckErrors(int errorCode, String message, Response.Status httpStatusCode) {
      this.errorCode = errorCode;
      this.message = message;
      this.httpStatusCode = httpStatusCode;
    }

    public String getMessage() {
      return message;
    }

    public int getErrorCode() {
      return errorCode;
    }

    public Response.Status getHttpStatusCode() {
      return httpStatusCode;
    }
  }

  private DelaCSRCheckErrors error;

  private String userMsg;
  private String devMsg;

  public DelaCSRCheckException(DelaCSRCheckErrors error) {
    this.error = error;
  }

  public DelaCSRCheckException(DelaCSRCheckErrors error, String userMsg) {
    this.error = error;
    this.userMsg = userMsg;
  }

  public DelaCSRCheckException(DelaCSRCheckErrors error, String userMsg, String devMsg) {
    this.error = error;
    this.userMsg = userMsg;
    this.devMsg = devMsg;
  }

  public DelaCSRCheckErrors getError() {
    return error;
  }

  public void setError(DelaCSRCheckErrors error) {
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
