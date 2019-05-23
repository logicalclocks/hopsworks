/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.auth.api.oauth2;

import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.remote.user.auth.oauth2.OAuthClientBuilder;
import io.hops.hopsworks.remote.user.auth.oauth2.OAuthController;
import io.hops.hopsworks.remote.user.auth.oauth2.OIDAuthorizationCodeFlowHelper;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

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
  public Response findAll(@Context UriInfo uriInfo) {
    OAuthClientDTO oAuthClientDTO = oAuthClientBuilder.buildItems(uriInfo);
    return Response.ok().entity(oAuthClientDTO).build();
  }
  
  @GET
  @Path("/client/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response findById(@PathParam("id") Integer id, @Context UriInfo uriInfo) {
    OAuthClientDTO oAuthClientDTO = oAuthClientBuilder.buildItem(uriInfo, id);
    return Response.ok().entity(oAuthClientDTO).build();
  }
  
  @GET
  @Path("/provider/{providerName}/login/uri")
  public Response getLoginURLByProvider(@PathParam("providerName") String providerName, @Context UriInfo uriInfo)
    throws URISyntaxException, UnsupportedEncodingException {
    if (!settings.isOAuthEnabled()) {
      return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }
    URI url;
    try {
      url = oidAuthorizationCodeFlowHelper.getAuthenticationRequestURL(providerName);
    } catch (Exception e) {
      return Response.temporaryRedirect(oAuthController.getErrorUrl(uriInfo, e)).build();
    }
    return Response.temporaryRedirect(url).build();
  }
  
}
