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
import io.hops.hopsworks.common.dao.hdfsUser.HdfsGroupsFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.persistence.entity.dataset.PermissionTransition;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetAccessPermission;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsGroups;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsUsers;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.security.UserAlreadyInGroupException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.AclEntry;
import org.apache.hadoop.fs.permission.AclEntryScope;
import org.apache.hadoop.fs.permission.AclEntryType;
import org.apache.hadoop.fs.permission.FsAction;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class HdfsUsersController {

  public static final String USER_NAME_DELIMITER = "__";
  
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private HdfsGroupsFacade hdfsGroupsFacade;
  @EJB
  private DistributedFsService dfsService;

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
  public void addProjectFolderOwner(Project project, DistributedFileSystemOps dfso) throws IOException {
    String owner = getHdfsUserName(project, project.getOwner());
    Path location = new Path(Utils.getProjectPath(project.getName()));
    //FsPermission(FsAction u, FsAction g, FsAction o) 555
    //We prohibit a user from creating top-level datasets bypassing Hopsworks UI (i.e. from as Spark app)
    //r_x for others to allow sharing
    dfso.setOwner(location, owner, project.getName());
    dfso.setPermission(location, FsPermissions.r_xr_xr_x);

    // Add project owner to the project group
    //TODO: do we need to add the owner to group if permission is r_xr_xr_x?
    HdfsGroups projectGroup = hdfsGroupsFacade.findByName(project.getName());
    if (projectGroup == null) {
      throw new IllegalArgumentException("No group found for project in HDFS.");
    }
    addToGroup(owner, projectGroup.getName(), dfso);
  }

  /**
   * Create a new group in HDFS with the name project.name__datasetName with write access and an Acl entry
   * project.name__datasetName__read with read access. Also sets permissions and adds all members of the project to the
   * appropriate groups.
   * <p>
   * @param owner
   * @param project
   * @param dataset
   * @param dfso
   * @throws java.io.IOException
   */
  public void createDatasetGroupsAndSetPermissions(Users owner, Project project, Dataset dataset, Path dsPath,
    DistributedFileSystemOps dfso) throws IOException {
    if (owner == null || project == null || project.getProjectTeamCollection() == null || dataset == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    String datasetGroup = getHdfsGroupName(project, dataset);
    String datasetAclGroup = getHdfsAclGroupName(project, dataset);
    String dsOwner = getHdfsUserName(project, owner);
    dfso.setOwner(dsPath, dsOwner, datasetGroup);
    dfso.addGroup(datasetAclGroup);
    dfso.setPermission(dsPath, getDatasetAcl(datasetAclGroup));
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(datasetGroup);
    if (hdfsGroup == null) {
      throw new IllegalArgumentException("Could not create dataset group in HDFS.");
    }

    // During the project creation we cannot rely on the owner being in the projectTeamCollection
    // when this method is invoked, hence we explicitly add them to the group.
    String hdfsUsername = getHdfsUserName(project, owner);
    addToGroup(hdfsUsername, hdfsGroup.getName(), dfso);

    //add every member to the new ds group
    addMembersToGroups(datasetGroup, datasetAclGroup, dfso, project.getProjectTeamCollection(),
      dataset.getPermission());
  }

  private List<AclEntry> getDatasetAcl(String aclGroup) {
    List<AclEntry> aclEntries = new ArrayList<>();
    AclEntry aclEntryUser = new AclEntry.Builder()
      .setType(AclEntryType.USER)
      .setScope(AclEntryScope.ACCESS)
      .setPermission(FsAction.ALL)
      .build();
    aclEntries.add(aclEntryUser);
    AclEntry aclEntryGroup = new AclEntry.Builder()
      .setType(AclEntryType.GROUP)
      .setScope(AclEntryScope.ACCESS)
      .setPermission(FsAction.ALL)
      .build();
    aclEntries.add(aclEntryGroup);
    AclEntry aclEntryDatasetGroup = new AclEntry.Builder()
      .setType(AclEntryType.GROUP)
      .setName(aclGroup)
      .setScope(AclEntryScope.ACCESS)
      .setPermission(FsAction.READ_EXECUTE)
      .build();
    aclEntries.add(aclEntryDatasetGroup);
    AclEntry aclEntryOther = new AclEntry.Builder()
      .setType(AclEntryType.OTHER)
      .setScope(AclEntryScope.ACCESS)
      .setPermission(FsAction.NONE)
      .build();
    aclEntries.add(aclEntryOther);
    AclEntry aclEntryDefault = new AclEntry.Builder()
      .setType(AclEntryType.GROUP)
      .setName(aclGroup)
      .setScope(AclEntryScope.DEFAULT)
      .setPermission(FsAction.READ_EXECUTE)
      .build();
    aclEntries.add(aclEntryDefault);
    return aclEntries;
  }
  
  /**
   * Deletes the project group and all associated groups from HDFS
   * <p>
   * @param hdfsDsGroups
   * @throws java.io.IOException
   */
  public void deleteGroups(List<HdfsGroups> hdfsDsGroups) throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      for (HdfsGroups hdfsDsGroup : hdfsDsGroups) {
        dfso.removeGroup(hdfsDsGroup.getName());
      }
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }

  /**
   * Deletes all users associated with this project from HDFS
   * <p>
   * @param users
   * @throws java.io.IOException
   */
  public void deleteUsers(Collection<HdfsUsers> users) throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      for (HdfsUsers user : users) {
        dfso.removeUser(user.getName());
      }
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }

  /**
   * Deletes the dataset group and datasetAcl group from HDFS
   * <p>
   * @param dataset
   * @throws java.io.IOException
   */
  public void deleteDatasetGroups(Project project, Dataset dataset) throws IOException {
    if (dataset == null) {
      throw new IllegalArgumentException("One or more arguments are null.");
    }
    String datasetGroup = getHdfsGroupName(dataset);
    String datasetAclGroup = getHdfsAclGroupName(project, dataset);
    
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      dfso.removeGroup(datasetGroup);
      dfso.removeGroup(datasetAclGroup);
    } finally {
      dfsService.closeDfsClient(dfso);
    }
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
    return project.getName() + USER_NAME_DELIMITER + ds.getInode().getInodePK().getName();
  }

  /**
   *
   * @param project
   * @param ds
   * @return
   */
  public String getHdfsAclGroupName(Project project, Dataset ds) {
    return getHdfsGroupName(project, ds) + USER_NAME_DELIMITER + "read";
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
    return dataset.getProject().getName() + USER_NAME_DELIMITER + dataset.getInode().getInodePK().getName();
  }


  public void makeImmutable(Path path, DistributedFileSystemOps dfso) throws IOException {
    List<AclEntry> aclEntries = new ArrayList<>();
    AclEntry aclEntryUser = new AclEntry.Builder()
      .setType(AclEntryType.USER)
      .setScope(AclEntryScope.ACCESS)
      .setPermission(FsAction.READ_EXECUTE)
      .build();
    aclEntries.add(aclEntryUser);
    AclEntry aclEntryGroup = new AclEntry.Builder()
      .setType(AclEntryType.GROUP)
      .setScope(AclEntryScope.ACCESS)
      .setPermission(FsAction.READ_EXECUTE)
      .build();
    aclEntries.add(aclEntryGroup);
    AclEntry aclEntryOther = new AclEntry.Builder()
      .setType(AclEntryType.OTHER)
      .setScope(AclEntryScope.ACCESS)
      .setPermission(FsAction.NONE)
      .build();
    aclEntries.add(aclEntryOther);
    dfso.setPermission(path, aclEntries);
  }

  public void undoImmutable(Path path, DistributedFileSystemOps dfso) throws IOException {
    List<AclEntry> aclEntries = new ArrayList<>();
    AclEntry aclEntryUser = new AclEntry.Builder()
      .setType(AclEntryType.USER)
      .setScope(AclEntryScope.ACCESS)
      .setPermission(FsAction.ALL)
      .build();
    aclEntries.add(aclEntryUser);
    AclEntry aclEntryGroup = new AclEntry.Builder()
      .setType(AclEntryType.GROUP)
      .setScope(AclEntryScope.ACCESS)
      .setPermission(FsAction.ALL)
      .build();
    aclEntries.add(aclEntryGroup);
    AclEntry aclEntryOther = new AclEntry.Builder()
      .setType(AclEntryType.OTHER)
      .setScope(AclEntryScope.ACCESS)
      .setPermission(FsAction.NONE)
      .build();
    aclEntries.add(aclEntryOther);
    dfso.setPermission(path, aclEntries);
  }

  /**
   * Add member to all project group and dataset groups.
   * @param teamMember
   * @throws IOException
   */
  public void addNewProjectMember(ProjectTeam teamMember) throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      addNewProjectMember(teamMember, dfso);
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }

  /**
   * Add member to all dataset groups in project
   * @param teamMember
   * @throws IOException
   */
  public void addNewMember(ProjectTeam teamMember) throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      addNewMember(teamMember, dfso);
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }

  /**
   * Add member to the dataset group
   * @param dataset
   * @param permission
   * @param teamMember
   * @throws IOException
   */
  public void addNewMember(Dataset dataset, DatasetAccessPermission permission, ProjectTeam teamMember)
    throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      addNewMember(dataset, permission, teamMember, dfso);
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }

  /**
   * Add member to project group and all dataset groups
   * @param teamMember
   * @param dfso
   * @throws IOException
   */
  public void addNewProjectMember(ProjectTeam teamMember, DistributedFileSystemOps dfso) throws IOException {
    createUserIfNotExist(teamMember, dfso);
    //add only data_owners to project group
    if (AllowedRoles.DATA_OWNER.equals(teamMember.getTeamRole())) {
      addToGroup(teamMember.getProject().getName(), teamMember, dfso);
    }
    addNewMember(teamMember, dfso);
  }

  /**
   * Add member to all dataset groups in project.
   * @param teamMember
   * @param dfso
   * @throws IOException
   */
  public void addNewMember(ProjectTeam teamMember, DistributedFileSystemOps dfso) throws IOException {
    createUserIfNotExist(teamMember, dfso);
    for (Dataset ds : teamMember.getProject().getDatasetCollection()) {
      addNewMember(ds, ds.getPermission(), teamMember, dfso);
    }
    for (DatasetSharedWith datasetSharedWith : teamMember.getProject().getDatasetSharedWithCollection()) {
      addNewMember(datasetSharedWith.getDataset(), datasetSharedWith.getPermission(), teamMember, dfso);
    }
  }

  /**
   * Will only remove the member from all dataset groups
   * @param teamMember
   * @throws IOException
   */
  public void removeMember(ProjectTeam teamMember) throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      removeMember(teamMember, dfso);
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }

  /**
   * Will remove the user from hdfs groups
   * @param teamMember
   * @throws IOException
   */
  public void removeUserName(ProjectTeam teamMember) throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      removeUserName(teamMember, dfso);
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }

  /**
   * Remove member from dataset group
   * @param dataset
   * @param permission
   * @param teamMember
   * @throws IOException
   */
  public void removeMember(Dataset dataset, DatasetAccessPermission permission, ProjectTeam teamMember)
    throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      removeMember(dataset, permission, teamMember, dfso);
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }

  /**
   * Will remove the user from hdfs groups
   * @param teamMember
   * @param dfso
   * @throws IOException
   */
  public void removeUserName(ProjectTeam teamMember, DistributedFileSystemOps dfso) throws IOException {
    String hdfsUserName = getHdfsUserName(teamMember.getProject(), teamMember.getUser());
    dfso.removeUser(hdfsUserName);
  }

  /**
   * Will only remove the member from all dataset groups
   * @param teamMember
   * @param dfso
   * @throws IOException
   */
  public void removeMember(ProjectTeam teamMember, DistributedFileSystemOps dfso) throws IOException {
    for (Dataset ds : teamMember.getProject().getDatasetCollection()) {
      removeMember(ds, ds.getPermission(), teamMember, dfso);
    }
    for (DatasetSharedWith datasetSharedWith : teamMember.getProject().getDatasetSharedWithCollection()) {
      removeMember(datasetSharedWith.getDataset(), datasetSharedWith.getPermission(), teamMember, dfso);
    }
  }

  /**
   * Move member to the appropriate dataset group.
   * The appropriate dataset group is decided by the dataset permission.
   * @param teamMember
   * @throws IOException
   */
  public void changeMemberRole(ProjectTeam teamMember) throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      changeMemberRole(teamMember, dfso);
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }

  /**
   * Move member to the appropriate dataset group.
   * The appropriate dataset group is decided by the permission.
   * @param dataset
   * @param permission
   * @param teamMember
   * @throws IOException
   */
  public void changeMemberRole(Dataset dataset, DatasetAccessPermission permission, ProjectTeam teamMember)
    throws IOException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      changeMemberRole(dataset, permission, teamMember, dfso);
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }

  /**
   * Move data owners to project group and users to the appropriate dataset group.
   * The appropriate dataset group is decided by the dataset permission.
   * @param teamMember
   * @param dfso
   * @throws IOException
   */
  public void changeMemberRole(ProjectTeam teamMember, DistributedFileSystemOps dfso) throws IOException {
    //add only data_owners to project group
    if (AllowedRoles.DATA_OWNER.equals(teamMember.getTeamRole())) {
      addToGroup(teamMember.getProject().getName(), teamMember, dfso);
    } else {
      removeFromGroup(teamMember.getProject().getName(), teamMember, dfso);
    }
    for (Dataset ds : teamMember.getProject().getDatasetCollection()) {
      changeMemberRole(ds, ds.getPermission(), teamMember, dfso);
    }
    for (DatasetSharedWith datasetSharedWith : teamMember.getProject().getDatasetSharedWithCollection()) {
      changeMemberRole(datasetSharedWith.getDataset(), datasetSharedWith.getPermission(), teamMember, dfso);
    }
  }

  /**
   * Add a member to the appropriate dataset group.
   * The appropriate dataset group is decided by the permission.
   * @param dataset
   * @param permission
   * @param teamMember
   * @param dfso
   * @throws IOException
   */
  public void addNewMember(Dataset dataset, DatasetAccessPermission permission, ProjectTeam teamMember,
    DistributedFileSystemOps dfso) throws IOException {
    String datasetGroup = getHdfsGroupName(dataset.getProject(), dataset);
    String datasetAclGroup = getHdfsAclGroupName(dataset.getProject(), dataset);
    String hdfsUserName = getHdfsUserName(teamMember.getProject(), teamMember.getUser());
    addNewMember(datasetGroup, datasetAclGroup, hdfsUserName, permission, teamMember, dfso);
  }

  private void addNewMember(String datasetGroup, String datasetAclGroup, String hdfsUserName,
    DatasetAccessPermission permission, ProjectTeam teamMember, DistributedFileSystemOps dfso) throws IOException {
    switch (permission) {
      case EDITABLE:
        addToGroup(hdfsUserName, datasetGroup, dfso);
        break;
      case EDITABLE_BY_OWNERS:
        if (AllowedRoles.DATA_OWNER.equals(teamMember.getTeamRole())) {
          addToGroup(hdfsUserName, datasetGroup, dfso);
        } else {
          addToGroup(hdfsUserName, datasetAclGroup, dfso);
        }
        break;
      case READ_ONLY:
        addToGroup(hdfsUserName, datasetAclGroup, dfso);
        break;
      default:
        throw new IOException("Unknown permission for dataset: " + datasetGroup);
    }
  }

  /**
   * Add all members to every dataset group in the project.
   * @param datasetGroup
   * @param datasetAclGroup
   * @param dfso
   * @param projectTeamMembers
   * @param permission
   * @throws IOException
   */
  public void addMembersToGroups(String datasetGroup, String datasetAclGroup, DistributedFileSystemOps dfso,
    Collection<ProjectTeam> projectTeamMembers, DatasetAccessPermission permission) throws IOException {
    if (projectTeamMembers == null || projectTeamMembers.isEmpty()) {
      return;
    }
    for(ProjectTeam teamMember : projectTeamMembers) {
      String hdfsUserName = getHdfsUserName(teamMember.getProject(), teamMember.getUser());
      addNewMember(datasetGroup, datasetAclGroup, hdfsUserName, permission, teamMember, dfso);
    }
  }

  /**
   * Remove a member from the appropriate dataset group.
   * The appropriate dataset group is decided by the permission.
   * @param dataset
   * @param permission
   * @param teamMember
   * @param dfso
   * @throws IOException
   */
  public void removeMember(Dataset dataset, DatasetAccessPermission permission, ProjectTeam teamMember,
    DistributedFileSystemOps dfso) throws IOException {
    String datasetGroup = getHdfsGroupName(dataset.getProject(), dataset);
    String datasetAclGroup = getHdfsAclGroupName(dataset.getProject(), dataset);
    String hdfsUserName = getHdfsUserName(teamMember.getProject(), teamMember.getUser());
    switch (permission) {
      case EDITABLE:
        removeFromGroup(hdfsUserName, datasetGroup, dfso);
        break;
      case EDITABLE_BY_OWNERS:
        if (AllowedRoles.DATA_OWNER.equals(teamMember.getTeamRole())) {
          removeFromGroup(hdfsUserName, datasetGroup, dfso);
        } else {
          removeFromGroup(hdfsUserName, datasetAclGroup, dfso);
        }
        break;
      case READ_ONLY:
        removeFromGroup(hdfsUserName, datasetAclGroup, dfso);
        break;
      default:
        throw new IOException("Unknown permission for dataset: " + dataset.getName());
    }
  }

  /**
   * Move member to the appropriate dataset group
   * The appropriate dataset group is decided by the permission.
   * @param dataset
   * @param permission
   * @param teamMember
   * @param dfso
   * @throws IOException
   */
  public void changeMemberRole(Dataset dataset, DatasetAccessPermission permission, ProjectTeam teamMember,
    DistributedFileSystemOps dfso) throws IOException {
    String datasetGroup = getHdfsGroupName(dataset.getProject(), dataset);
    String datasetAclGroup = getHdfsAclGroupName(dataset.getProject(), dataset);
    String hdfsUserName = getHdfsUserName(teamMember.getProject(), teamMember.getUser());
    switch (permission) {
      case EDITABLE:
      case READ_ONLY:
        //noop
        break;
      case EDITABLE_BY_OWNERS:
        if (AllowedRoles.DATA_OWNER.equals(teamMember.getTeamRole())) {
          removeFromGroup(hdfsUserName, datasetAclGroup, dfso);
          addToGroup(hdfsUserName, datasetGroup, dfso);
        } else {
          removeFromGroup(hdfsUserName, datasetGroup, dfso);
          addToGroup(hdfsUserName, datasetAclGroup, dfso);
        }
        break;
      default:
        throw new IOException("Unknown permission for dataset: " + dataset.getName());
    }
  }

  /**
   * Move all members to the appropriate dataset group after permission change.
   * @param ds
   * @param targetProject
   * @param permissionTransition
   * @throws IOException
   */
  public void changePermission(Dataset ds, Project targetProject, PermissionTransition permissionTransition)
    throws IOException {
    DistributedFileSystemOps dfso = dfsService.getDfsOps();
    try {
      dfso = dfsService.getDfsOps();
      changePermission(ds, targetProject, permissionTransition, dfso);
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }

  public void changePermission(Dataset ds, Project targetProject, PermissionTransition permissionTransition,
    DistributedFileSystemOps dfso) throws IOException {
    for (ProjectTeam teamMember : targetProject.getProjectTeamCollection()) {
      changePermission(ds, teamMember, permissionTransition, dfso);
    }
  }

  /**
   * Move member to the appropriate dataset group after permission change.
   * @param dataset
   * @param teamMember
   * @param permissionTransition
   * @throws IOException
   */
  public void changePermission(Dataset dataset, ProjectTeam teamMember, PermissionTransition permissionTransition)
    throws IOException {
    DistributedFileSystemOps dfso = dfsService.getDfsOps();
    try {
      dfso = dfsService.getDfsOps();
      changePermission(dataset, teamMember, permissionTransition, dfso);
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }

  /**
   * Move member to the appropriate dataset group after permission change.
   * @param dataset
   * @param teamMember
   * @param permissionTransition
   * @param dfso
   * @throws IOException
   */
  public void changePermission(Dataset dataset, ProjectTeam teamMember, PermissionTransition permissionTransition,
    DistributedFileSystemOps dfso) throws IOException {
    String hdfsUserName = getHdfsUserName(teamMember.getProject(), teamMember.getUser());
    String datasetGroup = getHdfsGroupName(dataset.getProject(), dataset);
    String datasetAclGroup = getHdfsAclGroupName(dataset.getProject(), dataset);
    switch (permissionTransition) {
      case EDITABLE_TO_EDITABLE:
      case READ_ONLY_TO_READ_ONLY:
      case EDITABLE_BY_OWNERS_TO_EDITABLE_BY_OWNERS:
        //noop
        break;
      case EDITABLE_TO_EDITABLE_BY_OWNERS:
        changePermissionEditableToEditableByOwners(datasetGroup, datasetAclGroup, hdfsUserName,
          teamMember.getTeamRole(), dfso);
        break;
      case EDITABLE_TO_READ_ONLY:
        changePermissionEditableToReadOnly(datasetGroup, datasetAclGroup, hdfsUserName, dfso);
        break;
      case EDITABLE_BY_OWNERS_TO_EDITABLE:
        changePermissionEditableByOwnersToEditable(datasetGroup, datasetAclGroup, hdfsUserName,
          teamMember.getTeamRole(), dfso);
        break;
      case EDITABLE_BY_OWNERS_TO_READ_ONLY:
        changePermissionEditableByOwnersToReadOnly(datasetGroup, datasetAclGroup, hdfsUserName,
          teamMember.getTeamRole(), dfso);
        break;
      case READ_ONLY_TO_EDITABLE:
        changePermissionReadOnlyToEditable(datasetGroup, datasetAclGroup, hdfsUserName, dfso);
        break;
      case READ_ONLY_TO_EDITABLE_BY_OWNERS:
        changePermissionReadOnlyToEditableByOwners(datasetGroup, datasetAclGroup, hdfsUserName,
          teamMember.getTeamRole(), dfso);
        break;
      default:
        throw new IllegalArgumentException("Illegal permission transition.");
    }
  }

  private void changePermissionReadOnlyToEditableByOwners(String datasetGroup, String datasetAclGroup,
    String hdfsUserName, String role, DistributedFileSystemOps dfso) throws IOException {
    if (AllowedRoles.DATA_OWNER.equals(role)) {
      removeFromGroup(hdfsUserName, datasetAclGroup, dfso);
      addToGroup(hdfsUserName, datasetGroup, dfso);
    }
  }

  private void changePermissionReadOnlyToEditable(String datasetGroup, String datasetAclGroup, String hdfsUserName,
    DistributedFileSystemOps dfso) throws IOException {
    removeFromGroup(hdfsUserName, datasetAclGroup, dfso);
    addToGroup(hdfsUserName, datasetGroup, dfso);
  }

  private void changePermissionEditableByOwnersToReadOnly(String datasetGroup, String datasetAclGroup,
    String hdfsUserName, String role, DistributedFileSystemOps dfso) throws IOException {
    if (AllowedRoles.DATA_OWNER.equals(role)) {
      removeFromGroup(hdfsUserName, datasetGroup, dfso);
      addToGroup(hdfsUserName, datasetAclGroup, dfso);
    }
  }

  private void changePermissionEditableByOwnersToEditable(String datasetGroup, String datasetAclGroup,
    String hdfsUserName, String role, DistributedFileSystemOps dfso) throws IOException {
    if (AllowedRoles.DATA_SCIENTIST.equals(role)) {
      removeFromGroup(hdfsUserName, datasetAclGroup, dfso);
      addToGroup(hdfsUserName, datasetGroup, dfso);
    }
  }

  private void changePermissionEditableToReadOnly(String datasetGroup, String datasetAclGroup, String hdfsUserName,
    DistributedFileSystemOps dfso) throws IOException {
    removeFromGroup(hdfsUserName, datasetGroup, dfso);
    addToGroup(hdfsUserName, datasetAclGroup, dfso);
  }

  private void changePermissionEditableToEditableByOwners(String datasetGroup, String datasetAclGroup,
    String hdfsUserName, String role, DistributedFileSystemOps dfso) throws IOException {
    if (AllowedRoles.DATA_SCIENTIST.equals(role)) {
      removeFromGroup(hdfsUserName, datasetGroup, dfso);
      addToGroup(hdfsUserName, datasetAclGroup, dfso);
    }
  }

  private void addToGroup(String group, ProjectTeam teamMember, DistributedFileSystemOps dfso) throws IOException {
    String hdfsUserName = getHdfsUserName(teamMember.getProject(), teamMember.getUser());
    addToGroup(hdfsUserName, group, dfso);
  }

  private void removeFromGroup(String group, ProjectTeam teamMember, DistributedFileSystemOps dfso) throws IOException {
    String hdfsUserName = getHdfsUserName(teamMember.getProject(), teamMember.getUser());
    removeFromGroup(hdfsUserName, group, dfso);
  }

  private void removeFromGroup(String hdfsUserName, String group, DistributedFileSystemOps dfso) throws IOException {
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(group);
    HdfsUsers hdfsUser = hdfsUsersFacade.findByName(hdfsUserName);
    removeFromGroup(hdfsUser, hdfsGroup, dfso);
    //user not in group
  }

  public void removeFromGroup(HdfsUsers hdfsUser, HdfsGroups hdfsGroup, DistributedFileSystemOps dfso)
    throws IOException {
    if (hdfsGroup.hasUser(hdfsUser)) {
      dfso.removeUserFromGroup(hdfsUser.getName(), hdfsGroup.getName());
    }
  }

  public void addToGroup(String hdfsUserName, String group, DistributedFileSystemOps dfso) throws IOException {
    try {
      dfso.addUserToGroup(hdfsUserName, group);
    } catch (UserAlreadyInGroupException e) {
      //user already in group
    }
  }

  private void createUserIfNotExist(ProjectTeam member, DistributedFileSystemOps dfso) throws IOException {
    String hdfsUsername = getHdfsUserName(member.getProject(), member.getUser());
    HdfsUsers memberHdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
    if (memberHdfsUser == null) {
      dfso.addUser(hdfsUsername);
    }
  }

  public HdfsUsers getOrCreateUser(String hdfsUsername, DistributedFileSystemOps dfso) throws IOException {
    HdfsUsers memberHdfsUser = hdfsUsersFacade.findByName(hdfsUsername);
    if (memberHdfsUser == null) {
      dfso.addUser(hdfsUsername);
    }
    return hdfsUsersFacade.findByName(hdfsUsername);
  }

}
