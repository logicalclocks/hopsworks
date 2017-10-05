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
