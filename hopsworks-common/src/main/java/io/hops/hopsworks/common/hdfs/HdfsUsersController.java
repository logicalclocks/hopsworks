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

package io.hops.hopsworks.common.hdfs;

import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsGroups;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsGroupsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.util.Settings;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class HdfsUsersController {

  private static final Logger LOGGER = Logger.getLogger(
      HdfsUsersController.class.
          getName());
  public static final String USER_NAME_DELIMITER = "__";

  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsGroupsFacade hdfsGroupsFacade;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private InodeFacade inodes;
  @EJB
  private UserFacade userFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private ProjectTeamFacade projectTeamFacade;

  /**
   * Creates a new group in HDFS with the name <code>projectName</code> if it
   * does not exist, then creates the owner in HDFS with the name
   * <code>projectName</code>__<code>username</code> , also if it does not
   * exist, and gets added to the group <code>projectName</code>.
   * <p>
   * @param project
   * @param dfso
   * @throws java.io.IOException
   */
  public void addProjectFolderOwner(Project project,
      DistributedFileSystemOps dfso) throws IOException {
    String owner = getHdfsUserName(project, project.getOwner());
    String projectPath = File.separator + Settings.DIR_ROOT + File.separator
        + project.getName();
    Path location = new Path(projectPath);
    //FsPermission(FsAction u, FsAction g, FsAction o) 555
    //We prohibit a user from creating top-level datasets bypassing Hopsworks UI (i.e. from as Spark app)
    FsPermission fsPermission = new FsPermission(FsAction.READ_EXECUTE,
        FsAction.READ_EXECUTE, FsAction.READ_EXECUTE);// 555
    dfso.setOwner(location, owner, project.getName());
    dfso.setPermission(location, fsPermission);
  }

  /**
   * Adds a new member to the project. This will create a new user in HDFS
   * with the name <code>projectName</code>__<code>username</code> and adds it
   * to the group <code>projectName</code>. throws IllegalArgumentException if
   * the project group is not found.
   * <p>
   * @param project
   * @param member
   * @throws java.io.IOException
   */
  public void addNewProjectMember(Project project, ProjectTeam member) {
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(project.getName());
    if (hdfsGroup == null) {
      throw new IllegalArgumentException("No group found for project in HDFS.");
    }
    String hdfsUsername;
    HdfsUsers memberHdfsUser;
    Users newMember = userFacade.findByEmail(member.getProjectTeamPK().
        getTeamMember());
    hdfsUsername = getHdfsUserName(project, newMember);
    memberHdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
    if (memberHdfsUser == null) {
      memberHdfsUser = new HdfsUsers(hdfsUsername);
      hdfsUsersFacade.persist(memberHdfsUser);
    }
    if (memberHdfsUser.getHdfsGroupsCollection() == null) {
      memberHdfsUser.setHdfsGroupsCollection(new ArrayList<>());
    }
    //add only data_owners to project group
    if (member.getTeamRole().equals(AllowedRoles.DATA_OWNER)) {
      if (!memberHdfsUser.getHdfsGroupsCollection().contains(hdfsGroup)) {
        memberHdfsUser.getHdfsGroupsCollection().add(hdfsGroup);
      }
    }
    String dsGroups;
    HdfsGroups hdfsDsGroup;
    // add the member to all dataset groups in the project.
    List<Dataset> dsInProject = datasetFacade.findByProject(project);
    for (Dataset ds : dsInProject) {
      dsGroups = getHdfsGroupName(ds);
      hdfsDsGroup = hdfsGroupsFacade.findByName(dsGroups);
      if (hdfsDsGroup != null) {
        if (!memberHdfsUser.getHdfsGroupsCollection().contains(hdfsDsGroup)) {
          memberHdfsUser.getHdfsGroupsCollection().add(hdfsDsGroup);
        }
      }
    }
    hdfsUsersFacade.merge(memberHdfsUser);
  }

  /**
   * Adds a user to project group if the member have a Data owner role in the
   * project.
   * <p>
   * throws IllegalArgumentException if the project group is not found.
   * <p>
   * @param project
   * @param member
   */
  public void addUserToProjectGroup(Project project, ProjectTeam member) {
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(project.getName());
    if (hdfsGroup == null) {
      throw new IllegalArgumentException("No group found for project in HDFS.");
    }
    String hdfsUsername;
    HdfsUsers memberHdfsUser;
    Users newMember = userFacade.findByEmail(member.getProjectTeamPK().
        getTeamMember());
    hdfsUsername = getHdfsUserName(project, newMember);
    memberHdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
    if (memberHdfsUser == null) {
      memberHdfsUser = new HdfsUsers(hdfsUsername);
      hdfsUsersFacade.persist(memberHdfsUser);
    }
    if (memberHdfsUser.getHdfsGroupsCollection() == null) {
      memberHdfsUser.setHdfsGroupsCollection(new ArrayList<>());
    }
    //add only data_owners to project group
    if (member.getTeamRole().equals(AllowedRoles.DATA_OWNER)) {
      if (!memberHdfsUser.getHdfsGroupsCollection().contains(hdfsGroup)) {
        memberHdfsUser.getHdfsGroupsCollection().add(hdfsGroup);
      }
    }
  }

  /**
   * Create a new group in HDFS with the name project.name__datasetName if it
   * does not exist, then adds all members of the project to this group. This
   * is done when a new dataset is created in a project. If stickyBit is set
   * true: all members of the project will be given r, w, x privileges. If
   * stickyBit is set false: user will get all privileges, and all other
   * members will have r and x privileges.
   * <p>
   * @param owner
   * @param project
   * @param dataset
   * @param dfso
   * @throws java.io.IOException
   */
  public void addDatasetUsersGroups(Users owner, Project project,
      Dataset dataset, DistributedFileSystemOps dfso) throws IOException {
    if (owner == null || project == null || project.getProjectTeamCollection()
        == null || dataset == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    String datasetGroup = getHdfsGroupName(project, dataset);
    String dsOwner = getHdfsUserName(project, owner);
    String dsPath = inodes.getPath(dataset.getInode());
    Path location = new Path(dsPath);
    dfso.setOwner(location, dsOwner, datasetGroup);

    String hdfsUsername;
    HdfsUsers hdfsUser;
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(datasetGroup);
    if (hdfsGroup == null) {
      throw new IllegalArgumentException(
          "Could not create dataset group in HDFS.");
    }
    if (hdfsGroup.getHdfsUsersCollection() == null) {
      hdfsGroup.setHdfsUsersCollection(new ArrayList<>());
    }
    /**
     * ****** add project generic user as a user ********
     */
    hdfsUser = hdfsUsersFacade.findByName(project.getProjectGenericUser());
    if (hdfsUser == null) {
      hdfsUser = new HdfsUsers(project.getProjectGenericUser());
      hdfsUsersFacade.persist(hdfsUser);
    }
    if (!hdfsGroup.getHdfsUsersCollection().contains(hdfsUser)) {
      hdfsGroup.getHdfsUsersCollection().add(hdfsUser);
    }
    /**
     * ****** **************************** ********
     */
    //add every member to the new ds group
    for (ProjectTeam member : project.getProjectTeamCollection()) {
      hdfsUsername = getHdfsUserName(project, member.getUser());
      hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
      //the owner does not need to be added to the group.
      if (hdfsUsername.equals(dsOwner)) {
        continue;
      }
      if (hdfsUser == null) {
        hdfsUser = new HdfsUsers(hdfsUsername);
        hdfsUsersFacade.persist(hdfsUser);
      }
      if (!hdfsGroup.getHdfsUsersCollection().contains(hdfsUser)) {
        hdfsGroup.getHdfsUsersCollection().add(hdfsUser);
      }
    }
    hdfsGroupsFacade.merge(hdfsGroup);
  }

  /**
   * Removes the user project__username. This should cascade to the groups the
   * user is a member of. This can be used to remove a data_owner or a
   * data_scientist from project.
   * <p>
   * @param user
   * @param project
   * @throws java.io.IOException
   */
  public void removeProjectMember(Users user, Project project) throws
      IOException {
    if (user == null || project == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    String userName = getHdfsUserName(project, user);
    HdfsUsers hdfsUser = hdfsUsersFacade.findByName(userName);
    dfsService.removeDfsOps(userName);
    removeHdfsUser(hdfsUser);
  }

  /**
   * Removes the user project__username from the group projectName. This means
   * the user is no longer a data_owner in this project. (will be a
   * data_scientist with r, x privileges on datasets inside the project)
   * <p>
   * @param user
   * @param project
   */
  public void modifyProjectMembership(Users user, Project project) {
    if (user == null || project == null || project.getProjectTeamCollection()
        == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    String userName = getHdfsUserName(project, user);
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(project.getName());
    HdfsUsers hdfsUser = hdfsUsersFacade.findByName(userName);
    if (hdfsUser == null || hdfsGroup == null) {
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
  public void shareDataset(Project project, Dataset dataset) {
    if (project == null || dataset == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    String datasetGroup = getHdfsGroupName(dataset);
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(datasetGroup);
    if (hdfsGroup == null) {
      throw new IllegalArgumentException("Dataset group not found");
    }
    if (hdfsGroup.getHdfsUsersCollection() == null) {
      hdfsGroup.setHdfsUsersCollection(new ArrayList<>());
    }
    String hdfsUsername;

    List<String> hdfsUsersToFlush = new ArrayList<>();

    HdfsUsers hdfsUser = hdfsUsersFacade.
        findByName(project.getProjectGenericUser());
    if (hdfsUser == null) {
      hdfsUser = new HdfsUsers(project.getProjectGenericUser());
      hdfsUsersFacade.persist(hdfsUser);
    }
    if (!hdfsGroup.getHdfsUsersCollection().contains(hdfsUser)) {
      hdfsGroup.getHdfsUsersCollection().add(hdfsUser);
      hdfsUsersToFlush.add(hdfsUser.getName());
    }

    Collection<ProjectTeam> projectTeam = projectTeamFacade.
        findMembersByProject(project);

    //every member of the project the ds is going to be shard with is
    //added to the dataset group.
    for (ProjectTeam member : projectTeam) {
      hdfsUsername = getHdfsUserName(project, member.getUser());
      hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
      if (hdfsUser == null) {
        hdfsUser = new HdfsUsers(hdfsUsername);
      }
      if (!hdfsGroup.getHdfsUsersCollection().contains(hdfsUser)) {
        hdfsGroup.getHdfsUsersCollection().add(hdfsUser);
        hdfsUsersToFlush.add(hdfsUser.getName());
      }
    }
    hdfsGroupsFacade.merge(hdfsGroup);
    for (String userName : hdfsUsersToFlush) {
      try {
        dfsService.getDfsOps().flushCache(userName, datasetGroup);
      } catch (IOException ex) {
        //FIXME: take an action?
        LOGGER.log(Level.WARNING,
            "Error while trying flush the cache", ex);
      }
    }
  }

  /**
   * Removes all members of project to the dataset's group.
   * <p>
   * @param project
   * @param dataset
   */
  public void unshareDataset(Project project, Dataset dataset) {
    if (project == null || dataset == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    String datasetGroup = getHdfsGroupName(dataset);
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(datasetGroup);
    if (hdfsGroup == null) {
      throw new IllegalArgumentException("Dataset group not found");
    }
    if (hdfsGroup.getHdfsUsersCollection() == null) {
      hdfsGroup.setHdfsUsersCollection(new ArrayList<>());
    }
    String hdfsUsername;
    HdfsUsers hdfsUser;

    List<String> hdfsUsersToFlush = new ArrayList<>();

    hdfsUser = hdfsUsersFacade.findByName(project.getName());
    if (hdfsUser != null && hdfsGroup.getHdfsUsersCollection().contains(hdfsUser)) {
      hdfsGroup.getHdfsUsersCollection().remove(hdfsUser);
      hdfsUsersToFlush.add(hdfsUser.getName());
    }

    Collection<ProjectTeam> projectTeam = projectTeamFacade.
        findMembersByProject(project);

    //every member of the project the ds is going to be unshard with is removed from the dataset group.
    for (ProjectTeam member : projectTeam) {
      hdfsUsername = getHdfsUserName(project, member.getUser());
      hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
      if (hdfsUser == null) {
        hdfsUser = new HdfsUsers(hdfsUsername);
      }
      if (hdfsGroup.getHdfsUsersCollection().contains(hdfsUser)) {
        hdfsGroup.getHdfsUsersCollection().remove(hdfsUser);
        hdfsUsersToFlush.add(hdfsUser.getName());
      }
    }
    hdfsGroupsFacade.merge(hdfsGroup);
    for (String userName : hdfsUsersToFlush) {
      try {
        dfsService.getDfsOps().flushCache(userName, datasetGroup);
      } catch (IOException ex) {
        //FIXME: take an action?
        LOGGER.log(Level.WARNING,
            "Error while trying flush the cache", ex);
      }
    }
  }

  /**
   * Deletes the project group from HDFS
   * <p>
   * @param project
   * @throws java.io.IOException
   */
  public void deleteProjectGroup(Project project) throws IOException {
    if (project == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(project.getName());
    removeHdfsGroup(hdfsGroup);
  }

  /**
   * Deletes the project group and all associated groups from HDFS
   * <p>
   * @param project
   * @param dsInProject
   * @throws java.io.IOException
   */
  public void deleteProjectGroupsRecursive(Project project,
      List<Dataset> dsInProject) throws IOException {
    if (project == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(project.getName());
    removeHdfsGroup(hdfsGroup);
    byte[] dsGroupId;
    String dsGroups;
    HdfsGroups hdfsDsGroup;
    for (Dataset ds : dsInProject) {
      dsGroups = getHdfsGroupName(project, ds);
      hdfsDsGroup = hdfsGroupsFacade.findByName(dsGroups);
      removeHdfsGroup(hdfsDsGroup);
    }
  }

  /**
   * Deletes the project group and all associated groups from HDFS
   * <p>
   * @param hdfsDsGroups
   * @throws java.io.IOException
   */
  public void deleteGroups(List<HdfsGroups> hdfsDsGroups) throws
      IOException {
    for (HdfsGroups hdfsDsGroup : hdfsDsGroups) {
      removeHdfsGroup(hdfsDsGroup);
    }
  }

  /**
   * Deletes all users associated with this project from HDFS
   * <p>
   * @param project
   * @param projectTeam
   * @throws java.io.IOException
   */
  public void deleteProjectUsers(Project project,
      Collection<ProjectTeam> projectTeam) throws IOException {
    if (project == null || projectTeam == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    String hdfsUsername;
    HdfsUsers hdfsUser;
    hdfsUser = hdfsUsersFacade.findByName(project.getName());
    removeHdfsUser(hdfsUser);
    for (ProjectTeam member : projectTeam) {
      hdfsUsername = getHdfsUserName(project, member.getUser());
      hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
      dfsService.removeDfsOps(hdfsUsername);
      removeHdfsUser(hdfsUser);
    }
  }

  /**
   * Deletes all users associated with this project from HDFS
   * <p>
   * @param users
   * @throws java.io.IOException
   */
  public void deleteUsers(Collection<HdfsUsers> users) throws IOException {
    for (HdfsUsers user : users) {
      dfsService.removeDfsOps(user.getName());
      removeHdfsUser(user);
    }
  }

  /**
   * Deletes the dataset group from HDFS
   * <p>
   * @param dataset
   * @throws java.io.IOException
   */
  public void deleteDatasetGroup(Dataset dataset) throws IOException {
    if (dataset == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    String datasetGroup = getHdfsGroupName(dataset);
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(datasetGroup);
    removeHdfsGroup(hdfsGroup);
  }

  /**
   * Remove all members of the project from the dataset group.
   * <p>
   * @param project
   * @param dataset
   */
  public void unShareDataset(Project project, Dataset dataset) {
    if (project == null || dataset == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    String datasetGroup = getHdfsGroupName(dataset);
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(datasetGroup);
    if (hdfsGroup == null) {
      throw new IllegalArgumentException("Dataset group not found");
    }
    if (hdfsGroup.getHdfsUsersCollection() == null) {
      throw new IllegalArgumentException("The dataset group have no members.");
    }
    Collection<ProjectTeam> projectTeam = projectTeamFacade.
        findMembersByProject(project);
    String hdfsUsername;
    HdfsUsers hdfsUser = hdfsUsersFacade.
        findByName(project.getProjectGenericUser());
    if (hdfsUser != null) {
      hdfsGroup.getHdfsUsersCollection().remove(hdfsUser);
    }

    //every member of the project the ds is going to be unshard from is
    //removed from the dataset group.
    for (ProjectTeam member : projectTeam) {
      hdfsUsername = getHdfsUserName(project, member.getUser());
      hdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
      if (hdfsUser != null) {
        try {
          dfsService.getDfsOps().flushCache(hdfsUsername, datasetGroup);
        } catch (IOException ex) {
          //FIXME: take an action?
          LOGGER.log(Level.WARNING,
              "Error while trying flush the cash", ex);
        }
        hdfsGroup.getHdfsUsersCollection().remove(hdfsUser);
      }
    }
    hdfsGroupsFacade.merge(hdfsGroup);
  }

  /**
   * Returns all the hdfs username corresponding to projectName
   * <p>
   * @param projectName
   * @return
   */
  public List<HdfsUsers> getAllProjectHdfsUsers(String projectName) {
    return hdfsUsersFacade.findProjectUsers(projectName);
  }

  /**
   * Returns all the hdfs groupname corresponding to projectName
   * <p>
   * @param projectName
   * @return
   */
  public List<HdfsGroups> getAllProjectHdfsGroups(String projectName) {
    return hdfsGroupsFacade.findProjectGroups(projectName);
  }

  public List<HdfsGroups> listProjectGroups(Project project, List<Dataset> dsInProject) {
    if (project == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    List<HdfsGroups> projectGroups = new ArrayList<>();
    projectGroups.add(hdfsGroupsFacade.findByName(project.getName()));
    String dsGroups;
    for (Dataset ds : dsInProject) {
      dsGroups = getHdfsGroupName(project, ds);
      projectGroups.add(hdfsGroupsFacade.findByName(dsGroups));
    }
    return projectGroups;
  }

  /**
   * Returns the hdfs username for the user in this project
   * <p>
   * @param project
   * @param user
   * @return
   */
  public String getHdfsUserName(Project project, Users user) {
    if (project == null || user == null) {
      throw new IllegalArgumentException("project or user were not provided");
    }
    return project.getName() + USER_NAME_DELIMITER + user.getUsername();
  }

  /**
   * Returns the username given a hdfs username
   * <p>
   * @param hdfsUser
   * @return
   */
  public String getUserName(String hdfsUser) {
    return hdfsUser.split(USER_NAME_DELIMITER)[1];
  }

  /**
   * Returns the project name given a hdfs username
   * <p>
   * @param hdfsUser
   * @return
   */
  public String getProjectName(String hdfsUser) {
    return hdfsUser.split(USER_NAME_DELIMITER)[0];
  }

  /**
   * If the dataset is shared with this project we will get a group name that
   * does not exist.
   * <p>
   * @param project
   * @param ds
   * @return
   */
  public String getHdfsGroupName(Project project, Dataset ds) {
    if (project == null || ds == null) {
      return null;
    }
    return project.getName() + USER_NAME_DELIMITER + ds.getInode().getInodePK().
        getName();
  }

  /**
   * If the dataset is shared with this project we will get a group name that
   * does not exist.
   * <p>
   * @param project
   * @param dataSetName
   * @return
   */
  public String getHdfsGroupName(Project project, String dataSetName) {
    if (project == null || dataSetName == null) {
      return null;
    }
    return project.getName() + USER_NAME_DELIMITER + dataSetName;
  }

  /**
   * This will return a group name for the dataset Warning if the dataset is
   * shared this will still give us the group in the owning project.
   * <p>
   * @param dataset
   * @return
   */
  public String getHdfsGroupName(Dataset dataset) {
    if (dataset == null) {
      return null;
    }
    Project owningProject = datasetController.getOwningProject(dataset);
    return owningProject.getName() + USER_NAME_DELIMITER
        + dataset.getInode().getInodePK().getName();

  }

  /**
   * Removes HDFS user and flush all cache related data in all NameNodes
   *
   * @param user
   * @throws IOException
   */
  private void removeHdfsUser(HdfsUsers user) throws IOException {
    if (user != null) {
      hdfsUsersFacade.removeHdfsUser(user);
      dfsService.getDfsOps().flushCachedUser(user.getName());
    }
  }

  /**
   * Removes HDFS group and flush all cache related data in all NameNodes
   *
   * @param group
   * @throws IOException
   */
  private void removeHdfsGroup(HdfsGroups group) throws IOException {
    if (group != null) {
      hdfsGroupsFacade.remove(group);
      dfsService.getDfsOps().flushCachedGroup(group.getName());
    }
  }
}
