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

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.user.ServiceJWTDTO;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.jwt.exception.DuplicateSigningKeyException;
import io.hops.hopsworks.jwt.exception.InvalidationException;
import io.hops.hopsworks.jwt.exception.JWTException;
import io.hops.hopsworks.jwt.exception.NotRenewableException;
import io.hops.hopsworks.jwt.exception.SigningKeyNotFoundException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/jwt")
@Stateless
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@JWTRequired(acceptedTokens = {Audience.SERVICES}, allowedUserRoles = {"AGENT"})
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "JWT service", description = "Manage application and service JWT")
public class JWTResource {

  private final static Logger LOGGER = Logger.getLogger(JWTResource.class.getName());

  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private Settings settings;

  @POST
  @ApiOperation(value = "Create application token", response = JWTResponseDTO.class)
  public Response createToken(JWTRequestDTO jWTRequestDTO, @Context SecurityContext sc) throws NoSuchAlgorithmException,
    SigningKeyNotFoundException, DuplicateSigningKeyException {
    JWTResponseDTO jWTResponseDTO = jWTHelper.createToken(jWTRequestDTO, settings.getJWTIssuer());
    return Response.ok().entity(jWTResponseDTO).build();
  }

  @PUT
  @ApiOperation(value = "Renew application token", response = JWTResponseDTO.class)
  public Response renewToken(JsonWebTokenDTO jsonWebTokenDTO, @Context SecurityContext sc)
    throws SigningKeyNotFoundException, NotRenewableException, InvalidationException {
    JWTResponseDTO jWTResponseDTO = jWTHelper.renewToken(jsonWebTokenDTO, true, new HashMap<>(3));
    return Response.ok().entity(jWTResponseDTO).build();
  }
  
  @DELETE
  @Path("/{token}")
  @ApiOperation(value = "Invalidate application token")
  public Response invalidateToken(
      @ApiParam(value = "Token to invalidate", required = true)
      @PathParam("token") String token, @Context SecurityContext sc) throws InvalidationException {
    jWTHelper.invalidateToken(token);
    return Response.ok().build();
  }
  
  @DELETE
  @Path("/key/{keyName}")
  @ApiOperation(value = "Delete a JWT signing key")
  public Response removeSigingKey(
      @ApiParam(value = "Name of the signing key to remove", required = true)
      @PathParam("keyName") String keyName, @Context SecurityContext sc) {
    jWTHelper.deleteSigningKeyByName(keyName);
    return Response.ok().build();
  }

  @PUT
  @Path("/service")
  @ApiOperation(value = "Renew a service JWT without invalidating the previous token", response = ServiceJWTDTO.class)
  public Response renewServiceToken(JsonWebTokenDTO jwt, @Context HttpServletRequest request)
      throws HopsSecurityException {
    // This token should be the one-time renewal token
    String token = jWTHelper.getAuthToken(request);
    Users user = jWTHelper.getUserPrincipal(request);
    if (user == null) {
      DecodedJWT decodedJWT = JWT.decode(token);
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.NOT_RENEWABLE_TOKEN, Level.FINE,
          "User not found associated with that JWT", "Could not find user in the database associated with JWT "
          + decodedJWT.getId());
    }
    try {
      ServiceJWTDTO renewedTokens = jWTHelper.renewServiceToken(jwt, token, user, request.getRemoteHost());
      return Response.ok().entity(renewedTokens).build();
    } catch (JWTException | NoSuchAlgorithmException ex) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.NOT_RENEWABLE_TOKEN, Level.WARNING,
          "Could not renew service JWT", "Could not renew service JWT for " + request.getRemoteHost());
    }
  }
  
  @DELETE
  @Path("/service/{token}")
  @ApiOperation(value = "Invalidate a service JWT and also delete the signing key encoded in the token")
  public Response invalidateServiceToken(
      @ApiParam(value = "Service token to invalidate", required = true)
      @PathParam("token") String token, @Context HttpServletRequest request)
    throws HopsSecurityException {
    DecodedJWT jwt2invalidate = JWT.decode(token);
    
    Users user = jWTHelper.getUserPrincipal(request);
    if (user == null) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.INVALIDATION_ERROR, Level.FINE,
          "Could not find registered user", "Could not find registered user associated with JWT "
            + jwt2invalidate.getId());
    }
    if (!user.getUsername().equals(jwt2invalidate.getSubject())) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.INVALIDATION_ERROR, Level.FINE,
          "Tried to invalidate token with different subject",
          "User " + user.getUsername() + " tried to invalidate token with Subject " + jwt2invalidate.getSubject());
    }
    
    jWTHelper.invalidateServiceToken(token);
    
    return Response.ok().build();
  }
  
  @GET
  @Path("/elk/key")
  @ApiOperation(value = "Get the signing key for ELK if exists otherwise " +
      "create a new one and return")
  public Response getSigningKeyforELK(@Context SecurityContext sc) throws ElasticException {
    String signingKey = jWTHelper.getSigningKeyForELK();
    return Response.ok().entity(signingKey).build();
  }
  
  @GET
  @Path("/elk/token/{projectId}")
  @ApiOperation(value = "Create elastic jwt token for the provided project as" +
      " Data Owner.")
  public Response createELKTokenAsDataOwner(@PathParam(
      "projectId") Integer projectId, @Context SecurityContext sc) throws ElasticException {
    if (projectId == null) {
      throw new IllegalArgumentException("projectId was not provided.");
    }
    ElasticJWTResponseDTO
        jWTResponseDTO = jWTHelper.createTokenForELKAsDataOwner(projectId);
    return Response.ok().entity(jWTResponseDTO).build();
  }
}
