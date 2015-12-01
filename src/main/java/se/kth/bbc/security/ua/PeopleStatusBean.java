/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;

import java.io.Serializable;
import java.net.SocketException;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import se.kth.bbc.security.audit.AuditManager;
import se.kth.bbc.security.audit.AuditUtil;
import se.kth.bbc.security.audit.LoginsAuditActions;
import se.kth.hopsworks.user.model.Users;
 
@ManagedBean
@RequestScoped
public class PeopleStatusBean implements Serializable {

  @EJB
  private UserManager userManager;

  @EJB
  private AuditManager am;

  private boolean open_reauests = false;
  private boolean open_consents = false;
  private int tabIndex;
  private Users user;

  public boolean isOpen_reauests() {
    return checkForRequests();
  }

  public void setOpen_reauests(boolean open_reauests) {
    this.open_reauests = open_reauests;
  }

  public Users getUser() {
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
  public boolean isSYSAdmin() {
    if (getRequest().getRemoteUser()!= null) {
      Users p = userManager.findByEmail(getRequest().getRemoteUser());
        return userManager.findGroups(p.getUid()).contains("SYS_ADMIN");
    } return false;
  }

  /**
   * Return both system wide and study wide roles
   * <p>
   * @return
   */
  public boolean isResearcher() {
    Users p = userManager.findByEmail(getRequest().getRemoteUser());
    List<String> roles = userManager.findGroups(p.getUid());
    return (roles.contains("BBC_RESEARCHER") || roles.contains("BBC_ADMIN")
            || roles.contains("BBC_USER"));
  }

  /**
   * Return study owner role
   * <p>
   * @return
   */
  public boolean isBBCAdmin() {
    Users p = userManager.findByEmail(getRequest().getRemoteUser());
    return userManager.findGroups(p.getUid()).contains("BBC_ADMIN");
  }

  public boolean isAnyAuthorizedResearcherRole() {

    Users p = userManager.findByEmail(getRequest().getRemoteUser());
    List<String> roles = userManager.findGroups(p.getUid());
    return (roles.contains("BBC_ADMIN") || roles.
            contains("BBC_RESEARCHER") || roles.contains("BBC_USER") || roles.
            contains("BBC_GUEST"));
  }

  public boolean isAuditorRole() {

    Users p = userManager.findByEmail(getRequest().getRemoteUser());
    List<String> roles = userManager.findGroups(p.getUid());
    return (roles.contains("AUDITOR") || roles.contains("SYS_ADMIN"));
  }

   
  public boolean isOnlyAuditorRole() {

    Users p = userManager.findByEmail(getRequest().getRemoteUser());
    List<String> roles = userManager.findGroups(p.getUid());
    return (roles.contains("AUDITOR") && ! roles.contains("SYS_ADMIN"));
  }
  /**
   *
   * @return
   */
  public boolean checkForRequests() {
    if (isSYSAdmin()) {
      //return false if no requests
      open_reauests = !(userManager.findAllByStatus(
              PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue()).isEmpty())
              || !(userManager.findAllByStatus(
                      PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue()).
              isEmpty());
    }
    return open_reauests;
  }

  public boolean isLoggedIn() {
    return getRequest().getRemoteUser() != null;
  }

  public String openRequests() {
    this.tabIndex = 1;
    if (!userManager.findAllByStatus(
            PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue()).isEmpty()) {
      return "mobUsers";
    }
    return "yubikeyUsers";
  }

  public String logOut() throws SocketException {
    getRequest().getSession().invalidate();

    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpSession sess = (HttpSession) ctx.getExternalContext().getSession(false);

    String ip = AuditUtil.getIPAddress();
    String browser = AuditUtil.getBrowserInfo();
    String os = AuditUtil.getOSInfo();
    String macAddress = AuditUtil.getMacAddress(ip);

    am.registerLoginInfo(getUser(), LoginsAuditActions.LOGOUT.getValue(), ip,
            browser, os, macAddress, "SUCCESS");

    userManager.setOnline(user.getUid(), -1);

    if (null != sess) {
      sess.invalidate();
    }
    return ("welcome");

  }
}
