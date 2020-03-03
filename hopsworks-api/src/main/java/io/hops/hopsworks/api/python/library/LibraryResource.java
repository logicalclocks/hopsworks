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

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.python.library.command.LibraryCommandsResource;
import io.hops.hopsworks.api.python.library.search.LibrarySearchBuilder;
import io.hops.hopsworks.api.python.library.search.LibrarySearchDTO;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.dao.python.LibraryFacade;
import io.hops.hopsworks.common.dao.python.PythonDep;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.python.library.LibraryController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
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
public class LibraryResource {

  @EJB
  private LibraryController libraryController;
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

  private static final Pattern VALIDATION_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-\\.]+");
  private static final Pattern CHANNEL_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-:/~?&\\.]+");

  private Project project;
  private String pythonVersion;
  
  public LibraryResource setProjectAndVersion(Project project, String pythonVersion) {
    this.project = project;
    this.pythonVersion = pythonVersion;
    return this;
  }
  
  public Project getProject() {
    return project;
  }

  @ApiOperation(value = "Get the python libraries installed in this environment", response = LibraryDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response get(@BeanParam Pagination pagination,
      @BeanParam LibrariesBeanParam librariesBeanParam,
      @Context UriInfo uriInfo, @Context SecurityContext sc) throws PythonException {
    environmentController.checkCondaEnabled(project, pythonVersion);
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
  
  @ApiOperation(value = "Get the a python library installed in this environment", response = LibraryDTO.class)
  @GET
  @Path("{library}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getByName(@PathParam("library") String library, @BeanParam LibraryExpansionBeanParam expansions,
    @Context UriInfo uriInfo, @Context SecurityContext sc) throws PythonException {
    validatePattern(library);
    environmentController.checkCondaEnabled(project, pythonVersion);
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
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response uninstall(@Context SecurityContext sc, @PathParam("library") String library)
    throws ServiceException, GenericException, ProjectException, PythonException, ElasticException {
    validatePattern(library);
    Users user = jwtHelper.getUserPrincipal(sc);
    environmentController.checkCondaEnabled(project, pythonVersion);
    if (settings.getPreinstalledPythonLibraryNames().contains(library)) {
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
  @Path("{library}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response install(@PathParam("library") String library,
                          @QueryParam("package_manager") LibraryDTO.PackageManager packageManager,
                          @QueryParam("version") String version,
                          @QueryParam("channel") String channel,
                          @QueryParam("machine") LibraryFacade.MachineType machine,
                          @Context UriInfo uriInfo,
                          @Context HttpServletRequest req)
      throws ServiceException, GenericException, PythonException,
      ProjectException, ElasticException {
    Users user = jwtHelper.getUserPrincipal(req);
    if (version == null || version.isEmpty()) {
      throw new PythonException(RESTCodes.PythonErrorCode.VERSION_NOT_SPECIFIED, Level.FINE);
    }
    if (machine == null) {
      throw new PythonException(RESTCodes.PythonErrorCode.MACHINE_TYPE_NOT_SPECIFIED, Level.FINE);
    }
    validatePattern(library);
    validatePattern(version);

    environmentController.checkCondaEnabled(project, pythonVersion);
    if (packageManager == null) {
      throw new PythonException(RESTCodes.PythonErrorCode.INSTALL_TYPE_NOT_SUPPORTED, Level.FINE);
    }
    switch (packageManager) {
      case PIP:
        //indicate that the library comes from the distribution published in PyPi
        channel = "PyPi";
        break;
      case CONDA:
        if(channel == null) {
          throw new PythonException(RESTCodes.PythonErrorCode.CONDA_INSTALL_REQUIRES_CHANNEL, Level.FINE);
        } else {
          validateChannel(channel);
        }
        break;
      default:
        throw new PythonException(RESTCodes.PythonErrorCode.INSTALL_TYPE_NOT_SUPPORTED, Level.FINE);
    }

    //TODO account for ongoing operations
    for(PythonDep dep: project.getPythonDepCollection()) {
      if(dep.getDependency().equalsIgnoreCase(library)) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.ANACONDA_DEP_INSTALL_FORBIDDEN, Level.FINE);
      }
    }

    environmentController.checkCondaEnvExists(project, user);
  
    PythonDep dep = libraryController.addLibrary(project, user, CondaCommandFacade.
        CondaInstallType.valueOf(packageManager.name().toUpperCase()), machine, channel, library, version);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.LIBRARIES);
    LibraryDTO libraryDTO = librariesBuilder.build(uriInfo, resourceRequest, dep, project);
    return Response.created(libraryDTO.getHref()).entity(libraryDTO).build();
  }


  @ApiOperation(value = "Search for libraries using conda or pip package managers", response = LibrarySearchDTO.class)
  @GET
  @Path("{search: conda|pip}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response search(@PathParam("search") String search,
                         @QueryParam("query") String query,
                         @QueryParam("channel") String channel, @Context UriInfo uriInfo, @Context SecurityContext sc)
    throws ServiceException, PythonException {
    validatePattern(query);
    environmentController.checkCondaEnabled(project, pythonVersion);
    LibrarySearchDTO librarySearchDTO;
    LibraryDTO.PackageManager packageManager = LibraryDTO.PackageManager.fromString(search);
    switch (packageManager) {
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
    return this.libraryCommandsResource.setProject(project, pythonVersion);
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
}
