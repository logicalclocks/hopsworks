package io.hops.hopsworks.common.device;


import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class DeviceResponseBuilder {

  public Response DEVICES_FEATURE_NOT_ACTIVE = failedJsonResponse(Response.Status.FORBIDDEN,
    "The devices feature for this project is not activated.");

  public Response DEVICE_NOT_REGISTERED = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The device is not registered.");

  public Response DEVICE_ALREADY_REGISTERED = failedJsonResponse(Response.Status.CONFLICT,
    "The device with the provided identifier is already registered.");

  public Response DEVICE_LOGIN_FAILED = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The device identifier and/or password is incorrect.");

  public Response DEVICE_DISABLED = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The device has been disabled.");

  public Response DEVICE_PENDING = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The device has not been approved yet. Contact an administrator for approval.");

  public Response DEVICE_UNKNOWN_STATE = failedJsonResponse(Response.Status.INTERNAL_SERVER_ERROR,
    "The state of the device can not been identified by the service. Contanct an administrator.");

  public Response MISSING_PARAMS = failedJsonResponse(Response.Status.BAD_REQUEST,
    "One or more of the mandatory params is missing.");

  public Response AUTH_UUID4_BAD_REQ = failedJsonResponse(Response.Status.BAD_REQUEST,
    "The deviceUuid param must be a valid UUID version 4.");

  public Response AUTH_HEADER_MISSING = failedJsonResponse(Response.Status.BAD_REQUEST,
    "The Authorization header is missing.");

  public Response AUTH_HEADER_BEARER_MISSING = failedJsonResponse(Response.Status.BAD_REQUEST,
    "The 'Bearer ' in Authorization header is missing.");

  public Response JWT_EXPIRED = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The jwt token has expired. Try to login again to get a new one.");

  public Response JWT_INVALID_TOKEN = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The jwt token provided is invalid.");

  public Response JWT_GENERATION_FAILED = failedJsonResponse(Response.Status.INTERNAL_SERVER_ERROR,
    "The process to generate the authorization jwt token failed.");

  public Response JWT_VALIDATION_FAILED = failedJsonResponse(Response.Status.INTERNAL_SERVER_ERROR,
    "The process to validate the authorization jwt token failed.");

  public Response JWT_CLAIM_MISSING = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "One or more claims within the jwt token is missing or its format is incorrect.");

  public Response PROJECT_NOT_FOUND = failedJsonResponse(Response.Status.NOT_FOUND,
    "No project with the given projectName has been found.");

  public Response PROJECT_ID_MISSMATCH = failedJsonResponse(Response.Status.UNAUTHORIZED,
    "The projectName does not match the projectId specified in the authentication token.");

  public Response PROJECT_USER_PASS_FOR_KS_TS_NOT_FOUND = failedJsonResponse(Response.Status.INTERNAL_SERVER_ERROR,
    "The passwords for the key store and the trust store that is used to produce to Kafka were not found.");

  public Response PROJECT_TOPIC_NOT_FOUND = failedJsonResponse(Response.Status.NOT_FOUND,
    "There is no topic found with the provided topic name under the project.");

  public Response PRODUCE_FAILED = failedJsonResponse(Response.Status.INTERNAL_SERVER_ERROR,
    "Produce failed.");

  public Response PRODUCE_DATA_MALFORMED = failedJsonResponse(Response.Status.BAD_REQUEST,
    "Produced data do not conform with the topic schema.");

  public Response PRODUCE_KEYSTORE_FILE_NOT_FOUND = failedJsonResponse(Response.Status.INTERNAL_SERVER_ERROR,
    "Keystore File not found, when producing.");

  public Response PRODUCE_KEYSTORE_IO_EXCEPTION = failedJsonResponse(Response.Status.INTERNAL_SERVER_ERROR,
    "Keystore File to Byte IO exception.");

  public Response UNEXPECTED_ERROR = failedJsonResponse(Response.Status.INTERNAL_SERVER_ERROR,
    "An unexpected error occurred.");


  public static Response failedJsonResponse(Response.Status status, String errorMessage) {
    Response.ResponseBuilder rb = Response.status(status);
    rb.type(MediaType.APPLICATION_JSON);
    DeviceResponse resp = new DeviceResponse(status.getStatusCode(), status.getReasonPhrase(), errorMessage);
    rb.entity(resp);
    return rb.build();
  }

  public static Response successfulJsonResponse(Response.Status status) {
    Response.ResponseBuilder rb = Response.status(status);
    rb.type(MediaType.APPLICATION_JSON);
    DeviceResponse resp = new DeviceResponse(status.getStatusCode(), status.getReasonPhrase());
    rb.entity(resp);
    return rb.build();
  }

  public static Response successfulJsonResponse(Response.Status status, String jwt) {
    Response.ResponseBuilder rb = Response.status(status);
    rb.type(MediaType.APPLICATION_JSON);
    DeviceResponse resp = new DeviceResponse(status.getStatusCode(), status.getReasonPhrase());
    resp.setJwt(jwt);
    rb.entity(resp);
    return rb.build();
  }
}
