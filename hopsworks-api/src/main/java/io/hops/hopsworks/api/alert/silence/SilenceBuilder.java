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

package io.hops.hopsworks.api.alert.silence;

import com.google.common.base.Strings;
import io.hops.hopsworks.alert.AMClient;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.api.alert.dto.PostableSilence;
import io.hops.hopsworks.alerting.api.alert.dto.Silence;
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
import javax.ws.rs.core.UriInfo;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SilenceBuilder {
  
  private static final Logger LOGGER = Logger.getLogger(SilenceResource.class.getName());
  
  @EJB
  private AMClient alertManager;
  
  public SilenceDTO uri(SilenceDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .build());
    return dto;
  }
  
  public SilenceDTO uri(SilenceDTO dto, UriInfo uriInfo, Silence silence) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .path(silence.getId())
        .build());
    return dto;
  }
  
  public SilenceDTO expand(SilenceDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.SILENCES)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public PostableSilence getPostableSilence(PostableSilenceDTO postableSilenceDTO) {
    PostableSilence postableSilence = new PostableSilence();
    postableSilence.setId(postableSilenceDTO.getId());
    postableSilence.setComment(postableSilenceDTO.getComment());
    postableSilence.setEndsAt(postableSilenceDTO.getEndsAt());
    postableSilence.setStartsAt(postableSilenceDTO.getStartsAt());
    postableSilence.setMatchers(postableSilenceDTO.getMatchers());
    return postableSilence;
  }
  
  public SilenceDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Silence silence) {
    SilenceDTO dto = new SilenceDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(silence.getId());
      dto.setStatus(silence.getStatus());
      dto.setUpdatedAt(silence.getUpdatedAt());
      dto.setComment(silence.getComment());
      dto.setCreatedBy(silence.getCreatedBy());
      dto.setEndsAt(silence.getEndsAt());
      dto.setStartsAt(silence.getStartsAt());
      dto.setMatchers(silence.getMatchers());
    }
    return dto;
  }
  
  public SilenceDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Silence silence) {
    SilenceDTO dto = new SilenceDTO();
    uri(dto, uriInfo, silence);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(silence.getId());
      dto.setStatus(silence.getStatus());
      dto.setUpdatedAt(silence.getUpdatedAt());
      dto.setComment(silence.getComment());
      dto.setCreatedBy(silence.getCreatedBy());
      dto.setEndsAt(silence.getEndsAt());
      dto.setStartsAt(silence.getStartsAt());
      dto.setMatchers(silence.getMatchers());
    }
    return dto;
  }
  
  /**
   * Build a single Silence
   *
   * @param uriInfo
   * @param resourceRequest
   * @param uuid
   * @return
   */
  public SilenceDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, String uuid, Project project)
      throws AlertException {
    Silence silence;
    try {
      if (project != null) {
        silence = alertManager.getSilence(uuid, project)
            .orElseThrow(() -> new AlertException(RESTCodes.AlertErrorCode.SILENCE_NOT_FOUND, Level.FINE,
                "Silence with the given id was not found. id=" + uuid));
      } else {
        silence = alertManager.getSilence(uuid);
      }
    } catch (AlertManagerClientCreateException | AlertManagerUnreachableException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    }
    if (silence == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.SILENCE_NOT_FOUND, Level.FINE, "Silence with the given uuid " +
          "not found.");
    }
    return build(uriInfo, resourceRequest, silence);
  }
  
  /**
   * Build a list of Silences
   *
   * @param uriInfo
   * @param resourceRequest
   * @return
   */
  public SilenceDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, SilenceBeanParam silenceBeanParam,
      Project project)
      throws AlertException {
    return items(new SilenceDTO(), uriInfo, resourceRequest, silenceBeanParam, project);
  }
  
  private SilenceDTO items(SilenceDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
      SilenceBeanParam silenceBeanParam, Project project)
      throws AlertException {
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      List<Silence> silenceList;
      try {
        if (project != null) {
          silenceList = alertManager.getSilences(silenceBeanParam.getSilenceFilterBy().getFilter(), project);
        } else {
          silenceList = alertManager.getSilences(silenceBeanParam.getSilenceFilterBy().getFilter());
        }
      } catch (AlertManagerClientCreateException | AlertManagerUnreachableException e) {
        throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
      } catch (AlertManagerResponseException e) {
        throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
      } catch (AlertManagerAccessControlException e) {
        throw new AlertException(RESTCodes.AlertErrorCode.ACCESS_CONTROL_EXCEPTION, Level.FINE, e.getMessage());
      }
      if (!Strings.isNullOrEmpty(silenceBeanParam.getSilenceFilterBy().getStatus())) {
        silenceList = silenceList.stream()
            .filter((s) -> s.getStatus().getState().equals(silenceBeanParam.getSilenceFilterBy().getStatus()))
            .collect(Collectors.toList());
      }
      if (!Strings.isNullOrEmpty(silenceBeanParam.getSilenceFilterBy().getCreatedBy())) {
        silenceList = silenceList.stream()
            .filter((s) -> s.getCreatedBy().equals(silenceBeanParam.getSilenceFilterBy().getCreatedBy()))
            .collect(Collectors.toList());
      }
      dto.setCount((long) silenceList.size());
      return items(dto, uriInfo, resourceRequest, silenceList, silenceBeanParam);
    }
    return dto;
  }
  
  private SilenceDTO items(SilenceDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, List<Silence> silences,
      SilenceBeanParam silenceBeanParam) {
    if (silences != null && !silences.isEmpty()) {
      silences.forEach((silence) -> dto.addItem(buildItems(uriInfo, resourceRequest, silence)));
      if (silenceBeanParam.getSortBySet() != null && !silenceBeanParam.getSortBySet().isEmpty()) {
        SilenceComparator silenceComparator = new SilenceComparator(silenceBeanParam.getSortBySet());
        dto.getItems().sort(silenceComparator);
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
  
  class SilenceComparator implements Comparator<SilenceDTO> {
    
    Set<SilenceSortBy> sortBy;
  
    SilenceComparator(Set<SilenceSortBy> sortBy) {
      this.sortBy = sortBy;
    }
    
    private int compare(SilenceDTO a, SilenceDTO b, SilenceSortBy sortBy) {
      switch (sortBy.getSortBy()) {
        case CREATED_BY:
          return order(a.getCreatedBy(), b.getCreatedBy(), sortBy.getParam());
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
    public int compare(SilenceDTO a, SilenceDTO b) {
      Iterator<SilenceSortBy> sort = sortBy.iterator();
      int c = compare(a, b, sort.next());
      for (; sort.hasNext() && c == 0;) {
        c = compare(a, b, sort.next());
      }
      return c;
    }
  }
}