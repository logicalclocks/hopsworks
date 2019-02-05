/*
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
 */
package io.hops.hopsworks.api.jwt;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/jwt")
@Stateless
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@JWTRequired(acceptedTokens = {Audience.SERVICES}, allowedUserRoles = {"AGENT"})
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JWTResource {

  private final static Logger LOGGER = Logger.getLogger(JWTResource.class.getName());

  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private Settings settings;

  @POST
  @Path("/create")
  public Response createToken(JWTRequestDTO jWTRequestDTO) throws NoSuchAlgorithmException, SigningKeyNotFoundException,
      DuplicateSigningKeyException {
    JWTResponseDTO jWTResponseDTO = jWTHelper.createToken(jWTRequestDTO, settings.getJWTIssuer());
    return Response.ok().entity(jWTResponseDTO).build();
  }

  @POST
  @Path("/renew")
  public Response renewToken(JsonWebTokenDTO jsonWebTokenDTO) throws SigningKeyNotFoundException, NotRenewableException,
      InvalidationException {
    JWTResponseDTO jWTResponseDTO = jWTHelper.renewToken(jsonWebTokenDTO);
    return Response.ok().entity(jWTResponseDTO).build();
  }

  @POST
  @Path("/invalidate")
  public Response invalidateToken(JsonWebTokenDTO jsonWebTokenDTO) throws InvalidationException {
    jWTHelper.invalidateToken(jsonWebTokenDTO);
    return Response.ok().build();
  }
  
  @PUT
  @Path("/remove/{keyName}")
  public Response removeSigingKey(@PathParam("keyName") String keyName) {
    jWTHelper.deleteSigningKey(keyName);
    return Response.ok().build();
  }

}
