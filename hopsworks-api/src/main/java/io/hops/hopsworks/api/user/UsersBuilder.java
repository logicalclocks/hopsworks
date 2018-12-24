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

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UsersBuilder {

  @EJB
  private UserFacade userFacade;

  public UserDTO uri(UserDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .build());
    return dto;
  }

  public UserProfileDTO uri(UserProfileDTO dto, UriInfo uriInfo, Users user) {
    dto.setHref(uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.USERS.toString())
        .path(Integer.toString(user.getUid()))
        .build());
    return dto;
  }

  public UserDTO uri(UserDTO dto, UriInfo uriInfo, Users user) {
    dto.setHref(uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.USERS.toString())
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

  public UserProfileDTO expand(UserProfileDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.USERS)) {
      dto.setExpand(true);
    }
    return dto;
  }

  public UserDTO expand(UserDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && (resourceRequest.contains(ResourceRequest.Name.USER)
        || resourceRequest.contains(ResourceRequest.Name.USERS)
        || resourceRequest.contains(ResourceRequest.Name.CREATOR))) {
      dto.setExpand(true);
    }
    return dto;
  }

  public UserDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user) {
    UserDTO dto = new UserDTO();
    uri(dto, uriInfo, user);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setFirstname(user.getFname());
      dto.setLastname(user.getLname());
      dto.setEmail(user.getEmail());
      dto.setUsername(user.getUsername());
    }
    return dto;
  }

  public UserProfileDTO buildFull(UriInfo uriInfo, ResourceRequest resourceRequest, Users user) {
    UserProfileDTO dto = new UserProfileDTO();
    uri(dto, uriInfo, user);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setFirstname(user.getFname());
      dto.setLastname(user.getLname());
      dto.setEmail(user.getEmail());
      dto.setUsername(user.getUsername());
      dto.setPhoneNumber(user.getMobile());
      dto.setMaxNumProjects(user.getMaxNumProjects());
      dto.setNumCreatedProjects(user.getNumCreatedProjects());
      dto.setNumActiveProjects(user.getNumActiveProjects());
    }
    return dto;
  }

  public UserProfileDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Integer id) {
    Users user = userFacade.find(id);
    return buildFull(uriInfo, resourceRequest, user);
  }

  public UserProfileDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, String email) {
    Users user = userFacade.findByEmail(email);
    return buildFull(uriInfo, resourceRequest, user);
  }

  public UserDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest) {
    UserDTO dto = new UserDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = userFacade.findAll(resourceRequest.getOffset(),
        resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort());
      //set the count
      dto.setCount(collectionInfo.getCount());
      collectionInfo.getItems().forEach(( user ) -> dto.addItem(build(uriInfo, resourceRequest, (Users) user)));
    }
    return dto;
  }

  public Comparator<Users> getComparator(ResourceRequest resourceRequest) {
    Set<UserFacade.SortBy> sortBy = (Set<UserFacade.SortBy>) resourceRequest.getSort();
    if (resourceRequest.getSort() != null && !resourceRequest.getSort().isEmpty()) {
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
          return b.compareTo(a);
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
