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

import com.google.common.base.Strings;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.git.branch.BranchBuilder;
import io.hops.hopsworks.api.git.branch.BranchDTO;
import io.hops.hopsworks.api.git.execution.ExecutionBeanParam;
import io.hops.hopsworks.api.git.execution.GitExecutionResource;
import io.hops.hopsworks.api.git.execution.GitOpExecutionBuilder;
import io.hops.hopsworks.api.git.execution.GitOpExecutionDTO;
import io.hops.hopsworks.api.git.remote.GitRepositoryRemoteBuilder;
import io.hops.hopsworks.api.git.remote.GitRepositoryRemoteDTO;
import io.hops.hopsworks.api.git.repository.GitRepositoryBuilder;
import io.hops.hopsworks.api.git.repository.GitRepositoryDTO;
import io.hops.hopsworks.api.git.repository.RepositoryBeanParam;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.git.BranchCommits;
import io.hops.hopsworks.common.git.CloneCommandConfiguration;
import io.hops.hopsworks.common.git.GitController;
import io.hops.hopsworks.common.git.GitRemotesAction;
import io.hops.hopsworks.common.git.GitBranchAction;
import io.hops.hopsworks.common.git.GitRepositoryAction;
import io.hops.hopsworks.common.git.GitCommitDTO;
import io.hops.hopsworks.common.git.util.GitCommandConfigurationValidator;
import io.hops.hopsworks.common.git.RepositoryActionCommandConfiguration;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Logger;

@Api(value = "Git Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class GitResource {
  private static final Logger LOGGER = Logger.getLogger(GitResource.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private GitController gitController;
  @EJB
  private GitOpExecutionBuilder executionBuilder;
  @EJB
  private GitCommitsBuilder gitCommitsBuilder;
  @EJB
  private BranchBuilder branchBuilder;
  @EJB
  private GitRepositoryBuilder gitRepositoryBuilder;
  @Inject
  private GitExecutionResource gitExecutionResource;
  @EJB
  private GitRepositoryRemoteBuilder gitRepositoryRemoteBuilder;
  @EJB
  private GitCommandConfigurationValidator commandConfigurationValidator;

  private Project project;

  public GitResource(){}

  public void setProjectId(Integer projectId) {
    this.project = this.projectFacade.find(projectId);
  }

  @ApiOperation(value = "Get all the repositories in a project", response = GitRepositoryDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response gitRepositories(@Context UriInfo uriInfo,
                                  @Context SecurityContext sc,
                                  @BeanParam Pagination pagination,
                                  @BeanParam RepositoryBeanParam repositoryBeanParam) {
    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.REPOSITORY);
    resourceRequest.setExpansions(repositoryBeanParam.getExpansions().getResources());
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setFilter(repositoryBeanParam.getFilter());
    resourceRequest.setSort(repositoryBeanParam.getSortBySet());
    GitRepositoryDTO repositories = gitRepositoryBuilder.build(uriInfo, resourceRequest, project, hopsworksUser);
    return Response.ok().entity(repositories).build();
  }

  @ApiOperation(value = "Get a repository with a particular Id", response = GitRepositoryDTO.class)
  @GET
  @Path("/repository/{repositoryId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response gitRepository(@PathParam("repositoryId") Integer repositoryId,
                                @Context SecurityContext sc,
                                @Context UriInfo uriInfo,
                                @BeanParam RepositoryBeanParam repositoryBeanParam) throws GitOpException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.REPOSITORY);
    resourceRequest.setExpansions(repositoryBeanParam.getExpansions().getResources());
    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    GitRepository gitRepository = commandConfigurationValidator.verifyRepository(project, hopsworksUser, repositoryId);
    GitRepositoryDTO dto = gitRepositoryBuilder.build(uriInfo, resourceRequest, project, gitRepository);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Clone a git repository", response = GitOpExecutionDTO.class)
  @POST
  @Path("/clone")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response clone(CloneCommandConfiguration commandDTO,
                        @Context SecurityContext sc,
                        @Context UriInfo uriInfo,
                        @BeanParam ExecutionBeanParam executionBeanParam)
      throws GitOpException, HopsSecurityException, IllegalArgumentException, UserException, DatasetException {
    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    GitOpExecution execution = gitController.clone(commandDTO, project, hopsworksUser);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXECUTION);
    resourceRequest.setExpansions(executionBeanParam.getExpansions().getResources());
    GitOpExecutionDTO dto = executionBuilder.build(uriInfo, resourceRequest, execution);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Perform a git repository action: commit, pull, push, status",
      response = GitOpExecutionDTO.class)
  @POST
  @Path("/repository/{repositoryId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response executeRepositoryAction(@PathParam("repositoryId") Integer repository,
                                          @QueryParam("action") GitRepositoryAction action,
                                          RepositoryActionCommandConfiguration configuration,
                                          @Context SecurityContext sc,
                                          @Context UriInfo uriInfo,
                                          @BeanParam ExecutionBeanParam executionBeanParam)
      throws GitOpException, HopsSecurityException, IllegalArgumentException {
    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    GitOpExecution execution = gitController.executeRepositoryAction(configuration, project, hopsworksUser, action,
        repository);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXECUTION);
    resourceRequest.setExpansions(executionBeanParam.getExpansions().getResources());
    GitOpExecutionDTO dto = executionBuilder.build(uriInfo, resourceRequest, execution);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Get branches for the given repository", response = BranchDTO.class)
  @GET
  @Path("/repository/{repositoryId}/branch")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getRepositoryBranches(@Context UriInfo uriInfo,
                                        @Context SecurityContext sc,
                                        @BeanParam Pagination pagination,
                                        @PathParam("repositoryId") Integer repositoryId) throws GitOpException {
    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, hopsworksUser, repositoryId);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.BRANCH);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    BranchDTO repositoryBranches = branchBuilder.build(uriInfo, resourceRequest, project, repository);
    return Response.ok().entity(repositoryBranches).build();
  }

  @ApiOperation(value = "Executes a git branch --option command action: add, checkout, delete",
      response = GitOpExecutionDTO.class)
  @POST
  @Path("/repository/{repositoryId}/branch")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response branch(@PathParam("repositoryId") Integer repositoryId,
                         @QueryParam("action") GitBranchAction action,
                         @QueryParam("branchName") String branchName,
                         @QueryParam("commit") String commit,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc,
                         @Context UriInfo uriInfo,
                         @BeanParam ExecutionBeanParam executionBeanParam)
      throws IllegalArgumentException, GitOpException, HopsSecurityException {
    if (action == null) {
      throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.INVALID_BRANCH_ACTION.getMessage());
    }
    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    GitOpExecution execution = gitController.executeBranchAction(action, project, hopsworksUser, repositoryId,
        branchName, commit);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXECUTION);
    resourceRequest.setExpansions(executionBeanParam.getExpansions().getResources());
    GitOpExecutionDTO dto = executionBuilder.build(uriInfo, resourceRequest,
        execution);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Gets the branch commits. Similar to git log --oneline", response = GitCommitDTO.class)
  @GET
  @Path("/repository/{repositoryId}/branch/{branchName}/commit")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getBranchCommits(@Context UriInfo uriInfo,
                                   @Context SecurityContext sc,
                                   @BeanParam Pagination pagination,
                                   @PathParam("repositoryId") Integer repositoryId,
                                   @PathParam("branchName") String branchName) throws GitOpException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.COMMIT);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, hopsworksUser, repositoryId);
    GitCommitDTO commits = gitCommitsBuilder.build(uriInfo, resourceRequest, project, repository, branchName);
    return Response.ok().entity(commits).build();
  }

  @ApiOperation(value = "Bulk update the branch commits on clone, checkout, pull, create/delete branch, commit.")
  @PUT
  @Path("/repository/{repositoryId}/branch/{branchName}/commit")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.GIT}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response updateBranchCommits(@PathParam("repositoryId") Integer repositoryId,
                                      @PathParam("branchName") String branchName,
                                      BranchCommits commits,
                                      @Context HttpServletRequest req,
                                      @Context SecurityContext sc) throws GitOpException {
    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    gitController.updateBranchCommits(project, hopsworksUser, commits, repositoryId, branchName);
    return Response.ok().build();
  }

  @ApiOperation(value = "Add or delete a git remote", response = GitOpExecutionDTO.class)
  @POST
  @Path("/repository/{repositoryId}/remote")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response remotes(@PathParam("repositoryId") Integer repositoryId,
                          @QueryParam("action") GitRemotesAction action, @QueryParam("url") String remoteUrl,
                          @QueryParam("name") String remoteName, @Context SecurityContext sc,
                          @Context UriInfo uriInfo,
                          @BeanParam ExecutionBeanParam executionBeanParam)
      throws GitOpException, HopsSecurityException, IllegalArgumentException {
    if (action == null) {
      throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.INVALID_REMOTES_ACTION.getMessage());
    }
    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    GitOpExecution execution = gitController.addOrDeleteRemote(action, project, hopsworksUser, repositoryId,
        remoteName, remoteUrl);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXECUTION);
    resourceRequest.setExpansions(executionBeanParam.getExpansions().getResources());
    GitOpExecutionDTO dto = executionBuilder.build(uriInfo, resourceRequest, execution);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Get repository configured remotes: git remotes -v", response = GitRepositoryRemoteDTO.class)
  @GET
  @Path("/repository/{repositoryId}/remote")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getRepositoryRemotes(@Context UriInfo uriInfo,
                                       @Context SecurityContext sc,
                                       @PathParam("repositoryId") Integer repositoryId)
      throws GitOpException {
    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, hopsworksUser, repositoryId);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.REMOTE);
    GitRepositoryRemoteDTO dto = gitRepositoryRemoteBuilder.build(uriInfo, resourceRequest, project, repository);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Get repository remote of a particular name", response = GitRepositoryRemoteDTO.class)
  @GET
  @Path("/repository/{repositoryId}/remote/{remoteName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getRepositoryRemote(@Context UriInfo uriInfo,
                                      @Context SecurityContext sc,
                                      @PathParam("repositoryId") Integer repositoryId,
                                      @PathParam("remoteName") String remoteName) throws GitOpException {
    if (Strings.isNullOrEmpty(remoteName)) {
      throw new IllegalArgumentException("Remote name is empty");
    }
    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, hopsworksUser, repositoryId);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.REMOTE);
    GitRepositoryRemoteDTO dto = gitRepositoryRemoteBuilder.build(uriInfo, resourceRequest, project, repository,
        remoteName);
    return Response.ok().entity(dto).build();
  }

  @ApiOperation(value = "Execute git checkout --filename. The final execution message will " +
      "contain the list of files in the status. If some of the files in the argument are present in the final " +
      "execution message then they failed to be checkout out", response = GitOpExecutionDTO.class)
  @POST
  @Path("/repository/{repositoryId}/file")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.GIT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response fileCheckout(@Context UriInfo uriInfo,
                               @PathParam("repositoryId") Integer repositoryId,
                               @Context SecurityContext sc,
                               GitFileCheckout files,
                               @BeanParam ExecutionBeanParam executionBeanParam) throws GitOpException,
      HopsSecurityException {
    Users hopsworksUser = jWTHelper.getUserPrincipal(sc);
    GitOpExecution execution = gitController.fileCheckout(project, hopsworksUser, repositoryId, files.getFiles());
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.EXECUTION);
    resourceRequest.setExpansions(executionBeanParam.getExpansions().getResources());
    GitOpExecutionDTO dto = executionBuilder.build(uriInfo, resourceRequest, execution);
    return Response.ok().entity(dto).build();
  }

  @Path("/repository/{repositoryId}/execution")
  public GitExecutionResource gitExecution(@PathParam("repositoryId") Integer repositoryId) throws GitOpException {
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, repositoryId);
    return this.gitExecutionResource.setRepository(repository);
  }
}
