/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.git;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.git.GitRepositoryRemotesFacade;
import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.git.GitRepositoryRemote;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class GitRepositoryRemoteBuilder {
  @EJB
  private GitRepositoryRemotesFacade gitRepositoryRemotesFacade;

  public URI uri(UriInfo uriInfo, Project project, GitRepository repository) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.GIT.toString())
        .path(ResourceRequest.Name.REPOSITORY.toString())
        .path(Integer.toString(repository.getId()))
        .path(ResourceRequest.Name.REMOTE.toString())
        .build();
  }

  public URI uri(UriInfo uriInfo, Project project, GitRepository repository, GitRepositoryRemote remote) {
    return uriInfo.getBaseUriBuilder()
        .path(ResourceRequest.Name.PROJECT.toString())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.GIT.toString())
        .path(ResourceRequest.Name.REPOSITORY.toString())
        .path(Integer.toString(repository.getId()))
        .path(ResourceRequest.Name.REMOTE.toString())
        .path(remote.getRemoteName())
        .build();
  }

  public boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.REMOTE);
  }

  public GitRepositoryRemoteDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                                      GitRepository repository, String remoteName) throws GitOpException {
    Optional<GitRepositoryRemote> optional = gitRepositoryRemotesFacade.findByNameAndRepository(repository, remoteName);
    GitRepositoryRemote remote = optional.orElseThrow(() ->
        new GitOpException(RESTCodes.GitOpErrorCode.REMOTE_NOT_FOUND, Level.FINE, "Remote with name [" + remoteName
            + "] not found"));
    return build(uriInfo, resourceRequest, project, repository, remote);
  }

  public GitRepositoryRemoteDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                                      GitRepository repository, GitRepositoryRemote remote) {
    GitRepositoryRemoteDTO dto = new GitRepositoryRemoteDTO();
    dto.setExpand(expand(resourceRequest));
    dto.setHref(uri(uriInfo, project, repository, remote));
    if (dto.isExpand()) {
      dto.setRemoteName(remote.getRemoteName());
      dto.setRemoteUrl(remote.getUrl());
    }
    return dto;
  }

  public GitRepositoryRemoteDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project,
                                      GitRepository repository) {
    GitRepositoryRemoteDTO repositoryRemoteDTO = new GitRepositoryRemoteDTO();
    repositoryRemoteDTO.setHref(uri(uriInfo, project, repository));
    repositoryRemoteDTO.setExpand(expand(resourceRequest));
    if (repositoryRemoteDTO.isExpand()) {
      List<GitRepositoryRemote> remotes = gitRepositoryRemotesFacade.findAllForRepository(repository);
      List<GitRepositoryRemoteDTO> dtos = new ArrayList<>();
      if (!remotes.isEmpty()) {
        dtos = remotes.stream().map(r -> build(uriInfo, resourceRequest, project, repository, r))
            .collect(Collectors.toList());
      }
      repositoryRemoteDTO.setItems(dtos);
      repositoryRemoteDTO.setCount((long)remotes.size());
    }
    return repositoryRemoteDTO;
  }
}
