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

package io.hops.hopsworks.api.alert;

import io.hops.hopsworks.alert.AlertManager;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.api.alert.dto.Alert;
import io.hops.hopsworks.alerting.api.alert.dto.AlertGroup;
import io.hops.hopsworks.alerting.api.alert.dto.PostableAlert;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AlertBuilder {

  private static final Logger LOGGER = Logger.getLogger(AlertBuilder.class.getName());

  @EJB
  private AlertManager alertManager;

  public RestDTO uri(RestDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .build());
    return dto;
  }

  public RestDTO uri(RestDTO dto, UriInfo uriInfo, Alert alert) {
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    dto.setHref(addQuery(builder, alert.getLabels()).build());
    return dto;
  }

  private void uri(AlertDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.ALERTS.toString().toLowerCase())
      .build());
  }

  private void uri(AlertDTO dto, UriInfo uriInfo, Project project, Alert alert) {
    UriBuilder builder = uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.ALERTS.toString().toLowerCase());
    dto.setHref(addQuery(builder, alert.getLabels()).build());
  }

  private void uri(AlertGroupDTO dto, UriInfo uriInfo, AlertGroup alertGroup) {
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    dto.setHref(addQuery(builder, alertGroup.getLabels()).build());
  }

  private UriBuilder addQuery(UriBuilder builder, Map<String, String> labels) {
    for (String label : labels.keySet()) {
      builder = builder.queryParam("filter", label + "=" + labels.get(label));
    }
    return builder;
  }

  public RestDTO expand(RestDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.ALERTS)) {
      dto.setExpand(true);
    }
    return dto;
  }

  public RestDTO expandAlertGroups(RestDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.ALERTGROUPS)) {
      dto.setExpand(true);
    }
    return dto;
  }

  private AlertDTO build(AlertDTO dto, ResourceRequest resourceRequest, Alert alert) {
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setLabels(alert.getLabels());
      dto.setAnnotations(alert.getAnnotations());
      dto.setReceivers(alert.getReceivers());
      dto.setStatus(alert.getStatus());
      dto.setFingerprint(alert.getFingerprint());
      dto.setUpdatedAt(alert.getUpdatedAt());
      dto.setStartsAt(alert.getStartsAt());
      dto.setEndsAt(alert.getEndsAt());
      dto.setGeneratorURL(alert.getGeneratorURL());
    }
    return dto;
  }

  public AlertDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Alert alert) {
    AlertDTO dto = new AlertDTO();
    uri(dto, uriInfo, alert);
    return build(dto, resourceRequest, alert);
  }

  public AlertDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Alert alert) {
    AlertDTO dto = new AlertDTO();
    uri(dto, uriInfo, project, alert);
    return build(dto, resourceRequest, alert);
  }

  public AlertGroupDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, AlertGroup alertGroup) {
    AlertGroupDTO dto = new AlertGroupDTO();
    uri(dto, uriInfo, alertGroup);
    expandAlertGroups(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setLabels(alertGroup.getLabels());
      dto.setReceiver(alertGroup.getReceiver());
      dto.setExpand(false);
      expand(dto, resourceRequest);
      if (dto.isExpand()) {
        dto.setAlertDTOs(alertGroup.getAlerts().stream().map(AlertDTO::new).collect(Collectors.toList()));
      }
    }
    return dto;
  }

  public PostableAlert build(PostableAlertDTO alert) {
    PostableAlert dto = new PostableAlert();
    dto.setEndsAt(alert.getEndsAt());
    dto.setStartsAt(alert.getStartsAt());
    dto.setLabels(alert.getLabels().stream()
      .filter(entry -> entry.getKey() != null && entry.getValue() != null)
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    dto.setAnnotations(alert.getAnnotations().stream()
      .filter(entry -> entry.getKey() != null && entry.getValue() != null)
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    dto.setGeneratorURL(alert.getGeneratorURL());
    return dto;
  }

  /**
   * Build a list of PostableAlert
   *
   * @return
   */
  public List<PostableAlert> buildItems(List<PostableAlertDTO> alerts) {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    if (alerts != null && !alerts.isEmpty()) {
      alerts.forEach((alert) -> postableAlerts.add(build(alert)));
    }
    return postableAlerts;
  }

  public AlertDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, AlertBeanParam alertBeanParam,
    Project project) throws AlertException {
    return items(new AlertDTO(), uriInfo, resourceRequest, alertBeanParam, project);
  }

  public AlertGroupDTO buildAlertGroupItems(UriInfo uriInfo, ResourceRequest resourceRequest,
    AlertFilterBy alertFilterBy, Project project) throws AlertException {
    return items(new AlertGroupDTO(), uriInfo, resourceRequest, alertFilterBy, project);
  }

  private AlertDTO items(AlertDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, AlertBeanParam alertBeanParam,
    Project project) throws AlertException {
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      List<Alert> alerts;
      try {
        if (project != null) {
          alerts = alertManager.getAlerts(alertBeanParam.getAlertFilterBy().isActive(),
            alertBeanParam.getAlertFilterBy().isSilenced(), alertBeanParam.getAlertFilterBy().isInhibited(),
            alertBeanParam.isUnprocessed(), alertBeanParam.getAlertFilterBy().getFilter(),
            alertBeanParam.getAlertFilterBy().getReceiver(), project);
        } else {
          alerts = alertManager.getAlerts(alertBeanParam.getAlertFilterBy().isActive(),
            alertBeanParam.getAlertFilterBy().isSilenced(), alertBeanParam.getAlertFilterBy().isInhibited(),
            alertBeanParam.isUnprocessed(), alertBeanParam.getAlertFilterBy().getFilter(),
            alertBeanParam.getAlertFilterBy().getReceiver());
        }
      } catch (AlertManagerResponseException a) {
        throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, a.getMessage());
      } catch (AlertManagerClientCreateException | AlertManagerUnreachableException e) {
        throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
      } catch (AlertManagerAccessControlException e) {
        throw new AlertException(RESTCodes.AlertErrorCode.ACCESS_CONTROL_EXCEPTION, Level.FINE, e.getMessage());
      }
      dto.setCount((long) alerts.size());
      return items(dto, uriInfo, resourceRequest, alertBeanParam, alerts);
    }
    return dto;
  }

  private AlertGroupDTO items(AlertGroupDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    AlertFilterBy alertFilterBy, Project project) throws AlertException {
    uri(dto, uriInfo);
    expandAlertGroups(dto, resourceRequest);
    if (dto.isExpand()) {
      List<AlertGroup> alertGroups;
      try {
        if (project != null) {
          alertGroups = alertManager.getAlertGroups(alertFilterBy.isActive(), alertFilterBy.isSilenced(),
            alertFilterBy.isInhibited(), alertFilterBy.getFilter(), alertFilterBy.getReceiver(), project);
        } else {
          alertGroups = alertManager.getAlertGroups(alertFilterBy.isActive(), alertFilterBy.isSilenced(),
            alertFilterBy.isInhibited(), alertFilterBy.getFilter(), alertFilterBy.getReceiver());
        }
      } catch (AlertManagerResponseException a) {
        throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, a.getMessage());
      } catch (AlertManagerClientCreateException | AlertManagerUnreachableException e) {
        throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
      } catch (AlertManagerAccessControlException e) {
        throw new AlertException(RESTCodes.AlertErrorCode.ACCESS_CONTROL_EXCEPTION, Level.FINE, e.getMessage());
      }
      dto.setCount((long) alertGroups.size());
      return items(dto, uriInfo, resourceRequest, alertGroups);
    }
    return dto;
  }

  private AlertDTO items(AlertDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, AlertBeanParam alertBeanParam,
    List<Alert> alerts) {
    if (alerts != null && !alerts.isEmpty()) {
      alerts.forEach((alert) -> dto.addItem(build(uriInfo, resourceRequest, alert)));
      if (dto.getItems() != null && dto.getItems().size() > 1 &&
        alertBeanParam.getSortBySet() != null && !alertBeanParam.getSortBySet().isEmpty()) {
        AlertComparator alertComparator = new AlertComparator(alertBeanParam.getSortBySet());
        dto.getItems().sort(alertComparator);
      }
      paginate(dto, resourceRequest);
    }
    return dto;
  }

  public AlertDTO getAlertDTOs(UriInfo uriInfo, ResourceRequest resourceRequest, List<Alert> alerts, Project project) {
    AlertDTO dto = new AlertDTO();
    uri(dto, uriInfo, project);
    expandAlertGroups(dto, resourceRequest);
    dto.setCount((long) alerts.size());
    if (!alerts.isEmpty()) {
      alerts.forEach((alert) -> dto.addItem(build(uriInfo, resourceRequest, project, alert)));
    }
    return dto;
  }

  private AlertGroupDTO items(AlertGroupDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    List<AlertGroup> alertGroups) {
    if (alertGroups != null && !alertGroups.isEmpty()) {
      alertGroups.forEach((alertGroup) -> dto.addItem(build(uriInfo, resourceRequest, alertGroup)));
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

  static class AlertComparator implements Comparator<AlertDTO> {

    Set<AlertSortBy> sortBy;

    AlertComparator(Set<AlertSortBy> sortBy) {
      this.sortBy = sortBy;
    }

    private int compare(AlertDTO a, AlertDTO b, AlertSortBy sortBy) {
      switch (sortBy.getSortBy()) {
        case ENDS_AT:
          return order(a.getEndsAt(), b.getEndsAt(), sortBy.getParam());
        case STARTS_AT:
          return order(a.getStartsAt(), b.getStartsAt(), sortBy.getParam());
        case UPDATED_AT:
          return order(a.getUpdatedAt(), b.getUpdatedAt(), sortBy.getParam());
        default:
          throw new UnsupportedOperationException("Sort By " + sortBy + " not supported");
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
    public int compare(AlertDTO a, AlertDTO b) {
      Iterator<AlertSortBy> sort = sortBy.iterator();
      int c = compare(a, b, sort.next());
      while (sort.hasNext() && c == 0) {
        c = compare(a, b, sort.next());
      }
      return c;
    }
  }
}