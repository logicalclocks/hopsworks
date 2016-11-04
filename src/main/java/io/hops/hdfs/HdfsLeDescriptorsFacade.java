/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.hdfs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import se.kth.kthfsdashboard.user.AbstractFacade;

@Stateless
public class HdfsLeDescriptorsFacade extends AbstractFacade<HdfsLeDescriptors> {
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public HdfsLeDescriptorsFacade() {
        super(HdfsLeDescriptors.class);
    }

    /**
     * HdfsLeDescriptors.hostname returns the hostname + port for the Leader NN (e.g., "127.0.0.1:8020")
     * @return 
     */
    public HdfsLeDescriptors findEndpoint() {
        try {
//            return em.createNamedQuery("HdfsLeDescriptors.findEndpoint", HdfsLeDescriptors.class).getSingleResult();
            List<HdfsLeDescriptors> res = em.createNamedQuery("HdfsLeDescriptors.findEndpoint", HdfsLeDescriptors.class).getResultList();
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
            FileSystem.get(new URI("hdfs://" + hdfsLeDesc.getHostname()),
                    conf);
            return hdfsLeDesc;
          } catch (URISyntaxException ex) {
            Logger.getLogger(HdfsLeDescriptorsFacade.class.getName()).
                    log(Level.SEVERE, null, ex);
          } catch (IOException ex) {
            //NN was not active, try the next one
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
