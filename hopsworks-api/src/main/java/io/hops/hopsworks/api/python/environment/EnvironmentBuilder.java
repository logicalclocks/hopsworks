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
package io.hops.hopsworks.api.python.environment;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.api.python.command.CommandBuilder;
import io.hops.hopsworks.api.python.conflicts.ConflictBuilder;
import io.hops.hopsworks.api.python.library.LibraryBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.python.library.LibraryController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EnvironmentBuilder {

  @EJB
  private LibraryBuilder librariesBuilder;
  @EJB
  private CommandBuilder commandBuilder;
  @EJB
  private ConflictBuilder conflictBuilder;
  @EJB
  private Settings settings;
  @EJB
  private LibraryController libraryController;

  public EnvironmentDTO uri(EnvironmentDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder().build());
    return dto;
  }
  
  public EnvironmentDTO uri(EnvironmentDTO dto, UriInfo uriInfo, Project project, String version) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(project.getId().toString())
      .path(ResourceRequest.Name.ENVIRONMENTS.toString())
      .path(version)
      .build());
    return dto;
  }
  
  public EnvironmentDTO expand(EnvironmentDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.ENVIRONMENTS)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public EnvironmentDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, String version)
      throws IOException, ServiceDiscoveryException {
    EnvironmentDTO dto = new EnvironmentDTO();
    uri(dto, uriInfo, project, version);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setPipSearchEnabled(libraryController.isPyPiIndexed());
      dto.setPythonConflicts(project.getPythonEnvironment().getConflicts() != null);
      dto.setCondaChannel(settings.getCondaDefaultRepo());
      dto.setPythonVersion(version);
      dto.setCommands(commandBuilder.buildItems(uriInfo, resourceRequest.get(ResourceRequest.Name.COMMANDS), project));
      dto.setLibraries(
        librariesBuilder.buildExpansionItem(uriInfo, resourceRequest.get(ResourceRequest.Name.LIBRARIES), project));
      dto.setConflicts(conflictBuilder.build(uriInfo, resourceRequest.get(ResourceRequest.Name.CONFLICTS), project));
    }
    return dto;
  }
  
  /**
   *
   * @param uriInfo
   * @param resourceRequest
   * @param project
   * @return
   */
  public EnvironmentDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Project project)
      throws IOException, ServiceDiscoveryException {
    EnvironmentDTO dto = new EnvironmentDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      List<String> envs =  new ArrayList<>();
      envs.add(project.getPythonEnvironment().getPythonVersion()); //Currently we only have one environment
      dto.setCount((long) envs.size());
      return buildItems(dto, uriInfo, resourceRequest, project, envs);
    }
    return dto;
  }
  
  private EnvironmentDTO buildItems(EnvironmentDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    Project project, List<String> envs) throws IOException, ServiceDiscoveryException {
    if (envs != null && !envs.isEmpty()) {
      for (String env : envs) {
        dto.addItem(build(uriInfo, resourceRequest, project, env));
      }
    }
    return dto;
  }
  
}
