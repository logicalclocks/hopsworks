package se.kth.bbc.security.ua;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import se.kth.bbc.security.ua.model.User;

/**
 *
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
@ManagedBean
@RequestScoped
public class PeopleStatusBean implements Serializable {

  private static final Logger logger = Logger.getLogger(UserManager.class.
          getName());

  @EJB
  private UserManager userManager;

  private boolean open_requests = false;
  private boolean open_consents = false;
  private int tabIndex;
  private User user;

  public boolean isOpen_requests() {
    return checkForRequests();
  }

  public void setOpen_requests(boolean open_requests) {
    this.open_requests = open_requests;
  }

  public User getUser() {
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
    User p = userManager.findByEmail(getRequest().getRemoteUser());
    return userManager.findGroups(p.getUid()).contains("SYS_ADMIN");
  }

  /**
   * Return both system wide and study wide roles
   * <p>
   * @return
   */
  public boolean isResearcher() {
    User p = userManager.findByEmail(getRequest().getRemoteUser());
    List<String> roles = userManager.findGroups(p.getUid());
    //TODO: Remove SYS_ADMIN in the final release
    return (roles.contains("BBC_RESEARCHER") || roles.contains("BBC_ADMIN")
            || roles.contains("SYS_ADMIN"));
  }

  /**
   * Return study owner role
   * <p>
   * @return
   */
  public boolean isBBCAdmin() {
    User p = userManager.findByEmail(getRequest().getRemoteUser());
    return userManager.findGroups(p.getUid()).contains("BBC_ADMIN");
  }

  public boolean isAnyAuthorizedRole() {

    User p = userManager.findByEmail(getRequest().getRemoteUser());
    List<String> roles = userManager.findGroups(p.getUid());
    return (roles.contains("SYS_ADMIN") || roles.contains("BBC_ADMIN") || roles.
            contains("BBC_RESEARCHER") || roles.contains("AUDITOR"));
  }

  public boolean isAuditorRole() {

    User p = userManager.findByEmail(getRequest().getRemoteUser());
    List<String> roles = userManager.findGroups(p.getUid());
    return (roles.contains("SYS_ADMIN") || roles.contains("AUDITOR"));
  }

  /**
   *
   * @return
   */
  public boolean checkForRequests() {
    if (isResearcher()) {
      //return false if no requests

      open_requests = !(userManager.findAllByStatus(
              PeopleAccountStatus.MOBILE_ACCOUNT_INACTIVE.getValue()).isEmpty())
              || !(userManager.findAllByStatus(
                      PeopleAccountStatus.YUBIKEY_ACCOUNT_INACTIVE.getValue()).
              isEmpty());
    }
    return open_requests;
  }

  public String logOut() {
    //TODO: fix this...
    //Now does not logout, just redirects to login page
    HttpServletRequest r = getRequest();
    HttpSession s = r.getSession();
    s.invalidate();
//    try {
//      r.logout();
//    } catch (ServletException ex) {
//      // ignore the exception if it happens
//    }
//        getRequest().getSession().invalidate();
    return "welcome";
  }

  public boolean isLoggedIn() {
    return getRequest().getRemoteUser() != null;
  }

  public String openRequests() {
    this.tabIndex = 1;
    return "userMgmt";
  }

  public boolean isOpen_consents() {
    return open_consents;
  }

  public void setOpen_consents(boolean open_consents) {
    this.open_consents = open_consents;
  }
}
