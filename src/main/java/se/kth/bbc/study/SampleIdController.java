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
 * @author roshan
 */
@Stateless
public class SampleIdController {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    protected EntityManager getEntityManager() {
        return em;
    }

    public SampleIdController() {

    }

    public void persistSample(SampleIds sampleID) {
        em.persist(sampleID);
    }

    public void removeSample(String sampleID, String name) {
        SampleIds smID = findByPrimaryKey(sampleID, name);
        if (smID != null) {
            em.remove(smID);
        }
    }

    public List<SampleIds> getExistingSampleIDs(String name, String user) {

        Query query = em.createNativeQuery("SELECT * FROM SampleIds WHERE study_name IN (SELECT name FROM StudyTeam WHERE team_member=? AND name=?)", SampleIds.class)
                .setParameter(1, user).setParameter(2, name);
        return query.getResultList();
    }

    public SampleIds findByPrimaryKey(String id, String name) {
        return em.find(SampleIds.class, new SampleIds(new SampleIdsPK(id, name)).getSampleIdsPK());
    }

    public boolean checkForExistingIDs(String id, String name) {
        SampleIds checkedID = findByPrimaryKey(id, name);
        if (checkedID != null) {
            return true;
        } else {
            return false;
        }
    }
    
    public List<SampleIds> findAllByStudy(String studyname){
        TypedQuery<SampleIds> query = em.createNamedQuery("SampleIds.findByStudyName", SampleIds.class).setParameter("studyName", studyname);
        return query.getResultList();
    }
}
