/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.oauth2;

import io.hops.hopsworks.common.dao.remote.oauth.OauthClientFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.RemoteAuthException;
import io.hops.hopsworks.persistence.entity.remote.oauth.OauthClient;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OAuthClientBuilder {
  
  @EJB
  private OauthClientFacade oauthClientFacade;
  @EJB
  private Settings settings;
  
  public OAuthClientDTO uri(OAuthClientDTO dto, UriInfo uriInfo, OauthClient oauthClient) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .path(Integer.toString(oauthClient.getId()))
      .build());
    return dto;
  }
  
  public OAuthClientDTO uri(OAuthClientDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .build());
    return dto;
  }
  
  private OAuthClientDTO build(OAuthClientDTO dto, OauthClient oauthClient) {
    dto.setId(oauthClient.getId());
    dto.setClientId(oauthClient.getClientId());
    dto.setClientSecret(oauthClient.getClientSecret());
    dto.setProviderName(oauthClient.getProviderName());
    dto.setProviderDisplayName(oauthClient.getProviderDisplayName());
    dto.setProviderLogoUri(oauthClient.getProviderLogoURI());
    dto.setProviderMetadataEndpointSupported(oauthClient.getProviderMetadataEndpointSupported());
    dto.setRedirectUri(settings.getOauthRedirectUri(oauthClient.getProviderName()));
    dto.setProviderUri(oauthClient.getProviderURI());
    dto.setAuthorizationEndpoint(oauthClient.getAuthorisationEndpoint());
    dto.setCodeChallenge(oauthClient.isCodeChallenge());
    dto.setCodeChallengeMethod(oauthClient.getCodeChallengeMethod());
    dto.setVerifyEmail(oauthClient.isVerifyEmail());
    dto.setOfflineAccess(oauthClient.isOfflineAccess());
    dto.setRegistrationDisabled(settings.isRegistrationDisabled());
    dto.setEndSessionEndpoint(oauthClient.getEndSessionEndpoint());
    dto.setLogoutRedirectParam(oauthClient.getLogoutRedirectParam());
    return dto;
  }
  
  public OAuthClientDTO build(UriInfo uriInfo, OauthClient oauthClient) {
    OAuthClientDTO dto = new OAuthClientDTO();
    uri(dto, uriInfo, oauthClient);
    return build(dto, oauthClient);
  }
  
  public OAuthClientDTO buildItem(UriInfo uriInfo, OauthClient oauthClient) {
    OAuthClientDTO dto = new OAuthClientDTO();
    uri(dto, uriInfo);
    return build(dto, oauthClient);
  }
  
  public OAuthClientDTO buildItem(UriInfo uriInfo, Integer id) throws RemoteAuthException {
    OauthClient client = oauthClientFacade.find(id);
    if (client == null) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.NOT_FOUND, Level.FINE, "Client not found.");
    }
    return buildItem(uriInfo, client);
  }
  
  public OAuthClientDTO buildItem(UriInfo uriInfo, String providerName) throws RemoteAuthException {
    OauthClient client = oauthClientFacade.findByProviderName(providerName);
    if (client == null) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.NOT_FOUND, Level.FINE, "Client not found.");
    }
    return build(uriInfo, client);
  }
  
  public OAuthClientDTO buildItems(UriInfo uriInfo) {
    List<OauthClient> clients = oauthClientFacade.findAll();
    long count = oauthClientFacade.count();
    OAuthClientDTO dto = new OAuthClientDTO();
    uri(dto, uriInfo);
    dto.setCount(count);
    clients.forEach((oauthClient) -> dto.addItem(build(uriInfo, oauthClient)));
    return dto;
  }
}
