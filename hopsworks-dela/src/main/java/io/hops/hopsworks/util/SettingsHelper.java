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

package io.hops.hopsworks.util;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dela.AddressJSON;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.exception.DelaException;

import java.util.logging.Level;

public class SettingsHelper {

  public static AddressJSON delaTransferEndpoint(Settings settings) throws DelaException {
    AddressJSON delaTransferEndpoint = settings.getDELA_PUBLIC_ENDPOINT();
    if (delaTransferEndpoint == null) {
      throw new DelaException(RESTCodes.DelaErrorCode.MISCONFIGURED, Level.FINE, DelaException.Source.SETTINGS,
        "DELA_TRANSFER_ENDPOINT");
    }
    return delaTransferEndpoint;
  }

  public static String delaTransferHttpEndpoint(Settings settings) throws DelaException {
    String delaTransferHttpEndpoint = settings.getDELA_TRANSFER_HTTP_ENDPOINT();
    if (delaTransferHttpEndpoint == null) {
      throw new DelaException(RESTCodes.DelaErrorCode.MISCONFIGURED, Level.FINE, DelaException.Source.SETTINGS,
        "DELA_TRANSFER_HTTP_ENDPOINT");
    }
    return delaTransferHttpEndpoint;
  }

  public static String delaHttpEndpoint(Settings settings) throws DelaException {
    String delaHttpEndpoint = settings.getDELA_SEARCH_ENDPOINT();
    if (delaHttpEndpoint == null) {
      throw new DelaException(RESTCodes.DelaErrorCode.MISCONFIGURED, Level.FINE, DelaException.Source.SETTINGS,
        "DELA_HTTP_ENDPOINT");
    }
    return delaHttpEndpoint;
  }

  public static String clusterId(Settings settings) throws DelaException {
    String clusterId = settings.getDELA_CLUSTER_ID();
    if (clusterId == null) {
      throw new DelaException(RESTCodes.DelaErrorCode.MISCONFIGURED, Level.FINE, DelaException.Source.SETTINGS,
        "DELA_CLUSTER_ID");
    }
    return clusterId;
  }

  public static String hopsSite(Settings settings) throws DelaException {
    String hopsSite = settings.getHOPSSITE();
    if (hopsSite == null) {
      throw new DelaException(RESTCodes.DelaErrorCode.MISCONFIGURED, Level.FINE, DelaException.Source.SETTINGS,
        "DELA_HOPS_SITE");
    }
    return hopsSite;
  }

  public static String hopsSiteHost(Settings settings) throws DelaException {
    String hopsSiteHost = settings.getHOPSSITE_HOST();
    if (hopsSiteHost == null) {
      throw new DelaException(RESTCodes.DelaErrorCode.MISCONFIGURED, Level.FINE, DelaException.Source.SETTINGS,
        "DELA_HOPS_SITE_HOST");
    }
    return hopsSiteHost;
  }
  
  public static Users getUser(UserFacade userFacade, String email) throws DelaException {
    Users user = userFacade.findByEmail(email);
    if (user == null) {
      throw new DelaException(RESTCodes.DelaErrorCode.USER_NOT_FOUND, Level.FINE, DelaException.Source.LOCAL);
    }
    return user;
  }
}
