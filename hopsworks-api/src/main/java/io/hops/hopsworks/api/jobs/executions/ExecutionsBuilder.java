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
package io.hops.hopsworks.api.jobs.executions;

import io.hops.hopsworks.api.jobs.executions.ExecutionDTO;
import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceProperties;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.UriInfo;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Stateless
public class ExecutionsBuilder {
  
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private UsersBuilder usersBuilder;
  
  
  /**
   * @param dto
   * @param uriInfo
   * @param execution
   * @return uri to single execution
   */
  public ExecutionDTO uri(ExecutionDTO dto, UriInfo uriInfo, Execution execution) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceProperties.Name.PROJECT.toString())
      .path(Integer.toString(execution.getJob().getProject().getId()))
      .path(ResourceProperties.Name.JOBS.toString())
      .path(execution.getJob().getName())
      .path(ResourceProperties.Name.EXECUTIONS.toString())
      .path(Integer.toString(execution.getId()))
      .build());
    return dto;
  }
  
  /**
   * @param dto
   * @param uriInfo
   * @param job
   * @return uri to all the executions of a job
   */
  public ExecutionDTO uri(ExecutionDTO dto, UriInfo uriInfo, Jobs job) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceProperties.Name.PROJECT.toString())
      .path(Integer.toString(job.getProject().getId()))
      .path(ResourceProperties.Name.JOBS.toString())
      .path(job.getName())
      .path(ResourceProperties.Name.EXECUTIONS.toString())
      .build());
    return dto;
  }
  
  public ExecutionDTO expand(ExecutionDTO dto, ResourceProperties resourceProperties) {
    if (resourceProperties != null && (resourceProperties.contains(ResourceProperties.Name.EXECUTIONS))) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public ExecutionDTO build(UriInfo uriInfo, ResourceProperties resourceProperties, Execution execution) {
    ExecutionDTO dto = new ExecutionDTO();
    uri(dto, uriInfo, execution);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      dto.setId(execution.getId());
      dto.setSubmissionTime(execution.getSubmissionTime());
      dto.setState(execution.getState());
      dto.setStdoutPath(execution.getStdoutPath());
      dto.setStderrPath(execution.getStderrPath());
      dto.setAppId(execution.getAppId());
      dto.setHdfsUser(execution.getHdfsUser());
      dto.setFinalStatus(execution.getFinalStatus());
      dto.setProgress(execution.getProgress());
      dto.setUser(usersBuilder.buildItem(uriInfo, resourceProperties, execution.getUser()));
      dto.setFilesToRemove(execution.getFilesToRemove());
      dto.setDuration(execution.getExecutionDuration());
    }
    return dto;
  }
  
  public ExecutionDTO build(UriInfo uriInfo, ResourceProperties resourceProperties, Jobs job) {
    ExecutionDTO dto = new ExecutionDTO();
    uri(dto, uriInfo, job);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.EXECUTIONS);
      List<Execution> executions;
      if (property.getOffset() != null || property.getLimit() != null || property.getFilter() != null) {
        executions = executionFacade.findByJob(property.getOffset(), property.getLimit(), property.getFilter(),
          property.getSort(), job);
        return items(new ExecutionDTO(), uriInfo, resourceProperties, executions, false);
      }
      executions = executionFacade.findByJob(job);
      return items(new ExecutionDTO(), uriInfo, resourceProperties, executions, true);
    }
    return dto;
  }
  
  private ExecutionDTO items(ExecutionDTO dto, UriInfo uriInfo, ResourceProperties resourceProperties,
    List<Execution> executions, boolean sort) {
    if (executions != null && !executions.isEmpty()) {
      if(sort) {
        ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.EXECUTIONS);
        //Sort collection and return elements based on offset, limit, sortBy, orderBy
        Comparator<Execution> comparator = getComparator(property);
        if (comparator != null) {
          executions.sort(comparator);
        }
      }
      executions.forEach((exec) -> {
        dto.addItem(build(uriInfo, resourceProperties, exec));
      });
    }
    return dto;
  }
  
  public Comparator<Execution> getComparator(ResourceProperties.ResourceProperty property) {
    Set<ExecutionFacade.SortBy> sortBy = (Set<ExecutionFacade.SortBy>) property.getSort();
    if (property.getSort() != null && !property.getSort().isEmpty()) {
      return new ExecutionsComparator(sortBy);
    }
    return null;
  }
  
  class ExecutionsComparator implements Comparator<Execution> {
    
    Set<ExecutionFacade.SortBy> sortBy;
  
    ExecutionsComparator(Set<ExecutionFacade.SortBy> sort) {
      this.sortBy = sort;
    }
    
    private int compare(Execution a, Execution b, ExecutionFacade.SortBy sortBy) {
      switch (sortBy) {
        case ID:
          return order(a.getId(), b.getId(), sortBy.getParam());
        case SUBMISSION_TIME:
          return order(a.getSubmissionTime(), b.getSubmissionTime(), sortBy.getParam());
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
          return a.compareTo(b);
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
    public int compare(Execution a, Execution b) {
      Iterator<ExecutionFacade.SortBy> sort = sortBy.iterator();
      int c = compare(a, b, sort.next());
      for (; sort.hasNext() && c == 0; ) {
        c = compare(a, b, sort.next());
      }
      return c;
    }
  }
  
}

