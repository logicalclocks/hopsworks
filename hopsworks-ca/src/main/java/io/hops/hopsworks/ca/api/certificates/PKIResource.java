/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.ca.api.certificates;

import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.ca.api.filter.Audience;
import io.hops.hopsworks.ca.api.filter.NoCacheResponse;
import io.hops.hopsworks.ca.controllers.PKI;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/pki")
@RequestScoped
@JWTRequired(acceptedTokens={Audience.SERVICES, Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
@TransactionAttribute(TransactionAttributeType.NEVER)
public class PKIResource {

  private static final Logger LOGGER = Logger.getLogger(PKIResource.class.getName());
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private PKI pki;

  @PUT
  @Path("/configuration")
  @ApiKeyRequired(acceptedScopes = {ApiScope.AUTH}, allowedUserRoles = {"AGENT"})
  public Response reconfigure() {
    LOGGER.log(Level.INFO, "Loading PKI configuration");
    pki.configure();
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }
}
