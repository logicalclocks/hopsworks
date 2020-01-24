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
package io.hops.hopsworks.testing.provenance;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.provenance.core.ProvenanceCleanerController;
import io.hops.hopsworks.common.provenance.util.dto.WrapperDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import org.javatuples.Pair;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/test/provenance")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Provenance Service", description = "Provenance Service")
public class TestProvenanceService {
  @EJB
  private Settings settings;
  @EJB
  private ProvenanceCleanerController cleanerCtrl;
  
  @POST
  @Path("/cleanup")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response cleanup(
    @QueryParam("size") Integer size) throws ProvenanceException {
    if(size == null) {
      size = settings.getProvCleanupSize();
    }
    Pair<Integer, String> result = cleanerCtrl.indexCleanupRound("", size);
    return Response.ok().entity(new WrapperDTO(result.getValue0())).build();
  }
}
