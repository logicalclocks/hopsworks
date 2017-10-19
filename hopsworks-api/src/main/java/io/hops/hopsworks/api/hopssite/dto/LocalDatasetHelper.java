package io.hops.hopsworks.api.hopssite.dto;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LocalDatasetHelper {

  public static List<LocalDatasetDTO> parse(InodeFacade inodes, List<Dataset> datasets) {
    List<LocalDatasetDTO> localDS = new ArrayList<>();
    for (Dataset d : datasets) {
      Date date = new Date(d.getInode().getModificationTime().longValue());
      long size = inodes.getSize(d.getInode());
      localDS.add(new LocalDatasetDTO(d.getInodeId(), d.getName(), d.getDescription(), d.getProject().getName(), date,
        date, size));
    }
    return localDS;
  }
}
