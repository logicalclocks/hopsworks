/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.servicediscovery;

public class Utilities {
  private static final String CONSUL_SERVICE_TEMPLATE = "%s.service.%s";
  private static final String CONSUL_SERVICE_REGION_TEMPLATE = "%s.service.%s.%s";

  public static String constructServiceFQDN(String serviceDomain,
                                            String serviceDiscoveryDomain) {
    if (serviceDomain.endsWith(".")) {
      serviceDomain = serviceDomain.substring(0, serviceDomain.length() - 1);
    }
    return String.format(CONSUL_SERVICE_TEMPLATE, serviceDomain, serviceDiscoveryDomain);
  }

  public static String constructServiceFQDNWithRegion(String serviceDomain,
                                                      String region,
                                                      String serviceDiscoveryDomain) {
    if (serviceDomain.endsWith(".")) {
      serviceDomain = serviceDomain.substring(0, serviceDomain.length() - 1);
    }
    return String.format(CONSUL_SERVICE_REGION_TEMPLATE, serviceDomain, region, serviceDiscoveryDomain);
  }
}
