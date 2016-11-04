package se.kth.hopsworks.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.EJBException;
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
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.jobs.quota.YarnPriceMultiplicator;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.ProjectTeam;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.project.services.ProjectServiceEnum;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.certificates.UserCertsFacade;
import se.kth.hopsworks.controller.DataSetDTO;
import se.kth.hopsworks.controller.DatasetController;
import se.kth.hopsworks.controller.ProjectController;
import se.kth.hopsworks.controller.ProjectDTO;
import se.kth.hopsworks.controller.QuotasDTO;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.controller.UsersController;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.dataset.DatasetFacade;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFileSystemOps;
import se.kth.hopsworks.hdfs.fileoperations.DistributedFsService;
import se.kth.hopsworks.hdfs.fileoperations.HdfsInodeAttributes;
import se.kth.hopsworks.hdfsUsers.controller.HdfsUsersController;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.util.LocalhostServices;
import se.kth.hopsworks.util.Settings;

@Path("/project")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
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
  private ProjectMembers projectMembers;
  @Inject
  private KafkaService kafka;
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
  private UserCertsFacade certificateBean;
  @EJB
  private Settings settings;
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
            = new GenericEntity<List<ProjectTeam>>(list) {
    };

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
    GenericEntity<List<Project>> projects
            = new GenericEntity<List<Project>>(list) {
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
      if (!d.getProjectId().getId().equals(proj.getId())) {
        sharedWith.add(d.getProjectId().getName());
      }
    }
    DataSetDTO dataset = new DataSetDTO(ds, proj, sharedWith);
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

        // if (s.compareToIgnoreCase(ProjectServiceEnum.BIOBANKING.toString()) == 0) {
        //   String owner = sc.getUserPrincipal().getName();
        //   try {
        //     projectController.createProjectConsentFolder(owner, project);
        //   } catch (ProjectInternalFoldersFailedException ex) {
        //     Logger.getLogger(ProjectService.class.getName()).log(Level.SEVERE,
        //             null, ex);
        //     json.setErrorMsg(s + ResponseMessages.PROJECT_FOLDER_NOT_CREATED
        //             + " 'consents' \n "
        //             + json.getErrorMsg());
        //   }
        // }
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

  @POST
  @Path("starterProject")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response starterProject(
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    ProjectDTO projectDTO = new ProjectDTO();
    JsonResponse json = new JsonResponse();
    Project project = null;
    projectDTO.setDescription("A demo project for getting started with Spark.");

    String owner = sc.getUserPrincipal().getName();
    String username = usersController.generateUsername(owner);
    projectDTO.setProjectName("demo_" + username);
    List<ProjectServiceEnum> projectServices = new ArrayList<>();
    List<ProjectTeam> projectMembers = new ArrayList<>();
    projectServices.add(ProjectServiceEnum.JOBS);
    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    try {
      dfso = dfs.getDfsOps();
      Users user = userManager.getUserByEmail(owner);
      try {
        //save the project
        project = projectController.createProject(projectDTO, owner, dfso);
        if (user != null) {
          username = hdfsUsersBean.getHdfsUserName(project, user);
          udfso = dfs.getDfsOps(username);
        }
        if (user == null | project == null) {
          logger.
                  log(Level.SEVERE, "Problem finding the user {} or project",
                          owner);
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                  ResponseMessages.PROJECT_FOLDER_NOT_CREATED);
        }
        //Severe: java.io.FileNotFoundException: /tmp/tempstores/demo_admin000__meb10000__kstore.jks (No such file or directory)
        LocalhostServices.
                createUserCertificates(settings.getIntermediateCaDir(),
                        project.getName(), user.getUsername());
        certificateBean.putUserCerts(project.getName(), user.getUsername());
      } catch (IOException ex) {
        logger.log(Level.SEVERE,
                ResponseMessages.PROJECT_FOLDER_NOT_CREATED, ex);
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.PROJECT_FOLDER_NOT_CREATED);
      } catch (IllegalArgumentException e) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.
                getLocalizedMessage());
      } catch (EJBException ex) {
        logger.log(Level.SEVERE, ResponseMessages.FOLDER_INODE_NOT_CREATED, ex);
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.FOLDER_INODE_NOT_CREATED);
      }
      if (project != null) {
        try {
          hdfsUsersBean.addProjectFolderOwner(project, dfso);
          projectController.createProjectLogResources(owner, project, dfso,
                  udfso);
          projectController.addExampleJarToExampleProject(owner, project, dfso,
                  udfso);
          //Persist README.md to hdfs for Default Datasets

          for (Settings.DefaultDataset ds : Settings.DefaultDataset.values()) {
            //Generate README.md for the Default Datasets
            datasetController.generateReadme(udfso, ds.getName(), 
                    ds.getDescription(), 
                    project.getName());
          }
          
          //TestJob dataset
          datasetController.generateReadme(udfso, "TestJob", 
                    "jar file to calculate pi", 
                    project.getName());
            
        } catch (ProjectInternalFoldersFailedException ee) {
          try {
            projectController.removeByID(project.getId(), owner, true, udfso);
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Could not create project resources");
          } catch (IOException e) {
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                    getStatusCode(), e.getMessage());
          }
        } catch (IOException ex) {
          try {
            projectController.removeByID(project.getId(), owner, true, udfso);
            throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                    "Could not add project folder owner in HDFS");
          } catch (IOException e) {
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                    getStatusCode(), e.getMessage());
          }
        }
      } else {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.PROJECT_NAME_EXIST);
      }

      projectController.addMembers(project, owner, projectMembers);
      //add the services for the project
      projectController.addServices(project, projectServices, owner);

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).
              entity(project).build();
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        udfso.close();
      }
    }
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response createProject(
          ProjectDTO projectDTO,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    JsonResponse json = new JsonResponse();
    List<String> failedMembers = null;
    Project project = null;

    String owner = sc.getUserPrincipal().getName();
    List<ProjectServiceEnum> projectServices = new ArrayList<>();

    for (String s : projectDTO.getServices()) {
      try {
        ProjectServiceEnum se = ProjectServiceEnum.valueOf(s.toUpperCase());
        se.toString();
        projectServices.add(se);
      } catch (IllegalArgumentException iex) {
        logger.log(Level.SEVERE,
                ResponseMessages.PROJECT_SERVICE_NOT_FOUND, iex);
        json.setErrorMsg(s + ResponseMessages.PROJECT_SERVICE_NOT_FOUND + "\n "
                + json.getErrorMsg());
      }
    }
    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    try {
      dfso = dfs.getDfsOps();

      try {
        //save the project
        project = projectController.createProject(projectDTO, owner, dfso);
        Users user = userManager.getUserByEmail(owner);
        if (user != null) {
          String username = hdfsUsersBean.getHdfsUserName(project, user);
          udfso = dfs.getDfsOps(username);
        }
        if (user == null | project == null) {
          logger.
                  log(Level.SEVERE, "Problem finding the user {} or project",
                          owner);
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                  ResponseMessages.PROJECT_FOLDER_NOT_CREATED);
        }
        LocalhostServices.
                createUserCertificates(settings.getIntermediateCaDir(), project.
                        getName(), user.getUsername());
        certificateBean.putUserCerts(project.getName(), user.getUsername());
      } catch (IOException ex) {
        logger.log(Level.SEVERE,
                ResponseMessages.PROJECT_FOLDER_NOT_CREATED, ex);
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.PROJECT_FOLDER_NOT_CREATED);
      } catch (IllegalArgumentException e) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), e.
                getLocalizedMessage());
      } catch (EJBException ex) {
        logger.log(Level.SEVERE, ResponseMessages.FOLDER_INODE_NOT_CREATED, ex);
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.FOLDER_INODE_NOT_CREATED);
      }
      try {
        hdfsUsersBean.addProjectFolderOwner(project, dfso);
        projectController.createProjectLogResources(owner, project, dfso,
                udfso);
        //Persist README.md to hdfs for Default Datasets
        
        for (Settings.DefaultDataset ds : Settings.DefaultDataset.values()) {
          datasetController.generateReadme(udfso, ds.getName(), 
                  ds.getDescription(), project.getName());
        }
      } catch (ProjectInternalFoldersFailedException ee) {
        try {
          projectController.removeByID(project.getId(), owner, true, udfso);
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                  "Could not create project resources");
        } catch (IOException e) {
          throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                  getStatusCode(), e.getMessage());
        }
      } catch (IOException ex) {
        try {
          projectController.removeByID(project.getId(), owner, true, udfso);
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                  "Could not add project folder owner in HDFS");
        } catch (IOException e) {
          throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                  getStatusCode(), e.getMessage());
        }
      }
      
      //add members of the project   
      // TODO - handle failure if we add members and it fails. Catch an AppException and remove the project as above.
      failedMembers = projectController.addMembers(project, owner, projectDTO.
              getProjectTeam());
      //add the services for the project
      projectController.addServices(project, projectServices, owner);

      json.setStatus("201");// Created 
      json.setSuccessMessage(ResponseMessages.PROJECT_CREATED);

      if (failedMembers != null) {
        json.setFieldErrors(failedMembers);
      }
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).
              entity(json).build();
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        udfso.close();
      }
    }
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

    String owner = sc.getUserPrincipal().getName();
    JsonResponse json = new JsonResponse();
    boolean success = true;
    DistributedFileSystemOps udfso = null;
    try {
      Users user = userManager.getUserByEmail(owner);
      Project project = projectFacade.find(id);
      if (project == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.PROJECT_NOT_FOUND);
      }
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      success = projectController.removeByID(id, owner, true, udfso);
    } catch (AccessControlException ex) {
      throw new AccessControlException(
              "Permission denied: You don't have delete permission to one or all files in this folder.");
    } catch (IOException ex) {
      logger.log(Level.SEVERE,
              ResponseMessages.PROJECT_FOLDER_NOT_REMOVED, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_FOLDER_NOT_REMOVED);
    } finally {
      udfso.close();
    }
    if (success) {
      json.setSuccessMessage(ResponseMessages.PROJECT_REMOVED);
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(
                      json).build();
    } else {
      json.setErrorMsg(ResponseMessages.PROJECT_FOLDER_NOT_REMOVED);
      return noCacheResponse.getNoCacheResponseBuilder(
              Response.Status.BAD_REQUEST).entity(
                      json).build();
    }

  }

  @POST
  @Path("{id}/remove")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeProjectNotFiles(
          @PathParam("id") Integer id,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    String owner = sc.getUserPrincipal().getName();
    JsonResponse json = new JsonResponse();
    boolean success = true;
    DistributedFileSystemOps udfso = null;
    try {
      Users user = userManager.getUserByEmail(owner);
      Project project = projectFacade.find(id);
      if (project == null) {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                ResponseMessages.PROJECT_NOT_FOUND);
      }
      String username = hdfsUsersBean.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      success = projectController.removeByID(id, owner, false, udfso);
    } catch (IOException ex) {
      logger.log(Level.SEVERE,
              ResponseMessages.PROJECT_FOLDER_NOT_REMOVED, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_FOLDER_NOT_REMOVED);
    } finally {
      udfso.close();
    }
    json.setStatus("OK");
    if (success) {
      json.setSuccessMessage(ResponseMessages.PROJECT_REMOVED_NOT_FOLDER);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @Path("{id}/projectMembers")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public ProjectMembers projectMembers(
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

    YarnPriceMultiplicator multiplicator = projectController.getYarnMultiplicator();

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
            = new GenericEntity<List<DataSetDTO>>(publicDatasets) {
    };

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
    newDS.setEditable(false);
    datasetFacade.persistDataset(newDS);
    Users user = userManager.getUserByEmail(sc.getUserPrincipal().getName());

    activityFacade.persistActivity(ActivityFacade.SHARED_DATA + newDS.toString()
            + " with project " + destProj.getName(), destProj, user);

    hdfsUsersBean.shareDataset(destProj, ds);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
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
}
