/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.exception;

import javax.ws.rs.core.Response;

public class AppException extends Exception {

  /**
   * contains the HTTP status of the response sent back to the
   * client in case of error,
   */
  Integer status;

  /**
   * Constructs an instance of <code>AppException</code> with the specified
   * detail message.
   *
   * @param status HTTP status
   * @param msg the detail message.
   */
  public AppException(int status, String msg) {
    super(msg);
    this.status = status;
  }

  public AppException(Response.Status status, String msg) {
    this(status.getStatusCode(), msg);
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

}
