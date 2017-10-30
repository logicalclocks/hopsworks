package io.hops.hopsworks.api.project;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.cert.CertPwDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.project.ProjectController;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

/**
 *
 * <p>
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CertService {

  private final static Logger LOGGER = Logger.getLogger(CertService.class.getName());
  @EJB
  private UserFacade userFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private ProjectController projectController;

  private Project project;

  public CertService setProject(Project project) {
    this.project = project;
    return this;
  }

  @GET
  @Path("/certpw")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response getCertPw(@QueryParam("keyStore") String keyStore,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    //Find user
    String userEmail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(userEmail);
    try {
      CertPwDTO respDTO = projectController.getProjectSpecificCertPw(user, project.getName(), keyStore);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(respDTO).build();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Could not retrieve certificate passwords for user:" + user.getUsername(), ex);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.EXPECTATION_FAILED).build();
    }
  }

}
