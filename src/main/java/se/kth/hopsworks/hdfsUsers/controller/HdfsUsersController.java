package se.kth.hopsworks.hdfsUsers.controller;

import se.kth.hopsworks.hdfsUsers.model.HdfsGroups;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.hdfsUsers.HdfsUsersFacade;
import se.kth.hopsworks.user.model.Users;
import io.hops.security.UsersGroups;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import se.kth.bbc.fileoperations.FileSystemOperations;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.project.ProjectTeam;
import se.kth.bbc.project.ProjectTeamFacade;
import se.kth.hopsworks.dataset.Dataset;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.hdfsUsers.HdfsGroupsFacade;
import se.kth.hopsworks.hdfsUsers.model.HdfsUsers;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HdfsUsersController {

  public static final String USER_NAME_DELIMITER = "__";

  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsGroupsFacade hdfsGroupsFacade;
  @EJB
  private FileSystemOperations fsOps;
  @EJB
  private ProjectTeamFacade teamFacade;

  /**
   * Add a new user in HDFS with the name
   * <code>projectName</code>__<code>username</code> , if it does not exist,
   * and gets added to the group <code>projectName</code>. This will add a
   * data_owner with full privileges to the project. If the project is an
   * existing one(i.e. if the group <code>projectName</code> exists) the member
   * will also gets added to all the dataset groups belonging to the project.
   * <p>
   * @param user
   * @param project
   * @throws java.io.IOException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void addProjectMember(Users user, Project project) throws IOException {
    String userName = project.getName() + USER_NAME_DELIMITER + user.
            getUsername();
    byte[] userId = UsersGroups.getUserID(userName);
    byte[] groupId = UsersGroups.getGroupID(project.getName());
    HdfsUsers hdfsUser = hdfsUsersFacade.findHdfsUser(userId);
    if (hdfsUser != null) {
      throw new IllegalArgumentException("User name already exists.");
    }
    hdfsUser = new HdfsUsers(userId, userName);
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findHdfsGroup(groupId);
    if (hdfsGroup == null) {
      throw new IllegalArgumentException("No group found for project in HDFS.");
    }
    if (hdfsUser.getHdfsGroupsCollection() == null) {
      hdfsUser.setHdfsGroupsCollection(new ArrayList<HdfsGroups>());
    }
    if (!hdfsUser.getHdfsGroupsCollection().contains(hdfsGroup)) {
      //add the user to the project group.
      hdfsUser.getHdfsGroupsCollection().add(hdfsGroup);
    }

    byte[] dsGroupId;
    String dsGroups;
    HdfsGroups hdfsDsGroup;
    //add the user to all dataset groups in the project.
    for (Dataset ds : project.getDatasetCollection()) {
      dsGroups = project.getName() + USER_NAME_DELIMITER + ds.getInode().
              getInodePK().getName();
      dsGroupId = UsersGroups.getGroupID(dsGroups);
      hdfsDsGroup = hdfsGroupsFacade.findHdfsGroup(dsGroupId);
      if (hdfsDsGroup != null && !hdfsUser.getHdfsGroupsCollection().contains(
              hdfsDsGroup)) {
        hdfsUser.getHdfsGroupsCollection().add(hdfsDsGroup);
      }
    }
    hdfsUsersFacade.persist(hdfsUser);
  }

  /**
   * Creates a new group in HDFS with the name <code>projectName</code> if it
   * does not exist, then creates the owner in HDFS with the name
   * <code>projectName</code>__<code>username</code> ,
   * also if it does not exist, and gets added to the group
   * <code>projectName</code>.
   * <p>
   * @param project
   * @throws java.io.IOException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void addProjectFolderOwner(Project project) throws IOException {
    String owner = project.getName() + USER_NAME_DELIMITER + project.getOwner().
            getUsername();
    byte[] userId = UsersGroups.getUserID(owner);
    byte[] groupId = UsersGroups.getGroupID(project.getName());
    HdfsUsers hdfsUser = hdfsUsersFacade.findHdfsUser(userId);

    if (hdfsUser != null) {
      System.err.println("hdfs user---------" + hdfsUser.getName());
      throw new IllegalArgumentException("User name already exists.");
    }
    hdfsUser = new HdfsUsers(userId, owner);
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findHdfsGroup(groupId);
    //if new project set owner and permission.
    if (hdfsGroup == null) {
      hdfsGroup = new HdfsGroups(groupId, project.getName());
      String projectPath = File.separator + Constants.DIR_ROOT + File.separator
              + project.getName();
      Path location = new Path(projectPath);
      //FsPermission(FsAction u, FsAction g, FsAction o) 775
      //Gives owner and group all access and read, execute for others
      //This means group is for data_owners and others for data_scientist
      //This means every body can see the content of a project.
      FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL,
              FsAction.READ_EXECUTE);// 775
      fsOps.setOwner(location, owner, hdfsGroup.getName());
      fsOps.setPermission(location, fsPermission);
    }
    if (hdfsUser.getHdfsGroupsCollection() == null) {
      hdfsUser.setHdfsGroupsCollection(new ArrayList<HdfsGroups>());
    }
    if (!hdfsUser.getHdfsGroupsCollection().contains(hdfsGroup)) {
      hdfsUser.getHdfsGroupsCollection().add(hdfsGroup);
    }
    hdfsUsersFacade.persist(hdfsUser);
  }

  /**
   * Adds all members of the project, as a user in HDFS with the name
   * <code>projectName</code>__<code>username</code> and adds them to the group
   * <code>projectName</code>.
   * <p>
   * @param project
   * @throws java.io.IOException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void addAllProjectMembers(Project project) throws IOException {
    byte[] groupId = UsersGroups.getGroupID(project.getName());
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findHdfsGroup(groupId);
    if (hdfsGroup == null) {
      throw new IllegalArgumentException("No group found for project in HDFS.");
    }
    String hdfsUsername;
    HdfsUsers memberHdfsUser;
    byte[] memberUserId;
    for (ProjectTeam member : project.getProjectTeamCollection()) {
      hdfsUsername = project.getName() + USER_NAME_DELIMITER + member.
              getUser().getUsername();
      memberUserId = UsersGroups.getUserID(hdfsUsername);
      memberHdfsUser = hdfsUsersFacade.findHdfsUser(memberUserId);
      if (memberHdfsUser == null) {
        memberHdfsUser = new HdfsUsers(memberUserId, hdfsUsername);
      }
      //add only data_owners to project group
      if (member.getTeamRole().equals(AllowedRoles.DATA_OWNER)) {
        if (memberHdfsUser.getHdfsGroupsCollection() == null) {
          memberHdfsUser.setHdfsGroupsCollection(new ArrayList<HdfsGroups>());
        }
        if (!memberHdfsUser.getHdfsGroupsCollection().contains(hdfsGroup)) {
          memberHdfsUser.getHdfsGroupsCollection().add(hdfsGroup);
        }
      }
      byte[] dsGroupId;
      String dsGroups;
      HdfsGroups hdfsDsGroup;
      // add all members to dataset groups in the project.
      for (Dataset ds : project.getDatasetCollection()) {
        dsGroups = project.getName() + USER_NAME_DELIMITER + ds.getInode().
                getInodePK().getName();
        dsGroupId = UsersGroups.getGroupID(dsGroups);
        hdfsDsGroup = hdfsGroupsFacade.findHdfsGroup(dsGroupId);
        if (hdfsDsGroup != null && !memberHdfsUser.getHdfsGroupsCollection().
                contains(hdfsDsGroup)) {
          memberHdfsUser.getHdfsGroupsCollection().add(hdfsDsGroup);
        }
      }
      hdfsUsersFacade.persist(memberHdfsUser);
    }
  }

  /**
   * Create a new group in HDFS with the name project.name__datasetName if it
   * does not exist, then adds all members of the project to this group. This is
   * done when a new dataset is created in a project.
   * If stickyBit is set true: all members of the project will be given r, w, x
   * privileges.
   * If stickyBit is set false: user will get all privileges, and all other
   * members
   * will have r and x privileges.
   * <p>
   * @param owner
   * @param project
   * @param datasetName
   * @param stickyBit
   * @throws java.io.IOException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void addDatasetUsersGroups(Users owner, Project project,
          String datasetName, boolean stickyBit) throws IOException {
    if (owner == null || project == null || project.getProjectTeamCollection()
            == null || datasetName == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    ProjectTeam pt = teamFacade.findByPrimaryKey(project, owner);
    if (pt == null || !pt.getTeamRole().equals(AllowedRoles.DATA_OWNER)) {
      throw new IllegalArgumentException(
              "User not in project team or does not have privilege to create a dataset.");
    }
    String datasetGroup = project.getName() + USER_NAME_DELIMITER + datasetName;
    String dsOwner = project.getName() + USER_NAME_DELIMITER + owner.
            getUsername();

    byte[] groupId = UsersGroups.getGroupID(datasetGroup);
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findHdfsGroup(groupId);
    if (hdfsGroup == null) {
      hdfsGroup = new HdfsGroups(groupId, datasetGroup);
      String dsPath = File.separator + Constants.DIR_ROOT + File.separator
              + project.getName() + File.separator + datasetName;
      Path location = new Path(dsPath);
      //FsPermission(FsAction u, FsAction g, FsAction o, boolean sb) 775
      FsPermission fsPermission = new FsPermission(FsAction.ALL,
              FsAction.READ_EXECUTE,
              FsAction.NONE);//Permission hdfs dfs -chmod 750
      if (stickyBit) {
        fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL,
                FsAction.NONE, stickyBit);//Permission hdfs dfs -chmod 1770
      }
      fsOps.setOwner(location, dsOwner, hdfsGroup.getName());
      fsOps.setPermission(location, fsPermission);
    }
    String hdfsUsername;
    HdfsUsers hdfsUser;
    byte[] userId;
    for (ProjectTeam member : project.getProjectTeamCollection()) {
      hdfsUsername = project.getName() + USER_NAME_DELIMITER + member.getUser().
              getUsername();
      userId = UsersGroups.getUserID(hdfsUsername);
      hdfsUser = hdfsUsersFacade.findHdfsUser(userId);
      if (hdfsUser == null) {
        hdfsUser = new HdfsUsers(userId, hdfsUsername);
      }
      if (hdfsUser.getHdfsGroupsCollection() == null) {
        hdfsUser.setHdfsGroupsCollection(new ArrayList<HdfsGroups>());
      }
      if (!hdfsUser.getHdfsGroupsCollection().contains(hdfsGroup)) {
        hdfsUser.getHdfsGroupsCollection().add(hdfsGroup);
      }
      hdfsUsersFacade.persist(hdfsUser);
    }

  }

  /**
   * This will add the user to all dataset groups in the project. This will
   * give the user read and execute privileges to the datasets in the project.
   * (make he user a data_scientist in the project)
   * <p>
   * @param user
   * @param project
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void addUserToDatasetGroups(Users user, Project project) {
    if (user == null || project == null || project.getProjectTeamCollection()
            == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    ProjectTeam pt = teamFacade.findByPrimaryKey(project, user);
    if (pt == null || !pt.getTeamRole().equals(AllowedRoles.DATA_OWNER)) {
      throw new IllegalArgumentException(
              "User not in project team or does not have privilege to create a dataset.");
    }
    String userName = project.getName() + USER_NAME_DELIMITER + user.
            getUsername();
    byte[] userId = UsersGroups.getUserID(userName);
    HdfsUsers hdfsUser = hdfsUsersFacade.findHdfsUser(userId);
    if (hdfsUser != null) {
      throw new IllegalArgumentException("User name already exists.");
    }
    hdfsUser = new HdfsUsers(userId, userName);
    if (hdfsUser.getHdfsGroupsCollection() == null) {
      hdfsUser.setHdfsGroupsCollection(new ArrayList<HdfsGroups>());
    }
    byte[] dsGroupId;
    String dsGroups;
    HdfsGroups hdfsGroup;
    for (Dataset ds : project.getDatasetCollection()) {
      dsGroups = project.getName() + USER_NAME_DELIMITER + ds.getInode().
              getInodePK().getName();
      dsGroupId = UsersGroups.getGroupID(dsGroups);
      hdfsGroup = hdfsGroupsFacade.findHdfsGroup(dsGroupId);
      if (hdfsGroup != null && !hdfsUser.getHdfsGroupsCollection().contains(
              hdfsGroup)) {
        hdfsUser.getHdfsGroupsCollection().add(hdfsGroup);
      }
    }
    hdfsUsersFacade.persist(hdfsUser);
  }

  /**
   * Removes the user project__username. This should cascade to the groups the
   * user is a member of. This can be used to remove a data_owner or a
   * data_scientist from project.
   * <p>
   * @param user
   * @param project
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void removeProjectMember(Users user, Project project) {
    if (user == null || project == null || project.getProjectTeamCollection()
            == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    //should check if the user is in the project team
    //waiting user refuctoring.
    String userName = project.getName() + USER_NAME_DELIMITER + user.
            getUsername();
    byte[] userId = UsersGroups.getUserID(userName);
    HdfsUsers hdfsUser = hdfsUsersFacade.findHdfsUser(userId);
    if (hdfsUser != null) {
      hdfsUsersFacade.remove(hdfsUser);
    }
  }

  /**
   * Removes the user project__username from the group projectName.
   * This means the user is no longer a data_owner in this project.
   * (will be a data_scientist with r, x privileges on datasets inside the
   * project)
   * <p>
   * @param user
   * @param project
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void modifyProjectMembership(Users user, Project project) {
    if (user == null || project == null || project.getProjectTeamCollection()
            == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    //should check if the user is in the project team
    //waiting user refuctoring.
    String userName = project.getName() + USER_NAME_DELIMITER + user.
            getUsername();
    byte[] userId = UsersGroups.getUserID(userName);
    byte[] groupId = UsersGroups.getGroupID(project.getName());
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findHdfsGroup(groupId);
    HdfsUsers hdfsUser = hdfsUsersFacade.findHdfsUser(userId);
    if (hdfsUser == null || hdfsGroup == null || !hdfsUser.
            getHdfsGroupsCollection().contains(hdfsGroup)) {
      throw new IllegalArgumentException(
              "Hdfs user not found or not in project group.");
    }
    hdfsUser.getHdfsGroupsCollection().remove(hdfsGroup);
    hdfsUsersFacade.merge(hdfsUser);
  }

  /**
   * Adds all members of project to the dataset's group. This will give the
   * added members read and execute privileges.
   * <p>
   * @param project
   * @param dataset
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void shareDataset(Project project, Dataset dataset) {
    if (project == null || project.getProjectTeamCollection()
            == null || dataset == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    if (dataset.getInode().getInodePK().getParentId() != dataset.getProjectId().
            getInode().getId()) {
      throw new IllegalArgumentException("Dataset is not owned by project.");
    }
    //should check if the user is in the project team
    //waiting user refuctoring. 
    String datasetGroup = dataset.getProjectId().getName() + USER_NAME_DELIMITER
            + dataset.getInode().getInodePK().getName();
    byte[] groupId = UsersGroups.getGroupID(datasetGroup);
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findHdfsGroup(groupId);
    String hdfsUsername;
    HdfsUsers hdfsUser;
    byte[] userId;
    for (ProjectTeam member : project.getProjectTeamCollection()) {
      hdfsUsername = project.getName() + USER_NAME_DELIMITER + member.getUser().
              getUsername();
      userId = UsersGroups.getUserID(hdfsUsername);
      hdfsUser = hdfsUsersFacade.findHdfsUser(userId);
      if (hdfsUser == null) {
        hdfsUser = new HdfsUsers(userId, hdfsUsername);
      }
      if (hdfsUser.getHdfsGroupsCollection() == null) {
        hdfsUser.setHdfsGroupsCollection(new ArrayList<HdfsGroups>());
      }
      if (!hdfsUser.getHdfsGroupsCollection().contains(hdfsGroup)) {
        hdfsUser.getHdfsGroupsCollection().add(hdfsGroup);
      }
      hdfsUsersFacade.persist(hdfsUser);
    }
  }

}
