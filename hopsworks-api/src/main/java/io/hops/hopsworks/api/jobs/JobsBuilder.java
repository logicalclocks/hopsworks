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

import io.hops.hopsworks.api.jobs.executions.ExecutionsBuilder;
import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;

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
public class JobsBuilder {
  
  @EJB
  private JobFacade jobFacade;
  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private ExecutionsBuilder executionsBuilder;
  
  
  public JobDTO uri(JobDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.JOBS.toString().toLowerCase())
      .build());
    return dto;
  }
  
  public JobDTO uri(JobDTO dto, UriInfo uriInfo, Jobs job) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(job.getProject().getId()))
      .path(ResourceRequest.Name.JOBS.toString().toLowerCase())
      .path(job.getName())
      .build());
    return dto;
  }
  
  public JobDTO expand(JobDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.JOBS)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public JobDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Jobs job) {
    JobDTO dto = new JobDTO();
    uri(dto, uriInfo, job);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(job.getId());
      dto.setName(job.getName());
      dto.setCreationTime(job.getCreationTime());
      dto.setConfig(job.getJobConfig());
      dto.setJobType(job.getJobType());
      dto.setCreator(usersBuilder.build(uriInfo, resourceRequest.get(ResourceRequest.Name.CREATOR), job.getCreator()));
      dto.setExecutions(executionsBuilder.build(uriInfo, resourceRequest.get(ResourceRequest.Name.EXECUTIONS), job));
    }
    return dto;
  }
  
  public JobDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    JobDTO dto = new JobDTO();
    uri(dto, uriInfo, project);
    expand(dto, resourceRequest);
    if(dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = jobFacade.findByProject(resourceRequest.getOffset(),
        resourceRequest.getLimit(),
        resourceRequest.getFilter(),
        resourceRequest.getSort(), project);
      //set the count
      dto.setCount(collectionInfo.getCount());
      collectionInfo.getItems().forEach((job) -> dto.addItem(build(uriInfo, resourceRequest, (Jobs) job)));
    }
    return dto;
  }
  
  public Comparator<Jobs> getComparator(ResourceRequest resourceRequest) {
    Set<JobFacade.SortBy> sortBy = (Set<JobFacade.SortBy>) resourceRequest.getSort();
    if (resourceRequest.getSort() != null && !resourceRequest.getSort().isEmpty()) {
      return new JobsComparator(sortBy);
    }
    return null;
  }
  
  class JobsComparator implements Comparator<Jobs> {
    
    Set<JobFacade.SortBy> sortBy;
    
    JobsComparator(Set<JobFacade.SortBy> sort) {
      this.sortBy = sort;
    }
    
    private int compare(Jobs a, Jobs b, JobFacade.SortBy sortBy) {
      switch (JobFacade.Sorts.valueOf(sortBy.getValue())) {
        case ID:
          return order(a.getId(), b.getId(), sortBy.getParam());
        case NAME:
          return order(a.getName(), b.getName(), sortBy.getParam());
        case DATE_CREATED:
          return order(a.getCreationTime(), b.getCreationTime(), sortBy.getParam());
        case CREATOR:
          return order(a.getCreator().getFname() + " " + a.getCreator().getLname(),
            b.getCreator().getFname()  + " " + b.getCreator().getLname(),
            sortBy.getParam());
        case CREATOR_LAST_NAME:
          return order(a.getCreator().getLname(), b.getCreator().getLname(), sortBy.getParam());
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
    public int compare(Jobs a, Jobs b) {
      Iterator<JobFacade.SortBy> sort = sortBy.iterator();
      int c = compare(a, b, sort.next());
      for (; sort.hasNext() && c == 0; ) {
        c = compare(a, b, sort.next());
      }
      return c;
    }
  }
  
}
