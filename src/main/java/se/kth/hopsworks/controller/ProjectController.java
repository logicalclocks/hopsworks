package se.kth.hopsworks.controller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.*;
import javax.ws.rs.core.Response;

import se.kth.bbc.activity.Activity;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectFacade;
import se.kth.bbc.project.ProjectRoleTypes;
import se.kth.bbc.project.ProjectTeam;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.bbc.project.ProjectTeamPK;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.project.fb.InodeView;
import se.kth.bbc.project.services.ProjectServiceEnum;
import se.kth.bbc.project.services.ProjectServiceFacade;
import se.kth.bbc.project.services.ProjectServices;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.User;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.user.model.SshKeys;
import se.kth.hopsworks.users.SshkeysFacade;
import se.kth.hopsworks.util.LocalhostServices;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectController {

  private final static Logger logger = Logger.getLogger(ProjectController.class.
          getName());
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private UserManager userBean;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private FileOperations fileOps;
  @EJB
  private ProjectServiceFacade projectServicesFacade;
  @EJB
  private InodeFacade inodes;
  @EJB
  private DatasetController datasetController;
  @EJB
  private SshkeysFacade sshKeysBean;

  /**
   * Creates a new project(project), the related DIR, the different services
   * in the project, and the master of the project.
   *
   * This needs to be an atomic operation (all or nothing) REQUIRES_NEW will
   * make sure a new transaction is created even if this method is called from
   * within a transaction.
   * <p>
   * @param newProject
   * @param email
   * @return
   * @throws IllegalArgumentException if the project name already exists.
   * @throws IOException if the DIR associated with the project could not be
   * created. For whatever reason.
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Project createProject(ProjectDTO newProject, String email) throws
          IOException {
    User user = userBean.getUserByEmail(email);
    //if there is no project by the same name for this user and project name is valid
    if (FolderNameValidator.isValidName(newProject.getProjectName())
            && !projectFacade.
            projectExists(newProject.getProjectName())) {
      //Create a new project object
      Date now = new Date();
      Project project = new Project(newProject.getProjectName(), user, now);
      project.setDescription(newProject.getDescription());

      /*
       * first create the folder structure in hdfs. If it is successful move on
       * to create the project in hopsworks
       */
      if (mkProjectDIR(project.getName())) {
        
        Inode projectInode = this.inodes.getProjectRoot(project.getName());
        project.setInode(projectInode);
        
        //Persist project object
        projectFacade.persistProject(project);
        projectFacade.flushEm();//flushing it to get project id
        //Add the activity information
        logActivity(ActivityFacade.NEW_PROJECT,
                ActivityFacade.FLAG_PROJECT, user, project);
        //update role information in project
        addProjectOwner(project.getId(), user.getEmail());
        logger.log(Level.FINE, "{0} - project created successfully.", project.
                getName());

        //Create default DataSets
        for (Constants.DefaultDataset ds : Constants.DefaultDataset.values()) {
          datasetController.createDataset(user, project, ds.getName(), ds.
                  getDescription(), -1, false);
        }
        return project;
      }
    } else {
      logger.log(Level.SEVERE, "Project with name {0} already exists!",
              newProject.getProjectName());
      throw new IllegalArgumentException(ResponseMessages.PROJECT_NAME_EXIST);
    }
    return null;
  }

  /**
   * Returns a Project
   * <p/>
   *
   * @param id the identifier for a Project
   * @return Project
   * @throws se.kth.hopsworks.rest.AppException if the project could not be
   * found.
   */
  public Project findProjectById(Integer id) throws AppException {

    Project project = projectFacade.find(id);
    if (project != null) {
      return project;
    } else {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
  }

  public boolean addServices(Project project, List<ProjectServiceEnum> services,
          String userEmail) {
    boolean addedService = false;
    //Add the desired services
    boolean sshAdded = false;
    for (ProjectServiceEnum se : services) {
      if (!projectServicesFacade.findEnabledServicesForProject(project).
              contains(se)) {
        projectServicesFacade.addServiceForProject(project, se);
        addedService = true;
        if (se == ProjectServiceEnum.SSH) {
          sshAdded = true;
        }
      }
    }

    if (addedService) {
      User user = userBean.getUserByEmail(userEmail);
      logActivity(ActivityFacade.ADDED_SERVICES, ActivityFacade.FLAG_PROJECT,
              user, project);
      if (sshAdded == true) {
        try {

          // For all members of the project, create an account for them and copy their public keys to ~/.ssh/authorized_keys
          List<ProjectTeam> members = projectTeamFacade.findMembersByProject(
                  project);
          for (ProjectTeam pt : members) {
            User myUser = pt.getUser();
            List<SshKeys> keys = sshKeysBean.findAllById(myUser.getUid());
            List<String> publicKeys = new ArrayList<>();
            for (SshKeys k : keys) {
              publicKeys.add(k.getPublicKey());
            }
            LocalhostServices.createUserAccount(myUser.getUsername(), project.
                    getName(), publicKeys);
          }

        } catch (IOException e) {
          // TODO - propagate exception?
          logger.warning("Could not create user account: " + e.getMessage());
        }
      }

    }
    return addedService;
  }

  /**
   * change the name of a project but not fully implemented to change the
   * related folder name in hdfs
   * <p/>
   *
   * @param project
   * @param newProjectName
   * @param userEmail
   * @throws AppException
   * @throws IOException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void changeName(Project project, String newProjectName,
          String userEmail)
          throws AppException, IOException {
    User user = userBean.getUserByEmail(userEmail);

    boolean nameExists = projectFacade.projectExistsForOwner(newProjectName,
            user);

    if (FolderNameValidator.isValidName(newProjectName) && nameExists) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NAME_EXIST);
    }

    String oldProjectName = project.getName();
    project.setName(newProjectName);
    projectFacade.mergeProject(project);
    fileOps.renameInHdfs(oldProjectName, newProjectName);

    logActivity(ActivityFacade.PROJECT_NAME_CHANGED, ActivityFacade.FLAG_PROJECT,
            user, project);
  }

  /**
   * Change the project description
   * <p/>
   *
   * @param project
   * @param newProjectDesc
   * @param userEmail of the user making the change
   */
  public void changeProjectDesc(Project project, String newProjectDesc,
          String userEmail) {
    User user = userBean.getUserByEmail(userEmail);

    project.setDescription(newProjectDesc);
    projectFacade.mergeProject(project);

    logActivity(ActivityFacade.PROJECT_DESC_CHANGED, ActivityFacade.FLAG_PROJECT,
            user, project);
  }

  //Set the project owner as project master in ProjectTeam table
  private void addProjectOwner(Integer project_id, String userName) {
    ProjectTeamPK stp = new ProjectTeamPK(project_id, userName);
    ProjectTeam st = new ProjectTeam(stp);
    st.setTeamRole(ProjectRoleTypes.DATA_OWNER.getTeam());
    st.setTimestamp(new Date());
    projectTeamFacade.persistProjectTeam(st);
  }

  //create project in HDFS
  private boolean mkProjectDIR(String projectName) throws IOException {

    String rootDir = Constants.DIR_ROOT;

    boolean rootDirCreated = false;
    boolean projectDirCreated = false;
    boolean childDirCreated = false;

    if (!fileOps.isDir(Constants.DIR_ROOT)) {
      /*
       * if the base path does not exist in the file system, create it first
       * and set it metaEnabled so that other folders down the dir tree
       * are getting registered in hdfs_metadata_log table
       */
      rootDirCreated = fileOps.mkDir(File.separator + rootDir);
      fileOps.setMetaEnabled(File.separator + rootDir);
    } else {
      rootDirCreated = true;
    }

    /*
     * Marking a project path as meta enabled means that all child folders/files
     * that'll be created down this directory tree will have as a parent this
     * inode.
     */
    String fullProjectPath = File.separator + rootDir + File.separator
            + projectName;
    String project = this.extractProjectName(fullProjectPath + File.separator);
    String projectPath = File.separator + rootDir + File.separator + project;

    //Create first the projectPath
    projectDirCreated = fileOps.mkDir(projectPath);
    fileOps.setMetaEnabled(projectPath);

    //create the rest of the child folders if any
    if (projectDirCreated && !fullProjectPath.equals(projectPath)) {
      childDirCreated = fileOps.mkDir(fullProjectPath);
    } else if (projectDirCreated) {
      childDirCreated = true;
    }

    return rootDirCreated && projectDirCreated && childDirCreated;
  }

  /**
   * Remove a project and optionally all associated files.
   *
   * @param projectID to be removed
   * @param email
   * @param deleteFilesOnRemove if the associated files should be deleted
   * @return true if the project and the associated files are removed
   * successfully, and false if the associated files could not be removed.
   * @throws IOException if the hole operation failed. i.e the project is not
   * removed.
   * @throws AppException if the project could not be found.
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public boolean removeByID(Integer projectID, String email,
          boolean deleteFilesOnRemove) throws IOException, AppException {
    boolean success = !deleteFilesOnRemove;
    User user = userBean.getUserByEmail(email);
    Project project = projectFacade.find(projectID);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    projectFacade.remove(project);
    //if we remove the project we cant store activity that has a reference to it!!
    //logActivity(ActivityFacade.REMOVED_PROJECT,
    //ActivityFacade.FLAG_PROJECT, user, project);

    if (deleteFilesOnRemove) {
      String path = File.separator + Constants.DIR_ROOT + File.separator
              + project.getName();
      success = fileOps.rmRecursive(path);
    }
    logger.log(Level.FINE, "{0} - project removed.", project.getName());

    return success;
  }

  /**
   * Adds new team members to a project(project) - bulk persist if team role
   * not specified or not in (Data owner or Data scientist)defaults to Data
   * scientist
   * <p/>
   *
   * @param project
   * @param email
   * @param projectTeams
   * @return a list of user names that could not be added to the project team
   * list.
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<String> addMembers(Project project, String email,
          List<ProjectTeam> projectTeams) {
    List<String> failedList = new ArrayList<>();
    User user = userBean.getUserByEmail(email);
    User newMember;
    for (ProjectTeam projectTeam : projectTeams) {
      try {
        if (!projectTeam.getProjectTeamPK().getTeamMember().equals(user.
                getEmail())) {

          //if the role is not properly set set it to the default resercher.
          if (projectTeam.getTeamRole() == null || (!projectTeam.getTeamRole().
                  equals(ProjectRoleTypes.DATA_SCIENTIST.getTeam())
                  && !projectTeam.
                  getTeamRole().equals(ProjectRoleTypes.DATA_OWNER.getTeam()))) {
            projectTeam.setTeamRole(ProjectRoleTypes.DATA_SCIENTIST.getTeam());
          }

          projectTeam.setTimestamp(new Date());
          newMember = userBean.getUserByEmail(projectTeam.getProjectTeamPK().
                  getTeamMember());
          if (newMember != null && !projectTeamFacade.isUserMemberOfProject(
                  project, newMember)) {
            //this makes sure that the member is added to the project sent as the
            //first param b/c the securty check was made on the parameter sent as path.
            projectTeam.getProjectTeamPK().setProjectId(project.getId());
            projectTeamFacade.persistProjectTeam(projectTeam);
            logger.log(Level.FINE, "{0} - member added to project : {1}.",
                    new Object[]{newMember.getEmail(),
                      project.getName()});

            List<SshKeys> keys = sshKeysBean.findAllById(newMember.getUid());
            List<String> publicKeys = new ArrayList<>();
            for (SshKeys k : keys) {
              publicKeys.add(k.getPublicKey());
            }

            logActivity(ActivityFacade.NEW_MEMBER + projectTeam.
                    getProjectTeamPK().getTeamMember(),
                    ActivityFacade.FLAG_PROJECT, user, project);
            createUserAccount(project, projectTeam, publicKeys, failedList);
          } else if (newMember == null) {
            failedList.add(projectTeam.getProjectTeamPK().getTeamMember()
                    + " was not found in the system.");
          } else {
            failedList.add(newMember.getEmail()
                    + " is already a member in this project.");
          }

        }
      } catch (EJBException ejb) {
        failedList.add(projectTeam.getProjectTeamPK().getTeamMember()
                + "could not be added. Try again later.");
        logger.log(Level.SEVERE, "Adding  team member {0} to members failed",
                projectTeam.getProjectTeamPK().getTeamMember());
      }
    }
    return failedList;
  }

  // Create Account for user on localhost if the SSH service is enabled
  private void createUserAccount(Project project, ProjectTeam projectTeam,
          List<String> publicKeys, List<String> failedList) {
    for (ProjectServices ps : project.getProjectServicesCollection()) {
      if (ps.getProjectServicesPK().getService().compareTo(
              ProjectServiceEnum.SSH) == 0) {
        try {
          String email = projectTeam.getProjectTeamPK().getTeamMember();
          User user = userBean.getUserByEmail(email);
          LocalhostServices.createUserAccount(user.getUsername(), project.
                  getName(), publicKeys);
        } catch (IOException e) {
          failedList.add(projectTeam.getProjectTeamPK().getTeamMember()
                  + "could not create the account on localhost. Try again later.");
          logger.log(Level.SEVERE,
                  "Create account on localhost for team member {0} failed",
                  projectTeam.getProjectTeamPK().getTeamMember());
        }
      }
    }
  }

  /**
   * Project info as data transfer object that can be sent to the user.
   * <p/>
   *
   * @param projectID of the project
   * @return project DTO that contains team members and services
   * @throws se.kth.hopsworks.rest.AppException
   */
  public ProjectDTO getProjectByID(Integer projectID) throws AppException {
    Project project = projectFacade.find(projectID);

    //find the project as an inode from hops database
    Inode inode = inodes.getInodeAtPath(File.separator + Constants.DIR_ROOT
            + File.separator + project.getName());

    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    List<ProjectTeam> projectTeam = projectTeamFacade.findMembersByProject(
            project);
    List<ProjectServiceEnum> projectServices = projectServicesFacade.
            findEnabledServicesForProject(project);
    List<String> services = new ArrayList<>();
    for (ProjectServiceEnum s : projectServices) {
      services.add(s.toString());
    }
    return new ProjectDTO(project, inode.getId(), services, projectTeam);
  }

  /**
   * Project info as data transfer object that can be sent to the user.
   * <p/>
   *
   * @param name
   * @return project DTO that contains team members and services
   * @throws se.kth.hopsworks.rest.AppException
   */
  public ProjectDTO getProjectByName(String name) throws AppException {
    //find the project entity from hopsworks database
    Project project = projectFacade.findByName(name);

    //find the project as an inode from hops database
    Inode inode = inodes.getInodeAtPath(File.separator + Constants.DIR_ROOT
            + File.separator + project.getName());

    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    List<ProjectTeam> projectTeam = projectTeamFacade.findMembersByProject(
            project);
    List<ProjectServiceEnum> projectServices = projectServicesFacade.
            findEnabledServicesForProject(project);
    List<String> services = new ArrayList<>();
    for (ProjectServiceEnum s : projectServices) {
      services.add(s.toString());
    }
    Inode parent;
    List<InodeView> kids = new ArrayList<>();

    Collection<Dataset> dsInProject = project.getDatasetCollection();
    for (Dataset ds : dsInProject) {
      parent = inodes.findParent(ds.getInode());
      kids.add(new InodeView(parent, ds, inodes.getPath(ds.getInode())));
    }

    //send the project back to client
    return new ProjectDTO(project, inode.getId(), services, projectTeam, kids);
  }

  /**
   * Deletes a member from a project
   *
   * @param project
   * @param email
   * @param toRemoveEmail
   * @throws AppException
   */
  public void deleteMemberFromTeam(Project project, String email,
          String toRemoveEmail) throws AppException {
    User userToBeRemoved = userBean.getUserByEmail(toRemoveEmail);
    if (userToBeRemoved == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.USER_DOES_NOT_EXIST);
      //user not found
    }
    ProjectTeam projectTeam = projectTeamFacade.findProjectTeam(project,
            userToBeRemoved);
    if (projectTeam == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.TEAM_MEMBER_NOT_FOUND);
    }
    projectTeamFacade.removeProjectTeam(project, userToBeRemoved);
    User user = userBean.getUserByEmail(email);
    logActivity(ActivityFacade.REMOVED_MEMBER + toRemoveEmail,
            ActivityFacade.FLAG_PROJECT, user, project);

    try {
      LocalhostServices.deleteUserAccount(email, project.getName());
    } catch (IOException e) {
      String errMsg = "Could not delete user account: " + LocalhostServices.
              getUsernameInProject(email, project.getName()) + " ";
      logger.warning(errMsg + e.getMessage());
      //  TODO: Should this be rethrown to give a HTTP Response to client??
    }
  }

  /**
   * Updates the role of a member
   * <p/>
   *
   * @param project
   * @param owner that is performing the update
   * @param toUpdateEmail
   * @param newRole
   * @throws AppException
   */
  public void updateMemberRole(Project project, String owner,
          String toUpdateEmail, String newRole) throws AppException {
    User projOwner = userBean.getUserByEmail(owner);
    User user = userBean.getUserByEmail(toUpdateEmail);
    if (user == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.USER_DOES_NOT_EXIST);
      //user not found
    }
    ProjectTeam projectTeam = projectTeamFacade.findProjectTeam(project, user);
    if (projectTeam == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.TEAM_MEMBER_NOT_FOUND);
      //member not found
    }
    if (!projectTeam.getTeamRole().equals(newRole)) {
      projectTeam.setTeamRole(newRole);
      projectTeam.setTimestamp(new Date());
      projectTeamFacade.update(projectTeam);
      logActivity(ActivityFacade.CHANGE_ROLE + toUpdateEmail,
              ActivityFacade.FLAG_PROJECT, projOwner, project);
    }

  }

  /**
   * Retrieves all the project teams that a user have a role
   * <p/>
   *
   * @param email of the user
   * @return a list of project team
   */
  public List<ProjectTeam> findProjectByUser(String email) {
    User user = userBean.getUserByEmail(email);
    return projectTeamFacade.findByMember(user);
  }

  /**
   * Retrieves all the project teams for a project
   * <p/>
   *
   * @param projectID
   * @return a list of project team
   */
  public List<ProjectTeam> findProjectTeamById(Integer projectID) {
    Project project = projectFacade.find(projectID);
    return projectTeamFacade.findMembersByProject(project);
  }

  /**
   * Logs activity
   * <p/>
   *
   * @param activityPerformed the description of the operation performed
   * @param flag on what the operation was performed(FLAG_PROJECT, FLAG_USER)
   * @param performedBy the user that performed the operation
   * @param performedOn the project the operation was performed on.
   */
  public void logActivity(String activityPerformed, String flag,
          User performedBy, Project performedOn) {
    Date now = new Date();
    Activity activity = new Activity();
    activity.setActivity(activityPerformed);
    activity.setFlag(flag);
    activity.setProject(performedOn);
    activity.setTimestamp(now);
    activity.setUser(performedBy);

    activityFacade.persistActivity(activity);
  }

  /**
   * Extracts the project name out of the given path. The project name is the
   * second part of this path.
   * <p>
   * @param path
   * @return
   */
  private String extractProjectName(String path) {

    int startIndex = path.indexOf('/', 1);
    int endIndex = path.indexOf('/', startIndex + 1);

    return path.substring(startIndex + 1, endIndex);
  }
}
