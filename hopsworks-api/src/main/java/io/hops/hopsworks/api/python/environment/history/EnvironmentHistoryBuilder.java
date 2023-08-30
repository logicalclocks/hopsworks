/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.python.environment.history;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.python.EnvironmentHistoryFacade;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.python.environment.EnvironmentHistoryController;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.history.EnvironmentDelta;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class EnvironmentHistoryBuilder {

  @EJB
  private EnvironmentHistoryFacade environmentHistoryFacade;
  @EJB
  private UsersBuilder usersBuilder;
  @EJB
  private EnvironmentController environmentController;
  @EJB
  private EnvironmentHistoryController environmentHistoryController;

  private final Integer DEFAULT_ENV_DIFFS_LIMIT = 20;

  public URI uri(UriInfo uriInfo, Project project, String version, Integer id) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString())
        .path(project.getId().toString())
        .path(ResourceRequest.Name.ENVIRONMENTS.toString())
        .path(version)
        .path(ResourceRequest.Name.ENVIRONMENT_HISTORY.toString())
        .path(String.valueOf(id))
        .build();
  }

  public URI uri(UriInfo uriInfo, Project project, String version) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString())
        .path(project.getId().toString())
        .path(ResourceRequest.Name.ENVIRONMENTS.toString())
        .path(version)
        .path(ResourceRequest.Name.ENVIRONMENT_HISTORY.toString())
        .build();
  }

  public boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.ENVIRONMENT_HISTORY);
  }

  public EnvironmentHistoryDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                                     String version) throws ServiceException {
    EnvironmentHistoryDTO dto = new EnvironmentHistoryDTO();
    dto.setHref(uri(uriInfo, project, version));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      Integer limit = resourceRequest.getLimit() == null ? DEFAULT_ENV_DIFFS_LIMIT : resourceRequest.getLimit();
      Integer offset = resourceRequest.getOffset() == null ? 0 : resourceRequest.getOffset();
      AbstractFacade.CollectionInfo<EnvironmentDelta> envDiffs =
          environmentHistoryFacade.getAll(project, resourceRequest.getFilter(), resourceRequest.getSort(),
              limit, offset);
      List<EnvironmentHistoryDTO> dtos = envDiffs.getItems().stream().map(e ->
      {
        try {
          return build(uriInfo, resourceRequest, e);
        } catch (ServiceException ex) {
          throw new RuntimeException(ex);
        }
      }).collect(Collectors.toList());
      dto.setItems(dtos);
      dto.setCount(envDiffs.getCount());
    }
    return dto;
  }

  public EnvironmentHistoryDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Integer buildId)
      throws ServiceException {
    Optional<EnvironmentDelta> optional = environmentHistoryFacade.getById(project, buildId);
    if (!optional.isPresent()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ENVIRONMENT_BUILD_NOT_FOUND,
          Level.FINE, "Build with id: " + buildId + " no found in project environment history");
    }
    return build(uriInfo, resourceRequest, optional.get());
  }

  public EnvironmentHistoryDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                                     EnvironmentDelta environmentDelta) throws ServiceException {
    EnvironmentHistoryDTO dto = new EnvironmentHistoryDTO();
    dto.setHref(uri(uriInfo, environmentDelta.getProject(),
        environmentDelta.getProject().getPythonEnvironment().getPythonVersion(), environmentDelta.getId()));
    dto.setExpand(expand(resourceRequest));
    if (dto.isExpand()) {
      ObjectMapper objectMapper = new ObjectMapper();
      dto.setId(environmentDelta.getId());
      dto.setEnvironmentName(environmentDelta.getDockerImage());
      dto.setPreviousEnvironment(environmentDelta.getPreviousDockerImage());
      dto.setDateCreated(environmentDelta.getCreated());
      try {
        dto.setInstalled(objectMapper.readValue(environmentDelta.getInstalled().toString(), List.class));
        dto.setUninstalled(objectMapper.readValue(environmentDelta.getUninstalled().toString(), List.class));
        dto.setDowngraded(objectMapper.readValue(environmentDelta.getDowngraded().toString(), List.class));
        dto.setUpgraded(objectMapper.readValue(environmentDelta.getUpgraded().toString(), List.class));
        dto.setInstalledLibraries(environmentHistoryController.getDependencies(environmentDelta.getProject(),
            environmentDelta.getUser(), environmentDelta.getDockerImage()));
      } catch (JsonProcessingException e) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ENVIRONMENT_HISTORY_READ_ERROR,
            Level.FINE, "Failed to parse environment build " + environmentDelta.getDockerImage());
      }

      dto.setYamlFilePath(environmentController.getDockerImageEnvironmentFile(environmentDelta.getProject(),
          environmentDelta.getDockerImage()));
      dto.setUser(
          usersBuilder.build(uriInfo, resourceRequest.get(ResourceRequest.Name.CREATOR), environmentDelta.getUser()));
    }
    return dto;
  }
}
