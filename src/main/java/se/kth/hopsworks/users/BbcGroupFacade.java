/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.users;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import se.kth.hopsworks.user.model.BbcGroup;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Stateless
public class BbcGroupFacade extends AbstractFacade<BbcGroup> {
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public BbcGroupFacade() {
        super(BbcGroup.class);
    }
    
    public BbcGroup findByGroupName(String name){
        return em.createNamedQuery("BbcGroup.findByGroupName", BbcGroup.class)
                         .setParameter("groupName", name).getSingleResult();    
    }
    

}
