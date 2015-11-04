/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.hops.hdfs;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

/**
 *
 * @author kerkinos
 */
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

    public HdfsLeDescriptors findById() {
        try {
            return em.createNamedQuery("HdfsLeDescriptors.findEndpoint", HdfsLeDescriptors.class).getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
    
}
