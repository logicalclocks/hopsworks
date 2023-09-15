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

import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Priority(Priorities.AUTHENTICATION - 1)
public class MultiRegionFilter implements ContainerRequestFilter {

  @EJB
  private MultiRegionController multiRegionController;

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
      containerRequestContext.abortWith(Response.status(Response.Status.FORBIDDEN).build());
    }
  }
}
