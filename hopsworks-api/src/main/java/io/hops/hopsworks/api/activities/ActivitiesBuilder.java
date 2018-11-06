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
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.Activity;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.exception.ActivitiesException;
import io.hops.hopsworks.common.exception.RESTCodes;
import java.util.Comparator;
import java.util.List;
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

  public ActivitiesDTO uri(ActivitiesDTO dto, UriInfo uriInfo, Activity activity) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .path(Integer.toString(activity.getId()))
        .build());
    return dto;
  }

  public ActivitiesDTO uriItems(ActivitiesDTO dto, UriInfo uriInfo, Activity activity) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
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
    uri(dto, uriInfo, activity);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      dto.setActivity(activity.getActivity());
      dto.setTimestamp(activity.getTimestamp());
      dto.setProjectName(activity.getProject().getName()); //(TODO: Ermias) make this expandable
      dto.setUserDTO(usersBuilder.build(activity.getUser(), uriInfo, resourceProperties));
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
    if (property.getLimit() != null || property.getOffset() != null) {
      activities = activityFacade.getPaginatedActivity(property.getOffset(), property.getLimit(), property.getOrderBy(),
          property.getSortBy());
      return items(dto, uriInfo, resourceProperties, activities, false);
    }
    activities = activityFacade.getAllActivities();
    return items(dto, uriInfo, resourceProperties, activities, true);
  }

  private ActivitiesDTO items(ActivitiesDTO dto, UriInfo uriInfo, ResourceProperties resourceProperties, Users user) {
    List<Activity> activities;
    ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.ACTIVITIES);
    if (property.getLimit() != null || property.getOffset() != null) {
      activities = activityFacade.getPaginatedActivityByUser(property.getOffset(), property.getLimit(), property.
          getOrderBy(), property.getSortBy(), user);
      return items(dto, uriInfo, resourceProperties, activities, false);
    }
    activities = activityFacade.getAllActivityByUser(user);
    return items(dto, uriInfo, resourceProperties, activities, true);
  }

  private ActivitiesDTO items(ActivitiesDTO dto, UriInfo uriInfo, ResourceProperties resourceProperties,
      Project project) {
    List<Activity> activities;
    ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.ACTIVITIES);
    if (property.getLimit() != null || property.getOffset() != null) {
      activities = activityFacade.getPaginatedActivityByProject(property.getOffset(), property.getLimit(), property.
          getOrderBy(), property.getSortBy(), project);
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
        activities.sort(getComparator(property));
      }
      activities.forEach(( activity ) -> {
        dto.addItem(build(uriInfo, resourceProperties, activity));
      });
    }
    return dto;
  }

  public Comparator<Activity> getComparator(ResourceProperties.ResourceProperty property) {
    if (property.getSortBy() != null) {
      switch (property.getSortBy()) {
        case ID:
          return new ActivityComparatorById(property.getOrderBy());
        case DATE_CREATED:
          return new ActivityComparatorByDateCreated(property.getOrderBy());
        default:
          break;
      }
    }
    return new ActivityComparatorById(property.getOrderBy());
  }

  class ActivityComparatorById implements Comparator<Activity> {

    ResourceProperties.OrderBy orderByAsc = ResourceProperties.OrderBy.ASC;

    ActivityComparatorById(ResourceProperties.OrderBy orderByAsc) {
      if (orderByAsc != null) {
        this.orderByAsc = orderByAsc;
      }
    }

    @Override
    public int compare(Activity a, Activity b) {
      switch (orderByAsc) {
        case ASC:
          return a.getId().compareTo(b.getId());
        case DESC:
          return b.getId().compareTo(a.getId());
        default:
          break;
      }
      throw new UnsupportedOperationException("Order By " + orderByAsc + " not supported");
    }
  }

  class ActivityComparatorByDateCreated implements Comparator<Activity> {

    ResourceProperties.OrderBy orderByAsc = ResourceProperties.OrderBy.ASC;

    ActivityComparatorByDateCreated(ResourceProperties.OrderBy orderByAsc) {
      if (orderByAsc != null) {
        this.orderByAsc = orderByAsc;
      }
    }

    @Override
    public int compare(Activity a, Activity b) {
      switch (orderByAsc) {
        case ASC:
          return a.getTimestamp().compareTo(b.getTimestamp());
        case DESC:
          return b.getTimestamp().compareTo(a.getTimestamp());
        default:
          break;
      }
      throw new UnsupportedOperationException("Order By " + orderByAsc + " not supported");
    }
  }

}
