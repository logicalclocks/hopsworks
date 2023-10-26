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
 *
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.multiregion;

import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.Utilities;
import io.hops.hopsworks.servicediscovery.tags.GlassfishTags;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Provider
@PreMatching
@Priority(Priorities.USER)
public class MultiRegionFilter implements ContainerRequestFilter {

  @Inject
  private MultiRegionController multiRegionController;
  @Inject
  private MultiRegionConfiguration multiRegionConfiguration;

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    if (!multiRegionController.isEnabled()) {
      // multiregion controller is not enabled, we should skip the check in this filter
      return;
    }

    if (!containerRequestContext.getRequest().getMethod().equalsIgnoreCase("GET") &&
        multiRegionController.blockSecondaryOperation()) {
      // the request is not a get operation - meaning someone is trying to write something.
      // we should only allow it if we are the primary region
      URI requestURI = containerRequestContext.getUriInfo().getRequestUri();
      URI forwardURI = null;
      try {
        int port = requestURI.getPort();
        if (port < 0) {
          if (requestURI.getScheme().equals("https")) {
            port = 443;
          } else {
            port = 80;
          }
        }
        forwardURI = new URI(requestURI.getScheme(),
            Utilities.constructServiceFQDNWithRegion(
                HopsworksService.GLASSFISH.getNameWithTag(GlassfishTags.hopsworks),
                multiRegionController.getPrimaryRegionName(),
                multiRegionConfiguration.getString(
                    MultiRegionConfiguration.MultiRegionConfKeys.SERVICE_DISCOVERY_DOMAIN)) + ":" + port,
            requestURI.getPath(), requestURI.getQuery(), requestURI.getFragment());
      } catch (URISyntaxException e) {
        throw new IOException(e);
      }

      containerRequestContext.abortWith(Response.temporaryRedirect(forwardURI).build());
    }
  }
}
