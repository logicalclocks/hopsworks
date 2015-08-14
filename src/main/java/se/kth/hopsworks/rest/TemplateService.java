package se.kth.hopsworks.rest;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import se.kth.bbc.lims.Constants;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 *
 * @author vangelis
 */
@Path("/template")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TemplateService {

  private final static Logger logger = Logger.getLogger(TemplateService.class.
          getName());
  @Inject
  private UploadService uploader;
  
  private String path;

  @Path("upload/{path: .+}")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public UploadService upload(
          @PathParam("path") String path) throws AppException {

    if (path == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.UPLOAD_PATH_NOT_SPECIFIED);
    }
    this.path = File.separator + Constants.DIR_ROOT + File.separator + path;

    //sanitize the path
    if(!path.endsWith(File.separator)){
      this.path += File.separator;
    }
    
    logger.log(Level.INFO, "UPLOADING THE FUCKING TEMPLATE TO PATH {0} ", this.path);
    this.uploader.setUploadPath(this.path);

    return this.uploader;
  }
}
