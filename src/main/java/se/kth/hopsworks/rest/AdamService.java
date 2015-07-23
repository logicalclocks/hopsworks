package se.kth.hopsworks.rest;

import java.io.IOException;
import java.util.logging.Level;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.adam.AdamCommand;
import se.kth.bbc.jobs.adam.AdamCommandDTO;
import se.kth.bbc.jobs.adam.AdamJobConfiguration;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.controller.AdamController;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 *
 * @author stig
 */
@RequestScoped
public class AdamService {

  private static final Logger logger = Logger.getLogger(AdamService.class.
          getName());

  @EJB
  private AdamController adamController;

  private Project project;

  AdamService setProject(Project project) {
    this.project = project;
    return this;
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
