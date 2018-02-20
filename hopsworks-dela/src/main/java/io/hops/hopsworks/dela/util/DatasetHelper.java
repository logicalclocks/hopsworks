/*
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
 *
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

  public static String getOwningDatasetPath(Dataset dataset, InodeFacade inodeFacade, ProjectFacade projectFacade,
      Settings settings) {
    Project owningProject = getOwningProject(dataset, inodeFacade, projectFacade);
    return settings.getProjectPath(owningProject.getName()) + File.separator + dataset.getName();
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
