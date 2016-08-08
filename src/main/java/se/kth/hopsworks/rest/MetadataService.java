package se.kth.hopsworks.rest;

import java.io.File;
import java.util.HashSet;
import java.io.StringReader;
import java.util.ArrayList;
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
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
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
import javax.xml.crypto.Data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.apache.hadoop.security.AccessControlException;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.meta.db.MTableFacade;
import se.kth.hopsworks.meta.db.TemplateFacade;
import se.kth.hopsworks.meta.db.TupleToFileFacade;
import se.kth.hopsworks.meta.entity.EntityIntf;
import se.kth.hopsworks.meta.entity.Field;
import se.kth.hopsworks.meta.entity.InodeTableComposite;
import se.kth.hopsworks.meta.entity.MTable;
import se.kth.hopsworks.meta.entity.Metadata;
import se.kth.hopsworks.meta.entity.MetadataView;
import se.kth.hopsworks.meta.entity.RawData;
import se.kth.hopsworks.meta.entity.Template;
import se.kth.hopsworks.meta.entity.TemplateView;
import se.kth.hopsworks.meta.entity.TupleToFile;
import se.kth.hopsworks.meta.exception.ApplicationException;
import se.kth.hopsworks.meta.exception.DatabaseException;
import se.kth.hopsworks.meta.wscomm.Protocol;
import se.kth.hopsworks.meta.wscomm.Utils;
import se.kth.hopsworks.meta.wscomm.message.ContentMessage;
import se.kth.hopsworks.meta.wscomm.message.Message;
import se.kth.hopsworks.meta.wscomm.message.TemplateMessage;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.users.UserFacade;
import se.kth.hopsworks.util.Settings;
import se.kth.hopsworks.util.JsonUtil;

/**
 *
 * @author vangelis
 */
@Path("/metadata")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MetadataService {

  private final static Logger logger = Logger.getLogger(MetadataService.class.
          getName());

  private enum MetadataOp {
    ADD, UPDATE, REMOVE
  };

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
  @EJB
  private Utils utils;
  @EJB
  private UserFacade userFacade;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;

  private String path;

  /**
   * Uploads a template file (.json) to the file system (hopsfs) and persists it to the database as well
   * <p/>
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
    this.path = File.separator + Settings.DIR_ROOT + File.separator + path;

    //sanitize the path
    if (!path.endsWith(File.separator)) {
      this.path += File.separator;
    }

    this.uploader.setUploadPath(this.path);
    this.uploader.setIsTemplate(true);

    return this.uploader;
  }

  /**
   * Fetch all metadata associated to an inode
   * <p/>
   * @param inodePid
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("{inodepid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response fetchMetadataCompact(
          @PathParam("inodepid") Integer inodePid,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (inodePid == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode templates, tables, fields;
    ArrayNode values;

    Inode inode = this.inodefacade.findById(inodePid);
    HashSet<Integer> tuples = new HashSet<Integer>();
    try {
      for(TupleToFile tuple: ttf.getTuplesByInodeId(inode.getInodePK().getParentId(), inode.getInodePK().getName())){
        tuples.add(tuple.getId());
      }
    }catch(DatabaseException e){
      logger.log(Level.WARNING, "Trying to fetch metadata.", e);
      throw new AppException(Response.Status.NO_CONTENT.getStatusCode(),
              "Could not fetch the metadata for the file " + inode.getInodePK().getName() + ".");
    }
    templates = mapper.createObjectNode();
    for(Template template : inode.getTemplates()){
      tables = mapper.createObjectNode();
      for(MTable table : template.getMTables()){
        fields = mapper.createObjectNode();
        for (Field field : table.getFields()) {
          values =  mapper.createArrayNode();
          for (RawData raw : field.getRawData()) {
            if (!tuples.contains(raw.getRawdataPK().getTupleid())) continue;
            for (Metadata mt : raw.getMetadata()) {
                values.add(mt.getData());
            }
          }
          fields.put(field.getName(), values);
        }
        tables.put(table.getName(), fields);
      }
      templates.put(template.getName(), tables);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(templates.toString()).build();
  }

  /**
   * Fetch metadata associated to an inode by table id
   * <p/>
   * @param inodePid
   * @param inodeName
   * @param tableid
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("fetchmetadata/{inodepid}/{inodename}/{tableid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response fetchMetadata(
          @PathParam("inodepid") Integer inodePid,
          @PathParam("inodename") String inodeName,
          @PathParam("tableid") Integer tableid,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (inodePid == null || inodeName == null || tableid == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    //metadata associated to a specific table and inode
    List<MetadataView> metadata = new LinkedList<>();

    try {
      Inode inode = this.inodefacade.findById(inodePid);
      List<Template> nodeTemplates = new LinkedList<>(inode.getTemplates());

      List<TupleToFile> tuples = ttf.getTuplesByInodeId(inodePid, inodeName);
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
              "Could not fetch the metadata for the file " + inodeName + ".");
    }
  }

  /**
   * Fetches all templates attached to a specific inode
   * <p/>
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
   * <p/>
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
   * <p/>
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

    //keep only the selected template to remove
    for (Template template : templates) {
      if (Objects.equals(template.getId(), templateid)) {
        template.getInodes().remove(inode);
        toremove = template;
      }
    }

    try {

      //remove the inode-template association
      this.templatefacade.updateTemplatesInodesMxN(toremove);

      //remove any associated metadata
      List<TupleToFile> tuples = ttf.getTuplesByInodeId(inode.getInodePK().
              getParentId(), inode.getInodePK().getName());

      if (toremove != null) {
        //scan all tables and fields this template carries
        for (MTable table : toremove.getMTables()) {
          for (Field field : table.getFields()) {

            //apply the filter
            for (RawData raw : field.getRawData()) {
              for (TupleToFile tuple : tuples) {
                if (raw.getRawdataPK().getTupleid() == tuple.getId()) {
                  this.ttf.deleteTTF(tuple);
                }
              }
            }
          }
        }
      }

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
   * <p/>
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
    ((ContentMessage) templateMessage).setTemplateid(templateid);

    Message message = this.protocol.GFR(templateMessage);

    JsonResponse response = new JsonResponse();
    //message contains all template content
    response.setSuccessMessage(message.getMessage());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            response).build();
  }

  @POST
  @Path("addWithSchema")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response addMetadataWithSchema(
          @Context SecurityContext sc, @Context HttpServletRequest req,
          String metaObj) throws
          AppException, AccessControlException {
    String email = sc.getUserPrincipal().getName();
    return mutateMetadata(email, metaObj, MetadataOp.ADD);
  }

  @POST
  @Path("updateWithSchema")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response updateMetadataWithSchema(
          @Context SecurityContext sc, @Context HttpServletRequest req,
          String metaObj) throws
          AppException, AccessControlException {

    String email = sc.getUserPrincipal().getName();
    return mutateMetadata(email, metaObj, MetadataOp.UPDATE);
  }

  @POST
  @Path("removeWithSchema")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response removeMetadataWithSchema(
          @Context SecurityContext sc, @Context HttpServletRequest req,
          String metaObj) throws
          AppException, AccessControlException {
    String email = sc.getUserPrincipal().getName();
    return mutateMetadata(email, metaObj, MetadataOp.REMOVE);
  }

  private Response mutateMetadata(String email, String metaObj, MetadataOp op) throws AppException {
    if (op == null || email == null || metaObj == null) {
      throw new NullPointerException("MetadataOp  or email or metaObj was null");
    }
    // Get Inode. Get project for the inode. Check if the user has DATA OWNER role for that project (privileges to add metadata)
    InodeTableComposite itc = JsonUtil.parseSchemaHeader(metaObj);
    //variables not set in the json message
    if (itc == null) {
      Logger.getLogger(MetadataService.class.getName()).log(Level.SEVERE, "Badly formatted json message/Missing values", new NullPointerException());
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Badly formatted json message/Missing values");
    }
    Inode parent = inodeFacade.findById(itc.getInodePid());
    if (parent == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Incorrect json message/Missing or incorrect parent inodeId");
    }
    Inode inode = inodeFacade.findByParentAndName(parent, itc.getInodeName());
    if (inode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Incorrect json message/Missing or incorrect inode name");
    }
    Inode projectInode = inodeFacade.getProjectRootForInode(inode);
    Project project = projectFacade.findByInodeId(projectInode.getInodePK().getParentId(), projectInode.getInodePK().getName());

    Users user = userFacade.findByEmail(email);

    JsonResponse json = new JsonResponse();
    Response.Status status = Response.Status.FORBIDDEN;
    String userRole = projectTeamFacade.findCurrentRole(project, user);

    if (userRole != null && userRole.isEmpty() == false && userRole.compareToIgnoreCase(AllowedRoles.DATA_OWNER) == 0) {
      List<EntityIntf> composite = new ArrayList<>();
      composite.add(itc);
      try {
        JsonObject obj = Json.createReader(new StringReader(metaObj)).readObject();
        
        int metaId = 0;
        switch (op) {
          case REMOVE:
            metaId = obj.getInt("metaid");
            JsonString metaObjPayload = obj.getJsonString("metadata");
            if (metaObjPayload == null) {
              throw new ApplicationException("removed metadata was null");
            }
            this.utils.removeMetadata(composite, metaId, metaObjPayload.getString());
            break;
          case UPDATE:
            metaId = obj.getInt("metaid");
            JsonObject updatedObj = obj.getJsonObject("metadata");
            if (updatedObj == null) {
              throw new ApplicationException("updated metadata was null");
            }
            JsonString metaObjValue = updatedObj.getJsonString("data");
            if (metaObjValue == null) {
              throw new ApplicationException("updated metadata value was null");
            }
            this.utils.updateMetadata(composite, metaId, metaObjValue.getString());
            break;
          case ADD:
            List<EntityIntf> rawData = JsonUtil.parseSchemaPayload(metaObj);
            //Persist metadata
            this.utils.storeRawData(composite, rawData);
            break;
          default:
            throw new IllegalStateException("Invalid metadata operation: " + op);
        }
        json.setSuccessMessage("Metadata deleted");
        status = Response.Status.OK;
      } catch (ApplicationException | NullPointerException | ClassCastException ex) {
        Logger.getLogger(MetadataService.class.getName()).log(Level.SEVERE, null, ex);
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), "Incorrect json message/Missing raw values");
      }

    } else {
      json.setErrorMsg("You do not have permission to modify metadata in this project.");
    }
    return noCacheResponse.getNoCacheResponseBuilder(status).entity(json).build();

  }
}
