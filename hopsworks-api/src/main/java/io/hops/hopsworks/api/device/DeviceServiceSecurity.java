package io.hops.hopsworks.api.device;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.hops.hopsworks.common.dao.device.ProjectDevice;
import io.hops.hopsworks.common.dao.device.ProjectDevicesSettings;
import io.hops.hopsworks.common.device.DeviceResponseBuilder;
import io.hops.hopsworks.common.exception.DeviceServiceException;

import java.util.Calendar;
import java.util.Date;

public class DeviceServiceSecurity {

  public static final String DEFAULT_DEVICE_USER_EMAIL = "devices@hops.io";

  public static final String DEVICE_UUID = "deviceUuid";

  public static final String PROJECT_ID = "projectId";


  /***
   * This method generates a jwt token (RFC 7519) which is unencrypted but signed with the given projectDevicesSettings.
   *
   * @param projectDevicesSettings Contains the secret which is used to sign the jwt token.
   * @param projectDevice Contains the device identification information for the project.
   * @return Returns the jwt token.
   */
  public static String generateJwt(
    ProjectDevicesSettings projectDevicesSettings, ProjectDevice projectDevice) throws DeviceServiceException {
    try {
      Calendar cal = Calendar.getInstance();
      cal.setTime(new Date());
      cal.add(Calendar.HOUR_OF_DAY, projectDevicesSettings.getJwtTokenDuration());
      Date expirationDate = cal.getTime();
      Algorithm algorithm = Algorithm.HMAC256(projectDevicesSettings.getJwtSecret());
      return JWT.create()
        .withExpiresAt(expirationDate)
        .withClaim(PROJECT_ID, projectDevice.getProjectDevicePK().getProjectId())
        .withClaim(DEVICE_UUID, projectDevice.getProjectDevicePK().getDeviceUuid())
        .sign(algorithm);
    } catch (Exception e) {
      throw new DeviceServiceException(new DeviceResponseBuilder().JWT_GENERATION_FAILED);
    }
  }


  /***
   * This method verifies the validity of a jwt token (RFC 7519) by checking the signature of the token
   * against the provided projectDevicesSettings.
   *
   * @param projectDevicesSettings Contains the secret which is used to verify the jwt token.
   * @param jwtToken The jwt token
   * @return Returns DecodedJWT if the token is verified and has not expired. Otherwise throws a DeviceServiceException.
   */
  public static DecodedJWT verifyJwt(
    ProjectDevicesSettings projectDevicesSettings, String jwtToken) throws DeviceServiceException{
    try {
      Algorithm algorithm = Algorithm.HMAC256(projectDevicesSettings.getJwtSecret());
      JWTVerifier verifier = JWT.require(algorithm).build();
      return verifier.verify(jwtToken);
    }catch (TokenExpiredException exception){
      throw new DeviceServiceException(new DeviceResponseBuilder().JWT_EXPIRED);
    }catch (JWTVerificationException exception){
      throw new DeviceServiceException(new DeviceResponseBuilder().JWT_INVALID_TOKEN);
    }catch (Exception exception){
      throw new DeviceServiceException(new DeviceResponseBuilder().JWT_VALIDATION_FAILED);
    }
  }
}
