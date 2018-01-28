package io.hops.hopsworks.api.device;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.io.ByteStreams;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.device.AckRecordDTO;
import io.hops.hopsworks.common.dao.device.AuthDeviceDTO;
import io.hops.hopsworks.common.dao.device.DeviceFacade;
import io.hops.hopsworks.common.dao.device.ProjectDevice;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.cert.CertPwDTO;
import io.hops.hopsworks.common.device.DeviceResponseBuilder;
import io.hops.hopsworks.common.exception.DeviceServiceException;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.net.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.SchemaDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;

@Path("/devices-api")
@Api(value = "Device Service",
    description = "Device Service")
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DeviceService {

  private final static Logger logger = Logger.getLogger(DeviceService.class.getName());

  public static final String UUID_V4_REGEX
      = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[4][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}";

  @EJB
  private DeviceFacade deviceFacade;

  @EJB
  private NoCacheResponse noCacheResponse;

  @EJB
  private KafkaFacade kafkaFacade;

  @EJB
  private CertsFacade userCerts; // Only used for the produce endpoint

  @EJB
  private CertificateMaterializer certificateMaterializer; // Only used for the produce endpoint

  @EJB
  private Settings settings; // Only used for the produce endpoint

  @EJB
  private ProjectController projectController; // Only used for the produce endpoint

  @EJB
  private ProjectFacade projectFacade; // Only used for the produce endpoint

  @EJB
  private UserFacade userFacade;

  public DeviceService() {
  }

  /**
   * This method validates an AuthProjectDeviceDTO object and checks that no critical information is missing and that
   * the deviceUuid is a valid UUID version 4. A "No news is good news" policy is applied. If no exception is thrown
   * then the object is considered validated.
   *
   * @param authDeviceDTO The object to validate
   * @throws DeviceServiceException It is thrown when there is a validation problem with the provided object.
   */
  private void validate(AuthDeviceDTO authDeviceDTO) throws DeviceServiceException {
    if (authDeviceDTO == null || authDeviceDTO.getDeviceUuid() == null || authDeviceDTO.getPassword() == null) {
      throw new DeviceServiceException(new DeviceResponseBuilder().MISSING_PARAMS);
    }
    if (!authDeviceDTO.getDeviceUuid().matches(UUID_V4_REGEX)) {
      throw new DeviceServiceException(new DeviceResponseBuilder().AUTH_UUID4_BAD_REQ);
    }
  }

  private SchemaDTO getSchemaForTopic(Integer projectId, String topicName) throws DeviceServiceException {
    SchemaDTO schemaDTO;
    try {
      schemaDTO = kafkaFacade.getSchemaForProjectTopic(projectId, topicName);
      if (schemaDTO == null) {
        throw new DeviceServiceException(new DeviceResponseBuilder().PROJECT_TOPIC_NOT_FOUND);
      }
    } catch (Exception e) {
      throw new DeviceServiceException(new DeviceResponseBuilder().PROJECT_TOPIC_NOT_FOUND);
    }
    return schemaDTO;
  }

  /**
   * Retrieves the entire keystore file of the provided path into a Base64 encoded string.
   *
   * @param keystoreFilePath the filepath to the keystore file.
   * @return a Base64 encoded string
   * @throws DeviceServiceException It is thrown when something went wrong with the retrieval of the file.
   */
  private String keystoreEncode(String keystoreFilePath) throws DeviceServiceException {
    try {
      FileInputStream kfin = new FileInputStream(new File(keystoreFilePath));
      byte[] kStoreBlob = ByteStreams.toByteArray(kfin);
      return Base64.encodeBase64String(kStoreBlob);
    } catch (FileNotFoundException e) {
      throw new DeviceServiceException(new DeviceResponseBuilder().PRODUCE_KEYSTORE_FILE_NOT_FOUND);
    } catch (IOException e) {
      throw new DeviceServiceException(new DeviceResponseBuilder().PRODUCE_KEYSTORE_IO_EXCEPTION);
    }
  }

  /**
   * This method converts the provided records in JSONArray format into a List of Avro-formatted records.
   *
   * @param avroSchemaContents the avro schema for the records.
   * @param records the records to be converted.
   * @return A List of Avro-formatted records
   * @throws IOException
   */
  private List<GenericData.Record> toAvro(String avroSchemaContents, JSONArray records)
      throws IOException {

    ArrayList<GenericData.Record> list = new ArrayList<>();
    for (int i = 0; i < records.length(); i++) {
      JSONObject json = records.getJSONObject(i);
      Schema.Parser parser = new Schema.Parser();
      Schema schema = parser.parse(avroSchemaContents);
      Decoder decoder = DecoderFactory.get().jsonDecoder(schema, json.toString());
      DatumReader<GenericData.Record> reader = new GenericDatumReader<>(schema);
      list.add(reader.read(null, decoder));
    }
    return list;
  }

  /**
   * Registers a device under a project.
   *
   * @param projectName
   * @param req
   * @param deviceDTO
   * @return
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  @POST
  @Path("/{projectName}/register")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postRegisterEndpoint(@PathParam("projectName") String projectName,
      @Context HttpServletRequest req, AuthDeviceDTO deviceDTO) throws AppException {
    try {
      validate(deviceDTO);
      try {
        Project project = projectFacade.findByName(projectName);
        deviceDTO.setPassword(DigestUtils.sha256Hex(deviceDTO.getPassword()));
        deviceFacade.createProjectDevice(project.getId(), deviceDTO);
        return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
      } catch (Exception e) {
        return new DeviceResponseBuilder().DEVICE_ALREADY_REGISTERED;
      }
    } catch (DeviceServiceException e) {
      return e.getResponse();
    }
  }

  /**
   * Logs in a device under a project.
   *
   * @param projectName
   * @param req
   * @param deviceDTO
   * @return
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  @POST
  @Path("/{projectName}/login")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response postLoginEndpoint(@PathParam("projectName") String projectName, @Context HttpServletRequest req,
      AuthDeviceDTO deviceDTO) throws AppException {
    try {
      validate(deviceDTO);
      Project project = projectFacade.findByName(projectName);
      ProjectDevice device = deviceFacade.readProjectDevice(project.getId(), deviceDTO.getDeviceUuid());
      if (device == null) {
        return new DeviceResponseBuilder().DEVICE_NOT_REGISTERED;
      }

      // Validates that the Device is in the Approved State.
      if (device.getState() != ProjectDevice.State.Approved) {
        if (device.getState() == ProjectDevice.State.Disabled) {
          return new DeviceResponseBuilder().DEVICE_DISABLED;
        }
        if (device.getState() == ProjectDevice.State.Pending) {
          return new DeviceResponseBuilder().DEVICE_PENDING;
        }
        return new DeviceResponseBuilder().DEVICE_UNKNOWN_STATE;
      }

      if (device.getPassword().equals(DigestUtils.sha256Hex(deviceDTO.getPassword()))) {
        deviceFacade.updateProjectDeviceLastLoggedIn(project.getId(), deviceDTO);
        return DeviceResponseBuilder.successfulJsonResponse(
            Status.OK, DeviceServiceSecurity.generateJwt(
                deviceFacade.readProjectDevicesSettings(project.getId()), device));
      }
      return new DeviceResponseBuilder().DEVICE_LOGIN_FAILED;
    } catch (DeviceServiceException e) {
      return e.getResponse();
    }
  }

  /**
   * Endpoint to verify the jwt token provided in the Authorization Header.
   * Useful for testing purposes for developers of devices that are integrating towards hopsworks.
   */
  @POST
  @Path("/{projectName}/verify-token")
  @DeviceJwtTokenRequired
  @Produces(MediaType.APPLICATION_JSON)
  public Response postVerifyTokenEndpoint(
      @PathParam("projectName") String projectName, @Context HttpServletRequest req) throws AppException {
    return DeviceResponseBuilder.successfulJsonResponse(Status.OK);
  }

  /**
   * Endpoint to get the schema of a topic under a project.
   * Useful for checking the schema of the topic on the client before producing.
   *
   * @param projectName
   * @param req
   * @return
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  @GET
  @Path("/{projectName}/topic-schema")
  @DeviceJwtTokenRequired
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTopicSchemaEndpoint(
      @PathParam("projectName") String projectName, @Context HttpServletRequest req) throws AppException {

    Integer projectId = (Integer) req.getAttribute(DeviceServiceSecurity.PROJECT_ID);
    String topicName = req.getParameter("topic");

    try {
      SchemaDTO schemaDTO = getSchemaForTopic(projectId, topicName);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(schemaDTO).build();

    } catch (DeviceServiceException e) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    }

  }

  /**
   * Endpoint to produce to kafka the specified records to the specified topic of the specified project.
   * @param projectName
   * @param req
   * @param jsonString
   * @return 
   * @throws io.hops.hopsworks.common.exception.AppException 
   */
  @POST
  @Path("/{projectName}/produce")
  @DeviceJwtTokenRequired
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public Response postProduceEndpoint(@PathParam("projectName") String projectName, @Context HttpServletRequest req,
      String jsonString) throws AppException {

    Users user = null;
    Project project = null;

    try {
      // projectId and deviceUuid are injected from the jwtToken into the Context by the DeviceJwtAuthFilter.
      Integer projectId = (Integer) req.getAttribute(DeviceServiceSecurity.PROJECT_ID);
      String deviceUuid = (String) req.getAttribute(DeviceServiceSecurity.DEVICE_UUID);

      // Extracts all the json parameters
      JSONObject json = new JSONObject(jsonString);
      String topicName = json.getString("topic");
      JSONArray records = json.getJSONArray("records");

      // Extracts the default device-user from the database
      user = userFacade.findByEmail(DeviceServiceSecurity.DEFAULT_DEVICE_USER_EMAIL);

      // Extracts the project from the database
      project = projectFacade.find(projectId);

      // Extracts the Avro Schema contents from the database
      SchemaDTO schemaDTO;
      try {
        schemaDTO = getSchemaForTopic(projectId, topicName);
      } catch (DeviceServiceException e) {
        return e.getResponse();
      }

      HopsUtils.copyUserKafkaCerts(userCerts, project, user.getUsername(), settings.getHopsworksTmpCertDir(),
          settings.getHdfsTmpCertDir(), certificateMaterializer, settings.getHopsRpcTls());

      String keyStoreFilePath = settings.getHopsworksTmpCertDir() + File.separator + HopsUtils.getProjectKeystoreName(
          project.getName(), user.getUsername());

      String base64EncodedKeyStore = keystoreEncode(keyStoreFilePath);

      CertPwDTO certPwDTO;
      try {
        certPwDTO = projectController.getProjectSpecificCertPw(user, project.getName(), base64EncodedKeyStore);
      } catch (Exception e) {
        return new DeviceResponseBuilder().PROJECT_USER_PASS_FOR_KS_TS_NOT_FOUND;
      }

      List<GenericData.Record> avroRecords = toAvro(schemaDTO.getContents(), records);
      List<AckRecordDTO> acks = kafkaFacade.produce(
          true, project, user, certPwDTO, deviceUuid, topicName, schemaDTO.getContents(), avroRecords);
      GenericEntity<List<AckRecordDTO>> listAcks = new GenericEntity<List<AckRecordDTO>>(acks) {
      };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(listAcks).build();
    } catch (JSONException e) {
      return new DeviceResponseBuilder().MISSING_PARAMS;
    } catch (DeviceServiceException e) {
      return e.getResponse();
    } catch (Exception e) {
      return new DeviceResponseBuilder().UNEXPECTED_ERROR;
    } finally {
      if (user != null && project != null) {
        certificateMaterializer.removeCertificate(user.getUsername(), project.getName());
      }
    }

  }

}
