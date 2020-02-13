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

import com.google.common.base.Strings;
import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.jobs.flink.FlinkMasterAddrCache;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;

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
public class ExecutionsBuilder {
  
  @EJB
  private ExecutionFacade executionFacade;
  @EJB
  private FlinkMasterAddrCache flinkMasterAddrCache;
  @EJB
  private UsersBuilder usersBuilder;
  
  
  /**
   * @param dto dto
   * @param uriInfo uriInfo
   * @param execution execution
   * @return uri to single execution
   */
  public ExecutionDTO uri(ExecutionDTO dto, UriInfo uriInfo, Execution execution) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(Integer.toString(execution.getJob().getProject().getId()))
      .path(ResourceRequest.Name.JOBS.toString())
      .path(execution.getJob().getName())
      .path(ResourceRequest.Name.EXECUTIONS.toString())
      .path(Integer.toString(execution.getId()))
      .build());
    return dto;
  }
  
  /**
   * @param dto dto
   * @param uriInfo uriInfo
   * @param job job
   * @return uri to all the executions of a job
   */
  public ExecutionDTO uri(ExecutionDTO dto, UriInfo uriInfo, Jobs job) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(Integer.toString(job.getProject().getId()))
      .path(ResourceRequest.Name.JOBS.toString())
      .path(job.getName())
      .path(ResourceRequest.Name.EXECUTIONS.toString())
      .build());
    return dto;
  }
  
  public ExecutionDTO expand(ExecutionDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && (resourceRequest.contains(ResourceRequest.Name.EXECUTIONS))) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public ExecutionDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Execution execution) {
    ExecutionDTO dto = new ExecutionDTO();
    uri(dto, uriInfo, execution);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(execution.getId());
      dto.setSubmissionTime(execution.getSubmissionTime());
      dto.setState(execution.getState());
      dto.setStdoutPath(execution.getStdoutPath());
      dto.setStderrPath(execution.getStderrPath());
      dto.setAppId(execution.getAppId());
      dto.setHdfsUser(execution.getHdfsUser());
      dto.setArgs(execution.getArgs());
      dto.setFinalStatus(execution.getFinalStatus());
      dto.setProgress(execution.getProgress());
      dto.setUser(usersBuilder.build(uriInfo, resourceRequest, execution.getUser()));
      dto.setFilesToRemove(execution.getFilesToRemove());
      dto.setDuration(execution.getExecutionDuration());
      // Get Flink Master URL if current execution hasn't finished
      if ((execution.getJob().getJobType() == JobType.FLINK || execution.getJob().getJobType() == JobType.BEAM_FLINK) &&
        !execution.getState().isFinalState() &&
        !Strings.isNullOrEmpty(execution.getAppId())) {
        String addr = flinkMasterAddrCache.get(execution.getAppId());
        if (!Strings.isNullOrEmpty(addr)) {
          dto.setFlinkMasterURL(addr);
        }
      }
    }
    
    return dto;
  }
  
  public ExecutionDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Jobs job) {
    ExecutionDTO dto = new ExecutionDTO();
    uri(dto, uriInfo, job);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = executionFacade.findByJob(resourceRequest.getOffset(),
        resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort(), job);
      dto.setCount(collectionInfo.getCount());
      collectionInfo.getItems().forEach((exec) -> dto.addItem(build(uriInfo, resourceRequest, (Execution) exec)));
    }
    return dto;
  }
  
  public Comparator<Execution> getComparator(ResourceRequest resourceRequest) {
    Set<ExecutionFacade.SortBy> sortBy = (Set<ExecutionFacade.SortBy>) resourceRequest.getSort();
    if (resourceRequest.getSort() != null && !resourceRequest.getSort().isEmpty()) {
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
      switch (ExecutionFacade.Sorts.valueOf(sortBy.getValue())) {
        case ID:
          return order(a.getId(), b.getId(), sortBy.getParam());
        case SUBMISSIONTIME:
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

