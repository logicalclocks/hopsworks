package se.kth.bbc.security.ua;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import se.kth.bbc.security.ua.model.People;
import se.kth.bbc.security.ua.model.PeopleGroup;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */

@Stateless
public class UserManager {
    
   private static final Logger logger = Logger.getLogger(UserManager.class.getName());

   @PersistenceContext(unitName = "kthfsPU")
   private EntityManager em;

   public List checkUser(String username) {
    List existing = em.createQuery(
         "SELECT p FROM People p WHERE p.username ='" +username+"'")
         .getResultList();
    return existing;
   }
 
     public People getUser(String username) {
    List existing = em.createQuery(
         "SELECT p FROM People p WHERE p.username ='"+username+"'")
         .getResultList();
        if (existing.size()>0)
            return (People) existing.get(0);
        return null;   
    }
  
     /**
      * Return the max uid in the table.
      * @return 
      */
    public int lastUserID() {
       Query query=  em.createNativeQuery("SELECT MAX(p.uid) FROM People p");
       Object obj = query.getSingleResult();
       
       // For the first user in the table as uid
       int uid = 10000;
       
       if (obj==null){
        return uid;
       }
       return (Integer) obj;
    }
    
    /**
     * Register the new users and assign them to a group
     * @param fname
     * @param lname
     * @param email
     * @param title
     * @param org
     * @param tel
     * @param orcid
     * @param uid
     * @param password
     * @param otpSecret
     * @return 
     */
    public String register(String fname, String lname,String email, String title, String org, String tel, 
            String orcid , int uid, String password, String otpSecret) {
        
       /* assigne a username*/
       String uname = "meb" + uid;

       People user = new People();
       user.setUsername(uname);
       user.setPassword(password);
       user.setSecret(otpSecret);
       user.setEmail(email);
       user.setFname(fname);
       user.setLname(lname);
       user.setHomeOrg(org);
       user.setMobile(tel);
       user.setUid(uid);
       user.setOrcid(orcid);
       user.setTitle(title);
       user.setActivated(new Timestamp(new Date().getTime()));
       // This needs to be set to -1 in future
       user.setActive(1);
       user.setIsonline(-1);
       
       em.persist(user);
       return uname; 
    }

    public boolean  registerGroup(int uid, int gidNumber) {
        PeopleGroup p = new PeopleGroup(uid, gidNumber);
        em.persist(p);
        return true;
    }
    
    public boolean increaseLockNum(int id, int val) {
        People p = (People) em.find(People.class, id);
        if (p!=null){
            p.setFalseLogin(val);
            em.merge(p);
        }
        return true;
    }
    
    public boolean setOnline(int id, int val) {
        People p = (People) em.find(People.class, id);
        p.setIsonline(val);
        em.merge(p);
        return true;
    }
    
   public boolean resetLock(int id) {
        People p = (People) em.find(People.class, id);
        p.setFalseLogin(0);
        em.merge(p);
        return true;
    }
   
   
   public boolean deactivateUser(int id) {
        People p = (People) em.find(People.class, id);
        if(p!=null){
        p.setActive(-1);
        em.merge(p);
        }
        return true;
    }
   
    public boolean resetPassword(int id, String pass) {
        People p = (People) em.find(People.class, id);
        p.setPassword(pass);
        em.merge(p);
        return true;
    }
}