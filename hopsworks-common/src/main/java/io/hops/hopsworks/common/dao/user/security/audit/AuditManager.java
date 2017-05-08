package io.hops.hopsworks.common.dao.user.security.audit;

import io.hops.hopsworks.common.constants.auth.AuthenticationConstants;
import io.hops.hopsworks.common.util.AuditUtil;
import io.hops.hopsworks.common.dao.user.consent.Consents;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import io.hops.hopsworks.common.dao.user.Users;

@Stateless
public class AuditManager {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  /**
   * Get the user last login info.
   *
   * @param uid
   * @return
   */
  public Userlogins getLastUserLogin(int uid) {
    String sql = "SELECT * FROM hopsworks.userlogins  WHERE uid=" + uid
            + " ORDER BY login_date DESC LIMIT 1 OFFSET 2";
    Query query = em.createNativeQuery(sql, Userlogins.class);

    List<Userlogins> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }

    if (ul.size() == 1) {
      return ul.get(0);
    }

    return ul.get(1);

  }

  /**
   * Get all user logins in a period.
   *
   * @param from
   * @param to
   * @param action
   * @return
   */
  public List<Userlogins> getUsersLoginsFromTo(Date from, Date to, String action) {

    String sql;
    if (action.equals(UserAuditActions.ALL.name())) {

      sql = "SELECT * FROM hopsworks.userlogins  WHERE  (login_date >= '"
              + from
              + "' AND login_date <='" + to + "')";

    } else if (action.equals(UserAuditActions.SUCCESS.name()) || action.equals(
            UserAuditActions.FAILED.name())
            || action.equals(UserAuditActions.ABORTED.name())) {

      sql = "SELECT * FROM hopsworks.userlogins  WHERE  (login_date >= '"
              + from
              + "' AND login_date <='" + to + "' AND outcome ='" + action + "')";

    } else {
      sql = "SELECT * FROM hopsworks.userlogins  WHERE  (login_date >= '"
              + from
              + "' AND login_date <='" + to + "' AND action ='" + action + "')";

    }
    Query query = em.createNativeQuery(sql, Userlogins.class);

    List<Userlogins> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }

    return ul;
  }

  /**
   * Get user logins with an specific action in a period of time.
   *
   * @param uid
   * @param from
   * @param to
   * @param action
   * @return
   */
  public List<Userlogins> getUserLoginsFromTo(int uid, Date from, Date to,
          String action) {

    String sql = "SELECT * FROM hopsworks.userlogins  WHERE (uid=" + uid
            + " AND login_date >= '" + from + "' AND login_date <='" + to
            + "' AND action ='" + action + "')";

    Query query = em.createNativeQuery(sql, Userlogins.class);

    List<Userlogins> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }

    return ul;
  }

  /**
   * Get user logins outcomes: success/failed.
   *
   * @param uid
   * @param from
   * @param to
   * @param outcome
   * @return
   */
  public List<Userlogins> getUserLoginsOutcome(int uid, Date from, Date to,
          String outcome) {
    String sql;

    if (uid >= AuthenticationConstants.STARTING_USER) {
      sql = "SELECT * FROM hopsworks.userlogins  WHERE (uid=" + uid
              + " AND login_date >= '" + from + "' AND login_date <='" + to
              + "' AND outcome ='" + outcome + "')";
    } else {

      sql = "SELECT * FROM hopsworks.userlogins  WHERE ( login_date >= '" + from
              + "' AND login_date <='" + to
              + "' AND outcome ='" + outcome + "')";

    }
    Query query = em.createNativeQuery(sql, Userlogins.class);

    List<Userlogins> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }

    return ul;
  }

  /**
   * Get account changes within a period.
   *
   * @param from
   * @param to
   * @param action
   * @return
   */
  public List<AccountAudit> getAccountAudit(Date from, Date to,
          String action) {

    String sql;
    if (action.equals(AccountsAuditActions.ALL.name())) {

      sql = "SELECT * FROM hopsworks.account_audit WHERE ( time >='" + from
              + "' AND time <='" + to + "')";
    } else if (action.equals(AccountsAuditActions.SUCCESS.name()) || action.
            equals(AccountsAuditActions.FAILED.name()) || action.equals(
            AccountsAuditActions.ABORTED.name())) {
      sql = "SELECT * FROM hopsworks.account_audit WHERE ( time >='" + from
              + "' AND time <='" + to + "' AND outcome ='"
              + action + "')";

    } else {

      sql = "SELECT * FROM hopsworks.account_audit WHERE ( time >='" + from
              + "' AND time <='" + to + "' AND action ='"
              + action + "')";
    }
    Query query = em.createNativeQuery(sql, AccountAudit.class);

    List<AccountAudit> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }
    return ul;
  }

  /**
   * Get account changes for a specific user.
   *
   * @param uid
   * @param from
   * @param to
   * @param action
   * @return
   */
  public List<AccountAudit> getAccountAudit(int uid, Date from, Date to,
          String action) {

    String sql = "SELECT * FROM hopsworks.account_audit WHERE (initiator=" + uid
            + " AND time >='" + from + "' AND time <='" + to + "' AND action ='"
            + action + "')";
    Query query = em.createNativeQuery(sql, AccountAudit.class);

    List<AccountAudit> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }
    return ul;
  }

  /**
   * Get roles entitlement for user.
   *
   * @param uid
   * @param from
   * @param to
   * @param action
   * @return
   */
  public List<RolesAudit> getRoletAudit(int uid, Date from, Date to,
          String action) {

    String sql = null;

    if (action.isEmpty() || action == null || action.equals("ALL")) {
      sql = "SELECT * FROM hopsworks.roles_audit WHERE (target=" + uid
              + " AND time >= '" + from + "' AND time <= '" + to
              + "')";
    } else {

      sql = "SELECT * FROM hopsworks.roles_audit WHERE (target=" + uid
              + " AND time >= '" + from + "' AND time <= '" + to
              + "' AND action = '"
              + action + "')";
    }

    Query query = em.createNativeQuery(sql, RolesAudit.class);

    List<RolesAudit> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }
    return ul;
  }

  /**
   * Get all role entitelments in a period of time.
   *
   * @param from
   * @param to
   * @param action
   * @return
   */
  public List<RolesAudit> getRolesAudit(Date from, Date to,
          String action) {
    String sql = null;

    if (action.isEmpty() || action == null || action.equals("ALL")) {
      sql = "SELECT * FROM hopsworks.roles_audit WHERE ( time >= '" + from
              + "' AND time <= '" + to + "')";
    } else {
      sql = "SELECT * FROM hopsworks.roles_audit WHERE ( time >= '" + from
              + "' AND time <= '" + to + "' AND action = '"
              + action + "')";
    }

    Query query = em.createNativeQuery(sql, RolesAudit.class);

    List<RolesAudit> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }
    return ul;
  }

  /**
   * Get all role outcomes: success/failure.
   *
   * @param from
   * @param to
   * @param outcome
   * @return
   */
  public List<RolesAudit> getRoletAuditOutcome(Date from, Date to,
          String outcome) {

    String sql = null;

    if (outcome.isEmpty() || outcome == null || outcome.equals("ALL")) {
      sql = "SELECT * FROM hopsworks.roles_audit WHERE ( time >= '" + from
              + "' AND time <= '" + to + "')";
    } else {
      sql = "SELECT * FROM hopsworks.roles_audit WHERE ( time >= '" + from
              + "' AND time <= '" + to + "' AND outcome = '"
              + outcome + "')";
    }
    Query query = em.createNativeQuery(sql, RolesAudit.class);

    List<RolesAudit> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }
    return ul;
  }

  /**
   * Get all account changes in a period of time based on outcome:
   * success/failure.
   *
   * @param from
   * @param to
   * @param outcome
   * @return
   */
  public List<AccountAudit> getAccountAuditOutcome(Date from, Date to,
          String outcome) {

    String sql = null;

    if (outcome.isEmpty() || outcome == null || outcome.equals("ALL")) {
      sql = "SELECT * FROM hopsworks.account_audit WHERE ( time >= '" + from
              + "' AND time <= '" + to + "')";
    } else {
      sql = "SELECT * FROM hopsworks.account_audit WHERE ( time >= '" + from
              + "' AND time <= '" + to + "' AND outcome = '"
              + outcome + "')";
    }

    Query query = em.createNativeQuery(sql, AccountAudit.class);

    List<AccountAudit> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }
    return ul;
  }

  public List<ConsentsAudit> getConsentsAudit(Date from, Date to,
          String action) {
    String sql = null;

    if (action.isEmpty() || action == null || action.equals("ALL")) {
      sql = "SELECT * FROM hopsworks.consents_audit WHERE ( time >= '" + from
              + "' AND time <= '" + to + "')";
    } else {
      sql = "SELECT * FROM hopsworks.consents_audit WHERE ( time >= '" + from
              + "' AND time <= '" + to + "' AND action = '"
              + action + "')";
    }

    Query query = em.createNativeQuery(sql, ConsentsAudit.class);

    List<ConsentsAudit> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }
    return ul;
  }

  /**
   * Register the login information into the log storage.
   * <p>
   * @param u
   * @param action
   * @param ip
   * @param browser
   * @param os
   * @param mac
   * @param outcome
   * @return
   */
  public void registerLoginInfo(Users u, String action, String ip,
          String browser, String os, String mac, String outcome) {

    Userlogins l = new Userlogins();
    l.setUid(u.getUid());
    l.setBrowser(browser);
    l.setIp(ip);
    l.setOs(os);
    l.setAction(action);
    l.setOutcome(outcome);
    l.setLoginDate(new Timestamp(new Date().getTime()));
    l.setEmail(u.getEmail());
    l.setMac(mac);
    em.persist(l);
  }

  /**
   * Register user logins attempts.
   *
   * @param user
   * @param action
   * @param outcome
   * @param req
   */
  public void registerLoginInfo(Users user, String action, String outcome,
          HttpServletRequest req) {

    Userlogins login = new Userlogins();
    login.setUid(user.getUid());
    login.setEmail(user.getEmail());
    login.setBrowser(AuditUtil.getBrowserInfo(req));
    login.setIp(AuditUtil.getIPAddress(req));
    login.setMac(AuditUtil.getMacAddress(AuditUtil.getIPAddress(req)));
    login.setAction(action);
    login.setOs(AuditUtil.getOSInfo(req));
    login.setOutcome(outcome);
    login.setLoginDate(new Date());
    em.persist(login);
  }

  /**
   * Register the role assignment changes.
   * <p>
   * @param u
   * @param action
   * @param outcome
   * @param message
   * @param req
   * @param tar
   * @return
   */
  public boolean registerRoleChange(Users u, String action, String outcome,
          String message,
          Users tar, HttpServletRequest req) {

    RolesAudit ra = new RolesAudit();
    ra.setInitiator(u);
    ra.setBrowser(AuditUtil.getBrowserInfo(req));
    ra.setIp(AuditUtil.getIPAddress(req));
    ra.setOs(AuditUtil.getOSInfo(req));
    ra.setEmail(u.getEmail());
    ra.setAction(action);
    ra.setOutcome(outcome);
    ra.setTime(new Timestamp(new Date().getTime()));
    ra.setMac(AuditUtil.getMacAddress(AuditUtil.getIPAddress(req)));
    ra.setMessage(message);
    ra.setTarget(tar);
    em.persist(ra);

    return true;
  }

  /**
   * Register role entitlement changes.
   *
   * @param u
   * @param action
   * @param outcome
   * @param message
   * @param tar
   * @return
   */
  public boolean registerRoleChange(Users u, String action, String outcome,
          String message,
          Users tar) {

    RolesAudit ra = new RolesAudit();
    ra.setInitiator(u);
    ra.setBrowser(AuditUtil.getBrowserInfo());
    ra.setIp(AuditUtil.getIPAddress());
    ra.setOs(AuditUtil.getOSInfo());
    ra.setAction(action);
    ra.setOutcome(outcome);
    ra.setTime(new Timestamp(new Date().getTime()));
    ra.setEmail(u.getEmail());
    ra.setMac(AuditUtil.getMacAddress(AuditUtil.getIPAddress()));
    ra.setMessage(message);
    ra.setTarget(tar);
    em.persist(ra);

    return true;
  }

  /**
   * Register the account update info.
   * <p>
   * @param init
   * @param action
   * @param outcome
   * @param message
   * @param target
   */
  public void registerAccountChange(Users init, String action, String outcome,
          String message, Users target) {

    AccountAudit aa = new AccountAudit();
    aa.setInitiator(init);
    aa.setBrowser(AuditUtil.getBrowserInfo());
    aa.setIp(AuditUtil.getIPAddress());
    aa.setOs(AuditUtil.getOSInfo());
    aa.setAction(action);
    aa.setOutcome(outcome);
    aa.setMessage(message);
    aa.setTime(new Timestamp(new Date().getTime()));
    aa.setMac(AuditUtil.getMacAddress(AuditUtil.getIPAddress()));
    aa.setEmail(target.getEmail());
    aa.setTarget(target);
    em.persist(aa);
  }

  /**
   * Register account related changes.
   *
   * @param init
   * @param action
   * @param outcome
   * @param message
   * @param target
   * @param req
   * @return
   */
  public void registerAccountChange(Users init, String action, String outcome,
          String message, Users target, HttpServletRequest req) {

    AccountAudit aa = new AccountAudit();
    aa.setInitiator(init);
    aa.setBrowser(AuditUtil.getBrowserInfo(req));
    aa.setIp(AuditUtil.getIPAddress(req));
    aa.setOs(AuditUtil.getOSInfo(req));
    aa.setAction(action);
    aa.setOutcome(outcome);
    aa.setMessage(message);
    aa.setTime(new Timestamp(new Date().getTime()));
    aa.setMac(AuditUtil.getMacAddress(AuditUtil.getIPAddress(req)));
    aa.setEmail(init.getEmail());
    aa.setTarget(target);
    em.persist(aa);
  }

  /**
   * Register consent information in audit logs.
   *
   * @param user
   * @param action
   * @param outcome
   * @param cons
   * @param req
   */
  public void registerConsentInfo(Users user, String action, String outcome,
          Consents cons,
          HttpServletRequest req) {

    ConsentsAudit ca = new ConsentsAudit();
    ca.setConsentID(cons);
    ca.setInitiator(user);
    ca.setBrowser(AuditUtil.getBrowserInfo(req));
    ca.setIp(AuditUtil.getIPAddress(req));
    ca.setOs(AuditUtil.getOSInfo(req));
    ca.setAction(action);
    ca.setOutcome(outcome);
    ca.setTime(new Timestamp(new Date().getTime()));
    ca.setMac(AuditUtil.getMacAddress(AuditUtil.getIPAddress(req)));

    em.persist(ca);
  }
}
