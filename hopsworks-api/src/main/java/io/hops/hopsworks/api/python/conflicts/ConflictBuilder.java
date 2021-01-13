/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.python.conflicts;

import com.google.common.base.Strings;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServiceEnum;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ConflictBuilder {

  @EJB
  private EnvironmentController environmentController;
  @EJB
  private Settings settings;

  public ConflictDTO uri(ConflictDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
        .build());
    return dto;
  }

  public ConflictDTO expand(ConflictDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.CONFLICTS)) {
      dto.setExpand(true);
    }
    return dto;
  }

  public ConflictDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project)
      throws IOException, ServiceDiscoveryException {
    ConflictDTO dto = new ConflictDTO();
    uri(dto, uriInfo, project);
    expand(dto, resourceRequest);
    if(dto.isExpand()) {
      ArrayList<String> conflicts = new ArrayList<>();
      if(!Strings.isNullOrEmpty(project.getPythonEnvironment().getConflicts())) {
        conflicts.addAll(Arrays.asList(project.getPythonEnvironment().getConflicts().split("\n")));
      }
      List<String> jupyterDeps = settings.getJupyterDependencies();
      for(String conflict: conflicts) {
        ConflictDTO conflictDTO = buildItem(conflict, jupyterDeps);
        if(!remove(conflictDTO, resourceRequest)) {
          dto.addItem(conflictDTO);
        }
      }
      if(dto.getItems() != null && dto.getItems().size() > 0) {
        dto.setCount((long) dto.getItems().size());
      }
    }
    return dto;
  }

  public ConflictDTO buildItem(String conflict, List<String> jupyterDeps) {
    ConflictDTO dto = new ConflictDTO();
    dto.setConflict(conflict);
    String conflictedLibrary = dto.getConflict().split("\\s+")[0];
    if(jupyterDeps.contains(conflictedLibrary)) {
      dto.setService(ProjectServiceEnum.JUPYTER);
    }
    return dto;
  }

  public boolean remove(ConflictDTO dto, ResourceRequest resourceRequest) {
    Set<AbstractFacade.FilterBy> filterBy = (Set<AbstractFacade.FilterBy>) resourceRequest.getFilter();
    if(filterBy == null || filterBy.isEmpty()) {
      return false;
    }

    for(AbstractFacade.FilterBy filter: filterBy) {
      if(filter.getParam().equalsIgnoreCase(Filters.SERVICE.name())) {
        if (dto.getService() != null && dto.getService().name().equalsIgnoreCase(filter.getValue())) {
          return false;
        }
      }
    }
    return true;
  }

  protected enum Filters {
    SERVICE
  }
}
