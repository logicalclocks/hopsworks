/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.testing.provenance.TestProvenanceResource;
import io.swagger.annotations.Api;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/test/project")
@Stateless
@JWTRequired(acceptedTokens = {Audience.API},
  allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Project Testing Service", description = "Project Testing Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TestProjectService {
  @Inject
  private TestProvenanceResource provenance;

  @Path("{projectId}/provenance")
  public TestProvenanceResource provenance(@PathParam("projectId") Integer id) {
    this.provenance.setProjectId(id);
    return provenance;
  }
}