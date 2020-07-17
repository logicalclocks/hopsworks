/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.common.dataset.acl;

import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWithFacade;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsGroupsFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.FsPermissions;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetAccessPermission;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsGroups;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsUsers;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectRoleTypes;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;

import javax.ejb.AccessTimeout;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@AccessTimeout(value = 5, unit = TimeUnit.SECONDS)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class PermissionsCleaner {
  
  private final static Logger LOGGER = Logger.getLogger(PermissionsCleaner.class.getName());
  
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private DatasetSharedWithFacade datasetSharedWithFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private HdfsGroupsFacade hdfsGroupsFacade;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private InodeController inodeController;
  
  private int counter = 0;
  
  @Schedule(persistent = false,
    minute = "*/15",
    hour = "*")
  public void fixPermissions(Timer timer) {
    counter = fixPermissions(counter, System.currentTimeMillis());
    LOGGER.log(Level.INFO, "Fix permissions triggered by timer counter={0}", counter);
  }

  @Asynchronous
  public void fixPermissions() {
    fixPermissions(0, 0l);
    LOGGER.log(Level.INFO, "Manual fix permissions triggered.");
  }

  private int fixPermissions(int index, Long startTime) {
    List<Project> projectList = projectFacade.findAllOrderByCreated();
    for (int i = index; i < projectList.size(); i++) {
      Project project = projectList.get(i);
      fixPermissions(project);
      index = i;
      if (startTime > 0 && System.currentTimeMillis() - startTime > 300000) {
        break;
      }
    }
    if (index >= projectList.size()) {
      index = 0;
    }
    return index;
  }
  
  public void fixPermissions(Project project) {
    if (isUnderRemoval(project)) {
      return;
    }
    try {
      fixProject(project);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to fix dataset permissions for project {0}. Error: {1}",
        new Object[]{project.getName(), e.getMessage()});
    }
  }
  
  private void fixProject(Project project) throws IOException {
    List<Dataset> datasetList = datasetFacade.findByProject(project);
    for (Dataset dataset : datasetList) {
      fixDataset(dataset);
    }
  }
  
  private void fixDataset(Dataset dataset) throws IOException {
    String datasetGroup = hdfsUsersController.getHdfsGroupName(dataset.getProject(), dataset);
    String datasetAclGroup = hdfsUsersController.getHdfsAclGroupName(dataset.getProject(), dataset);
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfsService.getDfsOps();
      HdfsGroups hdfsDatasetGroup = getOrCreateGroup(datasetGroup, dfso);
      HdfsGroups hdfsDatasetAclGroup = getOrCreateGroup(datasetAclGroup, dfso);
      fixPermission(dataset, hdfsDatasetGroup, hdfsDatasetAclGroup, dfso);
    } finally {
      dfsService.closeDfsClient(dfso);
    }
  }
  
  private HdfsGroups getOrCreateGroup(String group, DistributedFileSystemOps dfso) throws IOException {
    HdfsGroups hdfsGroup = hdfsGroupsFacade.findByName(group);
    if (hdfsGroup == null) {
      dfso.addGroup(group);
      hdfsGroup = hdfsGroupsFacade.findByName(group);
      LOGGER.log(Level.WARNING, "Found and fixed a missing group: group={0}", group);
    }
    return hdfsGroup;
  }
  
  private void fixPermission(Dataset dataset, HdfsGroups hdfsDatasetGroup, HdfsGroups hdfsDatasetAclGroup,
    DistributedFileSystemOps dfso) throws IOException {
    if (hdfsDatasetGroup == null || hdfsDatasetAclGroup == null) {
      LOGGER.log(Level.WARNING, "Failed to get groups: {0} or {1}", new Object[]{hdfsDatasetGroup.getName(),
        hdfsDatasetAclGroup.getName()});
      return;
    }
    if (dataset.isPublicDs() && !DatasetAccessPermission.READ_ONLY.equals(dataset.getPermission())) {
      dataset.setPermission(DatasetAccessPermission.READ_ONLY);
      datasetFacade.merge(dataset);
    }
    List<ProjectTeam> datasetTeamCollection = new ArrayList<>();
    testFsPermission(dataset, dfso);
    testAndFixPermissionForAllMembers(dataset.getProject(), dfso, hdfsDatasetGroup, hdfsDatasetAclGroup,
      dataset.getInode().getHdfsUser(), dataset.getPermission());
    datasetTeamCollection.addAll(dataset.getProject().getProjectTeamCollection());
    for (DatasetSharedWith datasetSharedWith : dataset.getDatasetSharedWithCollection()) {
      if (dataset.isPublicDs() && !DatasetAccessPermission.READ_ONLY.equals(datasetSharedWith.getPermission())) {
        datasetSharedWith.setPermission(DatasetAccessPermission.READ_ONLY);
        datasetSharedWithFacade.update(datasetSharedWith);
      }
      if (datasetSharedWith.getAccepted()) {
        testAndFixPermissionForAllMembers(datasetSharedWith.getProject(), dfso, hdfsDatasetGroup, hdfsDatasetAclGroup,
          null, datasetSharedWith.getPermission());
        datasetTeamCollection.addAll(datasetSharedWith.getProject().getProjectTeamCollection());
      }
    }
    testAndRemoveUsersFromGroup(datasetTeamCollection, hdfsDatasetGroup, hdfsDatasetAclGroup,
      dataset.getInode().getHdfsUser(), dfso);
  }
  
  private void testFsPermission(Dataset dataset, DistributedFileSystemOps dfso) throws IOException {
    FsPermission fsPermission = FsPermission.createImmutable(dataset.getInode().getPermission());
    FsPermission fsPermissionReadOnly = FsPermission.createImmutable((short)00550);
    FsPermission fsPermissionReadOnlyT = FsPermission.createImmutable((short)01550);
    FsPermission fsPermissionDefault = FsPermissions.rwxrwx___;
    FsPermission fsPermissionDefaultT = FsPermissions.rwxrwx___T;
    Path path = new Path(inodeController.getPath(dataset.getInode()));
    if (dataset.isPublicDs() && !fsPermissionReadOnly.equals(fsPermission) &&
      !fsPermissionReadOnlyT.equals(fsPermission)) {
      hdfsUsersController.makeImmutable(path, dfso);
      LOGGER.log(Level.WARNING, "Found and fixed a public dataset with wrong permission. id={0}, permission={1}",
        new Object[]{dataset.getId(), fsPermission});
    }
    if (!dataset.isPublicDs() && !fsPermissionDefault.equals(fsPermission) &&
      !fsPermissionDefaultT.equals(fsPermission)) {
      hdfsUsersController.undoImmutable(path, dfso);
      LOGGER.log(Level.WARNING, "Found and fixed a dataset with wrong permission. id={0}, permission={1}",
        new Object[]{dataset.getId(), fsPermission});
    }
  }
  
  private void testAndFixPermissionForAllMembers(Project project, DistributedFileSystemOps dfso,
    HdfsGroups hdfsDatasetGroup, HdfsGroups hdfsDatasetAclGroup, HdfsUsers owner, DatasetAccessPermission permission)
    throws IOException {
    for (ProjectTeam projectTeam : project.getProjectTeamCollection()) {
      testAndFixPermission(projectTeam, dfso, hdfsDatasetGroup, hdfsDatasetAclGroup, owner, permission);
    }
  }

  private void testAndRemoveUsersFromGroup(Collection<ProjectTeam> projectTeamCollection, HdfsGroups hdfsDatasetGroup,
    HdfsGroups hdfsDatasetAclGroup, HdfsUsers owner, DistributedFileSystemOps dfso) throws IOException {
    //Remove if member is not in team collection
    for (HdfsUsers hdfsUsers : hdfsDatasetGroup.getHdfsUsersCollection()) {
      testAndRemoveMember(projectTeamCollection, hdfsDatasetGroup, hdfsUsers, owner, dfso);
    }
    for (HdfsUsers hdfsUsers : hdfsDatasetAclGroup.getHdfsUsersCollection()) {
      testAndRemoveMember(projectTeamCollection, hdfsDatasetAclGroup, hdfsUsers, owner, dfso);
    }
  }
  
  private void testAndRemoveMember(Collection<ProjectTeam> projectTeamCollection, HdfsGroups group, HdfsUsers hdfsUser,
    HdfsUsers owner, DistributedFileSystemOps dfso) throws IOException {
    if (hdfsUser == null || hdfsUser.equals(owner)) {
      return;
    }
    boolean found = false;
    for (ProjectTeam projectTeam : projectTeamCollection) {
      String hdfsUsername = hdfsUsersController.getHdfsUserName(projectTeam.getProject(), projectTeam.getUser());
      if (hdfsUser.getName().equals(hdfsUsername)) {
        found = true;
      }
    }
    if (!found) {
      removeFromGroup(hdfsUser, group, dfso);
    }
  }
  
  private void testAndFixPermission(ProjectTeam projectTeam, DistributedFileSystemOps dfso, HdfsGroups hdfsDatasetGroup,
    HdfsGroups hdfsDatasetAclGroup, HdfsUsers owner, DatasetAccessPermission permission) throws IOException {
    if (projectTeam.getUser().getUsername().equals("srvmanager")) {
      return;//Does this user need to be in groups?
    }
    String hdfsUsername = hdfsUsersController.getHdfsUserName(projectTeam.getProject(), projectTeam.getUser());
    HdfsUsers hdfsUser = hdfsUsersController.getOrCreateUser(hdfsUsername, dfso);
    if (owner != null && owner.equals(hdfsUser)) {
      return;
    }
    switch (permission) {
      case EDITABLE:
        if (!hdfsDatasetGroup.hasUser(hdfsUser)) {
          addToGroup(hdfsUser, hdfsDatasetGroup, dfso);
        }
        if (hdfsDatasetAclGroup.hasUser(hdfsUser)) {
          removeFromGroup(hdfsUser, hdfsDatasetAclGroup, dfso);
        }
        break;
      case READ_ONLY:
        if (hdfsDatasetGroup.hasUser(hdfsUser)) {
          removeFromGroup(hdfsUser, hdfsDatasetGroup, dfso);
        }
        if (!hdfsDatasetAclGroup.hasUser(hdfsUser)) {
          addToGroup(hdfsUser, hdfsDatasetAclGroup, dfso);
        }
        break;
      case EDITABLE_BY_OWNERS:
        if (AllowedRoles.DATA_OWNER.equals(projectTeam.getTeamRole())) {
          if (!hdfsDatasetGroup.hasUser(hdfsUser)) {
            addToGroup(hdfsUser, hdfsDatasetGroup, dfso);
          }
          if (hdfsDatasetAclGroup.hasUser(hdfsUser)) {
            removeFromGroup(hdfsUser, hdfsDatasetAclGroup, dfso);
          }
        } else {
          if (hdfsDatasetGroup.hasUser(hdfsUser)) {
            removeFromGroup(hdfsUser, hdfsDatasetGroup, dfso);
          }
          if (!hdfsDatasetAclGroup.hasUser(hdfsUser)) {
            addToGroup(hdfsUser, hdfsDatasetAclGroup, dfso);
          }
        }
        break;
      default:
        LOGGER.log(Level.WARNING, "Found a dataset with an unknown permission: group={0}, project={1}",
          new Object[]{hdfsDatasetGroup, projectTeam.getProject().getName()});
    }
  }
  
  private void addToGroup(HdfsUsers hdfsUser, HdfsGroups group, DistributedFileSystemOps dfso) throws IOException {
    hdfsUsersController.addToGroup(hdfsUser.getName(), group.getName(), dfso);
    LOGGER.log(Level.WARNING, "Found and fixed a user not added to a dataset group. user={0}, group={1}",
      new Object[]{hdfsUser.getName(), group.getName()});
  }
  
  private void removeFromGroup(HdfsUsers hdfsUser, HdfsGroups group, DistributedFileSystemOps dfso) throws IOException {
    hdfsUsersController.removeFromGroup(hdfsUser, group, dfso);
    LOGGER.log(Level.WARNING, "Found and fixed a user in the wrong dataset group. user={0}, group={1}",
      new Object[]{hdfsUser.getName(), group.getName()});
  }
  
  private boolean isUnderRemoval(Project project) {
    for (ProjectTeam member : project.getProjectTeamCollection()) {
      if (ProjectRoleTypes.UNDER_REMOVAL.equals(member.getTeamRole())) {
        return true;
      }
    }
    return false;
  }
}
