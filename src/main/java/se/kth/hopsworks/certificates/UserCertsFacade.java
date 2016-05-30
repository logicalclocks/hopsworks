package se.kth.hopsworks.certificates;

import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import se.kth.hopsworks.rest.AppException;

/**
 *
 * @author paul
 */

@Stateless
public class UserCertsFacade {
    
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;
    
    protected EntityManager getEntityManager() {
        return em;
    }
      
    public UserCertsFacade() throws AppException, Exception {}
    
    public UserCerts findUserCert(String projectName, int uid){
        TypedQuery<UserCerts> query = em.createNamedQuery(
        "UserCerts.findUserProjectCert", UserCerts.class);
        query.setParameter("projectname", projectName);
        query.setParameter("userId", uid);
        try{
            UserCerts res = query.getSingleResult();
            return res;
        } catch (EntityNotFoundException e) {
            Logger.getLogger(UserCertsFacade.class.getName()).log(Level.SEVERE, null, e);
        }
        return new UserCerts();
    }
    
    public List<UserCerts> findAllCerts(){
        TypedQuery<UserCerts> query = em.createNamedQuery(
                "UserCerts.findAll", UserCerts.class);
        try {
            List<UserCerts> res = query.getResultList();
            return res;
        } catch (EntityNotFoundException e) {
            Logger.getLogger(UserCertsFacade.class.getName()).log(Level.SEVERE, null, e);
        }
        return new ArrayList<>();
    }
    
    public List<UserCerts> findUserCertsByProjectId(int projectId) {
        TypedQuery<UserCerts> query = em.createNamedQuery(
                "UserCerts.findByProjectId", UserCerts.class);
        query.setParameter("projectId", projectId);
        try {
            List<UserCerts> res = query.getResultList();      
            return res;
        } catch (EntityNotFoundException e) {
            Logger.getLogger(UserCertsFacade.class.getName()).log(Level.SEVERE, null, e);
        }
        return new ArrayList<>();
    }
    
    public List<UserCerts> findUserCertsByUid(int uid){
        TypedQuery<UserCerts> query = em.createNamedQuery(
            "UserCerts.findByUserId", UserCerts.class);
        query.setParameter("userId", uid);
        try {
            List<UserCerts> res = query.getResultList();      
            return res;
        } catch (EntityNotFoundException e) {
            Logger.getLogger(UserCertsFacade.class.getName()).log(Level.SEVERE, null, e);
        }
        return new ArrayList<>();
    } 
    
    public void persist(UserCerts uc) {
        em.persist(uc);
    }
    
    public void putUserCerts(String projectName, int userId){
      try(FileInputStream kfin = new FileInputStream(new File("/tmp/tempstores/" + projectName + "__" + userId + "__kstore.jks"));
          FileInputStream tfin = new FileInputStream(new File("/tmp/tempstores/" + projectName + "__" + userId + "__tstore.jks"))){
       
            byte[] kStoreBlob = ByteStreams.toByteArray(kfin);
            byte[] tStoreBlob = ByteStreams.toByteArray(tfin);
            
            UserCerts uc = new UserCerts(projectName, userId);
            uc.setUserKey(kStoreBlob);
            uc.setUserCert(tStoreBlob);
            em.merge(uc);
            em.persist(uc);
            em.flush();    
      
        } catch (FileNotFoundException e) {
            Logger.getLogger(UserCertsFacade.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException ex) {
            Logger.getLogger(UserCertsFacade.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void update(UserCerts uc) {
        em.merge(uc);
    }
    
    public void remove(UserCerts uc) {
        em.remove(uc);
    }
   
    public void removeUserProjectCerts(String projectName, int uid) {
        UserCerts item = findUserCert(projectName, uid);
           if(item != null){
                UserCerts tmp = em.merge(item);
                remove(tmp);
           }          
    } 
    
    public void removeAllCertsOfAUser(int uid){
        List<UserCerts> items = findUserCertsByUid(uid);
        if(items != null){
            for(UserCerts uc : items){
                UserCerts tmp = em.merge(uc);
                remove(tmp);
            }
        }
    }
    
    public void removeAllCertsOfAProject(int projectId){
        List<UserCerts> items = findUserCertsByProjectId(projectId);
        if(items != null){
            for(UserCerts uc : items){
                UserCerts tmp = em.merge(uc);
                remove(tmp);
            }
        }
    }    
}