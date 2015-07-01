package se.kth.hopsworks.rest;

import de.huberlin.wbi.cuneiform.core.semanticmodel.HasFailedException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
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
import se.kth.bbc.jobs.cuneiform.model.CuneiformRunWrapper;
import se.kth.bbc.jobs.cuneiform.model.WorkflowDTO;
import se.kth.bbc.jobs.jobhistory.JobHistory;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;
import se.kth.hopsworks.controller.CuneiformController;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 *
 * @author stig
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CuneiformService {

  @EJB
  private CuneiformController cfCtrl;
  @EJB
  private NoCacheResponse noCacheResponse;

  private Integer projectId;

  CuneiformService setProjectId(Integer id) {
    this.projectId = id;
    return this;
  }

  /**
   * Inspect the workflow stored at the given path. Returns a CuneiformWrapper
   * containing a WorkflowDTO with the workflow details and a
   * YarnJobConfiguration with Yarn job config.
   * <p>
   * @param path
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("/inspect/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response inspectStoredWorkflow(@PathParam("path") String path,
          @Context SecurityContext sc, @Context HttpServletRequest req)
          throws AppException {
    try {
      WorkflowDTO wf = cfCtrl.inspectWorkflow(path);
      CuneiformRunWrapper ret = new CuneiformRunWrapper(
              new YarnJobConfiguration(), wf);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(ret).build();
    } catch (IOException |
            HasFailedException ex) {
      Logger.getLogger(CuneiformService.class.getName()).log(Level.SEVERE,
              "Error upon inspecting workflow.",
              ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Failed to inspect the workflow file.");
    } catch (IllegalArgumentException ex) {
      Logger.getLogger(CuneiformService.class.getName()).log(Level.WARNING,
              "Error upon inspecting workflow:",
              ex);
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              "Failed not inspect the workflow file. Reason:" + ex.getMessage());
    }
  }

  /**
   * Run a workflow. The workflowDTO is passed as an argument. The workflow is
   * based on the given path. This call returns a JobHistory object if the call
   * succeeds.
   * <p>
   * @param runData
   * @param sc
   * @param req
   * @return
   * @throws se.kth.hopsworks.rest.AppException
   */
  @POST
  @Path("/run")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response runWorkFlow(CuneiformRunWrapper runData,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    try {
      JobHistory jh = cfCtrl.startWorkflow(runData, req.getUserPrincipal().
              getName(), projectId);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(jh).build();
    } catch (IOException ex) {
      Logger.getLogger(CuneiformService.class.getName()).log(Level.SEVERE,
              "Error running Cuneiform job.",
              ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Error running job.");
    }
  }

}
