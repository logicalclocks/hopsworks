/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.oauth2;

import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.remote.user.api.Audience;
import io.hops.hopsworks.remote.user.oauth2.OAuthController;
import io.hops.hopsworks.remote.user.oauth2.OIDAuthorizationCodeFlowHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

@Path("/oauth")
@Stateless
@Api(value = "OAuth2",
  description = "OAuth2 Resource")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OAuthClientResource {
  
  @EJB
  private OAuthClientBuilder oAuthClientBuilder;
  @EJB
  private OIDAuthorizationCodeFlowHelper oidAuthorizationCodeFlowHelper;
  @EJB
  private OAuthController oAuthController;
  @EJB
  private Settings settings;
  
  @GET
  @Path("/client")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response findAll(@Context UriInfo uriInfo) {
    OAuthClientDTO oAuthClientDTO = oAuthClientBuilder.buildItems(uriInfo);
    return Response.ok().entity(oAuthClientDTO).build();
  }
  
  @GET
  @Path("/client/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response findById(@PathParam("id") Integer id, @Context UriInfo uriInfo) {
    OAuthClientDTO oAuthClientDTO = oAuthClientBuilder.buildItem(uriInfo, id);
    return Response.ok().entity(oAuthClientDTO).build();
  }
  
  @GET
  @Path("/provider/{providerName}/login/uri")
  @ApiOperation(value = "Get login URL for the given provider name")
  public Response getLoginURLByProvider(@PathParam("providerName") String providerName, @Context UriInfo uriInfo,
    @QueryParam("redirect") @DefaultValue("true") boolean redirect,
    @Context HttpServletRequest req) throws URISyntaxException, UnsupportedEncodingException {
    URI redirectURI = new URI(settings.getOauthRedirectUri());
    return getAuthURI(providerName, redirectURI, null, redirect, req.getSession().getId(),uriInfo);
  }
  
  private Response getAuthURI(String providerName, URI redirectURI, Set<String> scopes, boolean redirect,
    String sessionId, UriInfo uriInfo)
    throws UnsupportedEncodingException, URISyntaxException {
    if (!settings.isOAuthEnabled()) {
      return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }
    URI url;
    try {
      url = oidAuthorizationCodeFlowHelper.getAuthenticationRequestURL(sessionId, providerName, redirectURI, scopes);
    } catch (Exception e) {
      if (redirect) {
        return Response.temporaryRedirect(oAuthController.getErrorUrl(uriInfo, e)).build();
      } else {
        return Response.ok(oAuthController.getErrorUrl(uriInfo, e)).build();
      }
    }
    if (redirect) {
      return Response.temporaryRedirect(url).build();
    } else {
      return Response.ok(url).build();
    }
  }
  
}
