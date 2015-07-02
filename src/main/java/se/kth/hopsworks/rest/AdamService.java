package se.kth.hopsworks.rest;

import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.jobs.adam.AdamCommand;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 *
 * @author stig
 */
@RequestScoped
public class AdamService {
  
  private Integer projectId;
  
  AdamService setProjectId(Integer id) {
    this.projectId = id;
    return this;
  }
  
  /**
   * Get a list of the available Adam commands. 
   * @param sc
   * @param req
   * @return 
   */
  @GET
  @Path("/commands")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getAdamCommands(@Context SecurityContext sc, @Context HttpServletRequest req){
    AdamCommand[] commands = AdamCommand.values();
    return Response.ok(commands).build();
  }
}
