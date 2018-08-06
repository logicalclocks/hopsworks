/*
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
 *
 */
package io.hops.hopsworks.util;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dela.AddressJSON;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.exception.ThirdPartyException.Source;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;

public class SettingsHelper {

  public static AddressJSON delaTransferEndpoint(Settings settings) throws ThirdPartyException {
    AddressJSON delaTransferEndpoint = settings.getDELA_PUBLIC_ENDPOINT();
    if (delaTransferEndpoint == null) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "DELA_TRANSFER_ENDPOINT",
          ThirdPartyException.Source.SETTINGS, "misconfigured");
    }
    return delaTransferEndpoint;
  }

  public static String delaTransferHttpEndpoint(Settings settings) throws ThirdPartyException {
    String delaTransferHttpEndpoint = settings.getDELA_TRANSFER_HTTP_ENDPOINT();
    if (delaTransferHttpEndpoint == null) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "DELA_TRANSFER_HTTP_ENDPOINT",
          ThirdPartyException.Source.SETTINGS, "misconfigured");
    }
    return delaTransferHttpEndpoint;
  }

  public static String delaHttpEndpoint(Settings settings) throws ThirdPartyException {
    String delaHttpEndpoint = settings.getDELA_SEARCH_ENDPOINT();
    if (delaHttpEndpoint == null) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "DELA_HTTP_ENDPOINT",
          ThirdPartyException.Source.SETTINGS, "misconfigured");
    }
    return delaHttpEndpoint;
  }

  public static String clusterId(Settings settings) throws ThirdPartyException {
    String clusterId = settings.getDELA_CLUSTER_ID();
    if (clusterId == null) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "DELA_CLUSTER_ID",
          ThirdPartyException.Source.SETTINGS, "misconfigured");
    }
    return clusterId;
  }

  public static String hopsSite(Settings settings) throws ThirdPartyException {
    String hopsSite = settings.getHOPSSITE();
    if (hopsSite == null) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "DELA_HOPS_SITE",
          ThirdPartyException.Source.SETTINGS, "misconfigured");
    }
    return hopsSite;
  }

  public static String hopsSiteHost(Settings settings) throws ThirdPartyException {
    String hopsSiteHost = settings.getHOPSSITE_HOST();
    if (hopsSiteHost == null) {
      throw new ThirdPartyException(Response.Status.EXPECTATION_FAILED.getStatusCode(), "DELA_HOPS_SITE_HOST",
          ThirdPartyException.Source.SETTINGS, "misconfigured");
    }
    return hopsSiteHost;
  }

  public static Users getUser(UserFacade userFacade, String email) throws ThirdPartyException {
    Users user;
    try {
      user = userFacade.findByEmail(email);
    } catch (AppException ex) {
      Logger.getLogger(SettingsHelper.class.getName()).log(Level.SEVERE, null, ex);
      throw new ThirdPartyException(ex.getStatus(), "Database not accessible", Source.MYSQL, "Database problems");
    }
    if (user == null) {
      throw new ThirdPartyException(Response.Status.FORBIDDEN.getStatusCode(), "user not found",
          ThirdPartyException.Source.LOCAL, "exception");
    }
    return user;
  }
}
