package se.kth.hopsworks.rest;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 *
 * @author stig
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CuneiformService {

  @EJB
  private FileOperations fops;

  @GET
  @Path("/inspect/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response inspectStoredWorkflow(@PathParam("path") String path,
          @Context SecurityContext sc, @Context HttpServletRequest reqJobType)
          throws AppException {
    //Get content as string
    // Inspect.
    // Return a workflow object, containing: 
    //  name
    //  input params, with their binding
    //  output params, and if queried
    return null;
  }

}
