/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.user;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
@ManagedBean
@RequestScoped
public class UserMB {

  private Username user;
  @EJB
  private UserFacade userFacade;

  public Username getUser() {
    if (user == null) {
      ExternalContext context = FacesContext.getCurrentInstance().
              getExternalContext();
      String userEmail = context.getUserPrincipal().getName();
      user = userFacade.findByEmail(userEmail);
    }
    return user;
  }

  public String getName() {
    return (getUser() != null) ? user.getName() : "null";
  }

  public boolean isUserAdmin() {
    return getRequest().isUserInRole("ADMIN");
  }

  public boolean isUserBbcAdmin() {
    return getRequest().isUserInRole("BBC_ADMIN");
  }

  public boolean isAdminUser() {
    return getRequest().isUserInRole("ADMIN") || getRequest().isUserInRole(
            "BBC_ADMIN");
  }

  public boolean isAnyAdminUser() {
    return getRequest().isUserInRole("BBC_ADMIN") || getRequest().isUserInRole(
            "BBC_RESEARCHER") || getRequest().isUserInRole("ADMIN");
  }

  public String logOut() {
    getRequest().getSession().invalidate();
    return "logout";
  }

  private HttpServletRequest getRequest() {
    return (HttpServletRequest) FacesContext.getCurrentInstance().
            getExternalContext().getRequest();
  }

  public boolean isLoggedIn() {
    return getRequest().getRemoteUser() != null;
  }

}
