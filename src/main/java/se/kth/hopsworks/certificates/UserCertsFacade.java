package se.kth.hopsworks.certificates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import se.kth.hopsworks.rest.AppException;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;
import se.kth.hopsworks.util.Settings;

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
    
    public UserCerts findUserCert(int projectId, int uid){
        TypedQuery<UserCerts> query = em.createNamedQuery(
        "UserCerts.findUserProjectCert", UserCerts.class);
        query.setParameter("projectId", projectId);
        query.setParameter("userId", uid);
        try{
            UserCerts res = query.getSingleResult();
            return res;
        } catch (EntityNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<UserCerts> findAllCerts(){
        TypedQuery<UserCerts> query = em.createNamedQuery(
                "UserCerts.findAll", UserCerts.class);
        try {
            List<UserCerts> res = query.getResultList();
            return res;
        } catch (EntityNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<UserCerts> findUserCertsByProjectId(int projectId) {
        TypedQuery<UserCerts> query = em.createNamedQuery(
                "UserCerts.findByProjectId", UserCerts.class);
        query.setParameter("projectId", projectId);
        try {
            List<UserCerts> res = query.getResultList();      
            return res;
        } catch (EntityNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public List<UserCerts> findUserCertsByUid(int uid){
        TypedQuery<UserCerts> query = em.createNamedQuery(
            "UserCerts.findByUserId", UserCerts.class);
        query.setParameter("userId", uid);
        try {
            List<UserCerts> res = query.getResultList();      
            return res;
        } catch (EntityNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    } 
    
    public void persist(UserCerts uc) {
        em.persist(uc);
    }
    
    public void putUserCerts(int projectId, int userId){
        try {
            Path keystore = Paths.get(Settings.CA_DIR + "/" + projectId + "__" + userId + "_kstore.jks");
            Path truststure = Paths.get(Settings.CA_DIR + "/" + projectId + "__" + userId + "_tstore.jks");
            byte[] kStoreBytes = Files.readAllBytes(keystore);
            byte[] tStoreBytes = Files.readAllBytes(truststure);
        
            UserCerts uc = new UserCerts(projectId, userId);
            uc.setUserKey(kStoreBytes);
            uc.setUserCert(tStoreBytes);
            em.merge(uc);
            em.persist(uc);
            em.flush();
        } catch (IOException | NullPointerException e) {
              e.printStackTrace();
        }
    }
    
    public void update(UserCerts uc) {
        em.merge(uc);
    }
    
    public void remove(UserCerts uc) {
        em.remove(uc);
    }
   
    public void removeUserProjectCerts(int projectId, int uid) {
        UserCerts item = findUserCert(projectId, uid);
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