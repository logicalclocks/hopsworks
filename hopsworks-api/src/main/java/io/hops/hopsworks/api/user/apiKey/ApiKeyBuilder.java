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
package io.hops.hopsworks.api.user.apiKey;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.security.apiKey.ApiKeyFacade;
import io.hops.hopsworks.exceptions.ApiKeyException;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiKey;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ApiKeyBuilder {
  
  private static final Logger LOGGER = Logger.getLogger(ApiKeyBuilder.class.getName());
  
  @EJB
  private ApiKeyFacade apikeyFacade;
  
  public ApiKeyDTO uri(ApiKeyDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .build());
    return dto;
  }
  
  public ApiKeyDTO uriItems(ApiKeyDTO dto, UriInfo uriInfo, ApiKey apikey) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .path(apikey.getName())
      .build());
    return dto;
  }
  
  public ApiKeyDTO expand(ApiKeyDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.APIKEY)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public ApiKeyDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, ApiKey apiKey) {
    ApiKeyDTO dto = new ApiKeyDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    expandDTO(dto, apiKey);
    return dto;
  }
  
  public ApiKeyDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, ApiKey apiKey) {
    ApiKeyDTO dto = new ApiKeyDTO();
    uriItems(dto, uriInfo, apiKey);
    expand(dto, resourceRequest);
    expandDTO(dto, apiKey);
    return dto;
  }
  
  private void expandDTO(ApiKeyDTO dto, ApiKey apiKey) {
    if (dto.isExpand()) {
      dto.setName(apiKey.getName());
      dto.setPrefix(apiKey.getPrefix());
      dto.setCreated(apiKey.getCreated());
      dto.setModified(apiKey.getModified());
      dto.setFromKeyScope(apiKey.getApiKeyScopeCollection());
    }
  }
  
  /**
   *
   * @param uriInfo
   * @param resourceRequest
   * @param user
   * @param name
   * @return
   * @throws ApiKeyException
   */
  public ApiKeyDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, String name)
    throws ApiKeyException {
    ApiKey apikey = apikeyFacade.findByUserAndName(user, name);
    if (apikey == null) {
      throw new ApiKeyException(RESTCodes.ApiKeyErrorCode.KEY_NOT_FOUND, Level.FINE);
    }
    return build(uriInfo, resourceRequest, apikey);
  }
  
  /**
   *
   * @param uriInfo
   * @param resourceRequest
   * @return
   */
  public ApiKeyDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Users user) {
    return items(new ApiKeyDTO(), uriInfo, resourceRequest, user);
  }
  
  private ApiKeyDTO items(ApiKeyDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, Users user) {
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = apikeyFacade.findByUser(resourceRequest.getOffset()
          , resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort(), user);
      dto.setCount(collectionInfo.getCount());
      return items(dto, uriInfo, resourceRequest, collectionInfo.getItems());
    }
    return dto;
  }
  
  private ApiKeyDTO items(ApiKeyDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, List<ApiKey> apiKeys) {
    if (apiKeys != null && !apiKeys.isEmpty()) {
      apiKeys.forEach((apikey) -> dto.addItem(buildItems(uriInfo, resourceRequest, apikey)));
    }
    return dto;
  }
}