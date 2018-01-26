package io.hops.hopsworks.api.device;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.api.filter.RequestAuthFilter;
import io.hops.hopsworks.common.dao.device.DeviceFacade;
import io.hops.hopsworks.common.dao.device.ProjectDevice;
import io.hops.hopsworks.common.dao.device.ProjectDevicesSettings;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.device.DeviceResponseBuilder;
import io.hops.hopsworks.common.exception.DeviceServiceException;

import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Logger;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class DeviceJwtAuthFilter implements ContainerRequestFilter {

  @EJB
  private ProjectFacade projectFacade;

  @EJB
  private DeviceFacade deviceFacade;

  @Context
  private ResourceInfo resourceInfo;

  private final static Logger log = Logger.getLogger(RequestAuthFilter.class.getName());

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {

    String path = requestContext.getUriInfo().getPath();
    Method method = resourceInfo.getResourceMethod();
    String[] pathParts = path.split("/");

    // Validates that the path is under the devices-api sphere of influence.
    if (pathParts.length > 1 && (pathParts[0].equalsIgnoreCase("devices-api"))) {
      String projectName = String.valueOf(pathParts[1]);

      // Validates that a project with the given projectName exists.
      Project project = projectFacade.findByName(projectName);
      if (project == null) {
        requestContext.abortWith(new DeviceResponseBuilder().PROJECT_NOT_FOUND);
        return;
      }

      // Validates that the Devices Feature is Active for the given project.
      ProjectDevicesSettings devicesSettings = deviceFacade.readProjectDevicesSettings(project.getId());
      if (devicesSettings == null){
        requestContext.abortWith(new DeviceResponseBuilder().DEVICES_FEATURE_NOT_ACTIVE);
        return;
      }

      // Validates that the DeviceJwtTokenRequired exists for the requested method.
      if (method.isAnnotationPresent(DeviceJwtTokenRequired.class)) {

        // Gets the contents of the Authorization Header.
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        // Validates that the Authorization Header is present.
        if (authorizationHeader == null) {
          requestContext.abortWith(new DeviceResponseBuilder().AUTH_HEADER_MISSING);
          return;
        }

        // Validates that the Authorization Header starts with Bearer.
        if(!authorizationHeader.startsWith("Bearer")){
          requestContext.abortWith(new DeviceResponseBuilder().AUTH_HEADER_BEARER_MISSING);
          return;
        }

        // Gets the jwtToken from the Authorization Header and removes empty characters.
        String jwtToken =  authorizationHeader.substring("Bearer".length()).replaceAll("\\s","");

        // Validates that the jwtToken is authentic.
        DecodedJWT decodedJWT;
        try {
          decodedJWT = DeviceServiceSecurity.verifyJwt(devicesSettings, jwtToken);
        } catch (DeviceServiceException e) {
          requestContext.abortWith(e.getResponse());
          return;
        }

        // Extracts the claims within the already verified jwtToken
        Map<String, Claim> claims = decodedJWT.getClaims();

        //Validates that the projectId and the deviceUuid exists within the claims of the verified jwtToken.
        Integer projectId;
        String deviceUuid;
        try {
          projectId = claims.get(DeviceServiceSecurity.PROJECT_ID).asInt();
          deviceUuid = claims.get(DeviceServiceSecurity.DEVICE_UUID).asString();
        }catch (Exception e) {
          // This is a clear indication that the secret of the project has been compromised.
          requestContext.abortWith(new DeviceResponseBuilder().JWT_CLAIM_MISSING);
          return;
        }

        // Passes the claims into the context object so that the endpoint can get the claims.
        requestContext.setProperty(DeviceServiceSecurity.PROJECT_ID, projectId);
        requestContext.setProperty(DeviceServiceSecurity.DEVICE_UUID, deviceUuid);

        // Validates that the projectId for the given projectName matches with the projectId within the jwtToken.
        if (project.getId() != projectId){
          // This is a clear indication that the secret of the project has been compromised.
          requestContext.abortWith(new DeviceResponseBuilder().PROJECT_ID_MISSMATCH);
          return;
        }

        // Validates that the device is already registered within the project.
        ProjectDevice device = deviceFacade.readProjectDevice(projectId, deviceUuid);
        if(device == null) {
          requestContext.abortWith(new DeviceResponseBuilder().DEVICE_NOT_REGISTERED);
          return;
        }

        // Validates that the Device is in the Approved State.
        if (device.getState() != ProjectDevice.State.Approved){
          if (device.getState() == ProjectDevice.State.Disabled){
            requestContext.abortWith(new DeviceResponseBuilder().DEVICE_DISABLED);
            return;
          }
          if (device.getState() == ProjectDevice.State.Pending){
            requestContext.abortWith(new DeviceResponseBuilder().DEVICE_PENDING);
            return;
          }
          requestContext.abortWith(new DeviceResponseBuilder().DEVICE_UNKNOWN_STATE);
          return;
        }
      }
    }
  }
}
