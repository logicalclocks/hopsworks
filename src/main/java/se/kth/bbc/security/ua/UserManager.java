package se.kth.bbc.security.ua;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.ua.model.User;
import se.kth.bbc.security.ua.model.PeopleGroup;
import se.kth.bbc.security.ua.model.PeopleGroupPK;
import se.kth.bbc.security.ua.model.Userlogins;
import se.kth.bbc.security.ua.model.Yubikey;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Stateless
public class UserManager {

    @PersistenceContext(unitName = "hopsPU")
    private EntityManager em;

    // Strating user id from 1000 to create a POSIX compliant username: meb1000
    private final int STARTING_USER = 1000;

    // BiobankCloud prefix username prefix
    private final String USERNAME_PREFIX = "meb";

    /**
     * Register a new group for user.
     *
     * @param uid
     * @param gidNumber
     * @return
     */
    public boolean registerGroup(User uid, int gidNumber) {
        PeopleGroup p = new PeopleGroup();
        p.setPeopleGroupPK(new PeopleGroupPK(uid.getUid(), gidNumber));
        em.persist(p);
        return true;
    }

    /**
     * Register an address for new mobile users.
     *
     * @return
     */
    public boolean registerAddress(User user) {
        Address add = new Address();
        add.setUid(user);
        add.setAddress1("-");
        add.setAddress2("-");
        add.setAddress3("-");
        add.setState("-");
        add.setCity("-");
        add.setCountry("-");
        add.setPostalcode("-");
        em.persist(add);
        return true;
    }

    public boolean increaseLockNum(int id, int val) {
        User p = (User) em.find(User.class, id);
        if (p != null) {
            p.setFalseLogin(val);
            em.merge(p);
        }
        return true;
    }

    public boolean setOnline(int id, int val) {
        User p = (User) em.find(User.class, id);
        p.setIsonline(val);
        em.merge(p);
        return true;
    }

    public boolean resetLock(int id) {
        User p = (User) em.find(User.class, id);
        p.setFalseLogin(0);
        em.merge(p);
        return true;
    }

    public boolean deactivateUser(int id) {
        User p = (User) em.find(User.class, id);
        if (p != null) {
            p.setStatus(PeoplAccountStatus.ACCOUNT_BLOCKED.getValue());
            em.merge(p);
        }
        return true;
    }

    public boolean closeUserAccount(int id, String note) {
        User p = (User) em.find(User.class, id);
        if (p != null) {
            p.setStatus(PeoplAccountStatus.ACCOUNT_DEACTIVATED.getValue());
            p.setNotes(note);
            em.merge(p);
        }
        return true;
    }
        
    public boolean resetPassword(User p, String pass) {
        p.setPassword(pass);
        p.setPasswordChanged(new Timestamp(new Date().getTime()));
        em.merge(p);
        return true;
    }

    public boolean resetSecQuestion(int id, String question, String ans) {
        User p = (User) em.find(User.class, id);
        p.setSecurityQuestion(question);
        p.setSecurityAnswer(ans);
        em.merge(p);
        return true;
    }

    public boolean updateStatus(User id, int stat) {
        id.setStatus(stat);
        em.merge(id);
        return true;
    }

    public boolean updateSecret(int id, String sec) {
        User p = (User) em.find(User.class, id);
        p.setSecret(sec);
        em.merge(p);
        return true;
    }

    public boolean updateGroup(int uid, int gid) {
        TypedQuery<PeopleGroup> query = em.createNamedQuery("PeopleGroup.findByUid", PeopleGroup.class);
        query.setParameter("uid", uid);
        PeopleGroup p = (PeopleGroup) query.getSingleResult();
        p.setPeopleGroupPK(new PeopleGroupPK(uid, gid));
        em.merge(p);
        return true;
    }

    public List<User> findInactivateUsers() {
        Query query = em.createNativeQuery("SELECT * FROM users p WHERE p.active = " + PeoplAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue());
        List<User> people = query.getResultList();
        return people;
    }

    public boolean registerYubikey(User uid) {
        Yubikey yk = new Yubikey();
        yk.setUid(uid);
        em.persist(yk);
        return true;
    }

    /**
     * Find a user through email.
     *
     * @param username
     * @return
     */
    public User getUser(String username) {
        TypedQuery<User> query = em.createNamedQuery("User.findByEmail", User.class);
        query.setParameter("email", username);
        List<User> list = query.getResultList();

        if (list == null || list.isEmpty()) {
            return null;
        }

        return list.get(0);
    }

    public boolean isUsernameTaken(String username) {

        return (getUser(username) != null);
    }

    public boolean findYubikeyUsersByStatus(int status) {
        List existing = em.createQuery(
                "SELECT p FROM User p WHERE p.status ='" + PeoplAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue() + "' AND p.yubikey_user = " + status)
                .getResultList();
        return (existing.size() > 0);
    }

    public Yubikey findYubikey(int uid) {
        TypedQuery<Yubikey> query = em.createNamedQuery("Yubikey.findByUid", Yubikey.class);
        query.setParameter("uid", uid);
        return query.getSingleResult();

    }

    public List<User> findAllUsers() {
        //TypedQuery<User> query = em.createNamedQuery("User.findAll", User.class);
        List<User> query = em.createQuery(
                "SELECT p FROM User p WHERE p.status !='" + PeoplAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue() + "' AND p.status!='" + PeoplAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue() + "' AND p.status!='" + PeoplAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue() + "'")
                .getResultList();

        return query;
    }

    public List<User> findAllByStatus(int status) {
        TypedQuery<User> query = em.createNamedQuery("User.findByStatus", User.class);
        query.setParameter("status", status);
        return query.getResultList();
    }

    public User findByEmail(String email) {
        TypedQuery<User> query = em.createNamedQuery("User.findByEmail", User.class);
        query.setParameter("email", email);
        return query.getSingleResult();
    }

    public Address findAddress(int uid) {
        TypedQuery<Address> query = em.createNamedQuery("Address.findByUid", Address.class);
        query.setParameter("uid", uid);
        return query.getSingleResult();
    }

    public List<String> findGroups(int uid) {
        String sql = "SELECT group_name FROM bbc_group INNER JOIN people_group ON (people_group.gid = bbc_group.gid AND people_group.uid = " + uid + " )";
        List existing = em.createNativeQuery(sql).getResultList();
        return existing;
    }

    /**
     * Remove user's group based on uid/gid.
     *
     * @param uid
     * @param gid
     */
    public void removeGroup(User user, int gid) {
        PeopleGroup p = em.find(PeopleGroup.class, new PeopleGroup(new PeopleGroupPK(user.getUid(), gid)).getPeopleGroupPK());
        em.remove(p);
    }

    /**
     * Study authorization methods
     *
     * @param name of the study
     * @return List of User objects for the study, if found
     */
    public List<User> filterUsersBasedOnStudy(String name) {

        Query query = em.createNativeQuery("SELECT * FROM users WHERE email NOT IN (SELECT team_member FROM study_team WHERE name=?)", User.class).setParameter(1, name);
        return query.getResultList();
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
    public boolean registerAddress(User uid, String address1, String address2, String address3, String city, String state, String country, String postalcode) {

        Address add = new Address();
        add.setUid(uid);
        add.setAddress1(checkDefaultValue(address1));
        add.setAddress2(checkDefaultValue(address2));
        add.setAddress3(checkDefaultValue(address3));
        add.setState(checkDefaultValue(state));
        add.setCity(checkDefaultValue(city));
        add.setCountry(checkDefaultValue(country));
        add.setPostalcode(checkDefaultValue(postalcode));
        em.persist(add);

        return true;
    }
    
    /**
     * Check the value and if empty set as '-'.
     * @param var
     * @return 
     */    
    public String checkDefaultValue(String var){
        if(var!=null && !var.isEmpty())
            return var;
        return "-";
    }

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
     * @param status
     * @param yubikey
     * @return
     */
    public User register(String fname, String lname, String email, String title, String org,
            String tel, String orcid, int uid, String password, String otpSecret,
            String question, String answer, int status, short yubikey) {

        // assigne a username
        String uname = USERNAME_PREFIX + uid;

        User user = new User();
        user.setUsername(uname);
        user.setPassword(password);
        user.setSecret(otpSecret);
        user.setEmail(email);
        user.setFname(fname);
        user.setLname(lname);
        user.setHomeOrg(checkDefaultValue(org));
        user.setMobile(checkDefaultValue(tel));
        user.setUid(uid);
        user.setOrcid(checkDefaultValue(orcid));
        user.setTitle(checkDefaultValue(title));
        user.setActivated(new Timestamp(new Date().getTime()));
        user.setStatus(status);
        /*
         * offline: -1
         * online:   1  
         */
        user.setIsonline(-1);
        user.setSecurityQuestion(question);
        user.setSecurityAnswer(answer);
        user.setPasswordChanged(new Timestamp(new Date().getTime()));
        user.setYubikeyUser(yubikey);

        em.persist(user);
        return user;
    }

    /**
     * Return the max uid in the table.
     *
     * @return
     */
    public int lastUserID() {
        Query query = em.createNativeQuery("SELECT MAX(p.uid) FROM users p");
        Object obj = query.getSingleResult();

        if (obj == null) {
            return STARTING_USER;
        }
        return (Integer) obj;
    }

    public void persist(User user) {
        em.persist(user);
    }

    public boolean updatePeople(User user) {
        em.merge(user);
        return true;
    }

    public void updateYubikey(Yubikey yubi) {
        em.merge(yubi);
    }

    public boolean updateAddress(Address add) {
        em.merge(add);
        return true;
    }

    /**
     * Remove a user by email address.
     *
     * @param email
     * @return
     */
    public boolean removeByEmail(String email) {
        boolean success = false;
        User u = findByEmail(email);
        if (u != null) {
            TypedQuery<PeopleGroup> query = em.createNamedQuery("PeopleGroup.findByUid", PeopleGroup.class);
            query.setParameter("uid", u.getUid());
            PeopleGroup p = (PeopleGroup) query.getSingleResult();

            Address a = u.getAddress();
         
                em.remove(a);
                em.remove(p);
                
                if (u.getYubikeyUser() == 1) {
                    em.remove(u.getYubikey());
                }
              
                em.remove(u);
            success = true;
        }

        return success;
    }

    public void registerLoginInfo(User uid, String action, String ip, String browser) {

        Userlogins l = new Userlogins();
        l.setUid(uid.getUid());
        l.setBrowser(browser);
        l.setIp(ip);

        l.setAction(action);
        l.setLoginDate(new Timestamp(new Date().getTime()));

        em.persist(l);

    }

    public Userlogins getLastUserLoing(int uid) {
        String sql = "SELECT * FROM userlogins  WHERE uid=" + uid + " ORDER BY login_date DESC LIMIT 1 OFFSET 2";
        Query query = em.createNativeQuery(sql, Userlogins.class);
        query.setMaxResults(2);

        List<Userlogins> ul = query.getResultList();

        if (ul.isEmpty()) {
            return null;
        }

        if (ul.size() == 1) {
            return ul.get(0);
        }

        return ul.get(1);

    }

}
