/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.audit;

import java.net.SocketException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import se.kth.bbc.security.audit.model.AccountAudit;
import se.kth.bbc.security.audit.model.RolesAudit;
import se.kth.bbc.security.audit.model.Userlogins;
import se.kth.hopsworks.user.model.Users;

@Stateless
public class AuditManager {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

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

  public List<Userlogins> getUsersLoginsFromTo(Date from, Date to, String action) {

    String sql;
    if(action.equals(UserAuditActions.ALL.name())) {
  
          sql = "SELECT * FROM hopsworks.userlogins  WHERE  (login_date >= '"
              + from
              + "' AND login_date <='" + to + "')";
    
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

  public List<AccountAudit> getAccountAudit(Date from, Date to,
          String action) {

    String sql = "SELECT * FROM hopsworks.account_audit WHERE ( time >='" + from
            + "' AND time <='" + to + "' AND action ='"
            + action + "')";
    Query query = em.createNativeQuery(sql, AccountAudit.class);

    List<AccountAudit> ul = query.getResultList();

    if (ul.isEmpty()) {
      return null;
    }
    return ul;
  }

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

  public List<RolesAudit> getRoletAudit(Date from, Date to,
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

  public List<RolesAudit> getInitiatorRoletAudit(int uid, Date from, Date to,
          String action) {

    String sql = "SELECT * FROM hopsworks.rolse_audit WHERE (initiator=" + uid
            + " AND time >= '" + from + "' AND time <= '" + to
            + "' AND action ='"
            + action + "')";
    Query query = em.createNativeQuery(sql, RolesAudit.class);

    List<RolesAudit> ul = query.getResultList();

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
  public boolean registerLoginInfo(Users u, String action, String ip,
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

    return true;
  }

  public void registerLoginInfo(Users user, String action, String outcome,
          HttpServletRequest req) throws SocketException {

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
   * @throws java.net.SocketException
   */
  public boolean registerRoleChange(Users u, String action, String outcome, String message,
          Users tar, HttpServletRequest req) throws SocketException {

    RolesAudit ra = new RolesAudit();
    ra.setInitiator(u);
    ra.setBrowser(AuditUtil.getBrowserInfo(req));
    ra.setIp(AuditUtil.getIPAddress(req));
    ra.setOs(AuditUtil.getOSInfo(req));
    ra.setEmail(u.getEmail());
    ra.setAction(action);
    ra.setOutcome(outcome);
    ra.setTime(new Timestamp(new Date().getTime()));
    ra.setEmail(u.getEmail());
    ra.setMac(AuditUtil.getMacAddress(AuditUtil.getIPAddress(req)));
    ra.setMessage(message);
    ra.setTarget(tar);
    em.persist(ra);

    return true;
  }
  /**
   * Register the role assignment changes.
   * <p>
   * @param u
   * @param action
   * @param ip
   * @param browser
   * @param os
   * @param mac
   * @param outcome
   * @param message
   * @param tar
   * @return
   */
  public boolean registerRoleChange(Users u, String action, String ip,
          String browser, String os, String mac, String outcome, String message,
          Users tar) {

    RolesAudit ra = new RolesAudit();
    ra.setInitiator(u);
    ra.setBrowser(browser);
    ra.setIp(ip);
    ra.setOs(os);
    ra.setEmail(u.getEmail());
    ra.setAction(action);
    ra.setOutcome(outcome);
    ra.setTime(new Timestamp(new Date().getTime()));
    ra.setEmail(u.getEmail());
    ra.setMac(mac);
    ra.setMessage(message);
    ra.setTarget(tar);
    em.persist(ra);

    return true;
  }
/**
 * Register the account update info.
 * 
 * @param init
 * @param action
 * @param outcome
 * @param message
 * @param target
 * @return
 * @throws SocketException 
 */
  public boolean registerAccountChange(Users init, String action, String outcome, String message, Users target) throws SocketException
  {

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
    em.persist(aa);

    return true;
  }

}
