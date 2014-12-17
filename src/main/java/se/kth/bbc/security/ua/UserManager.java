package se.kth.bbc.security.ua;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.ua.model.People;
import se.kth.bbc.security.ua.model.PeopleGroup;
import se.kth.bbc.security.ua.model.Yubikey;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Stateless
public class UserManager {

    private static final Logger logger = Logger.getLogger(UserManager.class.getName());

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    /**
     * Register a new user and assign default group.
     *
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
     * @param question
     * @param answer
     * @param yubikey
     * @return
     */
    public String register(String fname, String lname, String email, String title, String org,
                String tel, String orcid, int uid, String password, String otpSecret, 
                String question, String answer, int status, short yubikey) {

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
        user.setStatus(status);
        /*
         * offline: -1
         * online:   1  
         */
        user.setIsonline(-1);
        user.setSecurityQuestion(question);
        user.setSecurityAnswer(answer);
        user.setYubikeyUser(yubikey);
        em.persist(user);
        return uname;
    }

    public boolean registerGroup(int uid, int gidNumber) {
        PeopleGroup p = new PeopleGroup();
        p.setUid(uid);
        p.setGid(gidNumber);
        em.persist(p);
        return true;
    }
    
    public boolean registerAddress(int uid) {
        Address p = new Address();
        p.setUid(uid);
        em.persist(p);
        return true;
    }

    public boolean increaseLockNum(int id, int val) {
        People p = (People) em.find(People.class, id);
        if (p != null) {
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
        if (p != null) {
            p.setStatus(AccountStatusIF.ACCOUNT_BLOCKED);
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

    public boolean resetSecQuestion(int id, String  question, String ans) {
        People p = (People) em.find(People.class, id);
        p.setSecurityQuestion(question);
        p.setSecurityAnswer(question);
        em.merge(p);
        return true;
    }

    
    public boolean updateStatus(int id, int stat) {
        People p = (People) em.find(People.class, id);
        p.setStatus(stat);
        em.merge(p);
        return true;
    }

    public boolean updateSecret(int id, String sec) {
        People p = (People) em.find(People.class, id);
        p.setSecret(sec);
        em.merge(p);
        return true;
    }

    
    public boolean updateGroup(int uid, int gid) {
        TypedQuery<PeopleGroup> query = em.createNamedQuery("PeopleGroup.findByUid", PeopleGroup.class);
        query.setParameter("uid", uid);
        PeopleGroup p = (PeopleGroup) query.getSingleResult();
        p.setGid(gid);
        em.merge(p);
        return true;
    }

    public String getPeopleGroupName(int uid) {
        TypedQuery<PeopleGroup> query = em.createNamedQuery("PeopleGroup.findByUid", PeopleGroup.class);
        query.setParameter("uid", uid);
        PeopleGroup p = (PeopleGroup) query.getSingleResult();
        return new BBCGroups().getGroupName(p.getGid());
    }

      
    public List<People> findInactivateUsers() {
        Query query = em.createNativeQuery("SELECT * FROM People p WHERE p.active = "+ AccountStatusIF.MOBILE_ACCOUNT_INACTIVE );
        List<People> people = query.getResultList();
        return people;
    }

    public boolean registerYubikey(int uid) {
        Yubikey yk = new Yubikey();
        yk.setUid(uid);
        em.persist(yk);
        return true;
    }
    /**
     * Register Yubikey user's delivery address.
     *
     * @param uid
     * @param address1
     * @param address2
     * @param address3
     * @param city
     * @param state
     * @param country
     * @param postalcode
     * @return
     */
    public boolean registerAddress(int uid, String address1, String address2, String address3, String city, String state, String country, String postalcode) {

        Address add = new Address();
        add.setUid(uid);
        add.setAddress1(address1);
        add.setAddress2(address2);
        add.setAddress3(address3);
        add.setState(state);
        add.setCity(city);
        add.setCountry(country);
        add.setPostalcode(postalcode);
        em.persist(add);
        return true;
    }

    /**
     * Return the max uid in the table.
     *
     * @return
     */
    public int lastUserID() {
        Query query = em.createNativeQuery("SELECT MAX(p.uid) FROM People p");
        Object obj = query.getSingleResult();

        // For the first user in the table as uid
        int uid = 10000;

        if (obj == null) {
            return uid;
        }
        return (Integer) obj;
    }

    /**
     * Find a user through email.
     *
     * @param username
     * @return
     */
    public People getUser(String username) {
        List existing = em.createQuery(
                "SELECT p FROM People p WHERE p.email ='" + username + "'")
                .getResultList();

        if (existing.size() > 0) {
            return (People) existing.get(0);
        }
        return null;
    }

    public boolean isUsernameTaken(String username) {
        List existing = em.createQuery(
                "SELECT p FROM People p WHERE p.email ='" + username + "'")
                .getResultList();
        return (existing.size() > 0);
    }
    
    public boolean findYubikeyUsersByStatus(int status) {
        List existing = em.createQuery(
       "SELECT p FROM People p WHERE p.status ='" + AccountStatusIF.MOBILE_ACCOUNT_INACTIVE + "' AND p.yubikey_user = "+ status)
                .getResultList();
      return (existing.size() > 0);
    }
    
    
     public Yubikey findYubikey(int uid) {
       TypedQuery<Yubikey> query = em.createNamedQuery("Yubikey.findByUid", Yubikey.class);
       query.setParameter("uid", uid);
       return query.getSingleResult();
         
     }
    
    
    public List<People> findAllByName() {
        TypedQuery<People> query = em.createNamedQuery("People.findAllByName", People.class);
        return query.getResultList();
    }

    public List<People> findAllUsers() {
        TypedQuery<People> query = em.createNamedQuery("People.findAll", People.class);
        return query.getResultList();
    }

    public void persist(People user) {
        em.persist(user);
    }

    public void updatePeople(People user) {
        em.merge(user);
    }
    
    public void updateYubikey(Yubikey yubi) {
        em.merge(yubi);
    }
    
    public void updateAddress(Address add) {
        em.merge(add);
    }

    public void removeByEmail(String email) {
        People user = findByEmail(email);
        if (user != null) {
        TypedQuery<PeopleGroup> query = em.createNamedQuery("PeopleGroup.findByUid", PeopleGroup.class);
        query.setParameter("uid", user.getUid());
        PeopleGroup p = (PeopleGroup) query.getSingleResult();
        em.remove(p);
        em.remove(user);
       }
    }
 
    /**
     * Get user by status
     * @param status
     * @return 
     */
    public List<People> findAllByStatus(int status) {
        TypedQuery<People> query = em.createNamedQuery("People.findByStatus", People.class);
        query.setParameter("status", status);
        return query.getResultList();
    }
    

    public People findByEmail(String email) {
        TypedQuery<People> query = em.createNamedQuery("People.findByEmail", People.class);
        query.setParameter("email", email);
        return query.getSingleResult();
    }

     public Address findAddress(int uid) {
        TypedQuery<Address> query = em.createNamedQuery("Address.findByUid", Address.class);
        query.setParameter("uid", uid);
        return query.getSingleResult();
    }
   
}