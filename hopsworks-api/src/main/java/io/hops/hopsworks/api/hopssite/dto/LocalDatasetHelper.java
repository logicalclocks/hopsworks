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
