package se.kth.hopsworks.rest;

import io.hops.bbc.CharonDTO;
import io.hops.bbc.charon.CharonController;
import io.hops.bbc.charon.CharonRegisteredSiteDTOs;
import io.hops.bbc.charon.CharonSharedSiteDTO;
import io.hops.hdfs.HdfsLeDescriptors;
import io.hops.hdfs.HdfsLeDescriptorsFacade;
import java.util.ArrayList;

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
import javax.ws.rs.Produces;
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
import io.hops.bbc.charon.CharonRegisteredSiteDTO;
import io.hops.bbc.charon.CharonRegisteredSites;
import io.hops.bbc.charon.CharonRepoShared;
import io.hops.bbc.charon.CharonSharedSiteDTOs;
import javax.ws.rs.DELETE;
import javax.ws.rs.PathParam;
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
  @EJB
  private CharonController charonController;

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
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getMySiteId(
		  @Context SecurityContext sc,
		  @Context HttpServletRequest req) throws Exception {

	String siteID = CharonOperations.getMySiteId();

	return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
			siteID).build();
  }

 

  @GET
  @Path("/listSiteIds")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRegisteredSiteIds(
		  @Context SecurityContext sc,
		  @Context HttpServletRequest req) throws Exception {

	List<CharonRegisteredSiteDTO> sites = new ArrayList<>();
	for (CharonRegisteredSites s : charonController.getCharonRegisteredSites(project.getId())) {
	  sites.add(new CharonRegisteredSiteDTO(s));
	}
	CharonRegisteredSiteDTOs sitesDTO = new CharonRegisteredSiteDTOs(sites);
	return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
			sitesDTO).build();
  }

  @GET
  @Path("/listSharedRepos")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSharedSiteIds(
		  @Context SecurityContext sc,
		  @Context HttpServletRequest req) throws Exception {

	List<CharonSharedSiteDTO> sites = new ArrayList<>();
	for (CharonRepoShared s : charonController.getCharonSharedSites(project.getId())) {
	  sites.add(new CharonSharedSiteDTO(s));
	}
	CharonSharedSiteDTOs sitesDTO = new CharonSharedSiteDTOs(sites);

	return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
			sitesDTO).build();
  }

  @POST
  @Path("/addSiteId")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response addSiteId(@Context SecurityContext sc,
		  @Context HttpServletRequest req, CharonRegisteredSiteDTO site)
		  throws Exception {
	JsonResponse json = new JsonResponse();

//	String siteID = charon.getString();
	StringBuilder sb = new StringBuilder();
	sb.append("id=");
	sb.append(site.getSiteId()).append("\n");
	sb.append("name=");
	sb.append(site.getName()).append("\n");
	sb.append("addr=");
	sb.append(site.getAddr()).append("\n");
	sb.append("email=");
	sb.append(site.getEmail()).append("\n");

	logger.log(Level.INFO, "Site id: \n{0}", sb.toString());
	
	CharonOperations.addSiteId(sb.toString());
	charonController.registerSite(project.getId(), site.getSiteId(), site.getEmail(),
			site.getName(), site.getAddr());

	json.setSuccessMessage("Site added successfully.");
	Response.ResponseBuilder response = Response.ok();
	return response.entity(json).build();
  }

  @DELETE
  @Path("removeSiteId/{siteId}")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeSiteId(
		  @PathParam("siteId") Integer siteId,
		  @Context SecurityContext sc,
		  @Context HttpServletRequest req)
		  throws Exception {
	JsonResponse json = new JsonResponse();

//	CharonOperations.removeSiteId(siteId);
	charonController.removeSite(project.getId(), siteId);

	json.setSuccessMessage("Site removed successfully.");
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

	String path = project.getName() + File.separator + charon.getCharonPath();
	logger.info("Mkdir: " + path);

	CharonOperations.mkdir(path, null);

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

//	if (!charon.getString().contains("/" + project.getName())) {
	  path = project.getName() + File.separator + charon.getCharonPath();
//	} else {
//	  path = charon.getString();
//	}
	String permissions = charon.getPermissions();
//	int granteeId = Integer.parseInt(charon.getGranteeId());

	String token = CharonOperations.share(permissions, path, charon.getGranteeId());
	charonController.shareWithSite(project.getId(), charon.getGranteeId(), path, permissions, token);
	
	json.setSuccessMessage("Repository shared successfully.");
	Response.ResponseBuilder response = Response.ok();
	return response.entity(json).build();
  }

  
  @POST
  @Path("removeShare")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeShare(
		  @Context SecurityContext sc,
		  @Context HttpServletRequest req,
		  CharonSharedSiteDTO charon)
		  throws Exception {
	JsonResponse json = new JsonResponse();

//	CharonOperations.removeSiteId(siteId);
	charonController.removeShare(project.getId(), charon.getGranteeId(), charon.getPath());

	json.setSuccessMessage("Site removed successfully.");
	Response.ResponseBuilder response = Response.ok();
	return response.entity(json).build();
  }  
  
  @GET
  @Path("importRepo/{token}")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response importRepo(
		  @PathParam("token") String token,
		  @Context SecurityContext sc,
		  @Context HttpServletRequest req)
		  throws Exception {
	JsonResponse json = new JsonResponse();

	CharonOperations.addSharedRepository(token);
	
	json.setSuccessMessage("Site removed successfully.");
	Response.ResponseBuilder response = Response.ok();
	return response.entity(json).build();
  }  
  
 @POST
  @Path("/createSharedRepository")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response createSharedRepository(@Context SecurityContext sc,
		  @Context HttpServletRequest req, CharonSharedSiteDTO charon)
		  throws Exception {
	JsonResponse json = new JsonResponse();
	
	String fullPath = project.getName() + File.separator + charon.getPath();
	
	logger.log(Level.INFO, "Create new repo: {0}", fullPath);
	
	String token = "secret";
//	CharonOperations.createSharedRepository(
//			charon.getGranteeId(), fullPath,
//			charon.getPermissions());
//	
	
	charonController.shareWithSite(project.getId(), charon.getGranteeId(), fullPath,
			charon.getPermissions(), token);

	json.setSuccessMessage("Site added successfully.");
	Response.ResponseBuilder response = Response.ok();
	return response.entity(json).build();
  }  
}
