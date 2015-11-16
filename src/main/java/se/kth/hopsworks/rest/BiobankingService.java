package se.kth.hopsworks.rest;

import io.hops.bbc.ConsentDTO;
import io.hops.bbc.ConsentStatus;
import io.hops.bbc.ConsentType;
import io.hops.bbc.Consents;
import io.hops.bbc.ConsentsFacade;
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
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import java.util.ArrayList;
import javax.ws.rs.POST;
import javax.ws.rs.core.GenericEntity;
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
  @EJB
  private ConsentsFacade consentsFacade;
  @EJB
  private InodeFacade inodeFacade;

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

      String projectPath = "/" + Settings.DIR_ROOT + "/" + project.getName();
      String consentsPath = projectPath + "/" + Settings.DIR_CONSENTS;
      logger.log(Level.INFO, "Request to get all consent forms in: {0}", consentsPath);
      if (fops.exists(consentsPath) == false) {
        return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Consents path was missing").build();
      }
      // Get all entries for consents in this project in the consents_table in the DB
      // Return two lists: consents in the consents table, and consents not in the consents table.

      List<Inode> filesAvailable = fops.getChildInodes(consentsPath);
      logger.log(Level.INFO, "Consent forms files: {0}", filesAvailable.size());
      List<Consents> registeredConsents = consentsFacade.findAllInProject(project.getId());
      List<ConsentDTO> allConsents = new ArrayList<>();
      for (Consents c : registeredConsents) {
        String path = relativePath(inodeFacade.getPath(c.getInode()));
        allConsents.add(new ConsentDTO(path, c.getConsentType(), c.getConsentStatus()));
      }
      if (!filesAvailable.isEmpty()) {
        for (Inode i : filesAvailable) {
          boolean registered = false;
          if (registeredConsents.isEmpty() == false) {
            for (Consents c : registeredConsents) {
              if (c.getInode().equals(i)) {
                registered = true;
                break;
              }
            }
          }
          if (registered == false) {
            String path = relativePath(inodeFacade.getPath(i));
            allConsents.add(new ConsentDTO(path));
          }
        }
      }
      logger.info("Num of consent forms found: " + allConsents.size());

      GenericEntity<List<ConsentDTO>> consents
          = new GenericEntity<List<ConsentDTO>>(allConsents) {
          };

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
          consents).build();
    } catch (IOException ex) {
      Logger.getLogger(BiobankingService.class.getName()).log(Level.SEVERE, null, ex);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).entity(
          ex.getMessage()).build();

    }
  }

  private String relativePath(String path) {
    logger.info("relative path for: " + path);
    return path.replace("/" + Settings.DIR_ROOT + "/" + project.getName() + "/", "");
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response registerConsentForm(@Context SecurityContext sc,
      @Context HttpServletRequest req, ConsentDTO consent)
      throws AppException {
    logger.log(Level.INFO, "Registering consent: {0}", consent);
    String path = "/" + Settings.DIR_ROOT + "/" + project.getName() + "/" + consent.getPath();
    Inode i = inodeFacade.getInodeAtPath(path);
    if (i == null) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).entity(
          "Could not find file: " + consent.getPath()).build();
    }
    if (ConsentType.create(consent.getConsentType()).equals(ConsentType.UNDEFINED)) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.INTERNAL_SERVER_ERROR).entity(
          "You need to change the Consent Type to register the consent form for: " + consent.getPath()).build();
    }
    Consents consentBean = new Consents(ConsentType.create(consent.getConsentType()),
        ConsentStatus.PENDING, i, project);
    consentsFacade.persistConsent(consentBean);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }
}
