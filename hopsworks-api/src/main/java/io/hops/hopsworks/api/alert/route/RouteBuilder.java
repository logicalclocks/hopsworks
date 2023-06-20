/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.alert.route;

import io.hops.hopsworks.alert.AlertManagerConfiguration;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class RouteBuilder {

  @EJB
  private AlertManagerConfiguration alertManagerConfiguration;

  public RouteDTO uri(RouteDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .build());
    return dto;
  }

  public RouteDTO uri(RouteDTO dto, UriInfo uriInfo, Route route) {
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    return addQueryToUri(dto, builder, route);
  }

  public RouteDTO uriItem(RouteDTO dto, UriInfo uriInfo, Route route) {
    UriBuilder builder = uriInfo.getAbsolutePathBuilder()
        .path(route.getReceiver());
    return addQueryToUri(dto, builder, route);
  }

  private RouteDTO addQueryToUri(RouteDTO dto, UriBuilder builder, Route route) {
    if (route.getMatch() != null) {
      for (String key : route.getMatch().keySet()) {
        builder = builder.queryParam("match", key + ":" + route.getMatch().get(key));
      }
    }
    if (route.getMatchRe() != null) {
      for (String key : route.getMatchRe().keySet()) {
        builder = builder.queryParam("matchRe", key + ":" + route.getMatchRe().get(key));
      }
    }
    dto.setHref(builder.build());
    return dto;
  }

  public RouteDTO expand(RouteDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.ROUTES)) {
      dto.setExpand(true);
    }
    return dto;
  }

  public RouteDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Route route) {
    RouteDTO dto = new RouteDTO();
    uri(dto, uriInfo, route);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setGroupBy(route.getGroupBy());
      dto.setGroupWait(route.getGroupWait());
      dto.setGroupInterval(route.getGroupInterval());
      dto.setRepeatInterval(route.getRepeatInterval());
      dto.setReceiver(route.getReceiver());
      dto.setRoutes(route.getRoutes());
      dto.setContinue(route.getContinue());
      dto.setMatch(route.getMatch());
      dto.setMatchRe(route.getMatchRe());
    }
    return dto;
  }

  public RouteDTO buildItem(UriInfo uriInfo, ResourceRequest resourceRequest, Route route) {
    RouteDTO dto = new RouteDTO();
    uriItem(dto, uriInfo, route);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setGroupBy(route.getGroupBy());
      dto.setGroupWait(route.getGroupWait());
      dto.setGroupInterval(route.getGroupInterval());
      dto.setRepeatInterval(route.getRepeatInterval());
      dto.setReceiver(route.getReceiver());
      dto.setRoutes(route.getRoutes());
      dto.setContinue(route.getContinue());
      dto.setMatch(route.getMatch());
      dto.setMatchRe(route.getMatchRe());
    }
    return dto;
  }

  public Map<String, String> toMap(List<String> params) {
    Map<String, String> map = new HashMap<>();
    if (params == null || params.isEmpty()) {
      return null;
    }
    for (String param : params) {
      String[] paramParts = param.split(":");
      if (paramParts.length < 2) {
        throw new WebApplicationException("Match need to set <key>:<value> pairs, but found: " + param,
            Response.Status.NOT_FOUND);
      }
      map.put(paramParts[0], paramParts[1]);
    }
    return map;
  }

  public RouteDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Route routeToGet, Project project)
      throws AlertException {
    Route route;
    try {
      route = alertManagerConfiguration.getRoute(routeToGet, project);
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerNoSuchElementException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ROUTE_NOT_FOUND, Level.FINE, e.getMessage());
    }
    return build(uriInfo, resourceRequest, route);
  }

  /**
   * Build a list of Routes
   *
   * @param uriInfo
   * @param resourceRequest
   * @return
   */
  public RouteDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, RouteBeanParam routeBeanParam,
      Project project) throws AlertException {
    return items(new RouteDTO(), uriInfo, resourceRequest, routeBeanParam, project);
  }

  private RouteDTO items(RouteDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      RouteBeanParam routeBeanParam, Project project)
      throws AlertException {
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    List<Route> routes;
    if (dto.isExpand()) {
      try {
        if (project != null) {
          routes = alertManagerConfiguration.getRoutes(project);
        } else {
          routes = alertManagerConfiguration.getRoutes();
        }
      } catch (AlertManagerConfigReadException e) {
        throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
      }
      dto.setCount((long) routes.size());
      return items(dto, uriInfo, resourceRequest, routeBeanParam, routes);
    }
    return dto;
  }

  private RouteDTO items(RouteDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, RouteBeanParam routeBeanParam,
      List<Route> routes) {
    if (routes != null && !routes.isEmpty()) {
      routes.forEach((route) -> dto.addItem(buildItem(uriInfo, resourceRequest, route)));
      if (routeBeanParam.getSortBySet() != null && !routeBeanParam.getSortBySet().isEmpty()) {
        RouteComparator routeComparator = new RouteComparator(routeBeanParam.getSortBySet());
        dto.getItems().sort(routeComparator);
      }
      paginate(dto, resourceRequest);
    }
    return dto;
  }

  private void paginate(RestDTO restDTO, ResourceRequest resourceRequest) {
    if (restDTO.getItems() != null && restDTO.getItems().size() > 1) {
      int offset = resourceRequest.getOffset() != null ? resourceRequest.getOffset() : 0;
      int limit = resourceRequest.getLimit() != null ? resourceRequest.getLimit() : restDTO.getItems().size();
      restDTO.getItems()
          .subList(Math.min(restDTO.getItems().size(), offset), Math.min(restDTO.getItems().size(), offset + limit));
    }
  }

  static class RouteComparator implements Comparator<RouteDTO> {

    Set<RouteSortBy> sortBy;

    RouteComparator(Set<RouteSortBy> sortBy) {
      this.sortBy = sortBy;
    }

    private int compare(RouteDTO a, RouteDTO b, RouteSortBy sortBy) {
      switch (sortBy.getSortBy()) {
        case RECEIVER:
          return order(a.getReceiver(), b.getReceiver(), sortBy.getParam());
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

    @Override
    public int compare(RouteDTO a, RouteDTO b) {
      Iterator<RouteSortBy> sort = sortBy.iterator();
      int c = compare(a, b, sort.next());
      for (; sort.hasNext() && c == 0;) {
        c = compare(a, b, sort.next());
      }
      return c;
    }
  }
}