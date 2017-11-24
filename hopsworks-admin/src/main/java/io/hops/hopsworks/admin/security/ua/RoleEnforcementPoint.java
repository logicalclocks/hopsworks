package io.hops.hopsworks.admin.security.ua;

import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.ua.PeopleAccountStatus;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UsersController;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;

@ManagedBean
@RequestScoped
public class RoleEnforcementPoint implements Serializable {

  @EJB
  protected UsersController usersController;
  @EJB
  protected AuthController authController;
  @EJB
  private UserFacade userFacade;

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
      ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
      String userEmail = context.getUserPrincipal().getName();
      user = userFacade.findByEmail(userEmail);
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
    return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
  }

  /**
   * Return systemwide admin for user administration
   * <p>
   * @return
   */
  public boolean isAdmin() {
    if (getRequest().getRemoteUser() != null) {
      Users p = userFacade.findByEmail(getRequest().getRemoteUser());
      return usersController.isUserInRole(p, "HOPS_ADMIN");
    }
    return false;
  }

  /**
   * Return both system wide and study wide roles
   * <p>
   * @return
   */
  public boolean isUser() {
    Users p = userFacade.findByEmail(getRequest().getRemoteUser());
    return usersController.isUserInRole(p, "HOPS_USER");
  }

  public boolean isAuditorRole() {

    Users p = userFacade.findByEmail(getRequest().getRemoteUser());
    return (usersController.isUserInRole(p, "AUDITOR") || !usersController.isUserInRole(p, "HOPS_ADMIN"));
  }

  public boolean isAgentRole() {
    Users p = userFacade.findByEmail(getRequest().getRemoteUser());
    return (usersController.isUserInRole(p,"AGENT"));
  }

  public boolean isOnlyAuditorRole() {
    Users p = userFacade.findByEmail(getRequest().getRemoteUser());
    return (usersController.isUserInRole(p,"AUDITOR") && !usersController.isUserInRole(p,"HOPS_ADMIN"));
  }

  /**
   *
   * @return
   */
  public boolean checkForRequests() {
    if (isAdmin()) {
      //return false if no requests
      open_requests = !(userFacade.findAllByStatus(PeopleAccountStatus.NEW_MOBILE_ACCOUNT).isEmpty())
              || !(userFacade.findAllByStatus(PeopleAccountStatus.NEW_YUBIKEY_ACCOUNT).isEmpty()
              || !(userFacade.findAllByStatus(PeopleAccountStatus.VERIFIED_ACCOUNT).isEmpty()));
    }
    return open_requests;
  }

  public boolean isLoggedIn() {
    return getRequest().getRemoteUser() != null;
  }

  public String openRequests() {
    this.tabIndex = 1;
    if (!userFacade.findAllMobileRequests().isEmpty()) {
      return "mobUsers";
    } else if (!userFacade.findYubikeyRequests().isEmpty()) {
      return "yubikeyUsers";
    } else if (!userFacade.findAllByStatus(PeopleAccountStatus.SPAM_ACCOUNT).isEmpty()) {
      return "spamUsers";
    }

    return "mobUsers";
  }

  // MOVE OUT THIS
  public String logOut() {
    try {
      this.user = getUserFromSession();
      HttpServletRequest req = getRequest();
      req.getSession().invalidate();
      req.logout(); 
      if (user != null) {
        authController.registerLogout(user, req);
      }
      FacesContext.getCurrentInstance().getExternalContext().redirect("/hopsworks/#!/home");
    } catch (IOException | ServletException ex) {
      Logger.getLogger(RoleEnforcementPoint.class.getName()).log(Level.SEVERE, null, ex);
    }
    return ("welcome");
  }
}
