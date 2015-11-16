package se.kth.hopsworks.rest;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.project.ConsentsDTO;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.util.Settings;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class BiobankingService {

  private static final Logger logger = Logger.getLogger(BiobankingService.class.
      getName());

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FileOperations fops;
  @EJB
  private ActivityFacade activityFacade;

  private Project project;

  BiobankingService setProject(Project project) {
    this.project = project;
    return this;
  }

  /**
   * Get all the jobs in this project.
   * <p/>
   * @param sc
   * @param req
   * @return A list of all defined Jobs in this project.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getConsentForms(@Context SecurityContext sc,
      @Context HttpServletRequest req)
      throws AppException {

    try {
      String rootDir = Settings.DIR_ROOT;
      String projectPath = File.separator + rootDir + File.separator + project.getName();
      String consentsPath = projectPath + File.separator + Settings.DIR_CONSENTS;
      if (fops.exists(consentsPath)==false)  {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).entity(
          "Consents path was missing").build();
      }
      List<String> filesAvailable = fops.getChildNames(consentsPath);
      
      // Get all entries for consents in this project in the consents_table in the DB
      
      // Return two lists: consents in the consents table, and consents not in the consents table.
      
      ConsentsDTO consents = null;
      
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
          consents).build();
    } catch (IOException ex) {
      Logger.getLogger(BiobankingService.class.getName()).log(Level.SEVERE, null, ex);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).entity(
          ex.getMessage()).build();
      
    }
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response registerConsentForm(@Context SecurityContext sc,
      @Context HttpServletRequest req, ConsentsDTO consents)
      throws AppException {
  
      
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
    
    
  }
}
