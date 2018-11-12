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
import io.hops.hopsworks.common.dao.user.UserFacade;
import java.util.Comparator;
import java.util.List;
import javax.ejb.EJB;

import javax.ejb.Stateless;
import javax.ws.rs.core.UriInfo;

@Stateless
public class UsersBuilder {

  @EJB
  private UserFacade userFacade;

  public UserDTO uri(UserDTO dto, UriInfo uriInfo, Users user) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .build());
    return dto;
  }

  public UserDTO uriItems(UserDTO dto, UriInfo uriInfo, Users user) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .path(Integer.toString(user.getUid()))
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

  public UserDTO build(UriInfo uriInfo, ResourceProperties resourceProperties, Users user) {
    UserDTO dto = new UserDTO();
    uriItems(dto, uriInfo, user);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      dto.setFirstname(user.getFname());
      dto.setLastname(user.getLname());
      dto.setEmail(user.getEmail());
    }
    return dto;
  }

  public UserDTO buildFull(UriInfo uriInfo, ResourceProperties resourceProperties, Users user) {
    UserDTO dto = new UserDTO();
    uri(dto, uriInfo, user);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      dto.setFirstname(user.getFname());
      dto.setLastname(user.getLname());
      dto.setEmail(user.getEmail());
      dto.setPhoneNumber(user.getMobile());
      dto.setMaxNumProjects(user.getMaxNumProjects());
      dto.setNumCreatedProjects(user.getNumCreatedProjects());
      dto.setNumActiveProjects(user.getNumActiveProjects());
    }
    return dto;
  }

  public UserDTO build(UriInfo uriInfo, ResourceProperties resourceProperties, Integer id) {
    Users user = userFacade.find(id);
    return buildFull(uriInfo, resourceProperties, user);
  }
  
  public UserDTO build(UriInfo uriInfo, ResourceProperties resourceProperties, String email) {
    Users user = userFacade.findByEmail(email);
    return buildFull(uriInfo, resourceProperties, user);
  }

  public UserDTO buildItems(UriInfo uriInfo, ResourceProperties resourceProperties) {
    return items(new UserDTO(), uriInfo, resourceProperties);
  }

  private UserDTO items(UserDTO userDTO, UriInfo uriInfo, ResourceProperties resourceProperties) {
    List<Users> users;
    ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.USERS);
    if (property.getLimit() != null || property.getOffset() != null) {
      users = userFacade.findAll(property.getOffset(), property.getLimit(), property.getOrderBy(),
          property.getSortBy());
      return items(userDTO, uriInfo, resourceProperties, users, false);
    }
    users = userFacade.findAll();
    return items(userDTO, uriInfo, resourceProperties, users, true);
  }

  private UserDTO items(UserDTO dto, UriInfo uriInfo, ResourceProperties resourceProperties,
      List<Users> users, boolean sort) {
    if (users != null && !users.isEmpty()) {
      if (sort) {
        ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.USERS);
        users.sort(getComparator(property));
      }
      users.forEach(( user ) -> {
        dto.addItem(build(uriInfo, resourceProperties, user));
      });
    }
    return dto;
  }

  public Comparator<Users> getComparator(ResourceProperties.ResourceProperty property) {
    if (property.getSortBy() != null) {
      switch (property.getSortBy()) {
        case EMAIL:
          return new UsersComparatorByEmail(property.getOrderBy());
        case DATE_CREATED:
          return new UsersComparatorByDateCreated(property.getOrderBy());
        case FIRST_NAME:
          return new UsersComparatorByFirstName(property.getOrderBy());
        case LAST_NAME:
          return new UsersComparatorByLastName(property.getOrderBy());
        default:
          break;
      }
    }
    return new UsersComparatorByEmail(property.getOrderBy());
  }

  class UsersComparatorByEmail implements Comparator<Users> {

    ResourceProperties.OrderBy orderByAsc = ResourceProperties.OrderBy.ASC;

    UsersComparatorByEmail(ResourceProperties.OrderBy orderByAsc) {
      if (orderByAsc != null) {
        this.orderByAsc = orderByAsc;
      }
    }

    @Override
    public int compare(Users a, Users b) {
      switch (orderByAsc) {
        case ASC:
          return String.CASE_INSENSITIVE_ORDER.compare(a.getEmail(), b.getEmail());
        case DESC:
          return String.CASE_INSENSITIVE_ORDER.compare(b.getEmail(), a.getEmail());
        default:
          break;
      }
      throw new UnsupportedOperationException("Order By " + orderByAsc + " not supported");
    }
  }

  class UsersComparatorByFirstName implements Comparator<Users> {

    ResourceProperties.OrderBy orderByAsc = ResourceProperties.OrderBy.ASC;

    UsersComparatorByFirstName(ResourceProperties.OrderBy orderByAsc) {
      if (orderByAsc != null) {
        this.orderByAsc = orderByAsc;
      }
    }

    @Override
    public int compare(Users a, Users b) {
      switch (orderByAsc) {
        case ASC:
          return String.CASE_INSENSITIVE_ORDER.compare(a.getFname(), b.getFname());
        case DESC:
          return String.CASE_INSENSITIVE_ORDER.compare(b.getFname(), a.getFname());
        default:
          break;
      }
      throw new UnsupportedOperationException("Order By " + orderByAsc + " not supported");
    }
  }

  class UsersComparatorByLastName implements Comparator<Users> {

    ResourceProperties.OrderBy orderByAsc = ResourceProperties.OrderBy.ASC;

    UsersComparatorByLastName(ResourceProperties.OrderBy orderByAsc) {
      if (orderByAsc != null) {
        this.orderByAsc = orderByAsc;
      }
    }

    @Override
    public int compare(Users a, Users b) {
      switch (orderByAsc) {
        case ASC:
          return String.CASE_INSENSITIVE_ORDER.compare(a.getLname(), b.getLname());
        case DESC:
          return String.CASE_INSENSITIVE_ORDER.compare(b.getLname(), a.getLname());
        default:
          break;
      }
      throw new UnsupportedOperationException("Order By " + orderByAsc + " not supported");
    }
  }

  class UsersComparatorByDateCreated implements Comparator<Users> {

    ResourceProperties.OrderBy orderByAsc = ResourceProperties.OrderBy.ASC;

    UsersComparatorByDateCreated(ResourceProperties.OrderBy orderByAsc) {
      if (orderByAsc != null) {
        this.orderByAsc = orderByAsc;
      }
    }

    @Override
    public int compare(Users a, Users b) {
      switch (orderByAsc) {
        case ASC:
          return a.getActivated().compareTo(b.getActivated());
        case DESC:
          return b.getActivated().compareTo(a.getActivated());
        default:
          break;
      }
      throw new UnsupportedOperationException("Order By " + orderByAsc + " not supported");
    }
  }
}
