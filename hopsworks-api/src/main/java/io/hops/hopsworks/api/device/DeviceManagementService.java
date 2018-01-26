package io.hops.hopsworks.api.device;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.device.DeviceFacade;
import io.hops.hopsworks.common.dao.device.ProjectDeviceDTO;
import io.hops.hopsworks.common.dao.device.ProjectDevicesSettings;
import io.hops.hopsworks.common.dao.device.ProjectDevicesSettingsDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamPK;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.ProjectController;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DeviceManagementService {

  private final static Logger LOGGER = Logger.getLogger(DeviceManagementService.class.getName());

  @EJB
  private ProjectController projectController;

  @EJB
  private DeviceFacade deviceFacade;

  @EJB
  private NoCacheResponse noCacheResponse;

  private Integer projectId;

  public DeviceManagementService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  private void checkForProjectId() throws AppException {
    if (projectId == null) {
      throw new AppException(Status.BAD_REQUEST.getStatusCode(), "Incomplete request! Project id not present!");
    }
  }

  private void createDevicesSettings(Integer projectId, ProjectDevicesSettingsDTO settingsDTO) throws AppException{
    // Adds the device-user to the project as a Data Owner
    List<ProjectTeam> list = new ArrayList<>();
    ProjectTeam pt = new ProjectTeam(new ProjectTeamPK(projectId, DeviceServiceSecurity.DEFAULT_DEVICE_USER_EMAIL));
    pt.setTeamRole(AllowedProjectRoles.DATA_OWNER);
    pt.setTimestamp(new Date());
    list.add(pt);

    Project project = projectController.findProjectById(projectId);
    List<String>  failed = projectController.addMembers(project, project.getOwner().getEmail(), list);
    if (failed != null && failed.size() > 0){
      LOGGER.severe("Failure for user: " + failed.get(0));
      throw new AppException(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
        "Default Devices User could not be added to the project.");
    }

    // Generates a random UUID to serve as the project's jwt secret.
    String jwtSecret = UUID.randomUUID().toString();

    deviceFacade.createProjectDevicesSettings(
      new ProjectDevicesSettings(projectId, jwtSecret, settingsDTO.getJwtTokenDurationInHours()));
  }

  private ProjectDevicesSettingsDTO readDevicesSettings(Integer projectId){
    ProjectDevicesSettingsDTO settingsDTO;
    try {
      ProjectDevicesSettings projectDevicesSettings = deviceFacade.readProjectDevicesSettings(projectId);
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
    Integer projectId, SecurityContext sc, HttpServletRequest req) throws AppException {
    Project project = projectController.findProjectById(projectId);
    try {
      projectController.removeMemberFromTeam(project, sc.getUserPrincipal().getName(),
        DeviceServiceSecurity.DEFAULT_DEVICE_USER_EMAIL, req.getSession().getId());
    } catch (Exception e) {
      e.printStackTrace();
    }
    deviceFacade.deleteProjectDevicesSettings(projectId);
  }


  @GET
  @Path("/devicesSettings")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response getDevicesSettings(@Context HttpServletRequest req) throws AppException {
    checkForProjectId();
    ProjectDevicesSettingsDTO settingsDTO = readDevicesSettings(projectId);
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
                                      ProjectDevicesSettingsDTO settingsDTO) throws AppException {
    checkForProjectId();
    ProjectDevicesSettingsDTO oldSettingsDTO = readDevicesSettings(projectId);
    if(oldSettingsDTO.getEnabled() == 0 && settingsDTO.getEnabled() == 1){
      createDevicesSettings(projectId, settingsDTO);
    }else if(oldSettingsDTO.getEnabled() == 1 && settingsDTO.getEnabled() == 0){
      deleteDevicesSettings(projectId, sc, req);
    }else if(oldSettingsDTO.getEnabled() == 1 && settingsDTO.getEnabled() == 1){
      updateDevicesSettings(projectId, settingsDTO);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }


  @GET
  @Path("/devices")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response getDevices(
    @QueryParam("state") String state, @Context HttpServletRequest req) throws AppException {
    checkForProjectId();

    List<ProjectDeviceDTO> listDevices;
    if (state != null){
      listDevices = deviceFacade.readProjectDevices(projectId, state);
    }else{
      listDevices = deviceFacade.readProjectDevices(projectId);
    }
    GenericEntity<List<ProjectDeviceDTO>> projectDevices = new GenericEntity<List<ProjectDeviceDTO>>(listDevices){};
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projectDevices).build();

  }

  @PUT
  @Path("/device")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response putDevice( @Context HttpServletRequest req, ProjectDeviceDTO device) throws AppException {
    checkForProjectId();
    deviceFacade.updateProjectDevice(device);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }


  @DELETE
  @Path("/device/{deviceUuid}/remove")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response deleteDevice(
    @Context HttpServletRequest req, @PathParam("deviceUuid") String  deviceUuid) throws AppException {
    checkForProjectId();
    deviceFacade.deleteProjectDevice(projectId, deviceUuid);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

}
