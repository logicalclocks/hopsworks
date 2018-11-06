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
package io.hops.hopsworks.api.jobs;

import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceProperties;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
      .path(ResourceProperties.Name.PROJECTS.toString())
      .path(Integer.toString(execution.getJob().getProject().getId()))
      .path(ResourceProperties.Name.JOBS.toString())
      .path(Integer.toString(execution.getJob().getId()))
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
      .path(ResourceProperties.Name.PROJECTS.toString())
      .path(Integer.toString(job.getProject().getId()))
      .path(ResourceProperties.Name.JOBS.toString())
      .path(Integer.toString(job.getId()))
      .path(ResourceProperties.Name.EXECUTIONS.toString())
      .build());
    return dto;
  }
  
  public ExecutionDTO expand(ExecutionDTO dto, ResourceProperties resourceProperties) {
    if (resourceProperties != null) {
      ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.EXECUTIONS);
      if (property != null) {
        dto.setExpand(true);
      }
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
      dto.setUser(usersBuilder.build(execution.getUser(), uriInfo, resourceProperties));
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
      dto = build(dto, uriInfo, resourceProperties, job);
    }
    return dto;
  }
  
  
  public ExecutionDTO build(ExecutionDTO dto, UriInfo uriInfo, ResourceProperties resourceProperties, Jobs job) {
    ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.EXECUTIONS);
    List<Execution> executions = executionFacade.findForJob(job, property.getOffset(), property.getLimit());
    return build(dto, uriInfo, resourceProperties, executions);
  }
  
  public ExecutionDTO build(ExecutionDTO dto, UriInfo uriInfo, ResourceProperties resourceProperties, List<Execution>
    executions) {
    if (executions != null && !executions.isEmpty()) {
      ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.EXECUTIONS);
      //Sort collection and return elements based on offset, limit, sortBy, orderBy
      Collections.sort(executions, getComparator(property));
      
      executions.forEach((exec) -> {
        dto.addItem(build(uriInfo, resourceProperties, exec));
      });
    }
    return dto;
  }
  
  public Comparator<Execution> getComparator(ResourceProperties.ResourceProperty property) {
    if(property.getSortBy() != null) {
      switch (property.getSortBy()) {
        case ID:
          return new ExecutionComparatorById(property.getOrderBy());
        case NAME:
          throw new UnsupportedOperationException();
        default:
          break;
      }
    }
    return new ExecutionComparatorById(property.getOrderBy());
  }
  
  class ExecutionComparatorById implements Comparator<Execution> {
    ResourceProperties.OrderBy orderByAsc = ResourceProperties.OrderBy.ASC;
    
    ExecutionComparatorById(ResourceProperties.OrderBy orderByAsc){
      if(orderByAsc != null) {
        this.orderByAsc = orderByAsc;
      }
    }
    
    @Override
    public int compare(Execution a, Execution b) {
      switch (orderByAsc) {
        case ASC:
          return a.getSubmissionTime().compareTo(b.getSubmissionTime());
        case DESC:
          return b.getSubmissionTime().compareTo(a.getSubmissionTime());
        default:
          break;
      }
      throw new UnsupportedOperationException("Order By " + orderByAsc + " not supported");
    }
  }
}

