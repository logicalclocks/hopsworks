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
package io.hops.hopsworks.api.python.command;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CommandBuilder {
  
  @EJB
  private CondaCommandFacade condaCommandFacade;
  
  /**
   * @param dto
   * @param uriInfo
   * @return
   */
  public CommandDTO uri(CommandDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder().build());
    return dto;
  }
  
  public CommandDTO uriItems(CommandDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(project.getId().toString())
      .path(ResourceRequest.Name.ENVIRONMENTS.toString())
      .path(project.getPythonVersion())
      .path(ResourceRequest.Name.COMMANDS.toString())
      .build());
    return dto;
  }
  
  public CommandDTO uriItems(CommandDTO dto, UriInfo uriInfo, CondaCommands command, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(project.getId().toString())
      .path(ResourceRequest.Name.ENVIRONMENTS.toString())
      .path(project.getPythonVersion())
      .path(ResourceRequest.Name.COMMANDS.toString())
      .path(Integer.toString(command.getId()))
      .build());
    return dto;
  }
  
  public CommandDTO uriItems(CommandDTO dto, UriInfo uriInfo, Project project, String libName) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(project.getId().toString())
      .path(ResourceRequest.Name.ENVIRONMENTS.toString())
      .path(project.getPythonVersion())
      .path(ResourceRequest.Name.LIBRARIES.toString())
      .path(libName)
      .path(ResourceRequest.Name.COMMANDS.toString())
      .build());
    return dto;
  }
  
  public CommandDTO uriItems(CommandDTO dto, UriInfo uriInfo, Project project, String libName, CondaCommands command) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(project.getId().toString())
      .path(ResourceRequest.Name.ENVIRONMENTS.toString())
      .path(project.getPythonVersion())
      .path(ResourceRequest.Name.LIBRARIES.toString())
      .path(libName)
      .path(ResourceRequest.Name.COMMANDS.toString())
      .path(Integer.toString(command.getId()))
      .build());
    return dto;
  }
  
  public CommandDTO expand(CommandDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && (resourceRequest.contains(ResourceRequest.Name.COMMANDS))) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public CommandDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, CondaCommands command) {
    CommandDTO dto = new CommandDTO();
    uri(dto, uriInfo);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setStatus(command.getStatus().name());
      dto.setOp(command.getOp().name());
      dto.setHost(command.getHostId().getHostname());
    }
    return dto;
  }
  
  public CommandDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, CondaCommands command,
    Project project) {
    CommandDTO dto = new CommandDTO();
    uriItems(dto, uriInfo, command, project);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setStatus(command.getStatus().name());
      dto.setOp(command.getOp().name());
      dto.setHost(command.getHostId().getHostname());
    }
    return dto;
  }
  
  public CommandDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, String libName,
    CondaCommands command) {
    CommandDTO dto = new CommandDTO();
    uriItems(dto, uriInfo, project, libName, command);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setStatus(command.getStatus().name());
      dto.setOp(command.getOp().name());
      dto.setHost(command.getHostId().getHostname());
    }
    return dto;
  }
  
  public CommandDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Integer id)
    throws PythonException {
    CondaCommands command = condaCommandFacade.findCondaCommand(id);
    if (command == null || !command.getProjectId().equals(project)) {
      throw new PythonException(RESTCodes.PythonErrorCode.CONDA_COMMAND_NOT_FOUND, Level.FINE);
    }
    return build(uriInfo, resourceRequest, command);
  }
  
  public CommandDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, String libName, Integer id)
    throws PythonException {
    CondaCommands command = condaCommandFacade.findCondaCommand(id);
    if (command == null || !command.getProjectId().equals(project) || !command.getLib().equals(libName)) {
      throw new PythonException(RESTCodes.PythonErrorCode.CONDA_COMMAND_NOT_FOUND, Level.FINE);
    }
    return build(uriInfo, resourceRequest, command);
  }
  
  public CommandDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    return items(new CommandDTO(), uriInfo, resourceRequest, project);
  }
  
  public CommandDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, String libName) {
    return items(new CommandDTO(), uriInfo, resourceRequest, project, libName);
  }
  
  public CommandDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, String libName,
    List<CondaCommands> commands) {
    return items(new CommandDTO(), uriInfo, resourceRequest, project, libName, commands);
  }
  
  private CommandDTO items(CommandDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    uriItems(dto, uriInfo, project);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo =
        condaCommandFacade.findAllEnvCmdByProject(resourceRequest.getOffset()
          , resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort(), project);
      dto.setCount(collectionInfo.getCount());
      return items(dto, uriInfo, resourceRequest, collectionInfo.getItems(), project);
    }
    return dto;
  }
  
  private CommandDTO items(CommandDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    String libName) {
    uriItems(dto, uriInfo, project, libName);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo =
        condaCommandFacade.findAllLibCmdByProject(resourceRequest.getOffset()
          , resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort(), project, libName);
      dto.setCount(collectionInfo.getCount());
      return items(dto, uriInfo, resourceRequest, collectionInfo.getItems(), project, libName);
    }
    return dto;
  }
  
  private CommandDTO items(CommandDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
    String libName, List<CondaCommands> commands) {
    uriItems(dto, uriInfo, project, libName);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      return items(dto, uriInfo, resourceRequest, commands, project, libName);
    }
    return dto;
  }
  
  private CommandDTO items(CommandDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    List<CondaCommands> commands, Project project) {
    if (commands != null && !commands.isEmpty()) {
      commands.forEach((command) -> dto.addItem(buildItems(uriInfo, resourceRequest, command, project)));
    }
    return dto;
  }
  
  private CommandDTO items(CommandDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    List<CondaCommands> commands, Project project, String libName) {
    if (commands != null && !commands.isEmpty()) {
      commands.forEach((command) -> dto.addItem(buildItems(uriInfo, resourceRequest, project, libName, command)));
    }
    return dto;
  }
  
  private CommandDTO items(CommandDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    List<CondaCommands> commands) {
    if (commands != null && !commands.isEmpty()) {
      commands.forEach((command) -> dto.addItem(build(uriInfo, resourceRequest, command)));
    }
    return dto;
  }
  
}
