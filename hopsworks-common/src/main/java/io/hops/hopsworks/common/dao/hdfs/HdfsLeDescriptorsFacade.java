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

package io.hops.hopsworks.common.dao.hdfs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import org.apache.hadoop.conf.Configuration;
import io.hops.hopsworks.common.dao.AbstractFacade;

@Stateless
public class HdfsLeDescriptorsFacade extends AbstractFacade<HdfsLeDescriptors> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @EJB
  private DistributedFsService dfsService;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public HdfsLeDescriptorsFacade() {
    super(HdfsLeDescriptors.class);
  }

  /**
   * HdfsLeDescriptors.hostname returns the hostname + port for the Leader NN
   * (e.g., "127.0.0.1:8020")
   *
   * @return
   */
  public HdfsLeDescriptors findEndpoint() {
    try {
//            return em.createNamedQuery("HdfsLeDescriptors.findEndpoint", HdfsLeDescriptors.class).getSingleResult();
      List<HdfsLeDescriptors> res = em.createNamedQuery(
              "HdfsLeDescriptors.findEndpoint", HdfsLeDescriptors.class).
              getResultList();
      if (res.isEmpty()) {
        return null;
      } else {
        return res.get(0);
      }
    } catch (NoResultException e) {
      return null;
    }
  }

  /**
   *
   * @return "ip:port" for the first namenode found in the table.
   */
  public String getSingleEndpoint() {
    HdfsLeDescriptors hdfs = findEndpoint();
    if (hdfs == null) {
      return "";
    }
    return hdfs.getHostname();
  }

  /**
   * Get the currently active NameNode. Loops the NameNodes provided by the
   * hdfs_le_descriptors table.
   *
   * @return
   */
  public HdfsLeDescriptors getActiveNN() {
    try {
      List<HdfsLeDescriptors> res = em.createNamedQuery(
          "HdfsLeDescriptors.findEndpoint", HdfsLeDescriptors.class).
          getResultList();
    
      if (res.isEmpty()) {
        return null;
      } else {
        //Try to open a connection to NN
        Configuration conf = new Configuration();
        for (HdfsLeDescriptors hdfsLeDesc : res) {
          try {
            DistributedFileSystemOps dfso = dfsService.getDfsOps(
                new URI("hdfs://" + hdfsLeDesc.getHostname()));
            if (null != dfso) {
              return hdfsLeDesc;
            }
          } catch (URISyntaxException ex) {
            Logger.getLogger(HdfsLeDescriptorsFacade.class.getName()).
                log(Level.SEVERE, null, ex);
          }
        }
      }
    } catch (NoResultException e) {
      return null;
    }
    return null;
  }

}
