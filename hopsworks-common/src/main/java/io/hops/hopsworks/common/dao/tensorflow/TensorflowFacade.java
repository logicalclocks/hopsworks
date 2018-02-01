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

package io.hops.hopsworks.common.dao.tensorflow;

import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;

@Stateless
public class TensorflowFacade {

  private final static Logger LOGGER = Logger.getLogger(TensorflowFacade.class.
      getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  Settings settings;
  @EJB
  private DistributedFsService dfs;

  protected EntityManager getEntityManager() {
    return em;
  }

  public TensorflowFacade() throws Exception {
  }

  public String getTensorboardURI(String appId, String projectName) {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      String tensorboardFile = File.separator + Settings.DIR_ROOT
          + File.separator + projectName + File.separator + Settings.PROJECT_STAGING_DIR + File.separator
          + ".tensorboard." + appId;
      try {
        FSDataInputStream file = dfso.open(tensorboardFile);
        String uri = IOUtils.toString(file);
        return uri;
      } catch (IOException ex) {
        LOGGER.log(Level.WARNING, "error while trying to read tensorboard file: " + tensorboardFile, ex);
        return null;
      }

    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }
}
