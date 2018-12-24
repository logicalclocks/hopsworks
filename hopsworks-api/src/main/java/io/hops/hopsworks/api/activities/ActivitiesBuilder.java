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
package io.hops.hopsworks.api.activities;

import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.Activity;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.exception.ActivitiesException;
import io.hops.hopsworks.common.exception.RESTCodes;

import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ActivitiesBuilder {

  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private ActivityFacade activityFacade;

  public ActivitiesDTO uri(ActivitiesDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder().build());
    return dto;
  }

  public ActivitiesDTO uri(ActivitiesDTO dto, UriInfo uriInfo, Activity activity) {
    dto.setHref(uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.ACTIVITIES.toString())
        .path(Integer.toString(activity.getId()))
        .build());
    return dto;
  }

  public ActivitiesDTO uriItems(ActivitiesDTO dto, UriInfo uriInfo, Activity activity) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .path(Integer.toString(activity.getId()))
        .build());
    return dto;
  }

  public ActivitiesDTO expand(ActivitiesDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null) {
      dto.setExpand(true);
    }
    return dto;
  }

  public ActivitiesDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Activity activity) {
    ActivitiesDTO dto = new ActivitiesDTO();
    uri(dto, uriInfo, activity);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setActivity(activity.getActivity());
      dto.setTimestamp(activity.getTimestamp());
      dto.setProjectName(activity.getProject().getName());
      dto.setFlag(activity.getFlag());
      dto.setUser(usersBuilder.build(uriInfo, resourceRequest, activity.getUser()));
    }
    return dto;
  }

  public ActivitiesDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Activity activity) {
    ActivitiesDTO dto = new ActivitiesDTO();
    uriItems(dto, uriInfo, activity);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setActivity(activity.getActivity());
      dto.setTimestamp(activity.getTimestamp());
      dto.setProjectName(activity.getProject().getName());
      dto.setFlag(activity.getFlag());
      dto.setUser(usersBuilder.build(uriInfo, resourceRequest, activity.getUser()));
    }
    return dto;
  }

  public ActivitiesDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, Integer id) throws
      ActivitiesException {
    Activity activity = activityFacade.getActivityByIdAndUser(user, id);
    if (activity == null) {
      throw new ActivitiesException(RESTCodes.ActivitiesErrorCode.ACTIVITY_NOT_FOUND, Level.FINE, "activityId: " + id);
    }
    return build(uriInfo, resourceRequest, activity);
  }

  public ActivitiesDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Integer id) throws
      ActivitiesException {
    Activity activity = activityFacade.getActivityByIdAndProject(project, id);
    if (activity == null) {
      throw new ActivitiesException(RESTCodes.ActivitiesErrorCode.ACTIVITY_NOT_FOUND, Level.FINE, "activityId: " + id);
    }
    return build(uriInfo, resourceRequest, activity);
  }

  public ActivitiesDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Users user) {
    return items(new ActivitiesDTO(), uriInfo, resourceRequest, user);
  }

  public ActivitiesDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    return items(new ActivitiesDTO(), uriInfo, resourceRequest, project);
  }

  private ActivitiesDTO items(ActivitiesDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, Users user) {
    AbstractFacade.CollectionInfo
      collectionInfo = activityFacade.findAllByUser(resourceRequest.getOffset(), resourceRequest.getLimit(),
      resourceRequest.getFilter(), resourceRequest.getSort(), user);
    dto.setCount(collectionInfo.getCount());
    return items(dto, uriInfo, resourceRequest, collectionInfo.getItems());
  }

  private ActivitiesDTO items(ActivitiesDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    AbstractFacade.CollectionInfo collectionInfo = activityFacade.findAllByProject(resourceRequest.getOffset(),
      resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort(), project);
    dto.setCount(collectionInfo.getCount());
    return items(dto, uriInfo, resourceRequest, collectionInfo.getItems());
  }
  
  private ActivitiesDTO items(ActivitiesDTO dto, UriInfo uriInfo, ResourceRequest property, List<Activity> activities) {
    activities.forEach(activity -> dto.addItem(buildItems(uriInfo, property, activity)));
    return dto;
  }

  public Comparator<Activity> getComparator(ResourceRequest property) {
    Set<ActivityFacade.SortBy> sortBy = (Set<ActivityFacade.SortBy>) property.getSort();
    if (property.getSort() != null && !property.getSort().isEmpty()) {
      return new ActivityComparator(sortBy);
    }
    return null;
  }

  class ActivityComparator implements Comparator<Activity> {

    Set<ActivityFacade.SortBy> sortBy;

    ActivityComparator(Set<ActivityFacade.SortBy> sortBy) {
      this.sortBy = sortBy;
    }

    private int compare(Activity a, Activity b, ActivityFacade.SortBy sortBy) {
      switch (ActivityFacade.Sorts.valueOf(sortBy.getValue())) {
        case ID:
          return order(a.getId(), b.getId(), sortBy.getParam());
        case FLAG:
          return order(a.getFlag(), b.getFlag(), sortBy.getParam());
        case DATE_CREATED:
          return order(a.getTimestamp(), b.getTimestamp(), sortBy.getParam());
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

    private int order(Integer a, Integer b, AbstractFacade.OrderBy orderBy) {
      switch (orderBy) {
        case ASC:
          return a.compareTo(b);
        case DESC:
          return b.compareTo(a);
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
    public int compare(Activity a, Activity b) {
      Iterator<ActivityFacade.SortBy> sort = sortBy.iterator();
      int c = compare(a, b, sort.next());
      for (; sort.hasNext() && c == 0;) {
        c = compare(a, b, sort.next());
      }
      return c;
    }
  }

}
