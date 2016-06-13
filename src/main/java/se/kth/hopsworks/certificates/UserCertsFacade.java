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
    
    public UserCerts findUserCert(String projectName, String username){
        TypedQuery<UserCerts> query = em.createNamedQuery(
        "UserCerts.findUserProjectCert", UserCerts.class);
        query.setParameter("projectname", projectName);
        query.setParameter("username", username);
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
    
    public List<UserCerts> findUserCertsByProjectId(String projectname) {
        TypedQuery<UserCerts> query = em.createNamedQuery(
                "UserCerts.findByProjectname", UserCerts.class);
        query.setParameter("projectname", projectname);
        try {
            List<UserCerts> res = query.getResultList();      
            return res;
        } catch (EntityNotFoundException e) {
            Logger.getLogger(UserCertsFacade.class.getName()).log(Level.SEVERE, null, e);
        }
        return new ArrayList<>();
    }
    
    public List<UserCerts> findUserCertsByUid(String username){
        TypedQuery<UserCerts> query = em.createNamedQuery(
            "UserCerts.findByUsername", UserCerts.class);
        query.setParameter("username", username);
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
    
    public void putUserCerts(String projectname, String username){
      try(FileInputStream kfin = new FileInputStream(new File("/tmp/tempstores/" + projectname + "__" + username + "__kstore.jks"));
          FileInputStream tfin = new FileInputStream(new File("/tmp/tempstores/" + projectname + "__" + username + "__tstore.jks"))){
       
            byte[] kStoreBlob = ByteStreams.toByteArray(kfin);
            byte[] tStoreBlob = ByteStreams.toByteArray(tfin);
            
            UserCerts uc = new UserCerts(projectname, username);
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
   
    public void removeUserProjectCerts(String projectname, String username) {
        UserCerts item = findUserCert(projectname, username);
           if(item != null){
                UserCerts tmp = em.merge(item);
                remove(tmp);
           }          
    } 
    
    public void removeAllCertsOfAUser(String username){
        List<UserCerts> items = findUserCertsByUid(username);
        if(items != null){
            for(UserCerts uc : items){
                UserCerts tmp = em.merge(uc);
                remove(tmp);
            }
        }
    }
    
    public void removeAllCertsOfAProject(String projectname){
        List<UserCerts> items = findUserCertsByProjectId(projectname);
        if(items != null){
            for(UserCerts uc : items){
                UserCerts tmp = em.merge(uc);
                remove(tmp);
            }
        }
    }    
}