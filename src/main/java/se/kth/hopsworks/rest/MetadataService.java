package se.kth.hopsworks.rest;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.meta.db.MTableFacade;
import se.kth.hopsworks.meta.db.TemplateFacade;
import se.kth.hopsworks.meta.db.TupleToFileFacade;
import se.kth.hopsworks.meta.entity.Field;
import se.kth.hopsworks.meta.entity.MTable;
import se.kth.hopsworks.meta.entity.Metadata;
import se.kth.hopsworks.meta.entity.MetadataView;
import se.kth.hopsworks.meta.entity.RawData;
import se.kth.hopsworks.meta.entity.Template;
import se.kth.hopsworks.meta.entity.TemplateView;
import se.kth.hopsworks.meta.entity.TupleToFile;
import se.kth.hopsworks.meta.exception.DatabaseException;
import se.kth.hopsworks.meta.wscomm.Protocol;
import se.kth.hopsworks.meta.wscomm.message.ContentMessage;
import se.kth.hopsworks.meta.wscomm.message.Message;
import se.kth.hopsworks.meta.wscomm.message.TemplateMessage;

/**
 *
 * @author vangelis
 */
@Path("/metadata")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MetadataService {

  private final static Logger logger = Logger.getLogger(MetadataService.class.
          getName());
  @Inject
  private UploadService uploader;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private TupleToFileFacade ttf;
  @EJB
  private MTableFacade mtf;
  @EJB
  private TemplateFacade templatefacade;
  @EJB
  private InodeFacade inodefacade;
  @EJB
  private Protocol protocol;

  private String path;

  /**
   * Uploads a template file (.json) to the file system (hopsfs) and persists it
   * to the database as well
   * <p>
   * @param path
   * @return
   * @throws AppException
   */
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
    if (!path.endsWith(File.separator)) {
      this.path += File.separator;
    }

    this.uploader.setUploadPath(this.path);

    return this.uploader;
  }

  /**
   * Fetch metadata associated to an inode by table id
   * <p>
   * @param inodeid
   * @param tableid
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("fetchmetadata/{inodeid}/{tableid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response fetchMetadata(
          @PathParam("inodeid") Integer inodeid,
          @PathParam("tableid") Integer tableid,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (inodeid == null || tableid == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    //metadata associated to a specific table and inode
    List<MetadataView> metadata = new LinkedList<>();

    try {
      List<TupleToFile> tuples = ttf.getTuplesByInode(inodeid);
      MTable table = mtf.getTable(tableid);

      List<Field> fields = table.getFields();

      //groups metadata per table
      MetadataView metatopView = new MetadataView(table.getName());

      for (Field field : fields) {
        //groups metadata per field
        MetadataView metainnerView = new MetadataView(field.getName());

        /*
         * Load raw data based on the field id. Keep only data related to
         * a specific inode
         */
        List<RawData> rawList = field.getRawData();

        for (RawData raw : rawList) {
          for (TupleToFile tuple : tuples) {

            //filter out irrelevant metadata
            if (raw.getRawdataPK().getTupleid() == tuple.getId()) {
              List<Metadata> meta = new LinkedList<>(raw.getMetadata());

              for (Metadata mt : meta) {
                //all metadata for an inode a field carries
                metainnerView.getMetadataView().add(new MetadataView(mt.
                        getMetadataPK().getId(), mt.getData()));
              }
            }
          }
        }
        metatopView.getMetadataView().add(metainnerView);
      }

      metadata.add(metatopView);
      GenericEntity<List<MetadataView>> response
              = new GenericEntity<List<MetadataView>>(metadata) {
              };

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(response).build();

    } catch (DatabaseException ex) {
      logger.log(Level.WARNING, "Trying to fetch metadata.", ex);
      throw new AppException(Response.Status.NO_CONTENT.getStatusCode(),
              "Could not fetch the metadata for the file " + inodeid + ".");
    }
  }

  /**
   * Fetches all templates attached to a specific inode
   * <p>
   * @param inodeid
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("fetchtemplatesforinode/{inodeid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response fetchTemplatesforInode(
          @PathParam("inodeid") Integer inodeid,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (inodeid == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    Inode inode = this.inodefacade.findById(inodeid);
    List<Template> templates = new LinkedList<>(inode.getTemplates());
    List<TemplateView> tViews = new LinkedList<>();

    for (Template template : templates) {
      tViews.add(new TemplateView(template.getId(), template.getName()));
    }

    GenericEntity<List<TemplateView>> t
            = new GenericEntity<List<TemplateView>>(tViews) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            t).build();
  }

  /**
   * Fetches all templates not attached to a specific inode
   * <p>
   * @param inodeid
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("fetchavailabletemplatesforinode/{inodeid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response fetchAvailableTemplatesForInode(
          @PathParam("inodeid") Integer inodeid,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (inodeid == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    Inode inode = this.inodefacade.findById(inodeid);
    List<Template> nodeTemplates = new LinkedList<>(inode.getTemplates());
    List<Template> allTemplates = this.templatefacade.findAll();
    List<TemplateView> toreturn = new LinkedList<>();

    boolean found = false;
    for (Template template : allTemplates) {
      found = false;

      for (Template temp : nodeTemplates) {
        if (Objects.equals(template.getId(), temp.getId())) {
          found = true;
        }
      }
      if (!found) {
        toreturn.add(new TemplateView(template.getId(), template.getName()));
      }
    }

    GenericEntity<List<TemplateView>> t
            = new GenericEntity<List<TemplateView>>(toreturn) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            t).build();
  }

  /**
   * Fetch all templates attached to a specific inode
   * <p>
   * @param inodeid
   * @param templateid
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("detachtemplate/{inodeid}/{templateid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response detachTemplateFromInode(
          @PathParam("inodeid") Integer inodeid,
          @PathParam("templateid") Integer templateid,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (inodeid == null || templateid == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    Inode inode = this.inodefacade.findById(inodeid);
    List<Template> templates = new LinkedList<>(inode.getTemplates());

    if (templates.isEmpty()) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "The inode does not have any templates attached to it");
    }

    Template toremove = null;

    for (Template template : templates) {
      if (Objects.equals(template.getId(), templateid)) {
        template.getInodes().remove(inode);
        toremove = template;
      }
    }

    try {
      this.templatefacade.updateTemplatesInodesMxN(toremove);
    } catch (DatabaseException e) {
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Failed to remove template from inode " + inodeid);
    }

    JsonResponse json = new JsonResponse();
    json.setSuccessMessage("The template was successfully removed from inode "
            + inode.getInodePK().getName());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  /**
   * Fetches the content of a given template
   * <p>
   * @param templateid
   * @param sender
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("fetchtemplate/{templateid}/{sender}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response fetchTemplate(
          @PathParam("templateid") Integer templateid,
          @PathParam("sender") String sender,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (templateid == null || sender == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }
    
    String json = "{\"message\": \"{\"tempid\": " + templateid + "}\"}";

    //create a request message
    TemplateMessage templateMessage = new TemplateMessage();
    templateMessage.setSender(sender);
    templateMessage.setAction("fetch_template");
    templateMessage.setMessage(json);
    ((ContentMessage)templateMessage).setTemplateid(templateid);
    
    Message message = this.protocol.GFR(templateMessage);
    
    JsonResponse response = new JsonResponse();
    //message contains all template content
    response.setSuccessMessage(message.getMessage());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            response).build();
  }
}
