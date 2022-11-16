/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.ldap;

import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.RemoteAuthException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.remote.user.api.Audience;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Level;

@Logged
@Path("/ldap")
@Stateless
@Api(value = "LDAP", description = "LDAP Resource")
@JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LdapConfigResource {
  @EJB
  private LdapConfigBuilder ldapConfigBuilder;
  @EJB
  private Settings settings;
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response find(@Context UriInfo uriInfo, @Context HttpServletRequest req) {
    if (settings.isLdapEnabled()) {
      LdapConfigDTO ldapConfigDTO = ldapConfigBuilder.build(uriInfo);
      return Response.ok().entity(ldapConfigDTO).build();
    }
    return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response update(LdapConfigDTO ldapConfigDTO, @QueryParam("default") DefaultConfig defaultConfig,
    @Context UriInfo uriInfo, @Context HttpServletRequest req) throws RemoteAuthException {
    if (settings.isLdapEnabled()) {
      if (ldapConfigDTO == null) {
        throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
      }
      if (DefaultConfig.MIT_KRB.equals(defaultConfig) || DefaultConfig.ACTIVE_DIRECTORY.equals(defaultConfig)) {
        throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Default not " +
          "applicable.");
      }
      ldapConfigDTO = ldapConfigBuilder.update(ldapConfigDTO, defaultConfig, uriInfo);
      return Response.ok().entity(ldapConfigDTO).build();
    }
    return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
  }
}
