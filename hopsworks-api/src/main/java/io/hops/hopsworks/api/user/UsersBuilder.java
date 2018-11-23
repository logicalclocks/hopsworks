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
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
  
  public UserDTO uriItem(UserDTO dto, UriInfo uriInfo, Users user) {
    dto.setHref(uriInfo.getBaseUriBuilder()
        .path(ResourceProperties.Name.USERS.toString())
        .path(Integer.toString(user.getUid()))
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
    uri(dto, uriInfo, user);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      dto.setFirstname(user.getFname());
      dto.setLastname(user.getLname());
      dto.setEmail(user.getEmail());
    }
    return dto;
  }
  
  public UserDTO buildItem(UriInfo uriInfo, ResourceProperties resourceProperties, Users user) {
    UserDTO dto = new UserDTO();
    uriItem(dto, uriInfo, user);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      dto.setFirstname(user.getFname());
      dto.setLastname(user.getLname());
      dto.setEmail(user.getEmail());
    }
    return dto;
  }

  public UserDTO buildItems(UriInfo uriInfo, ResourceProperties resourceProperties, Users user) {
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
    if (property.getOffset() != null || property.getLimit() != null || property.getFilter() != null) {
      users = userFacade.findAll(property.getOffset(), property.getLimit(), property.getFilter(), property.getSort());
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
        Comparator<Users> comparator = getComparator(property);
        if (comparator != null) {
          users.sort(comparator);
        }
      }
      users.forEach(( user ) -> {
        dto.addItem(buildItems(uriInfo, resourceProperties, user));
      });
    }
    return dto;
  }

  public Comparator<Users> getComparator(ResourceProperties.ResourceProperty property) {
    Set<UserFacade.SortBy> sortBy = (Set<UserFacade.SortBy>) property.getSort();
    if (property.getSort() != null && !property.getSort().isEmpty()) {
      return new UsersComparator(sortBy);
    }
    return null;
  }

  class UsersComparator implements Comparator<Users> {

    Set<UserFacade.SortBy> sortBy;

    UsersComparator(Set<UserFacade.SortBy> sort) {
      this.sortBy = sort;
    }

    private int compare(Users a, Users b, UserFacade.SortBy sortBy) {
      switch (UserFacade.Sorts.valueOf(sortBy.getValue())) {
        case EMAIL:
          return order(a.getEmail(), b.getEmail(), sortBy.getParam());
        case DATE_CREATED:
          return order(a.getActivated(), b.getActivated(), sortBy.getParam());
        case FIRST_NAME:
          return order(a.getFname(), b.getFname(), sortBy.getParam());
        case LAST_NAME:
          return order(a.getLname(), b.getLname(), sortBy.getParam());
        default:
          throw new UnsupportedOperationException("Sort By " + sortBy + " not supported");
      }
    }

    private int order(String a, String b, AbstractFacade.OrderBy orderBy) {
      switch (orderBy) {
        case ASC:
          return String.CASE_INSENSITIVE_ORDER.compare(a, b);
        case DESC:
          return String.CASE_INSENSITIVE_ORDER.compare(b, a);
        default:
          throw new UnsupportedOperationException("Order By " + orderBy + " not supported");
      }
    }

    private int order(Date a, Date b, AbstractFacade.OrderBy orderBy) {
      switch (orderBy) {
        case ASC:
          return a.compareTo(b);
        case DESC:
          return a.compareTo(b);
        default:
          throw new UnsupportedOperationException("Order By " + orderBy + " not supported");
      }
    }

    @Override
    public int compare(Users a, Users b) {
      Iterator<UserFacade.SortBy> sort = sortBy.iterator();
      int c = compare(a, b, sort.next());
      for (; sort.hasNext() && c == 0;) {
        c = compare(a, b, sort.next());
      }
      return c;
    }
  }
}
