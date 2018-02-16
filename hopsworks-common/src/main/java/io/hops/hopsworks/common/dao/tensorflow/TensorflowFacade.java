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
