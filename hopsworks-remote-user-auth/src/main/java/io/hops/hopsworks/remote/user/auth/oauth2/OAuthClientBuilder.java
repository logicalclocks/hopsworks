/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.auth.oauth2;

import io.hops.hopsworks.common.dao.remote.oauth.OauthClient;
import io.hops.hopsworks.common.dao.remote.oauth.OauthClientFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.remote.user.auth.api.oauth2.OAuthClientDTO;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;

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
  
  public OAuthClientDTO build(UriInfo uriInfo, OauthClient oauthClient) {
    OAuthClientDTO dto = new OAuthClientDTO();
    uri(dto, uriInfo, oauthClient);
    dto.setClientId(oauthClient.getClientId());
    dto.setProviderName(oauthClient.getProviderName());
    dto.setProviderDisplayName(oauthClient.getProviderDisplayName());
    dto.setRedirectUri(settings.getOauthRedirectUri());
    dto.setProviderURI(oauthClient.getProviderURI());
    dto.setAuthorizationEndpoint(oauthClient.getAuthorisationEndpoint());
    return dto;
  }
  
  public OAuthClientDTO buildItem(UriInfo uriInfo, Integer id) {
    OauthClient client = oauthClientFacade.find(id);
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
