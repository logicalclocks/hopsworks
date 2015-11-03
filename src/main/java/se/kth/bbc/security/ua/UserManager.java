package se.kth.bbc.security.ua;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import se.kth.bbc.project.Project;
import se.kth.bbc.security.ua.model.Address;
import se.kth.bbc.security.ua.model.Organization;
import se.kth.bbc.security.ua.model.PeopleGroup;
import se.kth.bbc.security.ua.model.PeopleGroupPK;
import se.kth.bbc.security.ua.model.Yubikey;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@Stateless
public class UserManager {

  @PersistenceContext(unitName = "kthfsPU")
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
  public boolean registerGroup(Users uid, int gidNumber) {
    PeopleGroup p = new PeopleGroup();
    p.setPeopleGroupPK(new PeopleGroupPK(uid.getUid(), gidNumber));
    em.persist(p);
    return true;
  }

  /**
   * Register an address for new mobile users.
   *
   * @param user
   * @return
   */
  public boolean registerAddress(Users user) {
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
    Users p = (Users) em.find(Users.class, id);
    if (p != null) {
      p.setFalseLogin(val);
      em.merge(p);
    }
    return true;
  }

  public boolean setOnline(int id, int val) {
    Users p = (Users) em.find(Users.class, id);
    p.setIsonline(val);
    em.merge(p);
    return true;
  }

  public boolean resetLock(int id) {
    Users p = (Users) em.find(Users.class, id);
    p.setFalseLogin(0);
    em.merge(p);
    return true;
  }

  public boolean changeAccountStatus(int id, String note, int status) {
    Users p = (Users) em.find(Users.class, id);
    if (p != null) {
      p.setNotes(note);
      p.setStatus(status);
      em.merge(p);
    }
    return true;
  }

  public boolean resetKey(int id) {
    Users p = (Users) em.find(Users.class, id);
    p.setValidationKey(SecurityUtils.getRandomString(64));
    em.merge(p);
    return true;
  }

  public boolean resetPassword(Users p, String pass) {
    p.setPassword(pass);
    p.setPasswordChanged(new Timestamp(new Date().getTime()));
    em.merge(p);
    return true;
  }

  public boolean resetSecQuestion(int id, SecurityQuestion question, String ans) {
    Users p = (Users) em.find(Users.class, id);
    p.setSecurityQuestion(question);
    p.setSecurityAnswer(ans);
    em.merge(p);
    return true;
  }

  public boolean updateStatus(Users id, int stat) {
    id.setStatus(stat);
    em.merge(id);
    return true;
  }

  public boolean updateSecret(int id, String sec) {
    Users p = (Users) em.find(Users.class, id);
    p.setSecret(sec);
    em.merge(p);
    return true;
  }

  public boolean updateGroup(int uid, int gid) {
    TypedQuery<PeopleGroup> query = em.createNamedQuery("PeopleGroup.findByUid",
            PeopleGroup.class);
    query.setParameter("uid", uid);
    PeopleGroup p = (PeopleGroup) query.getSingleResult();
    p.setPeopleGroupPK(new PeopleGroupPK(uid, gid));
    em.merge(p);
    return true;
  }

  public List<Users> findInactivateUsers() {
    Query query = em.createNativeQuery("SELECT * FROM hopsworks.users p WHERE p.active = "
            + PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue());
    List<Users> people = query.getResultList();
    return people;
  }

  public boolean registerYubikey(Users uid) {
    Yubikey yk = new Yubikey();
    yk.setUid(uid);
    yk.setStatus(PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue());
    em.persist(yk);
    return true;
  }

  /**
   * Find a user through email.
   *
   * @param username
   * @return
   */
  public Users getUserByEmail(String username) {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByEmail", Users.class);
    query.setParameter("email", username);
    List<Users> list = query.getResultList();

    if (list == null || list.isEmpty()) {
      return null;
    }

    return list.get(0);
  }

  public Users getUserByUsernmae(String username) {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByUsername",
            Users.class);
    query.setParameter("username", username);
    List<Users> list = query.getResultList();

    if (list == null || list.isEmpty()) {
      return null;
    }

    return list.get(0);
  }

  public boolean isUsernameTaken(String username) {

    return (getUserByEmail(username) != null);
  }

  public boolean findYubikeyUsersByStatus(int status) {
    List existing = em.createQuery(
            "SELECT p FROM hopsworks.users p WHERE p.status ='"
            + PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue()
            + "' AND p.mode = " + status)
            .getResultList();
    return (existing.size() > 0);
  }

  public Yubikey findYubikey(int uid) {
    TypedQuery<Yubikey> query = em.createNamedQuery("Yubikey.findByUid",
            Yubikey.class);
    query.setParameter("uid", uid);
    return query.getSingleResult();

  }

  public List<Users> findAllUsers() {
    List<Users> query = em.createQuery(
            "SELECT p FROM Users p WHERE p.status !='"
            + PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue()
            + "' AND p.status!='"
            + PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue()
            + "' AND p.status!='" + PeopleAccountStatus.SPAM_ACCOUNT.getValue()
            + "' AND p.status!='" + PeopleAccountStatus.ACCOUNT_VERIFICATION.
            getValue()
            + "'")
            .getResultList();

    return query;
  }

  public List<Users> findAllByStatus(int status) {
    TypedQuery<Users> query = em.
            createNamedQuery("Users.findByStatus", Users.class);
    query.setParameter("status", status);
    return query.getResultList();
  }

  public List<Users> findAllSPAMAccounts() {
    List<Users> query = em.createQuery(
            "SELECT p FROM Users p WHERE (p.status ='"
            + PeopleAccountStatus.SPAM_ACCOUNT.getValue()
            + "' OR p.status ='" + PeopleAccountStatus.ACCOUNT_VERIFICATION.
            getValue()
            + "')")
            .getResultList();

    return query;
  }

  public Users findByEmail(String email) {
    TypedQuery<Users> query = em.createNamedQuery("Users.findByEmail", Users.class);
    query.setParameter("email", email);
    return query.getSingleResult();
  }

  public Address findAddress(int uid) {
    TypedQuery<Address> query = em.createNamedQuery("Address.findByUid",
            Address.class);
    query.setParameter("uid", uid);
    return query.getSingleResult();
  }

  public List<String> findGroups(int uid) {
    String sql
            = "SELECT group_name FROM hopsworks.bbc_group INNER JOIN hopsworks.people_group ON (hopsworks.people_group.gid = hopsworks.bbc_group.gid AND hopsworks.people_group.uid = "
            + uid + " )";
    List existing = em.createNativeQuery(sql).getResultList();
    return existing;
  }

  /**
   * Remove user's group based on uid/gid.
   *
   * @param user
   * @param gid
   */
  public void removeGroup(Users user, int gid) {
    PeopleGroup p = em.find(PeopleGroup.class, new PeopleGroup(
            new PeopleGroupPK(user.getUid(), gid)).getPeopleGroupPK());
    em.remove(p);
  }

  /**
   * Study authorization methods
   *
   * @param name of the study
   * @return List of User objects for the study, if found
   */
  public List<Users> filterUsersBasedOnStudy(String name) {

    Query query = em.createNativeQuery(
            "SELECT * FROM hopsworks.users WHERE email NOT IN (SELECT team_member FROM hopsworks.project_team WHERE name=?)",
            Users.class).setParameter(1, name);
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
  public boolean registerAddress(Users uid, String address1, String address2,
          String address3, String city, String state, String country,
          String postalcode) {

    Address add = new Address();
    add.setUid(null);
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
   *
   * @param var
   * @return
   */
  public String checkDefaultValue(String var) {
    if (var != null && !var.isEmpty()) {
      return var;
    }
    return "-";
  }

  /**
   * Register a new user and assign default group.
   *
   * @param fname
   * @param lname
   * @param email
   * @param title
   * @param tel
   * @param orcid
   * @param uid
   * @param password
   * @param otpSecret
   * @param question
   * @param answer
   * @param status
   * @param yubikey
   * @param validationKey
   * @return
   */
  public Users register(String fname, String lname, String email, String title,
          String tel, String orcid, int uid, String password, String otpSecret,
          SecurityQuestion question, String answer, int status, int yubikey,
          String validationKey) {

    // assigne a username
    String uname = USERNAME_PREFIX + uid;

    Users user = new Users();
    user.setUsername(uname);
    user.setPassword(password);
    user.setSecret(otpSecret);
    user.setEmail(email);
    user.setFname(fname);
    user.setLname(lname);
    user.setMobile(checkDefaultValue(tel));
    user.setOrcid(checkDefaultValue(orcid));
    user.setTitle(checkDefaultValue(title));
    user.setActivated(new Timestamp(new Date().getTime()));
    user.setStatus(status);
    user.setValidationKey(validationKey);
    /*
     * offline: -1
     * online: 1
     */
    user.setIsonline(-1);
    user.setSecurityQuestion(question);
    user.setSecurityAnswer(answer);
    user.setPasswordChanged(new Timestamp(new Date().getTime()));
    user.setMode(yubikey);
    em.persist(user);
    em.flush();
    return user;
  }

  /**
   * Return the max uid in the table.
   *
   * @return
   */
  public int lastUserID() {
    Query query = em.createNativeQuery("SELECT MAX(p.uid) FROM hopsworks.users p");
    Object obj = query.getSingleResult();

    if (obj == null) {
      return STARTING_USER;
    }
    return (Integer) obj;
  }

  public void persist(Users user) {
    em.persist(user);
  }

  public boolean updatePeople(Users user) {
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
    Users u = findByEmail(email);
    if (u != null) {
      TypedQuery<PeopleGroup> query = em.createNamedQuery(
              "PeopleGroup.findByUid", PeopleGroup.class);
      query.setParameter("uid", u.getUid());
      PeopleGroup p = (PeopleGroup) query.getSingleResult();

      Address a = u.getAddress();

      em.remove(a);
      em.remove(p);

      if (u.getMode() == PeopleAccountStatus.YUBIKEY_USER.getValue()) {
        em.remove(u.getMode());
      }

      em.remove(u);
      success = true;
    }

    return success;
  }

  public boolean registerOrg(Users uid, String org, String department) {

    Organization organization = new Organization();
    organization.setUid(uid);
    organization.setOrgName(checkDefaultValue(org));
    organization.setDepartment(checkDefaultValue(department));
    organization.setContactEmail(checkDefaultValue(""));
    organization.setContactPerson(checkDefaultValue(""));
    organization.setWebsite(checkDefaultValue(""));
    organization.setFax(checkDefaultValue(""));
    organization.setPhone(checkDefaultValue(""));
    em.persist(organization);
    return true;

  }

  public boolean updateOrganization(Organization org) {
    em.merge(org);
    return true;
  }

    /**
   * Get all the users that are not in the given project.
   *
   * @param project The project on which to search.
   * @return List of User objects that are not in the project.
   */
  public List<Users> filterUsersBasedOnProject(Project project) {
    TypedQuery<Users> query = em.createQuery(
            "SELECT u FROM hopsworks.users u WHERE u NOT IN (SELECT DISTINCT st.user FROM hopsworks.project_team st WHERE st.project = :project)",
            Users.class);
    query.setParameter("project", project);
    return query.getResultList();
  }


  
}
