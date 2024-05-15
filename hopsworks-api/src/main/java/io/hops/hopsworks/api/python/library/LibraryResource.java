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

import com.google.common.base.Strings;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.python.environment.EnvironmentSubResource;
import io.hops.hopsworks.api.python.library.command.LibraryCommandsResource;
import io.hops.hopsworks.api.python.library.search.LibrarySearchBuilder;
import io.hops.hopsworks.api.python.library.search.LibrarySearchDTO;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.python.library.LibraryController;
import io.hops.hopsworks.common.python.library.LibrarySpecification;
import io.hops.hopsworks.common.python.library.PackageSource;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.python.CondaInstallType;
import io.hops.hopsworks.persistence.entity.python.PythonDep;
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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Api(value = "Python Environment Library Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LibraryResource extends EnvironmentSubResource {

  @EJB
  private LibraryController libraryController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private DatasetController datasetController;
  @EJB
  private LibraryBuilder librariesBuilder;
  @EJB
  private LibrarySearchBuilder librariesSearchBuilder;
  @EJB
  private CommandsController commandsController;
  @EJB
  private Settings settings;
  @Inject
  private LibraryCommandsResource libraryCommandsResource;
  @EJB
  private EnvironmentController environmentController;
  @EJB
  private JWTHelper jwtHelper;
  @EJB
  private ProjectController projectController;

  private static final Pattern VALIDATION_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-\\.\\]\\[]+");
  private static final Pattern CHANNEL_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-:/~?&\\.]+");
  
  @Override
  protected ProjectController getProjectController() {
    return projectController;
  }

  @ApiOperation(value = "Get the python libraries installed in this environment", response = LibraryDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response get(@BeanParam Pagination pagination,
                      @BeanParam LibrariesBeanParam librariesBeanParam,
                      @Context UriInfo uriInfo,
                      @Context HttpServletRequest req,
                      @Context SecurityContext sc) throws PythonException, ProjectException {
    Project project = getProject();
    environmentController.checkCondaEnabled(project, getPythonVersion(), true);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.LIBRARIES);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(librariesBeanParam.getSortBySet());
    resourceRequest.setFilter(librariesBeanParam.getFilter());
    if (librariesBeanParam.getExpansions() != null) {
      resourceRequest.setExpansions(librariesBeanParam.getExpansions().getResources());
    }
    LibraryDTO libraryDTO = librariesBuilder.buildItems(uriInfo, resourceRequest, project);
    return Response.ok().entity(libraryDTO).build();
  }
  
  @ApiOperation(value = "Get a python library installed in this environment", response = LibraryDTO.class)
  @GET
  @Path("{library}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getByName(@PathParam("library") String library, @BeanParam LibraryExpansionBeanParam expansions,
                            @Context UriInfo uriInfo,
                            @Context HttpServletRequest req,
                            @Context SecurityContext sc) throws PythonException, ProjectException {
    Project project = getProject();
    validatePattern(library);
    environmentController.checkCondaEnabled(project, getPythonVersion(), true);
    PythonDep dep = libraryController.getPythonDep(library, project);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.LIBRARIES);
    if (expansions != null) {
      resourceRequest.setExpansions(expansions.getResources());
    }
    LibraryDTO libraryDTO = librariesBuilder.buildItem(uriInfo, resourceRequest, dep, project);
    return Response.ok().entity(libraryDTO).build();
  }

  @ApiOperation(value = "Uninstall a python library from the environment")
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{library}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response uninstall(@Context SecurityContext sc,
                            @Context HttpServletRequest req,
                            @PathParam("library") String library)
      throws ServiceException, GenericException, PythonException, ProjectException {
    Project project = getProject();
    validatePattern(library);
    Users user = jwtHelper.getUserPrincipal(sc);
    environmentController.checkCondaEnabled(project, getPythonVersion(), true);

    if (settings.getImmutablePythonLibraryNames().contains(library)) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_DEP_REMOVE_FORBIDDEN, Level.INFO,
          "library: " + library);
    }

    environmentController.checkCondaEnvExists(project, user);
    commandsController.deleteCommands(project, library);
    libraryController.uninstallLibrary(project, user, library);
    return Response.noContent().build();
  }

  @ApiOperation(value = "Install a python library in the environment")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{library}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response install(LibrarySpecification librarySpecification,
                          @PathParam("library") String library,
                          @Context UriInfo uriInfo,
                          @Context HttpServletRequest req,
                          @Context SecurityContext sc)
      throws ServiceException, GenericException, PythonException, DatasetException, ProjectException {

    Users user = jwtHelper.getUserPrincipal(sc);
    Project project = getProject();
    environmentController.checkCondaEnabled(project, getPythonVersion(), true);

    PackageSource packageSource = librarySpecification.getPackageSource();
    if (packageSource == null) {
      throw new PythonException(RESTCodes.PythonErrorCode.INSTALL_TYPE_NOT_SUPPORTED, Level.FINE);
    }
    switch (packageSource) {
      case PIP:
        validateLibrary(librarySpecification, library);
        librarySpecification.setChannelUrl("pypi");
        break;
      case CONDA:
        if (!settings.getEnableCondaInstall()) { // check if conda install is enabled
          throw new PythonException(RESTCodes.PythonErrorCode.CONDA_INSTALL_DISABLED, Level.FINE);
        }
        validateLibrary(librarySpecification, library);
        break;
      case EGG:
      case WHEEL:
      case REQUIREMENTS_TXT:
      case ENVIRONMENT_YAML:
        validateBundledDependency(user, librarySpecification, project);
        break;
      case GIT:
        validateGitURL(librarySpecification.getDependencyUrl());
        break;
      default:
        throw new PythonException(RESTCodes.PythonErrorCode.INSTALL_TYPE_NOT_SUPPORTED, Level.FINE);
    }

    environmentController.checkCondaEnvExists(project, user);

    PythonDep dep = libraryController.installLibrary(project, user,
        CondaInstallType.valueOf(packageSource.name().toUpperCase()), librarySpecification.getChannelUrl(),
        library, librarySpecification.getVersion(), librarySpecification.getDependencyUrl(),
        librarySpecification.getGitBackend(), librarySpecification.getGitApiKey());
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.LIBRARIES);
    LibraryDTO libraryDTO = librariesBuilder.build(uriInfo, resourceRequest, dep, project);
    return Response.created(libraryDTO.getHref()).entity(libraryDTO).build();
  }


  @ApiOperation(value = "Search for libraries using conda or pip package managers", response = LibrarySearchDTO.class)
  @GET
  @Path("{search: conda|pip}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PYTHON_LIBRARIES},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response search(@PathParam("search") String search,
                         @QueryParam("query") String query,
                         @QueryParam("channel") String channel,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc)
      throws ServiceException, PythonException, ProjectException {
    Project project = getProject();
    validatePattern(query);
    environmentController.checkCondaEnabled(project, getPythonVersion(), true);
    LibrarySearchDTO librarySearchDTO;
    PackageSource packageSource = PackageSource.fromString(search);
    switch (packageSource) {
      case CONDA:
        validateChannel(channel);
        librarySearchDTO = librariesSearchBuilder.buildCondaItems(uriInfo, query, project, channel);
        break;
      case PIP:
        librarySearchDTO = librariesSearchBuilder.buildPipItems(uriInfo, query, project);
        break;
      default:
        throw new PythonException(RESTCodes.PythonErrorCode.PYTHON_SEARCH_TYPE_NOT_SUPPORTED, Level.FINE);
    }
    return Response.ok().entity(librarySearchDTO).build();
  }
  
  @ApiOperation(value = "Python Library Commands sub-resource", tags = {"LibraryCommandsResource"})
  @Path("{library}/commands")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public LibraryCommandsResource libraryCommandsResource() {
    this.libraryCommandsResource.setProjectId(getProjectId());
    this.libraryCommandsResource.setPythonVersion(getPythonVersion());
    return this.libraryCommandsResource;
  }

  private void validatePattern(String element) throws IllegalArgumentException {
    Matcher matcher = VALIDATION_PATTERN.matcher(element);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Library names should follow the pattern: [a-zA-Z0-9_\\-]+");
    }
  }

  private void validateChannel(String channel) throws IllegalArgumentException {
    Matcher matcher = CHANNEL_PATTERN.matcher(channel);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Library names should follow the pattern: [a-zA-Z0-9_\\-:/~?&]+");
    }
  }

  private void validateLibrary(LibrarySpecification librarySpecification, String library) throws PythonException {

    String version = librarySpecification.getVersion();
    PackageSource packageSource = librarySpecification.getPackageSource();
    String channel = librarySpecification.getChannelUrl();

    if(packageSource.equals(PackageSource.CONDA)) {
      if(channel == null) {
        throw new PythonException(RESTCodes.PythonErrorCode.CONDA_INSTALL_REQUIRES_CHANNEL, Level.FINE);
      } else {
        validateChannel(channel);
      }
    }

    validatePattern(library);

    if(!Strings.isNullOrEmpty(version)) {
      validatePattern(version);
    }
  }

  private void validateBundledDependency(Users user, LibrarySpecification librarySpecification, Project project)
      throws DatasetException, PythonException {

    String dependencyUrl = librarySpecification.getDependencyUrl();
    PackageSource packageSource = librarySpecification.getPackageSource();

    datasetController.checkFileExists(new org.apache.hadoop.fs.Path(dependencyUrl),
        hdfsUsersController.getHdfsUserName(project, user));

    if(packageSource.equals(PackageSource.EGG) && !dependencyUrl.endsWith(".egg")) {
      throw new PythonException(RESTCodes.PythonErrorCode.INSTALL_TYPE_NOT_SUPPORTED, Level.FINE,
          "The library to install is not an .egg file: " + dependencyUrl);
    } else if(packageSource.equals(PackageSource.WHEEL) && !dependencyUrl.endsWith(".whl")) {
      throw new PythonException(RESTCodes.PythonErrorCode.INSTALL_TYPE_NOT_SUPPORTED, Level.FINE,
          "The library to install is not a .whl file: " + dependencyUrl);
    } else if(packageSource.equals(PackageSource.REQUIREMENTS_TXT) && !dependencyUrl.endsWith("/requirements.txt")) {
      throw new PythonException(RESTCodes.PythonErrorCode.INSTALL_TYPE_NOT_SUPPORTED, Level.FINE,
          "The library to install is not a requirements.txt file: " + dependencyUrl);
    } else if(packageSource.equals(PackageSource.ENVIRONMENT_YAML) && !dependencyUrl.endsWith(".yml")) {
      throw new PythonException(RESTCodes.PythonErrorCode.INSTALL_TYPE_NOT_SUPPORTED, Level.FINE,
          "The library to install is not a conda environment.yml file: " + dependencyUrl);
    }
  }

  private void validateGitURL(String url) {
    //url.startsWith("https://") is kept for backwards compatibility reasons
    if(!url.toLowerCase().contains("git+") && !url.toLowerCase().startsWith("https://")) {
      throw new IllegalArgumentException("The provided git installation url is not valid as it does not contain git+");
    }
  }
}
