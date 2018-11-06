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
package io.hops.hopsworks.api.user;

import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.api.ResourceProperties;

import javax.ejb.Stateless;
import javax.ws.rs.core.UriInfo;

@Stateless
public class UsersBuilder {

  public UserDTO uri(UserDTO dto, UriInfo uriInfo, Users user) {
    dto.setHref(uriInfo.getBaseUriBuilder()
        .path(ResourceProperties.Name.USERS.toString().toLowerCase())
        .path(user.getEmail())
        .build());
    return dto;
  }

  public UserDTO expand(UserDTO dto, ResourceProperties resourceProperties) {
    if (resourceProperties != null) {
      ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.USERS);
      if (property != null) {
        dto.setExpand(true);
      }
    }
    return dto;
  }

  public UserDTO build(Users user, UriInfo uriInfo, ResourceProperties resourceProperties) {
    UserDTO dto = new UserDTO();
    uri(dto, uriInfo, user);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      dto.setFirstname(user.getFname());
      dto.setLastname(user.getLname());
      dto.setEmail(user.getEmail());
    }
    return dto;
  }
}
