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
package io.hops.hopsworks.api.python.library;

import io.hops.hopsworks.api.python.command.CommandBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.python.LibraryFacade;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.PythonDep;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LibraryBuilder {
  
  private static final Logger LOGGER = Logger.getLogger(LibraryBuilder.class.getName());
  @EJB
  private CommandBuilder commandBuilder;
  @EJB
  private LibraryFacade libraryFacade;

  /**
   * @param dto
   * @param uriInfo
   * @return uri to single library
   */
  public LibraryDTO uri(LibraryDTO dto, UriInfo uriInfo, PythonDep dep) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .path(dep.getDependency())
      .build());
    return dto;
  }
  
  public LibraryDTO uri(LibraryDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .build());
    return dto;
  }
  
  public LibraryDTO uriItems(LibraryDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(project.getId().toString())
      .path(ResourceRequest.Name.ENVIRONMENTS.toString())
      .path(project.getPythonVersion())
      .path(ResourceRequest.Name.LIBRARIES.toString())
      .build());
    return dto;
  }
  
  public LibraryDTO uriItems(LibraryDTO dto, UriInfo uriInfo, PythonDep dep, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(project.getId().toString())
      .path(ResourceRequest.Name.ENVIRONMENTS.toString())
      .path(project.getPythonVersion())
      .path(ResourceRequest.Name.LIBRARIES.toString())
      .path(dep.getDependency())
      .build());
    return dto;
  }

  public LibraryDTO expand(LibraryDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.LIBRARIES)) {
      dto.setExpand(true);
    }
    return dto;
  }

  public LibraryDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    return buildItems(new LibraryDTO(), uriInfo, resourceRequest, project);
  }
  
  public LibraryDTO buildExpansionItem(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    return buildExpansionItem(new LibraryDTO(), uriInfo, resourceRequest, project);
  }
  
  private LibraryDTO buildItems(LibraryDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = libraryFacade.findInstalledPythonDepsByProject(resourceRequest.
        getOffset(), resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort(), project);
      dto.setCount(collectionInfo.getCount());
      return items(dto, uriInfo, resourceRequest, collectionInfo.getItems(), project);
    }
    return dto;
  }
  
  private LibraryDTO buildExpansionItem(LibraryDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    Project project) {
    uriItems(dto, uriInfo, project);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = libraryFacade.findInstalledPythonDepsByProject(resourceRequest.
        getOffset(), resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort(), project);
      dto.setCount(collectionInfo.getCount());
      return buildItems(dto, uriInfo, resourceRequest, collectionInfo.getItems(), project);
    }
    return dto;
  }

  public LibraryDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, PythonDep dep, Project project) {
    LibraryDTO dto = new LibraryDTO();
    uriItems(dto, uriInfo, dep, project);
    return buildDTO(dto, uriInfo, resourceRequest, dep, project);
  }
  
  public LibraryDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, PythonDep dep, Project project) {
    LibraryDTO dto = new LibraryDTO();
    uri(dto, uriInfo, dep);
    return buildDTO(dto, uriInfo, resourceRequest, dep, project);
  }
  
  public LibraryDTO buildItem(UriInfo uriInfo, ResourceRequest resourceRequest, PythonDep dep, Project project) {
    LibraryDTO dto = new LibraryDTO();
    uri(dto, uriInfo);
    return buildDTO(dto, uriInfo, resourceRequest, dep, project);
  }
  
  private LibraryDTO buildDTO(LibraryDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, PythonDep dep,
    Project project) {
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setLibrary(dep.getDependency());
      dto.setVersion(dep.getVersion());
      dto.setChannel(dep.getRepoUrl().getUrl());
      dto.setPackageManager(LibraryDTO.PackageManager.valueOf(dep.getInstallType().name()));
      //dto.setStatus(dep.getStatus());
      dto.setMachine(dep.getMachineType());
      dto.setPreinstalled(Boolean.toString(dep.isPreinstalled()));
      dto.setCommands(commandBuilder.buildItems(uriInfo, resourceRequest.get(ResourceRequest.Name.COMMANDS), project,
        dep.getDependency()));
    }
    return dto;
  }

  private LibraryDTO buildItems(LibraryDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, List<PythonDep> deps,
      Project project) {
    if (deps != null && !deps.isEmpty()) {
      deps.forEach(( dep ) -> dto.addItem(buildItems(uriInfo, resourceRequest, dep, project)));
    }
    return dto;
  }
  
  private LibraryDTO items(LibraryDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, List<PythonDep> deps,
    Project project) {
    if (deps != null && !deps.isEmpty()) {
      deps.forEach(( dep ) -> dto.addItem(build(uriInfo, resourceRequest, dep, project)));
    }
    return dto;
  }

}
