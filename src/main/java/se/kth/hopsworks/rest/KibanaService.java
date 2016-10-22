package se.kth.hopsworks.rest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import io.hops.kafka.AclDTO;
import io.hops.kafka.AclUserDTO;
import io.hops.kafka.TopicDTO;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.hopsworks.filters.AllowedRoles;
import io.hops.kafka.KafkaFacade;
import io.hops.kafka.PartitionDetailsDTO;
import io.hops.kafka.SchemaDTO;
import io.hops.kafka.SharedProjectDTO;
import io.hops.kafka.TopicDefaultValueDTO;
import java.util.logging.Level;
import javax.persistence.EntityExistsException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Cookie;
//import org.apache.avro.Schema;
import se.kth.hopsworks.util.Settings;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class KibanaService {

  private final static Logger LOGGER = Logger.getLogger(KibanaService.class.
          getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private KafkaFacade kafkaFacade;
  @EJB
  private Settings settings;

  private Integer projectId;
  private Project project;
  private String path;

  public KibanaService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
    this.project = this.projectFacade.find(projectId);
    String projectPath = settings.getProjectPath(this.project.getName());
    this.path = projectPath + File.separator;
  }

  public Integer getProjectId() {
    return projectId;
  }

  /**
   * Gets the list of topics for this project
   *
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/kibana")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getKibanaUI(@Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (projectId == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    String trackingUrl = "http://10.0.2.15:8080/hopsworks/kibana";

    ClientConfig config = new DefaultClientConfig();
    Client client = Client.create(config);
    WebResource service = client.resource(trackingUrl);
    final ClientResponse blogResponse = service.get(ClientResponse.class);
    final String blog = blogResponse.getEntity(String.class);
    
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(blog).build();
  }
}
