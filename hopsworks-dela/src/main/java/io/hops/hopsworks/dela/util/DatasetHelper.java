/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.dela.util;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;

public class DatasetHelper {

  public static String getOwningDatasetPath(Dataset dataset, InodeFacade inodeFacade, ProjectFacade projectFacade) {
    Project owningProject = getOwningProject(dataset, inodeFacade, projectFacade);
    String path = Settings.getProjectPath(owningProject.getName()) + File.separator + dataset.getName();
    return path;
  }

  public static String getDatasetPath(Project project, Dataset dataset) {
    String path = Settings.getProjectPath(project.getName()) + File.separator + dataset.getName();
    return path;
  }

  public static Project getOwningProject(Dataset ds, InodeFacade inodeFacade, ProjectFacade projectFacade) {
    // If the dataset is not a shared one, just return the project
    if (!ds.isShared()) {
      return ds.getProject();
    }

    // Get the owning project based on the dataset inode
    Inode projectInode = inodeFacade.findParent(ds.getInode());
    return projectFacade.findByName(projectInode.getInodePK().getName());
  }

}
