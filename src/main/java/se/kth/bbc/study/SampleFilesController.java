package se.kth.bbc.study;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 * Controls access to datatable SampleFiles.
 * @author stig
 */
@Stateless
public class SampleFilesController {
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    protected EntityManager getEntityManager() {
        return em;
    }

    public SampleFilesController() {}

    /**
     * Find all SampleFiles having id <i>id</i>.
     */
    public List<SampleFiles> findAllById(String id){
        TypedQuery<SampleFiles> query = em.createNamedQuery("SampleFiles.findById", SampleFiles.class).setParameter("id", id);
        return query.getResultList();
    }
    
    /**
     * Find all SampleFiles in database having id <i>id</i> and type <i>type</i>.
     */
    public List<SampleFiles> findAllByIdType(String id, String type){
        Query query = em.createNativeQuery("SELECT * FROM SampleFiles WHERE id LIKE ? AND file_type LIKE ?",SampleFiles.class);
        query.setParameter(1, id);
        query.setParameter(2, type);
        return query.getResultList();
    }
    
    /**
     * Find all file types featuring in SampleFiles for the sample <i>sampleId</i>.
     */
    public List<String> findAllExtensionsForSample(String sampleId){
        Query query = em.createNativeQuery("SELECT DISTINCT file_type FROM SampleFiles WHERE id LIKE ?");
        query.setParameter(1, sampleId);
        List<Object> res = query.getResultList();
        List<String> stringRes = new ArrayList<>(res.size());
        for(Object o: res){
            stringRes.add((String)o);
        }
        return stringRes;
    }
    
    
    public void persistSampleFiles(SampleFiles sampleFiles){
        em.persist(sampleFiles);
    }

    public SampleFiles findByPrimaryKey(String id, String filename) {
        return em.find(SampleFiles.class, new SampleFiles(new SampleFilesPK(id, filename)).getSampleFilesPK());
    }

    /**
     * Check if the SampleFile with id <i>id</i> and filename <i>filename</i> exists.
     */
    public boolean checkForExistingSampleFiles(String id, String filename) {
        SampleFiles checkedFile = findByPrimaryKey(id, filename);
        return checkedFile != null;
    }
    
    public void update(String id, String filename){
        SampleFiles sf = findByPrimaryKey(id, filename);
        if(sf != null) {
            sf.setStatus(SampleFileStatus.AVAILABLE.getFileStatus());
            em.merge(sf);
        }
    }
    
    public void deleteFile(String id, String filename){
        SampleFiles fetchedFile = findByPrimaryKey(id, filename);
        if(fetchedFile != null)
            em.remove(fetchedFile);
    }
    
    /**
     * Remove all SampleFiles in sample <i>sampleId</i> in study <i>study_name</i> with type <i>file_type</i>.
     */
    public void deleteFileTypeRecords(String sampleId, String study_name, String file_type){     
        //TODO: improve: one remove query, not one search and then remove every single item.
        Query query = em.createNativeQuery("SELECT * FROM SampleFiles WHERE id IN (SELECT id FROM SampleIds WHERE id=? AND study_name=?) AND file_type=?",SampleFiles.class).setParameter(1, sampleId)
                .setParameter(2, study_name).setParameter(3, file_type);
        List<SampleFiles> res = query.getResultList();
        if(res.iterator().hasNext()){
            SampleFiles sf = res.iterator().next();
            if(sf != null) em.remove(sf);
        }
    }
    
    /**
     * Get the number of files in study <i>study_name</i> in sample <i>id</i>.
     */
    public long checkFileCount(String id, String study_name){        
        Query query = em.createNativeQuery("SELECT COUNT(*) FROM SampleFiles WHERE id IN (SELECT id FROM SampleIds WHERE id=? AND study_name=?)",SampleFiles.class).setParameter(1, id)
                .setParameter(2, study_name);
        return (Long)query.getSingleResult();
    }
    
}
