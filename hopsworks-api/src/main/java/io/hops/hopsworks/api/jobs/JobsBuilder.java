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
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.core.UriInfo;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Stateless
public class JobsBuilder {
  
  @EJB
  private JobFacade jobFacade;
  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private ExecutionsBuilder executionsBuilder;
  
  public JobDTO uri(JobDTO dto, UriInfo uriInfo, Jobs job) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceProperties.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(job.getProject().getId()))
      .path(ResourceProperties.Name.JOBS.toString().toLowerCase())
      .path(job.getName())
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
    JobDTO dto = new JobDTO();
    uri(dto, uriInfo, job);
    expand(dto, resourceProperties);
    if (dto.isExpand()) {
      dto.setId(job.getId());
      dto.setName(job.getName());
      dto.setCreationTime(job.getCreationTime());
      dto.setConfig(job.getJobConfig());
      dto.setType(job.getJobType());
      dto.setCreator(usersBuilder.build(uriInfo, resourceProperties, job.getCreator()));
      dto.setExecutions(executionsBuilder.build(uriInfo, resourceProperties, job));
    }
    return dto;
  }
  
  public JobDTO build(UriInfo uriInfo, ResourceProperties resourceProperties, Project project) {
    ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.JOBS);
    List<Jobs> jobs;
    if (property.getOffset() != null || property.getLimit() != null || property.getFilter() != null) {
      jobs = jobFacade.findByProject(property.getOffset(), property.getLimit(), property.getFilter(),
        property.getSort(), project);
      return items(new JobDTO(), uriInfo, resourceProperties, jobs, false);
    }
    jobs = jobFacade.findByProject(project);
    return items(new JobDTO(), uriInfo, resourceProperties, jobs, true);
  }
  
  private JobDTO items(JobDTO dto, UriInfo uriInfo, ResourceProperties resourceProperties, List<Jobs> jobs,
    boolean sort) {
    if (jobs != null && !jobs.isEmpty()) {
      if (sort) {
        ResourceProperties.ResourceProperty property = resourceProperties.get(ResourceProperties.Name.JOBS);
        //Sort collection and return elements based on offset, limit, sortBy, orderBy
        Comparator<Jobs> comparator = getComparator(property);
        if (comparator != null) {
          jobs.sort(comparator);
        }
      }
      jobs.forEach((job) -> {
        dto.addItem(build(uriInfo, resourceProperties, job));
      });
    }
    return dto;
  }
  
  public Comparator<Jobs> getComparator(ResourceProperties.ResourceProperty property) {
    Set<JobFacade.SortBy> sortBy = (Set<JobFacade.SortBy>) property.getSort();
    if (property.getSort() != null && !property.getSort().isEmpty()) {
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
      switch (sortBy) {
        case ID:
          return order(a.getId(), b.getId(), sortBy.getParam());
        case NAME:
          return order(a.getName(), b.getName(), sortBy.getParam());
        case DATE_CREATED:
          return order(a.getCreationTime(), b.getCreationTime(), sortBy.getParam());
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
