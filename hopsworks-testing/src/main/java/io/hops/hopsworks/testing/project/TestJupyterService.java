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
package io.hops.hopsworks.testing.project;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.jupyter.JupyterNotebookCleaner;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@Path("/test/jupyter")
@Stateless
@JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.TEXT_PLAIN)
@Api(value = "Jupyter Testing Service", description = "Jupyter Testing Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TestJupyterService {
  private static final Logger LOGGER = Logger.getLogger(TestJupyterService.class.getName());
  @EJB
  private JupyterNotebookCleaner jupyterNotebookCleaner;

  @GET
  @Path("/cleanup")
  public Response doCleanup() {
    jupyterNotebookCleaner.doCleanup();
    return Response.ok("Cleaned").build();
  }
}
