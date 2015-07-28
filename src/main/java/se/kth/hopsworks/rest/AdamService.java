package se.kth.hopsworks.rest;

import com.google.common.base.Strings;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.adam.AdamCommand;
import se.kth.bbc.jobs.adam.AdamCommandDTO;
import se.kth.bbc.jobs.adam.AdamJobConfiguration;
import se.kth.bbc.jobs.model.configuration.JobConfiguration;
import se.kth.bbc.jobs.model.description.AdamJobDescription;
import se.kth.bbc.jobs.model.description.JobDescription;
import se.kth.bbc.jobs.model.description.JobDescriptionFacade;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;

/**
 *
 * @author stig
 */
@RequestScoped
public class AdamService {

  private static final Logger logger = Logger.getLogger(AdamService.class.
          getName());

  private Project project;

  @EJB
  private JobDescriptionFacade jobFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private UserFacade userFacade;

  AdamService setProject(Project project) {
    this.project = project;
    return this;
  }

  /**
   * Get all the jobs in this project of type Adam.
   * <p>
   * @param sc
   * @param req
   * @return A list of all JobDescription objects of type Adam in this
   * project.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findAllAdamJobs(@Context SecurityContext sc,
          @Context HttpServletRequest req)
          throws AppException {
    List<AdamJobDescription> jobs = jobFacade.
            findAdamJobsForProject(project);
    GenericEntity<List<AdamJobDescription>> jobList
            = new GenericEntity<List<AdamJobDescription>>(
                    jobs) {
                    };
            return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
                    entity(jobList).build();
  }

  /**
   * Get a list of the available Adam commands. This returns a list of command
   * names.
   * <p>
   * @param sc
   * @param req
   * @return
   */
  @GET
  @Path("/commands")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getAdamCommands(@Context SecurityContext sc,
          @Context HttpServletRequest req) {
    CommandListWrapper wrap = new CommandListWrapper();
    wrap.list = AdamCommandDTO.getAllCommandNames();
    return Response.ok(wrap).build();
  }

  /**
   * Returns a AdamJobConfiguration for the selected command.
   * <p>
   * @param commandName
   * @param sc
   * @param req
   * @return
   */
  @GET
  @Path("/commands/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getCommandDetails(@PathParam("name") String commandName,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) {
    AdamCommandDTO selected = new AdamCommandDTO(AdamCommand.getFromCommand(
            commandName));
    AdamJobConfiguration config = new AdamJobConfiguration(selected);
    return Response.ok(config).build();
  }

  /**
   * Create a new Job definition. If successful, the job is returned.
   * <p>
   * @param config The configuration from which to create a Job.
   * @param sc
   * @param req
   * @return
   * @throws se.kth.hopsworks.rest.AppException
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response createJob(AdamJobConfiguration config,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    if (config == null) {
      throw new AppException(Response.Status.NOT_ACCEPTABLE.getStatusCode(),
              "Cannot create job for a null argument.");
    } else {
      String email = sc.getUserPrincipal().getName();
      Users user = userFacade.findByEmail(email);
      if (user == null) {
        //Should not be possible, but, well...
        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
                "You are not authorized for this invocation.");
      }
      String jobname = Strings.isNullOrEmpty(config.getAppName())
              ? "Untitled Adam job" : config.getAppName();
      JobDescription<? extends JobConfiguration> created = jobFacade.create(
              jobname, user, project, config);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(created).build();
    }
  }

  @XmlRootElement(name = "commands")
  private static class CommandListWrapper {

    @XmlElement(name = "commands")
    String[] list;

    public CommandListWrapper() {
    }

    public void setList(String[] array) {
      this.list = array;
    }
  }
}
