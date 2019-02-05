package io.hops.hopsworks.api.device;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.device.DeviceFacade;
import io.hops.hopsworks.common.dao.device.ProjectDeviceDTO;
import io.hops.hopsworks.common.dao.device.ProjectDevicesSettings;
import io.hops.hopsworks.common.dao.device.ProjectDevicesSettingsDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamPK;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.KafkaException;
import io.hops.hopsworks.common.exception.ProjectException;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.exception.UserException;
import io.hops.hopsworks.common.project.ProjectController;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DeviceManagementService {

  private final static Logger LOGGER = Logger.getLogger(DeviceManagementService.class.getName());

  @EJB
  private ProjectController projectController;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private UserFacade userFacade;
  @EJB
  private DeviceFacade deviceFacade;

  @EJB
  private NoCacheResponse noCacheResponse;

  private Project project;

  public DeviceManagementService() {
  }

  public void setProject(Integer projectId) {
    this.project = projectFacade.find(projectId);
  }


  private void createDevicesSettings(Project project, ProjectDevicesSettingsDTO settingsDTO)
    throws ProjectException, KafkaException, UserException {
    // Adds the device-user to the project as a Data Owner
    List<ProjectTeam> list = new ArrayList<>();
    ProjectTeam pt =
      new ProjectTeam(new ProjectTeamPK(project.getId(), DeviceServiceSecurity.DEFAULT_DEVICE_USER_EMAIL));
    pt.setTeamRole(AllowedProjectRoles.DATA_OWNER);
    pt.setTimestamp(new Date());
    list.add(pt);

    List<String>  failed = projectController.addMembers(project, project.getOwner(), list);
    if (failed != null && failed.size() > 0){
      LOGGER.severe("Failure for user: " + failed.get(0));
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_MEMBER_ADDITION_ADDED, Level.SEVERE,
        "Default Devices User could not be added to the project.");
    }

    // Generates a random UUID to serve as the project's jwt secret.
    String jwtSecret = UUID.randomUUID().toString();

    deviceFacade.createProjectDevicesSettings(
      new ProjectDevicesSettings(project.getId(), jwtSecret, settingsDTO.getJwtTokenDurationInHours()));
  }

  private ProjectDevicesSettingsDTO readDevicesSettings(Project project){
    ProjectDevicesSettingsDTO settingsDTO;
    try {
      ProjectDevicesSettings projectDevicesSettings = deviceFacade.readProjectDevicesSettings(project.getId());
      settingsDTO = new ProjectDevicesSettingsDTO(1, projectDevicesSettings.getJwtTokenDuration());
    }catch (Exception e){
      // Default values for the project devices settings are defined here.
      settingsDTO = new ProjectDevicesSettingsDTO(0, 24 * 7);
    }
    return settingsDTO;
  }

  private void updateDevicesSettings(Integer projectId, ProjectDevicesSettingsDTO settingsDTO) {

    // Generates a random UUID to serve as the project secret.
    String projectSecret = UUID.randomUUID().toString();

    // Saves Project Secret
    deviceFacade.updateProjectDevicesSettings(new ProjectDevicesSettings(
      projectId, projectSecret, settingsDTO.getJwtTokenDurationInHours()));
  }

  private void deleteDevicesSettings(
    Integer projectId, SecurityContext sc) {
    try {
      Users user = userFacade.findByEmail(sc.getUserPrincipal().getName());
      projectController.removeMemberFromTeam(project, user, DeviceServiceSecurity.DEFAULT_DEVICE_USER_EMAIL);
    } catch (Exception e) {
      e.printStackTrace();
    }
    deviceFacade.deleteProjectDevicesSettings(projectId);
  }


  @GET
  @Path("/devicesSettings")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response getDevicesSettings(@Context HttpServletRequest req) {
    ProjectDevicesSettingsDTO settingsDTO = readDevicesSettings(project);
    GenericEntity<ProjectDevicesSettingsDTO> devicesSettings =
      new GenericEntity<ProjectDevicesSettingsDTO>(settingsDTO){};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(devicesSettings).build();
  }


  @POST
  @Path("/devicesSettings")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @TransactionAttribute(TransactionAttributeType.NEVER)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response postDevicesSettings(@Context SecurityContext sc, @Context HttpServletRequest req,
                                      ProjectDevicesSettingsDTO settingsDTO)
    throws KafkaException, ProjectException, UserException {
    ProjectDevicesSettingsDTO oldSettingsDTO = readDevicesSettings(project);
    if(oldSettingsDTO.getEnabled() == 0 && settingsDTO.getEnabled() == 1){
      createDevicesSettings(project, settingsDTO);
    }else if(oldSettingsDTO.getEnabled() == 1 && settingsDTO.getEnabled() == 0){
      deleteDevicesSettings(project.getId(), sc);
    }else if(oldSettingsDTO.getEnabled() == 1 && settingsDTO.getEnabled() == 1){
      updateDevicesSettings(project.getId(), settingsDTO);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }


  @GET
  @Path("/devices")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response getDevices(
    @QueryParam("state") String state, @Context HttpServletRequest req) {

    List<ProjectDeviceDTO> listDevices;
    if (state != null){
      listDevices = deviceFacade.readProjectDevices(project.getId(), state);
    }else{
      listDevices = deviceFacade.readProjectDevices(project.getId());
    }
    GenericEntity<List<ProjectDeviceDTO>> projectDevices = new GenericEntity<List<ProjectDeviceDTO>>(listDevices){};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projectDevices).build();

  }

  @PUT
  @Path("/device")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response putDevice( @Context HttpServletRequest req, ProjectDeviceDTO device) {
    deviceFacade.updateProjectDevice(device);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }


  @DELETE
  @Path("/device/{deviceUuid}/remove")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response deleteDevice(
    @Context HttpServletRequest req, @PathParam("deviceUuid") String  deviceUuid) {
    deviceFacade.deleteProjectDevice(project.getId(), deviceUuid);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

}
