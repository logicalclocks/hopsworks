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
import se.kth.bbc.security.ua.model.Yubikey;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Stateless
public class UserManager {


    @PersistenceContext(unitName = "hopsPU")
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
     * @param status
     * @param yubikey
     * @return
     */
    public String register(String fname, String lname, String email, String title, String org,
            String tel, String orcid, int uid, String password, String otpSecret,
            String question, String answer, int status, short yubikey) {

        /* assigne a username*/
        String uname = "meb" + uid;

        User user = new User();
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

    /**
     * Register a new group for user
     *
     * @param uid
     * @param gidNumber
     * @return
     */
    public boolean registerGroup(int uid, int gidNumber) {
        PeopleGroup p = new PeopleGroup();
        p.setUid(uid);
        p.setGid(gidNumber);
        em.persist(p);
        return true;
    }

    /**
     * Register an address for new users
     *
     * @param uid
     * @return
     */
    public boolean registerAddress(int uid) {
        Address p = new Address();
        p.setUid(uid);
        em.persist(p);
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

    public boolean resetPassword(int id, String pass) {
        User p = (User) em.find(User.class, id);
        p.setPassword(pass);
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

    public boolean updateStatus(int id, int stat) {
        User p = (User) em.find(User.class, id);
        p.setStatus(stat);
        em.merge(p);
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
        p.setGid(gid);
        em.merge(p);
        return true;
    }

    public List<User> findInactivateUsers() {
        Query query = em.createNativeQuery("SELECT * FROM USERS p WHERE p.active = " + PeoplAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue());
        List<User> people = query.getResultList();
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
        Query query = em.createNativeQuery("SELECT MAX(p.uid) FROM USERS p");
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
    public User getUser(String username) {
        List existing = em.createQuery(
                "SELECT p FROM User p WHERE p.email ='" + username + "'")
                .getResultList();

        if (existing.size() > 0) {
            return (User) existing.get(0);
        }
        return null;
    }

    public boolean isUsernameTaken(String username) {
        List existing = em.createQuery(
                "SELECT p FROM User p WHERE p.email ='" + username + "'")
                .getResultList();
        return (existing.size() > 0);
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
        TypedQuery<User> query = em.createNamedQuery("User.findAll", User.class);
        return query.getResultList();
    }

    public void persist(User user) {
        em.persist(user);
    }

    public void updatePeople(User user) {
        em.merge(user);
    }

    public void updateYubikey(Yubikey yubi) {
        em.merge(yubi);
    }

    public void updateAddress(Address add) {
        em.merge(add);
    }

    public boolean removeByEmail(String email) {
        boolean success = false;
        User u = findByEmail(email);
        if (u != null) {
            TypedQuery<PeopleGroup> query = em.createNamedQuery("PeopleGroup.findByUid", PeopleGroup.class);
            query.setParameter("uid", u.getUid());
            PeopleGroup p = (PeopleGroup) query.getSingleResult();

            TypedQuery<Address> ad = em.createNamedQuery("Address.findByUid", Address.class);
            ad.setParameter("uid", u.getUid());
            Address a = (Address) ad.getSingleResult();

            if (u.getYubikeyUser() == 1) {
                TypedQuery<Yubikey> yk = em.createNamedQuery("Yubikey.findByUid", Yubikey.class);
                yk.setParameter("uid", u.getUid());
                Yubikey y = (Yubikey) yk.getSingleResult();
                em.remove(y);
            }

            em.remove(p);
            em.remove(u);
            em.remove(a);
            success = true;
        }

        return success;
    }

    /**
     * Get user by status
     *
     * @param status
     * @return
     */
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

    /**
     * Get all groups based on user id
     *
     * @param uid
     * @return
     */
    public List<String> findGroups(int uid) {
        String sql = "SELECT group_name FROM BBCGroup INNER JOIN People_Group ON (People_Group.gid = BBCGroup.gid AND People_Group.uid = " + uid + " )";
        List existing = em.createNativeQuery(sql).getResultList();
        return existing;
    }

    /**
     * Remove user's group based on uid/gid
     *
     * @param uid
     * @param gid
     */
    public void removeGroup(int uid, int gid) {
        String sql = "delete from PeopleGroup p where (p.uid = " + uid + " and " + " p.gid= " + gid + ")";
        em.createQuery(sql).executeUpdate();

    }

    /**
     * Study authorization methods
     * @param name of the study
     * @return List of User objects for the study, if found
     */
    public List<User> filterUsersBasedOnStudy(String name) {

        Query query = em.createNativeQuery("SELECT * FROM USERS WHERE email NOT IN (SELECT team_member FROM StudyTeam WHERE name=?)", User.class).setParameter(1, name);
        return query.getResultList();
    }

}
