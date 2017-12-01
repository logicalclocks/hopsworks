package io.hops.hopsworks.admin.security.ua;

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

  public String login() {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
    this.user = userFacade.findByEmail(this.credentials.getUsername());
    if (user == null) {
      context.addMessage(null, new FacesMessage("Login failed. User" + this.credentials.getUsername()));
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
      return "/login.xhtml";
    }
    try {
      request.login(this.credentials.getUsername(), passwordWithSaltPlusOtp);
      authController.registerLogin(user, request);
    } catch (ServletException e) {
      authController.registerAuthenticationFailure(user, request);
      context.addMessage(null, new FacesMessage("Login failed."));
      return "";
    }
    return "adminIndex";
  }

  public boolean isLoggedIn() {
    HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    return req.getRemoteUser() != null;
  }

  public void checkAlreadyLoggedin() throws IOException {
    if (isLoggedIn()) {
      ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
      ec.redirect(ec.getRequestContextPath() + "/security/protected/admin/adminIndex.xhtml");
    }
  }

  public void gotoSupport() throws IOException {
    String link = "https://groups.google.com/forum/#!forum/hopshadoop";
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    externalContext.redirect(link.trim());
  }
}
