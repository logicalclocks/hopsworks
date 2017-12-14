package io.hops.hopsworks.kmon.user;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.user.AuthController;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@ManagedBean
@SessionScoped
public class LoginBean implements Serializable {

  private final static Logger LOGGER = Logger.getLogger(LoginBean.class.getName());
  @EJB
  private UserFacade userFacade;
  @EJB
  private AuthController authController;
  @Inject
  private Credentials credentials;

  private Users user;
  private boolean twoFactor;

  @PostConstruct
  public void init() {
    twoFactor = false;
  }

  public boolean isTwoFactor() {
    return twoFactor;
  }

  public void setTwoFactor(boolean twoFactor) {
    this.twoFactor = twoFactor;
  }

  public Credentials getCredentials() {
    return credentials;
  }

  public void setCredentials(Credentials credentials) {
    this.credentials = credentials;
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  public Users getUserFromSession() {
    if (user == null) {
      ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
      String userEmail = context.getUserPrincipal().getName();
      user = userFacade.findByEmail(userEmail);
    }
    return user;
  }

  public String loginUsername() {
    this.user = getUserFromSession();
    if (this.user != null) {
      return user.getFname() + " " + user.getLname();
    }
    return "No username";
  }

  public String login() {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
    this.user = userFacade.findByEmail(this.credentials.getUsername());
    if (user == null) {
      context.addMessage(null, new FacesMessage("Login failed. User " + this.credentials.getUsername()));
      return "";
    }
    String passwordWithSaltPlusOtp;
    try {
      passwordWithSaltPlusOtp = authController.preCustomRealmLoginCheck(user, this.credentials.getPassword(),
          this.credentials.getOtp(), request);
    } catch (AppException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      context.addMessage(null, new FacesMessage("Login failed."));
      return "";
    } catch (EJBException ie) {
      String msg = ie.getCausedByException().getMessage();
      if (msg != null && !msg.isEmpty() && msg.contains("Second factor required.")) {
        setTwoFactor(true);
      }
      context.addMessage(null, new FacesMessage(msg));
      return "login";
    }
    try {
      request.login(this.credentials.getUsername(), passwordWithSaltPlusOtp);
      authController.registerLogin(user, request);
    } catch (ServletException e) {
      authController.registerAuthenticationFailure(user, request);
      context.addMessage(null, new FacesMessage("Login failed."));
      return "";
    }
    return "monitoring";
  }

  public String logOut() {
    try {
      this.user = getUserFromSession();
      HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
      req.getSession().invalidate();
      req.logout();
      if (user != null) {
        authController.registerLogout(user, req);
      }
      FacesContext.getCurrentInstance().getExternalContext().redirect("/hopsworks/#!/home");
    } catch (IOException | ServletException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return "login";
  }

  public boolean isLoggedIn() {
    HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    return req.getRemoteUser() != null;
  }

  public void checkAlreadyLoggedin() throws IOException {
    if (isLoggedIn()) {
      ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
      ec.redirect(ec.getRequestContextPath() + "/monitor/clusters.xhtml");
    }
  }

  public void gotoSupport() throws IOException {
    String link = "https://groups.google.com/forum/#!forum/hopshadoop";
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    externalContext.redirect(link.trim());
  }

  public void gotoLoginHelp() throws IOException {
    String link = "/hopsworks-admin/security/login_issue.xhtml";
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    externalContext.redirect(link.trim());
  }
}
