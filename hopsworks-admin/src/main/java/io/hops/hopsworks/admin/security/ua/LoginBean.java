package io.hops.hopsworks.admin.security.ua;

import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.user.AuthController;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@ManagedBean
@Stateless
public class LoginBean {

  private final static Logger LOGGER = Logger.getLogger(LoginBean.class.getName());
  @EJB
  private UserFacade userFacade;
  @EJB
  private AuthController authController;
  
  private String username;
  private String password;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String login () {
    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
    Users user = userFacade.findByEmail(this.username);
    if (user == null) {
      context.addMessage(null, new FacesMessage("Login failed."));
      return "";
    }
    String passwordWithSaltPlusOtp;
    try {
      passwordWithSaltPlusOtp = authController.preCustomRealmLoginCheck(user, this.password, null, request);
    } catch (AppException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      context.addMessage(null, new FacesMessage("Login failed."));
      return "";
    }
    try {
      request.login(this.username, passwordWithSaltPlusOtp);
      authController.registerLogin(user, request);
    } catch (ServletException e) {
      authController.registerAuthenticationFailure(user, request);      
      context.addMessage(null, new FacesMessage("Login failed."));
      return "";
    }
    return "adminIndex";
  }
  
  public void gotoSupport() throws IOException {
    String link = "https://groups.google.com/forum/#!forum/hopshadoop";
    ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
    externalContext.redirect(link.trim());
  }
}
