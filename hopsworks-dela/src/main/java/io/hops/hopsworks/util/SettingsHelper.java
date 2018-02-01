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

package io.hops.hopsworks.util;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dela.AddressJSON;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
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
    Users user = userFacade.findByEmail(email);
    if (user == null) {
      throw new ThirdPartyException(Response.Status.FORBIDDEN.getStatusCode(), "user not found",
        ThirdPartyException.Source.LOCAL, "exception");
    }
    return user;
  }
}
