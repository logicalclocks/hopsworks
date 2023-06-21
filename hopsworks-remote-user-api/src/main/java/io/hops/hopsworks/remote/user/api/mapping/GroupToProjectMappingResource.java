/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.mapping;

import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.remote.group.RemoteGroupProjectMappingFacade;
import io.hops.hopsworks.common.dao.remote.user.RemoteUserFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.remote.RemoteUsersDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.RemoteAuthException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.remote.group.RemoteGroupProjectMapping;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.remote.user.RemoteGroupMapping;
import io.hops.hopsworks.remote.user.api.Audience;
import io.hops.hopsworks.remote.user.ldap.LdapRealm;
import io.hops.hopsworks.remote.user.ldap.LdapUserController;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import joptsimple.internal.Strings;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Logged
@Path("/mapping")
@Stateless
@Api(value = "Group to project mapping")
@JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
@TransactionAttribute(TransactionAttributeType.NEVER)
public class GroupToProjectMappingResource {
  private static final Logger LOGGER = Logger.getLogger(GroupToProjectMappingResource.class.getName());
  
  @EJB
  private LdapUserController ldapUserController;
  @EJB
  private MappingBuilder mappingBuilder;
  @EJB
  private ProjectController projectController;
  @EJB
  private RemoteGroupProjectMappingFacade remoteGroupProjectMappingFacade;
  @EJB
  private RemoteGroupMapping remoteGroupMapping;
  @EJB
  private UserFacade userFacade;
  @EJB
  private RemoteUserFacade remoteUserFacade;
  @EJB
  private Settings settings;
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all mappings.", response = MappingDTO.class)
  public Response getAll(@BeanParam Pagination pagination, @BeanParam MappingBeanParam mappingBeanParam,
    @Context UriInfo uriInfo, @Context HttpServletRequest req) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.PROJECT_LDAP_GROUP_MAPPING);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(mappingBeanParam.getSortBySet());
    resourceRequest.setFilter(mappingBeanParam.getFilter());
    MappingDTO mappingDTO = mappingBuilder.buildItems(uriInfo, resourceRequest);
    return Response.ok(mappingDTO).build();
  }
  
  @GET
  @Path("{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get a mapping.", response = MappingDTO.class)
  public Response getMapping(@PathParam("id") Integer id, @Context UriInfo uriInfo, @Context HttpServletRequest req)
    throws RemoteAuthException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.PROJECT_LDAP_GROUP_MAPPING);
    MappingDTO mappingDTO = mappingBuilder.buildItems(uriInfo, resourceRequest, id);
    return Response.ok(mappingDTO).build();
  }
  
  @PUT
  @Path("{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a mapping.", response = MappingDTO.class)
  public Response updateMapping(@PathParam("id") Integer id, MappingDTO mappingDTO, @Context UriInfo uriInfo,
    @Context HttpServletRequest req) throws RemoteAuthException, ProjectException {
    checkSyncEnabled(); // should not allow updating if LdapGroupMappingSyncEnabled == false
    RemoteGroupProjectMapping remoteGroupProjectMapping = remoteGroupProjectMappingFacade.find(id);
    if (remoteGroupProjectMapping == null) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.NOT_FOUND, Level.FINE, "Mapping not found.");
    }
    doCheck(mappingDTO);
    Project project = findByNameOrId(mappingDTO.getProjectName(), mappingDTO.getProjectId());
    // if changing project or remote group check if there is a mapping for the project to the group
    if (!remoteGroupProjectMapping.getProject().equals(project) ||
      !remoteGroupProjectMapping.getRemoteGroup().equals(mappingDTO.getRemoteGroup())) {
      RemoteGroupProjectMapping lgpm =
        remoteGroupProjectMappingFacade.findByGroupAndProject(mappingDTO.getRemoteGroup(), project);
      if (lgpm != null) {
        throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.DUPLICATE_ENTRY, Level.FINE,
          "Mapping already exists.");
      }
    }
  
    remoteGroupProjectMapping.setProject(project);
    remoteGroupProjectMapping.setRemoteGroup(mappingDTO.getRemoteGroup());
    remoteGroupProjectMapping.setProjectRole(mappingDTO.getProjectRole());
    remoteGroupProjectMapping = remoteGroupProjectMappingFacade.update(remoteGroupProjectMapping);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.PROJECT_LDAP_GROUP_MAPPING);
    MappingDTO resMappingDTO = mappingBuilder.build(new MappingDTO(), uriInfo, resourceRequest,
      remoteGroupProjectMapping);
    return Response.ok(resMappingDTO).build();
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create mapping.")
  public Response createMapping(MappingDTO mappingDTO, @Context UriInfo uriInfo, @Context HttpServletRequest req)
    throws RemoteAuthException, ProjectException {
    checkSyncEnabled(); // should not allow creating if LdapGroupMappingSyncEnabled == false
    RemoteGroupProjectMapping remoteGroupProjectMapping = createMapping(mappingDTO);
    return Response.created(uriInfo.getAbsolutePathBuilder()
      .path(Integer.toString(remoteGroupProjectMapping.getId()))
      .build()).build();
  }
  
  @POST
  @Path("bulk")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create mappings from multiple remote groups to a project.")
  public Response createMappingBulk(BulkMappingDTO bulkMappingDTO, @Context UriInfo uriInfo,
    @Context HttpServletRequest req) throws RemoteAuthException, ProjectException {
    checkSyncEnabled(); // should not allow creating if LdapGroupMappingSyncEnabled == false
    if (bulkMappingDTO == null) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Mapping not set.");
    }
    if (bulkMappingDTO.getRemoteGroup() == null || bulkMappingDTO.getRemoteGroup().isEmpty()) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Remote group not set" +
        ".");
    }
    List<MappingDTO> mappingDTOS = bulkMappingDTO.getRemoteGroup().stream().map((group) -> new MappingDTO(group,
        bulkMappingDTO.getProjectRole(), bulkMappingDTO.getProjectName(), bulkMappingDTO.getProjectId()))
      .collect(Collectors.toList());
    for (MappingDTO mappingDTO : mappingDTOS) {
      createMapping(mappingDTO);
    }
    
    return Response.status(Response.Status.CREATED).build();
  }
  
  @DELETE
  @Path("{id: [0-9]*}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Delete a mapping.")
  public Response deleteMapping(@PathParam("id") Integer id, @Context HttpServletRequest req) {
    //should allow deleting even if LdapGroupMappingSyncEnabled == false
    RemoteGroupProjectMapping remoteGroupProjectMapping = remoteGroupProjectMappingFacade.find(id);
    if (remoteGroupProjectMapping != null) {
      remoteGroupProjectMappingFacade.remove(remoteGroupProjectMapping);
    }
    return Response.noContent().build();
  }
  
  @GET
  @Path("search")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Search LDAP.", response = String[].class)
  public Response search(@QueryParam("query") String query, @Context HttpServletRequest req)
    throws RemoteAuthException {
    String searchQuery = Strings.isNullOrEmpty(query) ?
      settings.getLdapGroupsSearchFilter().replace(LdapRealm.SUBST_SUBJECT_CN, "*") : query;
    try {
      List<String> ldapGroupsSource = ldapUserController.getLDAPGroups(searchQuery);
      return Response.ok(ldapGroupsSource).build();
    } catch (EJBException e) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.WRONG_CONFIG, Level.FINE, "Remote auth not " +
        "configured.", e.getMessage());
    }
  }
  
  @GET
  @Path("members")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get group members.", response = RemoteUsersDTO.class)
  public Response getMembers(@QueryParam("group") Set<String> groups, @Context HttpServletRequest req) {
    RemoteUsersDTO remoteUsersDTO = null;
    if (groups != null && !groups.isEmpty()) {
      remoteUsersDTO = ldapUserController.getMembers(new ArrayList<>(groups));
    }
    return Response.ok(remoteUsersDTO).build();
  }
  
  @POST
  @Path("sync")
  @Produces(MediaType.APPLICATION_JSON)
  public Response syncRemoteGroup(@QueryParam("id") Integer id, @QueryParam("email") String email,
    @Context HttpServletRequest req, @Context SecurityContext sc) throws UserException, RemoteAuthException {
    checkSyncEnabled(); // should not allow syncing if LdapGroupMappingSyncEnabled == false
    if (id != null) {
      Users user = userFacade.find(id);
      syncMapping(user);
    } else if (!Strings.isNullOrEmpty(email)) {
      Users user = userFacade.findByEmail(email);
      syncMapping(user);
    } else {
      remoteGroupMapping.syncMappingAsync();
    }
    return Response.accepted().build();
  }
  
  private void checkSyncEnabled() throws RemoteAuthException {
    if (!settings.isLdapGroupMappingSyncEnabled()) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.NOT_ALLOWED, Level.FINE, "Group mapping disabled.");
    }
  }
  
  private RemoteGroupProjectMapping createMapping(MappingDTO mappingDTO) throws RemoteAuthException, ProjectException {
    doCheck(mappingDTO);
    Project project = findByNameOrId(mappingDTO.getProjectName(), mappingDTO.getProjectId());
    RemoteGroupProjectMapping lgpm =
      remoteGroupProjectMappingFacade.findByGroupAndProject(mappingDTO.getRemoteGroup(), project);
    if (lgpm != null) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.DUPLICATE_ENTRY, Level.FINE,
        "Mapping already exists.");
    }
    remoteGroupProjectMappingFacade.save(
      new RemoteGroupProjectMapping(mappingDTO.getRemoteGroup(), project, mappingDTO.getProjectRole()));
    return remoteGroupProjectMappingFacade.findByGroupAndProject(mappingDTO.getRemoteGroup(), project);
  }
  
  private void syncMapping(Users users) throws UserException {
    if (users == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "User not found");
    }
    RemoteUser remoteUser = remoteUserFacade.findByUsers(users);
    remoteGroupMapping.syncMapping(remoteUser, null);
  }
  
  private void doCheck(MappingDTO mappingDTO) throws RemoteAuthException {
    if (mappingDTO == null) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Mapping not set.");
    }
    if (Strings.isNullOrEmpty(mappingDTO.getRemoteGroup())) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Remote group not set" +
        ".");
    }
    if (Strings.isNullOrEmpty(mappingDTO.getProjectRole())) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Project role not set" +
        ".");
    }
    if (!mappingDTO.getProjectRole().equals(AllowedRoles.DATA_OWNER) &&
      !mappingDTO.getProjectRole().equals(AllowedRoles.DATA_SCIENTIST)) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Invalid role.");
    }
    checkGroupExist(mappingDTO.getRemoteGroup());
  }
  
  private Project findByNameOrId(String projectName, Integer projectId) throws ProjectException, RemoteAuthException {
    if (Strings.isNullOrEmpty(projectName) && projectId == null) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Project not set.");
    }
    if (!Strings.isNullOrEmpty(projectName)) {
      return projectController.findProjectByName(projectName);
    }
    return projectController.findProjectById(projectId);
  }
  
  private void checkGroupExist(String groupName) throws RemoteAuthException{
    try {
      String groupDN = ldapUserController.getGroupDN(groupName);
      if (Strings.isNullOrEmpty(groupDN)) {
        throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.NOT_FOUND, Level.FINE, "Group not found");
      }
    } catch (NamingException e) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.NOT_FOUND, Level.FINE, "Group not found.",
        e.getMessage());
    }
  }

}
