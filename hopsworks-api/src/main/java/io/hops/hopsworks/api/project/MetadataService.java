/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.project;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.metadata.wscomm.MetadataController;
import io.hops.hopsworks.api.metadata.wscomm.MetadataProtocol;
import io.hops.hopsworks.api.metadata.wscomm.message.ContentMessage;
import io.hops.hopsworks.api.metadata.wscomm.message.Message;
import io.hops.hopsworks.api.metadata.wscomm.message.TemplateMessage;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.api.util.UploadService;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dao.metadata.EntityIntf;
import io.hops.hopsworks.common.dao.metadata.Field;
import io.hops.hopsworks.common.dao.metadata.InodeTableComposite;
import io.hops.hopsworks.common.dao.metadata.MTable;
import io.hops.hopsworks.common.dao.metadata.Metadata;
import io.hops.hopsworks.common.dao.metadata.MetadataView;
import io.hops.hopsworks.common.dao.metadata.RawData;
import io.hops.hopsworks.common.dao.metadata.Template;
import io.hops.hopsworks.common.dao.metadata.TemplateView;
import io.hops.hopsworks.common.dao.metadata.TupleToFile;
import io.hops.hopsworks.common.dao.metadata.db.MTableFacade;
import io.hops.hopsworks.common.dao.metadata.db.TemplateFacade;
import io.hops.hopsworks.common.dao.metadata.db.TupleToFileFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.JsonUtil;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

@Path("/metadata")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Metadata Service", description = "Metadata Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MetadataService {

  private final static Logger LOGGER = Logger.getLogger(MetadataService.class.
          getName());

  private enum MetadataOp {
    ADD,
    UPDATE,
    REMOVE
  }

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
  private MetadataProtocol protocol;
  @EJB
  private MetadataController metadataController;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private InodeController inodeController;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private JWTHelper jWTHelper;

  /**
   * Uploads a template file (.json) to the file system (hopsfs) and persists it
   * to the database as well
   * <p/>
   * @return
   */
  @Path("upload")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public UploadService upload() {
    this.uploader.setParams(null, null, -1, true);
    return this.uploader;
  }

  /**
   * Fetch all metadata associated to an inode
   * <p/>
   * @param inodePid
   * @return
   */
  @GET
  @Path("{projectId}/{inodepid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response fetchMetadataCompact(@PathParam("inodepid") Long inodePid) {
  
    if (inodePid == null) {
      throw new IllegalArgumentException("inodepid was not provided.");
    }

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode templates, tables, fields;
    ArrayNode values;

    Inode inode = inodeFacade.findById(inodePid);
    HashSet<Integer> tuples = new HashSet<>();
    for (TupleToFile tuple : ttf.getTuplesByInodeId(inode.getInodePK().
      getParentId(), inode.getInodePK().getName())) {
      tuples.add(tuple.getId());
    }
    templates = mapper.createObjectNode();
    for (Template template : inode.getTemplates()) {
      tables = mapper.createObjectNode();
      for (MTable table : template.getMTables()) {
        fields = mapper.createObjectNode();
        for (Field field : table.getFields()) {
          values = mapper.createArrayNode();
          for (RawData raw : field.getRawData()) {
            if (!tuples.contains(raw.getRawdataPK().getTupleid())) {
              continue;
            }
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
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            templates.toString()).build();
  }

  /**
   * Fetch metadata associated to an inode by table id
   * <p/>
   * @param inodePid
   * @param inodeName
   * @param tableid
   * @return
   */
  @GET
  @Path("{projectId}/fetchmetadata/{inodepid}/{inodename}/{tableid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response fetchMetadata(
          @PathParam("inodepid") Long inodePid,
          @PathParam("inodename") String inodeName,
          @PathParam("tableid") Integer tableid) {

    if (inodePid == null || inodeName == null || tableid == null) {
      throw new IllegalArgumentException("Incomplete request.");
    }

    //metadata associated to a specific table and inode
    List<MetadataView> metadata = new LinkedList<>();
  
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

  }

  /**
   * Fetches all templates attached to a specific inode
   * <p/>
   * @param inodeid
   * @return
   */
  @GET
  @Path("{projectId}/fetchtemplatesforinode/{inodeid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response fetchTemplatesforInode(@PathParam("inodeid") Long inodeid) {

    if (inodeid == null) {
      throw new IllegalArgumentException("inodeid was not provided.");
    }

    Inode inode = this.inodeFacade.findById(inodeid);
    List<Template> templates = new LinkedList<>(inode.getTemplates());
    List<TemplateView> tViews = new LinkedList<>();

    for (Template template : templates) {
      tViews.add(new TemplateView(template.getId(), template.getName()));
    }

    GenericEntity<List<TemplateView>> t
            = new GenericEntity<List<TemplateView>>(tViews) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            t).build();
  }

  /**
   * Fetches all templates not attached to a specific inode
   * <p/>
   * @param inodeid
   * @return
   */
  @GET
  @Path("{projectId}/fetchavailabletemplatesforinode/{inodeid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response fetchAvailableTemplatesForInode(@PathParam("inodeid") Long inodeid) {
  
    if (inodeid == null) {
      throw new IllegalArgumentException("inodeid was not provided.");
    }

    Inode inode = inodeFacade.findById(inodeid);
    List<Template> nodeTemplates = new LinkedList<>(inode.getTemplates());
    List<Template> allTemplates = this.templatefacade.findAll();
    List<TemplateView> toreturn = new LinkedList<>();

    boolean found;
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
            = new GenericEntity<List<TemplateView>>(toreturn) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            t).build();
  }

  /**
   * Fetch all templates attached to a specific inode
   * <p/>
   * @param inodeid
   * @param templateid
   * @return
   */
  @GET
  @Path("{projectId}/detachtemplate/{inodeid}/{templateid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response detachTemplateFromInode(@PathParam("inodeid") Long inodeid,
          @PathParam("templateid") Integer templateid) throws MetadataException {

    if (inodeid == null || templateid == null) {
      throw new IllegalArgumentException("Either inodeid or templateId were not provided");
    }

    Inode inode = inodeFacade.findById(inodeid);
    List<Template> templates = new LinkedList<>(inode.getTemplates());

    if (templates.isEmpty()) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.TEMPLATE_NOT_ATTACHED, Level.FINE, "inodeid:" + inodeid +
        ", templatedId:"+templateid);
    }

    Template toremove = null;

    //keep only the selected template to remove
    for (Template template : templates) {
      if (Objects.equals(template.getId(), templateid)) {
        template.getInodes().remove(inode);
        toremove = template;
      }
    }
  
    //remove the inode-template association
    this.templatefacade.updateTemplatesInodesMxN(toremove);
  
    metadataController.logTemplateOperation(toremove, inode,
      OperationType.Delete);
  
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

    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage("The template was successfully removed from inode "
            + inode.getInodePK().getName());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }
  
  @POST
  @Path("{projectId}/attachTemplate")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response attachTemplate(FileTemplateDTO filetemplateData) {
    
    if (filetemplateData == null || filetemplateData.getInodePath() == null
      || filetemplateData.getInodePath().equals("")) {
      throw new IllegalArgumentException("filetempleateData was not provided or its InodePath was not set");
    }
    
    String inodePath = filetemplateData.getInodePath();
    int templateid = filetemplateData.getTemplateId();
    
    Inode inode = inodeController.getInodeAtPath(inodePath);
    Template temp = templatefacade.findByTemplateId(templateid);
    temp.getInodes().add(inode);
    
    //persist the relationship
    this.templatefacade.updateTemplatesInodesMxN(temp);
    
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setSuccessMessage("The template was attached to file " + inode.getId());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  /**
   * Fetches the content of a given template
   * <p/>
   * @param templateid
   * @param sender
   * @return
   */
  @GET
  @Path("{projectId}/fetchtemplate/{templateid}/{sender}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response fetchTemplate(@PathParam("templateid") Integer templateid,
          @PathParam("sender") String sender) throws GenericException, MetadataException {

    if (templateid == null || sender == null) {
      throw new IllegalArgumentException("templateId or sender were not provided");
    }

    String json = "{\"message\": \"{\"tempid\": " + templateid + "}\"}";

    //create a request message
    TemplateMessage templateMessage = new TemplateMessage();
    templateMessage.setSender(sender);
    templateMessage.setAction("fetch_template");
    templateMessage.setMessage(json);
    ((ContentMessage) templateMessage).setTemplateid(templateid);

    Message message = this.protocol.GFR(templateMessage);

    RESTApiJsonResponse response = new RESTApiJsonResponse();
    //message contains all template content
    response.setSuccessMessage(message.getMessage());
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            response).build();
  }

  @POST
  @Path("addWithSchema")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response addMetadataWithSchema(@Context SecurityContext sc, String metaObj) throws MetadataException,
      GenericException {
    Users user = jWTHelper.getUserPrincipal(sc);
    return mutateMetadata(user, metaObj, MetadataOp.ADD);
  }

  @POST
  @Path("updateWithSchema")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response updateMetadataWithSchema(@Context SecurityContext sc, String metaObj) throws MetadataException,
      GenericException {
    Users user = jWTHelper.getUserPrincipal(sc);
    return mutateMetadata(user, metaObj, MetadataOp.UPDATE);
  }

  @POST
  @Path("removeWithSchema")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response removeMetadataWithSchema(@Context SecurityContext sc, String metaObj) throws MetadataException,
      GenericException {
    Users user = jWTHelper.getUserPrincipal(sc);
    return mutateMetadata(user, metaObj, MetadataOp.REMOVE);
  }

  private Response mutateMetadata(Users user, String metaObj, MetadataOp op)
    throws MetadataException, GenericException {
    if (op == null || user == null || metaObj == null) {
      throw new IllegalArgumentException("MetadataOp  or email or metaObj were not provided.");
    }
    // Get Inode. Get project for the inode. Check if the user has DATA OWNER 
    //role for that project (privileges to add metadata)
    InodeTableComposite itc = JsonUtil.parseSchemaHeader(metaObj);
    //variables not set in the json message
    if (itc == null) {
      throw new IllegalArgumentException("Badly formatted json message/Missing values");
    }
    Inode parent = inodeFacade.findById(itc.getInodePid());
    if (parent == null) {
      throw new GenericException(RESTCodes.GenericErrorCode.INCOMPLETE_REQUEST, Level.FINE,
        "Incorrect json message/Missing or incorrect parent inodeId");
    }
    Inode inode = inodeFacade.findByInodePK(parent, itc.getInodeName(),
            HopsUtils.calculatePartitionId(parent.getId(), itc.getInodeName(), 3));
    if (inode == null) {
      throw new GenericException(RESTCodes.GenericErrorCode.INCOMPLETE_REQUEST, Level.FINE,
        "Incorrect json message/Missing or incorrect inode name");
    }
    Inode projectInode = inodeController.getProjectRootForInode(inode);
    Project project = projectFacade.findByInodeId(projectInode.getInodePK().
            getParentId(), projectInode.getInodePK().getName());

    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Response.Status status = Response.Status.FORBIDDEN;
    String userRole = projectTeamFacade.findCurrentRole(project, user);

    if (userRole != null && !userRole.isEmpty() && userRole.
            compareToIgnoreCase(AllowedProjectRoles.DATA_OWNER) == 0) {
      List<EntityIntf> composite = new ArrayList<>();
      composite.add(itc);
      JsonObject obj = Json.createReader(new StringReader(metaObj)).
        readObject();
  
      int metaId = 0;
      switch (op) {
        case REMOVE:
          metaId = obj.getInt("metaid");
          JsonString metaObjPayload = obj.getJsonString("metadata");
          if (metaObjPayload == null) {
            throw new MetadataException(RESTCodes.MetadataErrorCode.NO_METADATA_EXISTS, Level.FINE);
          }
          this.metadataController.removeMetadata(composite, metaId,
            metaObjPayload.getString());
          break;
        case UPDATE:
          metaId = obj.getInt("metaid");
          JsonObject updatedObj = obj.getJsonObject("metadata");
          if (updatedObj == null) {
            throw new MetadataException(RESTCodes.MetadataErrorCode.NO_METADATA_EXISTS, Level.FINE);
          }
          JsonString metaObjValue = updatedObj.getJsonString("data");
          if (metaObjValue == null) {
            throw new MetadataException(RESTCodes.MetadataErrorCode.NO_METADATA_EXISTS, Level.FINE,
              "data entity in json not found");
          }
          this.metadataController.updateMetadata(composite, metaId,
            metaObjValue.getString());
          break;
        case ADD:
          List<EntityIntf> rawData = JsonUtil.parseSchemaPayload(metaObj);
          //Persist metadata
          this.metadataController.storeRawData(composite, rawData);
          break;
        default:
          throw new IllegalStateException("Invalid metadata operation: " + op);
      }
      json.setSuccessMessage("Metadata deleted");
      status = Response.Status.OK;
    } else {
      json.setErrorMsg(
              "You do not have permission to modify metadata in this project.");
    }
    return noCacheResponse.getNoCacheResponseBuilder(status).entity(json).
            build();

  }
  
  @ApiOperation( value = "Create or Update a schemaless metadata for a path.")
  @PUT
  @Path("schemaless/{xattrName}/{path: .+}")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response attachSchemalessMetadata(@Context SecurityContext sc, @Context
      UriInfo uriInfo, @PathParam("xattrName") String xattrName, @PathParam("path") String path,
      String metaObj) throws DatasetException,
      MetadataException {
    Users user = jWTHelper.getUserPrincipal(sc);
  
    if (metaObj.length() > 13500 || xattrName.length() > 255) {
      throw new MetadataException(RESTCodes.MetadataErrorCode.METADATA_MAX_SIZE_EXCEEDED, Level.FINE);
    }
    Response.Status status = Response.Status.CREATED;
    String metadata = metadataController.getSchemalessMetadata(user, path,
        xattrName);
    if(metadata != null){
      status = Response.Status.OK;
    }
    
    metadataController.addSchemaLessMetadata(user, path, xattrName,
        metaObj);
    
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    if(status == Response.Status.CREATED) {
      return Response.created(builder.build()).build();
    } else {
      return Response.ok(builder.build()).build();
    }
  }
  
  @ApiOperation( value = "Get the schemaless metadata attached to a path.")
  @GET
  @Path("schemaless/{xattrName}/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getSchemalessMetadata(@Context SecurityContext sc,
      @PathParam("xattrName") String xattrName,
      @PathParam("path") String path) throws DatasetException, MetadataException {
    Users user = jWTHelper.getUserPrincipal(sc);
    String metadata = metadataController.getSchemalessMetadata(user, path,
        xattrName);
    return Response.accepted().entity(metadata).build();
  }
  
  @ApiOperation( value = "Delete the schemaless metadata attached to a path.")
  @DELETE
  @Path("schemaless/{xattrName}/{path: .+}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response detachSchemalessMetadata(@Context SecurityContext sc,
      @PathParam("xattrName") String xattrName,
      @PathParam("path") String path) throws DatasetException, MetadataException {
    Users user = jWTHelper.getUserPrincipal(sc);
    metadataController.removeSchemaLessMetadata(user, path, xattrName);
    return Response.noContent().build();
  }
}
