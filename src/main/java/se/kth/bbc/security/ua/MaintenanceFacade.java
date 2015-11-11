/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;

import se.kth.kthfsdashboard.user.AbstractFacade;

@Stateless
public class MaintenanceFacade extends AbstractFacade<Maintenance> {
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public MaintenanceFacade() {
        super(Maintenance.class);
    }

    public Maintenance findMaintenanceStatus() {
                try {
                        return em.createNamedQuery("Maintenance.findAll", Maintenance.class).getSingleResult();
                    } catch (NoResultException e) {
                        return null;
                    }
            }
}
