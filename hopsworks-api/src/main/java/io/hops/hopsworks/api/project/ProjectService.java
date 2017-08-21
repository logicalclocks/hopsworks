package io.hops.hopsworks.api.project;

import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jobs.BiobankingService;
import io.hops.hopsworks.api.jobs.JobService;
import io.hops.hopsworks.api.jobs.KafkaService;
import io.hops.hopsworks.api.jupyter.JupyterService;
import io.hops.hopsworks.api.pythonDeps.PythonDepsService;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.api.util.LocalFsService;
import io.hops.hopsworks.api.workflow.WorkflowService;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.DataSetDTO;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.HdfsInodeAttributes;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnPriceMultiplicator;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.MoreInfoDTO;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import io.hops.hopsworks.common.project.QuotasDTO;
import io.hops.hopsworks.common.project.TourProjectType;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
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
import javax.ws.rs.core.SecurityContext;
import org.apache.hadoop.security.AccessControlException;

@Path("/project")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Project Service", description = "Project Service")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectService {

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private NoCacheResponse noCacheResponse;
  @Inject
  private ProjectMembersService projectMembers;
  @Inject
  private KafkaService kafka;
  @Inject
  private JupyterService jupyter;
  @Inject
  private DataSetService dataSet;
  @Inject
  private LocalFsService localFs;
  @Inject
  private JobService jobs;
  @Inject
  private BiobankingService biobanking;
  @Inject
  private WorkflowService workflowService;
  @Inject
  private PythonDepsService pysparkService;
  @Inject
  private CertService certs;

  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private InodeFacade inodes;
  @EJB
  private HdfsUsersController hdfsUsersBean;

  @EJB
  private ActivityFacade activityController;
  @EJB
  private UsersController usersController;
  @EJB
  private UserManager userManager;
  @EJB
  private DistributedFsService dfs;

  private final static Logger logger = Logger.getLogger(ProjectService.class.
      getName());

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response findAllByUser(@Context SecurityContext sc,
      @Context HttpServletRequest req) {

    // Get the user according to current session and then get all its projects
    String email = sc.getUserPrincipal().getName();
    List<ProjectTeam> list = projectController.findProjectByUser(email);
    GenericEntity<List<ProjectTeam>> projects
        = new GenericEntity<List<ProjectTeam>>(list) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        projects).build();
  }

  @GET
  @Path("/getAll")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getAllProjects(@Context SecurityContext sc,
      @Context HttpServletRequest req) {

    List<Project> list = projectFacade.findAll();
    GenericEntity<List<Project>> projects = new GenericEntity<List<Project>>(
        list) {
    };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        projects).build();
  }

  @GET
  @Path("/getProjectInfo/{projectName}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getProjectByName(@PathParam("projectName") String projectName,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {

    ProjectDTO proj = projectController.getProjectByName(projectName);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        proj).build();
  }

  @GET
  @Path("/getMoreInfo/{type}/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMoreInfo(@PathParam("type") String type,
      @PathParam("id") Integer id) throws AppException {
    MoreInfoDTO info = null;
    if (id != null) {
      switch (type){
        case "proj":
          Project proj = projectFacade.find(id);
          info = new MoreInfoDTO(proj);
          break;
        case "ds":
          info = datasetInfo(id);
          break;
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        info).build();
  }

  @GET
  @Path("{id}/getMoreInfo/{type}/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getMoreInfo(@PathParam("id") Integer projectId, @PathParam("type") String type,
      @PathParam("inodeId") Integer id) throws AppException {
    MoreInfoDTO info = null;
    if (id != null) {
      switch (type){
        case "proj":
          Project proj = projectFacade.find(id);
          info = new MoreInfoDTO(proj);
          break;
        case "ds":
        case "inode":
          info = inodeInfo(id, projectId);
          break;
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        info).build();
  }
  
  @GET
  @Path("/readme/{path: .+}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getReadme(@PathParam("path") String path) throws AppException {
    if (path == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "No path given.");
    }
    String[] parts = path.split(File.separator);
    if (parts.length < 5) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Should specify full path.");
    }
    Project proj = projectFacade.findByName(parts[2]);
    Dataset ds = datasetFacade.findByNameAndProjectId(proj, parts[3]);
    if (ds != null && !ds.isSearchable()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Readme not accessable.");
    }
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    FilePreviewDTO filePreviewDTO;
    try {
      filePreviewDTO = datasetController.getReadme(path, dfso);
    } catch (IOException ex) {
      filePreviewDTO = new FilePreviewDTO();
      filePreviewDTO.setContent("No README file found for this dataset.");
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(
              filePreviewDTO).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        filePreviewDTO).build();
  }

  private MoreInfoDTO datasetInfo(Integer inodeId) {
    Inode inode = inodes.findById(inodeId);
    if (inode == null) {
      return null;
    }
    List<Dataset> ds = datasetFacade.findByInode(inode);
    if(ds !=null && !ds.isEmpty() && !ds.get(0).isSearchable()){
      return null;
    }
    MoreInfoDTO info = new MoreInfoDTO(inode);
    Users user = userManager.getUserByUsername(info.getUser());
    info.setUser(user.getFname() + " " + user.getLname());
    info.setSize(inodes.getSize(inode));
    info.setPath(inodes.getPath(inode));
    return info;
  }
  
  private MoreInfoDTO inodeInfo(Integer inodeId, Integer projectId) {
    Inode inode = inodes.findById(inodeId);
    if (inode == null) {
      return null;
    }
    String group = inode.getHdfsGroup().getName();
    Project project = projectFacade.find(projectId);
    if(project!=null && !project.getName().equals(hdfsUsersBean.getProjectName(group))){
      return null;
    }
    MoreInfoDTO info = new MoreInfoDTO(inode);
    Users user = userManager.getUserByUsername(info.getUser());
    info.setUser(user.getFname() + " " + user.getLname());
    info.setSize(inodes.getSize(inode));
    info.setPath(inodes.getPath(inode));
    return info;
  }
  
  @GET
  @Path("getDatasetInfo/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getDatasetInfo(
      @PathParam("inodeId") Integer inodeId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    Inode inode = inodes.findById(inodeId);
    if (inode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_NOT_FOUND);
    }

    Inode parent = inodes.findParent(inode);
    Project proj = projectFacade.findByName(parent.getInodePK().getName());
    Dataset ds = datasetFacade.findByProjectAndInode(proj, inode);

    if (ds == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_NOT_FOUND);
    }

    List<Dataset> projectsContainingInode = datasetFacade.findByInode(inode);
    List<String> sharedWith = new ArrayList<>();
    for (Dataset d : projectsContainingInode) {
      if (!d.getProject().getId().equals(proj.getId())) {
        sharedWith.add(d.getProject().getName());
      }
    }
    DataSetDTO dataset = new DataSetDTO(ds, proj, sharedWith);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        dataset).build();
  }

  @GET
  @Path("{id}/getInodeInfo/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getDatasetInfo(
      @PathParam("id") Integer projectId,
      @PathParam("inodeId") Integer inodeId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    Inode inode = inodes.findById(inodeId);
    if (inode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_NOT_FOUND);
    }
    String group = inode.getHdfsGroup().getName();
    Project project = projectFacade.find(projectId);
    if(project!=null && !project.getName().equals(hdfsUsersBean.getProjectName(group))){
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_NOT_PUBLIC);
    }

    DataSetDTO dataset = new DataSetDTO(inode.getInodePK().getName(), inodeId, project);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        dataset).build();
  }
  
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findByProjectID(
      @PathParam("id") Integer id,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {

    // Get a specific project based on the id, Annotated so that 
    // only the user with the allowed role is able to see it 
    ProjectDTO proj = projectController.getProjectByID(id);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        proj).build();
  }

  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response updateProject(
      ProjectDTO projectDTO,
      @PathParam("id") Integer id,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    boolean updated = false;

    Project project = projectController.findProjectById(id);
    String userEmail = sc.getUserPrincipal().getName();

    // Update the description if it have been chenged
    if (project.getDescription() == null || !project.getDescription().equals(
        projectDTO.getDescription())) {
      projectController.updateProject(project, projectDTO, userEmail);

      json.setSuccessMessage(ResponseMessages.PROJECT_DESCRIPTION_CHANGED);
      updated = true;
    }

    // Update the retention period if it have been chenged
    if (project.getRetentionPeriod() == null || !project.getRetentionPeriod().
        equals(
            projectDTO.getRetentionPeriod())) {
      projectController.updateProject(project, projectDTO,
          userEmail);
      activityController.persistActivity("Changed   retention period to "
          + projectDTO.getRetentionPeriod(), project, userEmail);
      json.setSuccessMessage(ResponseMessages.PROJECT_RETENTON_CHANGED);
      updated = true;
    }

    // Add all the new services
    List<ProjectServiceEnum> projectServices = new ArrayList<>();
    for (String s : projectDTO.getServices()) {
      try {
        ProjectServiceEnum se = ProjectServiceEnum.valueOf(s.toUpperCase());
        se.toString();
        if (se.equals(ProjectServiceEnum.ZEPPELIN)) {
          Users user = userManager.getUserByEmail(userEmail);
          DistributedFileSystemOps udfso = null;
          DistributedFileSystemOps dfso = null;
          Settings.DefaultDataset ds = Settings.DefaultDataset.ZEPPELIN;
          try {
            String username = hdfsUsersBean.getHdfsUserName(project, user);
            udfso = dfs.getDfsOps(username);
            dfso = dfs.getDfsOps();
            datasetController.createDataset(user, project, ds.getName(), ds.
                    getDescription(), -1, false, true, dfso, dfso);
            datasetController.generateReadme(udfso, ds.getName(),
                    ds.getDescription(), project.getName());
          } catch (IOException | AppException ex) {
            logger.log(Level.SEVERE, "Could not create zeppelin notebook dir.",
                    ex);
            json.setErrorMsg(json.getErrorMsg() + "\n " 
                    + "Failed to create zeppelin notebook dir. "
                    + "Zeppelin will not work properly. "
                    + "Try recreating "+ ds.getName() +" dir manualy.");
          } finally {
            if (udfso != null) {
              udfso.close();
            }
            if (dfso != null) {
              dfso.close();
            }
          }
        }
        projectServices.add(se);
      } catch (IllegalArgumentException iex) {
        logger.log(Level.SEVERE,
                ResponseMessages.PROJECT_SERVICE_NOT_FOUND);
        json.setErrorMsg(s + ResponseMessages.PROJECT_SERVICE_NOT_FOUND + "\n "
                + json.getErrorMsg());
      }
    }

    if (!projectServices.isEmpty()) {
      boolean added = projectController.addServices(project, projectServices,
          userEmail);
      if (added) {
        json.setSuccessMessage(ResponseMessages.PROJECT_SERVICE_ADDED);
        updated = true;
      }
    }
    if (!updated) {
      json.setSuccessMessage("Nothing to update.");
    }

    return noCacheResponse.getNoCacheResponseBuilder(
        Response.Status.CREATED).entity(json).build();
  }

  
  
  
  private void populateActiveServices(List<String> projectServices,
      TourProjectType tourType) {
    for (ProjectServiceEnum service : tourType.getActiveServices()) {
      projectServices.add(service.name());
    }
  }
  
  @POST
  @Path("starterProject/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response starterProject(
      @PathParam("type") String type,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    ProjectDTO projectDTO = new ProjectDTO();
    Project project = null;
    projectDTO.setDescription("A demo project for getting started with " + type);

    String owner = sc.getUserPrincipal().getName();
    String username = usersController.generateUsername(owner);
    List<String> projectServices = new ArrayList<>();
    Users user = userManager.getUserByEmail(owner);
    if (user == null) {
      logger.log(Level.SEVERE, "Problem finding the user {} ", owner);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_FOLDER_NOT_CREATED);
    }
    //save the project
    List<String> failedMembers = new ArrayList<>();

    TourProjectType demoType = null;
    String readMeMessage = null;
    if (TourProjectType.SPARK.getTourName().equalsIgnoreCase(type)) {
      // It's a Spark guide
      demoType = TourProjectType.SPARK;
      projectDTO.setProjectName("demo_" + TourProjectType.SPARK.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.SPARK);
      readMeMessage = "jar file to demonstrate the creation of a spark batch job";
    } else if (TourProjectType.KAFKA.getTourName().equalsIgnoreCase(type)) {
      // It's a Kafka guide
      demoType = TourProjectType.KAFKA;
      projectDTO.setProjectName("demo_" + TourProjectType.KAFKA.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.KAFKA);
      readMeMessage = "jar file to demonstrate Kafka streaming";
    } else if (TourProjectType.TENSORFLOW.getTourName().equalsIgnoreCase(type)) {
      // It's a TensorFlow guide
      demoType = TourProjectType.TENSORFLOW;
      projectDTO.setProjectName("demo_" + TourProjectType.TENSORFLOW.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.TENSORFLOW);
      readMeMessage = "Mnist data and python files to demonstrate the creation of a TensorFlow job";
    } else {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.STARTER_PROJECT_BAD_REQUEST);
    }
    projectDTO.setServices(projectServices);

    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    try {
      project = projectController.createProject(projectDTO, user, failedMembers, req.getSession().getId());
      dfso = dfs.getDfsOps();
      username = hdfsUsersBean.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      projectController.addTourFilesToProject(owner, project, dfso, dfso, demoType);
      //TestJob dataset
      datasetController.generateReadme(udfso, "TestJob", readMeMessage, project.getName());
    } catch (Exception ex) {
      projectController.cleanup(project, req.getSession().getId());
      throw ex;
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        udfso.close();
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).
        entity(project).build();
  }
  

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response createProject(
          ProjectDTO projectDTO,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    //check the user
    String owner = sc.getUserPrincipal().getName();
    Users user = userManager.getUserByEmail(owner);
    if (user == null) {
      logger.log(Level.SEVERE, "Problem finding the user {} ", owner);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_FOLDER_NOT_CREATED);
    }
    
    List<String> failedMembers = null;
    projectController.createProject(projectDTO, user, failedMembers, req.getSession().getId());

    JsonResponse json = new JsonResponse();
    json.setStatus("201");// Created 
    json.setSuccessMessage(ResponseMessages.PROJECT_CREATED);

    if (failedMembers != null && !failedMembers.isEmpty()) {
      json.setFieldErrors(failedMembers);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).
            entity(json).build();
  }
  
  @POST
  @Path("{id}/delete")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeProjectAndFiles(
          @PathParam("id") Integer id,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException,
          AccessControlException {

    String userMail = sc.getUserPrincipal().getName();
    JsonResponse json = new JsonResponse();

    try {
//      String jsessionId = "";
//      if (req.getCookies() != null && req.getCookies().length > 0) {
//        for (Cookie cookie : req.getCookies()) {
//          if (cookie.getName().equalsIgnoreCase("JSESSIONIDSSO")) {
//            jsessionId = cookie.getValue();
//          }
//        }
//      } 
      projectController.removeProject(userMail, id, req.getSession().getId());
    } catch (AppException ex) {
      json.setErrorMsg(ex.getMessage());
      return noCacheResponse.getNoCacheResponseBuilder(
          Response.Status.BAD_REQUEST).entity(
              json).build();
    }

    json.setSuccessMessage(ResponseMessages.PROJECT_REMOVED);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
            entity(json).build();

  }


  @Path("{id}/projectMembers")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public ProjectMembersService projectMembers(
      @PathParam("id") Integer id) throws AppException {
    this.projectMembers.setProjectId(id);

    return this.projectMembers;
  }

  @Path("{id}/dataset")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public DataSetService datasets(
      @PathParam("id") Integer id) throws AppException {
    Project project = projectController.findProjectById(id);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_NOT_FOUND);
    }
    this.dataSet.setProjectId(id);

    return this.dataSet;
  }

  @Path("{id}/localfs")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public LocalFsService localFs(
      @PathParam("id") Integer id) throws AppException {
    this.localFs.setProjectId(id);

    return this.localFs;
  }

  @Path("{projectId}/jobs")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public JobService jobs(@PathParam("projectId") Integer projectId) throws
      AppException {
    Project project = projectController.findProjectById(projectId);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_NOT_FOUND);
    }
    return this.jobs.setProject(project);
  }

  @Path("{projectId}/biobanking")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public BiobankingService biobanking(@PathParam("projectId") Integer projectId)
      throws
      AppException {
    Project project = projectController.findProjectById(projectId);
    return this.biobanking.setProject(project);
  }
  
  @Path("{projectId}/certs")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public CertService certs(@PathParam("projectId") Integer projectId) throws
      AppException {
    Project project = projectController.findProjectById(projectId);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_NOT_FOUND);
    }
    return this.certs.setProject(project);
  }

  @GET
  @Path("{id}/quotas")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response quotasByProjectID(
      @PathParam("id") Integer id,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {

    ProjectDTO proj = projectController.getProjectByID(id);
    String yarnQuota = projectController.getYarnQuota(proj.getProjectName());
    HdfsInodeAttributes inodeAttrs = projectController.getHdfsQuotas(proj.
        getInodeid());

    Long hdfsQuota = inodeAttrs.getDsquota().longValue();
    Long hdfsUsage = inodeAttrs.getDiskspace().longValue();
    Long hdfsNsQuota = inodeAttrs.getNsquota().longValue();
    Long hdfsNsCount = inodeAttrs.getNscount().longValue();
    QuotasDTO quotas = new QuotasDTO(yarnQuota, hdfsQuota, hdfsUsage,
        hdfsNsQuota, hdfsNsCount);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        quotas).build();
  }

  @GET
  @Path("{id}/multiplicator")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response getCurrentMultiplicator(
      @PathParam("id") Integer id,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {

    YarnPriceMultiplicator multiplicator = projectController.
        getYarnMultiplicator();

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        multiplicator).build();
  }

  @GET
  @Path("getPublicDatasets")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response getPublicDatasets(
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {

    List<DataSetDTO> publicDatasets = datasetFacade.findPublicDatasets();
    GenericEntity<List<DataSetDTO>> datasets
        = new GenericEntity<List<DataSetDTO>>(publicDatasets) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        datasets).build();
  }

  @GET
  @Path("{id}/importPublicDataset/{projectName}/{inodeId}")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response quotasByProjectID(
      @PathParam("id") Integer id,
      @PathParam("projectName") String projectName,
      @PathParam("inodeId") Integer dsId,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {

    Project destProj = projectController.findProjectById(id);
    Project dsProject = projectFacade.findByName(projectName);

    if (dsProject == null || destProj == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Could not find the project or dataset.");
    }
    Inode inode = inodes.findById(dsId);
    if (inode == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Could not find the dataset.");
    }
    Dataset ds = datasetFacade.findByProjectAndInode(dsProject, inode);
    if (ds == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Could not find the dataset.");
    }

    if (ds.isPublicDs() == false) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DATASET_NOT_PUBLIC);
    }

    Dataset newDS = new Dataset(inode, destProj);
    newDS.setShared(true);

    if (ds.getDescription() != null) {
      newDS.setDescription(ds.getDescription());
    }
    if (ds.isPublicDs()) {
      newDS.setPublicDs(true);
    }
    newDS.setEditable(false);
    datasetFacade.persistDataset(newDS);
    Users user = userManager.getUserByEmail(sc.getUserPrincipal().getName());

    activityFacade.persistActivity(ActivityFacade.SHARED_DATA + newDS.toString()
        + " with project " + destProj.getName(), destProj, user);

    hdfsUsersBean.shareDataset(destProj, ds);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Path("{id}/logs/enable")
  @Produces(MediaType.TEXT_PLAIN)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public Response enableLogs(@PathParam("id") Integer id) throws AppException {
    Project project = projectController.findProjectById(id);
    if (!project.getLogs()) {
      projectFacade.enableLogs(project);
      try {
        projectController.addElasticsearch(project.getName());
      } catch (IOException ex) {
        logger.log(Level.SEVERE, ex.getMessage());
        return noCacheResponse.getNoCacheResponseBuilder(
            Response.Status.SERVICE_UNAVAILABLE).build();
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity("").build();
  }

  @Path("{id}/kafka")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public KafkaService kafka(
      @PathParam("id") Integer id) throws AppException {
    Project project = projectController.findProjectById(id);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_NOT_FOUND);
    }
    this.kafka.setProjectId(id);

    return this.kafka;
  }
  
  @Path("{id}/jupyter")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public JupyterService jupyter(
      @PathParam("id") Integer id) throws AppException {
    Project project = projectController.findProjectById(id);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_NOT_FOUND);
    }
    this.jupyter.setProjectId(id);

    return this.jupyter;
  }

  @Path("{id}/workflows")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public WorkflowService workflows(@PathParam("id") Integer id) throws
      AppException {
    Project project = projectController.findProjectById(id);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_NOT_FOUND);
    }
    this.workflowService.setProject(project);
    return workflowService;
  }

  @Path("{id}/pythonDeps")
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER, AllowedRoles.DATA_SCIENTIST})
  public PythonDepsService pysparkDeps(@PathParam("id") Integer id) throws
      AppException {
    Project project = projectController.findProjectById(id);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_NOT_FOUND);
    }
    this.pysparkService.setProject(project);
    return pysparkService;
  }

}
