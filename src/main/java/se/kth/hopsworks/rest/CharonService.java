package se.kth.hopsworks.rest;

import io.hops.bbc.CharonDTO;
import io.hops.hdfs.HdfsLeDescriptors;
import io.hops.hdfs.HdfsLeDescriptorsFacade;

import java.io.File;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.fb.InodeFacade;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.util.CharonOperations;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CharonService {

  private static final Logger logger = Logger.getLogger(CharonService.class.
      getName());

//  private final String CHARON_PATH = "/srv/Charon";
//  private final String charonMountPointPath = "/srv/charon_fs";
//  private final String addNewGranteePath = CHARON_PATH + File.separator + "NewSiteIds";
//  private final String addNewSNSPath = CHARON_PATH + File.separator + "NewSNSs";
//  private final String addedGrantees = CHARON_PATH + File.separator + "config/addedGrantees";

  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private FileOperations fops;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private FileOperations fileOps;
  @EJB
  private HdfsLeDescriptorsFacade hdfsLeDescriptorsFacade;

  private Project project;

  CharonService setProject(Project project) {
    this.project = project;
    return this;
  }
  
  private String addNameNodeEndpoint(String str) {
    HdfsLeDescriptors hdfsLeDescriptors = hdfsLeDescriptorsFacade.findEndpoint();
    String ipPortEndpointNN = hdfsLeDescriptors.getHostname();
    return str.replaceFirst("hdfs://", "hdfs://" + ipPortEndpointNN + "/");
  }

  @POST
  @Path("/fromHDFS")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response copyFromHDFS(@Context SecurityContext sc,
      @Context HttpServletRequest req, CharonDTO charon)
      throws AppException {
    JsonResponse json = new JsonResponse();

    String src = charon.getHdfsPath();
    String dest = charon.getCharonPath();
  
    if (src == null || dest == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Some of the paths 'from' and 'to' are set to null!");
    }
    src = addNameNodeEndpoint(src);

    try {
      fileOps.copyToLocal(src, dest);
    } catch (IOException ex) {
      Logger.getLogger(CharonService.class.getName()).log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),
          "Could not copy file from HDFS to Charon.");
    }

    json.setSuccessMessage("File copied successfully from HDFS to Charon .");
    Response.ResponseBuilder response = Response.ok();
    return response.entity(json).build();
  }

  @POST
  @Path("/toHDFS")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response copyToHDFS(@Context SecurityContext sc,
      @Context HttpServletRequest req, CharonDTO charon)
      throws AppException {
    JsonResponse json = new JsonResponse();

    String src = charon.getCharonPath();
    String dest = charon.getHdfsPath();

    if (src == null || dest == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Some of the paths 'from' and 'to' are set to null!");
    }
    dest = addNameNodeEndpoint(dest);
    try {
      fileOps.copyToHDFSFromLocal(false, src, dest);
    } catch (IOException ex) {
      Logger.getLogger(CharonService.class.getName()).log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(),
          "Could not copy file from Charon to HDFS.");
    }

    json.setSuccessMessage("File copied successfully from Charon to HDFS.");
    Response.ResponseBuilder response = Response.ok();
    return response.entity(json).build();
  }

  @GET
  @Path("/mySiteID")
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getMySiteId(
      @Context
      SecurityContext sc,
      @Context
      HttpServletRequest req) throws Exception {

    String siteID = CharonOperations.getMySiteId();

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        siteID).build();
  }

  @POST
  @Path("/addSiteID")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response addSiteId(@Context SecurityContext sc,
      @Context HttpServletRequest req, CharonDTO charon)
      throws Exception {
    JsonResponse json = new JsonResponse();

    String siteID = charon.getString();

    CharonOperations.addSiteId(siteID);

    json.setSuccessMessage("Site added successfully.");
    Response.ResponseBuilder response = Response.ok();
    return response.entity(json).build();
  }

  @POST
  @Path("/mkdir")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response mkdir(@Context SecurityContext sc,
      @Context HttpServletRequest req, CharonDTO charon)
      throws Exception {
    JsonResponse json = new JsonResponse();

    String path = project.getName() + File.separator + charon.getString();

    CharonOperations.mkdir(path, null);

    json.setSuccessMessage("Repository created successfully.");
    Response.ResponseBuilder response = Response.ok();
    return response.entity(json).build();
  }

  @POST
  @Path("/createSharedRepository")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response createSharedRepository(@Context SecurityContext sc,
      @Context HttpServletRequest req, CharonDTO charon)
      throws Exception {
    JsonResponse json = new JsonResponse();
    String path;

    if (!charon.getString().contains("/"+project.getName())) {
       path = project.getName() + File.separator + charon.getString();
    } else {
       path = charon.getString();
    }
    String permissions = charon.getPermissions();
    int granteeId = Integer.parseInt(charon.getGranteeId());

    CharonOperations.createSharedRepository(granteeId,path,permissions);

    json.setSuccessMessage("Repository created successfully.");
    Response.ResponseBuilder response = Response.ok();
    return response.entity(json).build();
  }

  @POST
  @Path("/share")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response share(@Context SecurityContext sc,
      @Context HttpServletRequest req, CharonDTO charon)
      throws Exception {
    JsonResponse json = new JsonResponse();
    String path;

    if (!charon.getString().contains("/"+project.getName())) {
      path = project.getName() + File.separator + charon.getString();
    } else {
      path = charon.getString();
    }
    String permissions = charon.getPermissions();
    int granteeId = Integer.parseInt(charon.getGranteeId());

    CharonOperations.share(permissions,path,granteeId);

    json.setSuccessMessage("Repository shared successfully.");
    Response.ResponseBuilder response = Response.ok();
    return response.entity(json).build();
  }

}
