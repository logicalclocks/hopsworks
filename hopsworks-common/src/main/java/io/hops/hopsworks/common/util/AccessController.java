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
package io.hops.hopsworks.common.util;

import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AccessController {
  private static final Logger LOGGER = Logger.getLogger(AccessController.class.getName());
  
  public boolean hasAccess(Project userProject, Dataset targetDataset) {
    if(targetDataset.getProject().equals(userProject)) {
      return true;
    }
    return targetDataset.getDatasetSharedWithCollection().stream()
      .anyMatch(sds -> sds.getProject().equals(userProject));
  }
  
  public boolean hasExtendedAccess(Users user, Project project) {
    boolean result = project.getProjectTeamCollection().stream()
      .anyMatch(pt -> pt.getUser().equals(user));
    if(result) {
      return true;
    }
    result = project.getDatasetSharedWithCollection().stream()
      .filter(DatasetSharedWith::getAccepted)
      .anyMatch(sds -> sds.getProject().getProjectTeamCollection().stream().anyMatch(t -> t.getUser().equals(user)));
    if(result) {
      return true;
    }
    return project.getDatasetCollection().stream()
      .flatMap(ds -> ds.getDatasetSharedWithCollection().stream())
      .filter(DatasetSharedWith::getAccepted)
      .anyMatch(sds -> sds.getProject().getProjectTeamCollection().stream().anyMatch(t -> t.getUser().equals(user)));
  }
  
  public Collection<ProjectTeam> getExtendedMembers(Dataset dataset) {
    List<ProjectTeam> members = dataset.getProject().getProjectTeamCollection().stream()
      // filter serving and onlinefs user
      .filter(pt -> !pt.getUser().getEmail().equals("serving@hopsworks.se")
        && !pt.getUser().getEmail().equals("onlinefs@hopsworks.ai"))
      .collect(Collectors.toCollection(ArrayList::new));
    //get members of projects that this dataset has been shared with
    List<ProjectTeam> sharedDatasets = dataset.getDatasetSharedWithCollection().stream()
      .filter(DatasetSharedWith::getAccepted)
      .flatMap((sds) -> sds.getProject().getProjectTeamCollection().stream())
      // filter serving and onlinefs user
      .filter(pt -> !pt.getUser().getEmail().equals("serving@hopsworks.se")
        && !pt.getUser().getEmail().equals("onlinefs@hopsworks.ai"))
      .collect(Collectors.toCollection(ArrayList::new));
    members.addAll(sharedDatasets);
    return members;
  }
}
