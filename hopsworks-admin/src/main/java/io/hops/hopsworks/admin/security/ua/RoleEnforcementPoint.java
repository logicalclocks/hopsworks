package io.hops.hopsworks.admin.security.ua;

import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import io.hops.hopsworks.common.dao.user.security.audit.AuditManager;
import io.hops.hopsworks.common.dao.user.security.audit.UserAuditActions;
import io.hops.hopsworks.common.constants.auth.AuthenticationConstants;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.util.AuditUtil;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;

@ManagedBean
@RequestScoped
public class RoleEnforcementPoint implements Serializable {

  @EJB
  protected UserManager userManager;

  @EJB
  private AuditManager am;

  private boolean open_requests = false;
  private int tabIndex;
  private Users user;

  public boolean isOpen_requests() {
    return checkForRequests();
  }

  public void setOpen_requests(boolean open_reauests) {
    this.open_requests = open_reauests;
  }

  public Users getUserFromSession() {
    if (user == null) {
      ExternalContext context = FacesContext.getCurrentInstance().
              getExternalContext();
      String userEmail = context.getUserPrincipal().getName();
      user = userManager.findByEmail(userEmail);
    }
    return user;
  }

  public void setTabIndex(int index) {
    this.tabIndex = index;
  }

  public int getTabIndex() {
    int oldindex = tabIndex;
    tabIndex = 0;
    return oldindex;
  }

  private HttpServletRequest getRequest() {
    return (HttpServletRequest) FacesContext.getCurrentInstance().
            getExternalContext().getRequest();
  }

  /**
   * Return systemwide admin for user administration
   * <p>
   * @return
   */
  public boolean isAdmin() {
    if (getRequest().getRemoteUser() != null) {
      Users p = userManager.findByEmail(getRequest().getRemoteUser());
      return userManager.findGroups(p.getUid()).contains("HOPS_ADMIN");
    }
    return false;
  }

  /**
   * Return both system wide and study wide roles
   * <p>
   * @return
   */
  public boolean isUser() {

    Users p = userManager.findByEmail(getRequest().getRemoteUser());
    List<String> roles = userManager.findGroups(p.getUid());
    return (roles.contains("HOPS_USER"));
  }

  public boolean isAuditorRole() {

    Users p = userManager.findByEmail(getRequest().getRemoteUser());
    List<String> roles = userManager.findGroups(p.getUid());
    return (roles.contains("AUDITOR") || roles.contains("HOPS_ADMIN"));
  }

  public boolean isAgentRole() {

    Users p = userManager.findByEmail(getRequest().getRemoteUser());
    List<String> roles = userManager.findGroups(p.getUid());
    return (roles.contains("AGENT"));
  }

  public boolean isOnlyAuditorRole() {

    Users p = userManager.findByEmail(getRequest().getRemoteUser());
    List<String> roles = userManager.findGroups(p.getUid());
    return (roles.contains("AUDITOR") && !roles.contains("HOPS_ADMIN"));
  }

  /**
   *
   * @return
   */
  public boolean checkForRequests() {
    if (isAdmin()) {
      //return false if no requests
      open_requests = !(userManager.findAllByStatus(
              PeopleAccountStatus.NEW_MOBILE_ACCOUNT.getValue()).isEmpty())
              || !(userManager.findAllByStatus(
                      PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT.getValue()).
              isEmpty());
    }
    return open_requests;
  }

  public boolean isLoggedIn() {
    return getRequest().getRemoteUser() != null;
  }

  public String openRequests() {
    this.tabIndex = 1;
    if (!userManager.findMobileRequests().isEmpty()) {
      return "mobUsers";
    } else if (!userManager.findYubikeyRequests().isEmpty()) {
      return "yubikeyUsers";
    } else if (!userManager.findSPAMAccounts().isEmpty()) {
      return "spamUsers";
    }

    return "mobUsers";
  }

  // MOVE OUT THIS
  public String logOut() {
    try {
      getRequest().getSession().invalidate();
      
      FacesContext ctx = FacesContext.getCurrentInstance();
      HttpSession sess = (HttpSession) ctx.getExternalContext().getSession(false);
      HttpServletRequest req = (HttpServletRequest) ctx.getExternalContext().
            getRequest();

      String ip = AuditUtil.getIPAddress();
      String browser = AuditUtil.getBrowserInfo();
      String os = AuditUtil.getOSInfo();
      String macAddress = AuditUtil.getMacAddress(ip);
      
      am.registerLoginInfo(getUserFromSession(), UserAuditActions.LOGOUT.
              getValue(), ip,
              browser, os, macAddress, UserAuditActions.SUCCESS.name());
      
      userManager.setOnline(user.getUid(), AuthenticationConstants.IS_OFFLINE);
      req.logout();
      if (null != sess) {
        sess.invalidate();
      }
      ctx.getExternalContext().redirect("/hopsworks/#/home");
    } catch (IOException | ServletException ex) {
      Logger.getLogger(RoleEnforcementPoint.class.getName()).
              log(Level.SEVERE, null, ex);
    }
    return ("welcome");
  }
}
