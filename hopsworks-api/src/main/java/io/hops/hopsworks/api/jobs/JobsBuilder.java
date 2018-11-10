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
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.UriInfo;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@Stateless
public class JobsBuilder {
  
  @EJB
  private JobFacade jobFacade;
  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private ExecutionsBuilder executionsBuilder;
  
  public JobDTO uri(JobDTO dto, UriInfo uriInfo, Jobs job) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceProperties.Name.PROJECTS.toString().toLowerCase())
      .path(Integer.toString(job.getProject().getId()))
      .path(ResourceProperties.Name.JOBS.toString().toLowerCase())
      .path(Integer.toString(job.getId()))
      .build());
    return dto;
  }
  
  public JobDTO expand(JobDTO dto, ResourceProperties resourceProperties) {
    if (resourceProperties != null) {
      ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.JOBS);
      if (property != null) {
        dto.setExpand(true);
      }
    }
    return dto;
  }
  
  public JobDTO build(UriInfo uriInfo, ResourceProperties resourceProperties, Jobs job) {
    if(job == null){
      throw new IllegalArgumentException("job parameter was null.");
    }
    JobDTO dto = new JobDTO();
    uri(dto, uriInfo, job);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      dto.setId(job.getId());
      dto.setName(job.getName());
      dto.setCreationTime(job.getCreationTime());
      dto.setConfig(job.getJobConfig());
      dto.setType(job.getJobType());
      dto.setCreator(usersBuilder.build(job.getCreator(), uriInfo, resourceProperties));
      dto.setExecutions(executionsBuilder.build(uriInfo, resourceProperties, job));
    }
    return dto;
  }
  
  public JobDTO build(UriInfo uriInfo, ResourceProperties resourceProperties, Project project, JobType
    type) {
    List<Jobs> jobs;
    if (type != null) {
      jobs = jobFacade.findJobsForProjectAndType(project, EnumSet.of(type),
        resourceProperties.get(ResourceProperties.Name.JOBS).getOffset(),
        resourceProperties.get(ResourceProperties.Name.JOBS).getLimit());
    } else {
      jobs = jobFacade.findForProject(project,
        resourceProperties.get(ResourceProperties.Name.JOBS).getOffset(),
        resourceProperties.get(ResourceProperties.Name.JOBS).getLimit());
    }
    return build(new JobDTO(), uriInfo, resourceProperties, jobs);
  }
  
  public JobDTO build(JobDTO dto, UriInfo uriInfo, ResourceProperties resourceProperties, List<Jobs> jobs) {
    if (jobs != null && !jobs.isEmpty()) {
      ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.JOBS);
      //Sort collection and return elements based on offset, limit, sortBy, orderBy
      jobs.sort(getComparator(property));
      jobs.forEach((job) -> {
        dto.addItem(build(uriInfo, resourceProperties, job));
      });
    }
    return dto;
  }
  
  public Comparator<Jobs> getComparator(ResourceProperties.ResourceProperty property) {
    if (property.getSortBy() != null) {
      switch (property.getSortBy()) {
        case ID:
          return new JobComparatorById(property.getOrderBy());
        case NAME:
          return new JobComparatorByName(property.getOrderBy());
        default:
          break;
      }
    }
    return new JobComparatorById(property.getOrderBy());
  }
  
  class JobComparatorById implements Comparator<Jobs> {
    ResourceProperties.OrderBy orderByAsc = ResourceProperties.OrderBy.ASC;
    
    JobComparatorById(ResourceProperties.OrderBy orderByAsc) {
      if (orderByAsc != null) {
        this.orderByAsc = orderByAsc;
      }
    }
    
    @Override
    public int compare(Jobs a, Jobs b) {
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
  
  class JobComparatorByName implements Comparator<Jobs> {
    ResourceProperties.OrderBy orderByAsc = ResourceProperties.OrderBy.ASC;
    
    JobComparatorByName(ResourceProperties.OrderBy orderByAsc) {
      if (orderByAsc != null) {
        this.orderByAsc = orderByAsc;
      }
    }
    
    @Override
    public int compare(Jobs a, Jobs b) {
      switch (orderByAsc) {
        case ASC:
          return a.getName().compareTo(b.getName());
        case DESC:
          return b.getName().compareTo(a.getName());
        default:
          break;
      }
      throw new UnsupportedOperationException("Order By " + orderByAsc + " not supported");
    }
  }
  
}
