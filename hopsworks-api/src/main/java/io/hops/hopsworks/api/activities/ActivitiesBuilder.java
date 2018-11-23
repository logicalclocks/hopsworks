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
import io.hops.hopsworks.common.api.ResourceProperties;
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
import javax.ws.rs.core.UriInfo;

@Stateless
public class ActivitiesBuilder {

  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private ActivityFacade activityFacade;

  public ActivitiesDTO uri(ActivitiesDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .build());
    return dto;
  }
  
  public ActivitiesDTO uriItem(ActivitiesDTO dto, UriInfo uriInfo, Activity activity) {
    dto.setHref(uriInfo.getBaseUriBuilder()
        .path(ResourceProperties.Name.ACTIVITIES.toString())
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

  public ActivitiesDTO expand(ActivitiesDTO dto, ResourceProperties resourceProperties) {
    if (resourceProperties != null) {
      ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.ACTIVITIES);
      if (property != null) {
        dto.setExpand(true);
      }
    }
    return dto;
  }

  public ActivitiesDTO build(UriInfo uriInfo, ResourceProperties resourceProperties, Activity activity) {
    ActivitiesDTO dto = new ActivitiesDTO();
    uri(dto, uriInfo);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      dto.setActivity(activity.getActivity());
      dto.setTimestamp(activity.getTimestamp());
      dto.setProjectName(activity.getProject().getName()); //(TODO: Ermias) make this expandable
      dto.setUser(usersBuilder.buildItem(uriInfo, resourceProperties, activity.getUser()));
    }
    return dto;
  }
  
  public ActivitiesDTO buildItem(UriInfo uriInfo, ResourceProperties resourceProperties, Activity activity) {
    ActivitiesDTO dto = new ActivitiesDTO();
    uriItem(dto, uriInfo, activity);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      dto.setActivity(activity.getActivity());
      dto.setTimestamp(activity.getTimestamp());
      dto.setProjectName(activity.getProject().getName()); //(TODO: Ermias) make this expandable
      dto.setUser(usersBuilder.buildItem(uriInfo, resourceProperties, activity.getUser()));
    }
    return dto;
  }

  public ActivitiesDTO buildItems(UriInfo uriInfo, ResourceProperties resourceProperties, Activity activity) {
    ActivitiesDTO dto = new ActivitiesDTO();
    uriItems(dto, uriInfo, activity);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      dto.setActivity(activity.getActivity());
      dto.setTimestamp(activity.getTimestamp());
      dto.setProjectName(activity.getProject().getName()); //(TODO: Ermias) make this expandable
      dto.setUser(usersBuilder.buildItem(uriInfo, resourceProperties, activity.getUser()));
    }
    return dto;
  }

  public ActivitiesDTO build(UriInfo uriInfo, ResourceProperties resourceProperties, Integer id) throws
      ActivitiesException {
    Activity activity = activityFacade.activityByID(id);
    if (activity == null) {
      throw new ActivitiesException(RESTCodes.ActivitiesErrorCode.ACTIVITY_NOT_FOUND, Level.FINE, "activityId: " + id);
    }
    return build(uriInfo, resourceProperties, activity);
  }

  public ActivitiesDTO build(UriInfo uriInfo, ResourceProperties resourceProperties, Users user, Integer id) throws
      ActivitiesException {
    Activity activity = activityFacade.getActivityByIdAndUser(user, id);
    if (activity == null) {
      throw new ActivitiesException(RESTCodes.ActivitiesErrorCode.ACTIVITY_NOT_FOUND, Level.FINE, "activityId: " + id);
    }
    return build(uriInfo, resourceProperties, activity);
  }

  public ActivitiesDTO build(UriInfo uriInfo, ResourceProperties resourceProperties, Project project, Integer id) throws
      ActivitiesException {
    Activity activity = activityFacade.getActivityByIdAndProject(project, id);
    if (activity == null) {
      throw new ActivitiesException(RESTCodes.ActivitiesErrorCode.ACTIVITY_NOT_FOUND, Level.FINE, "activityId: " + id);
    }
    return build(uriInfo, resourceProperties, activity);
  }

  public ActivitiesDTO buildItems(UriInfo uriInfo, ResourceProperties resourceProperties) {
    return items(new ActivitiesDTO(), uriInfo, resourceProperties);
  }

  public ActivitiesDTO buildItems(UriInfo uriInfo, ResourceProperties resourceProperties, Users user) {
    return items(new ActivitiesDTO(), uriInfo, resourceProperties, user);
  }

  public ActivitiesDTO buildItems(UriInfo uriInfo, ResourceProperties resourceProperties, Project project) {
    return items(new ActivitiesDTO(), uriInfo, resourceProperties, project);
  }

  private ActivitiesDTO items(ActivitiesDTO dto, UriInfo uriInfo, ResourceProperties resourceProperties) {
    List<Activity> activities;
    ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.ACTIVITIES);
    if (property.getOffset() != null || property.getLimit() != null || property.getFilter() != null) {
      activities = activityFacade.findAll(property.getOffset(), property.getLimit(), property.getFilter(), 
          property.getSort());
      return items(dto, uriInfo, resourceProperties, activities, false);
    }
    activities = activityFacade.getAllActivities();
    return items(dto, uriInfo, resourceProperties, activities, true);
  }

  private ActivitiesDTO items(ActivitiesDTO dto, UriInfo uriInfo, ResourceProperties resourceProperties, Users user) {
    List<Activity> activities;
    ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.ACTIVITIES);
    if (property.getOffset() != null || property.getLimit() != null || property.getFilter() != null) {
      activities = activityFacade.findAllByUser(property.getOffset(), property.getLimit(), property.getFilter(), 
          property.getSort(), user);
      return items(dto, uriInfo, resourceProperties, activities, false);
    }
    activities = activityFacade.getAllActivityByUser(user);
    return items(dto, uriInfo, resourceProperties, activities, true);
  }

  private ActivitiesDTO items(ActivitiesDTO dto, UriInfo uriInfo, ResourceProperties resourceProperties,
      Project project) {
    List<Activity> activities;
    ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.ACTIVITIES);
    if (property.getOffset() != null || property.getLimit() != null || property.getFilter() != null) {
      activities = activityFacade.findAllByProject(property.getOffset(), property.getLimit(), property.getFilter(), 
          property.getSort(), project);
      return items(dto, uriInfo, resourceProperties, activities, false);
    }
    activities = activityFacade.getAllActivityByProject(project);
    return items(dto, uriInfo, resourceProperties, activities, true);
  }

  private ActivitiesDTO items(ActivitiesDTO dto, UriInfo uriInfo, ResourceProperties resourceProperties,
      List<Activity> activities, boolean sort) {
    if (activities != null && !activities.isEmpty()) {
      if (sort) {
        ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.ACTIVITIES);
        Comparator<Activity> comparator = getComparator(property);
        if (comparator != null) {
          activities.sort(comparator);
        }
      }
      activities.forEach(( activity ) -> {
        dto.addItem(buildItems(uriInfo, resourceProperties, activity));
      });
    }
    return dto;
  }

  public Comparator<Activity> getComparator(ResourceProperties.ResourceProperty property) {
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
      switch (sortBy) {
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
          return a.compareTo(b);
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
