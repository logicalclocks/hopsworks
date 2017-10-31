package io.hops.hopsworks.api.hopssite.dto;

import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.hadoop.fs.Path;

public class LocalDatasetHelper {

  public static List<LocalDatasetDTO> parse(DatasetController datasetCtrl, DistributedFileSystemOps dfso, 
    List<Dataset> datasets) {
    List<LocalDatasetDTO> localDS = new ArrayList<>();
    for (Dataset d : datasets) {
      Date date = new Date(d.getInode().getModificationTime().longValue());
      Path path = datasetCtrl.getDatasetPath(d);
      long size;
      try {
        size = dfso.getDatasetSize(path);
      } catch (IOException ex) {
        size = -1;
      }
      localDS.add(new LocalDatasetDTO(d.getInodeId(), d.getName(), d.getDescription(), d.getProject().getName(), date,
        date, size));
    }
    return localDS;
  }
}
