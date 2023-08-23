/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.oauth2;

import com.google.common.base.Strings;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.dao.remote.oauth.OauthClientFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.RemoteAuthException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthClient;
import io.hops.hopsworks.remote.user.api.Audience;
import io.hops.hopsworks.remote.user.api.AuthResource;
import io.hops.hopsworks.remote.user.oauth2.OAuthController;
import io.hops.hopsworks.remote.user.oauth2.OIDAuthorizationCodeFlowHelper;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged
@Path("/oauth")
@Stateless
@Api(value = "OAuth2",
  description = "OAuth2 Resource")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OAuthClientResource {
  private static final Logger LOGGER = Logger.getLogger(AuthResource.class.getName());
  @EJB
  private OAuthClientBuilder oAuthClientBuilder;
  @EJB
  private OIDAuthorizationCodeFlowHelper oidAuthorizationCodeFlowHelper;
  @EJB
  private OAuthController oAuthController;
  @EJB
  private OauthClientFacade oauthClientFacade;
  @EJB
  private Settings settings;
  
  @GET
  @Path("/client")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response findAll(@Context UriInfo uriInfo, @Context HttpServletRequest req) {
    OAuthClientDTO oAuthClientDTO = oAuthClientBuilder.buildItems(uriInfo);
    return Response.ok().entity(oAuthClientDTO).build();
  }
  
  @GET
  @Path("/client/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response findById(@PathParam("id") Integer id, @Context UriInfo uriInfo, @Context HttpServletRequest req)
    throws RemoteAuthException {
    OAuthClientDTO oAuthClientDTO = oAuthClientBuilder.buildItem(uriInfo, id);
    return Response.ok().entity(oAuthClientDTO).build();
  }
  
  @GET
  @Path("/provider/{providerName}/login/uri")
  @ApiOperation(value = "Get login URL for the given provider name. Returns temporaryRedirect (307)")
  public Response getLoginURLByProvider(@PathParam("providerName") String providerName, @Context UriInfo uriInfo,
    @QueryParam("redirect") @DefaultValue("true") boolean redirect, @Context HttpServletRequest req)
    throws URISyntaxException, UnsupportedEncodingException {
    return getAuthURI(providerName, settings.getOauthRedirectUri(providerName), null, redirect,
      req.getSession().getId(), uriInfo);
  }
  
  @GET
  @Path("/login/uri/{providerName}")
  @ApiOperation(value = "Get login URL for the given provider name", response = OAuthProvider.class)
  public Response getProviderLoginURL(@PathParam("providerName") String providerName,
    @QueryParam("redirect_uri") String redirectUri, @Context UriInfo uriInfo, @Context HttpServletRequest req)
    throws URISyntaxException, RemoteAuthException {
    if (!settings.isOAuthEnabled()) {
      return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }
    URI url = oidAuthorizationCodeFlowHelper.getAuthenticationRequestURL(req.getSession().getId(), providerName,
      redirectUri, null);
    OAuthProvider oAuthProvider = new OAuthProvider(url, providerName);
    return Response.ok(oAuthProvider).build();
  }
  
  private Response getAuthURI(String providerName, String redirectURI, Set<String> scopes, boolean redirect,
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
  
  @POST
  @Path("/client")
  @ApiOperation(value = "Create new client")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response createClient(OAuthClientDTO oauthClientDTO, @Context UriInfo uriInfo, @Context HttpServletRequest req)
      throws RemoteAuthException {
    if (!settings.isOAuthEnabled()) {
      return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }
    OauthClient oauthClient = new OauthClient();
    setValues(oauthClient, oauthClientDTO);
    oAuthController.saveClient(oauthClient);
    updateSettings(oauthClientDTO, uriInfo);
    OAuthClientDTO oAuthClientDTO = oAuthClientBuilder.buildItem(uriInfo, oauthClientDTO.getProviderName());
    return Response.created(oAuthClientDTO.getHref()).entity(oAuthClientDTO).build();
  }
  
  @PUT
  @Path("/client/{id}")
  @ApiOperation(value = "Update a client")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response updateClient(@PathParam("id") Integer id, OAuthClientDTO oauthClientDTO, @Context UriInfo uriInfo,
      @Context HttpServletRequest req) throws RemoteAuthException {
    if (!settings.isOAuthEnabled()) {
      return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }
    OauthClient oauthClient = oauthClientFacade.find(id);
    if (oauthClient == null) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.NOT_FOUND, Level.FINE, "Client not found.");
    }
    setValues(oauthClient, oauthClientDTO);
    oAuthController.updateClient(oauthClient);
    updateSettings(oauthClientDTO, uriInfo);
    OAuthClientDTO oAuthClientDTO = oAuthClientBuilder.buildItem(uriInfo, oauthClientDTO.getProviderName());
    return Response.ok(oAuthClientDTO).build();
  }
  
  private boolean fromBoolean(Boolean val) {
    return val != null? val : false;
  }
  
  private void setValues(OauthClient oauthClient, OAuthClientDTO oauthClientDTO) throws RemoteAuthException {
    validate(oauthClient, oauthClientDTO);
    oauthClient.setClientId(oauthClientDTO.getClientId());
    oauthClient.setClientSecret(oauthClientDTO.getClientSecret());
    oauthClient.setProviderURI(oauthClientDTO.getProviderUri());
    oauthClient.setProviderName(oauthClientDTO.getProviderName());
    oauthClient.setProviderLogoURI(oauthClientDTO.getProviderLogoUri());
    oauthClient.setProviderDisplayName(oauthClientDTO.getProviderDisplayName());
    oauthClient
        .setProviderMetadataEndpointSupported(fromBoolean(oauthClientDTO.getProviderMetadataEndpointSupported()));
    if (!oauthClient.getProviderMetadataEndpointSupported()) {
      oauthClient.setAuthorisationEndpoint(oauthClientDTO.getAuthorizationEndpoint());
      oauthClient.setTokenEndpoint(oauthClientDTO.getTokenEndpoint());
      oauthClient.setUserInfoEndpoint(oauthClientDTO.getUserInfoEndpoint());
      oauthClient.setJwksURI(oauthClientDTO.getJwksURI());  
    }
    oauthClient.setEndSessionEndpoint(oauthClientDTO.getEndSessionEndpoint());
    oauthClient.setLogoutRedirectParam(oauthClientDTO.getLogoutRedirectParam());
    oauthClient.setOfflineAccess(fromBoolean(oauthClientDTO.getOfflineAccess()));
    if (oauthClientDTO.getCodeChallengeMethod() != null) {
      oauthClient.setCodeChallenge(true);
      oauthClient.setCodeChallengeMethod(oauthClientDTO.getCodeChallengeMethod());
    }
    oauthClient.setVerifyEmail(fromBoolean(oauthClientDTO.getVerifyEmail()));
  }
  
  private void updateSettings(OAuthClientDTO oauthClientDTO, UriInfo uriInfo) throws RemoteAuthException {
    URL base = null;
    try {
      if (Strings.isNullOrEmpty(oauthClientDTO.getCallbackURI())) {
        URI uri = uriInfo.getAbsolutePath();
        base = new URL(uri.getScheme(), uri.getHost(), uri.getPort(), "/");
      } else {
        base = new URL(oauthClientDTO.getCallbackURI());
      }
    } catch (MalformedURLException e) {
      LOGGER.log(Level.WARNING, e.getMessage());
    }
    String baseUrl = base == null? "" : base.toString();
    baseUrl = baseUrl.endsWith("/")? baseUrl : baseUrl + "/";
    oAuthController.updateSettings(oauthClientDTO.getNeedConsent(), oauthClientDTO.getRegistrationDisabled(),
        oauthClientDTO.getActivateUser(), oauthClientDTO.getGroupMapping(), oauthClientDTO.getGroupMappings(), baseUrl,
        oauthClientDTO.getRejectRemoteNoGroup(), oauthClientDTO.getManagedCloudRedirectUri(),
      oauthClientDTO.getManagedCloudLogoutRedirectUri());
  }
  
  private void validate(OauthClient oauthClient, OAuthClientDTO oauthClientDTO) throws RemoteAuthException {
    if (oauthClientDTO == null) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    oauthClientDTO.validate();
    if (oauthClient.getId() != null) {
      if (!oauthClient.getClientId().equals(oauthClientDTO.getClientId()) &&
        oauthClientFacade.findByClientId(oauthClientDTO.getClientId()) != null) {
        throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.DUPLICATE_ENTRY, Level.FINE,
          "A client with the same id already exists.");
      }
      if (!oauthClient.getProviderName().equals(oauthClientDTO.getProviderName()) &&
        oauthClientFacade.findByProviderName(oauthClientDTO.getProviderName()) != null) {
        throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.DUPLICATE_ENTRY, Level.FINE,
          "A client with the same provider name already exists.");
      }
    } else {
      if (oauthClientFacade.findByClientId(oauthClientDTO.getClientId()) != null) {
        throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.DUPLICATE_ENTRY, Level.FINE,
          "A client with the same id already exists.");
      }
      if (oauthClientFacade.findByProviderName(oauthClientDTO.getProviderName()) != null) {
        throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.DUPLICATE_ENTRY, Level.FINE,
          "A client with the same provider name already exists.");
      }
    }
  }
  
  @DELETE
  @Path("/client/{id}")
  @ApiOperation(value = "Delete a client")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response updateClient(@PathParam("id") Integer id, @Context UriInfo uriInfo, @Context HttpServletRequest req)
      throws RemoteAuthException {
    if (!settings.isOAuthEnabled()) {
      return Response.status(Response.Status.METHOD_NOT_ALLOWED).build();
    }
    OauthClient oauthClient = oauthClientFacade.find(id);
    if (oauthClient == null) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.NOT_FOUND, Level.FINE, "Client not found.");
    }
    oAuthController.removeClient(oauthClient);
    return Response.noContent().build();
  }
}
