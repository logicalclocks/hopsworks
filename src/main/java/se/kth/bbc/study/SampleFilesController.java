/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 *
 * @author stig
 */
@Stateless
public class SampleFilesController {
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    protected EntityManager getEntityManager() {
        return em;
    }

    public SampleFilesController() {

    }

    public List<SampleFiles> findAllById(String id){
        TypedQuery<SampleFiles> query = em.createNamedQuery("SampleFiles.findById", SampleFiles.class).setParameter("id", id);
        return query.getResultList();
    }
    
    
    public void persistSampleFiles(SampleFiles sampleFiles){
        em.persist(sampleFiles);
    }

    public SampleFiles findByPrimaryKey(String id, String filename) {
        return em.find(SampleFiles.class, new SampleFiles(new SampleFilesPK(id, filename)).getSampleFilesPK());
    }

    public boolean checkForExistingSampleFiles(String id, String filename) {
        SampleFiles checkedFile = findByPrimaryKey(id, filename);
        if (checkedFile != null) {
            return true;
        } else {
            return false;
        }
    }
    
    public void update(String id, String filename){
        SampleFiles sf = findByPrimaryKey(id, filename);
        if(sf != null) {
            sf.setStatus(SampleFileStatus.AVAILABLE.getFileStatus());
            em.merge(sf);
        }
    }
    
}
