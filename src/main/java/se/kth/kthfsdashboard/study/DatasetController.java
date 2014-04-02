/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.study;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

/**
 *
 * @author roshan
 */
@Stateless
public class DatasetController {
     
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;
    
   
    public DatasetController() {
    }
    
    public List<Dataset> findAll() {
        TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findAll", Dataset.class);
        return query.getResultList();
    }
    
    public List<Dataset> findById() {
        TypedQuery<Dataset> query = em.createNamedQuery("Dataset.findById", Dataset.class);
        return query.getResultList();
    }
    
    
    public void persistDataset(Dataset dataset) {
        em.persist(dataset);
    }
    
    public void removeDataset(Dataset dataset) {
        em.remove(dataset);
    }
    
    
}
