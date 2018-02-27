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
        size = dfso.getLastUpdatedDatasetSize(path);
      } catch (IOException ex) {
        size = -1;
      }
      localDS.add(new LocalDatasetDTO(d.getInodeId(), d.getName(), d.getDescription(), d.getProject().getName(), date,
        date, size));
    }
    return localDS;
  }
}
