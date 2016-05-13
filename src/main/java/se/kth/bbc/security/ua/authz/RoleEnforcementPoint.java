package se.kth.bbc.security.ua.authz;

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
import se.kth.bbc.security.audit.UserAuditActions;
import se.kth.bbc.security.auth.AuthenticationConstants;
import se.kth.bbc.security.ua.PeopleAccountStatus;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.user.model.Users;
 
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
    if (getRequest().getRemoteUser()!= null) {
      Users p = userManager.findByEmail(getRequest().getRemoteUser());
        return userManager.findGroups(p.getUid()).contains("HOPS_ADMIN");
    } return false;
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

   
  public boolean isOnlyAuditorRole() {

    Users p = userManager.findByEmail(getRequest().getRemoteUser());
    List<String> roles = userManager.findGroups(p.getUid());
    return (roles.contains("AUDITOR") && ! roles.contains("HOPS_ADMIN"));
  }
  /**
   *
   * @return
   */
  public boolean checkForRequests() {
    if (isAdmin()) {
      //return false if no requests
      open_requests = !(userManager.findAllByStatus(
              PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue()).isEmpty())
              || !(userManager.findAllByStatus(
                      PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue()).
              isEmpty());
    }
    return open_requests;
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

  
  // MOVE OUT THIS
  public String logOut() {
    getRequest().getSession().invalidate();

    FacesContext ctx = FacesContext.getCurrentInstance();
    HttpSession sess = (HttpSession) ctx.getExternalContext().getSession(false);

    String ip = AuditUtil.getIPAddress();
    String browser = AuditUtil.getBrowserInfo();
    String os = AuditUtil.getOSInfo();
    String macAddress = AuditUtil.getMacAddress(ip);

    am.registerLoginInfo(getUserFromSession(), UserAuditActions.LOGOUT.getValue(), ip,
            browser, os, macAddress, UserAuditActions.SUCCESS.name());

    userManager.setOnline(user.getUid(), AuthenticationConstants.IS_OFFLINE);
    
    if (null != sess) {
      sess.invalidate();
    }
    return ("welcome");

  }
}
